/*
 * Copyright (c) 2015-2020, ForgeRock, Inc., All rights reserved
 * Use subject to license terms.
 */
package com.forgerock.frdp.resourceserver.handler;

import com.forgerock.frdp.common.ConstantsIF;
import com.forgerock.frdp.config.ConfigurationIF;
import com.forgerock.frdp.config.ConfigurationManagerIF;
import com.forgerock.frdp.dao.DataAccessIF;
import com.forgerock.frdp.dao.Operation;
import com.forgerock.frdp.dao.OperationIF;
import com.forgerock.frdp.handler.Handler;
import com.forgerock.frdp.handler.HandlerManagerIF;
import com.forgerock.frdp.utils.JSON;
import com.forgerock.frdp.utils.STR;
import java.util.logging.Level;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

/**
 * Abstract JaxRS / Jersey Handler
 *
 * @author Scott Fehrman, ForgeRock Inc.
 */
public abstract class JaxrsHandler extends Handler implements JaxrsHandlerIF {

   private final String CLASS = this.getClass().getName();

   protected static final String PROP_VAR_OWNER = "__owner__";
   protected static final String PROP_SORTKEYS = "_sortKeys";
   protected static final String PROP_QUERYFILTER = "_queryFilter";
   protected static final String AM_ATTR_RESOURCE_OWNER_ID = "resourceOwnerId";
   protected static final String AM_ATTR_RESOURCE_SERVER = "resourceServer";

   protected DataAccessIF _MongoDAO = null;
   protected DataAccessIF _AuthzServerDAO = null;

   protected ConfigurationManagerIF _configMgr = null;
   private HandlerManagerIF _handlerMgr = null;

   /**
    * Constructor
    *
    * @param configMgr ConfigurationManagerIF management of configurations
    * @param handlerMgr HandlerManagerIF handler manager
    */
   public JaxrsHandler(final ConfigurationManagerIF configMgr, final HandlerManagerIF handlerMgr) {
      super();

      String METHOD = "JaxrsHandler(configMgr, handlerMgr)";

      _logger.entering(CLASS, METHOD);

      _configMgr = configMgr;
      _handlerMgr = handlerMgr;

      _logger.exiting(CLASS, METHOD);

      return;
   }

   /**
    * Process the operation input object, returns an output operation object.
    *
    * @param operInput OperationIF input
    * @return OperationIF output
    */
   @Override
   public final synchronized OperationIF process(final OperationIF operInput) {
      String METHOD = Thread.currentThread().getStackTrace()[1].getMethodName();
      StringBuilder msg = new StringBuilder(CLASS + ":" + METHOD + ": ");
      OperationIF operOutput = null;

      _logger.entering(CLASS, METHOD);

      operOutput = new Operation(OperationIF.TYPE.NULL);

      if (this.getState() != STATE.READY) {
         operOutput.setError(true);
         operOutput.setState(this.getState());
         operOutput.setStatus(this.getStatus());
      } else {
         try {
            this.validate(operInput);
         } catch (Exception ex) {
            msg.append(ex.getMessage());
            if (operInput == null) {
               operOutput.setType(OperationIF.TYPE.NULL);
            } else {
               operOutput.setType(operInput.getType());
            }
            operOutput.setError(true);
            operOutput.setState(STATE.FAILED);
            operOutput.setStatus(msg.toString());
         }

         if (!operOutput.isError()) {
            switch (operInput.getType()) {
               case CREATE: {
                  operOutput = this.create(operInput);
                  break;
               }
               case READ: {
                  operOutput = this.read(operInput);
                  break;
               }
               case REPLACE: {
                  operOutput = this.replace(operInput);
                  break;
               }
               case DELETE: {
                  operOutput = this.delete(operInput);
                  break;
               }
               case SEARCH: {
                  operOutput = this.search(operInput);
                  break;
               }
               default: {
                  msg.append("Unsupported operation '").append(operInput.getType().toString()).append("'");
                  operOutput = new Operation(operInput.getType());
                  operOutput.setError(true);
                  operOutput.setState(STATE.FAILED);
                  operOutput.setStatus(msg.toString());
                  break;
               }
            }
         }
      }

      if (operOutput.getJSON() == null) {
         operOutput.setJSON(new JSONObject());
      }

      if (operOutput.isError()) {
         _logger.log(Level.WARNING, operOutput == null ? "Output is null" : operOutput.getStatus());
      }

      _logger.exiting(CLASS, METHOD);

      return operOutput;
   }

