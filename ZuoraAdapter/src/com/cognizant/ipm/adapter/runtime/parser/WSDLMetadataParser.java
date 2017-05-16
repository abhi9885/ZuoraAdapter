package com.cognizant.ipm.adapter.runtime.parser;

import java.io.InputStream;
import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import javax.wsdl.Definition;
import javax.wsdl.Operation;
import javax.wsdl.PortType;
import javax.wsdl.WSDLException;
import javax.wsdl.factory.WSDLFactory;
import javax.wsdl.xml.WSDLReader;
import javax.xml.namespace.QName;

import oracle.cloud.connector.api.CloudAdapterLoggingService;
import oracle.j2ee.ws.wsdl.extensions.oracle.schema.AllSchemaElement;
import oracle.j2ee.ws.wsdl.extensions.oracle.schema.AttributeSchemaElement;
import oracle.j2ee.ws.wsdl.extensions.oracle.schema.BaseSchemaElement;
import oracle.j2ee.ws.wsdl.extensions.oracle.schema.ChoiceSchemaElement;
import oracle.j2ee.ws.wsdl.extensions.oracle.schema.ComplexContentSchemaElement;
import oracle.j2ee.ws.wsdl.extensions.oracle.schema.ComplexTypeSchemaElement;
import oracle.j2ee.ws.wsdl.extensions.oracle.schema.ElementSchemaElement;
import oracle.j2ee.ws.wsdl.extensions.oracle.schema.ExtensionSchemaElement;
import oracle.j2ee.ws.wsdl.extensions.oracle.schema.SchemaCollection;
import oracle.j2ee.ws.wsdl.extensions.oracle.schema.SchemaSchemaElement;
import oracle.j2ee.ws.wsdl.extensions.oracle.schema.SchemaWalker;
import oracle.j2ee.ws.wsdl.extensions.oracle.schema.SequenceSchemaElement;
import oracle.j2ee.ws.wsdl.extensions.oracle.schema.SimpleTypeSchemaElement;
import oracle.j2ee.ws.wsdl.extensions.oracle.schema.TypeSchemaElement;
import oracle.j2ee.ws.wsdl.extensions.schema.SchemaImpl;
import oracle.j2ee.ws.wsdl.xml.WSDLReaderImpl;
import oracle.webservices.ConnectionConfig;
import oracle.webservices.ConnectionConfigBean;

import org.xml.sax.InputSource;

public class WSDLMetadataParser
{
  private static Map<String, Object> pluginContext = new HashMap();
  private static Set<String> supportedMediaTypes;
  private static String defaultUniquePrefix = "ComplexType1";
  private Set<String> uniqueNames = new HashSet();
  
  static {
    supportedMediaTypes = new HashSet();
    supportedMediaTypes.add("application/wsdl+xml");
  }

  public WSDLMetadataParser(Map<String, Object> contextObjects) {
    pluginContext = contextObjects;
  }
  
  public ApplicationModel parse(Definition def, CloudAdapterLoggingService m_logger)
  {
    ApplicationModel model = new ApplicationModel();
    parse(def, model, m_logger);
    return model;
  }
  
  public void parse(Definition definition, ApplicationModel model, CloudAdapterLoggingService m_logger)
  {
    processDataObjectNodes(model, definition, m_logger);
    processOperations(model, definition, m_logger);
  }

  private void processOperations(ApplicationModel model, Definition definition, CloudAdapterLoggingService m_logger)
  {
    Map portTypes = definition.getPortTypes();
    PortType portType = null;
    Iterator portitr = portTypes.keySet().iterator();
    if (portitr.hasNext()) {
      Object key = portitr.next();
      portType = (PortType)portTypes.get(key);
    }
    Iterator op2;
    if (portType != null) {
      List operations = portType.getOperations();
      for (op2 = operations.iterator(); op2.hasNext();) {
        Object op = op2.next();
        Operation operation = (Operation)op;
        OperationNode operationNode = new WSDLCloudOperationNode(definition.getTargetNamespace(), operation, model, definition);
        model.addOperation(operationNode);
      }
    }
  }

