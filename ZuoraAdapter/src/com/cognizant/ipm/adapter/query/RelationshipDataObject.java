package com.cognizant.ipm.adapter.query;

import java.util.ArrayList;

import oracle.tip.tools.ide.adapters.cloud.api.model.Field;

public class RelationshipDataObject
{
  private ArrayList<CloudRelationships> parentRelationships = null;
  private ArrayList<CloudRelationships> childRelationships = null;
  private ArrayList<Field> fieldList = null;
  private ArrayList<CloudFieldPropertyMap> fieldPropertyMap = null;
  private boolean queryable;
  private boolean searchable;
  
  public void setParentRelationships(ArrayList<CloudRelationships> parentRelationships)
  {
    this.parentRelationships = parentRelationships;
  }
  
  public ArrayList<CloudRelationships> getParentRelationships() {
    return this.parentRelationships;
  }
  
  public void setChildRelationships(ArrayList<CloudRelationships> childRelationships)
  {
    this.childRelationships = childRelationships;
  }
  
  public ArrayList<CloudRelationships> getChildRelationships() {
    return this.childRelationships;
  }
  
  public void setFields(ArrayList<Field> fieldList) {
    this.fieldList = fieldList;
  }
  
  public ArrayList<Field> getFields() {
    return this.fieldList;
  }
  
  public void setQueryable(boolean queryable) {
    this.queryable = queryable;
  }
  
  public boolean isQueryable() {
    return this.queryable;
  }
  
  public void setSearchable(boolean searchable) {
    this.searchable = searchable;
  }
  
  public boolean isSearchable() {
    return this.searchable;
  }
  
  public void setFieldPropertyMap(ArrayList<CloudFieldPropertyMap> fieldPropertyMap)
  {
    this.fieldPropertyMap = fieldPropertyMap;
  }
  
  public ArrayList<CloudFieldPropertyMap> getFieldPropertyMap() {
    return this.fieldPropertyMap;
  }
}