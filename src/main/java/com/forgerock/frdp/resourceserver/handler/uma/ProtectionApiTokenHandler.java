/*
 * Copyright (c) 2018-2020, ForgeRock, Inc., All rights reserved
 * Use subject to license terms.
 */
package com.forgerock.frdp.resourceserver.handler.uma;

import com.forgerock.frdp.common.ConstantsIF;
import com.forgerock.frdp.config.ConfigurationManagerIF;
import com.forgerock.frdp.dao.Operation;
import com.forgerock.frdp.dao.OperationIF;
import com.forgerock.frdp.handler.HandlerManagerIF;
import com.forgerock.frdp.resourceserver.ConfigIF;
import com.forgerock.frdp.resourceserver.handler.CredentialHandler;
import com.forgerock.frdp.utils.JSON;
import com.forgerock.frdp.utils.STR;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLDecoder;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.logging.Level;
import org.json.simple.JSONObject;

/**
 * Implements operations to the "credential" store (MongoDB) Create, Read,
 * Update, Delete, Search The "creation" of a credential is specific to the UMA
 * Protection API Token (PAT) - validate the user's SSO session - get an OAuth
 * authorization code - get an OAuth access token
 *
 * @author Scott Fehrman, ForgeRock, Inc.
 */
public class ProtectionApiTokenHandler extends CredentialHandler {

   private final String CLASS = this.getClass().getName();

   /**
    * Constructor
    *
    * @param configMgr ConfigurationManagerIF management of configurations
    * @param handlerMgr HandlerManagerIF provides management of Handlers
    */
   public ProtectionApiTokenHandler(final ConfigurationManagerIF configMgr, final HandlerManagerIF handlerMgr) {
      super(configMgr, handlerMgr);

      String METHOD = "ProtectionApiTokenHandler(configMgr, handlerMgr)";

      _logger.entering(CLASS, METHOD);

      this.init();

      _logger.exiting(CLASS, METHOD);

      return;
   }

   /*
    * ================= PROTECTED METHODS =================
    */
   /**
    * Override the "validate" interface, used to check the operation input
    *
    * @param oper OperationaIF operation input
    * @exception Exception could not validate the operation
    */
   @Override
   protected void validate(final OperationIF oper) throws Exception {
      String METHOD = Thread.currentThread().getStackTrace()[1].getMethodName();
      JSONObject jsonInput = null;

      _logger.entering(CLASS, METHOD);

      if (oper == null) {
         throw new Exception("Operation object is null");
      }

      jsonInput = oper.getJSON();
      if (jsonInput == null || jsonInput.isEmpty()) {
         throw new Exception("JSON Input is null or empty");
      }

      _logger.exiting(CLASS, METHOD);

      return;
   }

