package com.cognizant.ipm.adapter.runtime.parser;

public abstract interface CloudHeader
{
  public abstract String getNamespace();
  public abstract String getName();
  public abstract DataObjectNode getType();
  public abstract boolean required();
}