  private void processDataObjectNodes(ApplicationModel model, Definition definition, CloudAdapterLoggingService m_logger)
  {
    List typesExtensions = definition.getTypes().getExtensibilityElements();
    m_logger.logDebug("element type:" + typesExtensions.getClass().getSimpleName());
    
    SchemaCollection schemaCollection = new SchemaCollection();
    for (Iterator itr = typesExtensions.iterator(); itr.hasNext();) {
      Object obj = itr.next();
      if ((obj instanceof SchemaSchemaElement))
      {
        SchemaSchemaElement se = SchemaWalker.walkSchema(((SchemaSchemaElement)obj).getDOMElement());
        schemaCollection.addSchemaElement(se);
      }
      else if ((obj instanceof SchemaImpl))
      {
        SchemaSchemaElement se = SchemaWalker.walkSchema(((SchemaImpl)obj).getElement());
        schemaCollection.addSchemaElement(se);
      }
    }
    if ((pluginContext != null) && (schemaCollection != null)) {
      pluginContext.put("schemaCollection", schemaCollection);
    }
    Map globalNSMap = definition.getNamespaces();
    processDataObjectNodes(model, schemaCollection, globalNSMap, m_logger);
  }

  private void processDataObjectNodes(ApplicationModel model, SchemaCollection schemaCollection, Map globalNSMap, CloudAdapterLoggingService m_logger)
  {
    Iterator shemaElements = schemaCollection.getAllSchemaElements();
    while (shemaElements.hasNext()) {
      SchemaSchemaElement sse = (SchemaSchemaElement)shemaElements.next();
      Iterator childElements = sse.getChildElements();
      while (childElements.hasNext()) {
        Object childElement = childElements.next();
        if ((childElement instanceof SimpleTypeSchemaElement)) {
          SimpleTypeSchemaElement stse = (SimpleTypeSchemaElement)childElement;
          processSchemaSimpleType(model, stse, stse.getSchemaElement().getTargetNamespaceURI());
        }
        else if ((childElement instanceof ComplexTypeSchemaElement)) {
          ComplexTypeSchemaElement ctse = (ComplexTypeSchemaElement)childElement;
          processComplexType(model, ctse, schemaCollection, globalNSMap);
        }
        else if ((childElement instanceof ElementSchemaElement)) {
          ElementSchemaElement element = (ElementSchemaElement)childElement;
          processElement(model, element, element.getSchemaElement(), schemaCollection, globalNSMap);
        }
      }
    }
  }

  private void processElement(ApplicationModel model, ElementSchemaElement element, SchemaSchemaElement se, SchemaCollection collection, Map globalNSMap)
  {
    DataObjectNode dataObject = null;
    QName typeName = element.getTypeAsQName();
    if (typeName == null) {
      dataObject = processAnonymousComplexType(model, element, collection, globalNSMap);
    }
    else {
      dataObject = model.findDataObject(typeName);
    }
    if (dataObject == null) {
      BaseSchemaElement bse = se.findType(typeName);
      if ((bse instanceof ComplexTypeSchemaElement)) {
        dataObject = processComplexType(model, (ComplexTypeSchemaElement)bse, collection, globalNSMap);
      }
    }
    DataObjectReference ref = new DataObjectReference(new QName(se.getTargetNamespaceURI(), element.getName()), dataObject);
    model.addDataObjectReference(ref);
  }
  

  private DataObjectNode processAnonymousComplexType(ApplicationModel model, ElementSchemaElement element, SchemaCollection collection, Map globalNSMap)
  {
    Iterator childElements = element.getChildElements();
    DataObjectNode dataObject = null;
    while (childElements.hasNext()) {
      BaseSchemaElement child = (BaseSchemaElement)childElements.next();
      if ((child instanceof ComplexTypeSchemaElement)) {
        dataObject = processComplexType(model, (ComplexTypeSchemaElement)child, collection, globalNSMap);
      }
    }
    return dataObject;
  }
  

