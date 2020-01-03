/*
 * Copyright (c) 2018-2020, ForgeRock, Inc., All rights reserved
 * Use subject to license terms.
 */

package com.forgerock.frdp.resourceserver.handler;

import com.forgerock.frdp.common.ConstantsIF;
import com.forgerock.frdp.dao.Operation;
import com.forgerock.frdp.dao.OperationIF;
import com.forgerock.frdp.dao.mongo.MongoFactory;
import com.forgerock.frdp.handler.HandlerManagerIF;
import com.forgerock.frdp.resourceserver.ConfigIF;
import com.forgerock.frdp.utils.JSON;
import com.forgerock.frdp.utils.STR;
import java.util.Map;
import java.util.logging.Level;
import org.json.simple.JSONObject;

/**
 * Meta Data Handler ... data about the JSON data
 * 
 * @author Scott Fehrman, ForgeRock, Inc.
 */
public class MetaHandler extends JaxrsHandler {

   private final String CLASS = this.getClass().getName();
   private final String[] _attrRequired = { ConstantsIF.NAME, // UMA resource set
         ConstantsIF.TYPE // UMA resource set
   };

   /**
    * Constructor
    * 
    * @param config     JSONObject containing configuration data
    * @param handlerMgr HandlerManagerIF provides management of Handlers
    */
   public MetaHandler(final JSONObject config, final HandlerManagerIF handlerMgr) {
      super(config, handlerMgr);

      String METHOD = "MetaHandler(config, handlerMgr)";

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
      case READ: {
         this.checkUid(jsonInput);
         break;
      }
      case REPLACE: {
         this.checkUid(jsonInput);
         this.checkMeta(jsonInput);
         break;
      }
      case DELETE: {
         this.checkUid(jsonInput);
         break;
      }
      default:
         break;
      }

      _logger.exiting(CLASS, METHOD);

