package com.cognizant.ipm.adapter.runtime.parser;

import java.util.List;

public abstract interface OperationResponse
  extends MetadataNode
{
  public abstract DataObjectNode getResponseObject();
  
  public abstract List<CloudHeader> getResponseHeaders();
}