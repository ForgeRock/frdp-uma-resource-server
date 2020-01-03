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
import com.forgerock.frdp.utils.JSON;
import com.forgerock.frdp.utils.STR;
import javax.servlet.ServletContext;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Produces;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.UriInfo;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

/**
 * Meta data endpoint, PATH: .../rest/manage/resources/{id}/meta
 *
 * @author Scott Fehrman, ForgeRock, Inc.
 */
public class MetaResource extends RSResource {

   private String _resourceUid = null;
   private final String CLASS = this.getClass().getName();

   /**
    * Constructor
    * 
    * @param resourceUid String resource identifier
    * @param uriInfo     UriInfo uri information
    * @param servletCtx  ServletContext context from the servlet
    * @param httpHdrs    HttpHeaders header information
    */
   public MetaResource(final String resourceUid, final UriInfo uriInfo, final ServletContext servletCtx,
         final HttpHeaders httpHdrs) {
      super();

      String METHOD = "MetaResource()";

      _logger.entering(CLASS, METHOD);

      _resourceUid = resourceUid;
      _uriInfo = uriInfo;
      _servletCtx = servletCtx;
      _httpHdrs = httpHdrs;

      _logger.exiting(CLASS, METHOD);

      return;
   }

   /**
    * Read resource meta data
    * 
    * @return Response HTTP response object
    */
   @GET
   @Produces(MediaType.APPLICATION_JSON)
   public Response read() {
      String METHOD = Thread.currentThread().getStackTrace()[1].getMethodName();
      Response response = null;
      OperationIF operOutput = null;

      _logger.entering(CLASS, METHOD);

      if (_logger.isLoggable(DEBUG_LEVEL)) {
         _logger.log(DEBUG_LEVEL, "Get meta for resource: ''{0}''", (_resourceUid == null ? NULL : _resourceUid));
      }

      this.load();

      this.checkAuthenUserIsOwner(_resourceUid);

      operOutput = this.getMeta(_resourceUid);

      response = this.getResponseFromJSON(_uriInfo, operOutput);

      _logger.exiting(CLASS, METHOD);

      return response;
   }

   /**
    * Replace resource meta data
    * 
    * @param data String JSON payload
    * @return Response HTTP response object
    */
   @PUT
   @Consumes(MediaType.APPLICATION_JSON)
   public Response replace(String data) {
      String METHOD = Thread.currentThread().getStackTrace()[1].getMethodName();
      Response response = null;
      JSONObject jsonData = null;
      JSONObject jsonInput = null;
      OperationIF operInput = null;
      OperationIF operOutput = null;
      JSONParser parser = null;
      JaxrsHandlerIF metaHandler = null;

      _logger.entering(CLASS, METHOD);

      if (_logger.isLoggable(DEBUG_LEVEL)) {
         _logger.log(DEBUG_LEVEL, "Put meta for resource: ''{0}''", (_resourceUid == null ? NULL : _resourceUid));
      }

      if (STR.isEmpty(data)) {
         this.abort(METHOD, "Payload string is empty", Status.BAD_REQUEST);
      }

      if (STR.isEmpty(_resourceUid)) {
         this.abort(METHOD, "Path resource is empty", Status.BAD_REQUEST);
      }

      this.load();

      this.checkAuthenUserIsOwner(_resourceUid);

      parser = this.getParserFromCtx(_servletCtx);

      try {
         jsonData = (JSONObject) parser.parse(data);
      } catch (Exception ex) {
         this.abort(METHOD, "Could not parser String to JSON: '" + data + "', " + ex.getMessage(),
               Status.INTERNAL_SERVER_ERROR);
      }

      metaHandler = this.getHandler(JaxrsHandlerIF.HANDLER_META);

      /*
       * If registered, update the registration data
       */
      this.updateRegistration(jsonData);

      /*
       * Replace the meta data
       */
      jsonInput = new JSONObject();
      jsonInput.put(ConstantsIF.UID, _resourceUid);
      jsonInput.put(ConstantsIF.DATA, jsonData);

      operInput = new Operation(OperationIF.TYPE.REPLACE);
      operInput.setJSON(jsonInput);

      operOutput = metaHandler.process(operInput);

      response = this.getResponseFromJSON(_uriInfo, operOutput);

      _logger.exiting(CLASS, METHOD);

      return response;
   }

