/*     */ package com.cognizant.ipm.adapter.runtime.parser;
/*     */ 
/*     */ import java.util.ArrayList;
/*     */ import java.util.HashMap;
/*     */ import java.util.Iterator;
/*     */ import java.util.List;
/*     */ import java.util.Map;

/*     */ import javax.wsdl.BindingOperation;
/*     */ import javax.wsdl.BindingOutput;
/*     */ import javax.wsdl.Definition;
/*     */ import javax.wsdl.Message;
/*     */ import javax.wsdl.Operation;
/*     */ import javax.wsdl.Output;
/*     */ import javax.wsdl.Part;
/*     */ import javax.wsdl.extensions.soap.SOAPHeader;
/*     */ import javax.wsdl.extensions.soap12.SOAP12Header;
/*     */ import javax.xml.namespace.QName;
/*     */ 
/*     */ public class WSDLOperationResponse implements OperationResponse
/*     */ {
/*     */   private OperationNode parent;
/*     */   private QName qualifiedName;
/*     */   private DataObjectReference objReference;
/*  24 */   private Map<String, Object> nodeAttributes = new HashMap();
/*     */   
/*  26 */   private List<CloudHeader> responseHeaders = new ArrayList();
/*     */   
/*     */ 
/*     */   public WSDLOperationResponse(OperationNode parent, BindingOperation operation, ApplicationModel store, Definition definition)
/*     */   {
/*  31 */     this(parent, operation.getOperation(), store, definition);
/*     */   }
/*     */   
/*     */   public WSDLOperationResponse(OperationNode parent, Operation operation, ApplicationModel store, Definition definition)
/*     */   {
/*  36 */     Output output = operation.getOutput();
/*  37 */     this.parent = parent;
/*  38 */     Message message = output.getMessage();
/*  39 */     String name = "";
/*  40 */     if (message != null) {
/*  41 */       name = message.getQName().getLocalPart();
/*     */     }
/*  43 */     this.qualifiedName = new QName(null, name);
/*  44 */     Object partObj = output.getMessage().getParts().values().toArray()[0];
/*  45 */     Part part = (Part)partObj;
/*  46 */     QName refQName = part.getElementName();
/*  47 */     this.objReference = store.getDataObjectReference(refQName);
/*  48 */     this.nodeAttributes.put("elementWrapper", refQName);
/*     */   }
/*     */   
/*     */   public String getName() {
/*  52 */     return this.qualifiedName.getLocalPart();
/*     */   }
/*     */   
/*     */   public DataObjectNode getResponseObject() {
/*  56 */     return this.objReference.getDataObject();
/*     */   }
/*     */   
/*     */   public MetadataNode getParent() {
/*  60 */     return this.parent;
/*     */   }
/*     */   
/*     */   public QName getQualifiedName() {
/*  64 */     return this.qualifiedName;
/*     */   }
/*     */   
/*     */   public String getDescription() {
/*  68 */     return null;
/*     */   }
/*     */   
/*     */   public Map<String, Object> getNodeAttributes() {
/*  72 */     return this.nodeAttributes;
/*     */   }
/*     */   
/*     */   public List<CloudHeader> getResponseHeaders() {
/*  76 */     return this.responseHeaders;
/*     */   }
/*     */   
/*     */   public void addHeader(CloudHeader cloudHeader) {
/*  80 */     this.responseHeaders.add(cloudHeader);
/*     */   }
/*     */   
/*     */   private void processHeaders(BindingOperation bindingOperation, ApplicationModel store, Definition definition)
/*     */   {
/*  85 */     BindingOutput bindingOutput = bindingOperation.getBindingOutput();
/*  86 */     List extensibilityElements = bindingOutput.getExtensibilityElements();
/*     */     Iterator i$;
/*  88 */     if ((extensibilityElements != null) && (extensibilityElements.size() > 0))
/*     */     {
/*  90 */       for (i$ = extensibilityElements.iterator(); i$.hasNext();) {
/*  91 */         Object o = i$.next();
/*  92 */         if ((o instanceof SOAPHeader)) {
/*  93 */           procesHeader((SOAPHeader)o, store, definition);
/*  94 */         } else if ((o instanceof SOAP12Header))
/*  95 */           processHeader((SOAP12Header)o, store, definition);
/*     */       }
/*     */     }
/*     */   }
/*     */   
/*     */   private void processHeader(SOAP12Header soapHeader, ApplicationModel store, Definition definition) {
/* 101 */     QName messageName = soapHeader.getMessage();
/* 102 */     Message message = definition.getMessage(messageName);
/* 103 */     String partName = soapHeader.getPart();
/* 104 */     createNewHeader(message, partName, store, soapHeader.getRequired().booleanValue());
/*     */   }
/*     */   
/*     */ 
/*     */   private void procesHeader(SOAPHeader soapHeader, ApplicationModel store, Definition definition)
/*     */   {
/* 110 */     QName messageName = soapHeader.getMessage();
/* 111 */     Message message = definition.getMessage(messageName);
/* 112 */     String partName = soapHeader.getPart();
/* 113 */     createNewHeader(message, partName, store, soapHeader.getRequired().booleanValue());
/*     */   }
/*     */   
/*     */ 
/*     */   private void createNewHeader(Message message, String partName, ApplicationModel store, boolean required)
/*     */   {
/* 119 */     Part part = message.getPart(partName);
/* 120 */     QName elementName = part.getElementName();
/* 121 */     if (elementName != null) {
/* 122 */       DataObjectReference objectReference = store.getDataObjectReference(elementName);
/*     */       
/* 124 */       CloudHeader cloudHeader = new CloudHeaderImpl(elementName.getNamespaceURI(), elementName.getLocalPart(), objectReference.getDataObject(), required);
/*     */       
/*     */ 
/* 127 */       addHeader(cloudHeader);
/*     */     }
/*     */   }
/*     */ }


/* Location:              C:\Oracle\Middleware\Oracle_Home\soa\soa\modules\oracle.cloud.adapter_12.1.3\zuora.jar!\oracle\cloud\connector\zuora\parser\WSDLOperationResponse.class
 * Java compiler version: 6 (50.0)
 * JD-Core Version:       0.7.1
 */