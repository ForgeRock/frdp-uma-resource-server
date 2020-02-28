/*
 * Copyright (c) 2018-2020, ForgeRock, Inc., All rights reserved
 * Use subject to license terms.
 */
package com.forgerock.frdp.resourceserver.handler;

import com.forgerock.frdp.common.ConstantsIF;
import com.forgerock.frdp.common.DataIF;
import com.forgerock.frdp.config.ConfigurationIF;
import com.forgerock.frdp.config.ConfigurationManagerIF;
import com.forgerock.frdp.dao.Operation;
import com.forgerock.frdp.dao.OperationIF;
import com.forgerock.frdp.dao.mongo.MongoFactory;
import com.forgerock.frdp.dao.rest.RestDataAccess;
import com.forgerock.frdp.handler.HandlerManagerIF;
import com.forgerock.frdp.resourceserver.ConfigIF;
import com.forgerock.frdp.resourceserver.content.BasicContentService;
import com.forgerock.frdp.resourceserver.content.ContentServiceIF;
import com.forgerock.frdp.utils.JSON;
import com.forgerock.frdp.utils.STR;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

/**
 * Content Handler
 *
 * @author Scott Fehrman, ForgeRock, Inc.
 */
public class ContentHandler extends JaxrsHandler {

   private final String CLASS = this.getClass().getName();
   private final Map<String, ContentServiceIF> _services = new HashMap<>();

   /**
    * Constructor
    *
    * @param configMgr ConfigurationManagerIF management of configurations
    * @param handlerMgr HandlerManagerIF handler manager
    */
   public ContentHandler(final ConfigurationManagerIF configMgr, final HandlerManagerIF handlerMgr) {
      super(configMgr, handlerMgr);

      String METHOD = "ContentHandler(configMgr, handlerMgr)";

      _logger.entering(CLASS, METHOD);

      this.init();

      _logger.exiting(CLASS, METHOD);

      return;
   }

   /*
    * ================= 
    * PROTECTED METHODS 
    * =================
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
            this.checkUid(jsonInput);
            break;
         }
         default: {
            break;
         }
      }

      _logger.exiting(CLASS, METHOD);

      return;
   }

   /**
    * Enable "read" operation
    *
    * <pre>
    * JSON input ...
    * { "uid": "..." } // Content GUID
    * JSON output ...
    * {
    *   "data": {
    *     ...
    *   }
    * }
    * </pre>
    *
    * @param operInput OperationIF input object
    * @return OperationIF output object
    */
   @Override
   protected OperationIF read(OperationIF operInput) {
      String METHOD = Thread.currentThread().getStackTrace()[1].getMethodName();
      String contentUid = null;
      OperationIF operOutput = null;
      JSONObject jsonContent = null;
      JSONObject jsonWrapData = null;

      _logger.entering(CLASS, METHOD);

      if (_logger.isLoggable(DEBUG_LEVEL)) {
         _logger.log(DEBUG_LEVEL, "input=''{0}'', json=''{1}''",
            new Object[]{operInput != null ? operInput.toString() : NULL,
               operInput.getJSON() != null ? operInput.getJSON().toString() : NULL});
      }

      operOutput = new Operation(OperationIF.TYPE.READ);

      contentUid = JSON.getString(operInput.getJSON(), ConstantsIF.UID);

      if (!STR.isEmpty(contentUid)) {
         jsonContent = this.readImpl(contentUid);
      }

      if (jsonContent == null) {
         jsonContent = new JSONObject();
         operOutput.setState(STATE.NOTEXIST);
      } else {
         operOutput.setState(STATE.SUCCESS);
      }

      jsonWrapData = new JSONObject();
      jsonWrapData.put(ConstantsIF.DATA, jsonContent);

      operOutput.setJSON(jsonWrapData);

      if (_logger.isLoggable(DEBUG_LEVEL)) {
         _logger.log(DEBUG_LEVEL, "output=''{0}'', json=''{1}''",
            new Object[]{operOutput != null ? operOutput.toString() : NULL,
               operOutput.getJSON() != null ? operOutput.getJSON().toString() : NULL});
      }

      _logger.exiting(CLASS, METHOD);

      return operOutput;
   }

