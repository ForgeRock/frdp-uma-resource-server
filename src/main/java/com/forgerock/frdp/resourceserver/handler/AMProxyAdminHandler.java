/*
 * Copyright (c) 2019-2020, ForgeRock, Inc., All rights reserved
 * Use subject to license terms.
 */
package com.forgerock.frdp.resourceserver.handler;

import com.forgerock.frdp.common.ConstantsIF;
import com.forgerock.frdp.config.ConfigurationManagerIF;
import com.forgerock.frdp.dao.Operation;
import com.forgerock.frdp.dao.OperationIF;
import com.forgerock.frdp.handler.HandlerManagerIF;
import com.forgerock.frdp.resourceserver.ConfigIF;
import com.forgerock.frdp.utils.JSON;
import com.forgerock.frdp.utils.STR;
import java.util.logging.Level;
import org.json.simple.JSONObject;

/**
 * This Handler provides a SSO Session token for a proxy admin (amadmin)
 *
 * <pre>
 * Check the MongoDB for an existing token
 * If exists, validate it,
 * If valid return, else get a new token
 * If need new token
 * Create session and store in MongoDB
 * Return new token
 * </pre>
 *
 * @author Scott Fehrman, ForgeRock Inc.
 */
public class AMProxyAdminHandler extends CredentialHandler {

   private final String CLASS = this.getClass().getName();

   /**
    * Constructor
    *
    * @param configMgr ConfigurationManagerIF management of configurations
    * @param handlerMgr HandlerManagerIF handler manager
    */
   public AMProxyAdminHandler(final ConfigurationManagerIF configMgr, final HandlerManagerIF handlerMgr) {
      super(configMgr, handlerMgr);

      String METHOD = "AMProxyAdminHandler(configMgr, handlerMgr)";

      _logger.entering(CLASS, METHOD);

      this.init();

      _logger.exiting(CLASS, METHOD);

      return;
   }

   /*
    * ================= PROTECTED METHODS =================
    */
   /**
    * Validate the OperationIF object, overrides the subclass
    *
    * @param oper OperationIF
    * @throws Exception could not validate the operation
    */
   @Override
   protected void validate(final OperationIF oper) throws Exception {
      String METHOD = Thread.currentThread().getStackTrace()[1].getMethodName();

      _logger.entering(CLASS, METHOD);

      if (oper == null) {
         throw new Exception("Operation object is null");
      }

      _logger.exiting(CLASS, METHOD);

      return;
   }

   /**
    * Support the "read" operation
    *
    * <pre>
    * No inputs
    * Output is JSON for the session ...
    *
    * {
    *   "data": {
    *     "tokenId": "...*...*",
    *     "successUrl": "/openam/console",
    *     "realm": "/"
    *   }
    * }
    * </pre>
    *
    * @param operInput OperationIF input object
    * @return OperationIF output object
    */
   @Override
   protected OperationIF read(final OperationIF operInput) {
      boolean error = false;
      String METHOD = Thread.currentThread().getStackTrace()[1].getMethodName();
      String msg = null;
      OperationIF operOutput = null;
      OperationIF operReadOutput = null;
      JSONObject jsonOutput = null;
      JSONObject jsonCredential = null;

      _logger.entering(CLASS, METHOD);

      operOutput = new Operation(OperationIF.TYPE.READ);

      if (!error) {
         operReadOutput = this.readImpl();

         if (!operReadOutput.isError()) {
            jsonCredential = JSON.getObject(operReadOutput.getJSON(), ConstantsIF.DATA + "." + ConstantsIF.CREDENTIAL);

            jsonOutput = new JSONObject();
            jsonOutput.put(ConstantsIF.DATA, jsonCredential);

            operOutput.setJSON(jsonOutput);
            operOutput.setState(STATE.SUCCESS);
            operOutput.setStatus("valid session for : "
               + JSON.getObject(operReadOutput.getJSON(), ConstantsIF.DATA + "." + ConstantsIF.OWNER));
         } else {
            error = true;
            msg = operReadOutput.getStatus();

            _logger.log(Level.WARNING, "{0}: {1}: {2}", new Object[]{CLASS, METHOD, operReadOutput.getStatus()});
         }
      }

      if (error) {
         operOutput.setState(STATE.ERROR);
         operOutput.setError(true);
         operOutput.setStatus(msg);
      }

      _logger.exiting(CLASS, METHOD);

      return operOutput;
   }

   /*
    * =============== PRIVATE METHODS ===============
    */