   /**
    * Override interface to support the "read" operation. Get the "credentials"
    * For either the SSOToken (which is related to userId, the owner) Or the
    * explicit "owner" Search the collection, return the first result (should be
    * only one). Validate the credential, if validate return it Else, use
    * refresh token and create a new token, save If not exist, create a new
    * credential and save it
    *
    * <pre>
    * JSON input ... (if using an SSO token)
    * { "ssotoken": "..." }
    *
    * JSON input ... (if using an owner)
    * { "owner": "..." }
    *
    * JSON output ...
    * {
    *   "data": {
    *     "owner": "",
    *     "category" : "uma_pat",
    *     "credential": {
    *       "access_token": "...",
    *       "refresh_token": "...",
    *       "scope": "uma_protection",
    *       "token_type": "Bearer",
    *       "expires_in": "..."
    *     }
    *   }
    * }
    * </pre>
    *
    * @param operInput OperationIF input for create operation
    * @return OperationIF output from create operation
    */
   @Override
   protected OperationIF read(final OperationIF operInput) {
      boolean error = false;
      String METHOD = Thread.currentThread().getStackTrace()[1].getMethodName();
      String msg = null;
      String ssotoken = null;
      String owner = null;
      String category = null;
      String credUid = null;
      String configType = ConstantsIF.RESOURCE;
      OperationIF operOutput = null;
      OperationIF operReadInput = null;
      OperationIF operReadOutput = null;
      OperationIF operCreateInput = null;
      OperationIF operCreateOutput = null;
      JSONObject jsonInput = null;
      JSONObject jsonOutput = null;
      JSONObject jsonCredential = null;
      JSONObject jsonData = null;

      _logger.entering(CLASS, METHOD);

      operOutput = new Operation(OperationIF.TYPE.READ);

      jsonInput = operInput.getJSON();

      if (_logger.isLoggable(DEBUG_LEVEL)) {
         _logger.log(DEBUG_LEVEL, "input=''{0}'', json=''{1}''", new Object[]{
            operInput != null ? operInput.toString() : NULL, jsonInput != null ? jsonInput.toString() : NULL});
      }

      ssotoken = JSON.getString(jsonInput, ConstantsIF.SSO_TOKEN);

      /*
       * Get the user id (owner) related to the SSO Token Else, check for "owner" from
       * the input
       */
      if (!STR.isEmpty(ssotoken)) {
         owner = this.getUserIdFromSSO(ssotoken);
         if (STR.isEmpty(owner)) {
            error = true;
            msg = "'owner' is empty for the SSO session";
         }
      } else {
         owner = JSON.getString(jsonInput, ConstantsIF.OWNER);
      }

      if (!STR.isEmpty(owner)) {
         try {
            category = this.getConfigValue(configType, ConfigIF.RS_CREDENTIAL_CATEGORIES_PAT_ID);
            credUid = this.getCredentialUid(owner, category);
         } catch (Exception ex) {
            error = true;
            msg = ex.getMessage();
         }

         if (!error && !STR.isEmpty(credUid)) {
            jsonInput = new JSONObject();
            jsonInput.put(ConstantsIF.UID, credUid);

            operReadInput = new Operation(OperationIF.TYPE.READ);
            operReadInput.setJSON(jsonInput);

            operReadOutput = this.readImpl(operReadInput);

            if (!operReadOutput.isError()) {
               jsonCredential = JSON.getObject(operReadOutput.getJSON(),
                  ConstantsIF.DATA + "." + ConstantsIF.CREDENTIAL);
               operOutput = operReadOutput;
            } else {
               /*
                * If there was an error "reading" the existing credential It will be deleted
                * automatically. Log the error message and continue A new credential will be
                * created.
                */
               _logger.log(Level.WARNING, "{0}: {1}: {2}",
                  new Object[]{CLASS, METHOD, operReadOutput.getStatus()});
            }
         }
      }

      /*
       * If the "credential" was not found, need to create one, if there is a SSO
       * token
       */
      if (!error && jsonCredential == null && !STR.isEmpty(ssotoken)) {
         jsonData = new JSONObject();
         jsonData.put(ConstantsIF.SSO_TOKEN, ssotoken);
         jsonData.put(ConstantsIF.UID, owner);

         jsonInput = new JSONObject();
         jsonInput.put(ConstantsIF.DATA, jsonData);

         operCreateInput = new Operation(OperationIF.TYPE.CREATE);
         operCreateInput.setJSON(jsonInput);

         /*
          * JSON input ... { "data": { "ssotoken": "...", "uid": "bjensen" } } JSON
          * output ... { "uid": "..." }
          */
         operCreateOutput = this.createImpl(operCreateInput);

         if (operCreateOutput.getState() == STATE.SUCCESS) {
            jsonOutput = operCreateOutput.getJSON();

            /*
             * READ the newly created credential data JSON input ... { "uid": "..." } JSON
             * output ... { "data": { "credential": { ... }, ... } }
             */
            operReadInput = new Operation(OperationIF.TYPE.READ);
            operReadInput.setJSON(jsonOutput);

            operReadOutput = this.readImpl(operReadInput);

            if (operReadOutput.getState() == STATE.SUCCESS) {
               operOutput = operReadOutput;
            } else {
               error = true;
               msg = operReadOutput.getStatus();
            }
         } else {
            error = true;
            msg = operCreateOutput.getStatus();
         }
      }

      if (error) {
         operOutput.setError(true);
         operOutput.setState(STATE.ERROR);
         operOutput.setStatus(msg);

         _logger.log(Level.SEVERE, msg);
      }

      if (_logger.isLoggable(DEBUG_LEVEL)) {
         _logger.log(DEBUG_LEVEL, "output=''{0}''",
            new Object[]{operOutput != null ? operOutput.toString() : NULL});
      }

      _logger.exiting(CLASS, METHOD);

      return operOutput;
   }

