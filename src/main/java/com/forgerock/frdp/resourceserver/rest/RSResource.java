/*
 * Copyright (c) 2018-2020, ForgeRock, Inc., All rights reserved
 * Use subject to license terms.
 */
package com.forgerock.frdp.resourceserver.rest;

import com.forgerock.frdp.common.ConstantsIF;
import com.forgerock.frdp.config.BasicConfiguration;
import com.forgerock.frdp.config.ConfigurationIF;
import com.forgerock.frdp.config.ConfigurationManager;
import com.forgerock.frdp.config.ConfigurationManagerIF;
import com.forgerock.frdp.dao.Operation;
import com.forgerock.frdp.dao.OperationIF;
import com.forgerock.frdp.handler.HandlerManager;
import com.forgerock.frdp.handler.HandlerManagerIF;
import com.forgerock.frdp.resourceserver.ConfigIF;
import com.forgerock.frdp.resourceserver.handler.AMOAuth2Handler;
import com.forgerock.frdp.resourceserver.handler.AMProxyAdminHandler;
import com.forgerock.frdp.resourceserver.handler.AMSessionHandler;
import com.forgerock.frdp.resourceserver.handler.ContentHandler;
import com.forgerock.frdp.resourceserver.handler.JaxrsHandlerIF;
import com.forgerock.frdp.resourceserver.handler.MetaHandler;
import com.forgerock.frdp.resourceserver.handler.ResourcesHandler;
import com.forgerock.frdp.resourceserver.handler.uma.DiscoverHandler;
import com.forgerock.frdp.resourceserver.handler.uma.PermissionRequestHandler;
import com.forgerock.frdp.resourceserver.handler.uma.PolicyHandler;
import com.forgerock.frdp.resourceserver.handler.uma.ProtectionApiTokenHandler;
import com.forgerock.frdp.resourceserver.handler.uma.RegisterHandler;
import com.forgerock.frdp.resourceserver.handler.uma.RequestsHandler;
import com.forgerock.frdp.resourceserver.handler.uma.SharedWithMeHandler;
import com.forgerock.frdp.resourceserver.handler.uma.SubjectsHandler;
import com.forgerock.frdp.resourceserver.handler.uma.WellKnownHandler;
import com.forgerock.frdp.rest.Resource;
import com.forgerock.frdp.utils.JSON;
import com.forgerock.frdp.utils.STR;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import javax.servlet.ServletContext;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Cookie;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

/**
 * Abstract Resource class for REST Services. All REST Service end-point classes
 * extend this class.
 *
 * @author Scott Fehrman, ForgeRock, Inc.
 */
public abstract class RSResource extends Resource {

   private final String CLASS = this.getClass().getName();
   private HandlerManagerIF _handlerMgr = null;
   private ConfigurationManagerIF _configMgr = null;

   protected static final String CONFIG_FILE_RS = "config/resource-server.json";
   protected static final String CONFIG_FILE_CS = "config/content-service.json";
   protected static final String PUBLIC_FILE = "config/public.json";

   protected static final String CTX_ATTR_CONFIG_MGR = "com.forgerock.frdp.config.configmanager";
   protected static final String CTX_ATTR_PARAMS = "com.forgerock.frdp.params";
   protected static final String CTX_ATTR_PUBLIC = "com.forgerock.frdp.public";
   protected static final String CTX_ATTR_HANDLER_MGR = "com.forgerock.frdp.handler.handlermanager";
   protected static final String CTX_ATTR_UMA_WELL_KNOWN = "com.forgerock.frdp.uma.well.known";

   protected static final String PATH_OWNERS = ConstantsIF.OWNERS;
   protected static final String PATH_RESOURCES = ConstantsIF.RESOURCES;
   protected static final String PATH_WITHME = "withme";

   protected static final String GRANT_TYPE_AUTHORIZATION_CODE = "authorization_code";
   protected static final String TOKEN_TYPE_BEARER = "Bearer";
   protected static final String REQUESTING_PARTY_TOKEN = "rpt";

   @Context
   protected UriInfo _uriInfo;
   @Context
   protected ServletContext _servletCtx;
   @Context
   protected HttpHeaders _httpHdrs;

   public RSResource() {
      super();

      String METHOD = "RSResource()";

      _logger.entering(CLASS, METHOD);

      _logger.exiting(CLASS, METHOD);

      return;
   }

   /*
    * ================= PROTECTED METHODS =================
    */
   /**
    * Get the handler for the specified handler identifier.
    *
    * @param handlerId String handler identifier
    * @return JaxrsHandlerIF handler
    */
   protected JaxrsHandlerIF getHandler(String handlerId) {
      String METHOD = Thread.currentThread().getStackTrace()[1].getMethodName();
      JaxrsHandlerIF handler = null;

      _logger.entering(CLASS, METHOD);

      if (_handlerMgr == null) {
         this.abort(METHOD, "Handler Manager is null", Status.INTERNAL_SERVER_ERROR);
      } else {
         if (_handlerMgr.contains(handlerId)) {
            handler = (JaxrsHandlerIF) _handlerMgr.getHandler(handlerId);

            if (handler != null) {
               if (handler.getState() != STATE.READY) {
                  this.abort(METHOD, "Handler not ready:, handlerId='" + handlerId
                     + "', Status=" + handler.getStatus(),
                     Status.INTERNAL_SERVER_ERROR);
               }
            } else {
               this.abort(METHOD, "Handler is null, handlerId='" + handlerId + "'",
                  Status.INTERNAL_SERVER_ERROR);
            }
         } else {
            this.abort(METHOD, "Handler does not exist, handlerId='" + handlerId + "'",
               Status.INTERNAL_SERVER_ERROR);
         }
      }

      _logger.exiting(CLASS, METHOD);

      return handler;
   }

   /**
    * Get the specified attribute from the HTTP Header. Will abort if attribute
    * is missing / empty.
    *
    * @param attrName String attribute name
    * @return String attribute value
    */
   protected String getAttributeFromHeader(final String attrName) {
      String METHOD = Thread.currentThread().getStackTrace()[1].getMethodName();

      _logger.entering(CLASS, METHOD);
      _logger.exiting(CLASS, METHOD);

      return (this.getAttributeFromHeader(attrName, true));
   }

   /**
    * Get the specified attribute from the HTTP Header. Set True if the method
    * should abort when attribute is missing / empty, else False will return
    * null.
    *
    * @param attrName String attribute name
    * @param abort boolean if True, will abort on missing / null attribute
    * @return String attribute value
    */
   protected String getAttributeFromHeader(final String attrName, final boolean abort) {
      boolean error = false;
      String METHOD = Thread.currentThread().getStackTrace()[1].getMethodName();
      String headerName = null;
      String value = null;
      String configType = ConstantsIF.RESOURCE;
      List<String> hdrValues = null;

      _logger.entering(CLASS, METHOD);

      headerName = this.getConfigValueAsString(configType, attrName, false);

      if (_httpHdrs != null) {
         hdrValues = _httpHdrs.getRequestHeader(headerName);
         if (hdrValues == null || hdrValues.isEmpty()) {
            error = true;
            if (abort) {
               this.abort(METHOD, "Missing header '" + headerName + "'",
                  Status.BAD_REQUEST);
            }
         }
      } else {
         error = true;
         if (abort) {
            this.abort(METHOD, "HttpHeaders is null",
               Status.INTERNAL_SERVER_ERROR);
         }
      }

      if (!error) {
         value = hdrValues.get(0);
         if (STR.isEmpty(value)) {
            error = true;
            if (abort) {
               this.abort(METHOD, "Header '" + headerName + "' is empty",
                  Status.BAD_REQUEST);
            }
         }
      }

      if (_logger.isLoggable(DEBUG_LEVEL)) {
         _logger.log(DEBUG_LEVEL, "attrName=''{0}'', value=''{1}''",
            new Object[]{attrName == null ? NULL : attrName,
               value == null ? NULL : value});
      }

      _logger.exiting(CLASS, METHOD);

      return value;
   }

