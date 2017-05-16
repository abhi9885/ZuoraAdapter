package com.cognizant.ipm.adapter.runtime.parser;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.wsdl.BindingInput;
import javax.wsdl.BindingOperation;
import javax.wsdl.Definition;
import javax.wsdl.Fault;
import javax.wsdl.Input;
import javax.wsdl.Message;
import javax.wsdl.Operation;
import javax.wsdl.OperationType;
import javax.wsdl.Part;
import javax.wsdl.extensions.soap.SOAPHeader;
import javax.wsdl.extensions.soap.SOAPOperation;
import javax.wsdl.extensions.soap12.SOAP12Header;
import javax.wsdl.extensions.soap12.SOAP12Operation;
import javax.xml.namespace.QName;

public class WSDLCloudOperationNode implements OperationNode
{
	private static final long serialVersionUID = 1L;
	private List<OperationFault> faults = new ArrayList<OperationFault>();
  private List<CloudHeader> requestHeaders = new ArrayList<CloudHeader>();
  private OperationResponse response;
  private QName qualifiedName;
  List<RequestParameter> operationParameters;
  private String name;
  private OperationNode.InvocationStyle invocationStyle;
  private String operationPath;
  
  public WSDLCloudOperationNode(String namespace, BindingOperation operation, ApplicationModel store, Definition definition)
  {
    this(namespace, operation.getOperation(), store, definition);
    processHeaders(operation, store, definition);
    processOperationPath(operation);
  }
  
  private void processOperationPath(BindingOperation operation) {
    List elements = operation.getExtensibilityElements();
    Iterator i$;
    if ((elements != null) && (elements.size() > 0)) {
      for (i$ = elements.iterator(); i$.hasNext();) {
        Object extensibilityElement = i$.next();
        if ((extensibilityElement instanceof SOAPOperation)) {
          SOAPOperation sop = (SOAPOperation)extensibilityElement;
          setOperationPath(sop.getSoapActionURI());
          break;
        }
        if ((extensibilityElement instanceof SOAP12Operation)) {
          SOAP12Operation sop12 = (SOAP12Operation)extensibilityElement;
          if (!sop12.getSoapActionRequired().booleanValue())
            break;
          setOperationPath(sop12.getSoapActionURI());
          break;
        }
      }
    }
  }
  
  public WSDLCloudOperationNode(String namespace, Operation operation, ApplicationModel store, Definition definition) {
    processFaults(operation, store);
    this.qualifiedName = new QName(namespace, operation.getName());
    this.name = operation.getName();
    processRequestParameters(operation, store);
    processInvocationStyle(operation);
    if (this.invocationStyle == OperationNode.InvocationStyle.REQUEST_RESPONSE) {
      this.response = new WSDLOperationResponse(this, operation, store, definition);
    }
  }
  
  private void processInvocationStyle(Operation wsdlOperation)
  {
    OperationType type = wsdlOperation.getStyle();
    if (type != null) {
      if (type.equals(OperationType.NOTIFICATION)) {
        this.invocationStyle = OperationNode.InvocationStyle.ASYNCHRONOUS;
      } else if (type.equals(OperationType.ONE_WAY)) {
        this.invocationStyle = OperationNode.InvocationStyle.ONEWAY;
      } else if (type.equals(OperationType.REQUEST_RESPONSE)) {
        this.invocationStyle = OperationNode.InvocationStyle.REQUEST_RESPONSE;
      } else if (type.equals(OperationType.SOLICIT_RESPONSE))
        this.invocationStyle = OperationNode.InvocationStyle.ASYNCHRONOUS;
    } else {
      this.invocationStyle = OperationNode.InvocationStyle.REQUEST_RESPONSE;
    }
  }
  
  private void processFaults(Operation wsdlOperation, ApplicationModel store) {
    if (wsdlOperation.getFaults() != null) {
      Iterator i$ = wsdlOperation.getFaults().values().iterator();
      while (i$.hasNext()) {
        Object o = i$.next();
        Fault fault = (Fault)o;
        OperationFault oFault = new WSDLOperationFault(fault, store, this);
        
        this.faults.add(oFault);
      }
    }
  }
  