   /*
    * =============== PRIVATE METHODS ===============
    */
   /**
    * Implementation of the "read" operation.
    *
    * <pre>
    * JSON input ...
    * { "uid": "..." }
    * JSON output ...
    * {
    *   "data": {
    *     "owner": "",
    *     "category" : "uma_pat",
    *     "credential": {
    *       "access_token": "...",
    *       "refresh_token": "...",
    *       "scope": "uma_protection",
    *       "token_type": "Bearer",
    *       "expires_in": "..."
    *     }
    *   }
    * }
    * </pre>
    *
    * @param operInput OperationIF input
    * @return OperationIF output
    */
   private OperationIF readImpl(final OperationIF operInput) {
      boolean error = false;
      boolean delete = false;
      String METHOD = Thread.currentThread().getStackTrace()[1].getMethodName();
      StringBuilder buf = new StringBuilder(METHOD);
      OperationIF operReadOutput = null;
      OperationIF operValidateOutput = null;
      OperationIF operRefreshOutput = null;
      OperationIF operOutput = null;
      OperationIF operReplaceOutput = null;
      OperationIF operDeleteOutput = null;
      JSONObject jsonReadData = null;
      JSONObject jsonRefreshCred = null;
      JSONObject jsonInput = null;

      _logger.entering(CLASS, METHOD);

      if (_logger.isLoggable(DEBUG_LEVEL)) {
         _logger.log(DEBUG_LEVEL, "input=''{0}''",
            new Object[]{operInput != null ? operInput.toString() : NULL});
      }

      operOutput = new Operation(operInput.getType());

      jsonInput = operInput.getJSON();

      try {
         this.setDatabaseAndCollection(operInput, ConfigIF.RS_NOSQL_DATABASE,
            ConfigIF.RS_NOSQL_COLLECTIONS_CREDENTIALS_NAME);
      } catch (Exception ex) {
         error = true;
         buf.append(": ").append(ex.getMessage());
      }

      if (!error) {
         operReadOutput = _MongoDAO.execute(operInput); // READ the credential

         /*
          * validate the token
          */
         if (!operReadOutput.isError()) {
            try {
               operValidateOutput = this.validateToken(operReadOutput);
               switch (operValidateOutput.getState()) {
                  case SUCCESS: // 200 valid
                  {
                     operOutput = operReadOutput;
                     break;
                  }
                  case NOTAUTHORIZED: // 401 invalid
                  {
                     /*
                   * get a new access token from the refresh token
                      */

                     operRefreshOutput = this.refreshToken(operReadOutput);

                     if (operRefreshOutput.getState() == STATE.SUCCESS) {
                        /*
                      * Got a new set of credential attributes "access_token" and "refresh_token"
                      * need to replace these attributes in the record
                         */
                        jsonReadData = JSON.getObject(operReadOutput.getJSON(), ConstantsIF.DATA);

                        jsonRefreshCred = operRefreshOutput.getJSON();

                        if (jsonRefreshCred != null) {
                           jsonReadData.put(ConstantsIF.CREDENTIAL, jsonRefreshCred);

                           jsonInput.put(ConstantsIF.DATA, jsonReadData);

                           operInput.setType(OperationIF.TYPE.REPLACE);

                           operReplaceOutput = _MongoDAO.execute(operInput);

                           if (operReplaceOutput.getState() == STATE.SUCCESS) {
                              operOutput.setState(operReplaceOutput.getState());
                              operOutput.setStatus(operReplaceOutput.getStatus());
                              operOutput.setJSON(jsonInput);
                           } else {
                              /*
                            * failed to replace the credential, delete it
                               */
                              delete = true;
                           }
                        } else {
                           delete = true;
                           error = true;
                           buf.append(": JSON data from refresh is empty");
                        }
                     } else {
                        /*
                      * Could NOT get a refresh token, Delete the record
                         */
                        delete = true;
                     }
                     break;
                  }
                  case FAILED:
                  case ERROR: {
                     delete = true;
                     error = true;
                     buf.append(": ").append(operValidateOutput.getState().toString()).append(": ")
                        .append(operValidateOutput.getStatus());
                     break;
                  }
                  default:
                     error = true;
                     buf.append(": Validate Output has an unsupportted state: '")
                        .append(operValidateOutput.getState().toString()).append("'");
               }
            } catch (Exception ex) {
               delete = true;
               error = true;
               buf.append(": 'access_token' validation failed: ").append(ex.getMessage());
            }

         } else {
            error = true;
            buf.append(": Error reading credential: ").append(operReadOutput.getState().toString()).append(": ")
               .append(operReadOutput.getStatus());
         }
      }

      if (delete) {
         operInput.setType(OperationIF.TYPE.DELETE);

         _logger.log(Level.WARNING, "{0}: {1}: Deleting credential, {2}",
            new Object[]{CLASS, METHOD, operInput.toString()});

         operDeleteOutput = _MongoDAO.execute(operInput);

         if (operDeleteOutput.getState() == STATE.SUCCESS) {
            buf.append(": Deleted record");
            operOutput = new Operation(OperationIF.TYPE.READ);
            operOutput.setError(true);
            operOutput.setState(STATE.ERROR);
            operOutput.setStatus(buf.toString());
         } else {
            error = true;
            buf.append(": ").append(operDeleteOutput.getState().toString()).append(": ")
               .append(operDeleteOutput.getStatus());
         }
      }

      if (error) {
         operOutput = new Operation(OperationIF.TYPE.READ);
         operOutput.setError(true);
         operOutput.setState(STATE.ERROR);
         operOutput.setStatus(buf.toString());

         _logger.log(Level.SEVERE, buf.toString());
      }

      if (_logger.isLoggable(DEBUG_LEVEL)) {
         _logger.log(DEBUG_LEVEL, "output=''{0}''",
            new Object[]{operOutput != null ? operOutput.toString() : NULL});
      }

      _logger.exiting(CLASS, METHOD);

      return operOutput;
   }

