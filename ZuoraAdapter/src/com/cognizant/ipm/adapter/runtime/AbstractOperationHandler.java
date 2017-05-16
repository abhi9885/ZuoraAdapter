/*     */ package com.cognizant.ipm.adapter.runtime;
/*     */ 
/*     */ import java.util.Iterator;
/*     */ import java.util.Map;

/*     */ import javax.wsdl.Definition;
/*     */ import javax.wsdl.Operation;
/*     */ import javax.wsdl.Part;
/*     */ import javax.wsdl.PortType;
/*     */ import javax.xml.namespace.QName;

/*     */ import oracle.cloud.connector.api.CloudAdapterLoggingService;
/*     */ import oracle.cloud.connector.api.CloudInvocationContext;
/*     */ import oracle.cloud.connector.api.CloudInvocationException;
/*     */ import oracle.cloud.connector.impl.CloudAdapterUtil;

/*     */ import org.w3c.dom.Document;
/*     */ import org.w3c.dom.Element;
/*     */ import org.w3c.dom.Node;
/*     */ import org.w3c.dom.NodeList;
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ public abstract class AbstractOperationHandler
/*     */   implements OperationHandler
/*     */ {
/*     */   public void handleOperationRequest(CloudInvocationContext context, Document requestDocument, String version)
/*     */     throws CloudInvocationException
/*     */   {
/*  38 */     context.getLoggingService().logDebug("AbstractOperationHander#handleOperationRequest() invoked");
/*     */     
/*  40 */     if (context.getLoggingService().isLevel(CloudAdapterLoggingService.Level.DEBUG)) {
/*  41 */       context.getLoggingService().logDebug("Printing request document ====>");
/*     */       
/*  43 */       context.getLoggingService().logDebug(
/*  44 */         CloudAdapterUtil.getDomAsString(requestDocument, "UTF-8", true));
/*     */       
/*  46 */       context.getLoggingService().logDebug("<=====");
/*     */     }
/*     */     
/*  49 */     Element sourceRootElement = normalizeRootElement(context, requestDocument, version);
/*     */     
/*     */ 
/*     */ 
/*  53 */     requestDocument.normalizeDocument();
/*  54 */     String opName = sourceRootElement.getLocalName();
/*     */     
/*     */ 
/*  57 */     if (context.getLoggingService().isLevel(CloudAdapterLoggingService.Level.DEBUG)) {
/*  58 */       context.getLoggingService().logDebug("Printing transformred request document ====>");
/*     */       
/*  60 */       context.getLoggingService().logDebug(
/*  61 */         CloudAdapterUtil.getDomAsString(requestDocument, "UTF-8", true));
/*     */       
/*  63 */       context.getLoggingService().logDebug("<=====");
/*     */     }
/*     */   }
/*     */   
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */   protected Element normalizeRootElement(CloudInvocationContext context, Document requestDocument, String version)
/*     */   {
/*  76 */     Definition targetWSDL = (Definition)context.getContextObject("wsdlDefinition");
/*     */     
/*     */ 
/*  79 */     QName targetRootElementName = new QName(targetWSDL.getTargetNamespace(), context.getTargetOperationName());
/*  80 */     Element sourceRootElement = requestDocument.getDocumentElement();
/*     */     
/*  82 */     NodeList nodes = sourceRootElement.getChildNodes();
/*  83 */     Node node = null;
/*  84 */     for (int i = 0; i < nodes.getLength(); i++) {
/*  85 */       node = nodes.item(i);
/*  86 */       if ((node instanceof Element)) {
/*     */         break;
/*     */       }
/*     */     }
/*     */     
/*  91 */     sourceRootElement.getParentNode().replaceChild(node, sourceRootElement);
/*     */     
/*  93 */     sourceRootElement.normalize();
/*  94 */     return sourceRootElement;
/*     */   }
/*     */   
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */   public void handleOperationResponse(CloudInvocationContext context, Document responseDocument, String version)
/*     */     throws CloudInvocationException
/*     */   {
/* 111 */     context.getLoggingService().logDebug("AbstractOperationHander#handleOperationResponse() invoked");
/*     */     
/* 113 */     if (context.getLoggingService().isLevel(CloudAdapterLoggingService.Level.DEBUG)) {
/* 114 */       context.getLoggingService().logDebug("Printing response document ====>");
/*     */       
/* 116 */       context.getLoggingService().logDebug(
/* 117 */         CloudAdapterUtil.getDomAsString(responseDocument, "UTF-8", true));
/*     */       
/* 119 */       context.getLoggingService().logDebug("<=====");
/*     */     }
/*     */     
/* 122 */     String suppressResponse = (String)context.getCloudOperationProperties().get("suppressResponse");
/*     */     
/*     */ 
/* 125 */     boolean useVoid = suppressResponse != null ? Boolean.parseBoolean(suppressResponse) : false;
/*     */     
/* 127 */     context.getLoggingService().logDebug("AbstractOperationHander#handleOperationResponse suppressReponse => " + useVoid);
/*     */     
/*     */ 
/* 130 */     if (useVoid) {
/* 131 */       createVoidResponse(context, responseDocument, version);
/*     */     }
/*     */     else {
/* 134 */       adjustReponse(context, responseDocument, version);
/*     */     }
/* 136 */     if (context.getLoggingService().isLevel(CloudAdapterLoggingService.Level.DEBUG)) {
/* 137 */       context.getLoggingService().logDebug("Printing response document ====>");
/*     */       
/* 139 */       context.getLoggingService().logDebug(
/* 140 */         CloudAdapterUtil.getDomAsString(responseDocument, "UTF-8", true));
/*     */       
/* 142 */       context.getLoggingService().logDebug("<=====");
/*     */     }
/*     */   }
/*     */   
/*     */ 
/*     */   protected void createVoidResponse(CloudInvocationContext context, Document responseDocument, String version)
/*     */   {
/* 149 */     Element element = responseDocument.getDocumentElement();
/*     */     
/* 151 */     Node firstChild = element.getFirstChild();
/*     */     
/* 153 */     if (firstChild != null) {
/* 154 */       while (firstChild.getNextSibling() != null) {
/* 155 */         element.removeChild(firstChild.getNextSibling());
/*     */       }
/* 157 */       element.removeChild(firstChild);
/*     */     }
/* 159 */     renameResponseWrapper(context.getIntegrationWSDL(), element, responseDocument);
/*     */   }
/*     */   
/*     */ 
/*     */ 
/*     */   protected void renameResponseWrapper(Definition integrationWSDL, Element rootElement, Document sourceDoc)
/*     */   {
/* 166 */     Operation operation = getWSDLOperation(integrationWSDL);
/* 167 */     Part part = operation.getOutput().getMessage().getPart("parameters");
/* 168 */     QName newResponseQName = part.getElementName();
/*     */     
/*     */ 
/* 171 */     String newResponseName = newResponseQName.getPrefix() != null ? newResponseQName.getPrefix() + ":" + newResponseQName.getLocalPart() : newResponseQName.getLocalPart();
/* 172 */     sourceDoc.renameNode(rootElement, newResponseQName.getNamespaceURI(), newResponseName);
/*     */   }
/*     */   
/*     */ 
/*     */   private Operation getWSDLOperation(Definition integrationWSDL)
/*     */   {
/* 178 */     Map portTypes = integrationWSDL.getPortTypes();
/* 179 */     Iterator localIterator = portTypes.keySet().iterator(); if (localIterator.hasNext()) { Object key = localIterator.next();
/* 180 */       PortType pt = (PortType)portTypes.get(key);
/* 181 */       return (Operation)pt.getOperations().get(0);
/*     */     }
/* 183 */     return null;
/*     */   }
/*     */   
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */   protected void adjustReponse(CloudInvocationContext context, Document responseDocument, String version)
/*     */     throws CloudInvocationException
/*     */   {
/* 199 */     Element rootElement = responseDocument.getDocumentElement();
/*     */     
/*     */ 
/*     */ 
/* 203 */     Definition targetWSDL = (Definition)context.getContextObject("wsdlDefinition");
/*     */     
/*     */ 
/* 206 */     QName targetOperationElementName = new QName("http://example.oracle.com/cloud/sample/SampleAccountService.wsdl/types", context.getTargetOperationName());
/*     */     
/* 208 */     Element element = responseDocument.getDocumentElement();
/*     */     
/* 210 */     renameResponseWrapper(context.getIntegrationWSDL(), element, responseDocument, targetOperationElementName, context);
/*     */     
/* 212 */     element = responseDocument.getDocumentElement();
/*     */     
/* 214 */     String namespace = element.getNamespaceURI();
/*     */   }
/*     */   
/*     */ 
/*     */ 
/*     */   protected void renameResponseWrapper(Definition integrationWSDL, Element rootElement, Document sourceDoc, QName targetOperationElementName, CloudInvocationContext context)
/*     */   {
/* 221 */     Operation operation = getWSDLOperation(integrationWSDL);
/* 222 */     Part part = operation.getOutput().getMessage().getPart("parameters");
/* 223 */     QName newResponseQName = part.getElementName();
/*     */     
/*     */ 
/* 226 */     String newResponseName = newResponseQName.getPrefix() != null ? newResponseQName.getPrefix() + ":" + newResponseQName.getLocalPart() : newResponseQName.getLocalPart();
/* 227 */     sourceDoc.renameNode(rootElement, newResponseQName.getNamespaceURI(), newResponseName);
/*     */     
/* 229 */     NodeList nodes = sourceDoc.getDocumentElement().getChildNodes();
/* 230 */     Node resultNode = null;
/* 231 */     for (int i = 0; i < nodes.getLength(); i++) {
/* 232 */       resultNode = nodes.item(i);
/* 233 */       if ((resultNode instanceof Element)) {
/*     */         break;
/*     */       }
/*     */     }
/*     */     
/* 238 */     NodeList resultChildNodes = resultNode.getChildNodes();
/* 239 */     Node valueNode = null;
/* 240 */     for (int i = 0; i < resultChildNodes.getLength(); i++) {
/* 241 */       valueNode = resultChildNodes.item(i);
/* 242 */       if ((resultNode instanceof Element)) {
/*     */         break;
/*     */       }
/*     */     }
/*     */     
/*     */ 
/* 248 */     String prefix = context.getNamespaceManager().getOrCreatePrefix(targetOperationElementName.getNamespaceURI());
/*     */     
/* 250 */     sourceDoc.renameNode(valueNode, targetOperationElementName.getNamespaceURI(), prefix + ":" + "account");
/*     */     
/* 252 */     sourceDoc.renameNode(resultNode, targetOperationElementName.getNamespaceURI(), prefix + ":" + targetOperationElementName.getLocalPart());
/*     */   }
/*     */   
/*     */   public void handleOperationError(CloudInvocationContext context, Document errorDocument, String version)
/*     */     throws CloudInvocationException
/*     */   {}
/*     */ }


/* Location:              U:\Cognizant\Adapter\SugarCRMAdapter.jar!\com\cognizant\ipm\adapter\runtime\AbstractOperationHandler.class
 * Java compiler version: 8 (52.0)
 * JD-Core Version:       0.7.1
 */