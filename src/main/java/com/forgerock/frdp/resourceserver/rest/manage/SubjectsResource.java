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
import javax.servlet.ServletContext;
import javax.ws.rs.GET;
import javax.ws.rs.Produces;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import org.json.simple.JSONObject;

/**
 * Subject management:
 * 
 * <pre>
 * paths:
 * .../
 * </pre>
 * 
 * @author Scott Fehrman, ForgeRock, Inc.
 */
public class SubjectsResource extends RSResource {
   private final String CLASS = this.getClass().getName();

   /**
    * Constructor
    *
    * @param uriInfo    UriInfo uri information
    * @param servletCtx ServletContext context from the servlet
    * @param httpHdrs   HttpHeaders header information
    */
   public SubjectsResource(final UriInfo uriInfo, final ServletContext servletCtx, final HttpHeaders httpHdrs) {
      super();

      String METHOD = "SubjectsResource()";

      _logger.entering(CLASS, METHOD);

      _uriInfo = uriInfo;
      _servletCtx = servletCtx;
      _httpHdrs = httpHdrs;

      _logger.exiting(CLASS, METHOD);

      return;
   }

   /**
    * Get all the subjects, Requesting Parties (RqP), that have access to any
    * resource that is owned by the authenticated user. This service IS NOT part of
    * the UMA 2.0 specification, it is provided as a "value add" service using
    * Access Manager APIs
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
      JaxrsHandlerIF subjectsHandler = null;
      JSONObject jsonData = null;

      _logger.entering(CLASS, METHOD);

      operOutput = new Operation(OperationIF.TYPE.SEARCH);

      this.load();

      subjectsHandler = this.getHandler(JaxrsHandlerIF.HANDLER_UMA_SUBJECTS);

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

      operOutput = subjectsHandler.process(operInput);
      operOutput.setType(OperationIF.TYPE.READ); // treat as a "read"

      response = this.getResponseFromJSON(_uriInfo, operOutput);

      _logger.exiting(CLASS, METHOD);

      return response;
   }
}