   /**
    * Implementation of the "create" operation.
    *
    * <pre>
    * JSON input ...
    * {
    *   "data": {
    *     "ssotoken": "...",
    *     "uid": "bjensen"
    *   }
    * }
    * JSON output ...
    * {
    *   "uid": ...
    * }
    * a new credential
    * {
    *   "data": {
    *     "owner": "bjensen",  // from ssoOutput: uid attribute
    *     "category": "uma_pat",
    *     "credential": { // from tokenOutput
    *       "access_token": "...",
    *       "refresh_token": "...",
    *       "scope": "uma_protection",
    *       "token_type": "Bearer",
    *       "expires_in": "..."
    *     }
    *   }
    * }
    * </pre>
    *
    * @param operInput
    * @return
    */
   private OperationIF createImpl(final OperationIF operInput) {
      boolean error = false;
      String METHOD = Thread.currentThread().getStackTrace()[1].getMethodName();
      String msg = null;
      String owner = null; // 'uid' from SSO session validation
      String category = null;
      String configType = ConstantsIF.RESOURCE;
      OperationIF operOutput = null;
      OperationIF authzOutput = null;
      OperationIF tokenOutput = null;
      OperationIF credInput = null;
      OperationIF credOutput = null;
      JSONObject jsonOutput = null;
      JSONObject jsonCred = null;
      JSONObject jsonInput = null;
      JSONObject jsonData = null;

      _logger.entering(CLASS, METHOD);

      if (_logger.isLoggable(DEBUG_LEVEL)) {
         _logger.log(DEBUG_LEVEL, "input=''{0}''",
            new Object[]{operInput != null ? operInput.toString() : NULL});
      }

      operOutput = new Operation(operInput.getType());
      credInput = new Operation(OperationIF.TYPE.CREATE);

      owner = JSON.getString(operInput.getJSON(), ConstantsIF.DATA + "." + ConstantsIF.UID);

      if (!STR.isEmpty(owner)) {
         try {
            authzOutput = this.getAuthorizationCode(operInput);
         } catch (Exception ex) {
            error = true;
            msg = ex.getMessage();
         }
      } else {
         error = true;
         msg = "'owner' attribute is empty";
      }

      if (!error) {
         try {
            category = this.getConfigValue(configType, ConfigIF.RS_CREDENTIAL_CATEGORIES_PAT_ID);
            tokenOutput = this.getAccessToken(authzOutput);
         } catch (Exception ex) {
            error = true;
            msg = ex.getMessage();
         }

         if (!error) {
            /*
             * Store the "PAT" as a new credential
             */
            if (tokenOutput != null && !tokenOutput.isError()) {
               try {
                  this.setDatabaseAndCollection(credInput, ConfigIF.RS_NOSQL_DATABASE,
                     ConfigIF.RS_NOSQL_COLLECTIONS_CREDENTIALS_NAME);
               } catch (Exception ex) {
                  error = true;
                  msg = ex.getMessage();
               }
            } else {
               error = true;
               msg = (tokenOutput == null ? "Token output is null" : tokenOutput.getStatus());
            }

            if (!error) {
               jsonOutput = tokenOutput.getJSON();

               jsonCred = JSON.getObject(jsonOutput, ConstantsIF.DATA);

               if (jsonCred != null) {
                  jsonData = new JSONObject();
                  jsonData.put(ConstantsIF.OWNER, owner);
                  jsonData.put(ConstantsIF.CATEGORY, category);
                  jsonData.put(ConstantsIF.CREDENTIAL, jsonCred);

                  jsonInput = new JSONObject();
                  jsonInput.put(ConstantsIF.DATA, jsonData);

                  credInput.setJSON(jsonInput);

                  credOutput = _MongoDAO.execute(credInput);

                  if (credOutput != null) {
                     operOutput = credOutput;
                  } else {
                     error = true;
                     msg = "MongoDAO Credential output is null";
                  }
               } else {
                  error = true;
                  msg = "JSON object from token is null or missing 'data' object";
               }
            }
         }
      }

      if (error) {
         operOutput.setError(true);
         operOutput.setState(STATE.ERROR);
         operOutput.setStatus(msg);

         _logger.log(Level.SEVERE, msg);
      }

      if (_logger.isLoggable(DEBUG_LEVEL)) {
         _logger.log(DEBUG_LEVEL, "output=''{0}''",
            new Object[]{operOutput != null ? operOutput.toString() : NULL});
      }

      _logger.exiting(CLASS, METHOD);

      return operOutput;
   }

