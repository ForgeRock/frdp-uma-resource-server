/*
 * Copyright (c) 2020, ForgeRock, Inc., All rights reserved
 * Use subject to license terms.
 */
package com.forgerock.frdp.resourceserver.content;

import org.json.simple.JSONObject;

/**
 * Basic class for the Content Service
 *
 * @author Scott Fehrman, ForgeRock, Inc.
 */
public class BasicContentService extends ContentService {

   private final String CLASS = this.getClass().getName();

   /**
    * Default constructor
    * @param configuration JSON data
    */
   public BasicContentService(final JSONObject configuration) {
      super(configuration);

      String METHOD = "BasicContentService()";

      _logger.entering(CLASS, METHOD);

      _logger.exiting(CLASS, METHOD);

      return;
   }


}