   /**
    * Get the specified attribute from the HTTP Cookie. Set True if the method
    * should abort when attribute is missing / empty, else False will return
    * null.
    *
    * @param attrName String attribute name
    * @param abort boolean if True, will abort on missing / null attribute
    * @return String attribute value
    */
   protected String getAttributeFromCookie(final String attrName, final boolean abort) {
      String METHOD = Thread.currentThread().getStackTrace()[1].getMethodName();
      String cookieName = null;
      String value = null;
      String configType = ConstantsIF.RESOURCE;
      Cookie cookie = null;
      Map<String, Cookie> cookieMap = null;

      _logger.entering(CLASS, METHOD);

      cookieName = this.getConfigValueAsString(configType, attrName, false);

      if (_httpHdrs != null) {
         cookieMap = _httpHdrs.getCookies();
         if (cookieMap != null && !cookieMap.isEmpty()) {
            if (cookieMap.containsKey(cookieName)) {
               cookie = cookieMap.get(cookieName);
               if (cookie != null) {
                  value = cookie.getValue();

                  if (STR.isEmpty(value) && abort) {
                     this.abort(METHOD, "Cookie '" + cookieName + "' has an empty value",
                        Status.BAD_REQUEST);
                  }
               } else {
                  if (abort) {
                     this.abort(METHOD, "Cookie '" + cookieName + "' is null",
                        Status.BAD_REQUEST);
                  }
               }
            } else {
               if (abort) {
                  this.abort(METHOD, "Cookie '" + cookieName + "' is missing",
                     Status.BAD_REQUEST);
               }
            }
         } else {
            if (abort) {
               this.abort(METHOD, "Cookie Map is missing or empty",
                  Status.BAD_REQUEST);
            }
         }
      } else {
         if (abort) {
            this.abort(METHOD, "HttpHeaders object is null",
               Status.INTERNAL_SERVER_ERROR);
         }
      }

      if (_logger.isLoggable(DEBUG_LEVEL)) {
         _logger.log(DEBUG_LEVEL, "attrName=''{0}'', value=''{1}''",
            new Object[]{attrName == null ? NULL : attrName,
               value == null ? NULL : value});
      }

      _logger.exiting(CLASS, METHOD);

      return value;
   }

   /**
    * Get Single Sign On Token for the admin user, it is used to perform
    * privilaged operations.
    *
    * <pre>
    * Get a sso session token with "admin" credentials
    * {
    *   "data": {
    *     "tokenId": "...*...*",
    *     "successUrl": "/openam/console",
    *     "realm": "/"
    *   }
    * }
    * </pre>
    *
    * @return String sso token
    */
   protected String getSSOTokenForAdmin() {
      String METHOD = Thread.currentThread().getStackTrace()[1].getMethodName();
      String sso_token = null;
      JaxrsHandlerIF proxyAdmHandler = null;
      OperationIF operProxyInput = null;
      OperationIF operProxyOutput = null;

      _logger.entering(CLASS, METHOD);

      proxyAdmHandler = this.getHandler(JaxrsHandlerIF.HANDLER_AMPROXYADM);

      operProxyInput = new Operation(OperationIF.TYPE.READ);
      operProxyOutput = proxyAdmHandler.process(operProxyInput);

      sso_token = JSON.getString(operProxyOutput.getJSON(),
         ConstantsIF.DATA + "." + ConstantsIF.TOKENID);

      if (STR.isEmpty(sso_token)) {
         this.abort(METHOD, "ssotoken is empty", Status.INTERNAL_SERVER_ERROR);
      }

      _logger.exiting(CLASS, METHOD);

      return sso_token;
   }

   /**
    * Get the Single Sign On Token from the user's session
    *
    * @return String sso token
    */
   protected String getSSOTokenFromSSOSession() {
      String METHOD = Thread.currentThread().getStackTrace()[1].getMethodName();
      String ssotoken = null;

      _logger.entering(CLASS, METHOD);

      ssotoken = this.getAttributeFromCookie(ConfigIF.AS_COOKIE, false);

      if (STR.isEmpty(ssotoken)) {
         ssotoken = this.getAttributeFromHeader(ConfigIF.RS_HEADERS_SSOTOKEN);
      }

      _logger.exiting(CLASS, METHOD);

      return ssotoken;
   }

   /**
    * Get the User Id from the user's session
    *
    * <pre>
    * Need to get the logged-in uid / owner from the SSO session
    *    1: Check for AM SSO Cookie: "iPlanetDirectoryPro"
    *    2: Check for Header: "x-frdp-sso"
    *    If neither, then error
    * Read the session using the AMSessionHandler
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
    * {
    *   "valid":false
    * }
    * </pre>
    *
    * @return String user id
    */
   protected String getUserIdFromSSOSession() {
      String METHOD = Thread.currentThread().getStackTrace()[1].getMethodName();
      String userId = null;
      String ssotoken = null;
      JSONObject jsonInput = null;
      OperationIF operInput = null;
      OperationIF operOutput = null;
      JaxrsHandlerIF amsessionHandler = null;

      _logger.entering(CLASS, METHOD);

      ssotoken = this.getSSOTokenFromSSOSession();

      jsonInput = new JSONObject();
      jsonInput.put(ConstantsIF.UID, ssotoken);

      operInput = new Operation(OperationIF.TYPE.READ);
      operInput.setJSON(jsonInput);

      amsessionHandler = this.getHandler(JaxrsHandlerIF.HANDLER_AMSESSION);

      operOutput = amsessionHandler.process(operInput);

      if (operOutput.getState() == STATE.SUCCESS) {
         userId = JSON.getString(operOutput.getJSON(),
            ConstantsIF.DATA + "." + ConstantsIF.UID);
         if (STR.isEmpty(userId)) {
            this.abort(METHOD, "JSON output is null or missing 'uid', "
               + operOutput.getStatus(),
               Status.INTERNAL_SERVER_ERROR);
         }
      } else {
         this.abort(METHOD,
            "Could not read session: " + operOutput.getState().toString()
            + ": " + operOutput.getStatus(),
            Status.UNAUTHORIZED);
      }

      if (_logger.isLoggable(DEBUG_LEVEL)) {
         _logger.log(DEBUG_LEVEL, "ssotoken=''{0}'', userId=''{1}''",
            new Object[]{ssotoken == null ? NULL : ssotoken,
               userId == null ? NULL : userId});
      }

      _logger.exiting(CLASS, METHOD);

      return userId;
   }

   /**
    * Get an OAuth 2.0 Access Token from the user's SSO Token
    *
    * <pre>
    * input:
    * { "ssotoken": "..." }
    * output:
    * {
    *   "data": {
    *     "owner": "",
    *     "category" : "uma_pat",
    *     "credential": {
    *       "access_token": "...",
    *       "refresh_token": "...",
    *       "scope": "uma_protection",
    *       "token_type": "Bearer",
    *       "expires_in": "..."
    *     }
    *   }
    * }
    * </pre>
    *
    * @return String access token
    */
   protected String getAccessToken() {
      String METHOD = Thread.currentThread().getStackTrace()[1].getMethodName();
      String ssotoken = null;
      String access_token = null;
      OperationIF operInput = null;
      OperationIF operOutput = null;
      JSONObject jsonInput = null;
      JSONObject jsonOutput = null;
      JaxrsHandlerIF patHandler = null;

      /*
       * use the SSOToken to get an OAuth access_token
       */
      _logger.entering(CLASS, METHOD);

      patHandler = this.getHandler(JaxrsHandlerIF.HANDLER_UMA_PAT);

      ssotoken = this.getSSOTokenFromSSOSession();

      jsonInput = new JSONObject();
      jsonInput.put(ConstantsIF.SSO_TOKEN, ssotoken);

      operInput = new Operation(OperationIF.TYPE.READ);
      operInput.setJSON(jsonInput);

      operOutput = patHandler.process(operInput);

      jsonOutput = operOutput.getJSON();

      access_token = JSON.getString(jsonOutput,
         ConstantsIF.DATA + "." + ConstantsIF.CREDENTIAL + "." + ConstantsIF.ACCESS_TOKEN);

      if (_logger.isLoggable(DEBUG_LEVEL)) {
         _logger.log(DEBUG_LEVEL, "ssotoken=''{0}'', access_token=''{1}''",
            new Object[]{ssotoken == null ? NULL : ssotoken,
               access_token == null ? NULL : access_token});
      }

      _logger.exiting(CLASS, METHOD);

      return access_token;
   }

