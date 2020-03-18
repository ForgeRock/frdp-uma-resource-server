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
import javax.ws.rs.Produces;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.UriInfo;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

/**
 * Resource management: create, search, read, delete ... Routing for meta data,
 * content, registration information, and policies (permissions)
 *
 * <pre>
 * paths:
 * .../
 * .../{id}
 * .../{id}/meta
 * .../{id}/content
 * .../{id}/register
 * .../{id}/register/policy
 * </pre>
 *
 * @author Scott Fehrman, ForgeRock, Inc.
 */
public class ResourcesResource extends RSResource {

   private final String CLASS = this.getClass().getName();

   /**
    * Constructor
    *
    * @param uriInfo UriInfo uri information
    * @param servletCtx ServletContext context from the servlet
    * @param httpHdrs HttpHeaders header information
    */
   public ResourcesResource(final UriInfo uriInfo, final ServletContext servletCtx, final HttpHeaders httpHdrs) {
      super();

      String METHOD = "ResourcesResource()";

      _logger.entering(CLASS, METHOD);

      _uriInfo = uriInfo;
      _servletCtx = servletCtx;
      _httpHdrs = httpHdrs;

      _logger.exiting(CLASS, METHOD);

      return;
   }

   /**
    * Create resource. JSON data must contain a "meta" object. It may also
    * contain a "content" section, and/or "register" section.
    *
    * <pre>
    * Input JSON getResourcesIdContent:
    * {
    *   "meta": { ... },
    *   "content": { ... },  // optional
    *   "register": { ... }  // optional
    * }
    * </pre>
    *
    * @param data String resource payload as JSON
    * @return Response HTTP response object
    */
   @POST
   @Consumes(MediaType.APPLICATION_JSON)
   public Response create(String data) {
      String METHOD = Thread.currentThread().getStackTrace()[1].getMethodName();
      String userId = null;
      String resourceUid = null;
      Response response = null;
      JSONParser parser = null;
      JSONObject jsonNew = null;
      JSONObject jsonInput = null;
      JSONObject jsonData = null;
      JSONObject jsonMeta = null;
      JSONObject jsonContent = null;
      JSONObject jsonRegister = null;
      OperationIF operInput = null;
      OperationIF operNewOutput = null;
      JaxrsHandlerIF resourcesHandler = null;

      _logger.entering(CLASS, METHOD);

      if (_logger.isLoggable(DEBUG_LEVEL)) {
         _logger.log(DEBUG_LEVEL, "json=''{0}''",
            new Object[]{data != null ? data : NULL});
      }

      if (STR.isEmpty(data)) {
         this.abort(METHOD, "Payload string is empty", Status.BAD_REQUEST);
      }

      this.load();

      userId = this.getUserIdFromSSOSession();

      resourcesHandler = this.getHandler(JaxrsHandlerIF.HANDLER_RESOURCES);

      /*
       * Create a new (empty) resource, with "owner" attribute only
       */
      jsonNew = new JSONObject();
      jsonNew.put(ConstantsIF.OWNER, userId);

      jsonInput = new JSONObject();
      jsonInput.put(ConstantsIF.DATA, jsonNew);

      operInput = new Operation(OperationIF.TYPE.CREATE);
      operInput.setJSON(jsonInput);

      operNewOutput = resourcesHandler.process(operInput);

      resourceUid = this.getUidFromOperation(operNewOutput);

      /*
       * Get the parser and parser the incoming data
       */
      parser = this.getParserFromCtx(_servletCtx);

      try {
         jsonData = (JSONObject) parser.parse(data);
      } catch (Exception ex) {
         this.abort(METHOD, "Could not parser String to JSON: '" + data + "', "
            + ex.getMessage(), Status.BAD_REQUEST);
      }

      /*
       * process the "meta" data
       */
      jsonMeta = JSON.getObject(jsonData, ConstantsIF.META);

      if (jsonMeta != null && !jsonMeta.isEmpty()) {
         this.setMeta(resourceUid, jsonMeta);
      }

      /*
       * process the "content" ... INPUT will either have "data" or "uri" ...
       * {                          | {
       *    "id": "default",        |    "id": "refonly",
       *    "data": { ... }         |    "uri": "http://..."
       * }                          | }
       */
      jsonContent = JSON.getObject(jsonData, ConstantsIF.CONTENT);

      if (jsonContent != null && !jsonContent.isEmpty()) {
         this.contentCreate(resourceUid, jsonContent);
      }

      /*
       * process the "register" data
       */
      jsonRegister = JSON.getObject(jsonData, ConstantsIF.REGISTER);

      if (jsonRegister != null && !jsonRegister.isEmpty()) {
         this.setRegistration(resourceUid, jsonRegister);
      }

      response = this.getResponseFromJSON(_uriInfo, operNewOutput);

      _logger.exiting(CLASS, METHOD);

      return response;
   }

