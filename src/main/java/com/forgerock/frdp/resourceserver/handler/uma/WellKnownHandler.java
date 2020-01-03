/*
 * Copyright (c) 2018-2020, ForgeRock, Inc., All rights reserved
 * Use subject to license terms.
 */

package com.forgerock.frdp.resourceserver.handler.uma;

import com.forgerock.frdp.common.ConstantsIF;
import com.forgerock.frdp.dao.Operation;
import com.forgerock.frdp.dao.OperationIF;
import com.forgerock.frdp.handler.HandlerManagerIF;
import com.forgerock.frdp.resourceserver.ConfigIF;
import com.forgerock.frdp.resourceserver.dao.AMRestDataAccess;
import com.forgerock.frdp.resourceserver.handler.JaxrsHandler;
import com.forgerock.frdp.utils.JSON;
import java.util.logging.Level;
import org.json.simple.JSONObject;

/**
 * Get the "well known" information for UMA
 * 
 * <pre>
 * This class implements the following operations:
 * - read: get the "well known" information
 * </pre>
 * 
 * @author Scott Fehrman, ForgeRock, Inc.
 */
public class WellKnownHandler extends JaxrsHandler {

   private final String CLASS = this.getClass().getName();

   /**
    * Constructor
    *
    * @param config     JSONObject containing configuration data
    * @param handlerMgr HandlerManagerIF provides management of Handlers
    */
   public WellKnownHandler(final JSONObject config, final HandlerManagerIF handlerMgr) {
      super(config, handlerMgr);

      String METHOD = "WellKnownHandler(config, handlerMgr)";

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
      if (jsonInput == null) {
         oper.setJSON(new JSONObject());
      }

      _logger.exiting(CLASS, METHOD);

      return;
   }

   /**
    * Override interface to support the "read" operation
    * 
    * @param operInput OperationIF input for read operation
    * @return OperationIF output from read operation
    */
   @Override
   protected OperationIF read(final OperationIF operInput) // POST
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
    * Get the well known information:
    * https://.../openam/uma/.well-known/uma2-configuration
    * 
    * @param operInput OperationIF input
    * @return OperationIF output
    */
   private OperationIF readImpl(final OperationIF operInput) {
      boolean error = false;
      String METHOD = Thread.currentThread().getStackTrace()[1].getMethodName();
      String msg = null;
      OperationIF operOutput = null;

      _logger.entering(CLASS, METHOD);

      try {
         operInput.getJSON().put(ConstantsIF.PATH, this.getConfigValue(ConfigIF.AS_UMA_WELL_KNOWN_PATH));
      } catch (Exception ex) {
         msg = METHOD + ": Could not set 'path': " + ex.getMessage();
         error = true;
      }

      if (!error) {
         operOutput = _AuthzServerDAO.execute(operInput);
      } else {
         operOutput = new Operation(OperationIF.TYPE.READ);
         operOutput.setState(STATE.ERROR);
         operOutput.setStatus(msg);

         this.setState(STATE.ERROR);
         this.setStatus(msg);
      }

      _logger.exiting(CLASS, METHOD);

      return operOutput;
   }

   /**
    * Initialize object instance
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