   /**
    * Get an OAuth 2.0 Access Token for the specified owner
    *
    * <pre>
    * input:
    * { "owner": "..." }
    * output:
    * {
    *   "data": {
    *     "owner": "",
    *     "category" : "uma_pat",
    *     "credential": {
    *       "access_token": "...",
    *       "refresh_token": "...",
    *       "scope": "uma_protection",
    *       "token_type": "Bearer",
    *       "expires_in": "..."
    *     }
    *   }
    * }
    * </pre>
    *
    * @param owner String owner id
    * @return String the access token
    */
   protected String getAccessToken(final String owner) {
      String METHOD = Thread.currentThread().getStackTrace()[1].getMethodName();
      String access_token = null;
      OperationIF operInput = null;
      OperationIF operOutput = null;
      JSONObject jsonInput = null;
      JSONObject jsonOutput = null;
      JaxrsHandlerIF patHandler = null;

      /*
       * find access_token related to the owner
       */
      _logger.entering(CLASS, METHOD);

      if (STR.isEmpty(owner)) {
         this.abort(METHOD, "Owner is empty", Status.INTERNAL_SERVER_ERROR);
      }

      patHandler = this.getHandler(JaxrsHandlerIF.HANDLER_UMA_PAT);

      jsonInput = new JSONObject();
      jsonInput.put(ConstantsIF.OWNER, owner);

      operInput = new Operation(OperationIF.TYPE.READ);
      operInput.setJSON(jsonInput);

      operOutput = patHandler.process(operInput);

      jsonOutput = operOutput.getJSON();

      access_token = JSON.getString(jsonOutput,
         ConstantsIF.DATA + "." + ConstantsIF.CREDENTIAL + "." + ConstantsIF.ACCESS_TOKEN);

      if (_logger.isLoggable(DEBUG_LEVEL)) {
         _logger.log(DEBUG_LEVEL, "owner=''{0}'', access_token=''{1}''",
            new Object[]{owner == null ? NULL : owner,
               access_token == null ? NULL : access_token});
      }

      _logger.exiting(CLASS, METHOD);

      return access_token;
   }

   /**
    * Get the UMA registration GUID related to the specified resource identifier
    *
    * @param resourceUid String resource identifier
    * @return String registration GUID
    */
   protected String getRegisterGUID(final String resourceUid) {
      String METHOD = Thread.currentThread().getStackTrace()[1].getMethodName();
      String registerGUID = null;
      OperationIF operResourceInput = null;
      OperationIF operResourceOutput = null;
      JSONObject jsonResourceInput = null;
      JSONObject jsonResourceOutput = null;
      JaxrsHandlerIF resourcesHandler = null;

      _logger.entering(CLASS, METHOD);

      if (STR.isEmpty(resourceUid)) {
         this.abort(METHOD, "Resource identifier is empty.",
            Status.INTERNAL_SERVER_ERROR);
      }

      resourcesHandler = this.getHandler(JaxrsHandlerIF.HANDLER_RESOURCES);

      jsonResourceInput = new JSONObject();
      jsonResourceInput.put(ConstantsIF.UID, resourceUid);

      operResourceInput = new Operation(OperationIF.TYPE.READ);
      operResourceInput.setJSON(jsonResourceInput);

      operResourceOutput = resourcesHandler.process(operResourceInput);

      if (operResourceOutput.getState() == STATE.SUCCESS) {
         jsonResourceOutput = operResourceOutput.getJSON();
         registerGUID = JSON.getString(jsonResourceOutput,
            ConstantsIF.DATA + "." + ConstantsIF.REGISTER);
      }

      if (_logger.isLoggable(DEBUG_LEVEL)) {
         _logger.log(DEBUG_LEVEL, "resourceUid=''{0}'', registerGUID=''{1}''",
            new Object[]{resourceUid == null ? NULL : resourceUid,
               registerGUID == null ? NULL : registerGUID});
      }

      _logger.exiting(CLASS, METHOD);

      return registerGUID;
   }

   /**
    * Get the Resource related to the resource uid
    *
    * <pre>
    * JSON output ...
    * {
    *   "uid": "...",
    *   "data": {  ... },
    *   "timestamps": { ... }
    * }
    * </pre>
    *
    * @param resourceUid String resource uid
    * @return OperationIF output
    */
   protected OperationIF getResource(final String resourceUid) {
      String METHOD = Thread.currentThread().getStackTrace()[1].getMethodName();
      OperationIF operInput = null;
      OperationIF operOutput = null;
      JSONObject jsonInput = null;
      JaxrsHandlerIF resourcesHandler = null;

      _logger.entering(CLASS, METHOD);

      if (STR.isEmpty(resourceUid)) {
         this.abort(METHOD, "Resource is empty", Status.BAD_REQUEST);
      }

      resourcesHandler = this.getHandler(JaxrsHandlerIF.HANDLER_RESOURCES);

      jsonInput = new JSONObject();
      jsonInput.put(ConstantsIF.UID, resourceUid);

      operInput = new Operation(OperationIF.TYPE.READ);
      operInput.setJSON(jsonInput);

      operOutput = resourcesHandler.process(operInput);

      if (_logger.isLoggable(DEBUG_LEVEL)) {
         _logger.log(DEBUG_LEVEL, "resourceUid=''{0}'', operOutput=''{1}''",
            new Object[]{
               resourceUid == null ? NULL : resourceUid,
               operOutput == null ? NULL : operOutput.toString()});
      }

      _logger.exiting(CLASS, METHOD);

      return operOutput;
   }

   /**
    * Ge the Meta Data releated to the resource uid
    *
    * @param resourceUid String resource uid
    * @return OperationIF output
    */
   protected OperationIF getMeta(final String resourceUid) {
      String METHOD = Thread.currentThread().getStackTrace()[1].getMethodName();
      JSONObject jsonInput = null;
      OperationIF operInput = null;
      OperationIF operOutput = null;
      JaxrsHandlerIF metaHandler = null;

      _logger.entering(CLASS, METHOD);

      if (STR.isEmpty(resourceUid)) {
         this.abort(METHOD, "Resource is empty", Status.BAD_REQUEST);
      }

      metaHandler = this.getHandler(JaxrsHandlerIF.HANDLER_META);

      jsonInput = new JSONObject();
      jsonInput.put(ConstantsIF.UID, resourceUid);

      operInput = new Operation(OperationIF.TYPE.READ);
      operInput.setJSON(jsonInput);

      operOutput = metaHandler.process(operInput);

      if (_logger.isLoggable(DEBUG_LEVEL)) {
         _logger.log(DEBUG_LEVEL, "resourceUid=''{0}'', operOutput=''{1}''",
            new Object[]{
               resourceUid == null ? NULL : resourceUid,
               operOutput == null ? NULL : operOutput.toString()});
      }

      _logger.exiting(CLASS, METHOD);

      return operOutput;
   }

