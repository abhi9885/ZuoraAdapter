/*    */ package com.cognizant.ipm.adapter.runtime.parser;
/*    */ 
/*    */ import java.util.HashMap;
/*    */ import java.util.Map;

/*    */ import javax.wsdl.Part;
/*    */ import javax.xml.namespace.QName;
/*    */ 
/*    */ public class WSDLRequestParameter
/*    */   implements RequestParameter
/*    */ {
/*    */   private MetadataNode parent;
/*    */   private QName qualifiedName;
/*    */   private Map<String, Object> nodeAttributes;
/*    */   private boolean array;
/*    */   private DataObjectReference dataObjectRef;
/*    */   
/*    */   public WSDLRequestParameter(String namespace, MetadataNode parent, Part part, ApplicationModel store)
/*    */   {
/* 19 */     String localName = part.getName();
/* 20 */     this.qualifiedName = new QName(namespace, localName);
/* 21 */     this.parent = parent;
/* 22 */     QName elementName = part.getElementName();
/* 23 */     this.dataObjectRef = store.getDataObjectReference(elementName);
/*    */   }
/*    */   
/*    */   public String getName() {
/* 27 */     return this.qualifiedName.getLocalPart();
/*    */   }
/*    */   
/*    */   public DataObjectNode getDataType() {
/* 31 */     return this.dataObjectRef.getDataObject();
/*    */   }
/*    */   
/*    */   public boolean isArray() {
/* 35 */     return this.array;
/*    */   }
/*    */   
/*    */   public MetadataNode getParent() {
/* 39 */     return this.parent;
/*    */   }
/*    */   
/*    */   public QName getQualifiedName() {
/* 43 */     return this.qualifiedName;
/*    */   }
/*    */   
/*    */   public String getDescription() {
/* 47 */     return null;
/*    */   }
/*    */   
/*    */   public Map<String, Object> getNodeAttributes() {
/* 51 */     if (this.nodeAttributes == null) {
/* 52 */       this.nodeAttributes = new HashMap();
/*    */     }
/* 54 */     return this.nodeAttributes;
/*    */   }
/*    */ }


/* Location:              C:\Oracle\Middleware\Oracle_Home\soa\soa\modules\oracle.cloud.adapter_12.1.3\zuora.jar!\oracle\cloud\connector\zuora\parser\WSDLRequestParameter.class
 * Java compiler version: 6 (50.0)
 * JD-Core Version:       0.7.1
 */