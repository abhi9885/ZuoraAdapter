package com.cognizant.ipm.adapter.runtime.parser;

public abstract interface RequestParameter
  extends MetadataNode
{
  public abstract DataObjectNode getDataType();
  
  public abstract boolean isArray();
}