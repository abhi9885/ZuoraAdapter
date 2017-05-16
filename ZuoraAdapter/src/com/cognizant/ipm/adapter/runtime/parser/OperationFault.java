package com.cognizant.ipm.adapter.runtime.parser;

public abstract interface OperationFault
  extends MetadataNode
{
  public abstract DataObjectNode getFaultDataObject();
}