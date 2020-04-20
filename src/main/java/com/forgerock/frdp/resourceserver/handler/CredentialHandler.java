/*
 * Copyright (c) 2019-2020, ForgeRock, Inc., All rights reserved
 * Use subject to license terms.
 */
package com.forgerock.frdp.resourceserver.handler;

import com.forgerock.frdp.common.ConstantsIF;
import com.forgerock.frdp.config.ConfigurationIF;
import com.forgerock.frdp.config.ConfigurationManagerIF;
import com.forgerock.frdp.dao.Operation;
import com.forgerock.frdp.dao.OperationIF;
import com.forgerock.frdp.dao.mongo.MongoFactory;
import com.forgerock.frdp.handler.HandlerManagerIF;
import com.forgerock.frdp.resourceserver.ConfigIF;
import com.forgerock.frdp.resourceserver.dao.AMRestDataAccess;
import com.forgerock.frdp.utils.JSON;
import java.util.Map;
import java.util.logging.Level;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

/**
 * Abstract Credential Handler
 *
 * @author Scott Fehrman, ForgeRock, Inc.
 */
public abstract class CredentialHandler extends JaxrsHandler {

   private final String CLASS = this.getClass().getName();

   /**
    * Constructor
    *
    * @param configMgr ConfigurationManagerIF management of configurations
    * @param handlerMgr HandlerManagerIF provides management of Handlers
    */
   public CredentialHandler(final ConfigurationManagerIF configMgr, final HandlerManagerIF handlerMgr) {
      super(configMgr, handlerMgr);
      String METHOD = "CredentialHandler(configMgr, handlerMgr)";

      _logger.entering(CLASS, METHOD);
      _logger.exiting(CLASS, METHOD);

      return;
   }

   /**
    * Get Credential for the specified userid and category
    *
    * <pre>
    * JSON query structure ...
    * {
    *   "query": {
    *     "operator": "and",
    *     "queries": [
    *       {
    *         "operator": "equal",
    *         "attribute": "data.owner",
    *         "value": "amadmin"
    *       },
    *       {
    *         "operator": "equal",
    *         "attribute": "data.category",
    *         "value": "sso_session"
    *       }
    *     ]
    *   }
    * }
    *
    * Get first JSON object from array "results[0]" or "results"
    * {
    *   "results": [
    *     { ... },
    *     ...
    *   ]
    * }
    * </pre>
    *
    * @param owner userid
    * @param category type of credential
    * @return String credential value
    * @throws Exception could not get the uid
    */
   protected synchronized String getCredentialUid(final String owner, final String category) throws Exception {
      String METHOD = Thread.currentThread().getStackTrace()[1].getMethodName();
      String credUid = null;
      Number quantity = 0;
      JSONObject jsonQuery = null;
      JSONObject jsonInput = null;
      JSONArray jsonResults = null;
      JSONArray jsonQueries = null;
      OperationIF operInput = null;
      OperationIF operOutput = null;

      _logger.entering(CLASS, METHOD);

      jsonQueries = new JSONArray();

      jsonQuery = new JSONObject();
      jsonQuery.put(ConstantsIF.OPERATOR, ConstantsIF.EQUAL);
      jsonQuery.put(ConstantsIF.ATTRIBUTE, ConstantsIF.DATA + "." + ConstantsIF.OWNER);
      jsonQuery.put(ConstantsIF.VALUE, owner);

      jsonQueries.add(jsonQuery);

      jsonQuery = new JSONObject();
      jsonQuery.put(ConstantsIF.OPERATOR, ConstantsIF.EQUAL);
      jsonQuery.put(ConstantsIF.ATTRIBUTE, ConstantsIF.DATA + "." + ConstantsIF.CATEGORY);
      jsonQuery.put(ConstantsIF.VALUE, category);

      jsonQueries.add(jsonQuery);

      jsonQuery = new JSONObject();
      jsonQuery.put(ConstantsIF.OPERATOR, ConstantsIF.AND);
      jsonQuery.put(ConstantsIF.QUERIES, jsonQueries);

      jsonInput = new JSONObject();
      jsonInput.put(ConstantsIF.QUERY, jsonQuery);

      if (_logger.isLoggable(DEBUG_LEVEL)) {
         _logger.log(DEBUG_LEVEL, "jsonQuery=''{0}''", jsonQuery.toString());
      }

      operInput = new Operation(OperationIF.TYPE.SEARCH);
      operInput.setJSON(jsonInput);

      this.setDatabaseAndCollection(operInput, ConfigIF.RS_NOSQL_DATABASE,
         ConfigIF.RS_NOSQL_COLLECTIONS_CREDENTIALS_NAME);

      operOutput = _MongoDAO.execute(operInput);

      /*
       * Get first JSON object from array "results[0]" or "results"
       */
      quantity = JSON.getNumber(operOutput.getJSON(), ConstantsIF.QUANTITY);
      jsonResults = JSON.getArray(operOutput.getJSON(), ConstantsIF.RESULTS);

      if (quantity != null) {
         switch (quantity.intValue()) {
            case 0: // nothing found return null
            {
               break;
            }
            case 1: // "there can be only one ... Highlander"
            {
               credUid = JSON.getString((JSONObject) jsonResults.get(0), ConstantsIF.UID);
               break;
            }
            default: // Error: maybe multiple records found
            {
               _logger.log(Level.SEVERE, "Search quantity should be '1', value is: ''{0}''", quantity);
               this.removeDuplicates(jsonResults);
               break;
            }
         }
      } else {
         _logger.log(Level.SEVERE, "Search quantity Number is null");
      }

      if (_logger.isLoggable(DEBUG_LEVEL)) {
         _logger.log(DEBUG_LEVEL, "owner=''{0}'', credUid=''{1}''",
            new Object[]{owner != null ? owner : NULL, credUid != null ? credUid : NULL});
      }

      _logger.exiting(CLASS, METHOD);

      return credUid;
   }

