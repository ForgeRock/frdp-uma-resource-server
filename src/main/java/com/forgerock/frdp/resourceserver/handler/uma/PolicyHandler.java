/*
 * Copyright (c) 2018-2020, ForgeRock, Inc., All rights reserved
 * Use subject to license terms.
 */
package com.forgerock.frdp.resourceserver.handler.uma;

import com.forgerock.frdp.common.ConstantsIF;
import com.forgerock.frdp.config.ConfigurationIF;
import com.forgerock.frdp.config.ConfigurationManagerIF;
import com.forgerock.frdp.dao.Operation;
import com.forgerock.frdp.dao.OperationIF;
import com.forgerock.frdp.handler.HandlerManagerIF;
import com.forgerock.frdp.resourceserver.ConfigIF;
import com.forgerock.frdp.resourceserver.dao.AMRestDataAccess;
import com.forgerock.frdp.resourceserver.handler.JaxrsHandler;
import com.forgerock.frdp.utils.JSON;
import com.forgerock.frdp.utils.STR;
import java.util.Map;
import java.util.logging.Level;
import org.json.simple.JSONObject;

/**
 * Policy Handler. Provides create, read, replace, and delete operations for a
 * policy which is associated to a registered UMA resource.
 *
 * @author Scott Fehrman, ForgeRock, Inc.
 */
public class PolicyHandler extends JaxrsHandler {

   private final String CLASS = this.getClass().getName();

