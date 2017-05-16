package com.cognizant.ipm.adapter.runtime.parser;

import java.util.HashMap;
import java.util.Map;

public class CloudFieldImpl implements CloudField
{
  private DataObjectNode fieldType;
  private String name;
  private boolean array;
  private boolean required;
  private boolean nullable;
  private Map<String, Object> attributes = new HashMap<String, Object>();
  
  public CloudFieldImpl(String name, DataObjectNode fieldType, boolean array, boolean required, boolean nullable)
  {
    this.name = name;
    this.array = array;
    this.required = required;
    this.fieldType = fieldType;
    this.nullable = nullable;
  }
  
  public String getName() {
    return this.name;
  }
  
  public DataObjectNode getFieldType() {
    return this.fieldType;
  }
  
  public boolean isArray() {
    return this.array;
  }
  
  public boolean isRequired() {
    return this.required;
  }
  
  public Map<String, Object> getAttributes() {
    return this.attributes;
  }
  
  public boolean isNullAllowed() {
    return this.nullable;
  }
  
  public boolean equals(Object other) {
    return ((other instanceof CloudFieldImpl)) && (other != null) && (((CloudFieldImpl)other).getName().equals(this.name));
  }
  
  public int hashCode()
  {
    return this.name.hashCode();
  }
}