   /**
    * Get an OAuth 2.0 authorization code from the Authorization Server (AS).
    * Get authorization code using the ssotoken .../openam/oauth2/authorize
    *
    * <pre>
    * JSON input ...
    * {
    *   "data": {
    *     "ssotoken": "...", // dynamically set based on authenticated user
    *     "uid": "bjensen"
    *   }
    * }
    * JSON output ...
    * {
    *   "code": "..."
    * }
    * Build JSON "input" for getting the authorization code
    * {
    *   "form": {
    *     "response_type": "code",
    *     "save_consent": "off",
    *     "decision": "allow",
    *     "client_id": "...",
    *     "redirect_uri": "...",
    *     "scope": "uma_protection",
    *     "csrf": "<ssotoken>"
    *   }
    *   "headers": {
    *     "accept-api-version": "resource=2.0,protocol=1.0",
    *     "content-type": "application/x-www-form-urlencoded",
    *     "iPlanetDirectoryPro": "<ssotoken>",
    *     "cookie": "iPlanetDirectoryPro=<ssotoken>"
    *   },
    *   "path": "oauth2/realms/root/authorize"
    * }
    * Response is a 302 Found (redirect) ...
    * need to get the "authorization code" from the "Location" header
    * {
    *   "headers": {
    *     "Location": "http...&code=...";
    *     ...
    *   }
    * }
    * </pre>
    *
    * @param operInput OperationIF input
    * @return OperationIF output
    * @throws Exception
    */
   private OperationIF getAuthorizationCode(final OperationIF operInput) throws Exception {
      String METHOD = Thread.currentThread().getStackTrace()[1].getMethodName();
      String ssoToken = null;
      String location = null;
      String code = null;
      String configType = ConstantsIF.RESOURCE;
      StringBuilder buf = new StringBuilder(METHOD + ": ");
      OperationIF operOutput = null;
      OperationIF operASInput = null;
      OperationIF operASOutput = null;
      JSONObject jsonInput = null;
      JSONObject jsonForm = null;
      JSONObject jsonHeaders = null;
      URL url = null;
      Map<String, String> queryParams = null;

      _logger.entering(CLASS, METHOD);

      if (_logger.isLoggable(DEBUG_LEVEL)) {
         _logger.log(DEBUG_LEVEL, "input=''{0}''",
            new Object[]{operInput != null ? operInput.toString() : NULL});
      }

      operOutput = new Operation(OperationIF.TYPE.CREATE);

      jsonInput = operInput.getJSON();

      ssoToken = JSON.getString(jsonInput, ConstantsIF.DATA + "." + ConstantsIF.SSO_TOKEN);

      if (!STR.isEmpty(ssoToken)) {
         jsonForm = new JSONObject();
         jsonForm.put(ConstantsIF.RESPONSE_TYPE, ConstantsIF.CODE);
         jsonForm.put(ConstantsIF.SAVE_CONSENT, ConstantsIF.OFF);
         jsonForm.put(ConstantsIF.DECISION, ConstantsIF.ALLOW);
         jsonForm.put(ConstantsIF.CLIENT_ID, this.getConfigValue(configType, ConfigIF.RS_OAUTH2_CLIENT_ID));
         jsonForm.put(ConstantsIF.REDIRECT_URI, this.getConfigValue(configType, ConfigIF.RS_OAUTH2_CLIENT_REDIRECT));
         jsonForm.put(ConstantsIF.SCOPE, ConstantsIF.UMA_PROTECTION);
         jsonForm.put(ConstantsIF.CSRF, ssoToken);

         jsonHeaders = new JSONObject();
         jsonHeaders.put(ConstantsIF.ACCEPT_API_VERSION, this.getConfigValue(configType, ConfigIF.AS_OAUTH2_AUTHORIZE_ACCEPT));
         jsonHeaders.put(ConstantsIF.CONTENT_TYPE, ConstantsIF.APPLICATION_FORM_URLENCODED);
         jsonHeaders.put(ConstantsIF.ACCEPT, ConstantsIF.TYPE_WILDCARD);
         jsonHeaders.put(this.getConfigValue(configType, ConfigIF.AS_COOKIE), ssoToken);
         jsonHeaders.put(ConstantsIF.COOKIE, this.getConfigValue(configType, ConfigIF.AS_COOKIE) + "=" + ssoToken);

         jsonInput = new JSONObject();
         jsonInput.put(ConstantsIF.FORM, jsonForm);
         jsonInput.put(ConstantsIF.HEADERS, jsonHeaders);
         jsonInput.put(ConstantsIF.PATH, this.getConfigValue(configType, ConfigIF.AS_OAUTH2_AUTHORIZE_PATH));

         operASInput = new Operation(OperationIF.TYPE.CREATE); // POST
         operASInput.setJSON(jsonInput);

         operASOutput = _AuthzServerDAO.execute(operASInput); // get authorization code

         if (operASOutput.getState() == STATE.WARNING) // 302 Found, redirect
         {
            jsonHeaders = JSON.getObject(operASOutput.getJSON(), ConstantsIF.HEADERS);

            if (jsonHeaders != null) {
               if (jsonHeaders.containsKey("location")) {
                  location = JSON.getString(jsonHeaders, "location");
               } else if (jsonHeaders.containsKey("Location")) {
                  location = JSON.getString(jsonHeaders, "Location");
               } else {
                  buf.append("'").append(ConstantsIF.HEADERS).append("' object does not have a '")
                     .append(ConstantsIF.LOCATION).append("' attribute");
                  throw new Exception(buf.toString());
               }

               url = new URL(location);
               queryParams = this.getQueryParams(url);

               if (queryParams.containsKey(ConstantsIF.CODE)) {
                  code = queryParams.get(ConstantsIF.CODE);
                  if (!STR.isEmpty(code)) {
                     jsonInput = new JSONObject();
                     jsonInput.put(ConstantsIF.CODE, code);
                     operOutput.setJSON(jsonInput);
                     operOutput.setState(STATE.SUCCESS);
                     operOutput.setStatus("Obtained authorization code");
                  } else {
                     buf.append("Code value is null or empty");
                     throw new Exception(buf.toString());
                  }
               } else {
                  buf.append("Missing '").append(ConstantsIF.CODE).append("' query parameter");
                  if (queryParams.containsKey(ConstantsIF.ERROR)) {
                     buf.append(", error: ").append(queryParams.get(ConstantsIF.ERROR));
                  }
                  if (queryParams.containsKey(ConstantsIF.ERROR_DESCRIPTION)) {
                     buf.append(", description: ").append(queryParams.get(ConstantsIF.ERROR_DESCRIPTION));
                  }
                  throw new Exception(buf.toString());
               }
            } else {
               buf.append(ConstantsIF.HEADERS).append("' object is null or not JSON");
               throw new Exception(buf.toString());
            }
         } else {
            buf.append("Expected a 302 'Found' response.\n");
            buf.append(operASOutput.getStatus());
            throw new Exception(buf.toString());
         }
      } else {
         buf.append("JSON attribute '").append(ConstantsIF.SSO_TOKEN).append("' is empty");
         throw new Exception(buf.toString());
      }

      if (_logger.isLoggable(DEBUG_LEVEL)) {
         _logger.log(DEBUG_LEVEL, "output=''{0}''",
            new Object[]{operOutput != null ? operOutput.toString() : NULL});
      }

      _logger.exiting(CLASS, METHOD);

      return operOutput;
   }

