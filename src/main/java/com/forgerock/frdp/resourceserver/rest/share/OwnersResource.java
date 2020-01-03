/*
 * Copyright (c) 2018-2020, ForgeRock, Inc., All rights reserved
 * Use subject to license terms.
 */

package com.forgerock.frdp.resourceserver.rest.share;

import com.forgerock.frdp.common.ConstantsIF;
import com.forgerock.frdp.resourceserver.rest.RSResource;
import javax.servlet.ServletContext;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.UriInfo;

/**
 * Owner services
 *
 * @author Scott Fehrman, ForgeRock, Inc.
 */
public class OwnersResource extends RSResource {

   private final String CLASS = this.getClass().getName();

   /**
    * Constructor
    *
    * @param uriInfo    UriInfo uri information
    * @param servletCtx ServletContext context from the servlet
    * @param httpHdrs   HttpHeaders header information
    */
   public OwnersResource(final UriInfo uriInfo, final ServletContext servletCtx, final HttpHeaders httpHdrs) {
      super();

      String METHOD = "OwnersResource()";

      _logger.entering(CLASS, METHOD);

      _uriInfo = uriInfo;
      _servletCtx = servletCtx;
      _httpHdrs = httpHdrs;

      _logger.exiting(CLASS, METHOD);

      return;
   }

   /**
    * Route to sub-path "{id}/discover"
    * 
    * @param owner String resource owner
    * @return DiscoverResource
    */
   @Path("{" + ConstantsIF.ID + "}/" + ConstantsIF.DISCOVER)
   public DiscoverResource useDiscover(@PathParam(ConstantsIF.ID) String owner) {
      String METHOD = Thread.currentThread().getStackTrace()[1].getMethodName();
      DiscoverResource discoverResource = null;

      _logger.entering(CLASS, METHOD);

      discoverResource = new DiscoverResource(owner, _uriInfo, _servletCtx, _httpHdrs);

      _logger.exiting(CLASS, METHOD);

      return discoverResource;
   }
}
