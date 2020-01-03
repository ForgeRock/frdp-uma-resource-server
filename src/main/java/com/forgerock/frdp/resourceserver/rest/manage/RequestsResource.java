/*
 * Copyright (c) 2018-2020, ForgeRock, Inc., All rights reserved
 * Use subject to license terms.
 */

package com.forgerock.frdp.resourceserver.rest.manage;

import com.forgerock.frdp.common.ConstantsIF;
import com.forgerock.frdp.dao.Operation;
import com.forgerock.frdp.dao.OperationIF;
import com.forgerock.frdp.resourceserver.handler.JaxrsHandlerIF;
import com.forgerock.frdp.resourceserver.rest.RSResource;
import com.forgerock.frdp.utils.STR;
import javax.servlet.ServletContext;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.UriInfo;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

/**
 * Requests for access, PATH: .../rest/manage/requests PATH:
 * .../rest/manage/requests/{id}
 *
 * @author Scott Fehrman, ForgeRock, Inc.
 */
public class RequestsResource extends RSResource {
   private final String CLASS = this.getClass().getName();

   /**
    * Constructor
    *
    * @param uriInfo    UriInfo uri information
    * @param servletCtx ServletContext context from the servlet
    * @param httpHdrs   HttpHeaders header information
    */
   public RequestsResource(final UriInfo uriInfo, final ServletContext servletCtx, final HttpHeaders httpHdrs) {
      super();

      String METHOD = "RequestsResource()";

      _logger.entering(CLASS, METHOD);

      _uriInfo = uriInfo;
      _servletCtx = servletCtx;
      _httpHdrs = httpHdrs;

      _logger.exiting(CLASS, METHOD);

      return;
   }

   /**
    * Get a list of requests for access.
    * 
    * @return Response HTTP response object
    */
   @GET
   @Produces(MediaType.APPLICATION_JSON)
   public Response search() {
      String METHOD = Thread.currentThread().getStackTrace()[1].getMethodName();
      String userId = null;
      String sso_token = null;
      Response response = null;
      OperationIF operInput = null;
      OperationIF operOutput = null;
      JaxrsHandlerIF requestsHandler = null;
      JSONObject jsonData = null;

      _logger.entering(CLASS, METHOD);

      operOutput = new Operation(OperationIF.TYPE.SEARCH);

      this.load();

      requestsHandler = this.getHandler(JaxrsHandlerIF.HANDLER_UMA_REQUESTS);

      userId = this.getUserIdFromSSOSession(); // make sure authenticated user
      sso_token = this.getSSOTokenFromSSOSession();

      if (_logger.isLoggable(DEBUG_LEVEL)) {
         _logger.log(DEBUG_LEVEL, "userId=''{0}'', sso_token=''{1}''",
               new Object[] { userId != null ? userId : NULL, sso_token != null ? sso_token : NULL });
      }

      jsonData = new JSONObject();
      jsonData.put(ConstantsIF.SSO_TOKEN, sso_token);
      jsonData.put(ConstantsIF.OWNER, userId);

      operInput = new Operation(OperationIF.TYPE.SEARCH);
      operInput.setJSON(jsonData);

      operOutput = requestsHandler.process(operInput);

      response = this.getResponseFromJSON(_uriInfo, operOutput);

      _logger.exiting(CLASS, METHOD);

      return response;

   }