   /**
    * Get OAuth 2.0 Access Token using an Authorization Code, from the
    * Authorization Server (AS).
    *
    * <pre>
    * JSON input ...
    * {
    *   "code": "..."
    * }
    *
    * JSON output ...
    * {
    *   "data": {
    *     "access_token": "057ad16f-7dba-4049-9f34-e609d230d43a",
    *     "refresh_token": "340f82a4-9aa9-471c-ac42-f0ca1809c82b",
    *     "scope": "uma_protection",
    *     "token_type": "Bearer",
    *     "expires_in": 4999
    *   }
    * }
    *
    * curl --request POST
    * --header "accept-api-version: resource=2.0,protocol=1.0"
    * --header "Content-Type: application/x-www-form-urlencoded"
    * --data "grant_type=authorization_code&
    *         code=<code>&
    *         redirect_uri=${REDURI}&
    *         client_id=UMA-Resource-Server&
    *         client_secret=UMA-2.0"
    * https://.../openam/oauth2/realms/root/access_token
    *
    * Build JSON "input" for getting the access token
    * {
    *   "form": {
    *     "grant_type": "authorization_code",
    *     "code": "...",
    *     "redirect_uri": "...",
    *     "client_id": "...",
    *     "client_secret": "..."
    *   }
    *   "headers": {
    *     "accept-api-version": "resource=2.0,protocol=1.0",
    *     "content-type": "application/x-www-form-urlencoded"
    *   },
    *   "path": "oauth2/realms/root/access_token"
    * }
    * Response is a 200 OK
    * {
    *   "access_token": "057ad16f-7dba-4049-9f34-e609d230d43a",
    *   "refresh_token": "340f82a4-9aa9-471c-ac42-f0ca1809c82b",
    *   "scope": "uma_protection",
    *   "token_type": "Bearer",
    *   "expires_in": 4999
    * }
    *
    * </pre>
    *
    * @param operInput OperationIF input
    * @return OperationIF output
    * @throws Exception
    */
   private OperationIF getAccessToken(final OperationIF operInput) throws Exception {
      String METHOD = Thread.currentThread().getStackTrace()[1].getMethodName();
      String code = null;
      String configType = ConstantsIF.RESOURCE;
      StringBuilder buf = new StringBuilder(METHOD + ": ");
      OperationIF operOutput = null;
      OperationIF operASInput = null;
      OperationIF operASOutput = null;
      JSONObject jsonInput = null;
      JSONObject jsonOutput = null;
      JSONObject jsonForm = null;
      JSONObject jsonHeaders = null;
      JSONObject jsonData = null;

      _logger.entering(CLASS, METHOD);

      if (_logger.isLoggable(DEBUG_LEVEL)) {
         _logger.log(DEBUG_LEVEL, "input=''{0}''",
            new Object[]{operInput != null ? operInput.toString() : NULL});
      }

      operOutput = new Operation(OperationIF.TYPE.CREATE);

      jsonInput = operInput.getJSON();

      code = JSON.getString(jsonInput, ConstantsIF.CODE);

      if (!STR.isEmpty(code)) {
         jsonForm = new JSONObject();
         jsonForm.put(ConstantsIF.GRANT_TYPE, ConstantsIF.AUTHORIZATION_CODE);
         jsonForm.put(ConstantsIF.CODE, code);
         jsonForm.put(ConstantsIF.REDIRECT_URI, this.getConfigValue(configType, ConfigIF.RS_OAUTH2_CLIENT_REDIRECT));
         jsonForm.put(ConstantsIF.CLIENT_ID, this.getConfigValue(configType, ConfigIF.RS_OAUTH2_CLIENT_ID));
         jsonForm.put(ConstantsIF.CLIENT_SECRET, this.getConfigValue(configType, ConfigIF.RS_OAUTH2_CLIENT_SECRET));

         jsonHeaders = new JSONObject();
         jsonHeaders.put(ConstantsIF.ACCEPT_API_VERSION, this.getConfigValue(configType, ConfigIF.AS_OAUTH2_AUTHORIZE_ACCEPT));
         jsonHeaders.put(ConstantsIF.CONTENT_TYPE, ConstantsIF.APPLICATION_FORM_URLENCODED);

         jsonInput = new JSONObject();
         jsonInput.put(ConstantsIF.FORM, jsonForm);
         jsonInput.put(ConstantsIF.HEADERS, jsonHeaders);
         jsonInput.put(ConstantsIF.PATH, this.getConfigValue(configType, ConfigIF.AS_OAUTH2_ACCESS_TOKEN_PATH));

         operASInput = new Operation(OperationIF.TYPE.CREATE); // POST
         operASInput.setJSON(jsonInput);

         operASOutput = _AuthzServerDAO.execute(operASInput); // get access token

         if (operASOutput.getState() == STATE.SUCCESS) {
            jsonData = operASOutput.getJSON();

            if (jsonData != null && !jsonData.isEmpty()) {
               jsonOutput = new JSONObject();
               jsonOutput.put(ConstantsIF.DATA, jsonData);

               operOutput.setJSON(jsonOutput);
               operOutput.setState(STATE.SUCCESS);
               operOutput.setStatus("Obtained access token");
            } else {
               buf.append("JSON output is null or empty");
               throw new Exception(buf.toString());
            }
         } else {
            buf.append("Authz Server DAO: ").append(operASOutput.getState().toString()).append(": ")
               .append(operASOutput.getStatus());
            throw new Exception(buf.toString());
         }
      } else {
         buf.append("Attribute '").append(ConstantsIF.CODE).append("' is null or empty");
         throw new Exception(buf.toString());
      }

      if (_logger.isLoggable(DEBUG_LEVEL)) {
         _logger.log(DEBUG_LEVEL, "output=''{0}''",
            new Object[]{operOutput != null ? operOutput.toString() : NULL});
      }

      _logger.exiting(CLASS, METHOD);

      return operOutput;
   }