   /**
    * Get all resources that are owned by the authenticated user
    *
    * @return Response HTTP response object
    */
   @GET
   @Produces(MediaType.APPLICATION_JSON)
   public Response search() {
      String METHOD = Thread.currentThread().getStackTrace()[1].getMethodName();
      String userId = null;
      Response response = null;
      JSONObject jsonQuery = null;
      JSONObject jsonInput = null;
      OperationIF operInput = null;
      OperationIF operOutput = null;
      JaxrsHandlerIF resourcesHandler = null;

      _logger.entering(CLASS, METHOD);

      this.load();

      userId = this.getUserIdFromSSOSession(); // make sure authenticated user

      resourcesHandler = this.getHandler(JaxrsHandlerIF.HANDLER_RESOURCES);

      jsonQuery = new JSONObject();
      jsonQuery.put(ConstantsIF.OPERATOR, ConstantsIF.EQUAL);
      jsonQuery.put(ConstantsIF.ATTRIBUTE, ConstantsIF.DATA + "." + ConstantsIF.OWNER);
      jsonQuery.put(ConstantsIF.VALUE, userId);

      jsonInput = new JSONObject();
      jsonInput.put(ConstantsIF.QUERY, jsonQuery);

      operInput = new Operation(OperationIF.TYPE.SEARCH);
      operInput.setJSON(jsonInput);

      operOutput = resourcesHandler.process(operInput);

      response = this.getResponseFromJSON(_uriInfo, operOutput);

      _logger.exiting(CLASS, METHOD);

      return response;
   }

