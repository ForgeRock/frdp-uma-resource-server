/*
 * Copyright (c) 2018-2020, ForgeRock, Inc., All rights reserved
 * Use subject to license terms.
 */

package com.forgerock.frdp.resourceserver.rest.share;

import com.forgerock.frdp.common.BasicData;
import com.forgerock.frdp.common.ConstantsIF;
import com.forgerock.frdp.common.DataIF;
import com.forgerock.frdp.dao.Operation;
import com.forgerock.frdp.dao.OperationIF;
import com.forgerock.frdp.resourceserver.ConfigIF;
import com.forgerock.frdp.resourceserver.handler.JaxrsHandlerIF;
import com.forgerock.frdp.resourceserver.rest.RSResource;
import com.forgerock.frdp.utils.JSON;
import com.forgerock.frdp.utils.STR;
import javax.servlet.ServletContext;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.UriInfo;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

/**
 * Resource subject, Requesting Parrt (Rqp), management: GET
 * .../rest/share/resources/{id} DELETE .../rest/share/resources/{id}/policy
 *
 * @author Scott Fehrman, ForgeRock, Inc.
 */
public class ResourcesResource extends RSResource {
   private final String CLASS = this.getClass().getName();

   /**
    * Constructor
    *
    * @param uriInfo    UriInfo uri information
    * @param servletCtx ServletContext context from the servlet
    * @param httpHdrs   HttpHeaders header information
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
    * Get resource for the specified resource id and scopes (query param)
    *
    * <pre>
    * .../{id}?scopes=scope+list
    *
    * JSON output
    * {
    *   "meta": {
    *   },
    *   "content": {
    *   },
    *   "scopes": {
    *     "request": [ .... ],
    *     "policy": [ ... ]
    *   }
    * }
    * </pre>
    * 
    * @param resourceUid String resource identifier
    * @param scopes      String space separated list of scopes
    * @return Response HTTP response object
    */
   @GET
   @Path("/{" + ConstantsIF.ID + "}")
   @Produces(MediaType.APPLICATION_JSON)
   public Response getResources(@PathParam(ConstantsIF.ID) String resourceUid,
         @QueryParam(ConstantsIF.SCOPES) String scopes) {
      String METHOD = Thread.currentThread().getStackTrace()[1].getMethodName();
      Response response = null;
      OperationIF operOutput = null;

      _logger.entering(CLASS, METHOD);

      this.load();

      if (_logger.isLoggable(DEBUG_LEVEL)) {
         _logger.log(DEBUG_LEVEL, "resourceUid=''{0}'', scopes=''{1}''",
               new Object[] { resourceUid == null ? NULL : resourceUid, scopes == null ? NULL : scopes });
      }

      operOutput = this.readImpl(resourceUid, scopes);

      response = this.getResponseFromJSON(_uriInfo, operOutput);

      _logger.exiting(CLASS, METHOD);

      return response;
   }

   /**
    * Route sub-path "{id}/policy" to process resource policy requests
    * 
    * @param resourceUid String resource identifier
    * @return PolicyResource
    */
   @Path("/{" + ConstantsIF.ID + "}/" + ConstantsIF.POLICY)
   public PolicyResource usePolicy(@PathParam(ConstantsIF.ID) String resourceUid) {
      String METHOD = Thread.currentThread().getStackTrace()[1].getMethodName();
      PolicyResource policyResource = null;

      _logger.entering(CLASS, METHOD);

      if (_logger.isLoggable(DEBUG_LEVEL)) {
         _logger.log(DEBUG_LEVEL, "resourceUid=''{0}''", new Object[] { resourceUid == null ? NULL : resourceUid });
      }

      policyResource = new PolicyResource(_uriInfo, _servletCtx, _httpHdrs, resourceUid);

      _logger.exiting(CLASS, METHOD);

      return policyResource;
   }
   /*
    * =============== PRIVATE METHODS ===============
    */

