/*
 * Copyright (c) 2020, ForgeRock, Inc., All rights reserved
 * Use subject to license terms.
 */
package com.forgerock.frdp.resourceserver.content;

import com.forgerock.frdp.common.DataIF;
import com.forgerock.frdp.dao.OperationIF;

/**
 * Interface for the Content Service
 *
 * @author Scott Fehrman, ForgeRock, Inc.
 */
public interface ContentServiceIF extends DataIF {

   public void setOperation(OperationIF oper);
   
   public boolean hasOperation(OperationIF.TYPE type);

   public OperationIF getOperation(OperationIF.TYPE type);
   
   public String getId();
}