  private DataObjectNode processComplexType(ApplicationModel model, ComplexTypeSchemaElement ctse, SchemaCollection collection, Map globalNSMap)
  {
    String namespace = ctse.getSchemaElement().getTargetNamespaceURI();
    boolean anonymous = false;
    QName typeName = resolveSchemaTypeName(ctse, namespace);
    if (typeName == null) {
      String uniqueName = null;
      BaseSchemaElement parentElement = ctse.getParent();
      if ((parentElement != null) && ((parentElement instanceof ElementSchemaElement)))
      {
        ElementSchemaElement parentSchemaElement = (ElementSchemaElement)parentElement;
        uniqueName = parentSchemaElement.getName();
      }
      if (uniqueName == null) {
        uniqueName = getUniqueName(ctse.getSchemaElement().getElementLocalName());
      }
      
      typeName = new QName(namespace, uniqueName);
      anonymous = true;
    }
    DataObjectNode dataTypeNode = model.findDataObject(typeName);
    if (dataTypeNode != null) {
      return dataTypeNode;
    }
    DataObjectNode parent = null;
    parent = getOrProcessParent(model, ctse, collection, globalNSMap);
    dataTypeNode = new DataObjectNode(parent, typeName, DataObjectNode.ObjectCategory.STANDARD, DataType.OBJECT, anonymous);
    model.addDataObject(dataTypeNode);
    if (parent != null) {
      parent.getDescendants().add(dataTypeNode);
    }
    processDataObjectFields(model, dataTypeNode, ctse, collection, globalNSMap);
    
    return dataTypeNode;
  }
  
  private String getUniqueName(String prefix) {
    int suffix = 1;
    String uniqueName = prefix;
    if (uniqueName == null) {
      uniqueName = defaultUniquePrefix;
    }
    while (this.uniqueNames.contains(uniqueName)) {
      uniqueName = uniqueName + suffix;
      suffix++;
    }
    this.uniqueNames.add(uniqueName);
    return uniqueName;
  }
  

  private void processDataObjectFields(ApplicationModel model, DataObjectNode dataTypeNode, BaseSchemaElement ctse, SchemaCollection collection, Map globalNSMap)
  {
    Iterator it = ctse.getChildElements();
    while (it.hasNext()) {
      BaseSchemaElement bse = (BaseSchemaElement)it.next();
      if ((bse instanceof ComplexContentSchemaElement)) {
        processDataObjectFields(model, dataTypeNode, bse, collection, globalNSMap);
      }
      else if ((bse instanceof ExtensionSchemaElement)) {
        processDataObjectFields(model, dataTypeNode, bse, collection, globalNSMap);
      }
      else if ((bse instanceof SequenceSchemaElement)) {
        dataTypeNode.setFieldGrouping(ObjectGrouping.ORDERED);
        processDataObjectFields(model, dataTypeNode, bse, collection, globalNSMap);
      }
      else if ((bse instanceof ChoiceSchemaElement)) {
        if (dataTypeNode.getFieldGrouping() == ObjectGrouping.ORDERED) {
          dataTypeNode.setFieldGrouping(ObjectGrouping.ORDERED_CHOICE);
        }
        else if (dataTypeNode.getFieldGrouping() == ObjectGrouping.UNORDERED) {
          dataTypeNode.setFieldGrouping(ObjectGrouping.UNORDERED_CHOICE);
        }
        else {
          dataTypeNode.setFieldGrouping(ObjectGrouping.CHOICE);
        }
        processDataObjectFields(model, dataTypeNode, bse, collection, globalNSMap);
      }
      else if ((bse instanceof AllSchemaElement)) {
        dataTypeNode.setFieldGrouping(ObjectGrouping.UNORDERED);
      } else if ((bse instanceof ElementSchemaElement)) {
        ElementSchemaElement ese = (ElementSchemaElement)bse;
        createFieldFromElement(model, dataTypeNode, ese, collection, globalNSMap);
      }
      else if ((bse instanceof AttributeSchemaElement)) {
        AttributeSchemaElement ase = (AttributeSchemaElement)bse;
        createFiedFromAttribute(model, dataTypeNode, ase, collection, globalNSMap);
      }
    }
  }

