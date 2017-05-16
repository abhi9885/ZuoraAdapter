package com.cognizant.ipm.adapter.runtime.parser;

public class CloudHeaderImpl implements CloudHeader
{
  private String namespace;
  private String name;
  private DataObjectNode type;
  private boolean required;
  
  public CloudHeaderImpl(String namespace, String name, DataObjectNode dataObject, boolean required) {
    this.namespace = namespace;
    this.name = name;
    this.type = dataObject;
    this.required = required;
  }
  
  public String getNamespace() {
    return this.namespace;
  }
  
  public String getName() {
    return this.name;
  }
  
  public DataObjectNode getType() {
    return this.type;
  }
  
  public boolean required() {
    return this.required;
  }
  
  public boolean equals(Object other) {
    return ((other instanceof CloudHeaderImpl)) && (((CloudHeaderImpl)other).getName().equals(this.name));
  }
  
  public int hashCode()
  {
    return this.name.hashCode();
  }
}