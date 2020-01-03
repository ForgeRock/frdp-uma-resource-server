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
import javax.ws.rs.core.UriInfo;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

/**
 * Registration endpoint, PATH: .../rest/resources/{id}/register Manage the UMA
 * registration for a resource.
 *
 * @author Scott Fehrman, ForgeRock, Inc.
 */
public class RegisterResource extends RSResource {

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
   public RegisterResource(final String resourceUid, final UriInfo uriInfo, final ServletContext servletCtx,
         final HttpHeaders httpHdrs) {
      super();

      String METHOD = "RegisterResource()";

      _logger.entering(CLASS, METHOD);

      _resourceUid = resourceUid;
      _uriInfo = uriInfo;
      _servletCtx = servletCtx;
      _httpHdrs = httpHdrs;

      _logger.exiting(CLASS, METHOD);

      return;
   }

   /**
    * Read the resource UMA registration data.
    * 
    * <pre>
    * JSON output:
    * {
    *   "resource_scopes": ["..."], 
    *   "icon_uri": "..."
    * }
    * </pre>
    * 
    * @return Response HTTP response object
    */
   @GET
   @Produces(MediaType.APPLICATION_JSON)
   public Response read() {
      String METHOD = Thread.currentThread().getStackTrace()[1].getMethodName();
      Response response = null;
      JSONObject jsonData = null;
      JSONObject jsonOutput = null;
      OperationIF operRegisterOutput = null;

      _logger.entering(CLASS, METHOD);

      if (_logger.isLoggable(DEBUG_LEVEL)) {
         _logger.log(DEBUG_LEVEL, "Get registeration for resource: ''{0}''",
               (_resourceUid == null ? NULL : _resourceUid));
      }

      if (STR.isEmpty(_resourceUid)) {
         this.abort(METHOD, "Path resource is empty", Response.Status.BAD_REQUEST);
      }

      this.load();

      this.checkAuthenUserIsOwner(_resourceUid);

      operRegisterOutput = this.getRegistration(_resourceUid, null);

      if (operRegisterOutput != null && !operRegisterOutput.isError()) {
         jsonData = operRegisterOutput.getJSON();

         if (jsonData != null && !jsonData.isEmpty()) {
            jsonOutput = new JSONObject();
            jsonOutput.put(ConstantsIF.DATA, jsonData);

            operRegisterOutput.setJSON(jsonOutput);
         }
      }

      response = this.getResponseFromJSON(_uriInfo, operRegisterOutput);

      _logger.exiting(CLASS, METHOD);

      return response;
   }

   /**
    * Replace resource UMA registration data.
    * 
    * <pre>
    * minimum JSON input:
    * {
    *   "resource_scopes": ["..."]
    * }
    * </pre>
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
      OperationIF operOutput = null;
      JSONParser parser = null;

      _logger.entering(CLASS, METHOD);

      if (_logger.isLoggable(DEBUG_LEVEL)) {
         _logger.log(DEBUG_LEVEL, "Put registration for resource: ''{0}''",
               (_resourceUid == null ? NULL : _resourceUid));
      }

      if (STR.isEmpty(data)) {
         this.abort(METHOD, "Payload string is empty", Response.Status.BAD_REQUEST);
      }

      if (STR.isEmpty(_resourceUid)) {
         this.abort(METHOD, "Path resource is empty", Response.Status.BAD_REQUEST);
      }

      this.load();

      this.checkAuthenUserIsOwner(_resourceUid);

      parser = this.getParserFromCtx(_servletCtx);

      try {
         jsonData = (JSONObject) parser.parse(data);
      } catch (Exception ex) {
         this.abort(METHOD, "Could not parser String to JSON: '" + data + "', " + ex.getMessage(),
               Response.Status.INTERNAL_SERVER_ERROR);
      }

      operOutput = this.setRegistration(jsonData);

      response = this.getResponseFromJSON(_uriInfo, operOutput);

      _logger.exiting(CLASS, METHOD);

      return response;
   }

   /**
    * Delete resource UMA registration data.
    * 
    * @return Response HTTP response object
    */
   @DELETE
   public Response delete() {
      String METHOD = Thread.currentThread().getStackTrace()[1].getMethodName();
      Response response = null;
      JSONObject jsonInput = null;
      OperationIF operInput = null;
      OperationIF operOutput = null;

      _logger.entering(CLASS, METHOD);

      if (_logger.isLoggable(DEBUG_LEVEL)) {
         _logger.log(DEBUG_LEVEL, "Delete registration for resource: ''{0}''",
               (_resourceUid == null ? NULL : _resourceUid));
      }

      if (STR.isEmpty(_resourceUid)) {
         this.abort(METHOD, "Path resource is empty", Response.Status.BAD_REQUEST);
      }

      this.load();

      this.checkAuthenUserIsOwner(_resourceUid);

      jsonInput = new JSONObject();
      jsonInput.put(ConstantsIF.UID, _resourceUid);

      operInput = new Operation(OperationIF.TYPE.DELETE);
      operInput.setJSON(jsonInput);

      operOutput = this.deleteRegistration();

      response = this.getResponseFromJSON(_uriInfo, operOutput);

      _logger.exiting(CLASS, METHOD);

      return response;
   }

