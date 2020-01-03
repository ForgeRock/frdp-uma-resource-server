/*
 * Copyright (c) 2018-2019, ForgeRock, Inc., All rights reserved
 * Use subject to license terms.
 */

package com.forgerock.frdp.resourceserver.handler;

import com.forgerock.frdp.common.ConstantsIF;
import com.forgerock.frdp.dao.OperationIF;
import com.forgerock.frdp.handler.HandlerManagerIF;
import com.forgerock.frdp.resourceserver.ConfigIF;
import com.forgerock.frdp.resourceserver.dao.AMRestDataAccess;
import com.forgerock.frdp.utils.JSON;
import com.forgerock.frdp.utils.STR;
import java.util.logging.Level;
import org.json.simple.JSONObject;

/**
 * Access Manager Outh2 Handler, extends JaxRS Handler
 * 
 * @author Scott Fehrman, ForgeRock, Inc.
 */
public class AMOAuth2Handler extends JaxrsHandler {

   private final String CLASS = this.getClass().getName();

   /**
    * Constructor
    * 
    * @param config     JSONObject configuration data
    * @param handlerMgr HandlerManagerIF handler manager
    */
   public AMOAuth2Handler(final JSONObject config, final HandlerManagerIF handlerMgr) {
      super(config, handlerMgr);

      String METHOD = "AMOAuth2Handler(config, handlerMgr)";

      _logger.entering(CLASS, METHOD);

      this.init();

      _logger.exiting(CLASS, METHOD);

      return;
   }

   /*
    * ================= PROTECTED METHODS =================
    */
   /**
    * Validate the OperationIF object, overides the subclass
    * 
    * @param oper OperationIF
    * @throws Exception
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
         jsonInput.put(ConstantsIF.PATH, this.getConfigValue(ConfigIF.AS_OAUTH2_PATH));
      } else {
         jsonInput.put(ConstantsIF.PATH, this.getConfigValue(ConfigIF.AS_OAUTH2_PATH) + "/" + path);
      }

      _logger.exiting(CLASS, METHOD);

      return;
   }

   /**
    * Enable  "read" operation
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
               new Object[] { operInput != null ? operInput.toString() : NULL,
                     operInput.getJSON() != null ? operInput.getJSON().toString() : NULL });
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

      _logger.entering(CLASS, METHOD);

      /*
       * setup the REST Data Access Object, AM Authorization Server
       */
      if (_AuthzServerDAO == null) {
         try {
            _AuthzServerDAO = new AMRestDataAccess(JSON.convertToParams(JSON.getObject(_config, ConfigIF.AS_CONNECT)));
         } catch (Exception ex) {
            this.setError(true);
            this.setState(STATE.ERROR);
            this.setStatus(CLASS + ": " + METHOD + ": REST DAO: " + ex.getMessage());
            _logger.log(Level.SEVERE, this.getStatus());
         }
      }

      if (!this.isError()) {
         this.setState(STATE.READY);
      }

      _logger.exiting(CLASS, METHOD);

      return;
   }
}
