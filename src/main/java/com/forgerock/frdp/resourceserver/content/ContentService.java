/*
 * Copyright (c) 2020, ForgeRock, Inc., All rights reserved
 * Use subject to license terms.
 */
package com.forgerock.frdp.resourceserver.content;

import com.forgerock.frdp.common.ConstantsIF;
import com.forgerock.frdp.common.CoreIF;
import com.forgerock.frdp.common.Data;
import com.forgerock.frdp.dao.Operation;
import com.forgerock.frdp.dao.OperationIF;
import com.forgerock.frdp.utils.JSON;
import com.forgerock.frdp.utils.STR;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

/**
 * Abstract class for the Content Service
 *
 * @author Scott Fehrman, ForgeRock, Inc.
 */
public abstract class ContentService extends Data implements ContentServiceIF {

   private final String CLASS = this.getClass().getName();
   private String _id = null;  // unique identifier for the Service
   private final Map<OperationIF.TYPE, OperationIF> _operations = new HashMap<>();
   protected final Logger _logger = Logger.getLogger(this.getClass().getName());
   protected final OperationIF.TYPE[] _opertypes = {OperationIF.TYPE.CREATE, OperationIF.TYPE.READ, OperationIF.TYPE.REPLACE, OperationIF.TYPE.DELETE};

   /**
    * Default constructor
    * @param configuration JSON data
    */
   public ContentService(final JSONObject configuration) {
      super();

      String METHOD = "ContentService()";

      _logger.entering(CLASS, METHOD);

      this.init(configuration);

      _logger.exiting(CLASS, METHOD);

      return;
   }

   @Override
   public CoreIF copy() {
      throw new UnsupportedOperationException("Not supported yet.");
   }

   @Override
   public void setOperation(OperationIF oper) {
      String METHOD = Thread.currentThread().getStackTrace()[1].getMethodName();

      _logger.entering(CLASS, METHOD);

      _operations.put(oper.getType(), oper);

      _logger.exiting(CLASS, METHOD);

      return;
   }

   @Override
   public boolean hasOperation(OperationIF.TYPE type) {
      boolean found = false;
      String METHOD = Thread.currentThread().getStackTrace()[1].getMethodName();

      _logger.entering(CLASS, METHOD);

      found = _operations.containsKey(type);

      _logger.exiting(CLASS, METHOD);

      return found;
   }

   @Override
   public OperationIF getOperation(OperationIF.TYPE type) {
      String METHOD = Thread.currentThread().getStackTrace()[1].getMethodName();
      OperationIF oper = null;

      _logger.entering(CLASS, METHOD);

      if (_operations.containsKey(type)) {
         oper = _operations.get(type);
      }

      _logger.exiting(CLASS, METHOD);

      return oper;
   }

   @Override
   public String getId() {
      String METHOD = Thread.currentThread().getStackTrace()[1].getMethodName();

      _logger.entering(CLASS, METHOD);
      _logger.exiting(CLASS, METHOD);

      return _id;
   }

   /*
    * ===============
    * PRIVATE METHODS
    * ===============
    */
   private void init(JSONObject configuration) {
      String METHOD = Thread.currentThread().getStackTrace()[1].getMethodName();
      String msg = null;
      String attrName = null;
      String attrValue = null;
      String hdrName = null;
      String hdrValue = null;
      JSONObject jsonOperations = null;
      JSONObject jsonOperation = null;
      JSONObject jsonData = null;
      JSONObject jsonHeaders = null;
      OperationIF operation = null;
      Map<String, String> mapHeaders = null;

      /*
       * process the JSON configuration
       * {
       *     "id": "xxx",
       *     "label": "...",
       *     "description": "...",
       *     "operations": {
       *         "create": { // "create|read|replace|delete"
       *             "action": "post|get|put|delete|reference",
       *             "uri": "...",
       *             "headers": {
       *                 "Content-Type": "application/json"
       *             }
       *         }
       *     }
       * }
       */
      _logger.entering(CLASS, METHOD);

      if (configuration != null) {
         attrName = ConstantsIF.ID;
         attrValue = JSON.getString(configuration, attrName);
         if (!STR.isEmpty(attrValue)) {
            _id = new String(attrValue);

            attrName = ConstantsIF.LABEL;
            attrValue = JSON.getString(configuration, attrName);
            if (!STR.isEmpty(attrValue)) {
               this.setParam(attrName, attrValue);
            }

            attrName = ConstantsIF.DESCRIPTION;
            attrValue = JSON.getString(configuration, attrName);
            if (!STR.isEmpty(attrValue)) {
               this.setParam(attrName, attrValue);
            }

            jsonOperations = JSON.getObject(configuration, ConstantsIF.OPERATIONS);
            if (jsonOperations != null) {
               /*
                * Possible operations: CREATE, READ, REPLACE, DELETE
                */
               for (OperationIF.TYPE opertype : _opertypes) {
                  jsonOperation = JSON.getObject(jsonOperations, opertype.toString().toLowerCase());
                  if (jsonOperation != null) {
                     operation = new Operation(opertype);
                     /*
                      * Get "action" attribute (required)
                      */
                     attrName = ConstantsIF.ACTION;
                     attrValue = JSON.getString(jsonOperation, attrName);

                     if (!STR.isEmpty(attrValue)) {

                        switch (attrValue) {
                           case ConstantsIF.POST:
                              break;
                           case ConstantsIF.GET:
                              break;
                           case ConstantsIF.PUT:
                              break;
                           case ConstantsIF.DELETE:
                              break;
                           case ConstantsIF.REFERENCE:
                              break;
                           default:
                              msg = "Attribute '" + ConstantsIF.ACTION
                                 + "' has an invalid value for Operation '"
                                 + opertype.toString().toLowerCase()
                                 + "' in Service '" + _id + "'";
                              break;
                        }
                        if (msg != null) {
                           break; // break out of the for loop
                        }

                        operation.setParam(attrName, attrValue);

                        /*
                         * Get "uri" attribute (optional)
                         */
                        attrName = ConstantsIF.URI;
                        attrValue = JSON.getString(jsonOperation, attrName);

                        if (!STR.isEmpty(attrValue)) {
                           operation.setParam(attrName, attrValue);
                        }

                        /*
                         * Get "headers" object
                         */
                        attrName = ConstantsIF.HEADERS;
                        jsonHeaders = JSON.getObject(jsonOperation, attrName);

                        if (jsonHeaders != null && !jsonHeaders.isEmpty()) {
                           jsonData = new JSONObject();
                           jsonData.put(ConstantsIF.HEADERS, jsonHeaders);
                           operation.setJSON(jsonData);
                        }
                     } else {
                        msg = "Attribute '" + ConstantsIF.ACTION
                           + "' is empty for Operation '"
                           + opertype.toString().toLowerCase()
                           + "' in Service '" + _id + "'";
                        break; // break out of the for loop
                     }
                  }
                  if (msg == null) { // add operation to the map
                     _operations.put(operation.getType(), operation);
                  }
               }

            } else {
               msg = "Service 'operations' array is null";
            }
         } else {
            msg = "Service Attribute '" + ConstantsIF.ID + "' is empty";
         }
      } else {
         msg = "Configuration JSON input is null";
      }

      if (msg != null) {
         this.setError(true);
         this.setStatus(msg);
         _logger.log(Level.SEVERE, this.getStatus());
      }

      _logger.exiting(CLASS, METHOD);

      return;
   }

}