   /**
    * Disable copying of the instance
    */
   @Override
   public JaxrsHandlerIF copy() {
      throw new UnsupportedOperationException("Not supported yet.");
   }

   /*
    * ================= PROTECTED METHODS =================
    */
   abstract protected void validate(OperationIF oper) throws Exception;

   protected OperationIF create(OperationIF operInput) {
      throw new UnsupportedOperationException("Not supported yet.");
   }

   protected OperationIF read(OperationIF operInput) {
      throw new UnsupportedOperationException("Not supported yet.");
   }

   protected OperationIF replace(OperationIF operInput) {
      throw new UnsupportedOperationException("Not supported yet.");
   }

   protected OperationIF delete(OperationIF operInput) {
      throw new UnsupportedOperationException("Not supported yet.");
   }

   protected OperationIF search(OperationIF operInput) {
      throw new UnsupportedOperationException("Not supported yet.");
   }

   /**
    * Get the Handler for the specified identifier
    *
    * @param handlerId String handler identifier
    * @return JaxrsHandlerIF handler
    * @throws Exception could not get the handler instance
    */
   protected JaxrsHandlerIF getHandler(String handlerId) throws Exception {
      String METHOD = Thread.currentThread().getStackTrace()[1].getMethodName();
      JaxrsHandlerIF handler = null;

      _logger.entering(CLASS, METHOD);

      if (_handlerMgr == null) {
         this.abort(METHOD, "Handler Manager is null");
      } else {
         if (_handlerMgr.contains(handlerId)) {
            handler = (JaxrsHandlerIF) _handlerMgr.getHandler(handlerId);

            if (handler != null) {
               if (handler.getState() != STATE.READY) {
                  this.abort(METHOD,
                     "Handler not ready:, handlerId='" + handlerId + "', Status=" + handler.getStatus());
               }
            } else {
               this.abort(METHOD, "Handler is null, handlerId='" + handlerId + "'");
            }
         } else {
            this.abort(METHOD, "Handler does not exist, handlerId='" + handlerId + "'");
         }
      }

      _logger.exiting(CLASS, METHOD);

      return handler;
   }

   /**
    * Abort processing, write to log and throw exception.
    *
    * @param method String calling method data
    * @param msg String message
    * @throws Exception had to abort internal processing
    */
   protected void abort(final String method, final String msg) throws Exception {
      String METHOD = Thread.currentThread().getStackTrace()[1].getMethodName();

      _logger.entering(CLASS, METHOD);

      _logger.log(Level.SEVERE, "{0}:{1}: {2}",
         new Object[]{CLASS, (method == null ? "" : method), (msg == null ? NULL : msg)});

      _logger.exiting(CLASS, METHOD);

      throw new Exception(CLASS + ":" + METHOD + ": " + (msg == null ? NULL : msg));
   }

   /**
    * Get a value from the configuration data (JSON). Use the "configType" to
    * get the Configuration object from the ConfigurationManager Get the JSON
    * data from the Configuration object
    *
    * @param configType String what type of configuration (RESOURCE, CONTENT)
    * @param name String configuration attribute name
    * @return String configuration attribute value
    * @throws Exception could not get configuration value
    */
   protected String getConfigValue(
      final String configType, final String name) throws Exception {
      
      String METHOD = Thread.currentThread().getStackTrace()[1].getMethodName();
      String value = null;
      JSONObject json = null;

      _logger.entering(CLASS, METHOD);
      
      if (STR.isEmpty(name)) {
         throw new Exception("Attribute 'name' is null");
      }

      json = this.getConfiguration(configType);

      value = JSON.getString(json, name);

      if (STR.isEmpty(value)) {
         throw new Exception("Attribute '" + name + "' is empty");
      }

      _logger.exiting(CLASS, METHOD);

      return value;
   }
   
