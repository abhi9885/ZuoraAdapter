package com.cognizant.ipm.adapter.util;

import java.util.Collections;
import java.util.List;

public class PushtopicDetails
{
  private String apiVersion;
  private String id;
  private String isActive;
  private String name;
  private String notifyForFields;
  private String notifyForOperationCreate;
  private String notifyForOperationDelete;
  private String notifyForOperationUndelete;
  private String notifyForOperationUpdate;
  private String query;
  private String queryWithSortedFields;
  
  public void setApiVersion(String apiVersion)
  {
    this.apiVersion = apiVersion;
  }
  
  public String getApiVersion() {
    return this.apiVersion;
  }
  
  public void setId(String id) {
    this.id = id;
  }
  
  public String getId() {
    return this.id;
  }
  
  public void setActive(String isActive) {
    this.isActive = isActive;
  }
  
  public String IsActive() {
    return this.isActive;
  }
  
  public void setName(String name) {
    this.name = name;
  }
  
  public String getName() {
    return this.name;
  }
  
  public void setNotifyForFields(String notifyForFields) {
    this.notifyForFields = notifyForFields;
  }
  
  public String getNotifyForFields() {
    return this.notifyForFields;
  }
  
  public void setNotifyForOperationCreate(String notifyForOperationCreate) {
    this.notifyForOperationCreate = notifyForOperationCreate;
  }
  
  public String getNotifyForOperationCreate() {
    return this.notifyForOperationCreate;
  }
  
  public void setNotifyForOperationDelete(String notifyForOperationDelete) {
    this.notifyForOperationDelete = notifyForOperationDelete;
  }
  
  public String getNotifyForOperationDelete() {
    return this.notifyForOperationDelete;
  }
  
  public void setNotifyForOperationUndelete(String notifyForOperationUndelete) {
    this.notifyForOperationUndelete = notifyForOperationUndelete;
  }
  
  public String getNotifyForOperationUndelete() {
    return this.notifyForOperationUndelete;
  }
  
  public void setNotifyForOperationUpdate(String notifyForOperationUpdate) {
    this.notifyForOperationUpdate = notifyForOperationUpdate;
  }
  
  public String getNotifyForOperationUpdate() {
    return this.notifyForOperationUpdate;
  }
  
  public void setQuery(String query) {
    this.query = query;
    setQueryWithSortedFields();
  }
  
  public String getQuery() {
    return this.query;
  }
  
  private void setQueryWithSortedFields() {
    List<String> values = new java.util.ArrayList();
    int maxLen = 0;
    
    String fields = this.query.substring(this.query.toUpperCase().indexOf("SELECT") + "SELECT".length(), this.query.toUpperCase().indexOf("FROM")).trim();
    

    String[] fieldsArray = fields.split(",");
    
    for (String s : fieldsArray) {
      values.add(s.trim().toLowerCase());
      if (s.length() > maxLen) {
        maxLen = s.length();
      }
    }
    Collections.sort(values, new CloudStringUtil.MyComparator(maxLen));
    
    StringBuilder queryBuilder = new StringBuilder();
    queryBuilder.append("SELECT ");
    for (int i = 0; i < values.size(); i++) {
      queryBuilder.append((String)values.get(i));
      if (i != values.size() - 1) {
        queryBuilder.append(",");
      }
    }
    
    queryBuilder.append(" ").append(this.query.substring(this.query.toUpperCase().indexOf("FROM")));
    
    this.queryWithSortedFields = queryBuilder.toString();
  }
  
  public boolean equals(Object obj)
  {
    boolean result = false;
    if ((obj == null) || (!(obj instanceof PushtopicDetails))) {
      result = false;
    } else {
      PushtopicDetails sfPushtopicDetails = (PushtopicDetails)obj;
      if ((this.notifyForFields.equalsIgnoreCase(sfPushtopicDetails.notifyForFields)) && (this.notifyForOperationCreate.equalsIgnoreCase(sfPushtopicDetails.notifyForOperationCreate)) && (this.notifyForOperationDelete.equalsIgnoreCase(sfPushtopicDetails.notifyForOperationDelete)) && (this.notifyForOperationUndelete.equalsIgnoreCase(sfPushtopicDetails.notifyForOperationUndelete)) && (this.notifyForOperationUpdate.equalsIgnoreCase(sfPushtopicDetails.notifyForOperationUpdate)) && (this.queryWithSortedFields.equalsIgnoreCase(sfPushtopicDetails.queryWithSortedFields)))
      {
        result = true;
      }
    }
    return result;
  }
}