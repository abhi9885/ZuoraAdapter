package com.cognizant.ipm.adapter.runtime.parser;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import javax.wsdl.Definition;
import javax.xml.namespace.QName;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import oracle.cloud.connector.api.CloudAdapterLoggingService;
import oracle.j2ee.ws.wsdl.extensions.oracle.schema.SchemaCollection;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class JsonXmlTransformation
{
  private Map<String, Object> contextObjects = new HashMap();
  private SchemaCollection schemaCollection = new SchemaCollection();
  private DocumentBuilder docBuilder = null;
  private Document doc = null;
  private DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
  private List<CloudOperationMapping> mappings;
  private CloudOperationMapping mapping = null;
  CloudAdapterLoggingService m_logger = null;
  
  public JsonXmlTransformation(CloudAdapterLoggingService m_logger) {
    this.m_logger = m_logger;
  }
  
  public Document cachXMLDOC(Definition definition) throws Exception {
    WSDLMetadataParser parser = new WSDLMetadataParser(this.contextObjects);
    ApplicationModel model = parser.parse(definition, this.m_logger);
    this.schemaCollection = ((SchemaCollection)this.contextObjects.get("schemaCollection"));
    processOperationMappings(model);
    this.m_logger.logDebug("processOperationMappings DONE!!");
    try
    {
      this.docBuilder = this.docFactory.newDocumentBuilder();
      this.doc = this.docBuilder.newDocument();
    }
    catch (Exception e) {
      this.m_logger.logError("Exception caught in cachXMLDoc() : " + e.getMessage(), e);
      throw e;
    }
    Set<CloudField> cloudFields = null;
    Element rootElement = null;
    String ns = definition.getTargetNamespace();
    QName responseElementName = null;
    if (((OperationNode)model.getOperations().get(0)).getInvocationStyle() == OperationNode.InvocationStyle.REQUEST_RESPONSE)
    {
      OperationResponse response = this.mapping.getTargetOperation().getResponse();
      responseElementName = (QName)response.getNodeAttributes().get("elementWrapper");
      DataObjectReference ref = model.getDataObjectReference(responseElementName);
      cloudFields = ref.getDataObject().getFields();
      rootElement = this.doc.createElementNS(responseElementName.getNamespaceURI(), responseElementName.getLocalPart());
    }
    else if (((OperationNode)model.getOperations().get(0)).getInvocationStyle() == OperationNode.InvocationStyle.ONEWAY)
    {
      List<RequestParameter> request = this.mapping.getTargetOperation().getRequestParameters();
      responseElementName = ((RequestParameter)request.get(0)).getParent().getQualifiedName();
      DataObjectNode dataObject = ((RequestParameter)request.get(0)).getDataType();
      cloudFields = dataObject.getFields();
      rootElement = this.doc.createElementNS(responseElementName.getNamespaceURI(), responseElementName.getLocalPart());
    }
    this.doc.appendChild(rootElement);
    for (CloudField cloudField : cloudFields)
    {
      DataObjectNode fieldTypeNode = cloudField.getFieldType();
      if (!fieldTypeNode.getObjectCategory().equals(DataObjectNode.ObjectCategory.STANDARD)) {}
      Element childEle = this.doc.createElementNS(cloudField.getFieldType().getQualifiedName().getNamespaceURI(), cloudField.getName());
      rootElement.appendChild(childEle);
      Set<CloudField> fieldChildTypeNode = fieldTypeNode.getFields();
      addFieldsRecursively(fieldChildTypeNode, childEle, this.doc, ns);
    }
    return this.doc;
  }

  public void addFieldsRecursively(Set<CloudField> cloudFields, Element ele, Document doc, String ns)
  {
    for (CloudField cloudField : cloudFields)
    {
      DataObjectNode fieldTypeNode = cloudField.getFieldType();
      if (!fieldTypeNode.getObjectCategory().equals(DataObjectNode.ObjectCategory.BUILTIN)) {}
      Element rootElement = doc.createElementNS(ns, cloudField.getName());
      ele.appendChild(rootElement);
      Set<CloudField> fieldChildTypeNode = fieldTypeNode.getFields();
      
      if (!fieldChildTypeNode.isEmpty())
      {
        if (cloudField.getName().equalsIgnoreCase(fieldTypeNode.getName()))
        {
          addFieldsRecursively(fieldChildTypeNode, rootElement, doc, ns);
        }
        else
        {
          Element childrootElement = doc.createElementNS(ns, fieldTypeNode.getName());
          rootElement.appendChild(childrootElement);
          addFieldsRecursively(fieldChildTypeNode, childrootElement, doc, ns);
        }
      }
    }
  }

  private void processOperationMappings(ApplicationModel model)
  {
    Properties props = getInteractionProperties();
    OperationNode operation = (OperationNode)model.getOperations().get(0);
    this.mapping = new CloudOperationMapping(operation);
    ObjectGrouping requestGrouping = getRequestGrouping(operation, this.m_logger);
    this.mapping.setDefaultRequestObjectGrouping(requestGrouping);
    ObjectGrouping responseGrouping = getResponseGrouping(operation, this.m_logger);
    this.mapping.setDefaultResponseObjectGrouping(responseGrouping);
    List requestTypeMappings = getRequestTypeMappings(operation, this.m_logger);
    this.mapping.setRequestObjectMappings(requestTypeMappings);
    List responseTypeMappings = getResponseTypeMappings(operation, this.m_logger);
    this.mapping.setResponseObjectMapping(responseTypeMappings);
    this.mapping.setOperationProperties(props);
  }
  
  private static Properties getInteractionProperties()
  {
    Properties properties = new Properties();
    properties.put("name", "value");
    return properties;
  }
  
  public void addOperationMapping(CloudOperationMapping mapping) {
    this.mappings.add(mapping);
  }
  

  private static ObjectGrouping getRequestGrouping(OperationNode operation, CloudAdapterLoggingService m_logger)
  {
    List<RequestParameter> params = operation.getRequestParameters();
    if ((params != null) && (params.size() > 0)) {
      DataObjectNode dataObject = null;
      try {
        dataObject = ((RequestParameter)params.get(0)).getDataType();
      } catch (NullPointerException e) {
        m_logger.logDebug("NPException caught in getRequestGrouping method:" + e.getMessage(), e);
      }

      if (dataObject != null) {
        return dataObject.getFieldGrouping();
      }
    }
    return ObjectGrouping.ORDERED;
  }
  
  private static ObjectGrouping getResponseGrouping(OperationNode operation, CloudAdapterLoggingService m_logger)
  {
    OperationResponse response = operation.getResponse();
    if (response != null) {
      DataObjectNode dataObject = null;
      try
      {
        dataObject = response.getResponseObject();
      } catch (NullPointerException e) {
        m_logger.logDebug("NPException caught in getRequestGrouping method:" + e.getMessage(), e);
      }

      if (dataObject != null) {
        return dataObject.getFieldGrouping();
      }
    }
    return ObjectGrouping.ORDERED;
  }
  
  private static List<TypeMapping> getResponseTypeMappings(OperationNode operation, CloudAdapterLoggingService m_logger)
  {
    OperationResponse response = operation.getResponse();
    List typeMappings = new ArrayList();
    if (response != null) {
      DataObjectNode dataObjectNode = response.getResponseObject();
      if (dataObjectNode != null) {
        processTypeMappings(typeMappings, dataObjectNode, m_logger);
      }
    }
    return typeMappings;
  }
  
  private static List<TypeMapping> getRequestTypeMappings(OperationNode operation, CloudAdapterLoggingService m_logger)
  {
    List params = operation.getRequestParameters();
    List typeMappings = new ArrayList();
    if ((params != null) && (params.size() > 0)) {
      DataObjectNode dataObject = ((RequestParameter)params.get(0)).getDataType();
      processTypeMappings(typeMappings, dataObject, m_logger);
    }
    return typeMappings;
  }
  
  private static void processTypeMappings(List<TypeMapping> typeMappings, DataObjectNode dataObject, CloudAdapterLoggingService m_logger)
  {
    for (CloudField cloudField : dataObject.getFields()) {
      TypeMapping typeMapping = new TypeMapping(cloudField.getFieldType(), cloudField.isArray(), !cloudField.isRequired());
      typeMappings.add(typeMapping);
    }
  }
  
  public Document getDoc()
  {
    return this.doc;
  }
  
  public void setDoc(Document docu) {
    this.doc = docu;
  }
}