   /**
    * Get the resource related to the resource identifier and scopes Check if the
    * Resource Uid is valid Make sure the resource is registered Get the current
    * "policy" scopes for the resource + subject Scopes can not be "mixed", all
    * scopes must either be in the current "policy" or NOT in the current "policy"
    * Get Requesting Party Token (RPT) from header and validate If valid ... return
    * JSON output Else ... return a Permission Ticket create a permission ticket
    * based on the scopes
    *
    * @param resourceUid String resource identifier
    * @param scopes      String space separated list of scopes
    * @return OperationIF output
    */
   private OperationIF readImpl(final String resourceUid, final String scopes) {
      boolean bMeta = false;
      boolean bContent = false;
      boolean bDiscoverable = false;
      String METHOD = Thread.currentThread().getStackTrace()[1].getMethodName();
      String msg = null;
      String registerId = null;
      String owner = null;
      String subject = null;
      String icon_uri = null;
      String[] arrayScopes = null;
      DataIF dataRPT = null;
      OperationIF operOutput = null;
      OperationIF operResourceOutput = null;
      OperationIF operRegisterOutput = null;
      OperationIF operMetaOutput = null;
      OperationIF operContentOutput = null;
      OperationIF operPermTicketOutput = null;
      JSONObject jsonOutput = null;
      JSONObject jsonData = null;
      JSONObject jsonMeta = null;
      JSONObject jsonContent = null;
      JSONObject jsonResource = null;
      JSONObject jsonRegister = null;
      JSONObject jsonScopes = null;
      JSONObject jsonPermTicketData = null;
      JSONObject jsonPermTicketHdrs = null;
      JSONArray jsonScopesRequest = null;
      JSONArray jsonScopesToken = null;
      JSONArray jsonScopesPolicy = null;
      JSONArray jsonScopesResource = null;

      _logger.entering(CLASS, METHOD);

      if (STR.isEmpty(resourceUid)) {
         this.abort(METHOD, "Resource Uid is empty", Status.BAD_REQUEST);
      }

      /*
       * process the "scopes" argument
       */
      jsonScopesRequest = new JSONArray();

      if (!STR.isEmpty(scopes)) {
         arrayScopes = scopes.split(" ");

         for (String s : arrayScopes) {
            if (!STR.isEmpty(s)) {
               if (s.equalsIgnoreCase(ConstantsIF.META)) {
                  bMeta = true;
               } else if (s.equalsIgnoreCase(ConstantsIF.CONTENT)) {
                  bContent = true;
               }

               jsonScopesRequest.add(s);
            }
         }
      }

      subject = this.getUserIdFromSSOSession();

      operOutput = new Operation(OperationIF.TYPE.READ);

      jsonOutput = new JSONObject();
      jsonData = new JSONObject();

      operResourceOutput = this.getResource(resourceUid);

      if (operResourceOutput.getState() == STATE.SUCCESS) {
         jsonResource = operResourceOutput.getJSON();

         owner = JSON.getString(jsonResource, ConstantsIF.DATA + "." + ConstantsIF.OWNER);

         bDiscoverable = JSON.getBoolean(jsonResource,
               ConstantsIF.DATA + "." + ConstantsIF.META + "." + ConstantsIF.DISCOVERABLE);

         operRegisterOutput = this.getRegistration(resourceUid, owner);

         jsonRegister = operRegisterOutput.getJSON();

         jsonScopesResource = JSON.getArray(jsonRegister, ConstantsIF.RESOURCE_SCOPES);
         icon_uri = JSON.getString(jsonRegister, ConstantsIF.ICON_URI);

         registerId = JSON.getString(jsonResource, ConstantsIF.DATA + "." + ConstantsIF.REGISTER);

         if (!STR.isEmpty(registerId)) {
            jsonScopesPolicy = this.getPolicyScopes(resourceUid, owner, subject);

            dataRPT = this.validateRPT(scopes, operResourceOutput);

            jsonScopesToken = JSON.getArray(dataRPT.getJSON(), ConstantsIF.TOKEN);

            if (jsonScopesToken == null) {
               jsonScopesToken = new JSONArray();
            }

            jsonScopes = new JSONObject();
            jsonScopes.put(ConstantsIF.REQUEST, jsonScopesRequest);
            jsonScopes.put(ConstantsIF.TOKEN, jsonScopesToken);
            jsonScopes.put(ConstantsIF.POLICY, jsonScopesPolicy);

            if (bDiscoverable) {
               jsonScopes.put(ConstantsIF.RESOURCE, jsonScopesResource);
            } else {
               jsonScopes.put(ConstantsIF.RESOURCE, new JSONArray());
            }

            jsonData.put(ConstantsIF.SCOPES, jsonScopes);
            jsonData.put(ConstantsIF.TOKEN, JSON.getString(dataRPT.getJSON(), REQUESTING_PARTY_TOKEN));

            if (!jsonScopesRequest.isEmpty()) {
               if (this.validateScopes(jsonScopesRequest, jsonScopesResource)) {
                  if (this.isRequestMixed(jsonScopesRequest, jsonScopesPolicy)) {
                     msg = "Requested scopes are mixed";

                     operOutput.setError(true);
                     operOutput.setState(STATE.FAILED); // maps to 409: CONFLICT
                  } else {
                     jsonData.put(ConstantsIF.MESSAGE, dataRPT.getStatus());

                     if (!dataRPT.isError()) {
                        if (bMeta) {
                           operMetaOutput = this.getMeta(resourceUid);

                           jsonMeta = JSON.getObject(operMetaOutput.getJSON(), ConstantsIF.DATA);

                           if (jsonMeta != null) {
                              if (jsonMeta.containsKey(ConstantsIF.DISCOVERABLE)) {
                                 jsonMeta.remove(ConstantsIF.DISCOVERABLE);
                              }
                           } else {
                              jsonMeta = new JSONObject();
                           }

                           jsonMeta.put(ConstantsIF.OWNER, owner);
                           jsonMeta.put(ConstantsIF.ICON_URI, icon_uri);

                           jsonData.put(ConstantsIF.META, jsonMeta);
                        }

                        if (bContent) {
                           operContentOutput = this.getContent(resourceUid);

                           jsonContent = JSON.getObject(operContentOutput.getJSON(), ConstantsIF.DATA);

                           if (jsonContent == null || jsonContent.isEmpty()) {
                              jsonContent = new JSONObject();
                           }

                           jsonData.put(ConstantsIF.CONTENT, jsonContent);
                        }

                        msg = "Success";

                        operOutput.setState(STATE.SUCCESS); // maps to 200: OK
                     } else {
                        /*
                         * The Requesting Party Token (RPT) is NOT valid Get a "Permission Ticket",
                         * return to the client The client will use the "Permission Ticket" to get an
                         * RPT
                         */

                        operPermTicketOutput = this.getPermssionTicket(scopes, operResourceOutput);

                        jsonPermTicketHdrs = JSON.getObject(operPermTicketOutput.getJSON(), ConstantsIF.HEADERS);
                        if (jsonPermTicketHdrs != null) {
                           jsonOutput.put(ConstantsIF.HEADERS, jsonPermTicketHdrs);
                        }

                        jsonPermTicketData = JSON.getObject(operPermTicketOutput.getJSON(), ConstantsIF.DATA);

                        if (jsonPermTicketData != null) {
                           jsonData.put(ConstantsIF.TICKET, JSON.getString(jsonPermTicketData, ConstantsIF.TICKET));
                           jsonData.put(ConstantsIF.AS_URI, JSON.getString(jsonPermTicketData, ConstantsIF.AS_URI));
                        }

                        msg = "RPT is missing or invalid";

                        operOutput.setState(operPermTicketOutput.getState());
                     }
                  }
               } else {
                  msg = "Requested scope(s) not valid";

                  operOutput.setError(true);
                  operOutput.setState(STATE.ERROR); // maps to 400: BAD_REQUEST
               }
            } else {
               msg = "Missing scopes";

               operOutput.setError(true);
               operOutput.setState(STATE.ERROR); // maps to 400: BAD_REQUEST
            }
         } else {
            msg = "Resource is not registered";

            operOutput.setError(true);
            operOutput.setState(STATE.ERROR); // maps to 400: BAD_REQUEST
         }
      } else {
         msg = operResourceOutput.getStatus();

         operOutput.setError(true);
         operOutput.setState(STATE.NOTEXIST);
      }

      jsonData.put(ConstantsIF.MESSAGE, msg);
      // jsonData.put(ConstantsIF.STATE, operOutput.getState().toString());
      jsonOutput.put(ConstantsIF.DATA, jsonData);

      operOutput.setJSON(jsonOutput);
      operOutput.setStatus(msg);

      _logger.exiting(CLASS, METHOD);

      return operOutput;
   }

