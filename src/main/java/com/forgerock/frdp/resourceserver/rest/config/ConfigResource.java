/*
 * Copyright (c) 2019-2020, ForgeRock, Inc., All rights reserved
 * Use subject to license terms.
 */

package com.forgerock.frdp.resourceserver.rest.config;

import com.forgerock.frdp.common.ConstantsIF;
import com.forgerock.frdp.dao.Operation;
import com.forgerock.frdp.dao.OperationIF;
import com.forgerock.frdp.resourceserver.rest.RSResource;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

/**
 * Configuration endpoint, PATH: .../rest/config
 *
 * @author Scott Fehrman, ForgeRock, Inc.
 */
@Path(ConstantsIF.CONFIG)
public class ConfigResource extends RSResource {

   private final String CLASS = this.getClass().getName();

   /**
    * Constructor
    */
   public ConfigResource() {
      super();

      String METHOD = "ConfigResource()";

      _logger.entering(CLASS, METHOD);

      _logger.exiting(CLASS, METHOD);

      return;
   }

   /**
    * Get configuration data. HTTP Method is GET, endpoint is ".../config",
    * produces JSON
    * 
    * @return Response HTTP response object
    */
   @GET
   @Produces(MediaType.APPLICATION_JSON)
   public Response get() {
      byte[] bytes = null;
      Object obj = null;
      String METHOD = Thread.currentThread().getStackTrace()[1].getMethodName();
      String realPath = null;
      String publicFile = null;
      java.nio.file.Path pathPublicFile = null;
      JSONObject jsonOutput = null;
      JSONObject jsonData = null;
      Response response = null;
      OperationIF operOutput = null;
      JSONParser parser = null;

      _logger.entering(CLASS, METHOD);

      obj = _servletCtx.getAttribute(CTX_ATTR_PUBLIC);

      if (obj != null && obj instanceof JSONObject) {
         jsonData = (JSONObject) obj;
      } else {
         parser = this.getParserFromCtx(_servletCtx);
         realPath = _servletCtx.getRealPath("/");
         publicFile = realPath + "WEB-INF" + File.separator + PUBLIC_FILE;
         pathPublicFile = Paths.get(publicFile);

         try {
            bytes = Files.readAllBytes(pathPublicFile);
            obj = parser.parse(new String(bytes));
         } catch (IOException | ParseException ex) {
            this.abort(METHOD, "Exception: " + ex.getMessage(), Response.Status.INTERNAL_SERVER_ERROR);
         }

         if (obj != null && obj instanceof JSONObject) {
            jsonData = (JSONObject) obj;
            _servletCtx.setAttribute(CTX_ATTR_PUBLIC, jsonData);
         } else {
            this.abort(METHOD, "Public object is null or not a JSON object", Response.Status.INTERNAL_SERVER_ERROR);
         }
      }

      jsonOutput = new JSONObject();
      jsonOutput.put(ConstantsIF.DATA, jsonData);

      operOutput = new Operation(OperationIF.TYPE.READ);
      operOutput.setJSON(jsonOutput);

      response = this.getResponseFromJSON(_uriInfo, operOutput);

      _logger.exiting(CLASS, METHOD);

      return response;
   }
}
