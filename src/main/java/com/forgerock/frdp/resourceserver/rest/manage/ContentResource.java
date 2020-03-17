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
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Produces;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

/**
 * Content endpoint, PATH: .../rest/manage/resources/{id}/content
 *
 * @author Scott Fehrman, ForgeRock, Inc.
 */
public class ContentResource extends RSResource {

   private String _resourceUid = null;
   private final String CLASS = this.getClass().getName();

   /**
    * Constructor
    *
    * @param resourceUid String resource identifier
    * @param uriInfo UriInfo uri information
    * @param servletCtx ServletContext context from the servlet
    * @param httpHdrs HttpHeaders header information
    */
   public ContentResource(final String resourceUid, final UriInfo uriInfo, final ServletContext servletCtx,
      final HttpHeaders httpHdrs) {
      super();

      String METHOD = "ContentResource()";

      _logger.entering(CLASS, METHOD);

      _resourceUid = resourceUid;
      _uriInfo = uriInfo;
      _servletCtx = servletCtx;
      _httpHdrs = httpHdrs;

      _logger.exiting(CLASS, METHOD);

      return;
   }

   /**
    * Create content.
    *
    * CREATE: possible input formats ... will either have "data" or "uri" ...
    * <pre>
    * "content": {               | "content": {
    *    "id": "default",        |    "id": "refonly",
    *    "data": { ... }         |    "uri": "http://..."
    * }                          | }
    * </pre>
    * @param data String JSON data
    * @return Response HTTP response object
    */
   @POST
   @Consumes(MediaType.APPLICATION_JSON)
   public Response create(String data) {
      String METHOD = Thread.currentThread().getStackTrace()[1].getMethodName();
      Response response = null;
      JSONObject jsonContent = null;
      JSONParser parser = null;

      _logger.entering(CLASS, METHOD);

      if (_logger.isLoggable(DEBUG_LEVEL)) {
         _logger.log(DEBUG_LEVEL, "Get content for resource: ''{0}''",
            (_resourceUid == null ? NULL : _resourceUid));
      }

      if (STR.isEmpty(data)) {
         this.abort(METHOD, "Payload string is empty", Response.Status.BAD_REQUEST);
      }

      this.load();

      this.checkAuthenUserIsOwner(_resourceUid);

      parser = this.getParserFromCtx(_servletCtx);

      try {
         jsonContent = (JSONObject) parser.parse(data);
      } catch (Exception ex) {
         this.abort(METHOD, "Could not parser String to JSON: '" + data + "', "
            + ex.getMessage(),
            Response.Status.BAD_REQUEST);
      }

      this.contentCreate(_resourceUid, jsonContent);

      _logger.exiting(CLASS, METHOD);

      return response;
   }

   /**
    * Read content.
    *
    * @return Response HTTP response object
    */
   @GET
   @Produces(MediaType.APPLICATION_JSON)
   public Response read() {
      String METHOD = Thread.currentThread().getStackTrace()[1].getMethodName();
      JSONObject jsonOutput = null;
      JSONObject jsonData = null;
      Response response = null;
      OperationIF operOutput = null;

      _logger.entering(CLASS, METHOD);

      if (_logger.isLoggable(DEBUG_LEVEL)) {
         _logger.log(DEBUG_LEVEL, "Get content for resource: ''{0}''",
            (_resourceUid == null ? NULL : _resourceUid));
      }

      this.load();

      this.checkAuthenUserIsOwner(_resourceUid);

      operOutput = this.contentRead(_resourceUid);
      
      /*
       * Wrap JSON output in a "data" object, expected by getResponseFromJSON
       * {                          |   {
       *     "data" : {             |       "data": {
       *         "id": "default",   |           "id": "default",
       *         "data" : { ... }   |           "uri": "http://..."
       *     }                      |       }
       * }                          |   }
       */

      jsonOutput = operOutput.getJSON();
      
      if (jsonOutput == null) {
         this.abort(CLASS + ": " + METHOD, "JSON output is null", 
            Response.Status.BAD_REQUEST);
      }
      
      jsonData = new JSONObject();
      jsonData.put(ConstantsIF.DATA, jsonOutput);
      
      operOutput.setJSON(jsonData);
      
      response = this.getResponseFromJSON(_uriInfo, operOutput);

      _logger.exiting(CLASS, METHOD);

      return response;
   }