   /**
    * Validate the scopes
    * 
    * @param request  JSONArray request
    * @param resource JSONArray resource
    * @return boolean true if request scopes are in the resource scopes
    */
   private boolean validateScopes(final JSONArray request, final JSONArray resource) {
      boolean valid = true;
      boolean found = false;
      String METHOD = Thread.currentThread().getStackTrace()[1].getMethodName();

      /*
       * All of the "request" scopes MUST be in "resource" scopes
       */
      _logger.entering(CLASS, METHOD);

      if (request != null && resource != null) {
         for (Object req : request) {
            if (req != null && req instanceof String && !STR.isEmpty((String) req)) {
               found = false;
               for (Object res : resource) {
                  if (res != null && res instanceof String && !STR.isEmpty((String) res)) {
                     if (((String) req).equalsIgnoreCase((String) res)) {
                        found = true;
                        break;
                     }
                  }
               }
            }

            if (!found) {
               valid = false;
               break;
            }
         }
      } else {
         valid = false;
      }

      _logger.exiting(CLASS, METHOD);

      return valid;
   }

   /**
    * Validate the Requsting Party Token (RPT). Get the Requesting Party Token
    * (RPT) from the header. Get the "owner" from the Resource, needed to get the
    * PAT. Get the "register" (GUID) from the Resource (for comparison). Note: the
    * RPT is just an "access token". Call the Authorization Server (AS) to validate
    * the token. Check validate scope(s) and the Request Uid (mathes related
    * Resource uid).
    *
    * <pre>
    * JSON Data ...
    * {
    *   "policy": [ "...", ... ] // array of scopes from the policy
    * }
    *
    * Introspect the RPT (access_token)
    * curl --header 'authorization: Bearer <<PAT>>' \
    * 'https://.../openam/oauth2/introspect?token=<<RPT>>'
    *
    * A valid RPT ...
    * {
    *   "active": true,
    *   "permissions": [
    *     {
    *       "resource_id": "ef4d750e-3831-483b-b395-c6f059b5e15d0",
    *       "resource_scopes": ["download"],
    *       "exp": 1522334692
    *     }
    *   ],
    *   "token_type": "access_token",
    *   "exp": 1522334692,
    *   "iss": "https://openam.example.com:8443/openam/oauth2"
    * }
    *
    * An in-valid RPT ...
    * { "active": false }
    *
    * The output JSON is wrapped in a "data" object ...
    * {
    *   "data": {
    *     "active": true | false,
    *     ...
    *   }
    * }
    * </pre>
    * 
    * @param scopes       String list of scopes
    * @param operResource OperationIF input
    * @return DataIF output
    */
   private DataIF validateRPT(final String scopes, final OperationIF operResource) {
      boolean[] inputVerifiedArray = null;
      String METHOD = Thread.currentThread().getStackTrace()[1].getMethodName();
      String rpt = null; // UMA Requesting Party Token (OAuth2 access token)
      String owner = null;
      String pat = null; // UMA Protection API Token (OAuth2 access token)
      String resourceScope = null;
      String inputScope = null;
      String[] inputScopesArray = null;
      DataIF data = null;
      OperationIF operOauthInput = null;
      OperationIF operOauthOutput = null;
      JSONObject jsonData = null;
      JSONObject jsonResource = null;
      JSONObject jsonHeaders = null;
      JSONObject jsonQueryParams = null;
      JSONObject jsonInput = null;
      JSONObject jsonOutput = null;
      JSONObject permission = null;
      JSONArray jsonPermissions = null;
      JSONArray jsonResourceScopes = null;
      JaxrsHandlerIF oauth2Handler = null;

      _logger.entering(CLASS, METHOD);

      data = new BasicData();

      oauth2Handler = this.getHandler(JaxrsHandlerIF.HANDLER_AMOAUTH2);

      if (oauth2Handler.getState() != STATE.READY) {
         this.abort(METHOD,
               "OAuth2 Handler not ready: " + oauth2Handler.getState().toString() + ", " + oauth2Handler.getStatus(),
               Response.Status.INTERNAL_SERVER_ERROR);
      }

      rpt = this.getAttributeFromHeader(ConfigIF.RS_HEADERS_RPT, false);

      if (_logger.isLoggable(DEBUG_LEVEL)) {
         _logger.log(DEBUG_LEVEL, "rpt=''{0}''", new Object[] { rpt == null ? NULL : rpt });
      }

      if (!STR.isEmpty(rpt)) {
         // Resource JSON
         // {
         // "uid": "...",
         // "data": { "owner": "...", "register": "..." },
         // "timestamps": { ... }
         // }

         jsonResource = JSON.getObject(operResource.getJSON(), ConstantsIF.DATA);

         owner = JSON.getString(jsonResource, ConstantsIF.OWNER);

         pat = this.getAccessToken(owner);

         jsonHeaders = new JSONObject();
         jsonHeaders.put(ConstantsIF.AUTHORIZATION, "Bearer " + pat);

         jsonQueryParams = new JSONObject();
         jsonQueryParams.put(ConstantsIF.TOKEN, rpt);

         jsonInput = new JSONObject();
         jsonInput.put(ConstantsIF.PATH, ConstantsIF.INTROSPECT);
         jsonInput.put(ConstantsIF.HEADERS, jsonHeaders);
         jsonInput.put(ConstantsIF.QUERY_PARAMS, jsonQueryParams);

         operOauthInput = new Operation(OperationIF.TYPE.READ);
         operOauthInput.setJSON(jsonInput);

         operOauthOutput = oauth2Handler.process(operOauthInput); // validate the RPT

         jsonOutput = JSON.getObject(operOauthOutput.getJSON(), ConstantsIF.DATA);

         if (JSON.getBoolean(jsonOutput, ConstantsIF.ACTIVE)) {
            /*
             * Need to check: The response "resource_id" equals the resource.register The
             * response "resource_scopes" contain provides scopes Loop through the
             * "permissions" array ...
             */
            jsonPermissions = JSON.getArray(jsonOutput, ConstantsIF.PERMISSIONS);
            inputScopesArray = scopes.split(" ");
            inputVerifiedArray = new boolean[inputScopesArray.length];

            if (jsonPermissions != null && !jsonPermissions.isEmpty()) {
               if (_logger.isLoggable(DEBUG_LEVEL)) {
                  _logger.log(DEBUG_LEVEL, "scopes=''{0}'', permissions=''{1}''",
                        new Object[] { scopes == null ? NULL : scopes, jsonPermissions.toString() });
               }

               for (boolean b : inputVerifiedArray) // initialize to false
               {
                  b = false;
               }

               for (int i = 0; i < jsonPermissions.size(); i++) // each permission
               {
                  permission = JSON.getObject(jsonOutput, ConstantsIF.PERMISSIONS + "[" + i + "]");

                  if (permission != null && !permission.isEmpty()) {
                     if (JSON.getString(jsonResource, ConstantsIF.REGISTER)
                           .equals(JSON.getString(permission, ConstantsIF.RESOURCE_ID))) {
                        jsonResourceScopes = JSON.getArray(permission, ConstantsIF.RESOURCE_SCOPES);

                        if (jsonResourceScopes != null && !jsonResourceScopes.isEmpty()) {
                           for (int j = 0; j < jsonResourceScopes.size(); j++) // each resource scope
                           {
                              resourceScope = JSON.getString(permission, ConstantsIF.RESOURCE_SCOPES + "[" + j + "]");

                              if (!STR.isEmpty(resourceScope)) {
                                 for (int k = 0; k < inputScopesArray.length; k++) // each input scope
                                 {
                                    inputScope = inputScopesArray[k];

                                    if (!STR.isEmpty(inputScope)) {
                                       if (resourceScope.equalsIgnoreCase(inputScope)) {
                                          inputVerifiedArray[k] = true;
                                          break;
                                       }
                                    }
                                 }
                              }
                           }
                        }
                        break; // done processing the array of permissions
                     }
                  }
               }
            }
            /*
             * check the verified array, all MUST be True ... to return True
             */

            for (boolean b : inputVerifiedArray) {
               if (b == false) {
                  data.setStatus("Requested scope(s) not found in Token scopes");
                  data.setState(STATE.ERROR);
                  data.setError(true);
               }
            }
         } else {
            data.setStatus("Requesting Party Token is NOT valid");
            data.setState(STATE.ERROR);
            data.setError(true);
         }
      } else {
         rpt = null;
         data.setStatus("Requesting Party Token is empty");
         data.setState(STATE.NOTEXIST);
         data.setError(true);
      }

      jsonData = new JSONObject();

      if (!data.isError()) {
         data.setStatus("Requesting Party Token is valid");
         data.setState(STATE.SUCCESS);
      }

      if (jsonResourceScopes != null && !jsonResourceScopes.isEmpty()) {
         jsonData.put(ConstantsIF.TOKEN, jsonResourceScopes);
      }

      jsonData.put(REQUESTING_PARTY_TOKEN, rpt);

      data.setJSON(jsonData);

      _logger.exiting(CLASS, METHOD);

      return data;
   }

