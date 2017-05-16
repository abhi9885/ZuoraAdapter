package com.cognizant.ipm.adapter.runtime.parser;

import java.util.Set;

public abstract interface APINode
{
  public abstract String getName();
  
  public abstract String getNamespace();
  
  public abstract String getVersion();
  
  public abstract String getDescription();
  
  public abstract Set<String> getOperationNames();
}