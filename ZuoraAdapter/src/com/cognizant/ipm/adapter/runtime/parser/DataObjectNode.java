package com.cognizant.ipm.adapter.runtime.parser;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.namespace.QName;

public class DataObjectNode implements MetadataNode
{
  private String name;
  private MetadataNode parent;
  private QName qualifiedName;
  private String description;
  private Map<String, Object> nodeAttributes;
  private ObjectCategory objectCategory;
  private Set<CloudField> cloudFields;
  private List<DataObjectNode> children;
  private DataType dataType;
  private ObjectGrouping fieldGrouping;
  private boolean anonymous;
  
  public DataObjectNode(MetadataNode parent, QName qualifiedName, ObjectCategory objectCategory)
  {
    this(parent, qualifiedName, objectCategory, DataType.OBJECT);
  }
  
  public DataObjectNode(MetadataNode parent, QName qualifiedName, ObjectCategory objectCategory, DataType dataType)
  {
    this(parent, qualifiedName, objectCategory, dataType, false);
  }
  
  public DataObjectNode(MetadataNode parent, QName qualifiedName, ObjectCategory objectCategory, DataType dataType, boolean anonymous)
  {
    this.parent = parent;
    this.qualifiedName = qualifiedName;
    this.name = qualifiedName.getLocalPart();
    this.objectCategory = objectCategory;
    this.dataType = dataType;
    this.anonymous = anonymous;
  }
  
  public DataObjectNode(QName qualifiedName) {
    this.qualifiedName = qualifiedName;
    this.name = qualifiedName.getLocalPart();
  }
  
  public String getName() {
    return this.name;
  }
  
  public MetadataNode getParent() {
    return this.parent;
  }
  
  public QName getQualifiedName() {
    return this.qualifiedName;
  }
  
  public String getDescription() {
    return this.description;
  }
  
  public void setDescription(String description) {
    this.description = description;
  }
  
  public Map<String, Object> getNodeAttributes() {
    if (this.nodeAttributes == null) {
      this.nodeAttributes = new HashMap();
    }
    return this.nodeAttributes;
  }
  
  public ObjectCategory getObjectCategory() {
    return this.objectCategory;
  }
  
  public Set<CloudField> getFields() {
    return getFields(false);
  }
  
  public List<DataObjectNode> getDescendants() {
    if (this.children == null)
      this.children = new ArrayList();
    return this.children;
  }
  
  public void addChild(DataObjectNode dataObject) {
    if (this.children == null)
      this.children = new ArrayList();
    this.children.add(dataObject);
  }
  
  public boolean equals(Object other) {
    if ((other instanceof DataObjectNode)) {
      return this.qualifiedName.equals(((DataObjectNode)other).getQualifiedName());
    }
    

    return false;
  }
  
  public int hashCode() {
    return this.qualifiedName.hashCode();
  }
  
  public String toString() {
    return this.name;
  }
  
  public DataType getDataType() {
    return this.dataType;
  }
  
  public ObjectGrouping getFieldGrouping() {
    return this.fieldGrouping;
  }
  
  public void setFieldGrouping(ObjectGrouping grouping) {
    this.fieldGrouping = grouping;
  }
  
  public void setDataType(DataType type) {
    this.dataType = type;
  }
  
  public boolean addField(CloudField cloudField) {
    if (this.cloudFields == null) {
      this.cloudFields = new LinkedHashSet();
    }
    return this.cloudFields.add(cloudField);
  }
  
  public Set<CloudField> getFields(boolean includeAllParents) {
    Set allFields = new LinkedHashSet();
    
    if (this.cloudFields != null) {
      allFields.addAll(this.cloudFields);
      if ((this.parent != null) && (includeAllParents)) {
        allFields.addAll(((DataObjectNode)this.parent).getFields(true));
      }
    }
    
    return Collections.unmodifiableSet(allFields);
  }
  
  public void removeField(CloudField cloudField) {
    if (this.cloudFields != null)
      this.cloudFields.remove(cloudField);
  }
  
  public boolean addFields(Collection<CloudField> fieldCollection) {
    if (this.cloudFields == null) {
      this.cloudFields = new LinkedHashSet();
    }
    return this.cloudFields.addAll(fieldCollection);
  }
  
  public boolean isAnonymous() {
    return this.anonymous;
  }
  
  public void setParent(MetadataNode parentNode) {
    this.parent = parentNode;
  }
  

  public CloudField getField(String name)
  {
    return getField(name, false);
  }
  
  public CloudField getField(String name, boolean includeParentFields) {
    Set<CloudField> allFields = getFields(includeParentFields);
    CloudField foundField = null;
    for (CloudField f : allFields) {
      if (f.getName().equals(name)) {
        foundField = f;
        break;
      }
    }
    return foundField;
  }
  
  public static enum ObjectCategory {
    CUSTOM,  STANDARD,  BUILTIN;
    
    private ObjectCategory() {}
  }
}