/*
 * Copyright (c) 2018-2020, ForgeRock, Inc., All rights reserved
 * Use subject to license terms.
 */

package com.forgerock.frdp.resourceserver.handler;

import com.forgerock.frdp.dao.OperationIF;
import com.forgerock.frdp.handler.HandlerIF;

/**
 * JaxrsHandler interface
 * 
 * @author Scott Fehrman, ForgeRock Inc.
 */
public interface JaxrsHandlerIF extends HandlerIF {

   public static final String HANDLER_AMOAUTH2 = "AMOauth2Handler";
   public static final String HANDLER_AMPROXYADM = "AMProxyAdmHandler";
   public static final String HANDLER_AMSESSION = "AMSessionHandler";
   public static final String HANDLER_RESOURCES = "ResourcesHandler";
   public static final String HANDLER_META = "MetaHandler";
   public static final String HANDLER_CONTENT = "ContentHandler";
   public static final String HANDLER_UMA_PAT = "UMAProtectionApiTokenHandler";
   public static final String HANDLER_UMA_DISCOVER = "UMADiscoverHandler";
   public static final String HANDLER_UMA_REQUESTS = "UMARequestsHandler";
   public static final String HANDLER_UMA_PERMREQ = "UMAPermissionRequestHandler";
   public static final String HANDLER_UMA_POLICY = "UMAPolicyHandler";
   public static final String HANDLER_UMA_REGISTER = "UMARegisterHandler";
   public static final String HANDLER_UMA_SHAREDWITHME = "UMASharedWithMeHandler";
   public static final String HANDLER_UMA_SUBJECTS = "UMASubjectsHandler";
   public static final String HANDLER_UMA_WELLKNOWN = "UMAWellKnownHandler";

   public OperationIF process(final OperationIF operInput);
}
