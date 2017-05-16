/*    */ package com.cognizant.ipm.adapter.runtime.parser;
/*    */ 
/*    */ import java.util.HashMap;
/*    */ import java.util.Map;

/*    */ import javax.wsdl.Fault;
/*    */ import javax.wsdl.Message;
/*    */ import javax.wsdl.Part;
/*    */ import javax.xml.namespace.QName;
/*    */ 
/*    */ public class WSDLOperationFault implements OperationFault
/*    */ {
/*    */   private Fault fault;
/*    */   private QName qualifiedName;
/*    */   private OperationNode parent;
/*    */   private DataObjectReference dataObjectReference;
/* 16 */   private Map<String, Object> attributes = new HashMap();
/*    */   
/*    */   public WSDLOperationFault(Fault fault, ApplicationModel store, OperationNode parent)
/*    */   {
/* 20 */     this.fault = fault;
/* 21 */     this.qualifiedName = new QName(fault.getName());
/* 22 */     this.parent = parent;
/* 23 */     Message msg = fault.getMessage();
/* 24 */     if (msg != null) {
/* 25 */       Part part = (Part)msg.getParts().values().toArray()[0];
/* 26 */       this.attributes.put("elementWrapper", part.getElementName());
/* 27 */       this.dataObjectReference = store.getDataObjectReference(part.getElementName());
/*    */     }
/*    */   }
/*    */   
/*    */   public String getName()
/*    */   {
/* 33 */     return this.fault.getName();
/*    */   }
/*    */   
/*    */   public DataObjectNode getFaultDataObject() {
/* 37 */     return this.dataObjectReference.getDataObject();
/*    */   }
/*    */   
/*    */   public MetadataNode getParent() {
/* 41 */     return this.parent;
/*    */   }
/*    */   
/*    */   public QName getQualifiedName() {
/* 45 */     return this.qualifiedName;
/*    */   }
/*    */   
/*    */   public String getDescription() {
/* 49 */     return null;
/*    */   }
/*    */   
/*    */   public Map<String, Object> getNodeAttributes() {
/* 53 */     return this.attributes;
/*    */   }
/*    */ }


/* Location:              C:\Oracle\Middleware\Oracle_Home\soa\soa\modules\oracle.cloud.adapter_12.1.3\zuora.jar!\oracle\cloud\connector\zuora\parser\WSDLOperationFault.class
 * Java compiler version: 6 (50.0)
 * JD-Core Version:       0.7.1
 */