   /**
    * Read implementation
    *
    * <pre>
    * Get the credential record, for the "admin" proxy user
    * Validate the SSO session, using the "tokenId"
    * If NOT valid, re-authenticate using proxy admin name / password
    * Update the credential record
    *
    * JSON output ...
    * {
    *   "data": {
    *     "owner": "amadmin",
    *     "category" : "sso_session",
    *     "credential": {
    *       "tokenId": "...*...*",
    *       "successUrl": "/openam/console",
    *       "realm": "/"
    *     }
    *   }
    * }
    * </pre>
    *
    * @param operInput
    * @return
    */
   private synchronized OperationIF readImpl() {
      boolean error = false;
      boolean valid = false;
      String METHOD = Thread.currentThread().getStackTrace()[1].getMethodName();
      String owner = null;
      String category = null;
      String credUid = null;
      String configType = ConstantsIF.RESOURCE;
      StringBuilder buf = new StringBuilder(METHOD);
      OperationIF operOutput = null;
      OperationIF operCredInput = null;
      OperationIF operCredOutput = null;
      JSONObject jsonCredInput = null;
      JSONObject jsonCredOutput = null;
      JSONObject jsonValidateOutput = null;

      _logger.entering(CLASS, METHOD);

      operOutput = new Operation(OperationIF.TYPE.READ);

      try {
         owner = this.getConfigValue(configType, ConfigIF.AS_ADMIN_USER);
         category = this.getConfigValue(configType, ConfigIF.RS_CREDENTIAL_CATEGORIES_SSO_ID);
         credUid = this.getCredentialUid(owner, category);
      } catch (Exception ex) {
         error = true;
         buf.append(": ").append(ex.getMessage());
      }

      if (!STR.isEmpty(credUid)) {
         jsonCredInput = new JSONObject();
         jsonCredInput.put(ConstantsIF.UID, credUid);

         operCredInput = new Operation(OperationIF.TYPE.READ);
         operCredInput.setJSON(jsonCredInput);

         try {
            this.setDatabaseAndCollection(operCredInput, ConfigIF.RS_NOSQL_DATABASE,
               ConfigIF.RS_NOSQL_COLLECTIONS_CREDENTIALS_NAME);
         } catch (Exception ex) {
            error = true;
            buf.append(": ").append(ex.getMessage());
         }

         if (!error) {
            /*
             * JSON input ... { "uid": "..." }
             */
            operCredOutput = _MongoDAO.execute(operCredInput); // READ the credential

            /*
             * validate the sso token
             */
            if (!operCredOutput.isError()) {
               jsonCredOutput = operCredOutput.getJSON();
               try {
                  jsonValidateOutput = this.validateToken(jsonCredOutput);

                  valid = JSON.getBoolean(jsonValidateOutput, ConstantsIF.DATA + "." + ConstantsIF.VALID);

                  if (!valid) {
                     jsonCredOutput = this.getToken(credUid, jsonCredOutput);
                  }
               } catch (Exception ex) {
                  error = true;
                  buf.append(": ").append(ex.getMessage());
               }

            } else {
               error = true;
               buf.append(": Read error: ").append(operCredOutput.getStatus());
            }
         }
      } else {
         if (!error) {
            try {
               jsonCredOutput = this.getToken(credUid, jsonCredOutput);
            } catch (Exception ex) {
               error = true;
               buf.append(": ").append(ex.getMessage());
            }
         }
      }

      if (error) {
         operOutput.setState(STATE.ERROR);
         operOutput.setStatus(buf.toString());
         operOutput.setError(error);
      } else {
         operOutput.setJSON(jsonCredOutput);
         operOutput.setState(STATE.SUCCESS);
         operOutput.setStatus("valid: " + valid);
      }

      _logger.exiting(CLASS, METHOD);

      return operOutput;
   }

   /**
    * Validate the AM Single Sign On token
    *
    * <pre>
    * JSON input ...
    * {
    *   "data": {
    *     "owner": "amadmin",
    *     "category" : "sso_session",
    *     "credential": {
    *       "tokenId": "...*...*",
    *       "successUrl": "/openam/console",
    *       "realm": "/"
    *     }
    *   }
    * }
    * JSON output ...   200: OK
    * {
    *   "valid":true,
    *   "sessionUid":"209331b0-6d31-4740-8d5f-740286f6e69f-326295",
    *   "uid":"demo",
    *   "realm":"/"
    * }
    * -or-
    * {
    *   "valid":false
    * }
    * </pre>
    *
    * @param jsonInput
    * @return
    * @throws Exception
    */
   private JSONObject validateToken(final JSONObject jsonInput) throws Exception {
      String METHOD = Thread.currentThread().getStackTrace()[1].getMethodName();
      String sso_token = null;
      StringBuilder buf = new StringBuilder(METHOD + ": ");
      JSONObject jsonValidateInput = null;
      JSONObject jsonValidateOutput = null;
      JSONObject jsonData = null;

      _logger.entering(CLASS, METHOD);

      if (_logger.isLoggable(DEBUG_LEVEL)) {
         _logger.log(DEBUG_LEVEL, "input=''{0}''", new Object[]{jsonInput != null ? jsonInput.toString() : NULL});
      }

      sso_token = JSON.getString(jsonInput,
         ConstantsIF.DATA + "." + ConstantsIF.CREDENTIAL + "." + ConstantsIF.TOKENID);

      if (STR.isEmpty(sso_token)) {
         buf.append("JSON input has en empty '" + ConstantsIF.TOKENID + "' value");
         throw new Exception(buf.toString());
      }

      jsonValidateInput = new JSONObject();
      jsonValidateInput.put(ConstantsIF.UID, sso_token);

      jsonValidateOutput = this.validateSession(jsonValidateInput);

      jsonData = JSON.getObject(jsonValidateOutput, ConstantsIF.DATA);

      if (jsonData == null || !jsonData.containsKey(ConstantsIF.VALID)) {
         buf.append("JSON output is null or attribute 'valid' is missing");
         throw new Exception(buf.toString());
      }

      if (_logger.isLoggable(DEBUG_LEVEL)) {
         _logger.log(DEBUG_LEVEL, "output=''{0}''",
            new Object[]{jsonValidateOutput != null ? jsonValidateOutput.toString() : NULL});
      }

      _logger.exiting(CLASS, METHOD);

      return jsonValidateOutput;
   }

