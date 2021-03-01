/*
 * Copyright (c) 2019-2020, ForgeRock, Inc., All rights reserved
 * Use subject to license terms.
 */
package com.forgerock.frdp.resourceserver;

/**
 *
 * @author Scott Fehrman, ForgeRock Inc., scott.fehrman@forgerock.com
 */
public interface ConfigIF {

   public static final String AS_CONNECT = "as.connect";
   public static final String AS_CONNECT_HOST = "as.connect.host";
   public static final String AS_COOKIE = "as.cookie";
   public static final String AS_ADMIN_USER = "as.admin.user";
   public static final String AS_ADMIN_PASSWORD = "as.admin.password";
   public static final String AS_AUTHENTICATE_HEADERS_USER = "as.authenticate.headers.user";
   public static final String AS_AUTHENTICATE_HEADERS_PASSWORD = "as.authenticate.headers.password";
   public static final String AS_AUTHENTICATE_PARAMS = "as.authenticate.params";
   public static final String AS_AUTHENTICATE_PATH = "as.authenticate.path";
   public static final String AS_AUTHENTICATE_ACCEPT = "as.authenticate.accept-api-version";
   public static final String AS_OAUTH2_PATH = "as.oauth2.path";
   public static final String AS_OAUTH2_AUTHORIZE_ACCEPT = "as.oauth2.authorize.accept-api-version";
   public static final String AS_OAUTH2_AUTHORIZE_PATH = "as.oauth2.authorize.path";
   public static final String AS_OAUTH2_ACCESS_TOKEN_ACCEPT = "as.oauth2.access_token.accept-api-version";
   public static final String AS_OAUTH2_ACCESS_TOKEN_PATH = "as.oauth2.access_token.path";
   public static final String AS_OAUTH2_TOKENINFO_PATH = "as.oauth2.tokeninfo.path";
   public static final String AS_SESSIONS_ACCEPT = "as.sessions.accept-api-version";
   public static final String AS_SESSIONS_PATH = "as.sessions.path";
   public static final String AS_UMA_PATH = "as.uma.path";
   public static final String AS_UMA_PENDINGREQUESTS_PATH = "as.uma.pendingrequests.path";
   public static final String AS_UMA_PENDINGREQUESTS_ACCEPT = "as.uma.pendingrequests.accept-api-version";
   public static final String AS_UMA_PENDINGREQUESTS_QUERYFILTER = "as.uma.pendingrequests.queryfilter";
   public static final String AS_UMA_PENDINGREQUESTS_SORTKEYS = "as.uma.pendingrequests.sortkeys";
   public static final String AS_UMA_PERMISSION_REQUEST_PATH = "as.uma.permission_request.path";
   public static final String AS_UMA_POLICIES_ACCEPT = "as.uma.policies.accept-api-version";
   public static final String AS_UMA_POLICIES_PATH = "as.uma.policies.path";
   public static final String AS_UMA_RESOURCE_SET_PATH = "as.uma.resource_set.path";
   public static final String AS_UMA_SHAREDWITHME_PATH = "as.uma.sharedwithme.path";
   public static final String AS_UMA_SHAREDWITHME_ACCEPT = "as.uma.sharedwithme.accept-api-version";
   public static final String AS_UMA_SHAREDWITHME_QUERYFILTER = "as.uma.sharedwithme.queryfilter";
   public static final String AS_UMA_SHAREDWITHME_SORTKEYS = "as.uma.sharedwithme.sortkeys";
   public static final String AS_UMA_WELL_KNOWN_PATH = "as.uma.well_known.path";

   public static final String CS_CONNECT = "cs.connect";

   public static final String RS_HEADERS_SSOTOKEN = "rs.headers.ssotoken";
   public static final String RS_HEADERS_RPT = "rs.headers.rpt";
   public static final String RS_ID = "rs.id";
   public static final String RS_CREDENTIAL_CATEGORIES_PAT_ID = "rs.credential.categories.pat.id";
   public static final String RS_CREDENTIAL_CATEGORIES_SSO_ID = "rs.credential.categories.sso.id";
   public static final String RS_OAUTH2_CLIENT_ID = "rs.oauth2.client.id";
   public static final String RS_OAUTH2_CLIENT_SECRET = "rs.oauth2.client.secret";
   public static final String RS_OAUTH2_CLIENT_REDIRECT = "rs.oauth2.client.redirect";
   public static final String RS_NOSQL = "rs.nosql";
   public static final String RS_NOSQL_COLLECTIONS_CREDENTIALS_NAME = "rs.nosql.collections.credentials.name";
   public static final String RS_NOSQL_COLLECTIONS_RESOURCES_NAME = "rs.nosql.collections.resources.name";
   public static final String RS_NOSQL_DATABASE = "rs.nosql.database";

}
