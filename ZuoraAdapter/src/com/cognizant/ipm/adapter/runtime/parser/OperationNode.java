package com.cognizant.ipm.adapter.runtime.parser;

import java.util.List;

public abstract interface OperationNode extends java.io.Serializable, MetadataNode
{
  public abstract InvocationStyle getInvocationStyle();
  
  public abstract OperationResponse getResponse();
  
  public abstract List<RequestParameter> getRequestParameters();
  
  public abstract List<OperationFault> getFaults();
  
  public abstract List<CloudHeader> getRequestHeaders();
  
  public abstract void setName(String paramString);
  
  public abstract String getOperationPath();
  
  public abstract void setOperationPath(String paramString);
  
  public static enum InvocationStyle
  {
    REQUEST_RESPONSE,  ONEWAY,  ASYNCHRONOUS;
    
    private InvocationStyle() {}
  }
}