   /**
    * Read a request for access
    *
    * @param requestUid String request identifier from the URI path
    * @return Response HTTP response object
    */
   @GET
   @Path("{" + ConstantsIF.REQUEST + "}")
   @Produces(MediaType.APPLICATION_JSON)
   public Response read(@PathParam(ConstantsIF.REQUEST) String requestUid) {
      String METHOD = Thread.currentThread().getStackTrace()[1].getMethodName();
      String userId = null;
      String sso_token = null;
      Response response = null;
      OperationIF operInput = null;
      OperationIF operOutput = null;
      JaxrsHandlerIF requestsHandler = null;
      JSONObject jsonData = null;

      _logger.entering(CLASS, METHOD);

      if (STR.isEmpty(requestUid)) {
         this.abort(METHOD, "Path parameter 'request' is empty", Status.BAD_REQUEST);
      }

      operOutput = new Operation(OperationIF.TYPE.SEARCH);

      this.load();

      requestsHandler = this.getHandler(JaxrsHandlerIF.HANDLER_UMA_REQUESTS);

      userId = this.getUserIdFromSSOSession(); // make sure authenticated user
      sso_token = this.getSSOTokenFromSSOSession();

      if (_logger.isLoggable(DEBUG_LEVEL)) {
         _logger.log(DEBUG_LEVEL, "userId=''{0}'', sso_token=''{1}'', requestId=''{2}''",
               new Object[] { userId != null ? userId : NULL, sso_token != null ? sso_token : NULL,
                     requestUid != null ? requestUid : NULL });
      }

      jsonData = new JSONObject();
      jsonData.put(ConstantsIF.UID, requestUid);
      jsonData.put(ConstantsIF.SSO_TOKEN, sso_token);
      jsonData.put(ConstantsIF.OWNER, userId);

      operInput = new Operation(OperationIF.TYPE.READ);
      operInput.setJSON(jsonData);

      operOutput = requestsHandler.process(operInput);

      response = this.getResponseFromJSON(_uriInfo, operOutput);

      _logger.exiting(CLASS, METHOD);

      return response;
   }

   /**
    * Replace / update an existing request for access. Set action for "allow/deny".
    * Can also set the permissions / scopes
    * 
    * <pre>
    * Deny data:
    * {
    *   "action": "deny"
    * }
    * 
    * Approve data:
    * {
    *   "action": "approve",
    *   "permissions": [ "meta", "content" ]
    * }
    * </pre>
    * 
    * @param requestUid String request identifier from the URI path
    * @param data String JSON payload
    * @return Response HTTP response object
    */
   @PUT
   @Path("{" + ConstantsIF.REQUEST + "}")
   @Consumes(MediaType.APPLICATION_JSON)
   public Response replace(@PathParam(ConstantsIF.REQUEST) String requestUid, String data) {
      String METHOD = Thread.currentThread().getStackTrace()[1].getMethodName();
      String userId = null;
      String sso_token = null;
      Response response = null;
      OperationIF operInput = null;
      OperationIF operOutput = null;
      JSONParser parser = null;
      JaxrsHandlerIF requestsHandler = null;
      JSONObject jsonData = null;
      JSONObject jsonInput = null;

      _logger.entering(CLASS, METHOD);

      if (STR.isEmpty(requestUid)) {
         this.abort(METHOD, "Path parameter 'request' is empty", Status.BAD_REQUEST);
      }

      if (STR.isEmpty(data)) {
         this.abort(METHOD, "Payload string is empty", Status.BAD_REQUEST);
      }

      this.load();

      parser = this.getParserFromCtx(_servletCtx);

      try {
         jsonData = (JSONObject) parser.parse(data);
      } catch (Exception ex) {
         this.abort(METHOD, "Could not parser String to JSON: '" + data + "', " + ex.getMessage(),
               Status.INTERNAL_SERVER_ERROR);
      }

      requestsHandler = this.getHandler(JaxrsHandlerIF.HANDLER_UMA_REQUESTS);

      userId = this.getUserIdFromSSOSession(); // make sure authenticated user
      sso_token = this.getSSOTokenFromSSOSession();

      if (_logger.isLoggable(DEBUG_LEVEL)) {
         _logger.log(DEBUG_LEVEL, "userId=''{0}'', sso_token=''{1}'', requestId=''{2}'', data=''{3}''",
               new Object[] { userId != null ? userId : NULL, sso_token != null ? sso_token : NULL,
                     requestUid != null ? requestUid : NULL, data != null ? data : NULL });
      }

      operOutput = new Operation(OperationIF.TYPE.REPLACE);

      jsonInput = new JSONObject();
      jsonInput.put(ConstantsIF.DATA, jsonData);
      jsonInput.put(ConstantsIF.UID, requestUid);
      jsonInput.put(ConstantsIF.SSO_TOKEN, sso_token);
      jsonInput.put(ConstantsIF.OWNER, userId);

      operInput = new Operation(OperationIF.TYPE.REPLACE);
      operInput.setJSON(jsonInput);

      operOutput = requestsHandler.process(operInput);

      response = this.getResponseFromJSON(_uriInfo, operOutput);

      _logger.exiting(CLASS, METHOD);

      return response;
   }

}