   /**
    * Enable "replace" operation
    *
    * <pre>
    * JSON input ... replace content
    * {
    *   "uid": "...",
    *   "data": {
    *     ...
    *   }
    * }
    * </pre>
    *
    * @param operInput OperationIF input object
    * @return OperationIF output object
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
            new Object[]{operInput != null ? operInput.toString() : NULL,
               operInput.getJSON() != null ? operInput.getJSON().toString() : NULL});
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
         operOutput.setStatus("Replaced content");
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
    * Enable "delete" operation
    *
    * <pre>
    * JSON input ... delete content
    * {
    *   "uid": "..." // Resource Uid ... not the Content Uid
    * }
    * </pre>
    *
    * @param operInput OperationIF input object
    * @return OperationIF output object
    */
   @Override
   protected OperationIF delete(OperationIF operInput) {
      boolean error = false;
      OperationIF operOutput = null;
      String METHOD = Thread.currentThread().getStackTrace()[1].getMethodName();
      String resourceUid = null;
      JSONObject jsonInput = null;

      _logger.entering(CLASS, METHOD);

      if (_logger.isLoggable(DEBUG_LEVEL)) {
         _logger.log(DEBUG_LEVEL, "input=''{0}'', json=''{1}''",
            new Object[]{operInput != null ? operInput.toString() : NULL,
               operInput.getJSON() != null ? operInput.getJSON().toString() : NULL});
      }

      operOutput = new Operation(OperationIF.TYPE.DELETE);

      jsonInput = operInput.getJSON();
      resourceUid = JSON.getString(jsonInput, ConstantsIF.UID);

      try {
         this.deleteImpl(resourceUid);
      } catch (Exception ex) {
         error = true;
         operOutput.setError(true);
         operOutput.setState(STATE.ERROR);
         operOutput.setStatus(ex.getMessage());
      }

      if (!error) {
         operOutput.setState(STATE.SUCCESS);
         operOutput.setStatus("Deleted content");
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
    * =============== 
    * PRIVATE METHODS 
    * ===============
    */
   /**
    * Initialize the object
    */
   private void init() {
      String METHOD = Thread.currentThread().getStackTrace()[1].getMethodName();
      String msg = null;
      ConfigurationIF configResource = null;
      ConfigurationIF configContent = null;
      ContentServiceIF contentService = null;
      JSONObject jsonResource = null;
      JSONObject jsonContent = null;
      JSONObject jsonService = null;
      JSONArray jsonServices = null;
      Map<String, String> map = null;

      _logger.entering(CLASS, METHOD);

      /*
       * Get the "resource" JSON configuration 
       */
      configResource = _configMgr.getConfiguration(ConstantsIF.RESOURCE);

      if (configResource != null) {
         jsonResource = configResource.getJSON();
         if (jsonResource == null) {
            msg = CLASS + ": " + METHOD + ": JSON data for '" + ConstantsIF.RESOURCE + "' is null";
            this.setError(true);
         }
      } else {
         msg = CLASS + ": " + METHOD + ": Configuration for '" + ConstantsIF.RESOURCE + "' is null";
         this.setError(true);
      }

      /*
       * setup the REST Data Access Object
       */
      if (!this.isError() && _ContentServerDAO == null) {
         map = JSON.convertToParams(JSON.getObject(jsonResource, ConfigIF.CS_CONNECT));

         try {
            _ContentServerDAO = new RestDataAccess(map);
         } catch (Exception ex) {
            msg = CLASS + ": " + METHOD + ": REST DAO: " + ex.getMessage();
            this.setError(true);
         }
      }

      /*
       * setup the Mongo Data Access Object
       */
      if (!this.isError() && _MongoDAO == null) {
         map = JSON.convertToParams(JSON.getObject(jsonResource, ConfigIF.RS_NOSQL));

         try {
            _MongoDAO = MongoFactory.getInstance(map);
         } catch (Exception ex) {
            msg = CLASS + ": " + METHOD + ": Mongo DAO:" + ex.getMessage();
            this.setError(true);
         }
      }

      /*
       * Get the "content" JSON configuration
       */
      if (!this.isError()) {
         configContent = _configMgr.getConfiguration(ConstantsIF.CONTENT);

         if (configContent != null) {
            jsonContent = configContent.getJSON();
            if (jsonContent == null) {
               msg = CLASS + ": " + METHOD + ": JSON data for '" + ConstantsIF.CONTENT + "' is null";
               this.setError(true);
            }
         } else {
            msg = CLASS + ": " + METHOD + ": Configuration for '" + ConstantsIF.CONTENT + "' is null";
            this.setError(true);
         }
      }

      /*
       * Create content services from the JSON configuration
       */
      if (!this.isError()) {
         jsonServices = JSON.getArray(jsonContent, ConstantsIF.SERVICES);
         
         if (jsonServices != null && !jsonServices.isEmpty()) {
            for (Object obj : jsonServices) {
               
               if (obj != null && obj instanceof JSONObject) {
                  jsonService = (JSONObject) obj;

                  if (JSON.getBoolean(jsonService, ConstantsIF.ENABLED)) {
                     contentService = new BasicContentService(jsonService);

                     if (!contentService.isError()) {
                        _services.put(contentService.getId(), contentService);
                     } else {
                        msg = CLASS + ": " + METHOD + 
                           "Error creating ContentService : " + contentService.getStatus();
                        this.setError(true);
                     }
                  }
               } else {
                  msg = CLASS + ": " + METHOD + 
                     ": Content Services instance is null or not JSONObject";
                  this.setError(true);
               }
            }
         } else {
            msg = CLASS + ": " + METHOD + ": Content Services array is null or empty";
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
    * Create implementation
    *
    * <pre>
    * </pre>
    *
    * @param operInput OperationIF input
    * @return OperationIF output
    */
   private String createImpl(final JSONObject jsonContent) {
      String METHOD = Thread.currentThread().getStackTrace()[1].getMethodName();
      String uid = null;
      OperationIF operCreateInput = null;
      OperationIF operCreateOutput = null;
      JSONObject jsonData = null;
      JSONObject jsonOutput = null;

      /*
       * Save "content" (JSONObject) to the Content Server. Return uid
       */
      _logger.entering(CLASS, METHOD);

      if (_logger.isLoggable(DEBUG_LEVEL)) {
         _logger.log(DEBUG_LEVEL, "json=''{0}''", new Object[]{jsonContent != null ? jsonContent.toString() : NULL});
      }

      if (jsonContent != null && !jsonContent.isEmpty()) {
         jsonData = new JSONObject();
         jsonData.put(ConstantsIF.DATA, jsonContent);

         operCreateInput = new Operation(OperationIF.TYPE.CREATE);
         operCreateInput.setJSON(jsonData);

         operCreateOutput = _ContentServerDAO.execute(operCreateInput);

         jsonOutput = operCreateOutput.getJSON();

         uid = JSON.getString(jsonOutput, ConstantsIF.UID);
      }

      _logger.exiting(CLASS, METHOD);

      return uid;
   }

   /**
    * Read implementation
    *
    * <pre>
    * </pre>
    *
    * @param operInput OperationIF input
    * @return OperationIF output
    */
   private JSONObject readImpl(final String contentUid) {
      String METHOD = Thread.currentThread().getStackTrace()[1].getMethodName();
      OperationIF operReadInput = null;
      OperationIF operReadOutput = null;
      JSONObject jsonData = null;
      JSONObject jsonOutput = null;
      JSONObject jsonReturn = null;

      /*
       * Get "content" (JSONObject) from the Content Server, using the uid
       */
      _logger.entering(CLASS, METHOD);

      if (_logger.isLoggable(DEBUG_LEVEL)) {
         _logger.log(DEBUG_LEVEL, "contentUid=''{0}''", new Object[]{contentUid != null ? contentUid : NULL});
      }

      if (!STR.isEmpty(contentUid)) {
         jsonData = new JSONObject();
         jsonData.put(ConstantsIF.UID, contentUid);

         operReadInput = new Operation(OperationIF.TYPE.READ);
         operReadInput.setJSON(jsonData);

         operReadOutput = _ContentServerDAO.execute(operReadInput);

         jsonOutput = operReadOutput.getJSON();

         jsonReturn = JSON.getObject(jsonOutput, ConstantsIF.DATA);
      }

      _logger.exiting(CLASS, METHOD);

      return jsonReturn;
   }

   /**
    * Replace implementation
    *
    * <pre>
    * Read the existing entry using the "uid"
    * If the "content" attribute value exists, (uid for the Content Server)
    * - Replace the "data" on the Content Server
    * Else
    * - Create the "data" on the Content Server
    * - Set the "content" attribute with the uid from the Content Server
    * </pre>
    *
    * @param operInput OperationIF input
    * @return OperationIF output
    */
   private void replaceImpl(final String resourceUid, final JSONObject jsonContent) throws Exception {
      String METHOD = Thread.currentThread().getStackTrace()[1].getMethodName();
      String contentUid = null;
      OperationIF operReadInput = null;
      OperationIF operReadOutput = null;
      OperationIF operReplaceInput = null;
      OperationIF operReplaceOutput = null;
      JSONObject jsonInput = null;
      JSONObject jsonOutput = null;
      JSONObject jsonData = null;

      _logger.entering(CLASS, METHOD);

      if (_logger.isLoggable(DEBUG_LEVEL)) {
         _logger.log(DEBUG_LEVEL, "resourceUid=''{0}'', json=''{1}''", new Object[]{
            resourceUid != null ? resourceUid : NULL, jsonContent != null ? jsonContent.toString() : NULL});
      }

      jsonInput = new JSONObject();
      jsonInput.put(ConstantsIF.UID, resourceUid);

      operReadInput = new Operation(OperationIF.TYPE.READ);
      operReadInput.setJSON(jsonInput);

      this.setDatabaseAndCollection(operReadInput, ConfigIF.RS_NOSQL_DATABASE,
         ConfigIF.RS_NOSQL_COLLECTIONS_RESOURCES_NAME);

      operReadOutput = _MongoDAO.execute(operReadInput);

      if (operReadOutput != null && !operReadOutput.isError()) {
         jsonOutput = operReadOutput.getJSON();

         jsonData = JSON.getObject(jsonOutput, ConstantsIF.DATA);

         if (jsonData != null) {
            if (jsonData.containsKey(ConstantsIF.CONTENT)) // REPLACE CONTENT
            {
               contentUid = JSON.getString(jsonData, ConstantsIF.CONTENT);

               jsonInput = new JSONObject();
               jsonInput.put(ConstantsIF.UID, contentUid);
               jsonInput.put(ConstantsIF.DATA, jsonContent);

               operReplaceInput = new Operation(OperationIF.TYPE.REPLACE);
               operReplaceInput.setJSON(jsonInput);

               operReplaceOutput = _ContentServerDAO.execute(operReplaceInput);

               if (operReplaceOutput.isError()) {
                  throw new Exception(
                     METHOD + ": " + operReplaceOutput.getState().toString() + ": " + operReplaceOutput.getStatus());
               }
            } else // CREATE CONTENT
            {
               contentUid = this.createImpl(jsonContent);

               jsonData.put(ConstantsIF.CONTENT, contentUid);

               jsonInput = new JSONObject();
               jsonInput.put(ConstantsIF.UID, resourceUid);
               jsonInput.put(ConstantsIF.DATA, jsonData);

               operReplaceInput = new Operation(OperationIF.TYPE.REPLACE);
               operReplaceInput.setJSON(jsonInput);

               this.setDatabaseAndCollection(operReplaceInput, ConfigIF.RS_NOSQL_DATABASE,
                  ConfigIF.RS_NOSQL_COLLECTIONS_RESOURCES_NAME);

               operReplaceOutput = _MongoDAO.execute(operReplaceInput);

               if (operReplaceOutput.isError()) {
                  throw new Exception(
                     METHOD + ": " + operReplaceOutput.getState().toString() + ": " + operReplaceOutput.getStatus());
               }
            }
         } else {
            throw new Exception(METHOD + ": JSON 'data' from read is null");
         }
      } else {
         throw new Exception(METHOD + ": " + operReadOutput.getState().toString() + ": " + operReadOutput.getStatus());
      }

      _logger.exiting(CLASS, METHOD);

      return;
   }

   /**
    * Delete implementation
    *
    * <pre>
    * Read the existing entry using the Resource "uid"
    * If the "content" attribute value exists, (uid for the Content Server)
    * - Delete the "data" on the Content Server
    * </pre>
    *
    * @param operInput OperationIF input
    * @return OperationIF output
    */
   private void deleteImpl(final String resourceUid) throws Exception {
      String METHOD = Thread.currentThread().getStackTrace()[1].getMethodName();
      String contentUid = null;
      OperationIF operReadInput = null;
      OperationIF operReadOutput = null;
      OperationIF operDeleteInput = null;
      OperationIF operDeleteOutput = null;
      OperationIF operReplaceInput = null;
      OperationIF operReplaceOutput = null;
      JSONObject jsonInput = null;
      JSONObject jsonOutput = null;
      JSONObject jsonData = null;

      _logger.entering(CLASS, METHOD);

      if (_logger.isLoggable(DEBUG_LEVEL)) {
         _logger.log(DEBUG_LEVEL, "resourceUid=''{0}''", new Object[]{resourceUid != null ? resourceUid : NULL});
      }

      jsonInput = new JSONObject();
      jsonInput.put(ConstantsIF.UID, resourceUid);

      operReadInput = new Operation(OperationIF.TYPE.READ);
      operReadInput.setJSON(jsonInput);

      this.setDatabaseAndCollection(operReadInput, ConfigIF.RS_NOSQL_DATABASE,
         ConfigIF.RS_NOSQL_COLLECTIONS_RESOURCES_NAME);

      operReadOutput = _MongoDAO.execute(operReadInput);

      if (operReadOutput != null && !operReadOutput.isError()) {
         jsonOutput = operReadOutput.getJSON();

         jsonData = JSON.getObject(jsonOutput, ConstantsIF.DATA);

         if (jsonData != null) {
            if (jsonData.containsKey(ConstantsIF.CONTENT)) // DELETE CONTENT
            {
               contentUid = JSON.getString(jsonData, ConstantsIF.CONTENT);

               jsonInput = new JSONObject();
               jsonInput.put(ConstantsIF.UID, contentUid);

               operDeleteInput = new Operation(OperationIF.TYPE.DELETE);
               operDeleteInput.setJSON(jsonInput);

               operDeleteOutput = _ContentServerDAO.execute(operDeleteInput);

               if (operDeleteOutput.isError()) {
                  throw new Exception(
                     METHOD + ": " + operDeleteOutput.getState().toString() + ": " + operDeleteOutput.getStatus());
               }

               /*
                * Update the "resource" to remove the "content" reference
                */
               jsonData.remove(ConstantsIF.CONTENT);

               jsonInput = new JSONObject();
               jsonInput.put(ConstantsIF.UID, resourceUid);
               jsonInput.put(ConstantsIF.DATA, jsonData);

               operReplaceInput = new Operation(OperationIF.TYPE.REPLACE);
               operReplaceInput.setJSON(jsonInput);

               this.setDatabaseAndCollection(operReplaceInput, ConfigIF.RS_NOSQL_DATABASE,
                  ConfigIF.RS_NOSQL_COLLECTIONS_RESOURCES_NAME);

               operReplaceOutput = _MongoDAO.execute(operReplaceInput);

               if (operReplaceOutput.isError()) {
                  throw new Exception(
                     METHOD + ": " + operReplaceOutput.getState().toString() + ": " + operReplaceOutput.getStatus());
               }
            }
         }
      } else {
         throw new Exception(METHOD + ": " + operReadOutput.getState().toString() + ": " + operReadOutput.getStatus());
      }

      _logger.exiting(CLASS, METHOD);

      return;
   }
}
