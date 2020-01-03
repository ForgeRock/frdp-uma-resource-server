/*
 * Copyright (c) 2018-2020, ForgeRock, Inc., All rights reserved
 * Use subject to license terms.
 */

package com.forgerock.frdp.resourceserver.rest.share;

import com.forgerock.frdp.common.ConstantsIF;
import com.forgerock.frdp.resourceserver.rest.RSResource;
import javax.ws.rs.Path;

/**
 * Routing endpoints to specific implementation classes,
 * 
 * <pre>
 * paths: 
 * .../rest/share/resources
 * .../rest/share/owners
 * .../rest/share/withme
 * </pre>
 * 
 * @author Scott Fehrman, ForgeRock, Inc.
 */
@Path(ConstantsIF.SHARE)
public class ShareResource extends RSResource {
   private final String CLASS = this.getClass().getName();

   /**
    * Constructor
    */
   public ShareResource() {
      super();

      String METHOD = "ShareResource()";

      _logger.entering(CLASS, METHOD);
      _logger.exiting(CLASS, METHOD);

      return;
   }

   /**
    * Route to "resources" sub endpoint
    *
    * @return ResourcesResource
    */
   @Path(PATH_RESOURCES)
   public ResourcesResource useResources() {
      String METHOD = Thread.currentThread().getStackTrace()[1].getMethodName();
      ResourcesResource resourcesResource = null;

      _logger.entering(CLASS, METHOD);

      resourcesResource = new ResourcesResource(_uriInfo, _servletCtx, _httpHdrs);

      _logger.exiting(CLASS, METHOD);

      return resourcesResource;
   }

   /**
    * Route to "owners" sub endpoint
    *
    * @return OwnersResource
    */
   @Path(PATH_OWNERS)
   public OwnersResource useOwners() {
      String METHOD = Thread.currentThread().getStackTrace()[1].getMethodName();
      OwnersResource ownersResource = null;

      _logger.entering(CLASS, METHOD);

      ownersResource = new OwnersResource(_uriInfo, _servletCtx, _httpHdrs);

      _logger.exiting(CLASS, METHOD);

      return ownersResource;
   }

   /**
    * Route to "withme" sub endpoint
    * 
    * @return WithMeResource
    */
   @Path(PATH_WITHME)
   public WithMeResource useSharedWithMe() {
      String METHOD = Thread.currentThread().getStackTrace()[1].getMethodName();
      WithMeResource sharedResource = null;

      _logger.entering(CLASS, METHOD);

      sharedResource = new WithMeResource(_uriInfo, _servletCtx, _httpHdrs);

      _logger.exiting(CLASS, METHOD);

      return sharedResource;
   }
}
