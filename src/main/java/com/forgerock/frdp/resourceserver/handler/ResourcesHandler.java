/*
 * Copyright (c) 2018-2020, ForgeRock, Inc., All rights reserved
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
import com.forgerock.frdp.utils.JSON;
import java.util.Map;
import org.json.simple.JSONObject;

/**
 * Resource Handler
 *
 * @author Scott Fehrman, ForgeRock, Inc.
 */
public class ResourcesHandler extends JaxrsHandler {

   private final String CLASS = this.getClass().getName();

   /**
    * Constructor
    *
    * @param configMgr ConfigurationManagerIF management of configurations
    * @param handlerMgr HandlerManagerIF provides management of Handlers
    */
   public ResourcesHandler(final ConfigurationManagerIF configMgr, final HandlerManagerIF handlerMgr) {
      super(configMgr, handlerMgr);

      String METHOD = "ResourcesHandler(configMgr, handlerMgr)";

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
    *   "data": {
    *     "owner": "", // dynamically set based on authenticated user
    *     "access": "private"
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
      OperationIF operOutput = null;

      _logger.entering(CLASS, METHOD);

      if (_logger.isLoggable(DEBUG_LEVEL)) {
         _logger.log(DEBUG_LEVEL, "input=''{0}'', json=''{1}''",
            new Object[]{operInput != null ? operInput.toString() : NULL,
               operInput.getJSON() != null ? operInput.getJSON().toString() : NULL});
      }

      operOutput = new Operation(OperationIF.TYPE.CREATE);

      try {
         this.setDatabaseAndCollection(operInput, ConfigIF.RS_NOSQL_DATABASE,
            ConfigIF.RS_NOSQL_COLLECTIONS_RESOURCES_NAME);
      } catch (Exception ex) {
         error = true;
         operOutput.setError(true);
         operOutput.setState(STATE.ERROR);
         operOutput.setStatus(ex.getMessage());
      }

      if (!error) {
         operOutput = _MongoDAO.execute(operInput);
      }

      if (_logger.isLoggable(DEBUG_LEVEL)) {
         _logger.log(DEBUG_LEVEL, "output=''{0}'', json=''{1}''",
            new Object[]{operOutput != null ? operOutput.toString() : NULL,
               operOutput.getJSON() != null ? operOutput.getJSON().toString() : NULL});
      }

      _logger.exiting(CLASS, METHOD);

      return operOutput;
   }

   /**
    * Override interface to support the "search" operation
    *
    * <pre>
    * {
    *   "query": {
    *     "operator": "all"
    *   }
    * }
    * -or-
    * {
    *   "query": {
    *     "operator": "equal",
    *     "attribute": "data.owner",
    *     "value": "bjensen"
    *   }
    * }
    * JSON output ...
    * {
    *   "data": {
    *     "quantity": x,
    *     "results": [
    *       { ... },
    *       ...
    *     ]
    *   }
    * }
    * </pre>
    *
    * @param operInput OperationIF input for search operation
    * @return OperationIF output from search operation
    */
   @Override
   protected OperationIF search(final OperationIF operInput) {
      boolean error = false;
      String METHOD = Thread.currentThread().getStackTrace()[1].getMethodName();
      OperationIF operOutput = null;
      JSONObject jsonData = null;

      _logger.entering(CLASS, METHOD);

      if (_logger.isLoggable(DEBUG_LEVEL)) {
         _logger.log(DEBUG_LEVEL, "input=''{0}'', json=''{1}''",
            new Object[]{operInput != null ? operInput.toString() : NULL,
               operInput.getJSON() != null ? operInput.getJSON().toString() : NULL});
      }

      operOutput = new Operation(OperationIF.TYPE.SEARCH);

      try {
         this.setDatabaseAndCollection(operInput, ConfigIF.RS_NOSQL_DATABASE,
            ConfigIF.RS_NOSQL_COLLECTIONS_RESOURCES_NAME);
      } catch (Exception ex) {
         error = true;
         operOutput.setError(true);
         operOutput.setState(STATE.ERROR);
         operOutput.setStatus(ex.getMessage());
      }

      if (!error) {
         operOutput = _MongoDAO.execute(operInput);

         jsonData = new JSONObject();
         jsonData.put(ConstantsIF.DATA, operOutput.getJSON());

         operOutput.setJSON(jsonData);
      }

      if (_logger.isLoggable(DEBUG_LEVEL)) {
         _logger.log(DEBUG_LEVEL, "output=''{0}'', json=''{1}''",
            new Object[]{operOutput != null ? operOutput.toString() : NULL,
               operOutput.getJSON() != null ? operOutput.getJSON().toString() : NULL});
      }

      _logger.exiting(CLASS, METHOD);

      return operOutput;
   }

   /**
    * Override interface to support the "read" operation
    *
    * <pre>
    * JSON input ...
    * { "uid": "..." }
    * JSON output ...
    * {
    *   "uid": "...",
    *   "data": {
    *     "owner": "...",
    *     "access": "private | registered | shared",
    *     "meta": {
    *       "name": "...",
    *       ...
    *     },
    *     "content": "...",
    *     "register": "..."
    *   },
    *   "timestamps": {
    *     ...
    *   }
    * }
    * </pre>
    *
    * @param operInput OperationIF input for read operation
    * @return OperationIF output from read operation
    */
   @Override
   protected OperationIF read(final OperationIF operInput) {
      boolean error = false;
      String METHOD = Thread.currentThread().getStackTrace()[1].getMethodName();
      OperationIF operOutput = null;

      _logger.entering(CLASS, METHOD);

      if (_logger.isLoggable(DEBUG_LEVEL)) {
         _logger.log(DEBUG_LEVEL, "input=''{0}'', json=''{1}''",
            new Object[]{operInput != null ? operInput.toString() : NULL,
               operInput.getJSON() != null ? operInput.getJSON().toString() : NULL});
      }

      operOutput = new Operation(OperationIF.TYPE.READ);

      try {
         this.setDatabaseAndCollection(operInput, ConfigIF.RS_NOSQL_DATABASE,
            ConfigIF.RS_NOSQL_COLLECTIONS_RESOURCES_NAME);
      } catch (Exception ex) {
         error = true;
         operOutput.setError(true);
         operOutput.setState(STATE.ERROR);
         operOutput.setStatus(ex.getMessage());
      }

      if (!error) {
         operOutput = _MongoDAO.execute(operInput);
      }

      if (_logger.isLoggable(DEBUG_LEVEL)) {
         _logger.log(DEBUG_LEVEL, "output=''{0}'', json=''{1}''",
            new Object[]{operOutput != null ? operOutput.toString() : NULL,
               operOutput.getJSON() != null ? operOutput.getJSON().toString() : NULL});
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
    *   "uid": "...",
    *   "data": {
    *     ...
    *   }
    * }
    * </pre>
    *
    * @param operInput OperationIF input for replace operation
    * @return OperationIF output from replace operation
    */
   @Override
   protected OperationIF replace(OperationIF operInput) {
      boolean error = false;
      String METHOD = Thread.currentThread().getStackTrace()[1].getMethodName();
      OperationIF operOutput = null;

      _logger.entering(CLASS, METHOD);

      if (_logger.isLoggable(DEBUG_LEVEL)) {
         _logger.log(DEBUG_LEVEL, "input=''{0}'', json=''{1}''",
            new Object[]{operInput != null ? operInput.toString() : NULL,
               operInput.getJSON() != null ? operInput.getJSON().toString() : NULL});
      }

      operOutput = new Operation(OperationIF.TYPE.REPLACE);

      try {
         this.setDatabaseAndCollection(operInput, ConfigIF.RS_NOSQL_DATABASE,
            ConfigIF.RS_NOSQL_COLLECTIONS_RESOURCES_NAME);
      } catch (Exception ex) {
         error = true;
         operOutput.setError(true);
         operOutput.setState(STATE.ERROR);
         operOutput.setStatus(ex.getMessage());
      }

      if (!error) {
         operOutput = _MongoDAO.execute(operInput);
      }

      if (_logger.isLoggable(DEBUG_LEVEL)) {
         _logger.log(DEBUG_LEVEL, "output=''{0}'', json=''{1}''",
            new Object[]{operOutput != null ? operOutput.toString() : NULL,
               operOutput.getJSON() != null ? operOutput.getJSON().toString() : NULL});
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
    *   "uid": "..."
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
      OperationIF operOutput = null;

      _logger.entering(CLASS, METHOD);

      if (_logger.isLoggable(DEBUG_LEVEL)) {
         _logger.log(DEBUG_LEVEL, "input=''{0}'', json=''{1}''",
            new Object[]{operInput != null ? operInput.toString() : NULL,
               operInput.getJSON() != null ? operInput.getJSON().toString() : NULL});
      }

      operOutput = new Operation(OperationIF.TYPE.DELETE);

      try {
         this.setDatabaseAndCollection(operInput, ConfigIF.RS_NOSQL_DATABASE,
            ConfigIF.RS_NOSQL_COLLECTIONS_RESOURCES_NAME);
      } catch (Exception ex) {
         error = true;
         operOutput.setError(true);
         operOutput.setState(STATE.ERROR);
         operOutput.setStatus(ex.getMessage());
      }

      if (!error) {
         operOutput = _MongoDAO.execute(operInput);
      }

      if (_logger.isLoggable(DEBUG_LEVEL)) {
         _logger.log(DEBUG_LEVEL, "output=''{0}'', json=''{1}''",
            new Object[]{operOutput != null ? operOutput.toString() : NULL,
               operOutput.getJSON() != null ? operOutput.getJSON().toString() : NULL});
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
       * setup the Mongo Data Access Object
       */
      if (_MongoDAO == null) {
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
}