   /**
    * Get UMA 2.0 registration data related to the resource. If the owner is
    * "empty" the SSO token is used for "self"
    *
    * <pre>
    * JSON input ...
    * {
    *   "uid" : "...", // Register Id
    *   "access_token": "..."
    * }
    * JSON output ...
    * {
    *   "resource_scopes" [ ... ],
    *   "icon_uri": "..."
    * }
    * </pre>
    *
    * @param resourceUid String resource uid
    * @param owner String owner
    * @return OperationIF registration data
    */
   protected OperationIF getRegistration(final String resourceUid, final String owner) {
      String METHOD = Thread.currentThread().getStackTrace()[1].getMethodName();
      String registerId = null;
      String access_token = null;
      OperationIF operOutput = null;
      OperationIF operRegisterInput = null;
      OperationIF operRegisterOutput = null;
      JSONObject jsonOutput = null;
      JSONObject jsonRegisterInput = null;
      JSONObject jsonRegisterOutput = null;
      JSONArray jsonScopes = null;
      JaxrsHandlerIF registerHandler = null;

      _logger.entering(CLASS, METHOD);

      if (STR.isEmpty(resourceUid)) {
         this.abort(METHOD, "Resource is empty", Status.BAD_REQUEST);
      }

      operOutput = new Operation(OperationIF.TYPE.READ);

      if (STR.isEmpty(owner)) {
         access_token = this.getAccessToken(); // PAT for "self" use SSO token
      } else {
         access_token = this.getAccessToken(owner); // PAT for "owner"
      }

      registerHandler = this.getHandler(JaxrsHandlerIF.HANDLER_UMA_REGISTER);

      registerId = this.getRegisterGUID(resourceUid);

      if (!STR.isEmpty(registerId)) {
         jsonRegisterInput = new JSONObject();
         jsonRegisterInput.put(ConstantsIF.UID, registerId);
         jsonRegisterInput.put(ConstantsIF.ACCESS_TOKEN, access_token);

         operRegisterInput = new Operation(OperationIF.TYPE.READ);
         operRegisterInput.setJSON(jsonRegisterInput);

         operRegisterOutput = registerHandler.process(operRegisterInput);

         operOutput = operRegisterOutput;

         jsonRegisterOutput = JSON.getObject(operOutput.getJSON(), ConstantsIF.DATA);

         if (jsonRegisterOutput == null || jsonRegisterOutput.isEmpty()) {
            this.abort(METHOD, "JSON data for registered resource is empty: "
               + operOutput.getState().toString() + ": "
               + operOutput.getStatus(), Status.INTERNAL_SERVER_ERROR);
         }

         /*
          * we only want: "resource_scopes" (required) "icon_uri"
          */
         jsonScopes = JSON.getArray(jsonRegisterOutput, ConstantsIF.RESOURCE_SCOPES);

         if (jsonScopes == null || jsonScopes.isEmpty()) {
            this.abort(METHOD, "JSON Array 'resource_scopes' is empty: "
               + operOutput.getState().toString() + ": "
               + operOutput.getStatus(), Status.INTERNAL_SERVER_ERROR);
         }

         jsonOutput = new JSONObject();
         jsonOutput.put(ConstantsIF.RESOURCE_SCOPES, jsonScopes);

         if (jsonRegisterOutput.containsKey(ConstantsIF.ICON_URI)) {
            jsonOutput.put(ConstantsIF.ICON_URI, JSON.getString(jsonRegisterOutput, ConstantsIF.ICON_URI));
         }

         operOutput.setJSON(jsonOutput);
         operOutput.setState(operRegisterOutput.getState());
         operOutput.setStatus(operRegisterOutput.getStatus());
      } else {
         operOutput.setState(STATE.NOTEXIST);
         operOutput.setStatus("Resource is not registered");
      }

      if (_logger.isLoggable(DEBUG_LEVEL)) {
         _logger.log(DEBUG_LEVEL, "resourceUid=''{0}'', operOutput=''{1}''",
            new Object[]{
               resourceUid == null ? NULL : resourceUid,
               operOutput == null ? NULL : operOutput.toString()});
      }

      _logger.exiting(CLASS, METHOD);

      return operOutput;
   }

   /**
    * Get Policy for the specified resource uid
    *
    * <pre>
    * JSON input ...
    * {
    *   "uid" : "...",
    *   "sso_token": "...",
    *   "owner": "..."
    * }
    * JSON output ...
    * {
    *   "data": {
    *      "permissions": [
    *         { ... }
    *      ]
    *   }
    * }
    * Final JSON output ...
    * {
    *   "permissions": [
    *     { ... }
    *   ]
    * }
    * </pre>
    *
    * @param resourceUid String resource uid
    * @return OperationIF output
    */
   protected OperationIF getPolicy(final String resourceUid) {
      String METHOD = Thread.currentThread().getStackTrace()[1].getMethodName();
      OperationIF operOutput = null;

      _logger.entering(CLASS, METHOD);

      operOutput = this.getPolicy(resourceUid, null, null);

      _logger.exiting(CLASS, METHOD);

      return operOutput;
   }

   protected OperationIF getPolicy(final String resourceUid, String sso_token, String owner) {
      Object[] names = null;
      String METHOD = Thread.currentThread().getStackTrace()[1].getMethodName();
      String registerId = null;
      String name = null;
      OperationIF operOutput = null;
      OperationIF operPolicyInput = null;
      OperationIF operPolicyOutput = null;
      JSONObject jsonPolicyInput = null;
      JSONObject jsonPolicyOutput = null;
      JaxrsHandlerIF policyHandler = null;

      _logger.entering(CLASS, METHOD);

      if (STR.isEmpty(resourceUid)) {
         this.abort(METHOD, "Resource is empty", Status.BAD_REQUEST);
      }

      operOutput = new Operation(OperationIF.TYPE.READ);

      policyHandler = this.getHandler(JaxrsHandlerIF.HANDLER_UMA_POLICY);

      registerId = this.getRegisterGUID(resourceUid);

      if (!STR.isEmpty(registerId)) {
         if (STR.isEmpty(sso_token)) {
            sso_token = this.getSSOTokenFromSSOSession();
         }

         if (STR.isEmpty(owner)) {
            owner = this.getUserIdFromSSOSession();
         }

         jsonPolicyInput = new JSONObject();
         jsonPolicyInput.put(ConstantsIF.UID, registerId);
         jsonPolicyInput.put(ConstantsIF.SSO_TOKEN, sso_token);
         jsonPolicyInput.put(ConstantsIF.OWNER, owner);

         operPolicyInput = new Operation(OperationIF.TYPE.READ);
         operPolicyInput.setJSON(jsonPolicyInput);

         operPolicyOutput = policyHandler.process(operPolicyInput);

         if (operPolicyOutput.getState() == STATE.SUCCESS) {
            jsonPolicyOutput = JSON.getObject(operPolicyOutput.getJSON(), ConstantsIF.DATA);

            if (jsonPolicyOutput != null && !jsonPolicyOutput.isEmpty()) {
               /*
                * only want the "permissions" ... remove everything else
                */
               names = jsonPolicyOutput.keySet().toArray();

               for (Object o : names) {
                  name = o.toString();
                  if (!STR.isEmpty(name)) {
                     switch (name) {
                        case ConstantsIF.PERMISSIONS: {
                           break;
                        }
                        default: {
                           jsonPolicyOutput.remove(name);
                           break;
                        }
                     }
                  }
               }
            }
         }
         operOutput.setJSON(jsonPolicyOutput);
         operOutput.setState(operPolicyOutput.getState());
         operOutput.setStatus(operPolicyOutput.getStatus());
      } else {
         operOutput.setError(true);
         operOutput.setState(STATE.NOTEXIST);
         operOutput.setStatus("Resource is not registered");
      }

      if (_logger.isLoggable(DEBUG_LEVEL)) {
         _logger.log(DEBUG_LEVEL, "resourceUid=''{0}'', operOutput=''{1}''",
            new Object[]{resourceUid == null ? NULL : resourceUid, operOutput.toString()});
      }

      _logger.exiting(CLASS, METHOD);

      return operOutput;
   }

   /**
    * Get the "well known" UMA information
    *
    * @return JSONObject output
    */
   protected JSONObject getWellKnown() {
      String METHOD = Thread.currentThread().getStackTrace()[1].getMethodName();
      JSONObject jsonOutput = null;
      OperationIF operInput = null;
      OperationIF operOutput = null;
      JaxrsHandlerIF wellKnownHandler = null;

      _logger.entering(CLASS, METHOD);

      wellKnownHandler = this.getHandler(JaxrsHandlerIF.HANDLER_UMA_WELLKNOWN);

      operInput = new Operation(OperationIF.TYPE.READ);

      operOutput = wellKnownHandler.process(operInput);

      jsonOutput = operOutput.getJSON();

      if (_logger.isLoggable(DEBUG_LEVEL)) {
         _logger.log(DEBUG_LEVEL, "jsonOutput=''{0}''",
            new Object[]{jsonOutput == null ? NULL : jsonOutput.toString()});
      }

      _logger.exiting(CLASS, METHOD);

      return jsonOutput;
   }

