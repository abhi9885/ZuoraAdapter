package com.cognizant.ipm.adapter.runtime.parser;

import java.util.Map;

import javax.xml.namespace.QName;

public class DataObjectReference implements MetadataNode
{
  private QName qname;
  private DataObjectNode referencedObject;
  
  public DataObjectReference(QName qname, DataObjectNode referencedObject)
  {
    this.qname = qname;
    this.referencedObject = referencedObject;
  }
  
  public String getName() {
    return this.qname.getLocalPart();
  }
  
  public MetadataNode getParent() {
    return null;
  }
  
  public QName getQualifiedName() {
    return this.qname;
  }
  
  public String getDescription() {
    return null;
  }
  
  public Map<String, Object> getNodeAttributes() {
    return null;
  }
  
  public DataObjectNode getDataObject() {
    return this.referencedObject;
  }
}