   /**
    * Get an object (JSONObject) from the configuration data (JSON). Use the "configType" to
    * get the Configuration object from the ConfigurationManager Get the JSON
    * data from the Configuration object
    *
    * @param configType String what type of configuration (RESOURCE, CONTENT)
    * @param name String configuration array name
    * @return JSONObject configuration object value
    * @throws Exception could not get configuration value
    */
   protected JSONObject getConfigObject(
      final String configType, final String name) throws Exception {
      
      String METHOD = Thread.currentThread().getStackTrace()[1].getMethodName();
      JSONObject json = null;
      JSONObject object = null;
      
      _logger.entering(CLASS, METHOD);
      
      if (STR.isEmpty(name)) {
         throw new Exception("Attribute 'name' is null");
      }

      json = this.getConfiguration(configType);
      
      object = JSON.getObject(json, name);
      
      _logger.exiting(CLASS, METHOD);
      
      return object;
   }

   /**
    * Get an array (JSONArray) from the configuration data (JSON). Use the "configType" to
    * get the Configuration object from the ConfigurationManager Get the JSON
    * data from the Configuration object
    *
    * @param configType String what type of configuration (RESOURCE, CONTENT)
    * @param name String configuration array name
    * @return JSONArray configuration array value
    * @throws Exception could not get configuration value
    */
   protected JSONArray getConfigArray(
      final String configType, final String name) throws Exception {
      
      String METHOD = Thread.currentThread().getStackTrace()[1].getMethodName();
      JSONObject json = null;
      JSONArray array = null;
      
      _logger.entering(CLASS, METHOD);
      
      if (STR.isEmpty(name)) {
         throw new Exception("Attribute 'name' is null");
      }

      json = this.getConfiguration(configType);
      
      array = JSON.getArray(json, name);
      
      _logger.exiting(CLASS, METHOD);
      
      return array;
   }

   /**
    * Check for the specified attribute in the JSON data.
    *
    * @param json JSONObject JSON data
    * @param attrName String attribute name
    * @throws Exception could not verify the attribute name in the JSON object
    */
   protected void checkAttr(final JSONObject json, final String attrName) throws Exception {
      String METHOD = Thread.currentThread().getStackTrace()[1].getMethodName();

      _logger.entering(CLASS, METHOD);
      
      if (STR.isEmpty(attrName)) {
         this.abort(METHOD, "Attribute name is empty");
      }
      
      if (json == null || json.isEmpty()) {
         this.abort(METHOD, "JSON object is null or empty");
      }

      if (STR.isEmpty(JSON.getString(json, attrName))) {
         throw new Exception("Attribute '" + attrName + "' is missing or empty");
      }

      _logger.exiting(CLASS, METHOD);

      return;
   }

   /**
    * Check for "user" and "password" attributes in the JSON data
    *
    * @param json JSONObject JSON data
    * @throws Exception could not verify the user password
    */
   protected void checkUserPassword(JSONObject json) throws Exception {
      String METHOD = Thread.currentThread().getStackTrace()[1].getMethodName();

      _logger.entering(CLASS, METHOD);

      if (STR.isEmpty(JSON.getString(json, ConstantsIF.USER))
         || STR.isEmpty(JSON.getString(json, ConstantsIF.PASSWORD))) {
         throw new Exception(
            "Attribute '" + ConstantsIF.USER + "' or '" + ConstantsIF.PASSWORD + "' is missing or empty");
      }

      _logger.exiting(CLASS, METHOD);

      return;
   }

   /**
    * Set the database and collection parameters in the operation.
    *
    * @param oper OperationIF operation
    * @param database String database value
    * @param collection String collection value
    * @throws Exception could not set the database or collection in operation
    */
   protected void setDatabaseAndCollection(final OperationIF oper, final String database, final String collection)
      throws Exception {
      String METHOD = Thread.currentThread().getStackTrace()[1].getMethodName();

      _logger.entering(CLASS, METHOD);

      oper.setParam(ConstantsIF.DATABASE, this.getConfigValue(ConstantsIF.RESOURCE, database));
      oper.setParam(ConstantsIF.COLLECTION, this.getConfigValue(ConstantsIF.RESOURCE, collection));

      _logger.exiting(CLASS, METHOD);

      return;
   }