   /**
    * Get HTTP Response object for a JSON object.
    *
    * Override the method from
    * <pre>com.forgerock.frdp.rest.Resource</pre> The JSON payload in the above
    * class is processed completely as the "entity" and returned to the client.
    *
    * This class, uses the JSON payload "data" object as the "entity". The JSON
    * payload can support other objects, such as "headers" which need to be
    * handled differently
    *
    * JSON input structure (possible structure):
    * <pre>
    * {
    *   "uid" : "..."
    *   "timestamps": {
    *     ...
    *   },
    *   "data": {
    *     ...
    *   },
    *   "headers": {
    *     ...
    *   }
    * }
    * </pre>
    *
    * @param uri UriInfo URI info from the session
    * @param oper OperationIF input
    * @return Response HTTP response
    */
   @Override
   protected Response getResponseFromJSON(final UriInfo uri, final OperationIF oper) {
      String METHOD = Thread.currentThread().getStackTrace()[1].getMethodName();
      String str = null;
      String name = null;
      String value = null;
      String entity = null;
      JSONObject jsonOutput = null;
      JSONObject jsonData = null;
      JSONObject jsonHeaders = null;
      JSONObject jsonUids = null;
      ResponseBuilder responseBuilder = null;
      Response response = null;
      UriBuilder builder = null;
      MediaType media = null;

      _logger.entering(CLASS, METHOD);

      if (_logger.isLoggable(DEBUG_LEVEL)) {
         _logger.log(DEBUG_LEVEL, "oper=''{0}''",
            new Object[]{oper != null ? oper.toString() : NULL});
      }

      if (oper == null) {
         this.abort(METHOD, "Input Operation is null", Status.INTERNAL_SERVER_ERROR);
      }

      jsonOutput = oper.getJSON();

      if (_logger.isLoggable(DEBUG_LEVEL)) {
         _logger.log(DEBUG_LEVEL, "json=''{0}''",
            new Object[]{jsonOutput != null ? jsonOutput.toString() : NULL});
      }

      jsonData = JSON.getObject(jsonOutput, ConstantsIF.DATA);

      if (jsonData == null) {
         jsonData = new JSONObject();
      }

      if (oper.isError()) {
         if (jsonOutput != null) {
            media = MediaType.APPLICATION_JSON_TYPE;
            entity = jsonData.toString();
         } else {
            media = MediaType.TEXT_PLAIN_TYPE;
            entity = oper.getStatus();
         }

         response = Response.status(this.getStatusFromState(oper.getState())).type(media).entity(entity).build();
      } else {
         switch (oper.getType()) {
            case CREATE: {
               builder = uri.getAbsolutePathBuilder();
               /*
                * If the operation was initially a PUT, the URI has the UID The operation
                * object will be non-null if the CREATE was a REPLACE
                */
               if (oper.getObject() == null) {
                  str = this.getUidFromOperation(oper);
                  builder.path(str);
               }

               responseBuilder = Response.created(builder.build());
               break;
            }
            case READ: {
               responseBuilder = Response.status(this.getStatusFromState(oper.getState()))
                  .type(MediaType.APPLICATION_JSON).entity(jsonData.toString());
               break;
            }
            case REPLACE: {
               switch (oper.getState()) {
                  case FAILED: {
                     responseBuilder = Response.status(this.getStatusFromState(oper.getState()))
                        .entity(oper.getStatus());
                     break;
                  }
                  case NOTEXIST: {
                     responseBuilder = Response.status(Status.NOT_FOUND);
                     break;
                  }
                  default: {
                     responseBuilder = Response.noContent();
                     break;
                  }
               }
               break;
            }
            case DELETE: {
               responseBuilder = Response.noContent();
               break;
            }
            case SEARCH: {
               jsonUids = this.getUidsFromSearch(jsonData);
               str = jsonUids.toJSONString();
               responseBuilder = Response.ok().type(MediaType.APPLICATION_JSON).entity(str);
               break;
            }
            default: {
               responseBuilder = Response.status(Status.INTERNAL_SERVER_ERROR).type(MediaType.TEXT_PLAIN)
                  .entity("Unsupported Type: " + oper.getType().toString());
               break;
            }
         }

         /*
          * check for response headers
          */
         jsonHeaders = JSON.getObject(jsonOutput, ConstantsIF.HEADERS);

         if (jsonHeaders != null && !jsonHeaders.isEmpty()) {
            for (Object o : jsonHeaders.keySet()) {
               if (o != null && o instanceof String) {
                  name = (String) o;
                  value = JSON.getString(jsonHeaders, name);

                  if (!STR.isEmpty(value)) {
                     responseBuilder.header(name, value);
                  }
               }
            }
         }

         response = responseBuilder.build();
      }

      if (_logger.isLoggable(DEBUG_LEVEL)) {
         _logger.log(DEBUG_LEVEL, "uri=''{0}'', status=''{1}''",
            new Object[]{uri == null ? NULL : uri.getPath(),
               response == null ? NULL : response.getStatusInfo().getReasonPhrase()});
      }

      _logger.exiting(CLASS, METHOD);

      return response;
   }

   /**
    * Check if the authenticated user is the "owner" of the specified resource
    *
    * @param resourceUid String resource id
    * @return boolean True if user is the owner
    */
   protected boolean isAuthenUserOwner(final String resourceUid) {
      boolean isOwner = false;
      String METHOD = Thread.currentThread().getStackTrace()[1].getMethodName();
      String owner = null;
      String userId = null;
      OperationIF operResource = null;
      JSONObject jsonResource = null;

      _logger.entering(CLASS, METHOD);

      operResource = this.getResource(resourceUid);

      if (operResource != null && !operResource.isError()) {
         jsonResource = operResource.getJSON();

         if (jsonResource != null) {
            owner = JSON.getString(jsonResource, ConstantsIF.DATA + "." + ConstantsIF.OWNER);

            if (!STR.isEmpty(owner)) {
               userId = this.getUserIdFromSSOSession();
               if (!STR.isEmpty(userId)) {
                  if (owner.equalsIgnoreCase(userId)) {
                     isOwner = true;
                  }
               }
            }
         }
      }

      _logger.exiting(CLASS, METHOD);

      return isOwner;
   }

   /**
    * Check if the authenticated user is the owner of the specified resource.
    * Will abort if not the owner.
    *
    * @param resourceUid String resource id
    */
   protected void checkAuthenUserIsOwner(final String resourceUid) {
      boolean isOwner = false;
      String METHOD = Thread.currentThread().getStackTrace()[1].getMethodName();

      _logger.entering(CLASS, METHOD);

      isOwner = this.isAuthenUserOwner(resourceUid);

      if (!isOwner) {
         this.abort(METHOD, "User is not the owner of the resource", Status.FORBIDDEN);
      }

      _logger.exiting(CLASS, METHOD);

      return;
   }