   /**
    * Read a specific resource, include meta data, content, registration and
    * policy
    *
    * <pre>
    * JSON output ...
    * {
    *     "owner": "bjensen",
    *     "access": "shared",
    *     "meta": { ... },
    *     "content": { ... },
    *     "register": {
    *         ... ,
    *         "policy": { ... }
    *     }
    * }
    * </pre>
    *
    * @param resourceUid String resource identifier from the URI path
    * @param content Display mode for content: "data" | "reference"
    * @return Response HTTP response object
    */
   @GET
   @Path("{" + ConstantsIF.RESOURCE + "}")
   @Produces(MediaType.APPLICATION_JSON)
   public Response read(@PathParam(ConstantsIF.RESOURCE) String resourceUid,
      @QueryParam(ConstantsIF.CONTENT) String content) {
      String METHOD = Thread.currentThread().getStackTrace()[1].getMethodName();
      String access = null;
      String userId = null;
      String csId = null; // Content Service identifier
      String csUri = null; // Content Service URI
      Response response = null;
      JSONObject jsonOutput = null;
      JSONObject jsonData = null;
      JSONObject jsonMeta = null;
      JSONObject jsonContent = null;
      JSONObject jsonRegister = null;
      JSONObject jsonPolicy = null;
      JSONObject jsonOptions = null;
      OperationIF operResourceOutput = null;
      OperationIF operMetaOutput = null;
      OperationIF operContentOutput = null;
      OperationIF operRegisterOutput = null;
      OperationIF operPolicyOutput = null;

      _logger.entering(CLASS, METHOD);

      if (STR.isEmpty(resourceUid)) {
         this.abort(METHOD, "Path parameter 'resource' is empty",
            Status.BAD_REQUEST);
      }

      this.load();

      userId = this.getUserIdFromSSOSession();

      if (_logger.isLoggable(DEBUG_LEVEL)) {
         _logger.log(DEBUG_LEVEL, "resourceUid=''{0}'', userId=''{1}'''",
            new Object[]{resourceUid == null ? NULL : resourceUid,
               userId == null ? NULL : userId});
      }
      
      if (!STR.isEmpty(content)) {
         jsonOptions = new JSONObject();
         
         if (content.equalsIgnoreCase(ConstantsIF.REFERENCE)) {
            jsonOptions.put(ConstantsIF.CONTENT, ConstantsIF.REFERENCE);
         } else { // default: "data"
            jsonOptions.put(ConstantsIF.CONTENT, ConstantsIF.DATA);
         }
      }

      operResourceOutput = this.getResource(resourceUid);

      /*
       * JSON resource output ...
       * {
       *     "uid": "...",
       *     "data": { 
       *         "owner": "...", 
       *         "meta": { ... }, 
       *         "content": { ... },
       *         "register": "..." 
       *      },
       *     "timestamps": { ... }
       * }
       */
      if (operResourceOutput != null && !operResourceOutput.isError()) {
         this.checkAuthenUserIsOwner(resourceUid);

         access = ConstantsIF.PRIVATE; // default access level

         jsonOutput = operResourceOutput.getJSON();

         if (jsonOutput != null && !jsonOutput.isEmpty()) {
            jsonData = JSON.getObject(jsonOutput, ConstantsIF.DATA);

            if (jsonData != null && !jsonData.isEmpty()) {

               /*
                * Get "meta"
                */
               operMetaOutput = this.getMeta(resourceUid);
               jsonMeta = operMetaOutput.getJSON();
               jsonData.put(ConstantsIF.META, JSON.getObject(jsonMeta, ConstantsIF.DATA));

               /*
                * Get "content"
                */
               csId = JSON.getString(jsonData, ConstantsIF.CONTENT + "." + ConstantsIF.ID);
               csUri = JSON.getString(jsonData, ConstantsIF.CONTENT + "." + ConstantsIF.URI);

               if (!STR.isEmpty(csId) && !STR.isEmpty(csUri)) {
                  operContentOutput = this.contentRead(resourceUid, jsonOptions);

                  /*
                   * JSON content output options:
                   * {                       | {
                   *     ...                 |     "uri": "http://..."
                   * }                       | }
                   */
                  if (operContentOutput != null) {
                     jsonContent = operContentOutput.getJSON();

                     if (jsonContent != null) {
                        jsonData.put(ConstantsIF.CONTENT, jsonContent);
                     }
                  }
               }

               /*
                * Get "registration"
                */
               operRegisterOutput = this.getRegistration(resourceUid, null);
               jsonRegister = operRegisterOutput.getJSON();

               if (jsonRegister != null && !jsonRegister.isEmpty()) {
                  access = ConstantsIF.REGISTERED; // Found registration

                  operPolicyOutput = this.getPolicy(resourceUid);
                  jsonPolicy = operPolicyOutput.getJSON();

                  if (jsonPolicy != null && !jsonPolicy.isEmpty()) {
                     access = ConstantsIF.SHARED; // Found policy

                     jsonRegister.put(ConstantsIF.POLICY, jsonPolicy);
                  }

                  jsonData.put(ConstantsIF.REGISTER, jsonRegister);
               }
            } else {
               this.abort(METHOD, "The JSON 'data' object is null or empty, ResourceId: '"
                  + resourceUid + "'",
                  Status.INTERNAL_SERVER_ERROR);
            }

            jsonData.put(ConstantsIF.ACCESS, access);
         } else {
            this.abort(METHOD, "The Resource JSON object is null or empty, ResourceId: '"
               + resourceUid + "'",
               Status.INTERNAL_SERVER_ERROR);
         }
      } else {
         if (operResourceOutput.getState() == STATE.NOTEXIST) {
            operResourceOutput.setError(false);
         }
      }

      response = this.getResponseFromJSON(_uriInfo, operResourceOutput);

      _logger.exiting(CLASS, METHOD);

      return response;
   }