   /**
    * Create a new AM SSO Session (primarily for creating "admin" session)
    *
    * <pre>
    * JSON input ...
    * {
    *   "user": "_user_login_"
    *   "password": "_user_password_"
    * }
    * JSON output ...
    * {
    *   "tokenId": "....*...*",
    *   "successUrl": "/openam/console",
    *   "realm": "/"
    * }
    * </pre>
    *
    * @param jsonInput JSONObject input
    * @return JSONObject output
    * @throws Exception could not get the session
    */
   protected JSONObject getSession(final JSONObject jsonInput) throws Exception {
      String METHOD = Thread.currentThread().getStackTrace()[1].getMethodName();
      JSONObject jsonOutput = null;
      OperationIF operInput = null;
      OperationIF operOutput = null;

      _logger.entering(CLASS, METHOD);

      if (jsonInput == null || jsonInput.isEmpty()) {
         throw new Exception("Input JSON is null or empty");
      }

      operInput = new Operation(OperationIF.TYPE.CREATE);
      operInput.setJSON(jsonInput);

      operOutput = this.getSession(operInput);

      jsonOutput = operOutput.getJSON();

      if (jsonOutput == null || jsonOutput.isEmpty()) {
         if (operOutput.getState() != STATE.SUCCESS) {
            throw new Exception(METHOD + ": Output JSON is null or empty");
         } else {
            throw new Exception(METHOD + ": " + operOutput.getStatus());
         }
      }

      _logger.exiting(CLASS, METHOD);

      return jsonOutput;
   }

   /**
    * Validate the AM SSO Session
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
    * {"valid":false}
    * </pre>
    *
    * @param jsonInput JSONObject input
    * @return JSONObject output
    * @throws Exception could not validate the session
    */
   protected JSONObject validateSession(final JSONObject jsonInput) throws Exception {
      String METHOD = Thread.currentThread().getStackTrace()[1].getMethodName();
      JSONObject jsonOutput = null;
      OperationIF operInput = null;
      OperationIF operOutput = null;

      _logger.entering(CLASS, METHOD);

      if (jsonInput == null || jsonInput.isEmpty()) {
         throw new Exception("Input JSON is null or empty");
      }

      operInput = new Operation(OperationIF.TYPE.READ); // GET
      operInput.setJSON(jsonInput);

      operOutput = this.validateSession(operInput);

      jsonOutput = operOutput.getJSON();

      if (jsonOutput == null || jsonOutput.isEmpty()) {
         throw new Exception("Output JSON is null or empty");
      }

      _logger.exiting(CLASS, METHOD);

      return jsonOutput;
   }

   /**
    * Get user id from the AM SSO session
    *
    * <pre>
    * JSON input ...
    * { "tokenId" : "..." // sso token }
    *
    * JSON output ...
    * {
    *   "valid":true,
    *   "sessionUid":"209331b0-6d31-4740-8d5f-740286f6e69f-326295",
    *   "uid":"demo",
    *   "realm":"/"
    * }
    * -or-
    * {"valid":false}
    * </pre>
    *
    * @param ssotoken String sso token
    * @return String user id
    */
   protected String getUserIdFromSSO(final String ssotoken) {
      String METHOD = Thread.currentThread().getStackTrace()[1].getMethodName();
      String uid = null;
      JSONObject jsonInput = null;
      JSONObject jsonOutput = null;

      _logger.entering(CLASS, METHOD);

      jsonInput = new JSONObject();
      jsonInput.put(ConstantsIF.UID, ssotoken);

      try {
         jsonOutput = this.validateSession(jsonInput);
      } catch (Exception ex) {
         _logger.log(Level.WARNING, "Could not get userId from SSO session, exception: {0}", ex.getMessage());
      }

      if (jsonOutput != null) {
         uid = JSON.getString(jsonOutput, ConstantsIF.DATA + "." + ConstantsIF.UID);
      }

      _logger.exiting(CLASS, METHOD);

      return uid;
   }

   
   private synchronized JSONObject getConfiguration(
      final String configType) throws Exception {

      String METHOD = Thread.currentThread().getStackTrace()[1].getMethodName();
      ConfigurationIF configuration = null;
      JSONObject json = null;
      
      _logger.entering(CLASS, METHOD);

      if (STR.isEmpty(configType)) {
         throw new Exception("Attribute 'configType' is null");
      }

      configuration = _configMgr.getConfiguration(configType);

      if (configuration == null) {
         throw new Exception("Configuration is null for type '" + configType + "'");
      }
      
      json = configuration.getJSON();

      _logger.exiting(CLASS, METHOD);
      
      return json;
   }
   