   /**
    * Get the Permission Ticket
    *
    * <pre>
    * Generate a "Permission Ticket" for the specified resource and scopes
    * JSON input ...
    * {
    *   "data": {
    *     "owner": "...",
    *     ...,
    *     "register": "..."
    *   }
    * }
    * JSON output ...
    * {
    *   "data": {
    *     "ticket": "...",
    *     "as_uri": "..."
    *   },
    *   "headers": {
    *     ...
    *   }
    * }
    * curl example:
    * curl -X POST \
    * --header 'authorization: Bearer <<PAT>>' \
    * --data '[
    * {
    *   "resource_id" : "ef4d750e-3831-483b-b395-c6f059b5e15d0",
    *   "resource_scopes" : ["download"]
    * }
    * ]' \
    * https://.../openam/uma//realms/root/permission_request
    *
    * A valid tocket:
    * { "ticket": "..." }
    * </pre>
    * 
    * @param scopes       String list of scopes
    * @param operResource OperationIF input
    * @return OperationIF output
    */
   private OperationIF getPermssionTicket(final String scopes, final OperationIF operResource) {
      String METHOD = Thread.currentThread().getStackTrace()[1].getMethodName();
      String owner = null;
      String registerId = null;
      String pat = null; // UMA Protection API Token (is a OAuth2 access token)
      String issuer = null;
      OperationIF operInput = null;
      OperationIF operOutput = null;
      JSONObject jsonWellKnown = null;
      JSONObject jsonResource = null;
      JSONObject jsonHeaders = null;
      JSONObject jsonData = null;
      JSONObject jsonInput = null;
      JSONObject jsonOutput = null;
      JSONArray jsonScopes = null;
      JaxrsHandlerIF umaPermReqHandler = null;

      _logger.entering(CLASS, METHOD);

      umaPermReqHandler = this.getHandler(JaxrsHandlerIF.HANDLER_UMA_PERMREQ);

      if (umaPermReqHandler.getState() != STATE.READY) {
         this.abort(METHOD, "UMA Handler not ready: " + umaPermReqHandler.getState().toString() + ", "
               + umaPermReqHandler.getStatus(), Response.Status.INTERNAL_SERVER_ERROR);
      }

      jsonResource = JSON.getObject(operResource.getJSON(), ConstantsIF.DATA);

      owner = JSON.getString(jsonResource, ConstantsIF.OWNER);
      registerId = JSON.getString(jsonResource, ConstantsIF.REGISTER);

      pat = this.getAccessToken(owner);

      jsonScopes = new JSONArray();
      for (String s : scopes.split(" ")) {
         if (!STR.isEmpty(s)) {
            jsonScopes.add(s);
         }
      }

      jsonHeaders = new JSONObject();
      jsonHeaders.put(ConstantsIF.AUTHORIZATION, "Bearer " + pat);

      jsonData = new JSONObject();
      jsonData.put(ConstantsIF.RESOURCE_ID, registerId);
      jsonData.put(ConstantsIF.RESOURCE_SCOPES, jsonScopes);

      jsonInput = new JSONObject();
      jsonInput.put(ConstantsIF.PATH, ConstantsIF.PERMISSION_REQUEST);
      jsonInput.put(ConstantsIF.HEADERS, jsonHeaders);
      jsonInput.put(ConstantsIF.DATA, jsonData);

      if (_logger.isLoggable(DEBUG_LEVEL)) {
         _logger.log(DEBUG_LEVEL, "create input: ''{0}''", new Object[] { jsonInput.toString() });
      }

      operInput = new Operation(OperationIF.TYPE.CREATE);
      operInput.setJSON(jsonInput);

      /*
       * JSON input: {} JSON output: { "data": { ... }, "headers": { ... } }
       */
      operOutput = umaPermReqHandler.process(operInput);

      if (operOutput.getState() == STATE.NOTAUTHORIZED) {
         jsonOutput = operOutput.getJSON();

         if (_logger.isLoggable(DEBUG_LEVEL)) {
            _logger.log(DEBUG_LEVEL, "create output: ''{0}''",
                  new Object[] { jsonOutput == null ? NULL : jsonOutput.toString() });
         }

         jsonData = JSON.getObject(jsonOutput, ConstantsIF.DATA);

         if (jsonData != null) {
            jsonWellKnown = this.getWellKnown();

            issuer = JSON.getString(jsonWellKnown, ConstantsIF.ISSUER);
            if (!STR.isEmpty(issuer)) {
               jsonData.put(ConstantsIF.AS_URI, issuer);
            }
         }
      } else if (operOutput.getState() == STATE.WARNING) {
         operOutput.setError(true); // implicitly sets STATE = ERROR
         operOutput.setState(STATE.WARNING);
      }

      _logger.exiting(CLASS, METHOD);

      return operOutput;
   }

