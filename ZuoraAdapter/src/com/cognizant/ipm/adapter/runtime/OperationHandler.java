package com.cognizant.ipm.adapter.runtime;

import oracle.cloud.connector.api.CloudInvocationContext;
import oracle.cloud.connector.api.CloudInvocationException;

import org.w3c.dom.Document;

public abstract interface OperationHandler
{
  public abstract void handleOperationRequest(CloudInvocationContext paramCloudInvocationContext, Document paramDocument, String paramString)
    throws CloudInvocationException;
  
  public abstract void handleOperationResponse(CloudInvocationContext paramCloudInvocationContext, Document paramDocument, String paramString)
    throws CloudInvocationException;
  
  public abstract void handleOperationError(CloudInvocationContext paramCloudInvocationContext, Document paramDocument, String paramString)
    throws CloudInvocationException;
}


/* Location:              U:\Cognizant\Adapter\SugarCRMAdapter.jar!\com\cognizant\ipm\adapter\runtime\OperationHandler.class
 * Java compiler version: 8 (52.0)
 * JD-Core Version:       0.7.1
 */