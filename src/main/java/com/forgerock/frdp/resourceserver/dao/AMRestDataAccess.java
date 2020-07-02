/*
 * Copyright (c) 2018-2020, ForgeRock, Inc., All rights reserved
 * Use subject to license terms.
 */
package com.forgerock.frdp.resourceserver.dao;

import com.forgerock.frdp.common.ConstantsIF;
import com.forgerock.frdp.dao.OperationIF;
import com.forgerock.frdp.dao.rest.RestDataAccess;
import com.forgerock.frdp.utils.JSON;
import java.util.Map;
import org.json.simple.JSONObject;

/**
 * This class extends RestDataAccess to override the "validate" method. The
 * default "validate" method checks for a "uid" JSON attribute for all "GET"
 * request. Some of the Access Manager (AM) REST interfaces use GET requests but
 * there is not "uid". This "validate" method removes the check for "uid" when
 * it's a GET
 *
 * @author Scott Fehrman, ForgeRock, Inc.
 */
public class AMRestDataAccess extends RestDataAccess {

   private final String CLASS = this.getClass().getName();

   public AMRestDataAccess(Map<String, String> params) throws Exception {
      super(params);

      String METHOD = "AMRestDataAccess()";

      _logger.entering(CLASS, METHOD);
      _logger.exiting(CLASS, METHOD);

      return;
   }

   /*
    * ================= PROTECTED METHODS =================
    */
   /**
    * Validate the operation, custom processing for Access Manager
    *
    * @param oper OperationIF operation
    * @throws Exception could not validate the operation
    */
   @Override
   protected void validate(final OperationIF oper) throws Exception {
      String METHOD = Thread.currentThread().getStackTrace()[1].getMethodName();
      JSONObject jsonInput = null;
      JSONObject jsonData = null;
      JSONObject jsonForm = null;
      JSONObject jsonQuery = null;

      _logger.entering(CLASS, METHOD);

      if (oper == null) {
         throw new Exception("Operation object is null");
      }

      jsonInput = oper.getJSON();

      if (jsonInput == null || jsonInput.isEmpty()) {
         throw new Exception("JSON Input is null or empty");
      }

      // create and update require a "data" or "form" object
      if (oper.getType() == OperationIF.TYPE.CREATE || oper.getType() == OperationIF.TYPE.REPLACE) {
         if (jsonInput.containsKey(ConstantsIF.DATA)) {
            jsonData = JSON.getObject(jsonInput, ConstantsIF.DATA);

            if (jsonData == null) {
               throw new Exception("JSON 'data' is null");
            }
         } else if (jsonInput.containsKey(ConstantsIF.FORM)) {
            jsonForm = JSON.getObject(jsonInput, ConstantsIF.FORM);

            if (jsonForm == null) {
               throw new Exception("JSON 'form' is null");
            }
         } else {
            throw new Exception("JSON Input must contain either 'data' or 'form' objects.");
         }
      }

      // search require a "query" JSON object
      if (oper.getType() == OperationIF.TYPE.SEARCH) {
         jsonQuery = JSON.getObject(jsonInput, ConstantsIF.QUERY);

         if (jsonQuery == null || jsonQuery.isEmpty()) {
            throw new Exception("JSON Query is empty or missing");
         }
      }

      _logger.exiting(CLASS, METHOD);

      return;
   }

}