   /**
    * Delete resource ... including: content, meta data, registration, policy
    *
    * @param resourceUid String resource identifier from the URI path
    * @return Response HTTP response object
    */
   @DELETE
   @Path("{" + ConstantsIF.RESOURCE + "}")
   public Response delete(@PathParam(ConstantsIF.RESOURCE) String resourceUid) {
      String METHOD = Thread.currentThread().getStackTrace()[1].getMethodName();
      String registerUid = null;
      String access_token = null;
      String userId = null;
      Response response = null;
      JSONObject jsonResourceInput = null;
      JSONObject jsonResourceOutput = null;
      JSONObject jsonResourceData = null;
      JSONObject jsonRegisterInput = null;
      OperationIF operResourceInput = null;
      OperationIF operResourceOutput = null;
      OperationIF operRegisterInput = null;
      OperationIF operRegisterOutput = null;
      JaxrsHandlerIF resourcesHandler = null;
      JaxrsHandlerIF registerHandler = null;

      _logger.entering(CLASS, METHOD);

      if (STR.isEmpty(resourceUid)) {
         this.abort(METHOD, "Path resource is empty", Status.BAD_REQUEST);
      }

      this.load();

      userId = this.getUserIdFromSSOSession();

      if (_logger.isLoggable(DEBUG_LEVEL)) {
         _logger.log(DEBUG_LEVEL, "resourceUid=''{0}'', userId=''{1}''",
            new Object[]{resourceUid == null ? NULL : resourceUid,
               userId == null ? NULL : userId});
      }

      operResourceOutput = this.getResource(resourceUid);

      if (operResourceOutput != null && !operResourceOutput.isError()) {
         this.checkAuthenUserIsOwner(resourceUid);

         /*
          * Get the Handlers: "resources", "content", "register"
          */
         resourcesHandler = this.getHandler(JaxrsHandlerIF.HANDLER_RESOURCES);
         registerHandler = this.getHandler(JaxrsHandlerIF.HANDLER_UMA_REGISTER);

         jsonResourceInput = new JSONObject();
         jsonResourceInput.put(ConstantsIF.UID, resourceUid);

         operResourceInput = new Operation(OperationIF.TYPE.READ);
         operResourceInput.setJSON(jsonResourceInput);

         // Read the resource, need to check / get content uid and register uid
         // JSON input ...
         // { 
         //     "uid": "..." 
         // }
         // JSON output ...
         // {
         //     "owner": "...",
         //     "content": "...",
         //     "register": "..."
         // }
         if (operResourceOutput.getState() != STATE.SUCCESS) {
            this.abort(METHOD, ": Could not read resource: "
               + operResourceOutput.getState().toString() + ", "
               + operResourceOutput.getStatus(), Status.INTERNAL_SERVER_ERROR);
         }

         jsonResourceOutput = operResourceOutput.getJSON();

         jsonResourceData = JSON.getObject(jsonResourceOutput, ConstantsIF.DATA);

         registerUid = JSON.getString(jsonResourceData, ConstantsIF.REGISTER);

         if (!STR.isEmpty(registerUid)) {
            /*
             * Delete the registration
             */
            access_token = this.getAccessToken();

            jsonRegisterInput = new JSONObject();
            jsonRegisterInput.put(ConstantsIF.UID, registerUid);
            jsonRegisterInput.put(ConstantsIF.ACCESS_TOKEN, access_token);

            operRegisterInput = new Operation(OperationIF.TYPE.DELETE);
            operRegisterInput.setJSON(jsonRegisterInput);

            /*
             * JSON input ... 
             * { 
             *     "uid": "...", 
             *     "access_token": "..." 
             * }
             */
            operRegisterOutput = registerHandler.process(operRegisterInput);

            if (operRegisterOutput.getState() != STATE.SUCCESS) {
               this.abort(METHOD, ": Could not delete registration: "
                  + operRegisterOutput.getState().toString() + ", "
                  + operRegisterOutput.getStatus(), Status.INTERNAL_SERVER_ERROR);
            }
         }

         /*
          * Delete the content
          */
         this.contentDelete(resourceUid);

         /*
          * Delete the resource
          */
         operResourceInput = new Operation(OperationIF.TYPE.DELETE);
         operResourceInput.setJSON(jsonResourceInput);

         /*
          * JSON input ... 
          * { 
          *     "uid": "..." 
          * }
          */
         operResourceOutput = resourcesHandler.process(operResourceInput);

         if (operResourceOutput.getState() != STATE.SUCCESS) {
            this.abort(METHOD, ": Could not read resource: "
               + operResourceOutput.getState().toString() + ", "
               + operResourceOutput.getStatus(), Status.INTERNAL_SERVER_ERROR);
         }
      } else {
         if (operResourceOutput.getState() == STATE.NOTEXIST) {
            operResourceOutput.setError(false);
         }
      }

      operResourceOutput.setType(OperationIF.TYPE.DELETE);

      response = this.getResponseFromJSON(_uriInfo, operResourceOutput);

      _logger.exiting(CLASS, METHOD);

      return response;
   }

