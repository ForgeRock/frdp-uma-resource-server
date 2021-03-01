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
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

/**
 * Requests Handler. Provides the processing of access requests for a UMA
 * resource. Request processing is NOT part of the UMA 2.0 specification. The
 * class provides value-added services which leverage the Access Manager request
 * APIs.
 * <pre>
 * This class implements the following operations:
 * - search: find all requests for a given Resource owner
 * - read: get the details for a given request
 * - replace: set the requests action (allow / deny) and the scopes
 * </pre>
 *
 * @author Scott Fehrman, ForgeRock, Inc.
 */
public class RequestsHandler extends JaxrsHandler {

   private final String CLASS = this.getClass().getName();

   /**
    * Constructor
    *
    * @param configMgr ConfigurationManagerIF management of configurations
    * @param handlerMgr HandlerManagerIF provides management of Handlers
    */
   public RequestsHandler(final ConfigurationManagerIF configMgr, 
      final HandlerManagerIF handlerMgr) {
      super(configMgr, handlerMgr);

      String METHOD = "RequestsHandler(configMgr, handlerMgr)";

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
         case SEARCH: // GET
         case READ: // GET with id
         case REPLACE: // will map to a POST (approve or deny)
         {
            break;
         }
         default: {
            throw new Exception(METHOD 
               + ": Unsupported operation type: '" 
               + oper.getType().toString() + "'");
         }
      }

      _logger.exiting(CLASS, METHOD);

