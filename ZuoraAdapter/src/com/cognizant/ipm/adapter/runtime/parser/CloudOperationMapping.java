package com.cognizant.ipm.adapter.runtime.parser;

import java.util.List;

public class CloudOperationMapping
{
  private String newOperationName;
  private OperationNode targetOperation;
  private List<TypeMapping> responseObjectMappings;
  private List<TypeMapping> requestObjectMappings;
  private ObjectGrouping defaultRequestObjectGrouping;
  private ObjectGrouping defaultResponseObjectGrouping;
  private java.util.Properties operationProperties;
  
  public CloudOperationMapping(OperationNode targetOperation)
  {
    this(targetOperation, ObjectGrouping.ORDERED, targetOperation.getName());
  }
  
  public CloudOperationMapping(OperationNode targetOperation, ObjectGrouping requestGrouping, String newOperationName)
  {
    this(targetOperation, requestGrouping, ObjectGrouping.ORDERED, newOperationName);
  }
  


  public CloudOperationMapping(OperationNode targetOperation, ObjectGrouping requestGrouping, ObjectGrouping responseGrouping, String newOperationName)
  {
    this.targetOperation = targetOperation;
    this.defaultRequestObjectGrouping = requestGrouping;
    this.defaultResponseObjectGrouping = responseGrouping;
    this.newOperationName = newOperationName;
    if (this.newOperationName == null) {
      this.newOperationName = targetOperation.getName();
    }
    this.responseObjectMappings = new java.util.ArrayList();
    this.requestObjectMappings = new java.util.ArrayList();
  }
  
  public java.util.Properties getOperationProperties() {
    return this.operationProperties;
  }
  
  public void setOperationProperties(java.util.Properties operationProperties) {
    this.operationProperties = operationProperties;
  }
  
  public void setNewOperationName(String newOperationName) {
    this.newOperationName = newOperationName;
  }
  
  public String getNewOperationName() {
    return this.newOperationName;
  }
  
  public void setTargetOperation(OperationNode operation) {
    this.targetOperation = operation;
  }
  
  public OperationNode getTargetOperation() {
    return this.targetOperation;
  }
  
  public List<TypeMapping> getRequestObjectMappings() {
    return this.requestObjectMappings;
  }
  
  public void setRequestObjectMappings(List<TypeMapping> requestObjectMappings) {
    this.requestObjectMappings = requestObjectMappings;
  }
  
  public List<TypeMapping> getResponseObjectMapping() {
    return this.responseObjectMappings;
  }
  
  public void setResponseObjectMapping(List<TypeMapping> responseObjectMapping) {
    this.responseObjectMappings = responseObjectMapping;
  }
  
  public ObjectGrouping getDefaultRequestObjectGrouping() {
    return this.defaultRequestObjectGrouping;
  }
  
  public void setDefaultRequestObjectGrouping(ObjectGrouping grouping) {
    this.defaultRequestObjectGrouping = grouping;
  }
  
  public ObjectGrouping getDefaultResponseObjectGrouping() {
    return this.defaultResponseObjectGrouping;
  }
  
  public void setDefaultResponseObjectGrouping(ObjectGrouping grouping) {
    this.defaultResponseObjectGrouping = grouping;
  }
  
  public void setOperationProperty(String propertyName, String propertyValue) {
    if (this.operationProperties == null) {
      this.operationProperties = new java.util.Properties();
    }
    this.operationProperties.setProperty(propertyName, propertyValue);
  }
}