   /**
    * Get the Policies scopes
    * 
    * <pre>
    * Policy JSON output ... 
    * {
    *   "permissions": [
    *     {
    *       "subject": "bob",
    *       "scopes": ["view", "comment"]
    *     },
    *     { ... }
    *   ]
    * }
    * </pre>
    * 
    * @param resourceUid String resource identifier
    * @param owner       String resource owner
    * @param rqp         String requesting party
    * @return JSONArray output
    */
   private JSONArray getPolicyScopes(final String resourceUid, final String owner, final String rqp) {
      String METHOD = Thread.currentThread().getStackTrace()[1].getMethodName();
      String sso_token = null; // Proxy Admin Token
      String subject = null;
      JSONArray arrayScopes = null;
      JSONArray arrayPerms = null;
      OperationIF operPolicyOutput = null;

      _logger.entering(CLASS, METHOD);

      if (!STR.isEmpty(resourceUid) && !STR.isEmpty(owner) && !STR.isEmpty(rqp)) {
         sso_token = this.getSSOTokenForAdmin();

         operPolicyOutput = this.getPolicy(resourceUid, sso_token, owner);

         arrayPerms = JSON.getArray(operPolicyOutput.getJSON(), ConstantsIF.PERMISSIONS);

         if (arrayPerms != null && !arrayPerms.isEmpty()) {
            for (Object o : arrayPerms) {
               if (o != null && o instanceof JSONObject) {
                  subject = JSON.getString((JSONObject) o, ConstantsIF.SUBJECT);
                  if (!STR.isEmpty(subject) && subject.equalsIgnoreCase(rqp)) {
                     arrayScopes = JSON.getArray((JSONObject) o, ConstantsIF.SCOPES);
                  }
               }
            }
         }
      }

      if (arrayScopes == null) {
         arrayScopes = new JSONArray();
      }

      _logger.exiting(CLASS, METHOD);

      return arrayScopes;
   }

