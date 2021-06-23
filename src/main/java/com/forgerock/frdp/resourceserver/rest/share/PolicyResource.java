/*
 * Copyright (c) 2019-2021, ForgeRock, Inc., All rights reserved
 * Use subject to license terms.
 */

package com.forgerock.frdp.resourceserver.rest.share;

import com.forgerock.frdp.common.ConstantsIF;
import com.forgerock.frdp.dao.Operation;
import com.forgerock.frdp.dao.OperationIF;
import com.forgerock.frdp.resourceserver.handler.JaxrsHandlerIF;
import com.forgerock.frdp.resourceserver.rest.RSResource;
import com.forgerock.frdp.utils.JSON;
import com.forgerock.frdp.utils.STR;
import javax.servlet.ServletContext;
import javax.ws.rs.DELETE;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.UriInfo;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

/**
 * Policy operations for the subject, Requesting Party (RqP). This service
 * provides a means for the RqP to manage (remove) their own access. This
 * service IS NOT part of the UMA 2.0 specification. This service is provided as
 * value add and used the Access Manager APIs
 *
 * @author Scott Fehrman, ForgeRock, Inc.
 */
public class PolicyResource extends RSResource {
   private final String CLASS = this.getClass().getName();
   private String _resourceId = null;

   /**
    * 
    * @param uriInfo    UriInfo uri information
    * @param servletCtx ServletContext context from the servlet
    * @param httpHdrs   HttpHeaders header information
    * @param resourceId String resource identifier
    */
   public PolicyResource(final UriInfo uriInfo, final ServletContext servletCtx, 
      final HttpHeaders httpHdrs, final String resourceId) {
      super();

      String METHOD = "PolicyResource()";

      _logger.entering(CLASS, METHOD);

      _uriInfo = uriInfo;
      _servletCtx = servletCtx;
      _httpHdrs = httpHdrs;
      _resourceId = resourceId;

      _logger.exiting(CLASS, METHOD);

      return;
   }

   /**
    * Delete the subject (Rquesting Party), authenticated user, from the resource's
    * policy / permissions
    * 
    * @return Response HTTP response object
    */
   @DELETE
   public Response delete() {
      String METHOD = Thread.currentThread().getStackTrace()[1].getMethodName();
      String subject = null;
      Response response = null;
      OperationIF operOutput = null;

      _logger.entering(CLASS, METHOD);

      if (STR.isEmpty(_resourceId)) {
         this.abort(METHOD, "Path resource is empty", Status.BAD_REQUEST);
      }

      this.load();

      subject = this.getUserIdFromSSOSession(); // require a SSO session

      operOutput = this.updatePolicy(subject); // no scopes, remove permission

      response = this.getResponseFromJSON(_uriInfo, operOutput);

      _logger.exiting(CLASS, METHOD);

      return response;

   }
   /*
    * =============== PRIVATE METHODS ===============
    */