   /**
    * Delete resource meta data
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
      JaxrsHandlerIF metaHandler = null;

      _logger.entering(CLASS, METHOD);

      if (_logger.isLoggable(DEBUG_LEVEL)) {
         _logger.log(DEBUG_LEVEL, "Delete meta for resource: ''{0}''", (_resourceUid == null ? NULL : _resourceUid));
      }

      if (STR.isEmpty(_resourceUid)) {
         this.abort(METHOD, "Path resource is empty", Status.BAD_REQUEST);
      }

      this.load();

      this.checkAuthenUserIsOwner(_resourceUid);

      metaHandler = this.getHandler(JaxrsHandlerIF.HANDLER_META);

      jsonInput = new JSONObject();
      jsonInput.put(ConstantsIF.UID, _resourceUid);

      operInput = new Operation(OperationIF.TYPE.DELETE);
      operInput.setJSON(jsonInput);

      operOutput = metaHandler.process(operInput);

      response = this.getResponseFromJSON(_uriInfo, operOutput);

      _logger.exiting(CLASS, METHOD);

      return response;
   }

   /*
    * =============== PRIVATE METHODS ===============
    */
   /**
    * Update the UMA registration data
    * 
    * @param jsonNewData JSONObject data
    */
   private void updateRegistration(final JSONObject jsonNewData) {
      String METHOD = Thread.currentThread().getStackTrace()[1].getMethodName();
      String curName = null;
      String newName = null;
      String curType = null;
      String newType = null;
      String registerId = null;
      String access_token = null;
      JSONObject jsonReadInput = null;
      JSONObject jsonCurData = null;
      JSONObject jsonDeltaData = null;
      JSONObject jsonResourceInput = null;
      JSONObject jsonResourceOutput = null;
      JSONObject jsonRegisterInput = null;
      JSONObject jsonRegisterOutput = null;
      OperationIF operMetaInput = null;
      OperationIF operMetaOutput = null;
      OperationIF operResourceInput = null;
      OperationIF operResourceOutput = null;
      OperationIF operRegisterInput = null;
      OperationIF operRegisterOutput = null;
      JaxrsHandlerIF metaHandler = null;
      JaxrsHandlerIF resourcesHandler = null;
      JaxrsHandlerIF registerHandler = null;

      _logger.entering(CLASS, METHOD);

      metaHandler = this.getHandler(JaxrsHandlerIF.HANDLER_META);
      resourcesHandler = this.getHandler(JaxrsHandlerIF.HANDLER_RESOURCES);
      registerHandler = this.getHandler(JaxrsHandlerIF.HANDLER_UMA_REGISTER);

      jsonReadInput = new JSONObject();
      jsonReadInput.put(ConstantsIF.UID, _resourceUid);
      jsonReadInput.put(ConstantsIF.DATA, jsonNewData);

      operMetaInput = new Operation(OperationIF.TYPE.READ);
      operMetaInput.setJSON(jsonReadInput);

      operMetaOutput = metaHandler.process(operMetaInput);

      if (operMetaOutput.getState() != STATE.SUCCESS) {
         this.abort(METHOD, "Meta Read error: " + operMetaOutput.getStatus(), Status.INTERNAL_SERVER_ERROR);
      }

      jsonCurData = operMetaOutput.getJSON();

      /*
       * Compare the attributes "type" and "name" If current and new values are NOT
       * equal Add the new value to a "delta" JSON Object
       */
      jsonDeltaData = new JSONObject();

      curName = JSON.getString(jsonCurData, ConstantsIF.NAME);
      newName = JSON.getString(jsonNewData, ConstantsIF.NAME);

      if (!STR.isEmpty(curName) && !STR.isEmpty(newName)) {
         if (!curName.equals(newName)) {
            jsonDeltaData.put(ConstantsIF.NAME, newName);
         }
      }

      curType = JSON.getString(jsonCurData, ConstantsIF.TYPE);
      newType = JSON.getString(jsonNewData, ConstantsIF.TYPE);

      if (!STR.isEmpty(curType) && !STR.isEmpty(newType)) {
         if (!curType.equals(newType)) {
            jsonDeltaData.put(ConstantsIF.TYPE, newType);
         }
      }

      if (!jsonDeltaData.isEmpty()) {
         /*
          * Read the "complete" Resource to check for a "register" attribute
          */
         jsonResourceInput = new JSONObject();
         jsonResourceInput.put(ConstantsIF.UID, _resourceUid);

         operResourceInput = new Operation(OperationIF.TYPE.READ);
         operResourceInput.setJSON(jsonResourceInput);

         /*
          * Read the resource record JSON input: { "uid": "..." } JSON output: { "owner":
          * "", "access": "", "meta": { "name": "" }, "register": "" }
          */
         operResourceOutput = resourcesHandler.process(operResourceInput);

         if (operResourceOutput.getState() == STATE.ERROR) {
            this.abort(METHOD, "Could not read resource: " + operResourceOutput.getStatus(),
                  Status.INTERNAL_SERVER_ERROR);
         }

         jsonResourceOutput = operResourceOutput.getJSON();

         if (jsonResourceOutput == null || jsonResourceOutput.isEmpty()) {
            this.abort(METHOD, "JSON for resource is empty", Status.INTERNAL_SERVER_ERROR);
         }

         registerId = JSON.getString(jsonResourceOutput, ConstantsIF.REGISTER);

         if (!STR.isEmpty(registerId)) {
            access_token = this.getAccessToken(); // UMA PAT

            jsonRegisterInput = new JSONObject();
            jsonRegisterInput.put(ConstantsIF.UID, registerId);
            jsonRegisterInput.put(ConstantsIF.ACCESS_TOKEN, access_token);

            operRegisterInput = new Operation(OperationIF.TYPE.READ);
            operRegisterInput.setJSON(jsonRegisterInput);

            /*
             * JSON input ... { "uid" : "...", "access_token": "..." } JSON output ... {
             * "resource_scopes": [ "...", "..." ], "name": "...", "type": "..." }
             */
            operRegisterOutput = registerHandler.process(operRegisterInput);

            jsonRegisterOutput = operRegisterOutput.getJSON();

            if (jsonRegisterOutput == null || jsonRegisterOutput.isEmpty()) {
               this.abort(METHOD, "JSON data for registered resource is empty: "
                     + operRegisterOutput.getState().toString() + ": " + operRegisterOutput.getStatus(),
                     Status.INTERNAL_SERVER_ERROR);
            }

            for (Object o : jsonDeltaData.keySet()) {
               if (o != null && o instanceof String && !STR.isEmpty((String) o)) {
                  jsonRegisterOutput.put(o, jsonDeltaData.get(o));
               }
            }

            jsonRegisterInput.put(ConstantsIF.DATA, jsonRegisterOutput);

            operRegisterInput = new Operation(OperationIF.TYPE.REPLACE);
            operRegisterInput.setJSON(jsonRegisterInput);

            /*
             * PUT to AM UMA resource_set JSON input: "data":
             * {"resource_scopes":[""],"name":"","type":""},"access_token":"","uid": "..."
             */
            operRegisterOutput = registerHandler.process(operRegisterInput);

            if (operRegisterOutput.getState() == STATE.ERROR) {
               this.abort(METHOD, "Could not update registration: " + operRegisterOutput.getStatus(),
                     Status.INTERNAL_SERVER_ERROR);
            }
         }
      }

      _logger.exiting(CLASS, METHOD);

      return;
   }
}