   /**
    * Check for a "mixed" request. All Request scopes MUST be either part of the
    * Policy scopes or not. Can not have some Request scopes in Policy and some not
    * in the Policy
    * 
    * @param arrayRequest JSONArray requests
    * @param arrayPolicy  JSONArray policies
    * @return boolean True if "mixed" scopes
    */
   private boolean isRequestMixed(final JSONArray arrayRequest, final JSONArray arrayPolicy) {
      boolean mixed = false;
      boolean found = false;
      int policyIn = 0;
      int policyOut = 0;
      String METHOD = Thread.currentThread().getStackTrace()[1].getMethodName();

      /*
       */
      _logger.entering(CLASS, METHOD);

      if (arrayRequest != null && arrayPolicy != null) {
         for (Object r : arrayRequest) {
            if (r != null && r instanceof String && !STR.isEmpty((String) r)) {
               found = false;
               for (Object p : arrayPolicy) {
                  if (p != null && p instanceof String && !STR.isEmpty((String) p)) {
                     if (((String) r).equalsIgnoreCase((String) p)) {
                        found = true;
                        break;
                     }
                  }
               }

               if (found) {
                  policyIn++;
               } else {
                  policyOut++;
               }
            }
         }
      }

      if (policyIn > 0 && policyOut > 0) {
         mixed = true;
      }

      _logger.exiting(CLASS, METHOD);

      return mixed;
   }
}