   /**
    * Load instance run-time context information
    */
   protected synchronized void load() {
      byte[] bytes = null;
      Object obj = null;
      String METHOD = Thread.currentThread().getStackTrace()[1].getMethodName();
      String realPath = null;
      String configFile = null;
      java.nio.file.Path pathConfigFile = null;
      JSONParser parser = null;
      ConfigurationIF config = null;

      /*
       * Check the Servlet Context for ... 
       * - Configuration Manager
       * - Handler Manager
       */
      _logger.entering(CLASS, METHOD);

      /*
       * Need a Configuration Manager.  Check Servlet Context, else create one
       */
      if (_configMgr == null) {
         obj = _servletCtx.getAttribute(CTX_ATTR_CONFIG_MGR);

         if (obj != null && obj instanceof ConfigurationManagerIF) {
            _configMgr = (ConfigurationManagerIF) obj;
         } else {
            _configMgr = new ConfigurationManager();
            _servletCtx.setAttribute(CTX_ATTR_CONFIG_MGR, _configMgr);
         }

      }

      /*
       * Load the Resource Server configuration.
       * Check the Configuration Manger for existing RS Configuration object
       * Else, read configuration file, create new RS Configuration object, add to Config Manager
       */
      if (!_configMgr.contains(ConstantsIF.RESOURCE)) {
         config = new BasicConfiguration();

         parser = this.getParserFromCtx(_servletCtx);
         realPath = _servletCtx.getRealPath("/");
         configFile = realPath + "WEB-INF" + File.separator + CONFIG_FILE_RS; // Resource Server
         pathConfigFile = Paths.get(configFile);

         try {
            bytes = Files.readAllBytes(pathConfigFile);
            obj = parser.parse(new String(bytes));
         } catch (IOException | ParseException ex) {
            this.abort(METHOD, "Exception: " + ex.getMessage(),
               Response.Status.INTERNAL_SERVER_ERROR);
         }

         if (obj != null && obj instanceof JSONObject) {
            config.setJSON((JSONObject) obj);
            _configMgr.setConfiguration(ConstantsIF.RESOURCE, config);
         } else {
            this.abort(METHOD, "Resource Server Config object is null or not a JSON object",
               Response.Status.INTERNAL_SERVER_ERROR);
         }

         if (_logger.isLoggable(Level.INFO)) {
            _logger.log(Level.INFO, "Loaded configuration file : " + CONFIG_FILE_RS);
         }
      }

      /*
       * Load the Content Server configuration.
       * Check the Configuration Manger for existing CS Configuration object
       * Else, read configuration file, create new CS Configuration object, add to Config Manager
       */
      if (!_configMgr.contains(ConstantsIF.CONTENT)) {
         config = new BasicConfiguration();

         parser = this.getParserFromCtx(_servletCtx);
         realPath = _servletCtx.getRealPath("/");
         configFile = realPath + "WEB-INF" + File.separator + CONFIG_FILE_CS; // Content Server
         pathConfigFile = Paths.get(configFile);

         try {
            bytes = Files.readAllBytes(pathConfigFile);
            obj = parser.parse(new String(bytes));
         } catch (IOException | ParseException ex) {
            this.abort(METHOD, "Exception: " + ex.getMessage(),
               Response.Status.INTERNAL_SERVER_ERROR);
         }

         if (obj != null && obj instanceof JSONObject) {
            config.setJSON((JSONObject) obj);
            _configMgr.setConfiguration(ConstantsIF.CONTENT, config);
         } else {
            this.abort(METHOD, "Content Server Config object is null or not a JSON object",
               Response.Status.INTERNAL_SERVER_ERROR);
         }

         if (_logger.isLoggable(Level.INFO)) {
            _logger.log(Level.INFO, "Loaded configuration file : " + CONFIG_FILE_CS);
         }
      }

      if (_handlerMgr == null) {
         obj = _servletCtx.getAttribute(CTX_ATTR_HANDLER_MGR);

         if (obj != null && obj instanceof HandlerManagerIF) {
            _handlerMgr = (HandlerManagerIF) obj;
         } else {
            _handlerMgr = new HandlerManager();

            /*
             * Add all the handlers
             */
            _handlerMgr.setHandler(JaxrsHandlerIF.HANDLER_AMOAUTH2,
               new AMOAuth2Handler(_configMgr, _handlerMgr));

            _handlerMgr.setHandler(JaxrsHandlerIF.HANDLER_AMPROXYADM,
               new AMProxyAdminHandler(_configMgr, _handlerMgr));

            _handlerMgr.setHandler(JaxrsHandlerIF.HANDLER_AMSESSION,
               new AMSessionHandler(_configMgr, _handlerMgr));

            _handlerMgr.setHandler(JaxrsHandlerIF.HANDLER_CONTENT,
               new ContentHandler(_configMgr, _handlerMgr));

            _handlerMgr.setHandler(JaxrsHandlerIF.HANDLER_UMA_PAT,
               new ProtectionApiTokenHandler(_configMgr, _handlerMgr));

            _handlerMgr.setHandler(JaxrsHandlerIF.HANDLER_META,
               new MetaHandler(_configMgr, _handlerMgr));

            _handlerMgr.setHandler(JaxrsHandlerIF.HANDLER_RESOURCES,
               new ResourcesHandler(_configMgr, _handlerMgr));

            _handlerMgr.setHandler(JaxrsHandlerIF.HANDLER_UMA_DISCOVER,
               new DiscoverHandler(_configMgr, _handlerMgr));

            _handlerMgr.setHandler(JaxrsHandlerIF.HANDLER_UMA_POLICY,
               new PolicyHandler(_configMgr, _handlerMgr));

            _handlerMgr.setHandler(JaxrsHandlerIF.HANDLER_UMA_PERMREQ,
               new PermissionRequestHandler(_configMgr, _handlerMgr));

            _handlerMgr.setHandler(JaxrsHandlerIF.HANDLER_UMA_REGISTER,
               new RegisterHandler(_configMgr, _handlerMgr));

            _handlerMgr.setHandler(JaxrsHandlerIF.HANDLER_UMA_REQUESTS,
               new RequestsHandler(_configMgr, _handlerMgr));

            _handlerMgr.setHandler(JaxrsHandlerIF.HANDLER_UMA_SHAREDWITHME,
               new SharedWithMeHandler(_configMgr, _handlerMgr));

            _handlerMgr.setHandler(JaxrsHandlerIF.HANDLER_UMA_SUBJECTS,
               new SubjectsHandler(_configMgr, _handlerMgr));

            _handlerMgr.setHandler(JaxrsHandlerIF.HANDLER_UMA_WELLKNOWN,
               new WellKnownHandler(_configMgr, _handlerMgr));

            _servletCtx.setAttribute(CTX_ATTR_HANDLER_MGR, _handlerMgr);

            if (_logger.isLoggable(DEBUG_LEVEL)) {
               _logger.log(DEBUG_LEVEL, "Created Handler Manager");
            }
         }
      }

      _logger.exiting(CLASS, METHOD);

      return;
   }

   /**
    * Set JSON content.
    *
    * Create the "content", the following scenarios are allowed: All "content"
    * objects must have a "uid", value for the Content Service Id - If "data"
    * only: Stores the data, new URI is saved - If "uri" only: A new URI is
    * saved (maybe verified)
    *
    * CREATE: possible JSON input formats:
    * <pre>
    * {                             | {
    *    "id": "default",           |    "id": "refonly",
    *    "data": { ... }            |    "uri": "http://..."
    * }                             | }
    * </pre>
    *
    * @param resourceUid String resource identifier
    * @param jsonContent JSONObject input
    * @return OperationIF response from operation
    */
   protected OperationIF contentCreate(final String resourceUid, final JSONObject jsonContent) {
      String METHOD = Thread.currentThread().getStackTrace()[1].getMethodName();
      OperationIF operInput = null;
      OperationIF operOutput = null;
      JaxrsHandlerIF contentHandler = null;

      _logger.entering(CLASS, METHOD);

      if (_logger.isLoggable(DEBUG_LEVEL)) {
         _logger.log(DEBUG_LEVEL, "resourceUid=''{0}'', jsonContent=''{1}''",
            new Object[]{
               resourceUid == null ? NULL : resourceUid,
               jsonContent == null ? NULL : jsonContent.toString()});
      }

      if (STR.isEmpty(resourceUid)) {
         this.abort(METHOD, "Resource identifier is empty", Status.BAD_REQUEST);
      }

      contentHandler = this.getHandler(JaxrsHandlerIF.HANDLER_CONTENT);

      operInput = new Operation(OperationIF.TYPE.CREATE);
      operInput.setJSON(jsonContent);

      operOutput = contentHandler.process(operInput);

      if (operOutput.getState() != STATE.SUCCESS) {
         this.abort(METHOD, "Could not create Content: "
            + operOutput.getState().toString() + ", "
            + operOutput.getStatus(), Status.INTERNAL_SERVER_ERROR);
      }

      this.setContentInformation(resourceUid, operOutput);

      if (_logger.isLoggable(DEBUG_LEVEL)) {
         _logger.log(DEBUG_LEVEL, "resourceUid=''{0}'', operOutput=''{1}''",
            new Object[]{
               resourceUid == null ? NULL : resourceUid,
               operOutput == null ? NULL : operOutput.toString()});
      }

      _logger.exiting(CLASS, METHOD);

      return operOutput;
   }

