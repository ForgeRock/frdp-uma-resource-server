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
import com.forgerock.frdp.dao.mongo.MongoFactory;
import com.forgerock.frdp.handler.HandlerManagerIF;
import com.forgerock.frdp.resourceserver.ConfigIF;
import com.forgerock.frdp.resourceserver.dao.AMRestDataAccess;
import com.forgerock.frdp.resourceserver.handler.JaxrsHandler;
import com.forgerock.frdp.utils.JSON;
import com.forgerock.frdp.utils.STR;
import java.util.HashMap;
import java.util.Map;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

/**
 * Subjects Handler. Provides a service to get a Subjects (Requesting Party)
 * list of resources that they have access to (via a policy). This service is
 * NOT part of the UMA 2.0 specification. This class provides value-added
 * services which leverage the Access Manager APIs.
 *
 * <pre>
 * This class implements the following operations:
 * - search: get a Subjects (Requesting Party) list of resources
 * </pre>
 *
 * @author Scott Fehrman, ForgeRock, Inc.
 */
public class SubjectsHandler extends JaxrsHandler {

   private final String CLASS = this.getClass().getName();

   /**
    * Constructor
    *
    * @param configMgr ConfigurationManagerIF management of configurations
    * @param handlerMgr HandlerManagerIF provides management of Handlers
    */
   public SubjectsHandler(final ConfigurationManagerIF configMgr, final HandlerManagerIF handlerMgr) {
      super(configMgr, handlerMgr);

      String METHOD = "SubjectsHandler(configMgr, handlerMgr)";

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

      switch (oper.getType()) {
         case SEARCH: // GET
         {
            break;
         }
         default: {
            throw new Exception("Unsupported operation type: '" + oper.getType().toString() + "'");
         }
      }

      _logger.exiting(CLASS, METHOD);

      return;
   }

   /**
    * Override interface to support the "search" operation. Get the collection
    * of pending access requests for the give resource owner
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
    *     "result":[
    *       {
    *       }
    *     ],
    *     "resultCount":1,
    *     "pagedResultsCookie":null,
    *     "totalPagedResultsPolicy":"NONE",
    *     "totalPagedResults":-1,
    *     "remainingPagedResults":0
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

   /*
    * =============== PRIVATE METHODS ===============
    */
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

      /*
       * Get JSON data from the Config object via the Config Manager
       */
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

      /*
       * setup the Mongo Data Access Object
       */
      if (!this.isError() && _MongoDAO == null) {
         map = JSON.convertToParams(JSON.getObject(json, ConfigIF.RS_NOSQL));
         try {
            _MongoDAO = MongoFactory.getInstance(map);
         } catch (Exception ex) {
            msg = CLASS + ": " + METHOD + ": Mongo DAO:" + ex.getMessage();
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
    *     "result":[
    *       {
    *         "policyId": "057722e3-94dc-49bc-9a08-66c8e6ff1da60",
    *         "permissions": [
    *           {
    *             "subject": "myoshida",
    *             "scopes": [ "view", "markup" ]
    *           }
    *         ],
    *         "name": "Flood damage claim",
    *         "_id": "057722e3-94dc-49bc-9a08-66c8e6ff1da60"
    *       }
    *     ],
    *     "resultCount":1,
    *     "pagedResultsCookie":null,
    *     "totalPagedResultsPolicy":"NONE",
    *     "totalPagedResults":-1,
    *     "remainingPagedResults":0
    *   }
    * }
    * curl example:
    * curl -X GET\
    * --header "iPlanetDirectoryPro: ..." \
    * --header "Accept-API-Version: resource=1.0" \
    * --data-urlencode '_sortKeys=policyId,name' \
    * --data-urlencode '_pageSize=1' \
    * --data-urlencode '_pagedResultsOffset=0' \
    * --data-urlencode '_queryFilter=permissions/subject eq "bob"' \
    * https://.../openam/json/realms/root/users/<<owner>>/uma/policies
    *
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
         jsonQuery = new JSONObject(); // SEARCH requires a "query" object
         jsonQuery.put(ConstantsIF.OPERATOR, ConstantsIF.NONE);