   /**
    * Initialize the object
    */
   protected void init() {
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

      /*
       * setup the REST Data Access Object
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
         _logger.log(Level.SEVERE, msg);
      }

      _logger.exiting(CLASS, METHOD);

      return;
   }

   /*
    * =============== PRIVATE METHODS ===============
    */
   /**
    * Remove duplicate data from the results. If there is more than one result
    * in the JSON array, delete all the items in the array, something went
    * wrong. ... "There can be only one, Highlander"
    *
    * <pre>
    * For each result in the array, delete the entry
    * {
    *   "results": [
    *     {
    *       "uid": "...",
    *       ...
    *     },
    *     ...
    *   ]
    * }
    * </pre>
    *
    * @param jsonResults
    * @throws Exception
    */
   private void removeDuplicates(final JSONArray jsonResults) throws Exception {
      String METHOD = Thread.currentThread().getStackTrace()[1].getMethodName();
      JSONObject jsonInput = null;
      OperationIF operInput = null;
      OperationIF operOutput = null;

      _logger.entering(CLASS, METHOD);

      if (jsonResults != null && jsonResults.size() > 1) {
         operInput = new Operation(OperationIF.TYPE.DELETE);

         this.setDatabaseAndCollection(operInput, ConfigIF.RS_NOSQL_DATABASE,
            ConfigIF.RS_NOSQL_COLLECTIONS_CREDENTIALS_NAME);

         for (Object obj : jsonResults) {
            if (obj != null && obj instanceof JSONObject) {
               jsonInput = (JSONObject) obj;

               operInput.setJSON(jsonInput);

               operOutput = _MongoDAO.execute(operInput);

               if (operOutput.isError()) {
                  _logger.log(Level.WARNING, "Delete error: {0}, JSON=''{1}",
                     new Object[]{operOutput.getStatus(), jsonInput.toString()});
               } else {
                  _logger.log(Level.INFO, "Deleted entry: JSON=''{0}''", jsonInput.toString());
               }
            } else {
               _logger.log(Level.WARNING, "Array item is null or not a JSONObject");
            }
         }
      } else {
         _logger.log(Level.WARNING, "Array is null or has only 1 item");
      }

      _logger.exiting(CLASS, METHOD);

      return;
   }
}