  private void createFiedFromAttribute(ApplicationModel model, DataObjectNode dataTypeNode, AttributeSchemaElement ase, SchemaCollection collection, Map globalNSMap)
  {
    String name = ase.getName();
    QName type = ase.getAttributeValueAsQNameOrNull("type");
    DataObjectNode referencedObject = null;
    if (type == null) {
      referencedObject = BuiltInTypes.STRING;
      type = BuiltInTypes.STRING_TYPE;
    } else {
      referencedObject = BuiltInTypes.getBuiltIn(type);
    }
    String use = ase.getAttributeValueOrNull("use");
    boolean required = use != null ? use.equals("required") : false;
    CloudField cloudField = new CloudFieldImpl(name, referencedObject, false, !required, false);
    
    cloudField.getAttributes().put("isXMLAttribute", "true");
    dataTypeNode.addField(cloudField);
  }
  
  private void createFieldFromElement(ApplicationModel model, DataObjectNode dataTypeNode, ElementSchemaElement ese, SchemaCollection collection, Map globalNSMap)
  {
    String ref = ese.getRef();
    if (ref != null) {
      QName refName = resolveQName(ref, globalNSMap, ese.getSchemaElement());
      
      if (refName == null) {
        refName = ese.getRefAsQName();
        createFieldFromElement(model, dataTypeNode, collection.findElement(refName), collection, globalNSMap);
        return;
      }
    }
    String name = ese.getName();
    boolean array = isElementArray(ese);
    boolean required = isElementRequired(ese);
    boolean nullable = isElementNullable(ese);
    String typeAttributeValue = ese.getAttributeValueOrNull("type");
    QName referencedTypeName = null;
    if (typeAttributeValue != null) {
      referencedTypeName = resolveQName(typeAttributeValue, globalNSMap, ese.getSchemaElement());
    }
    
    if (referencedTypeName == null) {
      referencedTypeName = ese.getAttributeValueAsQNameOrNull("type");
    }
    
    DataObjectNode fieldObject = null;
    if (referencedTypeName == null) {
      fieldObject = processAnonymousComplexType(model, ese, collection, globalNSMap);
    }
    else if ("http://www.w3.org/2001/XMLSchema".equals(referencedTypeName.getNamespaceURI()))
    {
      fieldObject = BuiltInTypes.getBuiltIn(referencedTypeName);
      if (fieldObject == null) {
        fieldObject = new DataObjectNode(null, referencedTypeName, DataObjectNode.ObjectCategory.BUILTIN);
        

        fieldObject.setDataType(DataType.OBJECT);
      }
    } else {
      fieldObject = model.findDataObject(referencedTypeName);
      if (fieldObject == null) {
        TypeSchemaElement typeElement = collection.findType(referencedTypeName);
        if ((typeElement instanceof SimpleTypeSchemaElement)) {
          processSchemaSimpleType(model, (SimpleTypeSchemaElement)typeElement, typeElement.getSchemaElement().getTargetNamespaceURI());
        }
        else
        {
          processComplexType(model, (ComplexTypeSchemaElement)typeElement, collection, globalNSMap);
        }
      }

      fieldObject = model.findDataObject(referencedTypeName);
    }
    CloudField cloudField = new CloudFieldImpl(name, fieldObject, array, required, nullable);
    dataTypeNode.addField(cloudField);
  }
  
  private boolean isElementNullable(ElementSchemaElement ese) {
    String nullable = ese.getAttributeValueOrNull("nillable");
    return (nullable != null) && (nullable.equals("true"));
  }
  
  private boolean isElementRequired(ElementSchemaElement ese) {
    String minOccurs = ese.getMinOccurs();
    return (minOccurs == null) || (Integer.parseInt(minOccurs) > 0);
  }
  
  private boolean isElementArray(ElementSchemaElement ese) {
    String maxOccurs = ese.getMaxOccurs();
    if (maxOccurs == null) {
      maxOccurs = "1";
    }
    return (maxOccurs.equals("unbounded")) || (Integer.parseInt(maxOccurs) > 1);
  }

