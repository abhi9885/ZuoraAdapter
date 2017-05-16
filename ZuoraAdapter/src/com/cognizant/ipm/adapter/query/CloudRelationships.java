package com.cognizant.ipm.adapter.query;

public class CloudRelationships
{
  private String relationshipName;
  private String relatedObjectName;
  public String getRelationshipName()
  {
    return this.relationshipName;
  }
  
  public void setRelationshipName(String relationshipName) {
    this.relationshipName = relationshipName;
  }
  
  public String getRelatedObjectName() {
    return this.relatedObjectName;
  }
  
  public void setRelatedObjectName(String relatedObjectName) {
    this.relatedObjectName = relatedObjectName;
  }
}