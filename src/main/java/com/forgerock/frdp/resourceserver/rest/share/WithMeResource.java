/*
 * Copyright (c) 2019-2020, ForgeRock, Inc., All rights reserved
 * Use subject to license terms.
 */

package com.forgerock.frdp.resourceserver.rest.share;

import com.forgerock.frdp.common.ConstantsIF;
import com.forgerock.frdp.dao.Operation;
import com.forgerock.frdp.dao.OperationIF;
import com.forgerock.frdp.resourceserver.handler.JaxrsHandlerIF;
import com.forgerock.frdp.resourceserver.rest.RSResource;
import com.forgerock.frdp.utils.STR;
import javax.servlet.ServletContext;
import javax.ws.rs.GET;
import javax.ws.rs.Produces;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import org.json.simple.JSONObject;

/**
 * Resources that are "shared with" the Requesting Party (Rqp) ... aka: "me".
 * This is service IS NOT aprt of the UMA 2.0 specification. This service is
 * provided as value add using the Access Manager APIs
 *
 * @author Scott Fehrman, ForgeRock, Inc.
 */
public class WithMeResource extends RSResource {

   private final String CLASS = this.getClass().getName();

   /**
    * Constructor
    *
    * @param uriInfo    UriInfo uri information
    * @param servletCtx ServletContext context from the servlet
    * @param httpHdrs   HttpHeaders header information
    */
   public WithMeResource(final UriInfo uriInfo, final ServletContext servletCtx, final HttpHeaders httpHdrs) {
      super();

      String METHOD = "WithMeResource()";

      _logger.entering(CLASS, METHOD);

      _uriInfo = uriInfo;
      _servletCtx = servletCtx;
      _httpHdrs = httpHdrs;

      _logger.exiting(CLASS, METHOD);

      return;
   }

   /**
    * Get the resources that are currently "Shared With Me" (to the RqP) If query
    * parameter is "name", search for resources that have matching "name"
    * (priority) If "type", search for resources that have matching "type"
    * 
    * @return Response HTTP response object
    */
   @GET
   @Produces(MediaType.APPLICATION_JSON)
   public Response search() {
      Object obj = null;
      String METHOD = Thread.currentThread().getStackTrace()[1].getMethodName();
      String attribute = null;
      String value = null;
      String sso_token = null;
      String subject = null; // Requesting Party
      Response response = null;
      OperationIF operInput = null;
      OperationIF operOutput = null;
      JaxrsHandlerIF sharedHandler = null;
      JSONObject jsonData = null;
      JSONObject jsonQuery = null;
      MultivaluedMap<String, String> mmapQueryParams = null;

      _logger.entering(CLASS, METHOD);

      this.load();

      operOutput = new Operation(OperationIF.TYPE.SEARCH);

      sharedHandler = this.getHandler(JaxrsHandlerIF.HANDLER_UMA_SHAREDWITHME);

      subject = this.getUserIdFromSSOSession();
      sso_token = this.getSSOTokenFromSSOSession();

      if (_logger.isLoggable(DEBUG_LEVEL)) {
         _logger.log(DEBUG_LEVEL, "Get 'shared with me' for subject (rpq): ''{0}''",
               new Object[] { subject == null ? NULL : subject });
      }

      mmapQueryParams = _uriInfo.getQueryParameters();

      if (mmapQueryParams != null && !mmapQueryParams.isEmpty()) {
         if (mmapQueryParams.containsKey(ConstantsIF.NAME)) {
            obj = mmapQueryParams.getFirst(ConstantsIF.NAME);
            if (obj != null && obj instanceof String) {
               attribute = ConstantsIF.NAME;
               value = (String) obj;
            }
         } else if (mmapQueryParams.containsKey(ConstantsIF.TYPE)) {
            obj = mmapQueryParams.getFirst(ConstantsIF.TYPE);
            if (obj != null && obj instanceof String) {
               attribute = ConstantsIF.TYPE;
               value = (String) obj;
            }
         }
      }

      jsonData = new JSONObject();
      jsonData.put(ConstantsIF.SUBJECT, subject); // Requesting Party
      jsonData.put(ConstantsIF.SSO_TOKEN, sso_token);

      if (!STR.isEmpty(attribute) && !STR.isEmpty(value)) {
         jsonQuery = new JSONObject();
         jsonQuery.put(ConstantsIF.OPERATOR, ConstantsIF.EQUAL);
         jsonQuery.put(ConstantsIF.ATTRIBUTE, attribute);
         jsonQuery.put(ConstantsIF.VALUE, value);
         jsonData.put(ConstantsIF.QUERY, jsonQuery);
      }

      operInput = new Operation(OperationIF.TYPE.SEARCH);
      operInput.setJSON(jsonData);

      operOutput = sharedHandler.process(operInput);
      operOutput.setType(OperationIF.TYPE.READ); // treat as a "read"

      response = this.getResponseFromJSON(_uriInfo, operOutput);

      _logger.exiting(CLASS, METHOD);

      return response;
   }

}