   /**
    * Update the policy, remove the subject
    * 
    * <pre>
    * The current implementation of this method DOES NOT support
    * the modifications of scopes within a permissions for a subject.
    * It only support the removal of the complete permission
    * within a policy.
    *
    * JSON data input ...
    * {
    *   "action": "delete",
    *   "attributes": [
    *     {
    *       "name": "scopes",
    *       "values": [ "markup" ]
    *     }
    *   ]
    * }
    *
    * This is NOT necessarily about deleting the entire policy
    * related to the registered resource. We need to either ...
    *  - remove the "permission" associated with the "subject".
    *    if there are zero permissions, just delete the policy
    *  - change the "scopes", in a "permission", for the "subject"
    *    if there are no more "scopes", remove the "permission"
    * 
    * Need the AM "registerId" for the Resource
    * Get the Resource object, 
    * - obtain the resource owner
    * - obtain the register id (UMA resource_set id)
    * 
    * Read the existing policy ... need to use "admin" sso session
    * </pre>
    *
    * @param subject   String the authenticated user, the Requesting Party
    * @param jsonInput JSONObject input
    * @return OperationIF output
    */
   private OperationIF updatePolicy(final String subject) {
      boolean changed = false;
      String METHOD = Thread.currentThread().getStackTrace()[1].getMethodName();
      String registerId = null;
      String owner = null;
      String sso_token = null;
      String permSub = null;
      JSONObject jsonResource = null;
      JSONObject jsonPerm = null;
      JSONObject jsonReplace = null;
      JSONObject jsonData = null;
      JSONArray jsonCurPerms = null;
      JSONArray jsonNewPerms = null;
      OperationIF operOutput = null;
      OperationIF operResource = null;
      OperationIF operPolicyInput = null;
      OperationIF operPolicyOutput = null;
      JaxrsHandlerIF policyHandler = null;

      _logger.entering(CLASS, METHOD);

      policyHandler = this.getHandler(JaxrsHandlerIF.HANDLER_UMA_POLICY);

      operResource = this.getResource(_resourceId);

      if (operResource == null) {
         this.abort(METHOD, 
            "Resource operation is null", 
            Status.INTERNAL_SERVER_ERROR);
      }

      // JSON output ...
      // {
      // "uid": "...",
      // "data": { "owner": "...", "register": "..." },
      // "timestamps": { ... }
      // }

      jsonResource = operResource.getJSON();

      if (jsonResource == null || jsonResource.isEmpty()) {
         this.abort(METHOD, 
            "Resource JSON object is null or empty", 
            Status.INTERNAL_SERVER_ERROR);
      }

      owner = JSON.getString(jsonResource, 
         ConstantsIF.DATA + "." + ConstantsIF.OWNER);

      if (STR.isEmpty(owner)) {
         this.abort(METHOD, 
            "Owner is empty", 
            Status.INTERNAL_SERVER_ERROR);
      }

      registerId = JSON.getString(jsonResource, 
         ConstantsIF.DATA + "." + ConstantsIF.REGISTER);

      if (STR.isEmpty(registerId)) {
         this.abort(METHOD, 
            "Register Id is empty", 
            Status.INTERNAL_SERVER_ERROR);
      }

      sso_token = this.getSSOTokenForAdmin();

      if (STR.isEmpty(sso_token)) {
         this.abort(METHOD, 
            "Admin SSO Token is empty", 
            Status.INTERNAL_SERVER_ERROR);
      }

      // GET the current policy state
      // JSON output ...
      // {
      // "data": { "permissions": [ { ... } ] }
      // }

      operPolicyOutput = this.getPolicy(_resourceId, sso_token, owner);

      if (operPolicyOutput == null) {
         this.abort(METHOD, 
            "Existing Policy output is null", 
            Status.INTERNAL_SERVER_ERROR);
      }

      if (operPolicyOutput.getState() == STATE.NOTEXIST) {
         /*
          * A policy does not exist for the given resource + subject 
          * Nothing to delete, return a "success"
          */

         operOutput = new Operation(OperationIF.TYPE.DELETE);
         operOutput.setState(STATE.SUCCESS);
         operOutput.setStatus(operPolicyOutput.getStatus());
      } else {
         jsonCurPerms = JSON.getArray(operPolicyOutput.getJSON(), 
            ConstantsIF.DATA + "." + ConstantsIF.PERMISSIONS);

         if (jsonCurPerms == null || jsonCurPerms.isEmpty()) {
            this.abort(METHOD, 
               "Existing Policy permissions are null or empty", 
               Status.INTERNAL_SERVER_ERROR);
         }

         // "permissions" : [ { "subject": "...", "scopes": ["view"] } ]

         jsonNewPerms = new JSONArray();

         for (Object obj : jsonCurPerms) {
            if (obj != null && obj instanceof JSONObject) {
               jsonPerm = (JSONObject) obj;
               permSub = JSON.getString(jsonPerm, ConstantsIF.SUBJECT);
               if (!STR.isEmpty(permSub) && permSub.equalsIgnoreCase(subject)) {
                  changed = true; // remove permission, do not add to "new" array
               } else {
                  jsonNewPerms.add(jsonPerm); // no change add to "new" array
               }
            }
         }

         if (changed) {
            jsonReplace = new JSONObject();
            jsonReplace.put(ConstantsIF.UID, registerId);
            jsonReplace.put(ConstantsIF.SSO_TOKEN, sso_token);
            jsonReplace.put(ConstantsIF.OWNER, owner);
            /*
             * If the "new permission" array is empty ... 
             * removed the only permission then
             * delete the entire police else 
             * replace the policy's permissions (with the new one)
             */

            if (jsonNewPerms.isEmpty()) {
               // delete the policy ... JSON input ...
               // {
               // "uid" : "...", "sso_token": "...", "owner": "..."
               // }

               operPolicyInput = new Operation(OperationIF.TYPE.DELETE);
               operPolicyInput.setJSON(jsonReplace);

               operPolicyOutput = policyHandler.process(operPolicyInput);
            } else {
               // replace the permissions ... JSON input ...
               // {
               // "uid" : "...", // register id
               // "sso_token": "...",
               // "owner": "...",
               // "data": { "permissions" : [ { "subject": "", "scopes": ["view"] } ] }
               // }

               jsonData = new JSONObject();
               jsonData.put(ConstantsIF.PERMISSIONS, jsonNewPerms);

               jsonReplace.put(ConstantsIF.DATA, jsonData);

               operPolicyInput = new Operation(OperationIF.TYPE.REPLACE);
               operPolicyInput.setJSON(jsonReplace);

               operPolicyOutput = policyHandler.process(operPolicyInput);
            }

         }
         operOutput = operPolicyOutput;
      }

      _logger.exiting(CLASS, METHOD);

      return operOutput;
   }
}