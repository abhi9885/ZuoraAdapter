package com.cognizant.ipm.adapter.runtime.parser;

public class TypeMapping {
  private DataObjectNode targetDataObject;
  private boolean array;
  private boolean optional;
  private RequestParameter targetParameter;
  private TypeMask mask;
  
  public TypeMapping(DataObjectNode targetDataObject) {
    this(targetDataObject, false, false);
  }
  
  public TypeMapping(DataObjectNode targetDataObjectNode, boolean array, boolean optional)
  {
    this.targetDataObject = targetDataObjectNode;
    this.array = array;
    this.optional = optional;
  }
  
  public DataObjectNode getTargetDataObject() {
    return this.targetDataObject;
  }
  
  public void setTargetDataObject(DataObjectNode dataObjectNode) {
    this.targetDataObject = dataObjectNode;
  }
  
  public boolean isArray() {
    return this.array;
  }
  
  public void setArray(boolean array) {
    this.array = array;
  }
  
  public boolean isOptional() {
    return this.optional;
  }
  
  public void setOptional(boolean optional) {
    this.optional = optional;
  }
  
  public RequestParameter getTargetParameter() {
    return this.targetParameter;
  }
  
  public void setTargetParameter(RequestParameter targetParameter) {
    this.targetParameter = targetParameter;
  }
  
  public TypeMask getMask() {
    return this.mask;
  }
  
  public void setMask(TypeMask mask) {
    this.mask = mask;
  }
}