  private DataObjectNode getOrProcessParent(ApplicationModel model, ComplexTypeSchemaElement ctse, SchemaCollection schemaCollection, Map globalNSMap)
  {
    Iterator it = ctse.getChildElements();
    if (it.hasNext()) {
      BaseSchemaElement complexSchema = (BaseSchemaElement)it.next();
      if ((complexSchema instanceof ComplexContentSchemaElement)) {
        Iterator ccChildren = ((ComplexContentSchemaElement)complexSchema).getChildElements();
        if (ccChildren.hasNext()) {
          BaseSchemaElement el = (BaseSchemaElement)ccChildren.next();
          if ((el instanceof ExtensionSchemaElement)) {
            QName parentTypeName = resolveQName(el.getAttributeValueOrNull("base"), globalNSMap, el.getSchemaElement());
            if (parentTypeName == null) {
              parentTypeName = el.getAttributeValueAsQNameOrNull("base");
            }
            DataObjectNode parent = model.findDataObject(parentTypeName);
            if (parent == null) {
              parent = processComplexType(model, schemaCollection.findComplexType(parentTypeName), schemaCollection, globalNSMap);
            }
            return parent;
          }
        }
      }
    }
    
    return null;
  }
  
  private QName resolveQName(String prefixStr, Map rootNamespaceMap, SchemaSchemaElement sse)
  {
    String[] parts = prefixStr.split(":");
    if ((parts != null) && (parts.length == 2)) {
      String namespace = sse.findNamespaceUriForPrefix(parts[0]);
      if (namespace == null) {
        namespace = (String)rootNamespaceMap.get(parts[0]);
      }
      if (namespace != null) {
        return new QName(namespace, parts[1], parts[0]);
      }
    }
    return null;
  }
  
  private void processSchemaSimpleType(ApplicationModel model, SimpleTypeSchemaElement stse, String targetNamespace)
  {
    QName typeName = resolveSchemaTypeName(stse, targetNamespace);
    if (typeName == null) {
      return;
    }
    
    if (model.findDataObject(typeName) == null) {
      DataObjectNode dataTypeNode = new DataObjectNode(null, typeName, DataObjectNode.ObjectCategory.STANDARD);
      model.addDataObject(dataTypeNode);
      dataTypeNode.getNodeAttributes().put("sourceSchemaType", stse);
    }
  }
  
  private QName resolveSchemaTypeName(TypeSchemaElement tse, String targetNamespace)
  {
    String name = tse.getAttributeValueOrNull("name");
    if (name == null) {
      return null;
    }
    QName typeName = new QName(targetNamespace, name);
    return typeName;
  }
  
  private Definition parseDefinition(String wsdlURL, InputStream inputStream, Properties properties) throws WSDLException
  {
    WSDLReader reader = WSDLFactory.newInstance("oracle.webservices.wsdl.WSDLFactoryImpl").newWSDLReader();
    InputSource inputSource = new InputSource(inputStream);
    if ((wsdlURL != null) && (wsdlURL.startsWith("http"))) {
      setupProxy(reader, properties);
    }
    return reader.readWSDL(wsdlURL, inputSource);
  }
  
  protected void setupProxy(WSDLReader reader, Properties connectionProperties) {
    if (connectionProperties == null) {
      return;
    }
    String useProxy = connectionProperties.getProperty("useProxy");
    if ((useProxy != null) && (Boolean.parseBoolean(useProxy))) {
      String proxyHost = connectionProperties.getProperty("proxyHost");
      String proxyPort = connectionProperties.getProperty("proxyPort");
      if (proxyPort == null) {
        proxyPort = "80";
      }
      ConnectionConfig connConfig = new ConnectionConfigBean();
      connConfig.setProxyAddress(new InetSocketAddress(proxyHost, Integer.parseInt(proxyPort)));
      ((WSDLReaderImpl)reader).setConnectionConfiguration(connConfig);
    }
  }
  
  public Set<String> getSupportedMediaTypes() {
    return supportedMediaTypes;
  }
}