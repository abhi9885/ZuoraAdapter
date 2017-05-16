package com.cognizant.ipm.adapter.query;

import oracle.tip.tools.ide.adapters.cloud.api.model.CloudDataObjectNode;

public class QueryItemMetadata
{
  private String relRelationshipString = "";
  private String absRelationshipString = "";
  private String objectName = "";
  private String alias = "";
  private boolean isUserDefinedAlias = false;
  private CloudDataObjectNode customObjectNode = null;
  private CloudDataObjectNode modelObjectNode = null;
  
  public boolean isUserDefinedAlias() {
    return this.isUserDefinedAlias;
  }
  
  public void setCustomObjectNode(CloudDataObjectNode customObjectNode) {
    this.customObjectNode = customObjectNode;
  }
  
  public CloudDataObjectNode getCustomObjectNode() {
    return this.customObjectNode;
  }
  
  public void setModelObjectNode(CloudDataObjectNode modelObjectNode) {
    this.modelObjectNode = modelObjectNode;
  }
  
  public CloudDataObjectNode getModelObjectNode() {
    return this.modelObjectNode;
  }
  
  public void setRelRelationshipString(String relRelationshipString) {
    this.relRelationshipString = relRelationshipString;
  }
  
  public String getRelRelationshipString() {
    return this.relRelationshipString;
  }
  
  public void setAbsRelationshipString(String absRelationshipString) {
    this.absRelationshipString = absRelationshipString;
  }
  
  public String getAbsRelationshipString() {
    return this.absRelationshipString;
  }
  
  public void setObjectName(String objectName) {
    this.objectName = objectName;
  }
  
  public String getObjectName() {
    return this.objectName;
  }
  
  public void setAlias(String alias) {
    this.alias = alias;
  }
  
  public String getAlias() {
    return this.alias;
  }

  public QueryItemMetadata(String p_objectName, String p_alias, boolean p_userDefinedAlias, CloudDataObjectNode p_customObjectNode, CloudDataObjectNode p_modelObjectNode, String p_relRelationshipString, String p_absRelationshipString)
  {
    this.relRelationshipString = p_relRelationshipString;
    this.absRelationshipString = p_absRelationshipString;
    this.objectName = p_objectName;
    this.alias = p_alias;
    this.isUserDefinedAlias = p_userDefinedAlias;
    this.customObjectNode = p_customObjectNode;
    this.modelObjectNode = p_modelObjectNode;
  }

  public QueryItemMetadata(String p_relRelationshipString, String p_objectName, String p_alias, boolean p_userDefinedAlias, CloudDataObjectNode p_customObjectNode, CloudDataObjectNode p_modelObjectNode)
  {
    this.relRelationshipString = p_relRelationshipString;
    this.objectName = p_objectName;
    this.alias = p_alias;
    this.isUserDefinedAlias = p_userDefinedAlias;
    this.customObjectNode = p_customObjectNode;
    this.modelObjectNode = p_modelObjectNode;
  }

  public QueryItemMetadata(String p_objectName, CloudDataObjectNode p_customObjectNode, CloudDataObjectNode p_modelObjectNode)
  {
    this.relRelationshipString = "";
    this.absRelationshipString = "";
    this.objectName = p_objectName;
    this.alias = p_objectName;
    this.customObjectNode = p_customObjectNode;
    this.modelObjectNode = p_modelObjectNode;
  }
}