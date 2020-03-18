/*
 * Copyright (c) 2018-2020, ForgeRock, Inc., All rights reserved
 * Use subject to license terms.
 */
package com.forgerock.frdp.resourceserver.handler;

import com.forgerock.frdp.common.ConstantsIF;
import com.forgerock.frdp.config.ConfigurationIF;
import com.forgerock.frdp.config.ConfigurationManagerIF;
import com.forgerock.frdp.dao.DataAccessIF;
import com.forgerock.frdp.dao.Operation;
import com.forgerock.frdp.dao.OperationIF;
import com.forgerock.frdp.dao.rest.RestDataAccess;
import com.forgerock.frdp.handler.HandlerManagerIF;
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
   private DataAccessIF _RestDAO = null;

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
    * Validate the OperationIF object.
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

      /*
       * All input must have:
       * - "id" ... the Content Service Identifier
       */
      switch (oper.getType()) {
         case CREATE: {
            this.checkAttr(jsonInput, ConstantsIF.ID);
            break;
         }
         case READ: {
            this.checkAttr(jsonInput, ConstantsIF.ID);
            this.checkAttr(jsonInput, ConstantsIF.URI);
            break;
         }
         case REPLACE: {
            this.checkAttr(jsonInput, ConstantsIF.ID);
            this.checkAttr(jsonInput, ConstantsIF.URI);
            break;
         }
         case DELETE: {
            this.checkAttr(jsonInput, ConstantsIF.ID);
            this.checkAttr(jsonInput, ConstantsIF.URI);
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
    * Create content.
    *
    * Save the "content" based on the specified Content Service Identifier ("id"
    * attribute). The actual "content" may be either raw JSON ("data" object) or
    * a reference to external content ("uri" attribute).
    *
    * <b>JSON input</b> ...
    * <pre>
    * {                         | {
    *    "id": "default",       |    "id": "refonly",
    *    "data": { ... }        |    "uri": "http://..."
    * }                         | }
    * </pre>
    *
    * <b>JSON output</b> ...
    * <pre>
    * {
    *    "id": "default",
    *    "uri": "http://..."
    * }
    * </pre>
    *
    * @param operInput OperationIF input object
    * @return OperationIF output object
    */
   @Override
   protected OperationIF create(OperationIF operInput) {
      String METHOD = Thread.currentThread().getStackTrace()[1].getMethodName();
      String msg = null;
      OperationIF operOutput = null;

      _logger.entering(CLASS, METHOD);

      if (_logger.isLoggable(DEBUG_LEVEL)) {
         _logger.log(DEBUG_LEVEL, "input=''{0}'', json=''{1}''",
            new Object[]{operInput != null ? operInput.toString() : NULL,
               operInput.getJSON() != null ? operInput.getJSON().toString() : NULL});
      }

      try {
         operOutput = this.operationImpl(operInput);
      } catch (Exception ex) {
         msg = ex.getMessage();
      }

      if (operOutput != null) {
         if (STR.isEmpty(JSON.getString(operOutput.getJSON(), ConstantsIF.URI))) {
            msg = "Operation output 'uri' is empty";
         }
      } else {
         operOutput = new Operation(operInput.getType());
         msg = "Operation output is null";
      }

      if (msg != null) {
         operOutput.setState(STATE.ERROR);
         operOutput.setStatus(msg);
         operOutput.setError(true);
      }

      _logger.exiting(CLASS, METHOD);

      return operOutput;
   }

   /**
    * Read content.
    *
    * Read the content based on the specified Content Service Identifier ("id"
    * attribute) and a specific URI. The service may return raw JSON ("data"
    * object) or a URI ("uri" attribute).
    *
    * <b>JSON input</b> ...
    * <pre>
    * {
    *    "id": "default",
    *    "uri": "http://..."
    * }
    * </pre>
    *
    * <b>JSON output</b> ...
    * <pre>
    * {                     | {
    *    "id": "default",   |    "id": "refonly",
    *    "data": { ... }    |    "uri": "http://..."
    * }                     | }
    * </pre>
    *
    * @param operInput OperationIF input object
    * @return OperationIF output object
    */
   @Override
   protected OperationIF read(OperationIF operInput) {
      String METHOD = Thread.currentThread().getStackTrace()[1].getMethodName();
      String msg = null;
      OperationIF operOutput = null;

      _logger.entering(CLASS, METHOD);

      if (_logger.isLoggable(DEBUG_LEVEL)) {
         _logger.log(DEBUG_LEVEL, "input=''{0}'', json=''{1}''",
            new Object[]{operInput != null ? operInput.toString() : NULL,
               operInput.getJSON() != null ? operInput.getJSON().toString() : NULL});
      }

      try {
         operOutput = this.operationImpl(operInput);
      } catch (Exception ex) {
         msg = ex.getMessage();
      }

      if (operOutput == null) {
         operOutput = new Operation(operInput.getType());
      }

      if (msg != null) {
         operOutput.setState(STATE.ERROR);
         operOutput.setStatus(msg);
         operOutput.setError(true);
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
    * Replace content.
    *
    * Replace the "content" based on the specified Content Service Identifier
    * ("id" attribute). The actual "content" may be either raw JSON ("data"
    * object) or a reference to external content ("uri" attribute).
    *
    * <b>JSON input</b> ...
    * <pre>
    * {                             | {
    *    "id": "default",           |    "id":"refonly",
    *    "uri": "http://...",       |    "uri": "http://..."
    *    "data": { ... }            | }
    * }                             |
    * </pre>
    *
    * @param operInput OperationIF input object
    * @return OperationIF output object
    */
   @Override
   protected OperationIF replace(OperationIF operInput) {
      String METHOD = Thread.currentThread().getStackTrace()[1].getMethodName();
      String msg = null;
      OperationIF operOutput = null;
      JSONObject jsonInput = null;
      JSONObject jsonContent = null;

      _logger.entering(CLASS, METHOD);

      if (_logger.isLoggable(DEBUG_LEVEL)) {
         _logger.log(DEBUG_LEVEL, "input=''{0}'', json=''{1}''",
            new Object[]{operInput != null ? operInput.toString() : NULL,
               operInput.getJSON() != null ? operInput.getJSON().toString() : NULL});
      }

      try {
         operOutput = this.operationImpl(operInput);
      } catch (Exception ex) {
         msg = ex.getMessage();
      }

      if (operOutput == null) {
         operOutput = new Operation(operInput.getType());
      }

      if (msg != null) {
         operOutput.setState(STATE.ERROR);
         operOutput.setStatus(msg);
         operOutput.setError(true);
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
    * Delete content.
    *
    * Delete the content based on the specified Content Service Identifier ("id"
    * attribute) and a specific URI ("uri" attribute).
    *
    * <b>JSON input</b> ...
    * <pre>
    * {
    *    "id": "default",
    *    "uri": "http://..."
    * }
    * </pre>
    *
    * @param operInput OperationIF input object
    * @return OperationIF output object
    */
   @Override
   protected OperationIF delete(OperationIF operInput) {
      String METHOD = Thread.currentThread().getStackTrace()[1].getMethodName();
      String msg = null;
      OperationIF operOutput = null;

      _logger.entering(CLASS, METHOD);

      if (_logger.isLoggable(DEBUG_LEVEL)) {
         _logger.log(DEBUG_LEVEL, "input=''{0}'', json=''{1}''",
            new Object[]{operInput != null ? operInput.toString() : NULL,
               operInput.getJSON() != null ? operInput.getJSON().toString() : NULL});
      }

      try {
         operOutput = this.operationImpl(operInput);
      } catch (Exception ex) {
         msg = ex.getMessage();
      }

      if (operOutput == null) {
         operOutput = new Operation(operInput.getType());
      }

      if (msg != null) {
         operOutput.setState(STATE.ERROR);
         operOutput.setStatus(msg);
         operOutput.setError(true);
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
            msg = CLASS + ": " + METHOD + ": JSON data for '"
               + ConstantsIF.RESOURCE + "' is null";
            this.setError(true);
         }
      } else {
         msg = CLASS + ": " + METHOD + ": Configuration for '"
            + ConstantsIF.RESOURCE + "' is null";
         this.setError(true);
      }

      /*
       * setup the REST Data Access Object, no parameters
       */
      if (!this.isError() && _RestDAO == null) {

         try {
            _RestDAO = new RestDataAccess();
         } catch (Exception ex) {
            msg = CLASS + ": " + METHOD + ": REST DAO: " + ex.getMessage();
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
               msg = CLASS + ": " + METHOD + ": JSON data for '"
                  + ConstantsIF.CONTENT + "' is null";
               this.setError(true);
            }
         } else {
            msg = CLASS + ": " + METHOD + ": Configuration for '"
               + ConstantsIF.CONTENT + "' is null";
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
                        msg = CLASS + ": " + METHOD
                           + "Error creating ContentService : " + contentService.getStatus();
                        this.setError(true);
                     }
                  }
               } else {
                  msg = CLASS + ": " + METHOD
                     + ": Content Services instance is null or not JSONObject";
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
    * Process the operation with the Content Service.
    *
    * @param operInput OperationIF input request information
    * @return OperationIF operational response
    * @throws Exception Problem processing the Content
    */
   private OperationIF operationImpl(final OperationIF operInput) throws Exception {
      String METHOD = Thread.currentThread().getStackTrace()[1].getMethodName();
      String csId = null; // Content Service Identifier
      String csURI = null;
      String inputURI = null;
      String dataURI = null;
      String action = null;
      String contentName = null;
      JSONObject jsonInput = null;
      JSONObject jsonSrvcOper = null;
      JSONObject jsonData = null;
      JSONObject jsonHdrs = null;
      JSONObject jsonDAOInput = null;
      JSONObject jsonDAOOutput = null;
      OperationIF operOutput = null;
      OperationIF operDAOInput = null;
      OperationIF operDAOOutput = null;
      ContentServiceIF service = null;
      OperationIF operService = null;
      Map<String, String> mapHeaders = null;

      /*
       * CREATE: JSON input formats:
       * {                         | {
       *    "id": "default",       |    "id": "refonly",
       *    "data": { ... }        |    "uri": "http://..."
       * }                         | }
       * READ: JSON input formats:
       * {
       *    "id": "default",
       *    "uri": "http://..."
       * }
       * REPLACE: JSON input formats:
       * {                         | {
       *    "id": "default",       |    "id": "refonly",
       *    "uri": "http://...",   |    "uri": "http://..."
       *    "data": { ... }        |    "data": { "uri": "http://..." }
       * }                         | }
       * DELETE: JSON input formats:
       * {
       *    "id": "default",
       *    "uri": "http://..."
       * }
       * =============================
       * CREATE and REPLACE: JSON output formats:
       * {
       *    "id": "default",
       *    "uri": "http://..."
       * }
       */
      _logger.entering(CLASS, METHOD);

      jsonInput = operInput.getJSON();

      /*
       * Get Content Service, using the "id"
       * Get Opertion from the Content Service
       * Get Action from the Operation
       * Get Header(s) from Operation, Map object (if exists) .. make JSON 
       */
      csId = JSON.getString(jsonInput, ConstantsIF.ID);

      if (_services.containsKey(csId)) {
         service = _services.get(csId);

         if (service != null && !service.isError()) {

            contentName = service.getParam(ConstantsIF.CONTENT);

            if (service.hasOperation(operInput.getType())) {
               operService = service.getOperation(operInput.getType());
               action = operService.getParam(ConstantsIF.ACTION);

               if (!STR.isEmpty(action)) {
                  jsonSrvcOper = operService.getJSON();
                  if (jsonSrvcOper != null) {
                     jsonHdrs = JSON.getObject(jsonSrvcOper, ConstantsIF.HEADERS);
                  }
               } else {
                  this.abort(METHOD, "Content Service '" + csId + "', operation '"
                     + operInput.getType().toString() + "' has an empty '"
                     + ConstantsIF.ACTION + "' parameter");
               }
            } else {
               this.abort(METHOD, "Content Service '" + csId + "' does not support '"
                  + operInput.getType().toString() + "' operation");
            }
         } else {
            this.abort(METHOD, "Content Service is null or has an error for '" + csId + "'");
         }
      } else {
         this.abort(METHOD, "Content Service does not exist for '" + csId + "'");
      }

      /*
       * Process the operation, based on the type, then based on the action
       */
      jsonDAOInput = new JSONObject();
      jsonDAOOutput = new JSONObject();

      if (jsonHdrs != null) {
         jsonDAOInput.put(ConstantsIF.HEADERS, jsonHdrs);
      }

      switch (operInput.getType()) {
         case CREATE: {
            switch (action) {
               case ConstantsIF.POST: {
                  /*
                   * Send the JSON data to the Content Service
                   * 1: Input must have a non-null "data" object
                   * 2: Configuration must have required "uri" attribute
                   * The returned Location URI is added to the JSON output:
                   * { 
                   *   "id": "default",
                   *   "uri": "http://..."  
                   * }
                   */
                  jsonData = JSON.getObject(jsonInput, ConstantsIF.DATA);
                  if (jsonData == null) {
                     this.abort(METHOD, "Content Service '" + csId + "', operation '"
                        + operInput.getType().toString() + "', action '"
                        + action + "', Input has a null 'data' object");
                  }

                  jsonDAOInput.put(ConstantsIF.DATA, jsonData);

                  csURI = operService.getParam(ConstantsIF.URI);
                  if (STR.isEmpty(csURI)) {
                     this.abort(METHOD, "Content Service '" + csId + "', operation '"
                        + operInput.getType().toString() + "', action '"
                        + action + "', Required attribute 'uri' is empty");
                  }

                  jsonDAOInput.put(ConstantsIF.URI, csURI);

                  operDAOInput = new Operation(operInput.getType());
                  operDAOInput.setJSON(jsonDAOInput);

                  /*
                   * JSON input:
                   * {
                   *   "data": { ... },
                   *   "uri": "http://...",
                   *   "headers": { "X-FRDP-FOO": "foo", "X-FRDP-BAR": "bar" }
                   * }
                   */
                  operDAOOutput = _RestDAO.execute(operDAOInput);

                  if (operDAOOutput.isError()) {
                     this.abort(METHOD, "Content Service '" + csId + "', operation '"
                        + operInput.getType().toString() + "', action '"
                        + action + "', DAO output error: " + operDAOOutput.getStatus());
                  } else {
                     jsonDAOOutput = operDAOOutput.getJSON();
                     if (jsonDAOOutput == null || jsonDAOOutput.isEmpty()) {
                        this.abort(METHOD, "Content Service '" + csId + "', operation '"
                           + operInput.getType().toString() + "', action '"
                           + action + "', DAO output has empty JSON");
                     }
                  }

                  /* 
                   * add the "id", Content Service identifier
                   */
                  jsonDAOOutput.put(ConstantsIF.ID, csId);

                  operOutput = new Operation(operInput.getType());
                  operOutput.setState(STATE.SUCCESS);
                  operOutput.setStatus("URI was created");
                  operOutput.setJSON(jsonDAOOutput);

                  break;
               }
               case ConstantsIF.GET: {
                  /*
                   * Validate the URI, then return it, else error
                   */
                  inputURI = JSON.getString(jsonInput, ConstantsIF.URI);

                  if (STR.isEmpty(inputURI)) {
                     this.abort(METHOD, "Content Service '" + csId + "', operation '"
                        + operInput.getType().toString() + "', action '"
                        + action + "', Required input attribute 'uri' is empty");
                  }

                  jsonDAOInput.put(ConstantsIF.URI, inputURI);

                  operDAOInput = new Operation(OperationIF.TYPE.READ);
                  operDAOInput.setJSON(jsonDAOInput);

                  /*
                   * JSON input:
                   * {
                   *   "uri": "http://..."
                   * }
                   */
                  operDAOOutput = _RestDAO.execute(operDAOInput);

                  if (operDAOOutput.isError()) {
                     this.abort(METHOD, operDAOOutput.getState().toString()
                        + ": " + operDAOOutput.getStatus());
                  }

                  /* 
                   * add the "id", Content Service identifier
                   */
                  jsonDAOOutput.put(ConstantsIF.ID, csId);
                  jsonDAOOutput.put(ConstantsIF.URI, inputURI);

                  operOutput = new Operation(operInput.getType());
                  operOutput.setState(STATE.SUCCESS);
                  operOutput.setStatus("URI was found");
                  operOutput.setJSON(jsonDAOOutput);

                  break;
               }
               case ConstantsIF.REFERENCE: {

                  /*
                   * check for 'uri' attribute, return 'uri'
                   */
                  inputURI = JSON.getString(jsonInput, ConstantsIF.URI);

                  if (STR.isEmpty(inputURI)) {
                     this.abort(METHOD, "Content Service '" + csId + "', operation '"
                        + operInput.getType().toString() + "', action '"
                        + action + "', Required input attribute 'uri' is empty");
                  }


                  /* 
                   * add the "id", Content Service identifier
                   */
                  jsonDAOOutput.put(ConstantsIF.ID, csId);
                  jsonDAOOutput.put(ConstantsIF.URI, inputURI);

                  operOutput = new Operation(operInput.getType());
                  operOutput.setState(STATE.SUCCESS);
                  operOutput.setStatus("URI is reference");
                  operOutput.setJSON(jsonDAOOutput);

                  break;
               }
               default: {
                  this.abort(CLASS + ": " + METHOD, "Content Service '"
                     + csId + "', operation '"
                     + operInput.getType().toString() + "' has an invalid action '"
                     + action + "'");
               }
            }
            break;
         }
         case READ: {
            /*
             * check for 'uri' attribute
             */
            inputURI = JSON.getString(jsonInput, ConstantsIF.URI);

            if (STR.isEmpty(inputURI)) {
               this.abort(CLASS + ": " + METHOD, "Content Service '"
                  + csId + "', operation '"
                  + operInput.getType().toString() + "', action '"
                  + action + "', Required input attribute 'uri' is empty");
            }

            jsonDAOInput.put(ConstantsIF.URI, inputURI);

            switch (action) {
               case ConstantsIF.GET: {

                  /*
                   * GET the data (JSON) from the URI 
                   * JSON input:
                   * {
                   *   "uri": "http://..."
                   * }
                   */
                  operDAOInput = new Operation(operInput.getType());
                  operDAOInput.setJSON(jsonDAOInput);

                  /*
                   * JSON output:
                   * {
                   *   "uid": "GUID",
                   *   "data": { ... },
                   *   "timestamps" : { ... }
                   * }
                   */
                  operDAOOutput = _RestDAO.execute(operDAOInput);

                  if (operDAOOutput.isError()) {
                     this.abort(CLASS + ": " + METHOD, operDAOOutput.getState().toString()
                        + ": " + operDAOOutput.getStatus());
                  }

                  jsonData = operDAOOutput.getJSON();

                  if (jsonData == null) {
                     this.abort(CLASS + ": " + METHOD, "JSON output is null, "
                        + operDAOOutput.toString());
                  }

                  jsonDAOOutput.put(ConstantsIF.ID, csId);

                  /*
                   * Output may need to be un-wrapped from a 'content' object
                   * JSON output: 
                   * {
                   *   "id": "default",
                   *   "data": { ... }
                   * }
                   */
                  if (STR.isEmpty(contentName)) {
                     jsonDAOOutput.put(ConstantsIF.DATA, jsonData);
                  } else {
                     jsonDAOOutput.put(ConstantsIF.DATA, JSON.getObject(jsonData, contentName));
                  }

                  operOutput = new Operation(operInput.getType());
                  operOutput.setState(STATE.SUCCESS);
                  operOutput.setStatus("Found JSON data");
                  operOutput.setJSON(jsonDAOOutput);

                  break;
               }
               case ConstantsIF.REFERENCE: {

                  jsonDAOOutput.put(ConstantsIF.ID, csId);
                  jsonDAOOutput.put(ConstantsIF.URI, inputURI);

                  operOutput = new Operation(operInput.getType());
                  operOutput.setState(STATE.SUCCESS);
                  operOutput.setStatus("URI is reference");
                  operOutput.setJSON(jsonDAOOutput);

                  break;
               }
               default: {
                  this.abort(METHOD, "Content Service '" + csId + "', operation '"
                     + operInput.getType().toString() + "' has an invalid action '"
                     + action + "'");
               }
            }
            break;
         }
         case REPLACE: {
            /*
             * check for 'uri' attribute and check for 'data' object
             */
            inputURI = JSON.getString(jsonInput, ConstantsIF.URI);

            if (STR.isEmpty(inputURI)) {
               this.abort(METHOD, "Content Service '" + csId + "', operation '"
                  + operInput.getType().toString() + "', action '"
                  + action + "', Required input attribute 'uri' is empty");
            }

            jsonData = JSON.getObject(jsonInput, ConstantsIF.DATA);
            if (jsonData == null) {
               this.abort(METHOD, "Content Service '" + csId + "', operation '"
                  + operInput.getType().toString() + "', action '"
                  + action + "', Input has a null 'data' object");
            }

            switch (action) {
               case ConstantsIF.PUT: {
                  /*
                   * Send the JSON data to the Content Service for replacement
                   */

                  jsonDAOInput.put(ConstantsIF.URI, inputURI);
                  jsonDAOInput.put(ConstantsIF.DATA, jsonData);

                  operDAOInput = new Operation(operInput.getType());
                  operDAOInput.setJSON(jsonDAOInput);

                  /*
                   * JSON input:
                   * {
                   *   "uri": "http://...",
                   *   "data": { ... }
                   * }
                   */
                  operDAOOutput = _RestDAO.execute(operDAOInput);

                  if (operDAOOutput.isError()) {
                     this.abort(METHOD, operDAOOutput.getState().toString()
                        + ": " + operDAOOutput.getStatus());
                  }


                  /*
                   * JSON output:
                   * {
                   *   "id": "default",
                   *   "uri": "http://..." 
                   * }
                   */
                  jsonDAOOutput.put(ConstantsIF.ID, csId);
                  jsonDAOOutput.put(ConstantsIF.URI, inputURI);

                  operOutput = new Operation(operInput.getType());
                  operOutput.setState(STATE.SUCCESS);
                  operOutput.setStatus("URI is reference");
                  operOutput.setJSON(jsonDAOOutput);

                  break;
               }
               case ConstantsIF.REFERENCE: {
                  /*
                   * add data 'uri' attribute to the output 'uri' attribute
                   * JSON input:
                   * {
                   *   "uri": "http://...",
                   *   "data": { 
                   *     "uri": "http://..."
                   *   }
                   * }
                   */
                  dataURI = JSON.getString(jsonData, ConstantsIF.URI);

                  if (STR.isEmpty(dataURI)) {
                     this.abort(METHOD, "Content Service '" + csId + "', operation '"
                        + operInput.getType().toString() + "', action '"
                        + action + "', Required input attribute 'uri', from 'data' object, is empty");
                  }

                  /*
                   * JSON output:
                   * {
                   *   "id": "default",
                   *   "uri": "http://..."  this is the "new" uri
                   * }
                   */
                  jsonDAOOutput.put(ConstantsIF.ID, csId);
                  jsonDAOOutput.put(ConstantsIF.URI, dataURI);

                  operOutput = new Operation(operInput.getType());
                  operOutput.setState(STATE.SUCCESS);
                  operOutput.setStatus("URI is reference");
                  operOutput.setJSON(jsonDAOOutput);

                  break;
               }
               default: {
                  this.abort(METHOD, "Content Service '" + csId + "', operation '"
                     + operInput.getType().toString() + "' has an invalid action '"
                     + action + "'");
               }
            }
            break;
         }
         case DELETE: {
            switch (action) {
               case ConstantsIF.DELETE: {
                  /*
                   * Delete the JSON data on the Content Service
                   * Input must have a non-null "uri" attribute
                   */
                  inputURI = JSON.getString(jsonInput, ConstantsIF.URI);

                  if (STR.isEmpty(inputURI)) {
                     this.abort(METHOD, "Content Service '" + csId + "', operation '"
                        + operInput.getType().toString() + "', action '"
                        + action + "', Required input attribute 'uri' is empty");
                  }

                  jsonDAOInput.put(ConstantsIF.URI, inputURI);

                  operDAOInput = new Operation(operInput.getType());
                  operDAOInput.setJSON(jsonDAOInput);

                  /*
                   * JSON input:
                   * {
                   *   "uri": "http://...",
                   * }
                   */
                  operDAOOutput = _RestDAO.execute(operDAOInput);

                  if (operDAOOutput.isError()) {
                     this.abort(METHOD, operDAOOutput.getState().toString()
                        + ": " + operDAOOutput.getStatus());
                  }

                  /*
                   * JSON output: {}
                   */
                  operOutput = operDAOOutput;

                  break;
               }
               case ConstantsIF.REFERENCE: {

                  /*
                   * check for 'uri' attribute
                   */
                  inputURI = JSON.getString(jsonInput, ConstantsIF.URI);

                  if (STR.isEmpty(inputURI)) {
                     this.abort(METHOD, "Content Service '" + csId + "', operation '"
                        + operInput.getType().toString() + "', action '"
                        + action + "', Required input attribute 'uri' is empty");
                  }

                  operOutput = new Operation(operInput.getType());
                  operOutput.setState(STATE.SUCCESS);
                  operOutput.setStatus("URI is reference");

                  break;
               }
               default: {
                  this.abort(METHOD, "Content Service '" + csId + "', operation '"
                     + operInput.getType().toString() + "' has an invalid action '"
                     + action + "'");
               }
            }
            break;
         }
         default: {
            this.abort(METHOD, "Unsupported operation type '"
               + operInput.getType().toString() + "'");
         }
      }

      _logger.exiting(CLASS, METHOD);

      return operOutput;
   }
}