   /**
    * Get content (JSON) related to the resource uid.
    *
    * @param resourceUid String resource uid
    * @return OperationIF output
    */
   protected OperationIF contentRead(final String resourceUid) {
      String METHOD = Thread.currentThread().getStackTrace()[1].getMethodName();
      OperationIF operInput = null;
      OperationIF operOutput = null;
      JSONObject jsonContentInfo = null;
      JaxrsHandlerIF contentHandler = null;

      _logger.entering(CLASS, METHOD);

      if (_logger.isLoggable(DEBUG_LEVEL)) {
         _logger.log(DEBUG_LEVEL, "resourceUid=''{0}''",
            new Object[]{
               resourceUid == null ? NULL : resourceUid});
      }

      if (STR.isEmpty(resourceUid)) {
         this.abort(METHOD, "Resource Id is empty", Status.BAD_REQUEST);
      }

      contentHandler = this.getHandler(JaxrsHandlerIF.HANDLER_CONTENT);

      jsonContentInfo = this.getContentInformation(resourceUid);

      if (jsonContentInfo != null && !jsonContentInfo.isEmpty()) {

         /*
          * JSON resource content info input:
          * {
          *     "id": "default",
          *     "uri": "https://uma.example.com:443/service/rest/content-server/content/1234-abcd"
          * }
          */
         operInput = new Operation(OperationIF.TYPE.READ);
         operInput.setJSON(jsonContentInfo);

         /*
          * JSON content output options:
          * {                       | {
          *     "id": "default",    |     "id": "default",
          *     "data": { ... }     |     "uri": "http://..."
          * }                       | }
          */
         operOutput = contentHandler.process(operInput);
      } else {
         operOutput = new Operation(OperationIF.TYPE.READ);
         operOutput.setJSON(new JSONObject());
         operOutput.setState(STATE.NOTEXIST);
         operOutput.setStatus("Content information does not exist");
      }

      if (_logger.isLoggable(DEBUG_LEVEL)) {
         _logger.log(DEBUG_LEVEL, "resourceUid=''{0}'', operOutput=''{1}''",
            new Object[]{
               resourceUid == null ? NULL : resourceUid,
               operOutput == null ? NULL : operOutput.toString()});
      }

      _logger.exiting(CLASS, METHOD);

      return operOutput;
   }

   /**
    * Replace the content (JSON) related to the resource Id.
    *
    * If there's existing "content information" stored in the resource data
    * (JSON) perform the REPLACE operation.
    *
    * @param resourceUid
    * @param jsonContent
    * @return
    */
   protected OperationIF contentReplace(final String resourceUid, final JSONObject jsonContent) {
      String METHOD = Thread.currentThread().getStackTrace()[1].getMethodName();
      OperationIF operInput = null;
      OperationIF operOutput = null;
      JSONObject jsonContentInfo = null;
      JaxrsHandlerIF contentHandler = null;

      _logger.entering(CLASS, METHOD);

      if (_logger.isLoggable(DEBUG_LEVEL)) {
         _logger.log(DEBUG_LEVEL, "resourceUid=''{0}'', jsonContent=''{1}''",
            new Object[]{
               resourceUid == null ? NULL : resourceUid,
               jsonContent == null ? NULL : jsonContent.toString()});
      }

      if (STR.isEmpty(resourceUid)) {
         this.abort(METHOD, "Resource Id is empty", Status.BAD_REQUEST);
      }

      contentHandler = this.getHandler(JaxrsHandlerIF.HANDLER_CONTENT);

      jsonContentInfo = this.getContentInformation(resourceUid);

      if (jsonContentInfo != null && !jsonContentInfo.isEmpty()) {
         jsonContentInfo.put(ConstantsIF.DATA, jsonContent);

         /*
          * JSON input:
          * {
          *     "id": "default",
          *     "uri": "https://uma.example.com:443/service/rest/content-server/content/1234-abcd"
          *     "data": { ... }
          * }
          */
         operInput = new Operation(OperationIF.TYPE.REPLACE);
         operInput.setJSON(jsonContentInfo);

         operOutput = contentHandler.process(operInput);

         this.setContentInformation(resourceUid, operOutput);
      } else {
         operOutput = new Operation(OperationIF.TYPE.REPLACE);
         operOutput.setJSON(new JSONObject());
         operOutput.setState(STATE.NOTEXIST);
         operOutput.setStatus("Content information does not exist");
      }

      if (_logger.isLoggable(DEBUG_LEVEL)) {
         _logger.log(DEBUG_LEVEL, "resourceUid=''{0}'', operOutput=''{1}''",
            new Object[]{
               resourceUid == null ? NULL : resourceUid,
               operOutput == null ? NULL : operOutput.toString()});
      }

      _logger.exiting(CLASS, METHOD);

      return operOutput;
   }

