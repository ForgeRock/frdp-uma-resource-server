/*
 * Copyright (c) 2018-2020, ForgeRock, Inc., All rights reserved
 * Use subject to license terms.
 */
package com.forgerock.frdp.resourceserver.handler;

import com.forgerock.frdp.common.ConstantsIF;
import com.forgerock.frdp.config.ConfigurationIF;
import com.forgerock.frdp.config.ConfigurationManagerIF;
import com.forgerock.frdp.dao.OperationIF;
import com.forgerock.frdp.handler.HandlerManagerIF;
import com.forgerock.frdp.resourceserver.ConfigIF;
import com.forgerock.frdp.resourceserver.dao.AMRestDataAccess;
import com.forgerock.frdp.utils.JSON;
import com.forgerock.frdp.utils.STR;

import java.util.Map;
import java.util.logging.Level;
import org.json.simple.JSONObject;

/**
 * Access Manager OAuth2 Handler, extends JaxRS Handler
 *
 * @author Scott Fehrman, ForgeRock, Inc.
 */
public class AMOAuth2Handler extends JaxrsHandler {

   private final String CLASS = this.getClass().getName();

   /**
    * Constructor
    *
    * @param configMgr ConfigurationManagerIF management of configurations
    * @param handlerMgr HandlerManagerIF handler manager
    */
   public AMOAuth2Handler(final ConfigurationManagerIF configMgr, final HandlerManagerIF handlerMgr) {
      super(configMgr, handlerMgr);

      String METHOD = "AMOAuth2Handler(configMgr, handlerMgr)";

      _logger.entering(CLASS, METHOD);

      this.init();

      _logger.exiting(CLASS, METHOD);

      return;
   }

   /*
    * ================= PROTECTED METHODS =================
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
      String path = null;
      JSONObject jsonInput = null;

      _logger.entering(CLASS, METHOD);

      if (oper == null) {
         throw new Exception("Operation object is null");
      }

      jsonInput = oper.getJSON();
      if (jsonInput == null || jsonInput.isEmpty()) {
         throw new Exception("JSON Input is null or empty");
      }

      // Update the Path
      path = JSON.getString(jsonInput, ConstantsIF.PATH);

      if (STR.isEmpty(path)) {
         jsonInput.put(ConstantsIF.PATH, this.getConfigValue(ConstantsIF.RESOURCE, ConfigIF.AS_OAUTH2_PATH));
      } else {
         jsonInput.put(ConstantsIF.PATH,
            this.getConfigValue(ConstantsIF.RESOURCE, ConfigIF.AS_OAUTH2_PATH) + "/" + path);
      }

      _logger.exiting(CLASS, METHOD);

      return;
   }

   /**
    * Enable "read" operation
    *
    * @param operInput OperationIF input object
    * @return OperationIF output object
    */
   @Override
   protected OperationIF read(final OperationIF operInput) // GET
   {
      String METHOD = Thread.currentThread().getStackTrace()[1].getMethodName();
      OperationIF operOutput = null;

      _logger.entering(CLASS, METHOD);

      if (_logger.isLoggable(DEBUG_LEVEL)) {
         _logger.log(DEBUG_LEVEL, "input=''{0}'', json=''{1}''",
            new Object[]{operInput != null ? operInput.toString() : NULL,
               operInput.getJSON() != null ? operInput.getJSON().toString() : NULL});
      }

      operOutput = this.readImpl(operInput);

      _logger.exiting(CLASS, METHOD);

      return operOutput;
   }

   /*
    * =============== PRIVATE METHODS ===============
    */
   /**
    * Read implementation
    *
    * <pre>
    * JSON output ...
    * {
    *   "data": {
    *     ...
    *   }
    * }
    * </pre>
    *
    * @param operInput OperationIF input
    * @return OperationIF output
    */
   private OperationIF readImpl(final OperationIF operInput) {
      String METHOD = Thread.currentThread().getStackTrace()[1].getMethodName();
      OperationIF operOutput = null;
      JSONObject jsonWrapData = null;

      _logger.entering(CLASS, METHOD);

      operOutput = _AuthzServerDAO.execute(operInput);

      jsonWrapData = new JSONObject();
      jsonWrapData.put(ConstantsIF.DATA, operOutput.getJSON());

      operOutput.setJSON(jsonWrapData);

      _logger.exiting(CLASS, METHOD);

      return operOutput;
   }

   /**
    * Initialize the object
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
       * setup the REST Data Access Object, AM Authorization Server
       * get Configuration from the Config Manager, get JSON from Configuration
       * convert the JSON data to parameters
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
}