   /**
    * Validate the OAuth 2.0 access token from the Authorization Server.
    *
    * <pre>
    * JSON input ...
    * {
    *   "data": {
    *     "owner": "",
    *     "category" : "uma_pat",
    *     "credential": {
    *       "grant_type": "authorization_code",
    *       "access_token": "...",
    *       "refresh_token": "...",
    *       "scope": "uma_protection",
    *       "token_type": "Bearer",
    *       "expires_in": "..."
    *     }
    *   }
    * }
    * curl example:
    * curl --request GET \
    * --header "Authorization Bearer <<access_token>>" \
    * "https://.../openam/oauth2/realms/root/tokeninfo"
    *
    * Build JSON "input" for access token validation
    * {
    *   "headers": {
    *     "authorization": "Bearer <<access_token>>"
    *   },
    *   "path": "oauth2/realms/root/tokeninfo"
    * }
    * Response is a 200 OK
    * {
    *   "mail": "demo@example.com",
    *   "grant_type": "...",
    *   "scope": [ "uma_protection" ],
    *   "cn": "demo",
    *   "realm": "/",
    *   ...,
    *  "token_type": "Bearer",
    *  "expires_in": 577,
    *  "client_id": "MyClientID",
    *  "access_token": "f9063e26-3a29-41ec-86de-1d0d68aa85e9"
    * }
    * Response is a 401 UNAUTHORIZED
    * {
    *   "error_description": "The access token provided is ...",
    *   "error": "invalid_token"
    * }
    *
    * </pre>
    *
    * @param operInput OperationIF input
    * @return OperationIF output
    * @throws Exception
    */
   private OperationIF validateToken(final OperationIF operInput) throws Exception {
      String METHOD = Thread.currentThread().getStackTrace()[1].getMethodName();
      StringBuilder buf = new StringBuilder(METHOD + ": ");
      String access_token = null;
      String configType = ConstantsIF.RESOURCE;
      JSONObject jsonHeaders = null;
      JSONObject jsonInput = null;
      OperationIF operOutput = null;
      OperationIF operASInput = null;
      OperationIF operASOutput = null;

      _logger.entering(CLASS, METHOD);

      if (_logger.isLoggable(DEBUG_LEVEL)) {
         _logger.log(DEBUG_LEVEL, "input=''{0}''",
            new Object[]{operInput != null ? operInput.toString() : NULL});
      }

      access_token = JSON.getString(operInput.getJSON(),
         ConstantsIF.DATA + "." + ConstantsIF.CREDENTIAL + "." + ConstantsIF.ACCESS_TOKEN);

      if (STR.isEmpty(access_token)) {
         buf.append("JSON input has en empty '" + ConstantsIF.ACCESS_TOKEN + "' value");
         throw new Exception(buf.toString());
      }

      jsonHeaders = new JSONObject();
      jsonHeaders.put(ConstantsIF.AUTHORIZATION, "Bearer " + access_token);

      jsonInput = new JSONObject();
      jsonInput.put(ConstantsIF.HEADERS, jsonHeaders);
      jsonInput.put(ConstantsIF.PATH, this.getConfigValue(configType, ConfigIF.AS_OAUTH2_TOKENINFO_PATH));

      operASInput = new Operation(OperationIF.TYPE.READ); // GET
      operASInput.setJSON(jsonInput);

      operASOutput = _AuthzServerDAO.execute(operASInput); // validate

      operOutput = operASOutput;

      if (_logger.isLoggable(DEBUG_LEVEL)) {
         _logger.log(DEBUG_LEVEL, "output=''{0}''",
            new Object[]{operOutput != null ? operOutput.toString() : NULL});
      }

      _logger.exiting(CLASS, METHOD);

      return operOutput;
   }

