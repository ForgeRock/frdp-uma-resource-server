/*
 * Copyright (c) 2018-2020, ForgeRock, Inc., All rights reserved
 * Use subject to license terms.
 */

package com.forgerock.frdp.resourceserver.rest.manage;

import com.forgerock.frdp.common.ConstantsIF;
import com.forgerock.frdp.resourceserver.rest.RSResource;
import javax.ws.rs.Path;

/**
 * Routing endpoints to specific implementation classes, 
 * 
 * <pre>
 * paths:
 * .../rest/manage/resources
 * .../rest/manage/requests
 * .../rest/manage/subjects
 * </pre>
 * 
 * @author Scott Fehrman, ForgeRock, Inc.
 */
@Path(ConstantsIF.MANAGE)
public class ManageResource extends RSResource {

   private final String CLASS = this.getClass().getName();

   /**
    * Constructor
    */
   public ManageResource() {
      super();

      String METHOD = "ManageResource()";

      _logger.entering(CLASS, METHOD);
      _logger.exiting(CLASS, METHOD);

      return;
   }

   /**
    * Route to "resources" sub endpoint
    * 
    * @return ResourcesResource
    */
   @Path(ConstantsIF.RESOURCES)
   public ResourcesResource useResources() {
      String METHOD = Thread.currentThread().getStackTrace()[1].getMethodName();
      ResourcesResource resourcesResource = null;

      _logger.entering(CLASS, METHOD);

      resourcesResource = new ResourcesResource(_uriInfo, _servletCtx, _httpHdrs);

      _logger.exiting(CLASS, METHOD);

      return resourcesResource;
   }

   /**
    * Route to "requests" sub endpoint
    * 
    * @return RequestsResource
    */
   @Path(ConstantsIF.REQUESTS)
   public RequestsResource useRequests() {
      String METHOD = Thread.currentThread().getStackTrace()[1].getMethodName();
      RequestsResource requestsResource = null;

      _logger.entering(CLASS, METHOD);

      requestsResource = new RequestsResource(_uriInfo, _servletCtx, _httpHdrs);

      _logger.exiting(CLASS, METHOD);

      return requestsResource;
   }

   /**
    * Route to "subjects" sub endpoint
    * 
    * @return SubjectsResource
    */
   @Path(ConstantsIF.SUBJECTS)
   public SubjectsResource useSubjects() {
      String METHOD = Thread.currentThread().getStackTrace()[1].getMethodName();
      SubjectsResource subjectsResource = null;

      _logger.entering(CLASS, METHOD);

      subjectsResource = new SubjectsResource(_uriInfo, _servletCtx, _httpHdrs);

      _logger.exiting(CLASS, METHOD);

      return subjectsResource;
   }
}
