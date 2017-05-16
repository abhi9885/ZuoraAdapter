package com.cognizant.ipm.adapter.runtime.parser;

import java.util.Map;

public abstract interface CloudField
{
  public abstract String getName();
  public abstract DataObjectNode getFieldType();
  public abstract boolean isArray();
  public abstract boolean isRequired();
  public abstract boolean isNullAllowed();
  public abstract Map<String, Object> getAttributes();
}