      return;
   }

   /**
    * Override interface to support the "read" operation
    * 
    * <pre>
    * JSON input ...
    * { "uid": "..." } // Resource Id
    * JSON output ...
    * {
    *   "name": "...",
    *   "type": "...",
    *   "label": "...",
    *   "description": "...",
    *   "discoverable": ...
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
      OperationIF operOutput = null;
      JSONObject jsonOutput = null;
      JSONObject jsonMeta = null;
      JSONObject jsonWrapData = null;

      _logger.entering(CLASS, METHOD);

      if (_logger.isLoggable(DEBUG_LEVEL)) {
         _logger.log(DEBUG_LEVEL, "input=''{0}'', json=''{1}''",
               new Object[] { operInput != null ? operInput.toString() : NULL,
                     operInput.getJSON() != null ? operInput.getJSON().toString() : NULL });
      }

      operOutput = new Operation(OperationIF.TYPE.READ);

      try {
         this.setDatabaseAndCollection(operInput, ConfigIF.RS_NOSQL_DATABASE,
               ConfigIF.RS_NOSQL_COLLECTIONS_RESOURCES_NAME);
      } catch (Exception ex) {
         error = true;
         operOutput = new Operation(OperationIF.TYPE.READ);
         operOutput.setError(true);
         operOutput.setState(STATE.ERROR);
         operOutput.setStatus(ex.getMessage());
      }

      if (!error) {
         operOutput = _MongoDAO.execute(operInput);

         /*
          * Extract the "meta" object JSON ... {"data": {"meta": {...}}}
          */
         if (operOutput != null && !operOutput.isError()) {
            jsonOutput = operOutput.getJSON();

            jsonMeta = JSON.getObject(jsonOutput, ConstantsIF.DATA + "." + ConstantsIF.META);

            if (jsonMeta == null) {
               jsonMeta = new JSONObject();
            }

            jsonWrapData = new JSONObject();
            jsonWrapData.put(ConstantsIF.DATA, jsonMeta);

            operOutput.setJSON(jsonWrapData);
         }
      }

      if (_logger.isLoggable(DEBUG_LEVEL)) {
         _logger.log(DEBUG_LEVEL, "output=''{0}'', json=''{1}''",
               new Object[] { operOutput != null ? operOutput.toString() : NULL,
                     operOutput.getJSON() != null ? operOutput.getJSON().toString() : NULL });
      }

      _logger.exiting(CLASS, METHOD);

      return operOutput;
   }

   /**
    * Override interface to support the "replace" operation
    *
    * <pre>
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
      String resourceUid = null;
      OperationIF operOutput = null;
      JSONObject jsonInput = null;
      JSONObject jsonData = null;

      _logger.entering(CLASS, METHOD);

      if (_logger.isLoggable(DEBUG_LEVEL)) {
         _logger.log(DEBUG_LEVEL, "input=''{0}'', json=''{1}''",
               new Object[] { operInput != null ? operInput.toString() : NULL,
                     operInput.getJSON() != null ? operInput.getJSON().toString() : NULL });
      }

      operOutput = new Operation(OperationIF.TYPE.REPLACE);

      jsonInput = operInput.getJSON();

      resourceUid = JSON.getString(jsonInput, ConstantsIF.UID);
      jsonData = JSON.getObject(jsonInput, ConstantsIF.DATA);

      try {
         this.replaceImpl(resourceUid, jsonData);
      } catch (Exception ex) {
         error = true;
         operOutput.setError(true);
         operOutput.setState(STATE.ERROR);
         operOutput.setStatus(ex.getMessage());
      }

      if (!error) {
         operOutput.setState(STATE.SUCCESS);
         operOutput.setStatus("Replaced meta");
      }

      if (_logger.isLoggable(DEBUG_LEVEL)) {
         _logger.log(DEBUG_LEVEL, "output=''{0}''", new Object[] { operOutput != null ? operOutput.toString() : NULL });
      }

      _logger.exiting(CLASS, METHOD);

      return operOutput;
   }

   /**
    * Override interface to support the "delete" operation
    *
    * @param operInput OperationIF input for delete operation
    * @return OperationIF output from delete operation
    */
   @Override
   protected OperationIF delete(OperationIF operInput) {
      OperationIF operOutput = null;
      String METHOD = Thread.currentThread().getStackTrace()[1].getMethodName();
      String resourceUid = null;
      JSONObject jsonInput = null;

      _logger.entering(CLASS, METHOD);

      if (_logger.isLoggable(DEBUG_LEVEL)) {
         _logger.log(DEBUG_LEVEL, "input=''{0}'', json=''{1}''",
               new Object[] { operInput != null ? operInput.toString() : NULL,
                     operInput.getJSON() != null ? operInput.getJSON().toString() : NULL });
      }

      operOutput = new Operation(OperationIF.TYPE.DELETE);

      jsonInput = operInput.getJSON();
      resourceUid = JSON.getString(jsonInput, ConstantsIF.UID);

      try {
         this.deleteImpl(resourceUid);
      } catch (Exception ex) {
         operOutput.setError(true);
         operOutput.setState(STATE.ERROR);
         operOutput.setStatus(ex.getMessage());
      }

      if (!operOutput.isError()) {
         operOutput.setState(STATE.SUCCESS);
         operOutput.setStatus("Deleted meta");
      }

      if (_logger.isLoggable(DEBUG_LEVEL)) {
         _logger.log(DEBUG_LEVEL, "output=''{0}''", new Object[] { operOutput != null ? operOutput.toString() : NULL });
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
      Map<String, String> map = null;

      _logger.entering(CLASS, METHOD);

      /*
       * setup the Mongo Data Access Object
       */
      if (_MongoDAO == null) {
         map = JSON.convertToParams(JSON.getObject(_config, ConfigIF.RS_NOSQL));

         try {
            _MongoDAO = MongoFactory.getInstance(map);
         } catch (Exception ex) {
            this.setError(true);
            this.setState(STATE.ERROR);
            this.setStatus(CLASS + ": " + METHOD + ": Mongo DAO:" + ex.getMessage());
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
    * Check for required attributes in the metadata JSON payload
    * 
    * <pre>
    * {
    *   "data": {
    *     ...
    *   },
    *   ...
    * }
    * </pre>
    * 
    * @param jsonInput
    * @throws Exception
    */
   private void checkMeta(JSONObject jsonInput) throws Exception {
      String METHOD = Thread.currentThread().getStackTrace()[1].getMethodName();
      JSONObject jsonData = null;

      _logger.entering(CLASS, METHOD);

      if (jsonInput != null) {
         jsonData = JSON.getObject(jsonInput, ConstantsIF.DATA);

         if (jsonData != null) {
            for (String name : _attrRequired) {
               if (STR.isEmpty(JSON.getString(jsonData, name))) {
                  throw new Exception("Required attribute '" + name + "' is missing or empty");
               }
            }
         } else {
            throw new Exception("JSON object '" + ConstantsIF.DATA + "' is missing from input");
         }
      } else {
         throw new Exception("JSON input is null");
      }

      _logger.exiting(CLASS, METHOD);

      return;
   }

   /**
    * Implementation of the "replace" operation. Read the existing entry using the
    * "uid" The returned entry contains more then just the "meta" object Replace
    * the "meta" object. DO NOT change any other section / object Save (replace)
    * the entry.
    *
    * @param resourceUid String unique identifier for a resource
    * @param jsonMeta    JSONObject meta data for the resource
    * @throws Exception
    */
   private void replaceImpl(final String resourceUid, final JSONObject jsonMeta) throws Exception {
      String METHOD = Thread.currentThread().getStackTrace()[1].getMethodName();
      OperationIF readInput = null;
      OperationIF readOutput = null;
      OperationIF replaceInput = null;
      OperationIF replaceOutput = null;
      JSONObject jsonInput = null;
      JSONObject jsonOutput = null;
      JSONObject jsonData = null;

      _logger.entering(CLASS, METHOD);

      if (_logger.isLoggable(DEBUG_LEVEL)) {
         _logger.log(DEBUG_LEVEL, "resourceUid=''{0}'', json=''{1}''", new Object[] {
               resourceUid != null ? resourceUid : NULL, jsonMeta != null ? jsonMeta.toString() : NULL });
      }

      /*
       * check for "discoverable" attribute, set if not found
       */
      if (!jsonMeta.containsKey(ConstantsIF.DISCOVERABLE)) {
         jsonMeta.put(ConstantsIF.DISCOVERABLE, false);
      }

      jsonInput = new JSONObject();
      jsonInput.put(ConstantsIF.UID, resourceUid);

      readInput = new Operation(OperationIF.TYPE.READ);
      readInput.setJSON(jsonInput);

      this.setDatabaseAndCollection(readInput, ConfigIF.RS_NOSQL_DATABASE,
            ConfigIF.RS_NOSQL_COLLECTIONS_RESOURCES_NAME);

      readOutput = _MongoDAO.execute(readInput);

      if (readOutput != null && !readOutput.isError()) {
         jsonOutput = readOutput.getJSON();

         jsonData = JSON.getObject(jsonOutput, ConstantsIF.DATA);

         if (jsonData != null) {
            jsonData.put(ConstantsIF.META, jsonMeta);

            jsonInput.put(ConstantsIF.DATA, jsonData);

            replaceInput = new Operation(OperationIF.TYPE.REPLACE);
            replaceInput.setJSON(jsonInput);

            this.setDatabaseAndCollection(replaceInput, ConfigIF.RS_NOSQL_DATABASE,
                  ConfigIF.RS_NOSQL_COLLECTIONS_RESOURCES_NAME);

            replaceOutput = _MongoDAO.execute(replaceInput);

            if (replaceOutput.isError()) {
               throw new Exception(
                     METHOD + ": " + replaceOutput.getState().toString() + ": " + replaceOutput.getStatus());
            }
         }
      } else {
         throw new Exception(METHOD + ": " + readOutput == null ? "Output from DAO.execute() is null"
               : readOutput.getState().toString() + ": " + readOutput.getStatus());
      }

      _logger.exiting(CLASS, METHOD);

      return;
   }

   /**
    * Implementation of the "delete" operation. Read the existing entry using the
    * "uid"
    * 
    * @param resourceUid String unique identifier for a resource
    * @throws Exception
    */
   private void deleteImpl(final String resourceUid) throws Exception {
      String METHOD = Thread.currentThread().getStackTrace()[1].getMethodName();
      String registerId = null;
      OperationIF readInput = null;
      OperationIF readOutput = null;
      OperationIF deleteInput = null;
      OperationIF deleteOutput = null;
      JSONObject jsonInput = null;
      JSONObject jsonOutput = null;
      JSONObject jsonData = null;

      _logger.entering(CLASS, METHOD);

      if (_logger.isLoggable(DEBUG_LEVEL)) {
         _logger.log(DEBUG_LEVEL, "resourceUid=''{0}''", new Object[] { resourceUid != null ? resourceUid : NULL });
      }

      jsonInput = new JSONObject();
      jsonInput.put(ConstantsIF.UID, resourceUid);

      readInput = new Operation(OperationIF.TYPE.READ);
      readInput.setJSON(jsonInput);

      this.setDatabaseAndCollection(readInput, ConfigIF.RS_NOSQL_DATABASE,
            ConfigIF.RS_NOSQL_COLLECTIONS_RESOURCES_NAME);

      readOutput = _MongoDAO.execute(readInput);

      if (readOutput != null && !readOutput.isError()) {
         jsonOutput = readOutput.getJSON();

         jsonData = JSON.getObject(jsonOutput, ConstantsIF.DATA);

         if (jsonData != null) {
            registerId = JSON.getString(jsonData, ConstantsIF.REGISTER);
            if (jsonData.containsKey(ConstantsIF.META) && STR.isEmpty(registerId)) {
               jsonData.remove(ConstantsIF.META);

               jsonInput.put(ConstantsIF.DATA, jsonData);

               deleteInput = new Operation(OperationIF.TYPE.REPLACE);
               deleteInput.setJSON(jsonInput);

               this.setDatabaseAndCollection(deleteInput, ConfigIF.RS_NOSQL_DATABASE,
                     ConfigIF.RS_NOSQL_COLLECTIONS_RESOURCES_NAME);

               deleteOutput = _MongoDAO.execute(deleteInput);

               if (deleteOutput.isError()) {
                  throw new Exception(
                        METHOD + ": " + deleteOutput.getState().toString() + ": " + deleteOutput.getStatus());
               }
            } else {
               throw new Exception("The resource is 'registered', can not delete meta");
            }
         }
      } else {
         throw new Exception(METHOD + ": " + readOutput == null ? "Output from DAO.execute() is null"
               : readOutput.getState().toString() + ": " + readOutput.getStatus());
      }

      _logger.exiting(CLASS, METHOD);

      return;
   }
}