  private void processHeaders(BindingOperation bindingOperation, ApplicationModel store, Definition definition) {
    BindingInput bindingInput = bindingOperation.getBindingInput();
    List extensibilityElements = bindingInput.getExtensibilityElements();
    Iterator i$;
    if ((extensibilityElements != null) && (extensibilityElements.size() > 0))
    {
      for (i$ = extensibilityElements.iterator(); i$.hasNext();) {
        Object o = i$.next();
        if ((o instanceof SOAPHeader)) {
          procesHeader((SOAPHeader)o, store, definition);
        } else if ((o instanceof SOAP12Header))
          processHeader((SOAP12Header)o, store, definition);
      }
    }
  }
  
  private void processHeader(SOAP12Header soapHeader, ApplicationModel store, Definition definition) {
    QName messageName = soapHeader.getMessage();
    Message message = definition.getMessage(messageName);
    String partName = soapHeader.getPart();
    createNewHeader(message, partName, store, soapHeader.getRequired().booleanValue());
  }
  

  private void procesHeader(SOAPHeader soapHeader, ApplicationModel store, Definition definition)
  {
    QName messageName = soapHeader.getMessage();
    Message message = definition.getMessage(messageName);
    String partName = soapHeader.getPart();
    createNewHeader(message, partName, store, soapHeader.getRequired().booleanValue());
  }
  

  private void createNewHeader(Message message, String partName, ApplicationModel store, boolean required)
  {
    Part part = message.getPart(partName);
    QName elementName = part.getElementName();
    if (elementName != null) {
      DataObjectReference objectReference = store.getDataObjectReference(elementName);
      
      CloudHeader cloudHeader = new CloudHeaderImpl(elementName.getNamespaceURI(), elementName.getLocalPart(), objectReference.getDataObject(), required);
      

      addRequestHeader(cloudHeader);
    }
  }
  
  private void processRequestParameters(Operation wsdlOperation, ApplicationModel store)
  {
    this.operationParameters = new ArrayList();
    Input input = wsdlOperation.getInput();
    Iterator i$;
    if (input != null) {
      Message message = input.getMessage();
      Map messageParts = message.getParts();
      if (messageParts != null)
        for (i$ = messageParts.values().iterator(); i$.hasNext();) {
          Object o = i$.next();
          Part part = (Part)o;
          RequestParameter operationParameter = new WSDLRequestParameter(this.qualifiedName.getNamespaceURI(), this, part, store);
          

          this.operationParameters.add(operationParameter);
        }
    }
  }
  
  public String getName() {
    return this.name;
  }
  
  public MetadataNode getParent() {
    return null;
  }
  
  public QName getQualifiedName() {
    return this.qualifiedName;
  }
  
  public String getDescription() {
    return null;
  }
  
  public Map<String, Object> getNodeAttributes() {
    return null;
  }
  
  public OperationNode.InvocationStyle getInvocationStyle() {
    return this.invocationStyle;
  }
  
  public List<OperationFault> getFaults() {
    return this.faults;
  }
  
  public OperationResponse getResponse() {
    return this.response;
  }
  
  public List<RequestParameter> getRequestParameters() {
    return this.operationParameters;
  }
  
  public String toString() {
    return getName();
  }
  
  public int hashCode() {
    return getName().hashCode();
  }
  
  public boolean equals(Object other) {
    if ((other instanceof OperationNode)) {
      return ((OperationNode)other).getName().equals(getName());
    }
    return false;
  }
  
  public List<CloudHeader> getRequestHeaders() {
    return this.requestHeaders;
  }
  
  public void addRequestHeader(CloudHeader cloudHeader) {
    this.requestHeaders.add(cloudHeader);
  }
  
  public void setName(String name) {
    this.name = name;
  }
  
  public String getOperationPath() {
    return this.operationPath;
  }
  
  public void setOperationPath(String path) {
    this.operationPath = path;
  }
}