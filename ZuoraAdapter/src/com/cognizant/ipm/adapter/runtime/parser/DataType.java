package com.cognizant.ipm.adapter.runtime.parser;

public enum DataType {
  STRING("string"),  INT("int"),  LONG("long"),  SHORT("short"),  DATE("date"),  DATETIME("dateTime"), 
  BOOLEAN("boolean"),  INTEGER("integer"),  ID("ID"),  OBJECT("object"), 
  BYTE("byte"),  DOUBLE("double"),  QNAME("qname"),  FLOAT("float"), 
  DECIMAL("decimal"),  TIME("time");
  
  private final String dataType;
  
  private DataType(String val) {
    String dataTypeVal = val.toLowerCase();
    this.dataType = dataTypeVal;
  }
  
  public String getDataType() {
    return this.dataType;
  }
}