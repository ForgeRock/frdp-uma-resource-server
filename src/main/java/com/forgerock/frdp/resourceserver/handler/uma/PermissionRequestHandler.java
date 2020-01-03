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
import com.forgerock.frdp.utils.STR;
import java.util.logging.Level;
import org.json.simple.JSONObject;

/**
 * Permission Request Handler.  Contacts the Authorization Server (AS) to get a Permission Ticket
 * for the given resource, requesting party, client, and scopes
 * 
 * @author Scott Fehrman, ForgeRock, Inc.
 */
public class PermissionRequestHandler extends JaxrsHandler {

   private final String CLASS = this.getClass().getName();

   /**
    * Constructor
    *
    * @param config     JSONObject containing configuration data
    * @param handlerMgr HandlerManagerIF provides management of Handlers
    */
   public PermissionRequestHandler(final JSONObject config, final HandlerManagerIF handlerMgr) {
      super(config, handlerMgr);

      String METHOD = "PermissionRequestHandler(config, handlerMgr)";

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

      /*
       * Update the Path
       */
      path = JSON.getString(jsonInput, ConstantsIF.PATH);

      if (STR.isEmpty(path)) {
         jsonInput.put(ConstantsIF.PATH, this.getConfigValue(ConfigIF.AS_UMA_PERMISSION_REQUEST_PATH));
      } else {
         jsonInput.put(ConstantsIF.PATH, this.getConfigValue(ConfigIF.AS_UMA_PATH) + "/" + path);
      }

      _logger.exiting(CLASS, METHOD);

      return;
   }

   /**
    * Override interface to support the "create" operation
    * 
    * @param operInput OperationIF input for create operation
    * @return OperationIF output from create operation
    */
   @Override
   protected OperationIF create(final OperationIF operInput) // POST
   {
      String METHOD = Thread.currentThread().getStackTrace()[1].getMethodName();
      OperationIF operOutput = null;

      _logger.entering(CLASS, METHOD);

      if (_logger.isLoggable(DEBUG_LEVEL)) {
         _logger.log(DEBUG_LEVEL, "input=''{0}'', json=''{1}''",
               new Object[] { operInput != null ? operInput.toString() : NULL,
                     operInput.getJSON() != null ? operInput.getJSON().toString() : NULL });
      }

      operOutput = this.createImpl(operInput);

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
       * setup the REST Data Access Object, AM Authorization Server
       */

      if (_AuthzServerDAO == null) {
         try {
            _AuthzServerDAO = new AMRestDataAccess(JSON.convertToParams(JSON.getObject(_config, ConfigIF.AS_CONNECT)));
         } catch (Exception ex) {
            this.setError(true);
            this.setState(STATE.ERROR);
            this.setStatus(CLASS + ": " + METHOD + ": REST AMDAO: " + ex.getMessage());
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
    * Implementation of the "create" opertaion.
    * 
    * <pre>
    * Override the "operation output":
    * Add the "as_uri" attribute to the JSON data
    * - set output Type to "READ" (GET)
    * - If SUCCESS:
    *   set State to "NOTAUTHORIZED" ... will map to 401 Unauthorized
    * - Else:
    *   Set State to "WARNING" ... will map to 403 Forbidden
    * </pre>
    * 
    * @param operInput OperationIF input
    * @return OperationIF output
    */
   private OperationIF createImpl(final OperationIF operInput) {
      boolean error = false;
      String METHOD = Thread.currentThread().getStackTrace()[1].getMethodName();
      String msg = null;
      String host = null;
      OperationIF operOutput = null;
      JSONObject jsonOutput = null;
      JSONObject jsonData = null;
      JSONObject jsonHeaders = null;

      _logger.entering(CLASS, METHOD);

      try {
         host = this.getConfigValue(ConfigIF.AS_CONNECT_HOST);
      } catch (Exception ex) {
         msg = METHOD + ": Could not set 'host': " + ex.getMessage();
         error = true;
      }

      if (!error) {
         operOutput = _AuthzServerDAO.execute(operInput);

         if (operOutput.getState() == STATE.SUCCESS) {
            operOutput.setState(STATE.NOTAUTHORIZED);

            jsonData = operOutput.getJSON();

            jsonHeaders = new JSONObject();
            jsonHeaders.put(ConstantsIF.WWW_AUTHENTICATE, "UMA realm=\"" + host + "\"");

            jsonOutput = new JSONObject();
            jsonOutput.put(ConstantsIF.DATA, jsonData);
            jsonOutput.put(ConstantsIF.HEADERS, jsonHeaders);
         } else {
            operOutput.setState(STATE.WARNING);

            jsonHeaders = new JSONObject();
            jsonHeaders.put(ConstantsIF.WARNING, "199 - \"UMA Authorization Server Unreachable\"");

            jsonOutput = new JSONObject();
            jsonOutput.put(ConstantsIF.HEADERS, jsonHeaders);
         }

         operOutput.setJSON(jsonOutput);
         operOutput.setType(OperationIF.TYPE.READ);
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
}
