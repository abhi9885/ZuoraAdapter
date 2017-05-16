package com.cognizant.ipm.adapter.runtime.parser;
 
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
 
 public class TypeMask
 {
   private Set<CloudField> excludedFields = new java.util.HashSet();
   
   private Set<CloudField> includedFields = null;
   
   private Set<CloudField> prePendedFields = null;
   
   private Set<CloudField> appendedFields = null;
   
   private Map<CloudField, DataObjectNode> fieldTypeMappings = new HashMap();
   private String namespaceOverride;
   
   public Set<CloudField> getExcludedFields() {
     return this.excludedFields;
   }
   
   public void setExcludedFields(Set<CloudField> excludedFields) {
     this.excludedFields = excludedFields;
   }
   
   public Set<CloudField> getIncludedFields() {
     return this.includedFields;
   }
   
   public void setIncludedFields(Set<CloudField> includedFields) {
     this.includedFields = includedFields;
   }
   
   public Map<CloudField, DataObjectNode> getFieldTypeMappings() {
     return this.fieldTypeMappings;
   }
   
   public String getNamespaceOverride() {
     return this.namespaceOverride;
   }
   
   public void setNamespaceOverride(String namespace) {
     this.namespaceOverride = namespace;
   }
   
   public Set<CloudField> getPrependedFields() {
     return this.prePendedFields;
   }
   
   public void setPrependedFields(Set<CloudField> prePendedFields) {
     this.prePendedFields = prePendedFields;
   }
   
   public Set<CloudField> getAppendedFields() {
     return this.appendedFields;
   }
   
   public void setAppendedFields(Set<CloudField> appendedFields) {
     this.appendedFields = appendedFields;
   }
 }