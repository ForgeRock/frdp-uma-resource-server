/*
 * Copyright (c) 2018-2020, ForgeRock, Inc., All rights reserved
 * Use subject to license terms.
 */

package com.forgerock.frdp.resourceserver.rest;

import java.util.Set;
import javax.ws.rs.ApplicationPath;
import javax.ws.rs.core.Application;
/**
 *
 * @author Scott Fehrman, ForgeRock, Inc.
 */
@ApplicationPath("rest")
public class ApplicationConfig extends Application {

   @Override
   public Set<Class<?>> getClasses() {
      Set<Class<?>> resources = new java.util.HashSet<Class<?>>();
      resources.add(com.forgerock.frdp.resourceserver.rest.config.ConfigResource.class);
      resources.add(com.forgerock.frdp.resourceserver.rest.manage.ContentResource.class);
      resources.add(com.forgerock.frdp.resourceserver.rest.manage.ManageResource.class);
      resources.add(com.forgerock.frdp.resourceserver.rest.manage.MetaResource.class);
      resources.add(com.forgerock.frdp.resourceserver.rest.manage.PolicyResource.class);
      resources.add(com.forgerock.frdp.resourceserver.rest.manage.RegisterResource.class);
      resources.add(com.forgerock.frdp.resourceserver.rest.manage.RequestsResource.class);
      resources.add(com.forgerock.frdp.resourceserver.rest.manage.ResourcesResource.class);
      resources.add(com.forgerock.frdp.resourceserver.rest.manage.SubjectsResource.class);
      resources.add(com.forgerock.frdp.resourceserver.rest.share.DiscoverResource.class);
      resources.add(com.forgerock.frdp.resourceserver.rest.share.OwnersResource.class);
      resources.add(com.forgerock.frdp.resourceserver.rest.share.PolicyResource.class);
      resources.add(com.forgerock.frdp.resourceserver.rest.share.ResourcesResource.class);
      resources.add(com.forgerock.frdp.resourceserver.rest.share.ShareResource.class);
      resources.add(com.forgerock.frdp.resourceserver.rest.share.WithMeResource.class);
      // addRestResourceClasses(resources);
      return resources;
   }

   /**
    * Do not modify addRestResourceClasses() method. It is automatically populated
    * with all resources defined in the project. If required, comment out calling
    * this method in getClasses().
    */
   private void addRestResourceClasses(Set<Class<?>> resources) {
      resources.add(com.forgerock.frdp.resourceserver.rest.config.ConfigResource.class);
      resources.add(com.forgerock.frdp.resourceserver.rest.manage.ContentResource.class);
      resources.add(com.forgerock.frdp.resourceserver.rest.manage.ManageResource.class);
      resources.add(com.forgerock.frdp.resourceserver.rest.manage.MetaResource.class);
      resources.add(com.forgerock.frdp.resourceserver.rest.manage.PolicyResource.class);
      resources.add(com.forgerock.frdp.resourceserver.rest.manage.RegisterResource.class);
      resources.add(com.forgerock.frdp.resourceserver.rest.manage.RequestsResource.class);
      resources.add(com.forgerock.frdp.resourceserver.rest.manage.ResourcesResource.class);
      resources.add(com.forgerock.frdp.resourceserver.rest.manage.SubjectsResource.class);
      resources.add(com.forgerock.frdp.resourceserver.rest.share.DiscoverResource.class);
      resources.add(com.forgerock.frdp.resourceserver.rest.share.PolicyResource.class);
      resources.add(com.forgerock.frdp.resourceserver.rest.share.ResourcesResource.class);
      resources.add(com.forgerock.frdp.resourceserver.rest.share.ShareResource.class);
      resources.add(com.forgerock.frdp.resourceserver.rest.share.WithMeResource.class);
   }

}
