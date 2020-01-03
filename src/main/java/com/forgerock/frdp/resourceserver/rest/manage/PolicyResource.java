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
import javax.ws.rs.PUT;
import javax.ws.rs.Produces;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

/**
 * Policy endpoint, PATH: .../rest/resources/{id}/register/policy
 *
 * @author Scott Fehrman, ForgeRock, Inc.
 */
public class PolicyResource extends RSResource {

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
   public PolicyResource(final String resourceUid, final UriInfo uriInfo, final ServletContext servletCtx,
         final HttpHeaders httpHdrs) {
      super();

      String METHOD = "PolicyResource()";

      _logger.entering(CLASS, METHOD);

      _resourceUid = resourceUid;
      _uriInfo = uriInfo;
      _servletCtx = servletCtx;
      _httpHdrs = httpHdrs;

      _logger.exiting(CLASS, METHOD);

      return;
   }

   /**
    * Read resource policy (permissions)
    * 
    * <pre>
    * JSON output:
    * {
    *   "permissions": [
    *     {
    *       "subject": "...",
    *       "scopes": [ "view" ]
    *     },
    *     ...
    *   ]
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
      OperationIF operOutput = null;
      JSONObject jsonOutput = null;

      _logger.entering(CLASS, METHOD);

      if (_logger.isLoggable(DEBUG_LEVEL)) {
         _logger.log(DEBUG_LEVEL, "Get policy for resource: ''{0}''", (_resourceUid == null ? NULL : _resourceUid));
      }

      if (STR.isEmpty(_resourceUid)) {
         this.abort(METHOD, "Path resource is empty", Response.Status.BAD_REQUEST);
      }

      this.load();

      this.checkAuthenUserIsOwner(_resourceUid);

      operOutput = this.getPolicy(_resourceUid);

      jsonOutput = new JSONObject();
      jsonOutput.put(ConstantsIF.DATA, operOutput.getJSON());

      operOutput.setJSON(jsonOutput);

      response = this.getResponseFromJSON(_uriInfo, operOutput);

      _logger.exiting(CLASS, METHOD);

      return response;
   }

   /**
    * Replace resource policy (permissions)
    * 
    * <pre>
    * JSON input
    * {
    *   "permissions": [
    *     {
    *       "subject": "aadams",
    *       "scopes": [ "view" ]
    *     }
    *   ]
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
         _logger.log(DEBUG_LEVEL, "Put policy for resource: ''{0}''", (_resourceUid == null ? NULL : _resourceUid));
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

      operOutput = this.setPermissions(jsonData);

      response = this.getResponseFromJSON(_uriInfo, operOutput);

      _logger.exiting(CLASS, METHOD);

      return response;
   }

   /**
    * Delete resource policy (permissions)
    * 
    * @return Response HTTP response object
    */
   @DELETE 
   public Response delete() {
      String METHOD = Thread.currentThread().getStackTrace()[1].getMethodName();
      Response response = null;
      OperationIF operOutput = null;

      _logger.entering(CLASS, METHOD);

      if (_logger.isLoggable(DEBUG_LEVEL)) {
         _logger.log(DEBUG_LEVEL, "Delete policy for resource: ''{0}''", (_resourceUid == null ? NULL : _resourceUid));
      }

      if (STR.isEmpty(_resourceUid)) {
         this.abort(METHOD, "Path resource is empty", Response.Status.BAD_REQUEST);
      }

      this.load();

      this.checkAuthenUserIsOwner(_resourceUid);

      operOutput = this.deletePermissions(_resourceUid);

      response = this.getResponseFromJSON(_uriInfo, operOutput);

      _logger.exiting(CLASS, METHOD);

      return response;
   }
   /*
    * =============== PRIVATE METHODS ===============
    */

   /**
    * Set the registered resource permissions. Need to detemine if a policy exists
    * If no policy ... perform a POST, Else ... perform a PUT
    *
    * <pre>
    * JSON data:
    * {
    *   "permissions" : [
    *     {
    *       "subject": "...",
    *       "scopes": ["view"]
    *     }
    *   ]
    * }
    * </pre>
    * 
    * @param jsonData JSONObject permission data
    * @return OperationIF output
    */
   private OperationIF setPermissions(final JSONObject jsonData) {
      String METHOD = Thread.currentThread().getStackTrace()[1].getMethodName();
      String sso_token = null;
      String owner = null;
      String registerId = null;
      OperationIF operOutput = null;
      OperationIF operReadOutput = null;
      OperationIF operPolicyInput = null;
      OperationIF operPolicyOutput = null;
      JSONObject jsonPolicyInput = null;
      JaxrsHandlerIF policyHandler = null;

      _logger.entering(CLASS, METHOD);

      operOutput = new Operation(OperationIF.TYPE.READ);

      policyHandler = this.getHandler(JaxrsHandlerIF.HANDLER_UMA_POLICY);

      registerId = this.getRegisterGUID(_resourceUid);

      if (!STR.isEmpty(registerId)) {
         sso_token = this.getSSOTokenFromSSOSession();
         owner = this.getUserIdFromSSOSession();

         jsonPolicyInput = new JSONObject();
         jsonPolicyInput.put(ConstantsIF.UID, registerId);
         jsonPolicyInput.put(ConstantsIF.SSO_TOKEN, sso_token);
         jsonPolicyInput.put(ConstantsIF.OWNER, owner);
         jsonPolicyInput.put(ConstantsIF.DATA, jsonData);

         operReadOutput = this.getPolicy(_resourceUid);

         if (operReadOutput.getState() == STATE.SUCCESS) // Policy found
         {
            operPolicyInput = new Operation(OperationIF.TYPE.REPLACE);
         } else // Policy missing
         {
            operPolicyInput = new Operation(OperationIF.TYPE.CREATE);
         }

         operPolicyInput.setJSON(jsonPolicyInput);

         // JSON input ...
         // { "uid" : "...", "sso_token": "...", "owner": "...", "data": { ... } }
         // JSON output ...
         // { }

         operPolicyOutput = policyHandler.process(operPolicyInput);

         operOutput = operPolicyOutput;
      } else {
         operOutput.setError(true);
         operOutput.setState(STATE.NOTEXIST);
         operOutput.setStatus("Resource is not registered");
      }

      _logger.exiting(CLASS, METHOD);

      return operOutput;
   }

   /**
    * Delete permissions.
    *
    * @param resourceUid String resource id
    * @return OperationIF output
    */
   private OperationIF deletePermissions(final String resourceUid) {
      String METHOD = Thread.currentThread().getStackTrace()[1].getMethodName();
      String registerId = null;
      String sso_token = null;
      String owner = null;
      OperationIF operOutput = null;
      OperationIF operPolicyInput = null;
      OperationIF operPolicyOutput = null;
      JSONObject jsonPolicyInput = null;
      JaxrsHandlerIF policyHandler = null;

      _logger.entering(CLASS, METHOD);

      if (STR.isEmpty(resourceUid)) {
         this.abort(METHOD, "Resource is empty", Response.Status.BAD_REQUEST);
      }

      operOutput = new Operation(OperationIF.TYPE.DELETE);

      policyHandler = this.getHandler(JaxrsHandlerIF.HANDLER_UMA_POLICY);

      registerId = this.getRegisterGUID(resourceUid);

      if (!STR.isEmpty(registerId)) {
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

         operOutput = operPolicyOutput;
      } else {
         operOutput.setError(true);
         operOutput.setState(STATE.NOTEXIST);
         operOutput.setStatus("Resource is not registered");
      }

      _logger.exiting(CLASS, METHOD);

      return operOutput;
   }
}