   /**
    * Route sub-path "{id}/meta" to manage resource meta data
    *
    * @param resourceUid String resource identifier from the URI path
    * @return MetaResource
    */
   @Path("{" + ConstantsIF.ID + "}/" + ConstantsIF.META)
   public MetaResource useMeta(@PathParam(ConstantsIF.ID) String resourceUid) {
      String METHOD = Thread.currentThread().getStackTrace()[1].getMethodName();
      MetaResource metaResource = null;

      _logger.entering(CLASS, METHOD);

      metaResource = new MetaResource(resourceUid, _uriInfo, _servletCtx, _httpHdrs);

      _logger.exiting(CLASS, METHOD);

      return metaResource;
   }

   /**
    * Route sub-path "{id}/content" to manage resource related JSON content
    *
    * @param resourceUid String resource identifier from the URI path
    * @return ContentResource
    */
   @Path("{" + ConstantsIF.ID + "}/" + ConstantsIF.CONTENT)
   public ContentResource useContent(@PathParam(ConstantsIF.ID) String resourceUid) {
      String METHOD = Thread.currentThread().getStackTrace()[1].getMethodName();
      ContentResource contentResource = null;

      _logger.entering(CLASS, METHOD);

      contentResource = new ContentResource(resourceUid, _uriInfo, _servletCtx, _httpHdrs);

      _logger.exiting(CLASS, METHOD);

      return contentResource;
   }

   /**
    * Route sub-path "{id}/register" to manage resource UMA registration
    *
    * @param resourceUid String resource identifier from the URI path
    * @return RegisterResource
    */
   @Path("{" + ConstantsIF.ID + "}/" + ConstantsIF.REGISTER)
   public RegisterResource useRegister(@PathParam(ConstantsIF.ID) String resourceUid) {
      String METHOD = Thread.currentThread().getStackTrace()[1].getMethodName();
      RegisterResource registerResource = null;

      _logger.entering(CLASS, METHOD);

      registerResource = new RegisterResource(resourceUid, _uriInfo, _servletCtx, _httpHdrs);

      _logger.exiting(CLASS, METHOD);

      return registerResource;
   }

   /**
    * Route sub-path "{id}/register/policy" to manage resource UMA policies
    * (permissions)
    *
    * @param resourceUid String resource identifier from the URI path
    * @return PolicyResource
    */
   @Path("{" + ConstantsIF.ID + "}/" + ConstantsIF.REGISTER + "/" + ConstantsIF.POLICY)
   public PolicyResource useRegsiterPolicy(@PathParam(ConstantsIF.ID) String resourceUid) {
      String METHOD = Thread.currentThread().getStackTrace()[1].getMethodName();
      PolicyResource policyResource = null;

      _logger.entering(CLASS, METHOD);

      policyResource = new PolicyResource(resourceUid, _uriInfo, _servletCtx, _httpHdrs);

      _logger.exiting(CLASS, METHOD);

      return policyResource;
   }

