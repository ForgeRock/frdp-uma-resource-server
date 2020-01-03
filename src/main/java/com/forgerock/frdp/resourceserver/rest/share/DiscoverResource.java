/*
 * Copyright (c) 2018-2020, ForgeRock, Inc., All rights reserved
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
 * discovery of resources
 *
 * @author Scott Fehrman, ForgeRock, Inc.
 */
public class DiscoverResource extends RSResource {
   private String _owner = null;
   private final String CLASS = this.getClass().getName();

   /**
    * Constructor
    *
    * @param owner      String resource owner
    * @param uriInfo    UriInfo uri information
    * @param servletCtx ServletContext context from the servlet
    * @param httpHdrs   HttpHeaders header information
    */
   public DiscoverResource(final String owner, final UriInfo uriInfo, final ServletContext servletCtx,
         final HttpHeaders httpHdrs) {
      super();

      String METHOD = "DiscoverResource()";

      _logger.entering(CLASS, METHOD);

      _owner = owner;
      _uriInfo = uriInfo;
      _servletCtx = servletCtx;
      _httpHdrs = httpHdrs;

      _logger.exiting(CLASS, METHOD);

      return;
   }

   /**
    * Search for "discoverable" resources associated a specific owner
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
      String subject = null;
      Response response = null;
      OperationIF operInput = null;
      OperationIF operOutput = null;
      JaxrsHandlerIF discoverHandler = null;
      JSONObject jsonData = null;
      JSONObject jsonQuery = null;
      MultivaluedMap<String, String> mmapQueryParams = null;

      /*
       * Get the resources, for the given owner, that are "discoverable" If query
       * parameter: - "name" search for resources that have matching "name" (priority)
       * - "type" search for resources that have matching "type"
       */
      _logger.entering(CLASS, METHOD);

      operOutput = new Operation(OperationIF.TYPE.SEARCH);

      this.load();

      subject = this.getUserIdFromSSOSession(); // require a SSO session

      discoverHandler = this.getHandler(JaxrsHandlerIF.HANDLER_UMA_DISCOVER);

      if (_logger.isLoggable(DEBUG_LEVEL)) {
         _logger.log(DEBUG_LEVEL, "Get discover for owner: ''{0}'', subject: ''{1}''",
               new Object[] { _owner == null ? NULL : _owner, subject == null ? NULL : subject });
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
      jsonData.put(ConstantsIF.OWNER, _owner);
      jsonData.put(ConstantsIF.ACCESS_TOKEN, this.getAccessToken(_owner));

      if (!STR.isEmpty(attribute) && !STR.isEmpty(value)) {
         jsonQuery = new JSONObject();
         jsonQuery.put(ConstantsIF.OPERATOR, ConstantsIF.EQUAL);
         jsonQuery.put(ConstantsIF.ATTRIBUTE, attribute);
         jsonQuery.put(ConstantsIF.VALUE, value);
         jsonData.put(ConstantsIF.QUERY, jsonQuery);
      }

      operInput = new Operation(OperationIF.TYPE.SEARCH);
      operInput.setJSON(jsonData);

      operOutput = discoverHandler.process(operInput);
      operOutput.setType(OperationIF.TYPE.READ); // treat as a "read"

      response = this.getResponseFromJSON(_uriInfo, operOutput);

      _logger.exiting(CLASS, METHOD);

      return response;
   }

}
