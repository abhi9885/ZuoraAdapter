package com.cognizant.ipm.adapter.query;

import java.util.ArrayList;
import java.util.List;

import oracle.tip.tools.ide.adapters.cloud.api.model.CloudDataObjectNode;

import com.cognizant.ipm.adapter.util.CloudUtil;
import com.cognizant.ipm.adapter.util.CloudValidationError;

public class CloudQueryModel
{
  private List<QueryItemMetadata> queryItems = new ArrayList<QueryItemMetadata>();
  private List<QueryItemMetadata> tablesMetadata = new ArrayList<QueryItemMetadata>();
  private CloudDataObjectNode mainQDrivingObject = null;
  private CloudDataObjectNode curQDrivingObject = null;
  private List<CloudValidationError> errorList = null;

  public void setErrorList(List<CloudValidationError> errorList)
  {
    this.errorList = errorList;
  }
  
  public List<CloudValidationError> getErrorList() {
    return this.errorList;
  }
  
  public void setCurQDrivingObject(CloudDataObjectNode curQDrivingObject) {
    this.curQDrivingObject = curQDrivingObject;
  }
  
  public CloudDataObjectNode getCurQDrivingObject() {
    return this.curQDrivingObject;
  }
  
  public List<QueryItemMetadata> getQueryItems() {
    return this.queryItems;
  }
  
  public void setMainQDrivingObject(CloudDataObjectNode mainQDrivingObject) {
    this.mainQDrivingObject = mainQDrivingObject;
  }
  
  public CloudDataObjectNode getMainQDrivingObject() {
    return this.mainQDrivingObject;
  }
  
  public void addQueryItem(QueryItemMetadata p_queryItem) {
    if (null == getQueryItemByObjectName(p_queryItem.getObjectName())) {
      this.queryItems.add(p_queryItem);
    }
  }
  
  public void addTableMetadata(QueryItemMetadata p_queryItem) {
    if (null == getTablesMetadataByObjectName(p_queryItem.getObjectName()))
    {
      this.tablesMetadata.add(p_queryItem);
    }
  }
  
  public List<CloudDataObjectNode> getSelectedCustomObjectsList() {
    List<CloudDataObjectNode> toBeReturnList = new ArrayList<CloudDataObjectNode>();
    for (QueryItemMetadata queryItem : this.queryItems) {
      toBeReturnList.add(queryItem.getCustomObjectNode());
    }
    return toBeReturnList;
  }
  
  public List<CloudDataObjectNode> getSelectedModelObjectsList() {
    List<CloudDataObjectNode> toBeReturnList = new ArrayList<CloudDataObjectNode>();
    for (QueryItemMetadata queryItem : this.queryItems) {
      toBeReturnList.add(queryItem.getModelObjectNode());
    }
    return toBeReturnList;
  }

  public QueryItemMetadata getQueryItemByObjectName(String p_objName)
  {
    for (QueryItemMetadata queryItem : this.queryItems) {
      if ((queryItem.getObjectName().equalsIgnoreCase(p_objName)) || 
    		  (queryItem.getAlias().equalsIgnoreCase(p_objName)) || 
    		  (queryItem.getModelObjectNode().getName().equalsIgnoreCase(p_objName)) || 
    		  (CloudUtil.getSingularObjectName(queryItem.getObjectName()).equalsIgnoreCase(p_objName)))
      {
        return queryItem;
      }
    }
    return null;
  }
  
  public QueryItemMetadata getTablesMetadataByObjectName(String p_objName) {
    for (QueryItemMetadata queryItem : this.tablesMetadata) {
      if ((queryItem.getObjectName().equalsIgnoreCase(p_objName)) || 
    		  ((queryItem.isUserDefinedAlias()) && (queryItem.getAlias().equalsIgnoreCase(p_objName))) || 
    		  (CloudUtil.getSingularObjectName(queryItem.getObjectName()).equalsIgnoreCase(p_objName)))
      {
        return queryItem;
      }
    }
    return null;
  }
}