   /*
    * =============== PRIVATE METHODS ===============
    */
   /**
    * Set meta data
    *
    * @param resourceUid String resource identifier
    * @param jsonMeta JSONObject input
    */
   private void setMeta(final String resourceUid, final JSONObject jsonMeta) {
      String METHOD = Thread.currentThread().getStackTrace()[1].getMethodName();
      OperationIF operMetaInput = null;
      OperationIF operMetaOutput = null;
      JSONObject jsonInput = null;
      JaxrsHandlerIF metaHandler = null;

      /*
       * Replace the "meta" sub-object, under the "data" object
       */
      _logger.entering(CLASS, METHOD);

      metaHandler = this.getHandler(JaxrsHandlerIF.HANDLER_META);

      jsonInput = new JSONObject();
      jsonInput.put(ConstantsIF.UID, resourceUid);
      jsonInput.put(ConstantsIF.DATA, jsonMeta);

      operMetaInput = new Operation(OperationIF.TYPE.REPLACE);
      operMetaInput.setJSON(jsonInput);

      operMetaOutput = metaHandler.process(operMetaInput);

      if (operMetaOutput.getState() != STATE.SUCCESS) {
         this.abort(METHOD, "Could not update meta data: "
            + operMetaOutput.getState().toString() + ", "
            + operMetaOutput.getStatus(), Status.INTERNAL_SERVER_ERROR);
      }

      _logger.exiting(CLASS, METHOD);

      return;
   }