   /*
    * =============== PRIVATE METHODS ===============
    */
   /**
    * Set the resource's registration data
    * 
    * <pre>
    * JSON data:
    * {
    *   "resource_scopes": ["..."]
    * }
    * JSON input (via operInput):
    * {
    *   "owner": "bsmith",
    *   "register": "...", // AM resource_set _id
    *   ...
    * }
    * JSON output (via operOutput)
    * { "uid" : "..." }
    *
    * No "register" id, create UMA resource_set: POST to AM
    * else, update existing UMA resource_set: PUT to AM with "register" id
    * </pre>
    * 
    * @param jsonData JSONObject permission data
    * @return OperationIF output
    */
   private OperationIF setRegistration(final JSONObject jsonData) {
      String METHOD = Thread.currentThread().getStackTrace()[1].getMethodName();
      String registerId = null;
      String access_token = null;
      OperationIF operOutput = null;
      OperationIF operResourceInput = null;
      OperationIF operResourceOutput = null;
      OperationIF operRegisterInput = null;
      OperationIF operRegisterOutput = null;
      JSONArray arrayScopes = null;
      JSONObject jsonResourceInput = null;
      JSONObject jsonResourceOutput = null;
      JSONObject jsonResourceData = null;
      JSONObject jsonRegisterInput = null;
      JSONObject jsonRegisterOutput = null;
      JaxrsHandlerIF resourcesHandler = null;
      JaxrsHandlerIF registerHandler = null;

      /*
       */
      _logger.entering(CLASS, METHOD);

      arrayScopes = JSON.getArray(jsonData, ConstantsIF.RESOURCE_SCOPES);

      if (arrayScopes == null || arrayScopes.isEmpty()) {
         this.abort(METHOD, "JSON Array 'resource_scopes' is null or empty", Response.Status.BAD_REQUEST);
      }

      operOutput = new Operation(OperationIF.TYPE.REPLACE);

      access_token = this.getAccessToken(); // UMA PAT

      resourcesHandler = this.getHandler(JaxrsHandlerIF.HANDLER_RESOURCES);
      registerHandler = this.getHandler(JaxrsHandlerIF.HANDLER_UMA_REGISTER);

      operResourceOutput = this.getResource(_resourceUid);

      jsonResourceOutput = JSON.getObject(operResourceOutput.getJSON(), ConstantsIF.DATA);

      jsonData.put(ConstantsIF.NAME, JSON.getString(jsonResourceOutput, ConstantsIF.META + "." + ConstantsIF.NAME));
      jsonData.put(ConstantsIF.TYPE, JSON.getString(jsonResourceOutput, ConstantsIF.META + "." + ConstantsIF.TYPE));

      jsonRegisterInput = new JSONObject();
      jsonRegisterInput.put(ConstantsIF.DATA, jsonData);
      jsonRegisterInput.put(ConstantsIF.ACCESS_TOKEN, access_token);

      registerId = this.getRegisterGUID(_resourceUid);

      if (STR.isEmpty(registerId)) // not registered, create
      {
         // POST to AM UMA resource_set
         // JSON input:
         // {
         // "data": { "resource_scopes": [ "..." ], "name": "...", "type": "..." },
         // "access_token": "..."
         // }

         operRegisterInput = new Operation(OperationIF.TYPE.CREATE);
         operRegisterInput.setJSON(jsonRegisterInput);

         operRegisterOutput = registerHandler.process(operRegisterInput);

         if (operRegisterOutput.getState() == STATE.ERROR) {
            this.abort(METHOD, "Could not register resource: " + operRegisterOutput.getStatus(),
                  Response.Status.INTERNAL_SERVER_ERROR);
         }

         jsonRegisterOutput = operRegisterOutput.getJSON();

         registerId = JSON.getString(jsonRegisterOutput, ConstantsIF.UID);

         if (STR.isEmpty(registerId)) {
            this.abort(METHOD, "Failed to register resource, Id is empty", Response.Status.INTERNAL_SERVER_ERROR);
         }

         // update the resource, set the register id
         // JSON input:
         // {
         // "uid": "...", "data": { }
         // }

         jsonResourceData = jsonResourceOutput;
         jsonResourceData.put(ConstantsIF.REGISTER, registerId);

         jsonResourceInput = new JSONObject();
         jsonResourceInput.put(ConstantsIF.UID, _resourceUid);
         jsonResourceInput.put(ConstantsIF.DATA, jsonResourceData);

         operResourceInput = new Operation(OperationIF.TYPE.REPLACE);
         operResourceInput.setJSON(jsonResourceInput);

         operResourceOutput = resourcesHandler.process(operResourceInput);

         if (operResourceOutput.getState() == STATE.ERROR) {
            this.abort(METHOD, "Could not update resource with register Id: " + operResourceOutput.getStatus(),
                  Response.Status.INTERNAL_SERVER_ERROR);
         }
      } else // existing registration
      {
         // PUT to AM UMA resource_set
         // JSON input:
         // {
         // "data": { "resource_scopes": [ "..." ], "name": "...", "type": "..." },
         // "access_token": "...",
         // "uid": "..."
         // }

         jsonRegisterInput.put(ConstantsIF.UID, registerId);

         operRegisterInput = new Operation(OperationIF.TYPE.REPLACE);
         operRegisterInput.setJSON(jsonRegisterInput);

         operRegisterOutput = registerHandler.process(operRegisterInput);

         if (operRegisterOutput.getState() == STATE.ERROR) {
            this.abort(METHOD, "Could not update registration: " + operRegisterOutput.getStatus(),
                  Response.Status.INTERNAL_SERVER_ERROR);
         }
      }

      operOutput.setState(STATE.SUCCESS);
      operOutput.setStatus("Registration updated");
      operOutput.setJSON(new JSONObject());

      _logger.exiting(CLASS, METHOD);

      return operOutput;
   }