      return;
   }

   /**
    * Override interface to support the "search" operation Get the collection of
    * pending access requests for the give resource owner
    *
    * <pre>
    * JSON input ... search for pending requests
    * {
    *   "sso_token": "...", // header: iPlanetDirectoryPro
    *   "owner": "..." // used to build the URL
    * }
    * JSON output ...
    * {
    *   "data": {
    *     "result”:[
    *       {
    *         "_id":"e0841875-512b-4955-bcf0-74adc1bea9df2”,
    *         "user":"aadams”,
    *         "resource":"Lab Report”,
    *         "when":1544109770039,
    *         "permissions":["meta”]
    *       }
    *     ],
    *     "resultCount":1,
    *     "pagedResultsCookie":null,
    *     "totalPagedResultsPolicy":"NONE”,
    *     "totalPagedResults":-1,
    *     "remainingPagedResults”:0
    *   }
    * }
    * </pre>
    *
    * @param operInput OperationIF input for search operation
    * @return OperationIF output from search operation
    */
   @Override
   protected OperationIF search(OperationIF operInput) {
      String METHOD = Thread.currentThread().getStackTrace()[1].getMethodName();
      OperationIF operOutput = null;

      _logger.entering(CLASS, METHOD);

      if (_logger.isLoggable(DEBUG_LEVEL)) {
         _logger.log(DEBUG_LEVEL, "input=''{0}'', json=''{1}''",
            new Object[]{operInput != null ? operInput.toString() : NULL,
               operInput.getJSON() != null ? operInput.getJSON().toString() : NULL});
      }

      try {
         operOutput = this.searchImpl(operInput);
      } catch (Exception ex) {
         operOutput = new Operation(OperationIF.TYPE.SEARCH);
         operOutput.setError(true);
         operOutput.setState(STATE.ERROR);
         operOutput.setStatus(ex.getMessage());
      }

      _logger.exiting(CLASS, METHOD);

      return operOutput;
   }

   /**
    * Override interface to support the "read" operation Get details related to
    * an access request for the given resource owner
    *
    * <pre>
    * JSON input ... read request details
    * {
    *   "uid": "...", // Request GUID (was generated by AM)
    *   "sso_token": "...", // header: iPlanetDirectoryPro
    *   "owner": "..." // used to build the URL
    * }
    * JSON output ...
    * {
    *   "data": {
    *     "_id":"b68c5f1e-3f74-4def-b076-2a0f9b0422b33”, // Request GUID
    *     "_rev":"941699340”,
    *     "user":"aadams”, // the Requestng Party (RqP)
    *     "resource":"Lab Report”, // the UMA Resource Name
    *     "when":1544128568976,
    *     "permissions":["meta”]
    *   }
    * }
    * </pre>
    *
    * @param operInput OperationIF input for read operation
    * @return OperationIF output from read operation
    */
   @Override
   protected OperationIF read(OperationIF operInput) {
      String METHOD = Thread.currentThread().getStackTrace()[1].getMethodName();
      OperationIF operOutput = null;

      _logger.entering(CLASS, METHOD);

      if (_logger.isLoggable(DEBUG_LEVEL)) {
         _logger.log(DEBUG_LEVEL, "input=''{0}'', json=''{1}''",
            new Object[]{operInput != null ? operInput.toString() : NULL,
               operInput.getJSON() != null ? operInput.getJSON().toString() : NULL});
      }

      try {
         operOutput = this.readImpl(operInput);
      } catch (Exception ex) {
         operOutput = new Operation(OperationIF.TYPE.READ);
         operOutput.setError(true);
         operOutput.setState(STATE.ERROR);
         operOutput.setStatus(ex.getMessage());
      }

      _logger.exiting(CLASS, METHOD);

      return operOutput;
   }

   /**
    * Override interface to support the "replace" operation The POST HTTP Method
    * is used by the AM API to approve or deny requests for access to a
    * resource, from a requesting party.
    *
    * <pre>
    * JSON input ...
    * {
    *   "uid": "...", // Request GUID (was generated by AM)
    *   "sso_token": "...", // header: iPlanetDirectoryPro
    *   "owner": "...", // used to build the URL
    *   "data": {
    *     "action": "deny|approve",
    *     "permissions": [ "meta", "content" ]
    *   }
    * }
    * JSON output ...
    * {}
    * </pre>
    *
    * @param operInput OperationIF input for replace operation
    * @return OperationIF output from replace operation
    */
   @Override
   protected OperationIF replace(OperationIF operInput) {
      String METHOD = Thread.currentThread().getStackTrace()[1].getMethodName();
      OperationIF operOutput = null;

      _logger.entering(CLASS, METHOD);

      if (_logger.isLoggable(DEBUG_LEVEL)) {
         _logger.log(DEBUG_LEVEL, "input=''{0}'', json=''{1}''",
            new Object[]{operInput != null ? operInput.toString() : NULL,
               operInput.getJSON() != null ? operInput.getJSON().toString() : NULL});
      }

      try {
         operOutput = this.replaceImpl(operInput);
      } catch (Exception ex) {
         operOutput = new Operation(OperationIF.TYPE.REPLACE);
         operOutput.setError(true);
         operOutput.setState(STATE.ERROR);
         operOutput.setStatus(ex.getMessage());
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
      if (_AuthzServerDAO == null) {
         map = JSON.convertToParams(JSON.getObject(json, ConfigIF.AS_CONNECT));
         try {
            _AuthzServerDAO = new AMRestDataAccess(map);
         } catch (Exception ex) {
            msg = CLASS + ": " + METHOD + ": REST AMDAO: " + ex.getMessage();
            this.setError(true);
         }
      }

      if (!this.isError()) {
         this.setState(STATE.READY);
      } else {
         this.setState(STATE.ERROR);
         this.setStatus(msg);
         _logger.severe(this.getStatus());
      }

      _logger.exiting(CLASS, METHOD);

      return;
   }

   /**
    * Implementation of the "search" operation. Get the "pending" access
    * requests for the owner
    *
    * <pre>
    * JSON input ...
    * {
    *   "sso_token": "...", // header: iPlanetDirectoryPro
    *   "owner": "..." // used to build the URL
    * }
    * JSON output ...
    * {
    *   "data": {
    *     "result”:[
    *       {
    *         "_id":"e0841875-512b-4955-bcf0-74adc1bea9df2”,
    *         "user":"aadams”,
    *         "resource":"Lab Report”,
    *         "when":1544109770039,
    *         "permissions":["meta”]
    *       }
    *     ],
    *     "resultCount":1,
    *     "pagedResultsCookie":null,
    *     "totalPagedResultsPolicy":"NONE”,
    *     "totalPagedResults":-1,
    *     "remainingPagedResults”:0
    *   }
    * }
    *
    * curl example:
    * curl -X GET
    * 'https://.../openam/json/realms/root/users/<<owner>>/uma/pendingrequests\
    * ?_pageSize=10&_sortKeys=user&_queryFilter=true&_pagedResultsOffset=0’
    * -H 'Accept-API-Version: protocol=1.0,resource=1.0’
    * -H 'Cookie: iPlanetDirectoryPro=...*’
    * </pre>
    *
    * @param operInput OperationIF input
    * @return OperationIF output
    * @throws Exception
    */
   private OperationIF searchImpl(final OperationIF operInput) throws Exception {
      String METHOD = Thread.currentThread().getStackTrace()[1].getMethodName();
      String sso_token = null;
      String owner = null;
      String configType = ConstantsIF.RESOURCE;
      JSONObject jsonData = null;
      JSONObject jsonQuery = null;
      JSONObject jsonHeaders = null;
      JSONObject jsonParams = null;
      JSONObject jsonSearch = null;
      JSONObject jsonOutput = null;
      OperationIF operOutput = null;
      OperationIF operASInput = null;
      OperationIF operASOutput = null;

      _logger.entering(CLASS, METHOD);

      operOutput = new Operation(operInput.getType());

      sso_token = JSON.getString(operInput.getJSON(), ConstantsIF.SSO_TOKEN);
      owner = JSON.getString(operInput.getJSON(), ConstantsIF.OWNER);

      if (!STR.isEmpty(sso_token) && !STR.isEmpty(owner)) {
         jsonQuery = new JSONObject(); // SEARCH Operations require a "query" object
         jsonQuery.put(ConstantsIF.OPERATOR, ConstantsIF.NONE);

         jsonHeaders = new JSONObject();
         
         jsonHeaders.put(ConstantsIF.HDR_ACCEPT_API_VERSION, 
            this.getConfigValue(configType, ConfigIF.AS_UMA_PENDINGREQUESTS_ACCEPT));
         
         jsonHeaders.put(this.getConfigValue(configType, 
            ConfigIF.AS_COOKIE), sso_token);

         jsonParams = new JSONObject();
         
         jsonParams.put(PROP_SORTKEYS, this.getConfigValue(configType, 
            ConfigIF.AS_UMA_PENDINGREQUESTS_SORTKEYS));
         
         jsonParams.put(PROP_QUERYFILTER, this.getConfigValue(configType, 
            ConfigIF.AS_UMA_PENDINGREQUESTS_QUERYFILTER));

         jsonSearch = new JSONObject();
         jsonSearch.put(ConstantsIF.QUERY, jsonQuery);
         jsonSearch.put(ConstantsIF.HEADERS, jsonHeaders);
         jsonSearch.put(ConstantsIF.QUERY_PARAMS, jsonParams);
         jsonSearch.put(ConstantsIF.PATH,
            this.getConfigValue(configType, ConfigIF.AS_UMA_PENDINGREQUESTS_PATH)
               .replaceAll(PROP_VAR_OWNER, owner));

         operASInput = new Operation(OperationIF.TYPE.SEARCH); // GET
         operASInput.setJSON(jsonSearch);

         operASOutput = _AuthzServerDAO.execute(operASInput);

         if (operASOutput.getState() == STATE.NOTEXIST) // 404 NOT FOUND
         {
            jsonData = new JSONObject();
            jsonData.put(ConstantsIF.RESULTS, new JSONArray());
         } else {
            jsonData = operASOutput.getJSON();
         }

         jsonOutput = new JSONObject();
         jsonOutput.put(ConstantsIF.DATA, jsonData);

         operOutput.setState(operASOutput.getState());
         operOutput.setStatus(operASOutput.getStatus());
         operOutput.setJSON(jsonOutput);
      } else {
         throw new Exception(METHOD + ": sso_token or owner is empty");
      }

      _logger.exiting(CLASS, METHOD);

      return operOutput;
   }

   /**
    * Implementation of the "read" operation. Get details related to an access
    * request for the given resource owner
    *
    * <pre>
    * JSON input ... read request details
    * {
    *   "uid": "...", // Request GUID (was generated by AM)
    *   "sso_token": "...", // header: iPlanetDirectoryPro
    *   "owner": "..." // used to build the URL
    * }
    * JSON output ...
    * {
    *   "data": {
    *     "_id":"b68c5f1e-3f74-4def-b076-2a0f9b0422b33”, // Request GUID
    *     "_rev":"941699340”,
    *     "user":"aadams”, // the Requestng Party (RqP)
    *     "resource":"Lab Report”, // the UMA Resource Name
    *     "when":1544128568976,
    *     "permissions":["meta”]
    *   }
    * }
    *
    * curl example:
    * curl -X GET
    * 'https://.../openam/json/realms/root/users/<<owner>>/uma/pendingrequests/<<id>>’
    * -H 'Accept-API-Version: protocol=1.0,resource=1.0’
    * -H 'Cookie: iPlanetDirectoryPro =...'
    * </pre>
    *
    * @param jsonInput JSONObject input
    * @return JSONObject output
    * @throws Exception
    */
   private OperationIF readImpl(final OperationIF operInput) throws Exception {
      String METHOD = Thread.currentThread().getStackTrace()[1].getMethodName();
      String sso_token = null;
      String owner = null;
      String requestId = null;
      String configType = ConstantsIF.RESOURCE;
      JSONObject jsonHeaders = null;
      JSONObject jsonRead = null;
      JSONObject jsonOutput = null;
      OperationIF operASInput = null;
      OperationIF operASOutput = null;
      OperationIF operOutput = null;

      _logger.entering(CLASS, METHOD);

      operOutput = new Operation(operInput.getType());

      sso_token = JSON.getString(operInput.getJSON(), ConstantsIF.SSO_TOKEN);
      owner = JSON.getString(operInput.getJSON(), ConstantsIF.OWNER);

      if (!STR.isEmpty(sso_token) && !STR.isEmpty(owner)) {
         requestId = JSON.getString(operInput.getJSON(), ConstantsIF.UID);

         if (!STR.isEmpty(requestId)) {
            jsonHeaders = new JSONObject();
            
            jsonHeaders.put(ConstantsIF.HDR_ACCEPT_API_VERSION,
               this.getConfigValue(configType, 
                  ConfigIF.AS_UMA_PENDINGREQUESTS_ACCEPT));
            
            jsonHeaders.put(this.getConfigValue(configType, ConfigIF.AS_COOKIE), 
               sso_token);

            jsonRead = new JSONObject();
            jsonRead.put(ConstantsIF.HEADERS, jsonHeaders);
            jsonRead.put(ConstantsIF.PATH,
               this.getConfigValue(configType, ConfigIF.AS_UMA_PENDINGREQUESTS_PATH)
                  .replaceAll(PROP_VAR_OWNER, owner));
            jsonRead.put(ConstantsIF.UID, requestId);

            operASInput = new Operation(OperationIF.TYPE.READ); // GET
            operASInput.setJSON(jsonRead);

            operASOutput = _AuthzServerDAO.execute(operASInput);

            jsonOutput = new JSONObject();
            jsonOutput.put(ConstantsIF.DATA, operASOutput.getJSON());

            operOutput.setState(operASOutput.getState());
            operOutput.setStatus(operASOutput.getStatus());
            operOutput.setJSON(jsonOutput);
         } else {
            throw new Exception(METHOD + ": requestId is empty");
         }
      } else {
         throw new Exception(METHOD + ": sso_token or owner is empty");
      }

      _logger.exiting(CLASS, METHOD);

      return operOutput;
   }

   /**
    * Implementation of the "replace" operation. Update (replace) an access
    * request, "deny" or "approve" for the given resource owner
    *
    * <pre>
    * JSON input ... read request details
    * {
    *   "uid": "...", // Request GUID (was generated by AM)
    *   "sso_token": "...", // header: iPlanetDirectoryPro
    *   "owner": "...", // used to build the URL
    *   "data": {
    *     "action": "deny|approve",
    *     "permissions": [ "meta", "content" ]
    *   }
    * }
    * JSON output ...
    * {}
    *
    * curl example:
    * curl -X POST
    * 'https://.../openam/json/realms/root/users/<<owner>>/uma/pendingrequests/<<requesrUid>>?_action=approve’
    * -H 'Accept-API-Version: protocol=1.0,resource=1.0’
    * -H 'Cookie: iPlanetDirectoryPro=...'
    * --data-binary '{"scopes":["meta"]}’
    * --compressed
    * </pre>
    *
    * @param jsonInput JSONObject input
    * @return OperationIF output
    * @throws Exception
    */
   private OperationIF replaceImpl(final OperationIF operInput) throws Exception {
      String METHOD = Thread.currentThread().getStackTrace()[1].getMethodName();
      String sso_token = null;
      String owner = null;
      String requestId = null;
      String action = null;
      String configType = ConstantsIF.RESOURCE;
      JSONObject jsonInput = null;
      JSONObject jsonDataInput = null;
      JSONObject jsonPayload = null;
      JSONObject jsonHeaders = null;
      JSONObject jsonQueryParams = null;
      JSONObject jsonReplace = null;
      JSONObject jsonOutput = null;
      JSONArray jsonScopes = null;
      JSONArray jsonPermissions = null;
      OperationIF operReadOutput = null;
      OperationIF operASInput = null;
      OperationIF operASOutput = null;
      OperationIF operOutput = null;

      _logger.entering(CLASS, METHOD);

      operOutput = new Operation(operInput.getType());

      jsonInput = operInput.getJSON();

      /*
       * Read the Request, need the "permissions" / "scopes"
       */
      operInput.setType(OperationIF.TYPE.READ);

      operReadOutput = this.readImpl(operInput);

      if (operReadOutput.getState() == STATE.SUCCESS) {
         jsonScopes = JSON.getArray(operReadOutput.getJSON(), 
            ConstantsIF.DATA + "." + ConstantsIF.PERMISSIONS);
      } else {
         throw new Exception(METHOD + ": Faild to read request");
      }

      /*
       * If the input JSON "data" contains a "permissions" array use the "permissions"
       * for the "scopes" else all registered resource scopes are allowed
       */
      sso_token = JSON.getString(jsonInput, ConstantsIF.SSO_TOKEN);
      owner = JSON.getString(jsonInput, ConstantsIF.OWNER);

      if (!STR.isEmpty(sso_token) && !STR.isEmpty(owner)) {
         requestId = JSON.getString(jsonInput, ConstantsIF.UID);

         if (!STR.isEmpty(requestId)) {
            jsonDataInput = JSON.getObject(jsonInput, ConstantsIF.DATA);

            if (jsonDataInput != null && !jsonDataInput.isEmpty()) {
               action = JSON.getString(jsonDataInput, ConstantsIF.ACTION);

               if (!STR.isEmpty(action)
                  && (action.equalsIgnoreCase(ConstantsIF.DENY) 
                  || action.equalsIgnoreCase(ConstantsIF.APPROVE))) {
                  
                  jsonPayload = new JSONObject();
                  
                  jsonPayload.put(ConstantsIF.ACTION, action);

                  if (action.equalsIgnoreCase(ConstantsIF.APPROVE)) {
                     jsonPermissions = JSON.getArray(jsonDataInput, 
                        ConstantsIF.PERMISSIONS);

                     if (jsonPermissions != null && !jsonPermissions.isEmpty()) {
                        jsonPayload.put(ConstantsIF.SCOPES, jsonPermissions);
                     } else {
                        if (_logger.isLoggable(Level.WARNING)) {
                           _logger.log(Level.WARNING,
                              "Approved access request is missing permissions, "
                              + "will grant all scopes");
                        }
                        if (jsonScopes != null && !jsonScopes.isEmpty()) {
                           jsonPayload.put(ConstantsIF.SCOPES, jsonScopes);
                        } else {
                           throw new Exception(METHOD 
                              + ": 'approve' action must have at least one scope");
                        }
                     }
                  }

                  jsonHeaders = new JSONObject();
                  jsonHeaders.put(ConstantsIF.HDR_ACCEPT_API_VERSION,
                     this.getConfigValue(configType, 
                        ConfigIF.AS_UMA_PENDINGREQUESTS_ACCEPT));
                  jsonHeaders.put(this.getConfigValue(configType, 
                     ConfigIF.AS_COOKIE), sso_token);

                  jsonQueryParams = new JSONObject();
                  jsonQueryParams.put("_action", action);

                  jsonReplace = new JSONObject();
                  jsonReplace.put(ConstantsIF.HEADERS, jsonHeaders);
                  jsonReplace.put(ConstantsIF.QUERY_PARAMS, jsonQueryParams);
                  jsonReplace.put(ConstantsIF.PATH,
                     this.getConfigValue(configType,
                        ConfigIF.AS_UMA_PENDINGREQUESTS_PATH)
                        .replaceAll(PROP_VAR_OWNER, owner));
                  jsonReplace.put(ConstantsIF.UID, requestId);
                  jsonReplace.put(ConstantsIF.DATA, jsonPayload);

                  operASInput = new Operation(OperationIF.TYPE.CREATE); // POST
                  operASInput.setJSON(jsonReplace);

                  operASOutput = _AuthzServerDAO.execute(operASInput);

                  jsonOutput = new JSONObject();
                  jsonOutput.put(ConstantsIF.DATA, operASOutput.getJSON());

                  operOutput.setState(operASOutput.getState());
                  operOutput.setStatus(operASOutput.getStatus());
                  operOutput.setJSON(jsonOutput);
               } else {
                  throw new Exception(METHOD 
                     + ": action must be 'approve' or 'deny'");
               }
            } else {
               throw new Exception(METHOD 
                  + ": JSON data is empty");
            }
         } else {
            throw new Exception(METHOD 
               + ": requestId is empty");
         }
      } else {
         throw new Exception(METHOD 
            + ": sso_token or owner is empty");
      }

      _logger.exiting(CLASS, METHOD);

      return operOutput;
   }
}
