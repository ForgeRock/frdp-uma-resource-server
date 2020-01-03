/*
 * Copyright (c) 2018-2019, ForgeRock, Inc., All rights reserved
 * Use subject to license terms.
 */

package com.forgerock.frdp.resourceserver.handler;

import com.forgerock.frdp.common.ConstantsIF;
import com.forgerock.frdp.dao.Operation;
import com.forgerock.frdp.dao.OperationIF;
import com.forgerock.frdp.handler.HandlerManagerIF;
import com.forgerock.frdp.resourceserver.ConfigIF;
import com.forgerock.frdp.resourceserver.dao.AMRestDataAccess;
import com.forgerock.frdp.utils.JSON;
import com.forgerock.frdp.utils.STR;
import java.util.logging.Level;
import org.json.simple.JSONObject;

/**
 * Access Manager Session Handler
 * 
 * @author Scott Fehrman, ForgeRock, Inc.
 */

public class AMSessionHandler extends JaxrsHandler {

   private final String CLASS = this.getClass().getName();

   public AMSessionHandler(final JSONObject config, final HandlerManagerIF handlerMgr) {
      super(config, handlerMgr);

      String METHOD = "AMSessionHandler(config, handlerMgr)";

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
      case CREATE: {
         this.checkUserPassword(jsonInput);
         break;
      }
      case READ: {
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
    * Enable "create" operation
    *
    * <pre>
    * JSON input ...
    * {
    *   "user": "...",
    *   "password": "..."
    * }
    * JSON output ...
    * {
    *   "tokenId": "...",
    *   "successUrl": "/openam/console",
    *   "realm": "/"
    * }
    * </pre>
    * 
    * @param operInput OperationIF input
    * @return OperationIF
    */
   @Override
   protected OperationIF create(final OperationIF operInput) {
      String METHOD = Thread.currentThread().getStackTrace()[1].getMethodName();
      OperationIF operOutput = null;

      _logger.entering(CLASS, METHOD);

      if (_logger.isLoggable(DEBUG_LEVEL)) {
         _logger.log(DEBUG_LEVEL, "input=''{0}'', json=''{1}''",
               new Object[] { operInput != null ? operInput.toString() : NULL,
                     operInput.getJSON() != null ? operInput.getJSON().toString() : NULL });
      }

      try {
         operOutput = this.createImpl(operInput);
      } catch (Exception ex) {
         operOutput = new Operation(OperationIF.TYPE.CREATE);
         operOutput.setError(true);
         operOutput.setState(STATE.ERROR);
         operOutput.setStatus(ex.getMessage());
      }

      _logger.exiting(CLASS, METHOD);

      return operOutput;
   }

   /**
    * Enable "read" operation
    *
    * <pre>
    * JSON input ...
    * {
    *   "uid" : "..." // sso token
    * }
    *
    * JSON output ...
    * {
    *   "data": {
    *     "valid": true,
    *     "sessionUid": "209331b0-6d31-4740-8d5f-740286f6e69f-326295",
    *     "uid": "demo",
    *     "realm": "/"
    *   }
    *   -or-
    *   "data": { "valid": false }
    * }
    * </pre>
    *
    * @param operInput OperationIF input
    * @return OperationIF output
    */
   @Override
   protected OperationIF read(final OperationIF operInput) {
      String METHOD = Thread.currentThread().getStackTrace()[1].getMethodName();
      OperationIF operOutput = null;
      JSONObject jsonWrapData = null;

      _logger.entering(CLASS, METHOD);

      if (_logger.isLoggable(DEBUG_LEVEL)) {
         _logger.log(DEBUG_LEVEL, "input=''{0}'', json=''{1}''",
               new Object[] { operInput != null ? operInput.toString() : NULL,
                     operInput.getJSON() != null ? operInput.getJSON().toString() : NULL });
      }

      try {
         operOutput = this.readImpl(operInput);
      } catch (Exception ex) {
         operOutput = new Operation(OperationIF.TYPE.READ);
         operOutput.setError(true);
         operOutput.setState(STATE.ERROR);
         operOutput.setStatus(ex.getMessage());
      }

      jsonWrapData = new JSONObject();
      jsonWrapData.put(ConstantsIF.DATA, operOutput.getJSON());

      operOutput.setJSON(jsonWrapData);

      _logger.exiting(CLASS, METHOD);

      return operOutput;
   }

   /*
    * =============== PRIVATE METHODS ===============
    */

   /**
    * iInitialize the object
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

   /**
    * Create implementation
    *
    * @param operInput OperationIF input
    * @return OperationIF output
    * @throws Exception
    */
   private OperationIF createImpl(final OperationIF operInput) throws Exception {
      String METHOD = Thread.currentThread().getStackTrace()[1].getMethodName();
      String user = null;
      String password = null;
      JSONObject jsonInput = null;
      JSONObject jsonData = null;
      JSONObject jsonHeaders = null;
      JSONObject jsonCreate = null;
      OperationIF operOutput = null;
      OperationIF operASInput = null;
      OperationIF operASOutput = null;

      _logger.entering(CLASS, METHOD);

      jsonInput = operInput.getJSON();

      user = JSON.getString(jsonInput, ConstantsIF.USER);
      password = JSON.getString(jsonInput, ConstantsIF.PASSWORD);

      if (STR.isEmpty(user) || STR.isEmpty(password)) {
         throw new Exception("'" + ConstantsIF.USER + "' or '" + ConstantsIF.PASSWORD + "' is empty");
      }

      operOutput = new Operation(operInput.getType());

      jsonHeaders = new JSONObject();

      jsonHeaders.put(ConstantsIF.ACCEPT_API_VERSION, this.getConfigValue(ConfigIF.AS_AUTHENTICATE_ACCEPT));
      jsonHeaders.put(this.getConfigValue(ConfigIF.AS_AUTHENTICATE_HEADERS_USER), user);
      jsonHeaders.put(this.getConfigValue(ConfigIF.AS_AUTHENTICATE_HEADERS_PASSWORD), password);

      jsonCreate = new JSONObject();

      jsonCreate.put(ConstantsIF.PATH, this.getConfigValue(ConfigIF.AS_AUTHENTICATE_PATH));
      jsonCreate.put(ConstantsIF.HEADERS, jsonHeaders);

      /*
       * The REST DAO for POST methods require either a "data" or "form" object Add a
       * "data" object with some object to pass validation, if none
       */
      jsonData = JSON.getObject(jsonInput, ConstantsIF.DATA);

      if (jsonData == null) {
         jsonData = new JSONObject();
      }

      jsonCreate.put(ConstantsIF.DATA, jsonData);

      operASInput = new Operation(OperationIF.TYPE.CREATE); // POST
      operASInput.setJSON(jsonCreate);

      if (_AuthzServerDAO != null && _AuthzServerDAO.getState() == STATE.READY) {
         /*
          * POST https://.../openam/json/realms/root/authenticate
          */
         operASOutput = _AuthzServerDAO.execute(operASInput);

         operOutput.setState(operASOutput.getState());
         operOutput.setStatus(operASOutput.getStatus());
         operOutput.setJSON(operASOutput.getJSON());
      } else {
         throw new Exception("Authorization Server DAO is not ready, " + (_AuthzServerDAO == null ? NULL
               : "state='" + _AuthzServerDAO.getState().toString() + "', status='" + _AuthzServerDAO.getStatus()
                     + "'"));
      }

      _logger.exiting(CLASS, METHOD);

      return operOutput;
   }

   /**
    * Read implementation
    *
    * <pre>
    * JSON input ...
    * {
    *   "uid": "...", // AM sso token
    * }
    * JSON output ...
    * {
    *   "valid":true,
    *   "sessionUid":"209331b0-6d31-4740-8d5f-740286f6e69f-326295",
    *   "uid":"demo",
    *   "realm":"/"
    * }
    * -or-
    * { "valid":false }
    * </pre>
    * 
    * @param operInput
    * @return
    * @throws Exception
    */
   private OperationIF readImpl(final OperationIF operInput) throws Exception {
      boolean error = false;
      Boolean isValid = false;
      String METHOD = Thread.currentThread().getStackTrace()[1].getMethodName();
      String msg = null;
      String uid = null;
      OperationIF operASInput = null;
      OperationIF operASOutput = null;
      JSONObject jsonData = null;
      JSONObject jsonInput = null;
      JSONObject jsonHeaders = null;
      JSONObject jsonQueryParams = null;

      _logger.entering(CLASS, METHOD);

      if (operInput == null) {
         throw new Exception("Input Operation in null");
      }

      uid = JSON.getString(operInput.getJSON(), ConstantsIF.UID);

      if (_logger.isLoggable(DEBUG_LEVEL)) {
         _logger.log(DEBUG_LEVEL, "tokenId=''{0}''", new Object[] { uid != null ? uid : NULL });
      }

      if (!STR.isEmpty(uid)) {

         // Build JSON structure to validate SSO session
         // {
         // "path" : "json/realms/root/sessions", "data" : { "tokenId": "..." },
         // "headers": { "Accept-API-Version": "resource=2.1, protocol=1.0" },
         // "queryParams": { "_action": "validate" },
         // "cookies": { "iPlanetDirectoryPro": "$tokenId" }
         // }

         jsonData = new JSONObject();
         jsonData.put(ConstantsIF.TOKENID, uid);

         jsonHeaders = new JSONObject();
         jsonHeaders.put(ConstantsIF.ACCEPT_API_VERSION, this.getConfigValue(ConfigIF.AS_SESSIONS_ACCEPT));
         jsonHeaders.put(this.getConfigValue(ConfigIF.AS_COOKIE), uid);

         jsonQueryParams = new JSONObject();
         jsonQueryParams.put("_action", ConstantsIF.VALIDATE);

         jsonInput = new JSONObject();
         jsonInput.put(ConstantsIF.PATH, this.getConfigValue(ConfigIF.AS_SESSIONS_PATH));
         jsonInput.put(ConstantsIF.DATA, jsonData);
         jsonInput.put(ConstantsIF.HEADERS, jsonHeaders);
         jsonInput.put(ConstantsIF.QUERY_PARAMS, jsonQueryParams);

         operASInput = new Operation(OperationIF.TYPE.CREATE); // POST
         operASInput.setJSON(jsonInput);

         if (_AuthzServerDAO != null && _AuthzServerDAO.getState() == STATE.READY) {
            operASOutput = _AuthzServerDAO.execute(operASInput);
            isValid = JSON.getBoolean(operASOutput.getJSON(), ConstantsIF.VALID);

            if (isValid) {
               operASOutput.setState(STATE.SUCCESS);
               operASOutput.setStatus("SSO session is valid");
            } else {
               operASOutput.setState(STATE.NOTAUTHORIZED);
               operASOutput.setStatus("SSO session is NOT valid");
            }
         } else {
            error = true;
            msg = "Authorization Server DAO is null or not ready";
         }
      } else {
         error = true;
         msg = "JSON attribute '" + ConstantsIF.UID + "' is empty";
      }

      if (error) {
         throw new Exception(msg);
      }

      _logger.exiting(CLASS, METHOD);

      return operASOutput;
   }
}