   /**
    * Refresh the token, using the refresh_token, from the Authorization Server
    * (AS)
    *
    * <pre>
    * REST input ...
    * {
    *   "data": {
    *     "owner": "",
    *     "category" : "uma_pat",
    *     "credential": {
    *       "grant_type": "authorization_code",
    *       "access_token": "...",
    *       "refresh_token": "...",
    *       "scope": "uma_protection",
    *       "token_type": "Bearer",
    *       "expires_in": "..."
    *     }
    *   }
    * }
    * JSON output ...
    * {
    *   "data": {
    *     "access_token": "057ad16f-7dba-4049-9f34-e609d230d43a",
    *     "refresh_token": "340f82a4-9aa9-471c-ac42-f0ca1809c82b",
    *     "scope": "uma_protection",
    *     "token_type": "Bearer",
    *     "expires_in": 4999
    *   }
    * }
    * curl example:
    * curl -s -k --request POST \
    * --data "\
    *         grant_type=refresh_token&\
    *         redirect_uri=${URI}&\
    *         refresh_token=${RT}&\
    *         client_id=UMA-Resource-Server&\
    *         client_secret=UMA-2.0&\
    *         scope=uma_protection" \
    * https://.../openam/oauth2/realms/root/access_token
    *
    * Build JSON "input" for getting the access token
    * {
    *   "form": {
    *     "grant_type": "refresh_token",
    *     "redirect_uri": "...",
    *     "refresh_token": "...",
    *     "client_id": "...",
    *     "client_secret": "...",
    *     "scope": "uma_protection"
    *   }
    *   "headers": {
    *     "accept-api-version": "resource=2.0,protocol=1.0",
    *     "content-type": "application/x-www-form-urlencoded"
    *   },
    *   "path": "oauth2/realms/root/access_token"
    * }
    * Response is a 200 OK
    * {
    *   "access_token": "057ad16f-7dba-4049-9f34-e609d230d43a",
    *   "refresh_token": "340f82a4-9aa9-471c-ac42-f0ca1809c82b",
    *   "scope": "uma_protection",
    *   "token_type": "Bearer",
    *   "expires_in": 4999
    * }
    * </pre>
    *
    * @param operInput OperationIF input
    * @return OperationIF output
    * @throws Exception
    */
   private OperationIF refreshToken(final OperationIF operInput) throws Exception {
      String METHOD = Thread.currentThread().getStackTrace()[1].getMethodName();
      String refreshToken = null;
      String configType = ConstantsIF.RESOURCE;
      StringBuilder buf = new StringBuilder(METHOD + ": ");
      OperationIF operOutput = null;
      OperationIF operASInput = null;
      OperationIF operASOutput = null;
      JSONObject jsonForm = null;
      JSONObject jsonHeaders = null;
      JSONObject jsonInput = null;

      _logger.entering(CLASS, METHOD);

      if (_logger.isLoggable(DEBUG_LEVEL)) {
         _logger.log(DEBUG_LEVEL, "input=''{0}''", new Object[]{operInput != null ? operInput.toString() : NULL});
      }

      operOutput = new Operation(OperationIF.TYPE.CREATE);

      refreshToken = JSON.getString(operInput.getJSON(),
         ConstantsIF.DATA + "." + ConstantsIF.CREDENTIAL + "." + ConstantsIF.REFRESH_TOKEN);

      if (!STR.isEmpty(refreshToken)) {
         jsonForm = new JSONObject();
         jsonForm.put(ConstantsIF.GRANT_TYPE, ConstantsIF.REFRESH_TOKEN);
         jsonForm.put(ConstantsIF.REDIRECT_URI, this.getConfigValue(configType, ConfigIF.RS_OAUTH2_CLIENT_REDIRECT));
         jsonForm.put(ConstantsIF.REFRESH_TOKEN, refreshToken);
         jsonForm.put(ConstantsIF.CLIENT_ID, this.getConfigValue(configType, ConfigIF.RS_OAUTH2_CLIENT_ID));
         jsonForm.put(ConstantsIF.CLIENT_SECRET, this.getConfigValue(configType, ConfigIF.RS_OAUTH2_CLIENT_SECRET));
         jsonForm.put(ConstantsIF.SCOPE, ConstantsIF.UMA_PROTECTION);

         jsonHeaders = new JSONObject();
         jsonHeaders.put(ConstantsIF.ACCEPT_API_VERSION, this.getConfigValue(configType, ConfigIF.AS_OAUTH2_ACCESS_TOKEN_ACCEPT));
         jsonHeaders.put(ConstantsIF.CONTENT_TYPE, ConstantsIF.APPLICATION_FORM_URLENCODED);

         jsonInput = new JSONObject();
         jsonInput.put(ConstantsIF.FORM, jsonForm);
         jsonInput.put(ConstantsIF.HEADERS, jsonHeaders);
         jsonInput.put(ConstantsIF.PATH, this.getConfigValue(configType, ConfigIF.AS_OAUTH2_ACCESS_TOKEN_PATH));

         operASInput = new Operation(OperationIF.TYPE.CREATE); // POST
         operASInput.setJSON(jsonInput);

         operASOutput = _AuthzServerDAO.execute(operASInput); // get access token

         operOutput = operASOutput;
      } else {
         buf.append("Attribute '").append(ConstantsIF.REFRESH_TOKEN).append("' is null or empty");
         throw new Exception(buf.toString());
      }

      if (_logger.isLoggable(DEBUG_LEVEL)) {
         _logger.log(DEBUG_LEVEL, "output=''{0}''", new Object[]{operOutput != null ? operOutput.toString() : NULL});
      }

      _logger.exiting(CLASS, METHOD);

      return operOutput;
   }

   /**
    * Get query parameters from a URL
    *
    * @param url URL
    * @return Map<String, String> Map of query parameter key / value
    * @throws UnsupportedEncodingException
    */
   private Map<String, String> getQueryParams(URL url) throws UnsupportedEncodingException {
      int index = 0;
      String METHOD = Thread.currentThread().getStackTrace()[1].getMethodName();
      String query = url.getQuery();
      String[] pairs = query.split("&");
      Map<String, String> query_pairs = new LinkedHashMap<>();

      _logger.entering(CLASS, METHOD);

      for (String pair : pairs) {
         index = pair.indexOf("=");
         query_pairs.put(URLDecoder.decode(pair.substring(0, index), "UTF-8"),
            URLDecoder.decode(pair.substring(index + 1), "UTF-8"));
      }

      _logger.exiting(CLASS, METHOD);

      return query_pairs;
   }
}