   /**
    * Delete the content.
    *
    * JSON object ...
    * <pre>
    * {
    *     "id": "default",
    *     "uri": "https://..."
    * }
    * </pre>
    *
    * @param resourceUid String Resource identifier
    * @return OperationIF operation response
    */
   protected OperationIF contentDelete(final String resourceUid) {
      String METHOD = Thread.currentThread().getStackTrace()[1].getMethodName();
      OperationIF operInput = null;
      OperationIF operOutput = null;
      JSONObject jsonContentInfo = null;
      JaxrsHandlerIF contentHandler = null;

      _logger.entering(CLASS, METHOD);

      if (_logger.isLoggable(DEBUG_LEVEL)) {
         _logger.log(DEBUG_LEVEL, "resourceUid=''{0}''", new Object[]{
            resourceUid == null ? NULL : resourceUid});
      }

      if (STR.isEmpty(resourceUid)) {
         this.abort(METHOD, "Resource Id is empty", Status.BAD_REQUEST);
      }

      contentHandler = this.getHandler(JaxrsHandlerIF.HANDLER_CONTENT);

      jsonContentInfo = this.getContentInformation(resourceUid);

      operInput = new Operation(OperationIF.TYPE.DELETE);
      operInput.setJSON(jsonContentInfo);

      operOutput = contentHandler.process(operInput);

      if (operOutput.getState() != STATE.SUCCESS) {
         this.abort(METHOD, ": Could not delete content: "
            + operOutput.getState().toString() + ", "
            + operOutput.getStatus(), Status.INTERNAL_SERVER_ERROR);
      }

      this.setContentInformation(resourceUid, operOutput);

      if (_logger.isLoggable(DEBUG_LEVEL)) {
         _logger.log(DEBUG_LEVEL, "resourceUid=''{0}'', operOutput=''{1}''",
            new Object[]{
               resourceUid == null ? NULL : resourceUid,
               operOutput == null ? NULL : operOutput.toString()});
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
    * Get content information related to the specified resource identifier
    *
    * @param resourceUid String resource identifier
    * @return JSONObject content information
    */
   private JSONObject getContentInformation(final String resourceUid) {
      String METHOD = Thread.currentThread().getStackTrace()[1].getMethodName();
      OperationIF operResourceInput = null;
      OperationIF operResourceOutput = null;
      JSONObject jsonResourceInput = null;
      JSONObject jsonResourceOutput = null;
      JSONObject jsonContentInfo = null;
      JaxrsHandlerIF resourcesHandler = null;

      /*
       * content information:
       * {
       *     "id": "default",
       *     "uri": "https://uma.example.com:443/service/rest/content-server/content/1234-abcd"
       * }
       */
      _logger.entering(CLASS, METHOD);

      if (STR.isEmpty(resourceUid)) {
         this.abort(METHOD, "Resource identifier is empty.",
            Status.INTERNAL_SERVER_ERROR);
      }

      resourcesHandler = this.getHandler(JaxrsHandlerIF.HANDLER_RESOURCES);

      jsonResourceInput = new JSONObject();
      jsonResourceInput.put(ConstantsIF.UID, resourceUid);

      operResourceInput = new Operation(OperationIF.TYPE.READ);
      operResourceInput.setJSON(jsonResourceInput);

      operResourceOutput = resourcesHandler.process(operResourceInput);

      if (operResourceOutput.getState() == STATE.SUCCESS) {
         jsonResourceOutput = operResourceOutput.getJSON();
         jsonContentInfo = JSON.getObject(jsonResourceOutput, ConstantsIF.DATA
            + "." + ConstantsIF.CONTENT);
      } else {
         this.abort(METHOD, "Could not get content information, resourceUid='"
            + resourceUid + "'", Status.INTERNAL_SERVER_ERROR);
      }

      if (_logger.isLoggable(DEBUG_LEVEL)) {
         _logger.log(DEBUG_LEVEL, "resourceUid=''{0}'', content=''{1}''",
            new Object[]{
               resourceUid == null ? NULL : resourceUid,
               jsonContentInfo == null ? NULL : jsonContentInfo.toString()});
      }

      _logger.exiting(CLASS, METHOD);

      return jsonContentInfo;
   }

   /**
    * Update the Resource with Content information.
    *
    * For CREATE and REPLACE operations, add / replace "content" object. For
    * DELETE operation, remove "content" object.
    *
    * @param resourceUid String resource identifier
    * @param operInput OperationIF operation information
    */
   private void setContentInformation(final String resourceUid, final OperationIF operInput) {
      String METHOD = Thread.currentThread().getStackTrace()[1].getMethodName();
      OperationIF operResourceInput = null;
      OperationIF operResourceOutput = null;
      JSONObject jsonInput = null;
      JSONObject jsonData = null;
      JSONObject jsonResourceInput = null;
      JaxrsHandlerIF resourcesHandler = null;

      _logger.entering(CLASS, METHOD);

      if (_logger.isLoggable(DEBUG_LEVEL)) {
         _logger.log(DEBUG_LEVEL, "resourceUid=''{0}'', operOutput=''{1}''",
            new Object[]{
               resourceUid == null ? NULL : resourceUid,
               operInput == null ? NULL : operInput.toString()});
      }

      if (STR.isEmpty(resourceUid)) {
         this.abort(METHOD, "Resource identifier is empty",
            Status.INTERNAL_SERVER_ERROR);
      }

      if (operInput == null) {
         this.abort(METHOD, "Input operation is null",
            Status.INTERNAL_SERVER_ERROR);
      }

      resourcesHandler = this.getHandler(JaxrsHandlerIF.HANDLER_RESOURCES);

      jsonInput = operInput.getJSON();

      if (jsonInput == null || jsonInput.isEmpty()) {
         this.abort(METHOD, "Input JSON is null or empty",
            Status.INTERNAL_SERVER_ERROR);
      }

      /*
       * JSON input ...
       * {
       *     "id": "default",
       *     "uri": "http://..."
       * }
       */
      operResourceOutput = this.getResource(resourceUid);

      /*
       * JSON resource ...
       * {
       *   "uid": "...",
       *   "data": {  
       *     "meta": { ... },
       *     "content": { ... },
       *     "register": "GUID"
       *   },
       *   "timestamps": { ... }
       * }
       */
      if (operResourceOutput == null) {
         this.abort(METHOD, "Could not get resource, resourceUid='"
            + resourceUid + "'", Status.INTERNAL_SERVER_ERROR);
      }

      jsonData = JSON.getObject(operResourceOutput.getJSON(), ConstantsIF.DATA);

      if (jsonData == null || jsonData.isEmpty()) {
         this.abort(METHOD, "Respource JSON data is null or empty",
            Status.INTERNAL_SERVER_ERROR);
      }

      /*
       * Remove existing 'content' object and add new 'content' object
       */
      if (jsonData.containsKey(ConstantsIF.CONTENT)) {
         jsonData.remove(ConstantsIF.CONTENT);
      }

      if (operInput.getType() == OperationIF.TYPE.CREATE
         || operInput.getType() == OperationIF.TYPE.REPLACE) {
         jsonData.put(ConstantsIF.CONTENT, jsonInput);
      }

      jsonResourceInput = new JSONObject();
      jsonResourceInput.put(ConstantsIF.UID, resourceUid);
      jsonResourceInput.put(ConstantsIF.DATA, jsonData);

      operResourceInput = new Operation(OperationIF.TYPE.REPLACE);
      operResourceInput.setJSON(jsonResourceInput);

      operResourceOutput = resourcesHandler.process(operResourceInput);

      if (operResourceOutput.getState() != STATE.SUCCESS) {
         this.abort(METHOD, "Could not replace content information, resourceUid='"
            + resourceUid + "'", Status.INTERNAL_SERVER_ERROR);
      }

      if (_logger.isLoggable(DEBUG_LEVEL)) {
         _logger.log(DEBUG_LEVEL, "resourceUid=''{0}'', data=''{1}''",
            new Object[]{resourceUid == null ? NULL : resourceUid,
               jsonData == null ? NULL : jsonData.toString()});
      }

      _logger.exiting(CLASS, METHOD);

      return;
   }

   /**
    * Attempt to get a String value from the configuration JSON object "name" is
    * a "dot" delimited JSON object name: "rs.headers.ssotoken" If the flag
    * "allowEmpty" is false, abort if attribute does not exist or is empty
    *
    * @param configType configuration type
    * @param name String configuration name
    * @param allowEmpty boolean allow empty values
    * @return String configuration value
    */
   private String getConfigValueAsString(final String configType, final String name, final boolean allowEmpty) {
      String METHOD = Thread.currentThread().getStackTrace()[1].getMethodName();
      String value = null;
      JSONObject configData = null;

      _logger.entering(CLASS, METHOD);

      if (STR.isEmpty(configType)) {
         this.abort(METHOD, "Attribute 'configType' is empty", 
            Status.INTERNAL_SERVER_ERROR);
      }

      if (STR.isEmpty(name)) {
         this.abort(METHOD, "Attribute 'name' is empty", 
            Status.INTERNAL_SERVER_ERROR);
      }

      configData = this.getConfiguration(configType);

      value = JSON.getString(configData, name);

      if (STR.isEmpty(value) && !allowEmpty) {
         this.abort(METHOD, "Config attribute '" + name + "' is null or empty", 
            Status.INTERNAL_SERVER_ERROR);
      }

      _logger.exiting(CLASS, METHOD);

      return value;
   }

//   /**
//    * Attempt to get a JSONObject value from the configuration JSON object
//    * "name" is a "dot" delimited JSON object name: "rs.connect" If the flag
//    * "allowEmpty" is false, abort if attribute does not exist or is empty
//    *
//    * @param name String configuration name
//    * @param allowEmpty boolean allow empty values
//    * @return JSONObject configuration object
//    */
//   private JSONObject getConfigValueAsJSONObject(final String configType, final String name, final boolean allowEmpty) {
//      String METHOD = Thread.currentThread().getStackTrace()[1].getMethodName();
//      JSONObject value = null;
//      JSONObject configData = null;
//
//      _logger.entering(CLASS, METHOD);
//
//      if (STR.isEmpty(configType)) {
//         this.abort(METHOD, "Attribute 'configType' is empty", Status.INTERNAL_SERVER_ERROR);
//      }
//
//      if (STR.isEmpty(name)) {
//         this.abort(METHOD, "Attribute 'name' is empty", Status.INTERNAL_SERVER_ERROR);
//      }
//
//      configData = this.getConfiguration(configType);
//
//      value = JSON.getObject(configData, name);
//
//      if ((value == null || value.isEmpty()) && !allowEmpty) {
//         this.abort(METHOD, "Config object '" + name + "' is null or empty", Status.INTERNAL_SERVER_ERROR);
//      }
//
//      _logger.exiting(CLASS, METHOD);
//
//      return value;
//   }
   /**
    * Get the JSON data associated with a configuration type.
    *
    * @param configType the configuration type
    * @return JSONobject JSON data
    */
   private JSONObject getConfiguration(final String configType) {
      String METHOD = Thread.currentThread().getStackTrace()[1].getMethodName();
      String msg = null;
      JSONObject json = null;
      ConfigurationIF config = null;

      _logger.entering(CLASS, METHOD);

      if (!STR.isEmpty(configType)) {
         config = _configMgr.getConfiguration(configType);
         if (config != null) {
            json = config.getJSON();
            if (json == null) {
               msg = "JSON data for configuration '" + configType + "' is null";
            }
         } else {
            msg = "Configuration for type '" + configType + "' is null";
         }
      } else {
         msg = "Argument 'configType' is empty";
      }

      if (msg != null) {
         this.abort(METHOD, msg, Status.INTERNAL_SERVER_ERROR);
      }

      _logger.exiting(CLASS, METHOD);

      return json;
   }

}