   /**
    * Set UMA registration
    *
    * <pre>
    * JSON register input
    * {
    *   "resource_scopes": [ "...", "...", ... ]
    *   "icon_uri": "http://..."
    *   "policy": {
    *      ...
    *   }
    * }
    * </pre>
    *
    * @param resourceUid String resource identifier
    * @param jsonInputData JSONObject input
    */
   private void setRegistration(final String resourceUid, final JSONObject jsonInputData) {
      String METHOD = Thread.currentThread().getStackTrace()[1].getMethodName();
      String access_token = null;
      String registerUid = null;
      OperationIF operResourceInput = null;
      OperationIF operResourceOutput = null;
      OperationIF operRegisterInput = null;
      OperationIF operRegisterOutput = null;
      JSONObject jsonResourceInput = null;
      JSONObject jsonResourceData = null;
      JSONObject jsonRegisterData = null;
      JSONObject jsonRegisterInput = null;
      JSONObject jsonRegisterOutput = null;
      JSONObject jsonPolicyData = null;
      JSONArray arrayResourceScopes = null;
      JaxrsHandlerIF resourcesHandler = null;
      JaxrsHandlerIF registerHandler = null;

      _logger.entering(CLASS, METHOD);

      /*
       * Get the handlers ... "resources" and "register"
       */
      resourcesHandler = this.getHandler(JaxrsHandlerIF.HANDLER_RESOURCES);
      registerHandler = this.getHandler(JaxrsHandlerIF.HANDLER_UMA_REGISTER);

      arrayResourceScopes = JSON.getArray(jsonInputData, ConstantsIF.RESOURCE_SCOPES);

      if (arrayResourceScopes == null || arrayResourceScopes.isEmpty()) {
         this.abort(METHOD, "JSON Array 'resource_scopes' is null or empty",
            Status.BAD_REQUEST);
      }

      /*
       * Read the new Resource
       */
      jsonResourceInput = new JSONObject();
      jsonResourceInput.put(ConstantsIF.UID, resourceUid);

      operResourceInput = new Operation(OperationIF.TYPE.READ);
      operResourceInput.setJSON(jsonResourceInput);

      // JSON input ...
      // { 
      //     "uid": "..." 
      // }
      // JSON output ...
      // {
      //     "data": {
      //     "owner": "...",
      //     "meta": { 
      //         "name": "...", 
      //         "type": "...", 
      //         ... 
      //     },
      // ...,
      // }
      // }
      operResourceOutput = resourcesHandler.process(operResourceInput);

      if (operResourceOutput.getState() != STATE.SUCCESS) {
         this.abort(METHOD, "Could not read resource: "
            + operResourceOutput.getState().toString() + ", "
            + operResourceOutput.getStatus(), Status.INTERNAL_SERVER_ERROR);
      }

      jsonResourceData = JSON.getObject(operResourceOutput.getJSON(), ConstantsIF.DATA);

      if (jsonResourceData == null || jsonResourceData.isEmpty()) {
         this.abort(METHOD, "Resource JSON data is null or empty", Status.BAD_REQUEST);
      }

      /*
       * Check for a "policy" object in the JSON input If exists, copy it and save it
       * to the Policy Input remove it from the JSON input
       */
      jsonPolicyData = JSON.getObject(jsonInputData, ConstantsIF.POLICY);

      if (jsonPolicyData != null) {
         jsonInputData.remove(ConstantsIF.POLICY);
      }

      /*
       * Create / Register the resource
       */
      access_token = this.getAccessToken();

      if (STR.isEmpty(access_token)) {
         this.abort(CLASS + ": " + METHOD, "access_token is empty", Status.BAD_REQUEST);
      }

      jsonRegisterData = jsonInputData;

      jsonRegisterData.put(ConstantsIF.NAME,
         JSON.getString(jsonResourceData, ConstantsIF.META + "." + ConstantsIF.NAME));

      jsonRegisterData.put(ConstantsIF.TYPE,
         JSON.getString(jsonResourceData, ConstantsIF.META + "." + ConstantsIF.TYPE));

      jsonRegisterInput = new JSONObject();
      jsonRegisterInput.put(ConstantsIF.DATA, jsonRegisterData);
      jsonRegisterInput.put(ConstantsIF.ACCESS_TOKEN, access_token);

      operRegisterInput = new Operation(OperationIF.TYPE.CREATE);
      operRegisterInput.setJSON(jsonRegisterInput);

      // JSON input ...
      // {
      //     "data": { 
      //         "name": "", 
      //         "type": "", 
      //         "resource_scopes": ["view"], 
      //         "icon_uri": ""
      //     },
      //     "access_token": "..."
      // }
      // JSON output ...
      // { 
      //     "uid": "..." 
      // }
      operRegisterOutput = registerHandler.process(operRegisterInput);

      if (operRegisterOutput.getState() != STATE.SUCCESS) {
         this.abort(CLASS + ": " + METHOD, "Could not register resource: "
            + operRegisterOutput.getState().toString() + ", "
            + operRegisterOutput.getStatus(), Status.INTERNAL_SERVER_ERROR);
      }

      jsonRegisterOutput = operRegisterOutput.getJSON();

      registerUid = JSON.getString(jsonRegisterOutput, ConstantsIF.UID);

      if (STR.isEmpty(registerUid)) {
         this.abort(CLASS + ": " + METHOD, "UMA Registration Uid is empty: "
            + operRegisterOutput.getState().toString() + ", "
            + operRegisterOutput.getStatus(), Status.INTERNAL_SERVER_ERROR);
      }

      /*
       * Update the resource with the registration uid
       */
      jsonResourceData.put(ConstantsIF.REGISTER, registerUid);

      jsonResourceInput = new JSONObject();
      jsonResourceInput.put(ConstantsIF.UID, resourceUid);
      jsonResourceInput.put(ConstantsIF.DATA, jsonResourceData);

      operResourceInput = new Operation(OperationIF.TYPE.REPLACE);
      operResourceInput.setJSON(jsonResourceInput);

      // JSON input ...
      // {
      //     "uid": "...",
      //     "data": { 
      //         "owner": "...", 
      //         "meta": { ... }, 
      //         "register": "..." 
      //     }
      // }
      operResourceOutput = resourcesHandler.process(operResourceInput);

      if (operResourceOutput.getState() != STATE.SUCCESS) {
         this.abort(METHOD, "Could not update resource: "
            + operResourceOutput.getState().toString() + ", "
            + operResourceOutput.getStatus(), Status.INTERNAL_SERVER_ERROR);
      }

      if (jsonPolicyData != null && !jsonPolicyData.isEmpty()) {
         this.setPermissions(resourceUid, jsonPolicyData);
      }

      _logger.exiting(CLASS, METHOD);

      return;
   }