   /**
    * Create a new AM SSO Session (primarily for creating "admin" session)
    *
    * <pre>
    * JSON input ...
    * {
    *   "user": "<<user_login>>"
    *   "password": "<<user_password>>"
    * }
    * JSON output ...
    * {
    *   "tokenId": "....*...*",
    *   "successUrl": "/openam/console",
    *   "realm": "/"
    * }
    * </pre>
    *
    * @param operInput OperationIF input
    * @return OperationIF output
    * @throws Exception
    */
   private OperationIF getSession(final OperationIF operInput) throws Exception {
      boolean error = false;
      String METHOD = Thread.currentThread().getStackTrace()[1].getMethodName();
      String msg = null;
      String user = null;
      String password = null;
      OperationIF operOutput = null;
      JaxrsHandlerIF sessionHandler = null;

      _logger.entering(CLASS, METHOD);

      if (operInput == null) {
         throw new Exception("Input Operation in null");
      }

      user = JSON.getString(operInput.getJSON(), ConstantsIF.USER);
      password = JSON.getString(operInput.getJSON(), ConstantsIF.PASSWORD);

      if (_logger.isLoggable(DEBUG_LEVEL)) {
         _logger.log(DEBUG_LEVEL, "user=''{0}'', password=''{1}''",
            new Object[]{user != null ? user : NULL, password != null ? password : NULL});
      }

      if (!STR.isEmpty(user) && !STR.isEmpty(password)) {
         sessionHandler = this.getHandler(JaxrsHandlerIF.HANDLER_AMSESSION);

         operOutput = sessionHandler.process(operInput);
      } else {
         error = true;
         msg = "'" + ConstantsIF.USER + "' or '" + ConstantsIF.PASSWORD + "' is empty";
      }

      if (error) {
         throw new Exception(msg);
      }

      _logger.exiting(CLASS, METHOD);

      return operOutput;
   }

   /**
    * Validate the SSO session
    *
    * <pre>
    * JSON input ...
    * { "uid" : "..." // sso token }
    *
    * JSON output ...
    * {
    *   "valid":true,
    *   "sessionUid":"209331b0-6d31-4740-8d5f-740286f6e69f-326295",
    *   "uid":"demo",
    *   "realm":"/"
    * }
    * -or-
    * {"valid":false}
    * </pre>
    *
    * @param operInput
    * @return
    * @throws Exception
    */
   private OperationIF validateSession(final OperationIF operInput) throws Exception {
      boolean error = false;
      String METHOD = Thread.currentThread().getStackTrace()[1].getMethodName();
      String msg = null;
      String tokenId = null;
      OperationIF operOutput = null;
      JaxrsHandlerIF sessionHandler = null;

      _logger.entering(CLASS, METHOD);

      if (operInput == null) {
         throw new Exception("Input Operation in null");
      }

      tokenId = JSON.getString(operInput.getJSON(), ConstantsIF.UID);

      if (_logger.isLoggable(DEBUG_LEVEL)) {
         _logger.log(DEBUG_LEVEL, "tokenId=''{0}''", new Object[]{tokenId != null ? tokenId : NULL});
      }

      if (!STR.isEmpty(tokenId)) {
         sessionHandler = this.getHandler(JaxrsHandlerIF.HANDLER_AMSESSION);

         operOutput = sessionHandler.process(operInput);
      } else {
         error = true;
         msg = "JSON attribute '" + ConstantsIF.UID + "' is empty or null";
      }

      if (error) {
         throw new Exception(msg);
      }

      _logger.exiting(CLASS, METHOD);

      return operOutput;
   }
}
