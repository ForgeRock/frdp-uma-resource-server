/*
 * Copyright (c) 2018-2020, ForgeRock, Inc., All rights reserved
 * Use subject to license terms.
 */

package com.forgerock.frdp.resourceserver.handler.uma;

import com.forgerock.frdp.common.ConstantsIF;
import com.forgerock.frdp.dao.Operation;
import com.forgerock.frdp.dao.OperationIF;
import com.forgerock.frdp.dao.mongo.MongoFactory;
import com.forgerock.frdp.handler.HandlerManagerIF;
import com.forgerock.frdp.resourceserver.ConfigIF;
import com.forgerock.frdp.resourceserver.dao.AMRestDataAccess;
import com.forgerock.frdp.resourceserver.handler.JaxrsHandler;
import com.forgerock.frdp.utils.JSON;
import com.forgerock.frdp.utils.STR;
import java.util.logging.Level;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

/**
 * Handler for the "discover" feature which enables a UMA Resource Owner (RO) to
 * indicate resources that are discoverable by a UMA Requesting Party (RqP).
 * This feature IS NOT part of the UMA 2.0 specification, this is "value add"
 * functionality of the Resource Server (RS).
 * 
 * @author Scott Fehrman, ForgeRock, Inc.
 */
public class DiscoverHandler extends JaxrsHandler {

   private final String CLASS = this.getClass().getName();
   private String _uma_resourceset_path = null;

   /**
    * Constructor
    * 
    * @param config     JSONObject containing configuration data
    * @param handlerMgr HandlerManagerIF provides management of Handlers
    */
   public DiscoverHandler(final JSONObject config, final HandlerManagerIF handlerMgr) {
      super(config, handlerMgr);

      String METHOD = "DiscoverHandler(config, handlerMgr)";

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
    * @exception Exception
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
         break;
      default:
         throw new Exception("Unsupported operation type: '" + oper.getType().toString() + "'");
      }

      _logger.exiting(CLASS, METHOD);