   /**
    * Delete resource's registration data Get the "credentials" for the owner
    * (logged-in user) Read the "resources" record, get the "register" uid If
    * "shared", there's a Policy ... delete that first Issue DELETE for the
    * registration Update the "resource" to remove the "register" data Set "access"
    * attribute to "private"
    * 
    * @return OperationIF output
    */
   private OperationIF deleteRegistration() {
      String METHOD = Thread.currentThread().getStackTrace()[1].getMethodName();
      String access_token = null;
      String registerId = null;
      String access = null;
      String owner = null;
      String sso_token = null;
      OperationIF operOutput = null;
      OperationIF operResourcesInput = null;
      OperationIF operResourcesOutput = null;
      OperationIF operRegisterInput = null;
      OperationIF operRegisterOutput = null;
      OperationIF operPolicyInput = null;
      OperationIF operPolicyOutput = null;
      JSONObject jsonResourcesInput = null;
      JSONObject jsonResourcesOutput = null;
      JSONObject jsonRegisterInput = null;
      JSONObject jsonRegisterOutput = null;
      JSONObject jsonPolicyInput = null;
      JSONObject jsonData = null;
      JaxrsHandlerIF resourcesHandler = null;
      JaxrsHandlerIF registerHandler = null;
      JaxrsHandlerIF policyHandler = null;

      _logger.entering(CLASS, METHOD);

      operOutput = new Operation(OperationIF.TYPE.DELETE);

      access_token = this.getAccessToken(); // UMA PAT

      resourcesHandler = this.getHandler(JaxrsHandlerIF.HANDLER_RESOURCES);
      registerHandler = this.getHandler(JaxrsHandlerIF.HANDLER_UMA_REGISTER);
      policyHandler = this.getHandler(JaxrsHandlerIF.HANDLER_UMA_POLICY);

      operResourcesOutput = this.getResource(_resourceUid);

      jsonResourcesOutput = JSON.getObject(operResourcesOutput.getJSON(), ConstantsIF.DATA);

      registerId = this.getRegisterGUID(_resourceUid);

      if (!STR.isEmpty(registerId)) {
         access = JSON.getString(jsonResourcesOutput, ConstantsIF.ACCESS);

         if (!STR.isEmpty(access) && access.equals(ConstantsIF.SHARED)) {
            /*
             * There's a Policy ... delete it
             */

            sso_token = this.getSSOTokenFromSSOSession();
            owner = this.getUserIdFromSSOSession();

            jsonPolicyInput = new JSONObject();
            jsonPolicyInput.put(ConstantsIF.UID, registerId);
            jsonPolicyInput.put(ConstantsIF.SSO_TOKEN, sso_token);
            jsonPolicyInput.put(ConstantsIF.OWNER, owner);

            operPolicyInput = new Operation(OperationIF.TYPE.DELETE);
            operPolicyInput.setJSON(jsonPolicyInput);

            // JSON input ...
            // { "uid" : "...", "sso_token": "...", "owner": "..." }

            operPolicyOutput = policyHandler.process(operPolicyInput);
         }

         jsonRegisterInput = new JSONObject();
         jsonRegisterInput.put(ConstantsIF.UID, registerId);
         jsonRegisterInput.put(ConstantsIF.ACCESS_TOKEN, access_token);

         operRegisterInput = new Operation(OperationIF.TYPE.DELETE);
         operRegisterInput.setJSON(jsonRegisterInput);

         // JSON input ...
         // { "uid" : "...", "access_token": "..." }
         // JSON output ...
         // { }

         operRegisterOutput = registerHandler.process(operRegisterInput);

         if (operRegisterOutput.getState() != STATE.SUCCESS) {
            jsonRegisterOutput = operRegisterOutput.getJSON();

            this.abort(METHOD,
                  "Could not delete registration for resource: STATE: " + operRegisterOutput.getState().toString()
                        + ": STATUS: " + operRegisterOutput.getStatus() + ": JSON: "
                        + (jsonRegisterOutput != null ? jsonRegisterOutput.toString() : "(null)"),
                  Response.Status.INTERNAL_SERVER_ERROR);
         }

         /*
          * Update the resource record, remove the "register" attribute
          */
         jsonData = jsonResourcesOutput;
         jsonData.remove(ConstantsIF.REGISTER);

         jsonResourcesInput = new JSONObject();
         jsonResourcesInput.put(ConstantsIF.UID, _resourceUid);
         jsonResourcesInput.put(ConstantsIF.DATA, jsonData);

         operResourcesInput = new Operation(OperationIF.TYPE.REPLACE);
         operResourcesInput.setJSON(jsonResourcesInput);

         operResourcesOutput = resourcesHandler.process(operResourcesInput);

         operOutput = operResourcesOutput;
      } else {
         operOutput.setState(STATE.NOTEXIST);
         operOutput.setStatus("Resource is not registered");
      }

      _logger.exiting(CLASS, METHOD);

      return operOutput;
   }
}