         jsonHeaders = new JSONObject();
         jsonHeaders.put(ConstantsIF.ACCEPT_API_VERSION, this.getConfigValue(configType, ConfigIF.AS_UMA_POLICIES_ACCEPT));
         jsonHeaders.put(this.getConfigValue(configType, ConfigIF.AS_COOKIE), sso_token);

         jsonParams = new JSONObject();
         jsonParams.put("_pageSize", "10");
         jsonParams.put("_sortKeys", "policyId,name");
         jsonParams.put("_queryFilter", "true");
         jsonParams.put("_pagedResultsOffset", "0");

         jsonSearch = new JSONObject();
         jsonSearch.put(ConstantsIF.QUERY, jsonQuery);
         jsonSearch.put(ConstantsIF.HEADERS, jsonHeaders);
         jsonSearch.put(ConstantsIF.QUERY_PARAMS, jsonParams);
         jsonSearch.put(ConstantsIF.PATH,
            this.getConfigValue(configType, ConfigIF.AS_UMA_POLICIES_PATH)
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
         operOutput.setJSON(this.getSubjects(jsonOutput));
      } else {
         throw new Exception(METHOD + ": sso_token or owner is empty");
      }

      _logger.exiting(CLASS, METHOD);

      return operOutput;
   }

   /**
    * Get the "pending" access requests for the owner
    *
    * <pre>
    * JSON input ...
    * {
    *   "data": {
    *     "result":[
    *       {
    *         "policyId": "057722e3-94dc-49bc-9a08-66c8e6ff1da60",
    *         "permissions": [
    *           {
    *             "subject": "myoshida",
    *             "scopes": [ "view", "markup" ]
    *           }
    *         ],
    *         "name": "Flood damage claim",
    *         "_id": "057722e3-94dc-49bc-9a08-66c8e6ff1da60"
    *       }
    *     ]
    *   }
    * }
    *
    * JSON output ...
    * {
    *   "data": {
    *     "quantity": X,
    *     "results": [
    *       {
    *         "subject": "aadams",
    *         "resources": [
    *           {
    *             "_id": "057722e3-94dc-49bc-9a08-66c8e6ff1da60",
    *             "name": "Flood damage claim",
    *             "scopes": [ "view", "markup" ]
    *           }
    *         ]
    *       }
    *     ]
    *   }
    * }
    * </pre>
    *
    * @param jsonPolicies JSONObject policies with subjects
    * @return JSONObject subjects with resource policies
    */
   private JSONObject getSubjects(final JSONObject jsonPolicies) {
      String METHOD = Thread.currentThread().getStackTrace()[1].getMethodName();
      String registerId = null;
      String resourceId = null;
      String name = null;
      String subject = null;
      JSONObject jsonResults = null;
      JSONObject jsonData = null;
      JSONObject jsonPolicy = null;
      JSONObject jsonPermission = null;
      JSONObject jsonResource = null;
      JSONObject jsonSubject = null;
      JSONArray arraySubjects = null;
      JSONArray arrayPolicies = null;
      JSONArray arrayPermissons = null;
      JSONArray arrayScopes = null;
      JSONArray arrayResources = null;
      Map<String, JSONArray> mapResources = null;

      _logger.entering(CLASS, METHOD);

      arraySubjects = new JSONArray();

      mapResources = new HashMap<>();

      if (jsonPolicies != null && !jsonPolicies.isEmpty()) {
         arrayPolicies = JSON.getArray(jsonPolicies, ConstantsIF.DATA + "." + ConstantsIF.RESULT);

         if (arrayPolicies != null && !arrayPolicies.isEmpty()) {

            for (Object policy : arrayPolicies) {
               if (policy != null && policy instanceof JSONObject) {
                  jsonPolicy = (JSONObject) policy;
                  registerId = JSON.getString(jsonPolicy, ConstantsIF.POLICYID);
                  name = JSON.getString(jsonPolicy, ConstantsIF.NAME);
                  arrayPermissons = JSON.getArray(jsonPolicy, ConstantsIF.PERMISSIONS);

                  resourceId = this.getResourceIdFromRegisterId(registerId);

                  if (!STR.isEmpty(resourceId)) {
                     for (Object permission : arrayPermissons) {
                        if (permission != null && permission instanceof JSONObject) {
                           jsonPermission = (JSONObject) permission;
                           subject = JSON.getString(jsonPermission, ConstantsIF.SUBJECT);
                           arrayScopes = JSON.getArray(jsonPermission, ConstantsIF.SCOPES);

                           if (mapResources.containsKey(subject)) {
                              arrayResources = mapResources.get(subject);
                           } else {
                              arrayResources = new JSONArray();
                              mapResources.put(subject, arrayResources);
                           }

                           jsonResource = new JSONObject();
                           jsonResource.put(ConstantsIF.NAME, name);
                           jsonResource.put(ConstantsIF.SCOPES, arrayScopes);
                           jsonResource.put(ConstantsIF.ID, resourceId);

                           arrayResources.add(jsonResource);
                        }
                     }
                  }
               }
            }
         }
      }

      for (String sub : mapResources.keySet()) {
         jsonSubject = new JSONObject();
         jsonSubject.put(ConstantsIF.SUBJECT, sub);
         jsonSubject.put(ConstantsIF.RESOURCES, mapResources.get(sub));

         arraySubjects.add(jsonSubject);
      }

      jsonData = new JSONObject();
      jsonData.put(ConstantsIF.QUANTITY, arraySubjects.size());
      jsonData.put(ConstantsIF.RESULTS, arraySubjects);

      jsonResults = new JSONObject();
      jsonResults.put(ConstantsIF.DATA, jsonData);

      _logger.exiting(CLASS, METHOD);

      return jsonResults;
   }

   /**
    * Search MongoDB "resources" collection. Use the register id to get the
    * resource id (uid)
    *
    * <pre>
    * JSON input ...
    * {
    *   "query": {
    *     "operator": "eq",
    *     "attribute": "data.owner",
    *     "value": "..."
    *   }
    * }
    * JSON output ... from MongoDB
    * {
    *   "quantity": X,
    *   "results" : [
    *     {
    *       "uid": "...",
    *       "data: {
    *         ...
    *       }
    *     },
    *     ...
    *   ]
    * }
    * </pre>
    *
    * @param registerId String registeration identifier
    * @return String resource identifier
    */
   private String getResourceIdFromRegisterId(final String registerId) {
      String METHOD = Thread.currentThread().getStackTrace()[1].getMethodName();
      String resourceId = null;
      JSONObject jsonInput = null;
      JSONObject jsonQuery = null;
      OperationIF operInput = null;
      OperationIF operOutput = null;

      _logger.entering(CLASS, METHOD);

      if (!STR.isEmpty(registerId)) {
         operInput = new Operation(OperationIF.TYPE.SEARCH);

         jsonQuery = new JSONObject();
         jsonQuery.put(ConstantsIF.OPERATOR, ConstantsIF.EQUAL);
         jsonQuery.put(ConstantsIF.ATTRIBUTE, "data.register");
         jsonQuery.put(ConstantsIF.VALUE, registerId);

         jsonInput = new JSONObject();
         jsonInput.put(ConstantsIF.QUERY, jsonQuery);

         operInput.setJSON(jsonInput);

         try {
            this.setDatabaseAndCollection(operInput, ConfigIF.RS_NOSQL_DATABASE,
               ConfigIF.RS_NOSQL_COLLECTIONS_RESOURCES_NAME);
         } catch (Exception ex) {
            this.setError(true);
            operOutput = new Operation(OperationIF.TYPE.SEARCH);
            operOutput.setError(true);
            operOutput.setState(STATE.ERROR);
            operOutput.setStatus(METHOD + ": " + ex.getMessage());

            _logger.severe(METHOD + ": " + ex.getMessage());
         }

         if (!this.isError()) {
            operOutput = _MongoDAO.execute(operInput);

            if (operOutput.getState() == STATE.SUCCESS) {
               resourceId = JSON.getString(operOutput.getJSON(), ConstantsIF.RESULTS + "[0]." + ConstantsIF.UID);
            }
         }
      }

      _logger.exiting(CLASS, METHOD);

      return resourceId;
   }
}
