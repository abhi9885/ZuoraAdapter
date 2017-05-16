package com.cognizant.ipm.adapter.query;

import java.util.ArrayList;

public class CloudFieldPropertyMap
{
  private String property;
  private ArrayList<String> fieldList = new ArrayList<String>();
  
  public CloudFieldPropertyMap(String property, ArrayList<String> fieldList) {
    this.property = property;
    this.fieldList.addAll(fieldList);
  }
  
  public String getPropertyName() {
    return this.property;
  }
  
  public ArrayList<String> getFieldList() {
    return this.fieldList;
  }
}