   /**
    * Get a new sso session for the admin user. Create or update the credential
    * record
    *
    * <pre>
    * credUid MAY BE NULL
    * JSON input ...  MAY BE NULL
    * {
    *   "data": {
    *     "owner": "amadmin",
    *     "category" : "sso_session",
    *     "credential": {
    *       "tokenId": "...*...*",
    *       "successUrl": "/openam/console",
    *       "realm": "/"
    *     }
    *   }
    * }
    *
    * Create a sso session (and token) for the admin user
    * JSON input ...
    * {
    *   "user": "<<user_login>>"
    *   "password": "<<user_password>>"
    * }
    * JSON output ...
    * {
    *   "tokenId": "....*...*",
    *   "successUrl": "/openam/console",
    *   "realm": "/"
    * }
    * </pre>
    *
    * @param credUid
    * @param jsonInput
    * @return
    * @throws Exception
    */
   private JSONObject getToken(final String credUid, final JSONObject jsonInput) throws Exception {
      String METHOD = Thread.currentThread().getStackTrace()[1].getMethodName();
      String configType = ConstantsIF.RESOURCE;
      JSONObject jsonGetInput = null;
      JSONObject jsonGetOutput = null;
      JSONObject jsonCredInput = null;
      JSONObject jsonData = null;
      JSONObject jsonOutput = null;
      OperationIF operCredInput = null;
      OperationIF operCredOutput = null;

      _logger.entering(CLASS, METHOD);

      jsonGetInput = new JSONObject();
      jsonGetInput.put(ConstantsIF.USER, this.getConfigValue(configType, ConfigIF.AS_ADMIN_USER));
      jsonGetInput.put(ConstantsIF.PASSWORD, this.getConfigValue(configType, ConfigIF.AS_ADMIN_PASSWORD));

      jsonGetOutput = this.getSession(jsonGetInput);

      if (STR.isEmpty(JSON.getString(jsonGetOutput, ConstantsIF.TOKENID))) {
         throw new Exception(METHOD + ": Session 'tokenId' is empty");
      }

      jsonData = new JSONObject();

      jsonData.put(ConstantsIF.OWNER, this.getConfigValue(configType, ConfigIF.AS_ADMIN_USER));
      jsonData.put(ConstantsIF.CATEGORY, this.getConfigValue(configType, ConfigIF.RS_CREDENTIAL_CATEGORIES_SSO_ID));
      jsonData.put(ConstantsIF.CREDENTIAL, jsonGetOutput);

      jsonCredInput = new JSONObject();
      jsonCredInput.put(ConstantsIF.DATA, jsonData);

      /*
       * If the credUid is empty then "create" cred, else "replace" cred
       */
      if (STR.isEmpty(credUid)) // CREATE
      {
         operCredInput = new Operation(OperationIF.TYPE.CREATE);

         this.setDatabaseAndCollection(operCredInput, ConfigIF.RS_NOSQL_DATABASE,
            ConfigIF.RS_NOSQL_COLLECTIONS_CREDENTIALS_NAME);

         operCredInput.setJSON(jsonCredInput);

         operCredOutput = _MongoDAO.execute(operCredInput);
      } else // REPLACE
      {
         operCredInput = new Operation(OperationIF.TYPE.REPLACE);

         this.setDatabaseAndCollection(operCredInput, ConfigIF.RS_NOSQL_DATABASE,
            ConfigIF.RS_NOSQL_COLLECTIONS_CREDENTIALS_NAME);

         jsonCredInput.put(ConstantsIF.UID, credUid);

         operCredInput.setJSON(jsonCredInput);

         operCredOutput = _MongoDAO.execute(operCredInput);
      }

      if (operCredOutput.isError() || operCredOutput.getState() != STATE.SUCCESS) {
         throw new Exception(METHOD + ": Mongo DAO error: " + operCredOutput.getState().toString() + ": "
            + operCredOutput.getStatus());
      }

      jsonOutput = jsonCredInput;

      _logger.exiting(CLASS, METHOD);

      return jsonOutput;
   }

}