   /**
    * Constructor
    *
    * @param configMgr ConfigurationManagerIF management of configurations
    * @param handlerMgr HandlerManagerIF provides management of Handlers
    */
   public PolicyHandler(final ConfigurationManagerIF configMgr, 
      final HandlerManagerIF handlerMgr) {
      super(configMgr, handlerMgr);

      String METHOD = "PolicyHandler(configMgr, handlerMgr)";

      _logger.entering(CLASS, METHOD);

      this.init();

      _logger.exiting(CLASS, METHOD);

      return;
   }

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
         throw new Exception(METHOD + ": Operation object is null");
      }

      jsonInput = oper.getJSON();
      if (jsonInput == null || jsonInput.isEmpty()) {
         throw new Exception(METHOD + ": JSON Input is null or empty");
      }

      switch (oper.getType()) {
         case READ:
         case REPLACE:
         case DELETE: {
            this.checkAttr(jsonInput, ConstantsIF.UID);
            break;
         }
         default:
            break;
      }

      _logger.exiting(CLASS, METHOD);

      return;
   }

   /**
    * Override interface to support the "create" operation
    *
    * <pre>
    * JSON input ...
    * {
    *   "uid" : "...", // register id
    *   "sso_token": "...",
    *   "owner": "...",
    *   "data": {
    *     "permissions" : [
    *       {
    *         "subject": "...",
    *         "scopes": ["view"]
    *       }
    *     ]
    *   }
    * }
    * JSON output ...
    * { "uid": "..." }
    * </pre>
    *
    * @param operInput OperationIF input for create operation
    * @return OperationIF output from create operation
    */
   @Override
   protected OperationIF create(final OperationIF operInput) {
      boolean error = false;
      String METHOD = Thread.currentThread().getStackTrace()[1].getMethodName();
      String msg = null;
      OperationIF operOutput = null;
      JSONObject jsonInput = null;

      _logger.entering(CLASS, METHOD);

      if (_logger.isLoggable(DEBUG_LEVEL)) {
         _logger.log(DEBUG_LEVEL, "input=''{0}'', json=''{1}''",
            new Object[]{
               operInput != null ? operInput.toString() : NULL,
               operInput.getJSON() != null ? operInput.getJSON().toString() : NULL
            });
      }

      operOutput = new Operation(operInput.getType());

      jsonInput = operInput.getJSON();

      try {
         operOutput = this.createImpl(jsonInput);
      } catch (Exception ex) {
         error = true;
         msg = ex.getMessage();
      }

      if (error) {
         operOutput.setError(true);
         operOutput.setState(STATE.ERROR);
         operOutput.setStatus(METHOD + ": " + msg);
      }

      _logger.exiting(CLASS, METHOD);

      return operOutput;
   }

   /**
    * Override interface to support the "read" operation
    *
    * <pre>
    * JSON input ... read policy
    * {
    *   "uid": "...", // Register GUID
    *   "sso_token": "...",
    *   "owner": "..."
    * }
    * JSON output ...
    * {
    *   "data": {
    *     ...
    *   }
    * }
    * </pre>
    *
    * @param operInput OperationIF input for read operation
    * @return OperationIF output from read operation
    */
   @Override
   protected OperationIF read(OperationIF operInput) {
      boolean error = false;
      String METHOD = Thread.currentThread().getStackTrace()[1].getMethodName();
      String msg = null;
      OperationIF operOutput = null;
      JSONObject jsonInput = null;
      JSONObject jsonOutput = null;
      JSONObject jsonWrapData = null;

      _logger.entering(CLASS, METHOD);

      if (_logger.isLoggable(DEBUG_LEVEL)) {
         _logger.log(DEBUG_LEVEL, "input=''{0}'', json=''{1}''",
            new Object[]{
               operInput != null ? operInput.toString() : NULL,
               operInput.getJSON() != null ? operInput.getJSON().toString() : NULL
            });
      }

      operOutput = new Operation(operInput.getType());

      jsonInput = operInput.getJSON();

      try {
         jsonOutput = this.readImpl(jsonInput);
      } catch (Exception ex) {
         error = true;
         msg = ex.getMessage();
      }

      if (!error) {
         if (jsonOutput == null) {
            operOutput.setState(STATE.NOTEXIST);
            operOutput.setStatus("Missing entry");
         } else {
            jsonWrapData = new JSONObject();
            jsonWrapData.put(ConstantsIF.DATA, jsonOutput);

            operOutput.setJSON(jsonWrapData);
            operOutput.setState(STATE.SUCCESS);
            operOutput.setStatus("Found entry");
         }
      } else {
         operOutput.setError(true);
         operOutput.setState(STATE.ERROR);
         operOutput.setStatus(msg);
      }

      _logger.exiting(CLASS, METHOD);

      return operOutput;
   }

   /**
    * Override interface to support the "replace" operation
    *
    * <pre>
    * JSON input ...
    * {
    *   "uid" : "...", // register id
    *   "sso_token": "...",
    *   "owner": "...",
    *   "data": {
    *     "permissions" : [
    *       {
    *         "subject": "...",
    *         "scopes": ["view"]
    *       }
    *     ]
    *   }
    * }
    * JSON output ...
    * { "uid": "..." }
    * </pre>
    *
    * @param operInput OperationIF input for replace operation
    * @return OperationIF output from replace operation
    */
   @Override
   protected OperationIF replace(final OperationIF operInput) {
      boolean error = false;
      String METHOD = Thread.currentThread().getStackTrace()[1].getMethodName();
      String msg = null;
      OperationIF operOutput = null;
      JSONObject jsonInput = null;

      _logger.entering(CLASS, METHOD);

      if (_logger.isLoggable(DEBUG_LEVEL)) {
         _logger.log(DEBUG_LEVEL, "input=''{0}'', json=''{1}''",
            new Object[]{
               operInput != null ? operInput.toString() : NULL,
               operInput.getJSON() != null ? operInput.getJSON().toString() : NULL
            });
      }

      operOutput = new Operation(operInput.getType());

      jsonInput = operInput.getJSON();

      try {
         operOutput = this.replaceImpl(jsonInput);
      } catch (Exception ex) {
         error = true;
         msg = ex.getMessage();
      }

      if (error) {
         operOutput.setError(true);
         operOutput.setState(STATE.ERROR);
         operOutput.setStatus(METHOD + ": " + msg);
      }

      _logger.exiting(CLASS, METHOD);

      return operOutput;
   }

   /**
    * Override interface to support the "delete" operation
    *
    * <pre>
    * JSON input ...
    * {
    *   "uid" : "...",
    *   "sso_token": "...",
    *   "owner": "..."
    * }
    * </pre>
    *
    * @param operInput OperationIF input for delete operation
    * @return OperationIF output from delete operation
    */
   @Override
   protected OperationIF delete(final OperationIF operInput) {
      boolean error = false;
      String METHOD = Thread.currentThread().getStackTrace()[1].getMethodName();
      String msg = null;
      OperationIF operOutput = null;
      JSONObject jsonInput = null;

      _logger.entering(CLASS, METHOD);

      if (_logger.isLoggable(DEBUG_LEVEL)) {
         _logger.log(DEBUG_LEVEL, "input=''{0}'', json=''{1}''",
            new Object[]{
               operInput != null ? operInput.toString() : NULL,
               operInput.getJSON() != null ? operInput.getJSON().toString() : NULL
            });
      }

      operOutput = new Operation(operInput.getType());

      jsonInput = operInput.getJSON();

      try {
         this.deleteImpl(jsonInput);
      } catch (Exception ex) {
         error = true;
         msg = ex.getMessage();
      }

      if (!error) {
         operOutput.setJSON(new JSONObject());
         operOutput.setState(STATE.SUCCESS);
         operOutput.setStatus("Deleted entry");
      } else {
         operOutput.setError(true);
         operOutput.setState(STATE.ERROR);
         operOutput.setStatus(msg);
      }

      _logger.exiting(CLASS, METHOD);

      return operOutput;
   }

   /**
    * Initialize object instance
    */
   private void init() {
      String METHOD = Thread.currentThread().getStackTrace()[1].getMethodName();
      String msg = null;
      String type = ConstantsIF.RESOURCE;
      ConfigurationIF config = null;
      JSONObject json = null;
      Map<String, String> map = null;

      _logger.entering(CLASS, METHOD);

      config = _configMgr.getConfiguration(type);

      if (config != null) {
         json = config.getJSON();
         if (json == null) {
            msg = CLASS + ": " + METHOD + ": JSON data for '" + type + "' is null";
            this.setError(true);
         }
      } else {
         msg = CLASS + ": " + METHOD + ": Configuration for '" + type + "' is null";
         this.setError(true);
      }

      /*
       * setup the REST Data Access Object for the Authorization Server (AS)
       */
      if (!this.isError() && _AuthzServerDAO == null) {
         map = JSON.convertToParams(JSON.getObject(json, ConfigIF.AS_CONNECT));
         try {
            _AuthzServerDAO = new AMRestDataAccess(map);
         } catch (Exception ex) {
            msg = CLASS + ": " + METHOD + ": REST DAO: " + ex.getMessage();
            this.setError(true);
         }
      }

      if (!this.isError()) {
         this.setState(STATE.READY);
      } else {
         this.setState(STATE.ERROR);
         this.setStatus(msg);
         _logger.log(Level.SEVERE, this.getStatus());
      }

      _logger.exiting(CLASS, METHOD);

      return;
   }

   /**
    * Implementation of the "read" operation. Get the policy data from the
    * Authorization Server
    *
    * <pre>
    * JSON input ... read policy
    * {
    *   "uid": "...",
    *   "sso_token": "...",
    *   "owner": "..."
    * }
    * JSON output ...
    * {
    *   ...
    * }
    * curl example:
    * curl -X GET \
    * -H "iPlanetDirectoryPro: <<ssoToken>>" \
    * https://.../openam/json/realms/root/users/<<owner>>/uma/policies/<<regId>>
    *
    * {
    *   "_id": "0d7790de-9066-4bb6-8e81-25b6f9d0b8853",
    *   "_rev": "1444644662",
    *   "policyId": "0d7790de-9066-4bb6-8e81-25b6f9d0b8853",
    *   "name": "Photo Album",
    *   "permissions": [
    *     {
    *       "subject": "bob",
    *       "scopes": ["view", "comment"]
    *     }
    *   ]
    * }
    * </pre>
    *
    * @param jsonInput JSONObject input
    * @return JSONObject output
    * @throws Exception
    */
   private JSONObject readImpl(final JSONObject jsonInput) throws Exception {
      String METHOD = Thread.currentThread().getStackTrace()[1].getMethodName();
      String sso_token = null;
      String registerId = null;
      String owner = null;
      String configType = ConstantsIF.RESOURCE;
      OperationIF operInput = null;
      OperationIF operOutput = null;
      JSONObject jsonOutput = null;
      JSONObject jsonHeaders = null;
      JSONObject jsonData = null;

      _logger.entering(CLASS, METHOD);

      owner = JSON.getString(jsonInput, ConstantsIF.OWNER);

      if (!STR.isEmpty(owner)) {
         sso_token = JSON.getString(jsonInput, ConstantsIF.SSO_TOKEN);

         if (!STR.isEmpty(sso_token)) {
            registerId = JSON.getString(jsonInput, ConstantsIF.UID);

            if (!STR.isEmpty(registerId)) {
               jsonHeaders = new JSONObject();
               
               jsonHeaders.put(
                  this.getConfigValue(configType, ConfigIF.AS_COOKIE), 
                  sso_token);

               jsonData = new JSONObject();
               
               jsonData.put(ConstantsIF.HEADERS, jsonHeaders);
               
               jsonData.put(ConstantsIF.UID, registerId);
               
               jsonData.put(ConstantsIF.PATH,
                  this.getConfigValue(configType, 
                     ConfigIF.AS_UMA_POLICIES_PATH).replaceAll(PROP_VAR_OWNER, 
                        owner));

               operInput = new Operation(OperationIF.TYPE.READ); // GET
               
               operInput.setJSON(jsonData);

               operOutput = _AuthzServerDAO.execute(operInput);

               if (operOutput.getState() == STATE.SUCCESS) {
                  jsonOutput = operOutput.getJSON();

                  if (jsonOutput == null || jsonOutput.isEmpty()) {
                     throw new Exception(METHOD 
                        + ": JSON output is empty: " + operOutput.getStatus());
                  }
               } else if (operOutput.getState() == STATE.NOTEXIST) {
                  jsonOutput = null; // TODO
               } else {
                  throw new Exception(METHOD 
                     + ": Could not read resource policy: " + operOutput.getStatus());
               }
            } else {
               throw new Exception(METHOD 
                  + ": registered resource id is empty");
            }
         } else {
            throw new Exception(METHOD 
               + ": access_token is empty");
         }
      } else {
         throw new Exception(METHOD 
            + ": owner is empty");
      }

      _logger.exiting(CLASS, METHOD);

      return jsonOutput;
   }

   /**
    * Implement the "create" operation.
    *
    * <pre>
    * JSON input ...
    * {
    *   "uid" : "...", // register id
    *   "sso_token": "...",
    *   "owner": "...",
    *   "data": {
    *     "permissions" : [
    *       {
    *         "subject": "...",
    *         "scopes": ["view"]
    *       }
    *     ]
    *   }
    * }
    * JSON output ...
    * { "uid": "..." }
    *
    * curl example: CREATE A NEW UMA RESOURCE POLICY
    * curl -X PUT \
    * -H 'Accept-API-Version: resource=1.0' \
    * -H 'cache-control: no-cache' \
    * -H 'content-type: application/json' \
    * -H "iPlanetDirectoryPro: <<ssoToken>>" \
    * -H "If-None-Match: *" \
    * --data '{
    *           "policyId": "<<regId>>",
    *           "permissions": [
    *             {
    *               "subject": "bob",
    *               "scopes": ["view"]
    *             }
    *           ]
    *         }' \
    * https://.../openam/json/users/alice/uma/policies/<<regId>>
    * </pre>
    *
    * @param jsonInput JSONObject input
    * @return OperationIF output
    * @throws Exception
    */
   private OperationIF createImpl(final JSONObject jsonInput) throws Exception {
      String METHOD = Thread.currentThread().getStackTrace()[1].getMethodName();
      String owner = null;
      String sso_token = null;
      String registerId = null;
      String configType = ConstantsIF.RESOURCE;
      JSONObject jsonHeaders = null;
      JSONObject jsonData = null;
      JSONObject jsonCreateInput = null;
      OperationIF operInput = null;
      OperationIF operOutput = null;

      _logger.entering(CLASS, METHOD);

      owner = JSON.getString(jsonInput, ConstantsIF.OWNER);

      if (!STR.isEmpty(owner)) {
         sso_token = JSON.getString(jsonInput, ConstantsIF.SSO_TOKEN);

         if (!STR.isEmpty(sso_token)) {
            registerId = JSON.getString(jsonInput, ConstantsIF.UID);

            if (!STR.isEmpty(registerId)) {
               jsonHeaders = new JSONObject();
               
               jsonHeaders.put(ConstantsIF.HDR_ACCEPT_API_VERSION, 
                  this.getConfigValue(configType, 
                     ConfigIF.AS_UMA_POLICIES_ACCEPT));
               
               jsonHeaders.put(ConstantsIF.HDR_CONTENT_TYPE, 
                  ConstantsIF.TYPE_JSON);
               
               jsonHeaders.put(this.getConfigValue(configType, 
                  ConfigIF.AS_COOKIE), sso_token);
               
               jsonHeaders.put("If-None-Match", "*"); // CREATE

               jsonData = JSON.getObject(jsonInput, ConstantsIF.DATA);
               
               jsonData.put(ConstantsIF.POLICYID, registerId);

               jsonCreateInput = new JSONObject();
               
               jsonCreateInput.put(ConstantsIF.DATA, jsonData);
               
               jsonCreateInput.put(ConstantsIF.HEADERS, jsonHeaders);
               
               jsonCreateInput.put(ConstantsIF.UID, registerId);
               
               jsonCreateInput.put(ConstantsIF.PATH,
                  this.getConfigValue(configType, ConfigIF.AS_UMA_POLICIES_PATH)
                     .replaceAll(PROP_VAR_OWNER, owner));

               operInput = new Operation(OperationIF.TYPE.REPLACE); // PUT
               
               operInput.setJSON(jsonCreateInput);

               operOutput = _AuthzServerDAO.execute(operInput);
            } else {
               throw new Exception(METHOD + ": registered resource id is empty");
            }
         } else {
            throw new Exception(METHOD + ": access_token is empty");
         }
      } else {
         throw new Exception(METHOD + ": owner is empty");
      }

      _logger.exiting(CLASS, METHOD);

      return operOutput;
   }

   /**
    * Implementation of the "replace" operation.
    *
    * <pre>
    * JSON input ...
    * {
    *   "uid" : "...", // register id
    *   "sso_token": "...",
    *   "owner": "...",
    *   "data": {
    *     "permissions" : [
    *       {
    *         "subject": "...",
    *         "scopes": ["view"]
    *       }
    *     ]
    *   }
    * }
    * JSON output ...
    * { "uid": "..." }
    *
    * curl example: UPDATE A NEW UMA RESOURCE POLICY
    * curl -X PUT \
    * -H 'Accept-API-Version: resource=1.0' \
    * -H 'cache-control: no-cache' \
    * -H 'content-type: application/json' \
    * -H "iPlanetDirectoryPro: <<ssoToken>>" \
    * -H "If-Match: *" \
    * --data '{
    *           "policyId": "<<regId>>",
    *           "permissions": [
    *             {
    *               "subject": "bob",
    *               "scopes": ["view"]
    *             }
    *           ]
    *         }' \
    * https://.../openam/json/users/alice/uma/policies/<<regId>>
    *
    * </pre>
    *
    * @param jsonInput JSONObject input
    * @return OperationIF output
    * @throws Exception
    */
   private OperationIF replaceImpl(final JSONObject jsonInput) throws Exception {
      String METHOD = Thread.currentThread().getStackTrace()[1].getMethodName();
      String owner = null;
      String sso_token = null;
      String registerId = null;
      String configType = ConstantsIF.RESOURCE;
      JSONObject jsonHeaders = null;
      JSONObject jsonData = null;
      JSONObject jsonCreateInput = null;
      OperationIF operInput = null;
      OperationIF operOutput = null;

      _logger.entering(CLASS, METHOD);

      owner = JSON.getString(jsonInput, ConstantsIF.OWNER);

      if (!STR.isEmpty(owner)) {
         sso_token = JSON.getString(jsonInput, ConstantsIF.SSO_TOKEN);

         if (!STR.isEmpty(sso_token)) {
            registerId = JSON.getString(jsonInput, ConstantsIF.UID);

            if (!STR.isEmpty(registerId)) {
               jsonHeaders = new JSONObject();
               
               jsonHeaders.put(ConstantsIF.HDR_ACCEPT_API_VERSION, 
                  this.getConfigValue(configType, 
                     ConfigIF.AS_UMA_POLICIES_ACCEPT));
               
               jsonHeaders.put(ConstantsIF.HDR_CONTENT_TYPE, 
                  ConstantsIF.TYPE_JSON);
               
               jsonHeaders.put(this.getConfigValue(configType, 
                  ConfigIF.AS_COOKIE), sso_token);
               
               jsonHeaders.put("If-Match", "*"); // UPDATE

               jsonData = JSON.getObject(jsonInput, ConstantsIF.DATA);
               
               jsonData.put(ConstantsIF.POLICYID, registerId);

               jsonCreateInput = new JSONObject();
               
               jsonCreateInput.put(ConstantsIF.DATA, jsonData);
               
               jsonCreateInput.put(ConstantsIF.HEADERS, jsonHeaders);
               
               jsonCreateInput.put(ConstantsIF.UID, registerId);
               
               jsonCreateInput.put(ConstantsIF.PATH,
                  this.getConfigValue(configType, ConfigIF.AS_UMA_POLICIES_PATH)
                     .replaceAll(PROP_VAR_OWNER, owner));

               operInput = new Operation(OperationIF.TYPE.REPLACE); // PUT
               
               operInput.setJSON(jsonCreateInput);

               operOutput = _AuthzServerDAO.execute(operInput);
            } else {
               throw new Exception(METHOD + ": registered resource id is empty");
            }
         } else {
            throw new Exception(METHOD + ": access_token is empty");
         }
      } else {
         throw new Exception(METHOD + ": owner is empty");
      }

      _logger.exiting(CLASS, METHOD);

      return operOutput;
   }

   /**
    * Implementation of the "delete" operation. Delete the policy data from the
    * Authorization Server
    *
    * <pre>
    * JSON input ... delete policy
    * {
    *   "uid": "...",
    *   "sso_token": "...",
    *   "owner": "..."
    * }
    *
    * curl example:
    * curl -X DELETE \
    * --header "iPlanetDirectoryPro: <<sso_token>>" \
    * --header "Accept-API-Version: resource=1.0" \
    * https://.../openam/json/realms/root/realms/root/users/demo/json/policies/<<regId>>
    * </pre>
    *
    * @param jsonInput JSONObject input
    * @throws Exception
    */
   private void deleteImpl(final JSONObject jsonInput) throws Exception {
      String METHOD = Thread.currentThread().getStackTrace()[1].getMethodName();
      String sso_token = null;
      String registerId = null;
      String owner = null;
      String configType = ConstantsIF.RESOURCE;
      OperationIF operInput = null;
      OperationIF operOutput = null;
      JSONObject jsonHeaders = null;
      JSONObject jsonData = null;

      _logger.entering(CLASS, METHOD);

      owner = JSON.getString(jsonInput, ConstantsIF.OWNER);

      if (!STR.isEmpty(owner)) {
         sso_token = JSON.getString(jsonInput, ConstantsIF.SSO_TOKEN);

         if (!STR.isEmpty(sso_token)) {
            registerId = JSON.getString(jsonInput, ConstantsIF.UID);

            if (!STR.isEmpty(registerId)) {
               jsonHeaders = new JSONObject();
               
               jsonHeaders.put(ConstantsIF.HDR_ACCEPT_API_VERSION, 
                  this.getConfigValue(configType, 
                     ConfigIF.AS_UMA_POLICIES_ACCEPT));
               
               jsonHeaders.put(this.getConfigValue(configType, 
                  ConfigIF.AS_COOKIE), sso_token);

               jsonData = new JSONObject();
               
               jsonData.put(ConstantsIF.HEADERS, jsonHeaders);
               
               jsonData.put(ConstantsIF.UID, registerId);
               
               jsonData.put(ConstantsIF.PATH,
                  this.getConfigValue(configType, ConfigIF.AS_UMA_POLICIES_PATH)
                     .replaceAll(PROP_VAR_OWNER, owner));

               operInput = new Operation(OperationIF.TYPE.DELETE); // DELETE
               
               operInput.setJSON(jsonData);

               operOutput = _AuthzServerDAO.execute(operInput);

               if (operOutput.getState() != STATE.SUCCESS 
                  && operOutput.getState() != STATE.NOTEXIST) {
                  throw new Exception(METHOD 
                     + ": Could not delete resource policy: " 
                     + operOutput.getStatus());
               }
            } else {
               throw new Exception(METHOD 
                  + ": registered resource id is empty");
            }
         } else {
            throw new Exception(METHOD 
               + ": access_token is empty");
         }
      } else {
         throw new Exception(METHOD 
            + ": owner is empty");
      }

      _logger.exiting(CLASS, METHOD);

      return;
   }
}