      return;
   }

   /**
    * Override interface to support the "search" operation
    * 
    * <pre>
    * Get the collection of "discoverable" resources for the owner
    *
    * JSON input ... search for pending requests
    * { 
    *   "owner": "bjensen",
    *   "access_token": "..." // PAT for the owner
    * }
    *
    * JSON output ...
    * {
    *   "data": {
    *     "results”:[
    *       {
    *         "id": "c1e0e9bd-371a-4021-aea6-52305a0e0f36",
    *         "owner": "bjensen",
    *         "name": "labRpt01",
    *         "type": "healthcare/report",
    *         "desc": "Lab Report from Jan 2001"
    *       }
    *     ],
    *     "quantity":1
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
               new Object[] { operInput != null ? operInput.toString() : NULL,
                     operInput.getJSON() != null ? operInput.getJSON().toString() : NULL });
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

      _logger.entering(CLASS, METHOD);

      /*
       * setup the Mongo Data Access Object
       */

      if (_MongoDAO == null) {
         try {
            _MongoDAO = MongoFactory.getInstance(JSON.convertToParams(JSON.getObject(_config, ConfigIF.RS_NOSQL)));
         } catch (Exception ex) {
            this.setError(true);
            this.setState(STATE.ERROR);
            this.setStatus(CLASS + ": " + METHOD + ": Mongo DAO:" + ex.getMessage());
            _logger.severe(this.getStatus());
         }
      }

      if (!this.isError() && _AuthzServerDAO == null) {
         try {
            _AuthzServerDAO = new AMRestDataAccess(JSON.convertToParams(JSON.getObject(_config, ConfigIF.AS_CONNECT)));
         } catch (Exception ex) {
            this.setError(true);
            this.setState(STATE.ERROR);
            this.setStatus(CLASS + ": " + METHOD + ": REST DAO: " + ex.getMessage());
            _logger.severe(this.getStatus());
         }
      }

      if (!this.isError()) {
         try {
            _uma_resourceset_path = this.getConfigValue(ConfigIF.AS_UMA_RESOURCE_SET_PATH);
         } catch (Exception ex) {
            this.setError(true);
            this.setState(STATE.ERROR);
            this.setStatus(CLASS + ": " + METHOD + ": _path : " + ex.getMessage());
            _logger.log(Level.SEVERE, this.getStatus());
         }
      }

      if (!this.isError()) {
         this.setState(STATE.READY);
      }

      _logger.exiting(CLASS, METHOD);

      return;
   }

   /**
    * Implementation of the "search" operation.
    * 
    * <pre>
    * Get the "discoverable" resources for the given Resource Owner 
    *
    * JSON input ... search for pending requests
    * { 
    *   "owner": "bjensen", // Resource Owner (RO)
    *   "access_token": "...", // Protection API Token (PAT) for the RO
    *   "query": { // OPTIONAL ... used to filter the results
    *     "operator": "equal",
    *     "attribute": "type",
    *     "value": "healthcare"
    *   }
    * }
    *
    * JSON output ...
    * {
    *   "data": {
    *     "results”:[
    *       {
    *         "id": "c1e0e9bd-371a-4021-aea6-52305a0e0f36",
    *         "owner": "bjensen",
    *         "name": "labRpt01",
    *         "type": "healthcare/report",
    *         "desc": "Lab Report from Jan 2001"
    *       }
    *     ],
    *     "quantity":1
    *   }
    * }
    * </pre>
    * 
    * @param operInput OperatrionIF input
    * @return OperationIF output
    * @throws Exception
    */
   private OperationIF searchImpl(final OperationIF operInput) throws Exception {
      String METHOD = Thread.currentThread().getStackTrace()[1].getMethodName();
      String owner = null;
      String access_token = null;
      JSONObject jsonInput = null;
      JSONObject jsonData = null;
      JSONObject jsonQuery = null;
      JSONObject jsonSearch = null;
      JSONObject jsonOutput = null;
      OperationIF operOutput = null;
      OperationIF operMongoInput = null;
      OperationIF operMongoOutput = null;

      _logger.entering(CLASS, METHOD);

      operOutput = new Operation(operInput.getType());

      jsonInput = operInput.getJSON();

      owner = JSON.getString(jsonInput, ConstantsIF.OWNER);
      access_token = JSON.getString(jsonInput, ConstantsIF.ACCESS_TOKEN);

      if (!STR.isEmpty(owner)) {
         jsonQuery = new JSONObject(); // data.owner == owner
         jsonQuery.put(ConstantsIF.OPERATOR, ConstantsIF.EQUAL);
         jsonQuery.put(ConstantsIF.ATTRIBUTE, "data.owner");
         jsonQuery.put(ConstantsIF.VALUE, owner);

         jsonSearch = new JSONObject();
         jsonSearch.put(ConstantsIF.QUERY, jsonQuery);

         operMongoInput = new Operation(OperationIF.TYPE.SEARCH); // GET
         operMongoInput.setJSON(jsonSearch);

         try {
            this.setDatabaseAndCollection(operMongoInput, ConfigIF.RS_NOSQL_DATABASE,
                  ConfigIF.RS_NOSQL_COLLECTIONS_RESOURCES_NAME);
         } catch (Exception ex) {
            this.setError(true);
            operOutput = new Operation(OperationIF.TYPE.SEARCH);
            operOutput.setError(true);
            operOutput.setState(STATE.ERROR);
            operOutput.setStatus(METHOD + ": " + ex.getMessage());

            _logger.log(Level.SEVERE, "{0}: {1}", new Object[] { METHOD, ex.getMessage() });
         }

         if (!this.isError()) {
            operMongoOutput = _MongoDAO.execute(operMongoInput);

            if (operMongoOutput.getState() == STATE.NOTEXIST) // 404 NOT FOUND
            {
               jsonData = new JSONObject();
               jsonData.put(ConstantsIF.RESULTS, new JSONArray());
            } else {
               jsonData = operMongoOutput.getJSON();
            }

            operOutput.setState(operMongoOutput.getState());
            operOutput.setStatus(operMongoOutput.getStatus());
         } else {
            jsonData = new JSONObject();
            jsonData.put(ConstantsIF.RESULTS, new JSONArray());
            jsonData.put(ConstantsIF.QUANTITY, 0);
         }

         jsonOutput = new JSONObject();
         jsonOutput.put(ConstantsIF.DATA,
               this.filter(this.getDiscoverable(jsonData, access_token), JSON.getObject(jsonInput, ConstantsIF.QUERY)));

         operOutput.setJSON(jsonOutput);
      } else {
         throw new Exception(METHOD + ": owner is empty");
      }

      _logger.exiting(CLASS, METHOD);

      return operOutput;
   }

   /**
    * Post processes the default search results. The JSON Array (results) contains
    * a simpler object with attributes and removes internal reference keys. The
    * resource must be: discoverable attribute == true and register attribute !=
    * NULL
    * 
    * <pre>
    * JSON input ...
    * {
    *   "quantity": 13,
    *   "results": [
    *     {
    *       "uid": "ff1667c5-ce54-4095-a385-734ab9bcfe3d",
    *       "data": {
    *         "owner": "bjensen",
    *         "meta": {
    *           "discoverable": false,
    *           "name": "Sam Savings",
    *           "description": "Savings account with spouse",
    *           "label": "Joint Savings",
    *           "type": "finance/savings"
    *         },
    *         "content": "056b0692-866f-4f92-9f46-152666f3c39b",
    *         "register": "f4040d6c-5bcb-4d3d-850b-2f91fafbf2213"
    *       }
    *     },
    *     { ...}
    *   ]
    * }
    *
    * JSON output ...
    * {
    *   "quantity": X,
    *   "results": [
    *     {
    *       "id": "ff1667c5-ce54-4095-a385-734ab9bcfe3d", // Resource Id
    *       "owner": "bjensen",
    *       "name": "Sam Savings",
    *       "description": "Savings account with spouse",
    *       "label": "Joint Savings",
    *       "type": "finance/savings"
    *     },
    *     { ... }
    *   ]
    * }
    * </pre>
    * 
    * @param jsonInput    JSONObject input
    * @param access_token String single sign on access token
    * @return JSONObject output
    */
   private JSONObject getDiscoverable(final JSONObject jsonInput, final String access_token) {
      String METHOD = Thread.currentThread().getStackTrace()[1].getMethodName();
      String registerId = null;
      JSONObject jsonOutput = null;
      JSONObject jsonRegistration = null;
      JSONObject jsonResultInput = null;
      JSONObject jsonResultOutput = null;
      JSONArray arrayResultsInput = null;
      JSONArray arrayResultsOutput = null;

      _logger.entering(CLASS, METHOD);

      jsonOutput = new JSONObject();
      arrayResultsOutput = new JSONArray();

      if (jsonInput != null && !jsonInput.isEmpty()) {
         arrayResultsInput = JSON.getArray(jsonInput, ConstantsIF.RESULTS);

         if (arrayResultsInput != null && !arrayResultsInput.isEmpty()) {
            for (Object obj : arrayResultsInput) {
               if (obj != null && obj instanceof JSONObject) {
                  jsonResultInput = (JSONObject) obj;

                  if (JSON.getBoolean(jsonResultInput,
                        ConstantsIF.DATA + "." + ConstantsIF.META + "." + ConstantsIF.DISCOVERABLE)) {
                     registerId = JSON.getString(jsonResultInput, ConstantsIF.DATA + "." + ConstantsIF.REGISTER);

                     if (!STR.isEmpty(registerId)) {
                        jsonResultOutput = new JSONObject();

                        jsonResultOutput.put(ConstantsIF.ID, JSON.getString(jsonResultInput, ConstantsIF.UID));
                        jsonResultOutput.put(ConstantsIF.OWNER,
                              JSON.getString(jsonResultInput, ConstantsIF.DATA + "." + ConstantsIF.OWNER));
                        jsonResultOutput.put(ConstantsIF.NAME, JSON.getString(jsonResultInput,
                              ConstantsIF.DATA + "." + ConstantsIF.META + "." + ConstantsIF.NAME));
                        jsonResultOutput.put(ConstantsIF.DESCRIPTION, JSON.getString(jsonResultInput,
                              ConstantsIF.DATA + "." + ConstantsIF.META + "." + ConstantsIF.DESCRIPTION));
                        jsonResultOutput.put(ConstantsIF.LABEL, JSON.getString(jsonResultInput,
                              ConstantsIF.DATA + "." + ConstantsIF.META + "." + ConstantsIF.LABEL));
                        jsonResultOutput.put(ConstantsIF.TYPE, JSON.getString(jsonResultInput,
                              ConstantsIF.DATA + "." + ConstantsIF.META + "." + ConstantsIF.TYPE));
                        /*
                         * Get the UMA registration data
                         */
                        jsonRegistration = this.getRegistration(registerId, access_token);

                        jsonResultOutput.put(ConstantsIF.SCOPES,
                              JSON.getArray(jsonRegistration, ConstantsIF.RESOURCE_SCOPES));
                        jsonResultOutput.put(ConstantsIF.ICON_URI,
                              JSON.getString(jsonRegistration, ConstantsIF.ICON_URI));

                        arrayResultsOutput.add(jsonResultOutput);
                     }
                  }
               }
            }
         }
      }

      _logger.exiting(CLASS, METHOD);

      jsonOutput.put(ConstantsIF.RESULTS, arrayResultsOutput);
      jsonOutput.put(ConstantsIF.QUANTITY, arrayResultsOutput.size());

      return jsonOutput;
   }

   /**
    * Get "register" data from the Auhorization Server. The registered UMA resource
    * has some attributes that are sourced from the Authorization Server (AS).
    *
    * <pre>
    * JSON input ...
    * {
    *   "uid" : "...", // Register Id
    *   "access_token": "..."
    * }
    * JSON output ... 
    * {
    *   "resource_scopes": ["scope1","scope2","scope3"],
    *   "name": "...",
    *   "_id": "...",
    *   "type": "...",
    *   "labels": [],
    *   "user_access_policy_uri": "https://.../openam/XUI/?realm=/#uma/share/..."
    * }
    * curl example ... get the resource_set (the resource data) for a given resource
    * curl -X GET\
    * -H "Authorization: Bearer <<access_token>>" \
    * https://.../openam/uma/realms/root/resource_set/<<uid>>
    * {
    *   "resource_scopes": ["read","view","print"],
    *   "name": "...",
    *   "_id": "<<uid>>",
    *   "type": "...",
    *   "icon_uri": "...",
    *   "labels": ["VIP","3D"],
    *   "user_access_policy_uri": ".../openam/XUI/?realm=/#uma/share/<<uid>>"
    * }
    * </pre>
    *
    * @param registerId   String UMA registered resource identifier
    * @param access_token String single sign on token
    * @return JOSNObject resource attributes
    */
   private JSONObject getRegistration(final String registerId, final String access_token) {
      String METHOD = Thread.currentThread().getStackTrace()[1].getMethodName();
      JSONObject jsonOutput = null;
      JSONObject jsonHeaders = null;
      JSONObject jsonData = null;
      OperationIF operInput = null;
      OperationIF operOutput = null;

      _logger.entering(CLASS, METHOD);

      jsonOutput = new JSONObject();

      if (!STR.isEmpty(registerId) && !STR.isEmpty(access_token)) {
         jsonHeaders = new JSONObject();
         jsonHeaders.put(ConstantsIF.AUTHORIZATION, "Bearer " + access_token);

         jsonData = new JSONObject();
         jsonData.put(ConstantsIF.HEADERS, jsonHeaders);
         jsonData.put(ConstantsIF.UID, registerId);
         jsonData.put(ConstantsIF.PATH, _uma_resourceset_path);

         operInput = new Operation(OperationIF.TYPE.READ); // GET
         operInput.setJSON(jsonData);

         operOutput = _AuthzServerDAO.execute(operInput);

         if (operOutput.getState() == STATE.SUCCESS) {
            jsonOutput = operOutput.getJSON();

            if (jsonOutput == null || jsonOutput.isEmpty()) {
               jsonOutput = new JSONObject();
            }
         }
      }

      _logger.exiting(CLASS, METHOD);

      return jsonOutput;
   }

   /**
    * Filter the results array. Return a new results array that only contains
    * objects that have an attribute with a specific value.
    *
    * <pre>
    * JSON input ...
    * {
    *   "quantity": X,
    *   "results": [
    *     {
    *       "id": "ff1667c5-ce54-4095-a385-734ab9bcfe3d", // Resource Id
    *       "owner": "bjensen",
    *       "name": "Sam Savings",
    *       "description": "Savings account with spouse",
    *       "label": "Joint Savings",
    *       "type": "finance/savings"
    *     },
    *     { ... }
    *   ]
    * }
    * JSON filter ...
    * {
    *   "attribute": "owner",
    *   "value": "bjensen"
    * }
    *
    * </pre>
    * 
    * @param jsonInput  JSONObject original results data
    * @param jsonFilter JSONObject containing filter attribute name and attribute
    *                   value
    * @return JSONObject modified results data
    */
   private JSONObject filter(final JSONObject jsonInput, final JSONObject jsonFilter) {
      String METHOD = Thread.currentThread().getStackTrace()[1].getMethodName();
      String filterAttr = null;
      String filterVal = null;
      String attrVal = null;
      JSONObject jsonOutput = null;
      JSONObject jsonResult = null;
      JSONArray arrayResultsInput = null;
      JSONArray arrayResultsOutput = null;

      _logger.entering(CLASS, METHOD);

      if (jsonFilter != null) {
         jsonOutput = new JSONObject();
         arrayResultsOutput = new JSONArray();

         filterAttr = JSON.getString(jsonFilter, ConstantsIF.ATTRIBUTE);
         filterVal = JSON.getString(jsonFilter, ConstantsIF.VALUE);

         if (!STR.isEmpty(filterAttr) && !STR.isEmpty(filterVal)) {
            arrayResultsInput = JSON.getArray(jsonInput, ConstantsIF.RESULTS);
            if (arrayResultsInput != null && !arrayResultsInput.isEmpty()) {
               for (Object obj : arrayResultsInput) {
                  if (obj != null && obj instanceof JSONObject) {
                     jsonResult = (JSONObject) obj;
                     attrVal = JSON.getString(jsonResult, filterAttr);

                     if (!STR.isEmpty(attrVal) && attrVal.equalsIgnoreCase(filterVal)) {
                        arrayResultsOutput.add(jsonResult);
                     }
                  }
               }
            }
         }
         jsonOutput.put(ConstantsIF.RESULTS, arrayResultsOutput);
         jsonOutput.put(ConstantsIF.QUANTITY, arrayResultsOutput.size());
      } else {
         jsonOutput = jsonInput;
      }

      _logger.exiting(CLASS, METHOD);

      return jsonOutput;
   }
}