   /**
    * Set resource permissions
    *
    * <pre>
    * JSON input
    * {
    *   "permissions" : [
    *     {
    *       "subject": "...",
    *       "scopes": [ "...", ... ]
    *     },
    *     { ... },
    *     ...
    *   ]
    * }
    * </pre>
    *
    * @param resourceUid String resource identifier
    * @param jsonPolicyData JSONObject input
    */
   private void setPermissions(final String resourceUid, final JSONObject jsonPolicyData) {
      String METHOD = Thread.currentThread().getStackTrace()[1].getMethodName();
      String registerId = null;
      String sso_token = null;
      String owner = null;
      OperationIF operResourceInput = null;
      OperationIF operResourceOutput = null;
      OperationIF operPolicyInput = null;
      OperationIF operPolicyOutput = null;
      JSONObject jsonResourceInput = null;
      JSONObject jsonResourceData = null;
      JSONObject jsonPolicyInput = null;
      JSONArray arrayPermissions = null;
      JaxrsHandlerIF resourcesHandler = null;
      JaxrsHandlerIF policyHandler = null;

      _logger.entering(CLASS, METHOD);

      /*
       * Get the handlers ... "resources" and "policy"
       */
      resourcesHandler = this.getHandler(JaxrsHandlerIF.HANDLER_RESOURCES);
      policyHandler = this.getHandler(JaxrsHandlerIF.HANDLER_UMA_POLICY);

      arrayPermissions = JSON.getArray(jsonPolicyData, ConstantsIF.PERMISSIONS);

      if (arrayPermissions == null || arrayPermissions.isEmpty()) {
         this.abort(METHOD, "JSON Array 'permissions' is null or empty",
            Status.BAD_REQUEST);
      }

      /*
       * Read the new Resource
       */
      jsonResourceInput = new JSONObject();
      jsonResourceInput.put(ConstantsIF.UID, resourceUid);

      operResourceInput = new Operation(OperationIF.TYPE.READ);
      operResourceInput.setJSON(jsonResourceInput);

      // JSON input ...
      // { 
      //     "uid": "..." 
      // }
      // JSON output ...
      // {
      //     "owner": "...", 
      //     "meta": { "name": "...", "type": "...", ... },
      //     "register": "..."
      // }
      operResourceOutput = resourcesHandler.process(operResourceInput);

      if (operResourceOutput.getState() != STATE.SUCCESS) {
         this.abort(METHOD, "Could not read resource: "
            + operResourceOutput.getState().toString() + ", "
            + operResourceOutput.getStatus(), Status.INTERNAL_SERVER_ERROR);
      }

      jsonResourceData = JSON.getObject(operResourceOutput.getJSON(), ConstantsIF.DATA);

      registerId = JSON.getString(jsonResourceData, ConstantsIF.REGISTER);

      if (STR.isEmpty(registerId)) {
         this.abort(METHOD, "Registration Id is empty", Status.BAD_REQUEST);
      }

      sso_token = this.getSSOTokenFromSSOSession();
      owner = this.getUserIdFromSSOSession();

      jsonPolicyInput = new JSONObject();
      jsonPolicyInput.put(ConstantsIF.UID, registerId);
      jsonPolicyInput.put(ConstantsIF.SSO_TOKEN, sso_token);
      jsonPolicyInput.put(ConstantsIF.OWNER, owner);
      jsonPolicyInput.put(ConstantsIF.DATA, jsonPolicyData);

      operPolicyInput = new Operation(OperationIF.TYPE.CREATE);

      operPolicyInput.setJSON(jsonPolicyInput);

      // JSON input ...
      // {
      //     "uid" : "...", 
      //     "sso_token": "...", 
      //     "owner": "...", 
      //     "data": { ... }
      // }
      // JSON output ...
      // {}
      operPolicyOutput = policyHandler.process(operPolicyInput);

      if (operPolicyOutput.getState() != STATE.SUCCESS) {
         this.abort(METHOD, "Could not set policy for resource: "
            + operPolicyOutput.getState().toString() + ", "
            + operPolicyOutput.getStatus(), Status.INTERNAL_SERVER_ERROR);
      }

      _logger.exiting(CLASS, METHOD);

      return;
   }
}