   /**
    * Replace content.
    *
    * REPLACE: possible input formats:
    * <pre>
    * {                          | {
    *    "id": "default",        |    "id": "refonly",
    *    "uri": "http://..."     |    "uri": "http://..."
    *    "data": {               | }
    *       ...                  |
    *    }                       |
    * }                          |
    * </pre>
    * @param data String JSON payload
    * @return Response HTTP response object
    */
   @PUT
   @Consumes(MediaType.APPLICATION_JSON)
   public Response replace(String data) {
      String METHOD = Thread.currentThread().getStackTrace()[1].getMethodName();
      Response response = null;
      JSONObject jsonContent = null;
      OperationIF operOutput = null;
      JSONParser parser = null;

      _logger.entering(CLASS, METHOD);

      if (_logger.isLoggable(DEBUG_LEVEL)) {
         _logger.log(DEBUG_LEVEL, "Put content for resource: ''{0}''", 
            (_resourceUid == null ? NULL : _resourceUid));
      }

      if (STR.isEmpty(data)) {
         this.abort(CLASS + ": " + METHOD, "Payload string is empty", 
            Response.Status.BAD_REQUEST);
      }

      if (STR.isEmpty(_resourceUid)) {
         this.abort(CLASS + ": " + METHOD, "Path resource is empty", 
            Response.Status.BAD_REQUEST);
      }

      this.load();

      this.checkAuthenUserIsOwner(_resourceUid);

      parser = this.getParserFromCtx(_servletCtx);

      try {
         jsonContent = (JSONObject) parser.parse(data);
      } catch (Exception ex) {
         this.abort(CLASS + ": " + METHOD, "Could not parser String to JSON: '" 
            + data + "', " + ex.getMessage(),
            Response.Status.BAD_REQUEST);
      }

      operOutput = this.contentReplace(_resourceUid, jsonContent);

      response = this.getResponseFromJSON(_uriInfo, operOutput);

      _logger.exiting(CLASS, METHOD);

      return response;
   }

   /**
    * Delete JSON content
    *
    * @return Response HTTP response object
    */
   @DELETE
   @Consumes(MediaType.APPLICATION_JSON)
   public Response delete() {
      String METHOD = Thread.currentThread().getStackTrace()[1].getMethodName();
      Response response = null;
      JSONObject jsonInput = null;
      OperationIF operInput = null;
      OperationIF operOutput = null;
      JaxrsHandlerIF contentHandler = null;

      _logger.entering(CLASS, METHOD);

      if (_logger.isLoggable(DEBUG_LEVEL)) {
         _logger.log(DEBUG_LEVEL, "Delete content for resource: ''{0}''", 
            (_resourceUid == null ? NULL : _resourceUid));
      }

      if (STR.isEmpty(_resourceUid)) {
         this.abort(METHOD, "Path resource is empty", Response.Status.BAD_REQUEST);
      }

      this.load();

      this.checkAuthenUserIsOwner(_resourceUid);

      contentHandler = this.getHandler(JaxrsHandlerIF.HANDLER_CONTENT);

      jsonInput = new JSONObject();
      jsonInput.put(ConstantsIF.UID, _resourceUid);

      operInput = new Operation(OperationIF.TYPE.DELETE);
      operInput.setJSON(jsonInput);

      operOutput = contentHandler.process(operInput);

      response = this.getResponseFromJSON(_uriInfo, operOutput);

      _logger.exiting(CLASS, METHOD);

      return response;
   }
}
