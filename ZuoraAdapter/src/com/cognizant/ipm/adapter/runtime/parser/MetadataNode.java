package com.cognizant.ipm.adapter.runtime.parser;

import java.util.Map;

import javax.xml.namespace.QName;

public abstract interface MetadataNode
{
  public abstract String getName();
  
  public abstract MetadataNode getParent();
  
  public abstract QName getQualifiedName();
  
  public abstract String getDescription();
  
  public abstract Map<String, Object> getNodeAttributes();
}