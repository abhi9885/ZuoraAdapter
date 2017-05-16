package com.cognizant.ipm.adapter.runtime;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.wsdl.Definition;
import javax.wsdl.Operation;
import javax.wsdl.PortType;
import javax.xml.namespace.QName;
import javax.xml.soap.MessageFactory;
import javax.xml.soap.SOAPHeader;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import oracle.cloud.connector.api.CloudAdapterLoggingService;
import oracle.cloud.connector.api.CloudInvocationContext;
import oracle.cloud.connector.api.CloudInvocationException;
import oracle.cloud.connector.api.CloudMessage;
import oracle.cloud.connector.api.CloudMessageHandler;
import oracle.cloud.connector.api.MessageHeader;
import oracle.cloud.connector.api.RemoteApplicationException;
import oracle.cloud.connector.impl.HTTPHeaderBuilder;
import oracle.cloud.connector.impl.SOAPHeaderBuilder;
import oracle.tip.tools.ide.adapters.cloud.api.plugin.AdapterPluginContext;
import oracle.tip.tools.ide.adapters.cloud.api.plugin.CloudApplicationAdapterException;
import oracle.tip.tools.ide.adapters.cloud.api.plugin.Repository;
import oracle.tip.tools.ide.adapters.cloud.impl.plugin.DefaultAdapterPluginContext;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.cognizant.ipm.adapter.util.CloudRuntimeUtil;
import com.cognizant.ipm.adapter.util.CloudSessionData;
import com.cognizant.ipm.adapter.util.CloudUtil;
import com.cognizant.ipm.adapter.util.SoapLoginUtil;

/**
 * Handles all outbound request and responses
 * @author Upendar Reddy
 *
 */
public class CloudRuntimeMessageHandler implements CloudMessageHandler
{
  private static Properties prop = new Properties();
  private CloudInvocationContext context;
  private CloudSessionData sessionData = null;
  private String loginEndpointURL = null;
  private AdapterPluginContext pluginContext = null;
  
  static { 
	  try { 
		  prop.load(CloudRuntimeMessageHandler.class.getClassLoader().getResourceAsStream("resources/ObjectPrefix.properties"));
    }
    catch (Exception e1) {}
  }

  private String NS;
  private List<String> selectedObjectsList;
  private Pattern bindParameterPattern = Pattern.compile("[^&]&{1}\\b[A-Z|a-z][A-Z|a-z|0-9|_]*\\b");

  /**
   * method performs header and payload processing
   */
  public boolean handleRequestMessage(CloudInvocationContext context, CloudMessage message) throws CloudInvocationException
  {
	  System.out.println("Executing method "+Thread.currentThread().getStackTrace()[1]);
    boolean isCustomWSDL = false;
    if ("Custom WSDL".equals(context.getCloudOperationProperties().get("outboundWSDLType")))
    {
      isCustomWSDL = true;
    }
    System.out.println("isCustomWSDL = "+isCustomWSDL);
    this.context = context;
    this.selectedObjectsList = getSelectedObjectsList(context.getCloudOperationProperties());
    Definition targetWSDL = (Definition)context.getContextObject("wsdlDefinition");
    if (isCustomWSDL) {
      String customWsdlTargetNS = (String)context.getCloudConnectionProperties().get("customWSDLTargetNS");
      this.NS = customWsdlTargetNS;
    } else {
      this.NS = CloudRuntimeUtil.getNamespace(context);
    }
    
    QName targetOperationName = new QName(this.NS, context.getTargetOperationName());
    System.out.println(CloudRuntimeMessageHandler.class.getName() + " :target Operation name: " + targetOperationName);
    
    try
    {
      if (isCustomWSDL)
      {
        transformRequestDocumentForCustomWSDL(message);
      }
      else {
        transformRequestDocument(message, targetOperationName, context.getCloudOperationProperties());
      }
      System.out.println("context.getCloudOperationProperties() = "+context.getCloudOperationProperties());
      processHeaders(message, context.getCloudOperationProperties(), targetWSDL);
      setHttpHeader(message);
      Document document = message.getMessagePayloadAsDocument();
      printDocument(document, System.out);
    } catch (CloudInvocationException e) {
      System.out.println("Exception caught in handleRequestMessage() method: CloudInvocationException - " + e.getMessage());
      throw new CloudInvocationException(e);
    } catch (Exception e) {
      System.out.println("Exception caught in handleRequestMessage() method: Unhandled Exception- " + e.getMessage());
      System.out.println(CloudRuntimeUtil.getResourceBundle().getString("UNKNOWN_EXCEPTION_REQUEST"));
      throw new CloudInvocationException(CloudRuntimeUtil.getResourceBundle().getString("UNKNOWN_EXCEPTION_REQUEST"));
    }
    
    return true;
  }
  
  public static void printDocument(Document doc, OutputStream out) {
	    try {
			TransformerFactory tf = TransformerFactory.newInstance();
			Transformer transformer = tf.newTransformer();
			transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "no");
			transformer.setOutputProperty(OutputKeys.METHOD, "xml");
			transformer.setOutputProperty(OutputKeys.INDENT, "yes");
			transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
			transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");
			transformer.transform(new DOMSource(doc), new StreamResult(new OutputStreamWriter(out, "UTF-8")));
		} catch (Exception e) {
		}
	}
  
  private void setHttpHeader(CloudMessage message) {
	  System.out.println("Executing method "+Thread.currentThread().getStackTrace()[1]);
    Map<String, String> messageProperties = message.getMessageProperties();
    if (messageProperties != null) {
      String httpTimeoutValue = (String)messageProperties.get("jca.zuora.HttpTimeout");
      if (httpTimeoutValue != null) {
        HTTPHeaderBuilder headerBuilder = new HTTPHeaderBuilder();
        long httpTimeoutValueInMillis = Integer.parseInt(httpTimeoutValue) * 1000;
        httpTimeoutValue = String.valueOf(httpTimeoutValueInMillis);
        message.addMessageHeader(headerBuilder.setName("com.sun.xml.internal.ws.request.timeout").addValue(httpTimeoutValue).build());
        System.out.println(CloudRuntimeMessageHandler.class.getName() + ": HttpTimeoutProperty set to value: " + httpTimeoutValue + "millis");
      }
    }
  }

  /**
   * gets selected object list
   * @param jcaEndpointInteractionProperties
   * @return
   */
  private List<String> getSelectedObjectsList(Map<String, String> jcaEndpointInteractionProperties)
  {
	  System.out.println("Executing method "+Thread.currentThread().getStackTrace()[1]);
    this.selectedObjectsList = new ArrayList();
    String objects = (String)jcaEndpointInteractionProperties.get("selectedObjects");
    
    String objectName = "";
    if (objects != null) {
      while (objects.contains(",")) {
        objects = objects.trim();
        objectName = objects.substring(0, objects.indexOf(","));
        this.selectedObjectsList.add(objectName);
        objects = objects.substring(objects.indexOf(",") + 1, objects.length());
      }
      
      if ((objects.length() != 0) && (!objects.contains(","))) {
        objects = objects.trim();
        this.selectedObjectsList.add(objects);
      }
    }
    System.out.println(CloudRuntimeMessageHandler.class.getName() + ": selectedObjects: " + this.selectedObjectsList);
    return this.selectedObjectsList;
  }

  /**
   * process header information
   * @param message
   * @param jcaEndpointInteractionProperties
   * @param targetWSDL
   * @throws CloudInvocationException
   */
  private void processHeaders(CloudMessage message, Map<String, String> jcaEndpointInteractionProperties, Definition targetWSDL)
    throws CloudInvocationException, Exception
  {
	  System.out.println("Executing method "+Thread.currentThread().getStackTrace()[1]);
    this.loginEndpointURL = CloudRuntimeUtil.getEndpointURLFromWSDL(this.context);
    
    String pluginID = "zuora";
    String bindingName = "bindingName";
    Repository repository = null;
    try {
      this.pluginContext = new DefaultAdapterPluginContext(pluginID, bindingName, repository);
    }
    catch (CloudApplicationAdapterException e) {
      throw new CloudInvocationException(e);
    }
    
    this.sessionData = SoapLoginUtil.login(this.context, this.loginEndpointURL, this.pluginContext);

    SOAPHeader soapHeader = CloudUtil.createSessionHeader(MessageFactory.newInstance().createMessage(), NS, this.sessionData.getSessionId());
	List<MessageHeader> messageHeaderList = SOAPHeaderBuilder.buildFromSOAPHeader(soapHeader);
    message.addAllHeaders(messageHeaderList);
  }

  private void transformRequestDocumentForCustomWSDL(CloudMessage message) throws CloudInvocationException
  {
	  System.out.println("Executing method "+Thread.currentThread().getStackTrace()[1]);
	  Document sourceDoc = message.getMessagePayloadAsDocument();
	  Element rootElement = sourceDoc.getDocumentElement();
	  String namespacePrefix = this.context.getNamespaceManager().getOrCreatePrefix(this.NS);
	  CloudUtil.renameNamespaceRecursive(sourceDoc, rootElement, true, this.NS, namespacePrefix);
  }

  /**
   * transforms the request documents
   * @param message
   * @param targetRootElementName
   * @param jcaEndpointInteractionProperties
   * @throws CloudInvocationException
   */
  private void transformRequestDocument(CloudMessage message, QName targetRootElementName, Map<String, String> jcaEndpointInteractionProperties)
    throws CloudInvocationException
  {
	  System.out.println("Executing method "+Thread.currentThread().getStackTrace()[1]);
    //String AdapterNamespace = getAdapterNamespace(this.context.getCloudOperationProperties(), false);
    Document sourceDoc = message.getMessagePayloadAsDocument();
    String targetOperationName = targetRootElementName.getLocalPart();
    Element sourceRootElement = sourceDoc.getDocumentElement();
    String sfPrefix = this.context.getNamespaceManager().getOrCreatePrefix(this.NS);
    if ((sourceRootElement != null) && (sourceRootElement.lookupNamespaceURI(sfPrefix) == null)) {
      sourceRootElement.setAttributeNS("http://www.w3.org/2000/xmlns/", CloudUtil.getQualifiedName("xmlns", sfPrefix), this.NS);
    }
    sourceRootElement = (Element)sourceDoc.renameNode(sourceRootElement, this.NS, CloudUtil.getQualifiedName(sfPrefix, targetRootElementName.getLocalPart()));
    if ((targetOperationName.equalsIgnoreCase("query")) || (targetOperationName.equalsIgnoreCase("queryMore")))
    {
      handleQueryRequest(jcaEndpointInteractionProperties, message, targetOperationName);
    }
    if (targetOperationName.equalsIgnoreCase("search")) {
      handleSearchRequest(jcaEndpointInteractionProperties, sourceDoc, targetOperationName);
    }
    if ((targetOperationName.equalsIgnoreCase("delete")) || (targetOperationName.equalsIgnoreCase("undelete")))
    {
      //NodeList idNodeList = sourceRootElement.getElementsByTagNameNS(AdapterNamespace, "ids");
      //verifyIdsPrefix(sourceDoc, targetOperationName, idNodeList);
    }
    if ((targetOperationName.equalsIgnoreCase("getDeleted")) || (targetOperationName.equalsIgnoreCase("getUpdated")) || (targetOperationName.equalsIgnoreCase("retrieve")))
    {
      if (sourceDoc.getElementsByTagName("sObjectType").getLength() == 0)
      {
        addSobjectTypeNodeInRequest(sourceDoc, targetOperationName);
      }
    }
    if (targetOperationName.equalsIgnoreCase("merge")) {
      handleMergeRequest(sourceDoc, sfPrefix);
    }
    if ((targetOperationName.equalsIgnoreCase("create")) || 
    		(targetOperationName.equalsIgnoreCase("update")) || 
    		(targetOperationName.equalsIgnoreCase("upsert")) || 
    		(targetOperationName.equalsIgnoreCase("process")))
    {
      NodeList nodelist = sourceRootElement.getChildNodes();
      for (int i = 0; i < nodelist.getLength(); i++) {
        Node node = nodelist.item(i);
        if ((node instanceof Element)) {
          Element childElement = (Element)node;
          String elementName = childElement.getLocalName();
          
          if (!this.selectedObjectsList.isEmpty()) {
            if ((this.selectedObjectsList.contains(elementName)) || (elementName.equalsIgnoreCase("ProcessSubmitRequest")) || (elementName.equalsIgnoreCase("ProcessWorkitemRequest")))
            {
              normalizeElement(childElement, sourceDoc, targetOperationName);
            }
          }
          else {
            System.out.println(CloudRuntimeUtil.getResourceBundle().getString("SELECTED_OBJECTS_LIST_EMPTY"));
            throw new CloudInvocationException(CloudRuntimeUtil.getResourceBundle().getString("SELECTED_OBJECTS_LIST_EMPTY"));
          }
        }
      }
      if (targetOperationName.equalsIgnoreCase("process"))
      {
        handleProcessOperation(sourceDoc, targetOperationName);
      }
    }
    sourceRootElement.normalize();
    sourceDoc.normalizeDocument();
  }

  private void handleProcessOperation(Document sourceDoc, String targetOperationName)
    throws CloudInvocationException
  {
	  System.out.println("Executing method "+Thread.currentThread().getStackTrace()[1]);
    NodeList idNodeList = null;
    Element sourceRootElement = sourceDoc.getDocumentElement();
    idNodeList = sourceRootElement.getElementsByTagNameNS("*", "objectId");
    verifyIdsPrefix(sourceDoc, targetOperationName, idNodeList);
  }

  private void handleMergeRequest(Document sourceDoc, String sfPrefix)
    throws CloudInvocationException
  {
	  System.out.println("Executing method "+Thread.currentThread().getStackTrace()[1]);
    String AdapterNamespace = getAdapterNamespace(this.context.getCloudOperationProperties(), false);
    if (!this.selectedObjectsList.isEmpty()) {
      Element sourceRootElement = sourceDoc.getDocumentElement();
      NodeList masterRecordNodeList = sourceRootElement.getElementsByTagNameNS(AdapterNamespace, "masterRecord");
      for (int i = 0; i < masterRecordNodeList.getLength(); i++) {
        Element masterRecordNode = (Element)masterRecordNodeList.item(i);
        masterRecordNode.setAttributeNS("http://www.w3.org/2001/XMLSchema-instance", CloudUtil.getQualifiedName("xsi", "type"), CloudUtil.getQualifiedName(sfPrefix, (String)this.selectedObjectsList.get(0)));
      }
      NodeList mergeRequestNodeList = sourceRootElement.getElementsByTagNameNS(AdapterNamespace, "MergeRequest");
      for (int i = 0; i < mergeRequestNodeList.getLength(); i++) {
        Element nodeToRename = (Element)mergeRequestNodeList.item(i);
        if (nodeToRename != null) {
          if (nodeToRename.lookupNamespaceURI(sfPrefix) == null) {
            nodeToRename.setAttributeNS("http://www.w3.org/2000/xmlns/", CloudUtil.getQualifiedName("xmlns", sfPrefix), this.NS);
          }
          sourceDoc.renameNode(nodeToRename, this.NS, CloudUtil.getQualifiedName(sfPrefix, "request"));
        }
      }
    }
    else
    {
      System.out.println(CloudRuntimeUtil.getResourceBundle().getString("SELECTED_OBJECTS_LIST_EMPTY"));
      throw new CloudInvocationException(CloudRuntimeUtil.getResourceBundle().getString("SELECTED_OBJECTS_LIST_EMPTY"));
    }
  }

  private void addSobjectTypeNodeInRequest(Document sourceDoc, String targetOperationName)
    throws CloudInvocationException
  {
	  System.out.println("Executing method "+Thread.currentThread().getStackTrace()[1]);
    String AdapterNamespace = getAdapterNamespace(this.context.getCloudOperationProperties(), false);
    
    if (!this.selectedObjectsList.isEmpty()) {
      Element sourceRootElement = sourceDoc.getDocumentElement();
      String objectName = (String)this.selectedObjectsList.get(0);
      Element sObjectType = sourceDoc.createElement("sObjectType");
      
      sObjectType.appendChild(sourceDoc.createTextNode(objectName));
      Node refNode = null;
      if (targetOperationName.equalsIgnoreCase("retrieve"))
      {
        NodeList idNodeList = sourceRootElement.getElementsByTagNameNS(AdapterNamespace, "ids");
        
        if (idNodeList.getLength() != 0) {
          refNode = idNodeList.item(0);
        }
      } else {
        refNode = sourceRootElement.getChildNodes().item(0);
      }
      
      if (refNode != null)
        sourceRootElement.insertBefore(sObjectType, refNode);
    } else {
      System.out.println(CloudRuntimeUtil.getResourceBundle().getString("SELECTED_OBJECTS_LIST_EMPTY"));
      
      throw new CloudInvocationException(CloudRuntimeUtil.getResourceBundle().getString("SELECTED_OBJECTS_LIST_EMPTY"));
    }
  }

  private void handleSearchRequest(Map<String, String> jcaEndpointInteractionProperties, Document sourceDoc, String targetOperationName)
    throws CloudInvocationException
  {
	  System.out.println("Executing method "+Thread.currentThread().getStackTrace()[1]);
    Element sourceRootElement = sourceDoc.getDocumentElement();
    if (sourceDoc.getElementsByTagName("searchString").getLength() == 0)
    {
      String searchString = (String)jcaEndpointInteractionProperties.get("queryString");
      System.out.println("Search string before replacing bind parameters: " + searchString);
      if (searchString == null) {
        System.out.println(CloudRuntimeUtil.getResourceBundle().getString("MISSING_SEARCH_STRING"));
        throw new CloudInvocationException(CloudRuntimeUtil.getResourceBundle().getString("MISSING_SEARCH_STRING"));
      }
      
      searchString = handleBindParameters(searchString, sourceDoc);
      System.out.println("Search string after replacing bind parameters: " + searchString);
      Element searchStringElement = null;
      searchStringElement = sourceDoc.createElementNS(this.NS, CloudUtil.getQualifiedName("ns1", "searchString"));
      searchStringElement.appendChild(sourceDoc.createTextNode(searchString));
      sourceRootElement.appendChild(searchStringElement);
    }
  }

  /**
   * handles the Query operations of CRM
   * @param jcaEndpointInteractionProperties
   * @param message
   * @param targetOperationName
   * @throws CloudInvocationException
   */
  private void handleQueryRequest(Map<String, String> jcaEndpointInteractionProperties, CloudMessage message, String targetOperationName)
    throws CloudInvocationException
  {
	  System.out.println("Executing method "+Thread.currentThread().getStackTrace()[1]);
    Document sourceDoc = message.getMessagePayloadAsDocument();
    Element sourceRootElement = sourceDoc.getDocumentElement();
    String queryLocator = null;
    if (message.getMessageProperties() != null) {
      Map<String, String> messagePropMap = message.getMessageProperties();
      
      if (messagePropMap != null) {
        queryLocator = (String)messagePropMap.get("jca.zuora.queryLocator");
      }
    }
    if ((queryLocator != null) && (!queryLocator.equals(""))) {
      handleQueryMore(sourceDoc, queryLocator);
    }
    else if (sourceDoc.getElementsByTagName("queryString").getLength() == 0)
    {
      String queryString = (String)jcaEndpointInteractionProperties.get("queryString");
      if (queryString == null) {
        throw new CloudInvocationException(CloudRuntimeUtil.getResourceBundle().getString("MISSING_QUERY_STRING"));
      }
      
      System.out.println("querystring before replacing bind parameters: " + queryString);
      queryString = handleBindParameters(queryString, sourceDoc);
      System.out.println("querystring after replacing bind parameters: " + queryString);
      Element queryStringElement = null;
      queryStringElement = sourceDoc.createElementNS(this.NS, CloudUtil.getQualifiedName("ns1", "queryString"));
      queryStringElement.appendChild(sourceDoc.createTextNode(queryString));
      sourceRootElement.appendChild(queryStringElement);
      if(sourceDoc != null) {
    	  System.out.println("payload = "+sourceDoc.getTextContent());
      }
    }
  }

  private void handleQueryMore(Document sourceDoc, String queryLocator)
  {
	  System.out.println("Executing method "+Thread.currentThread().getStackTrace()[1]);
    String sfPrefix = this.context.getNamespaceManager().getOrCreatePrefix(this.NS);
    Element sourceRootElement = sourceDoc.getDocumentElement();
    sourceRootElement = (Element)sourceDoc.renameNode(sourceRootElement, this.NS, CloudUtil.getQualifiedName(sfPrefix, "queryMore"));
    removeQueryParametersNodesFromRequest(sourceDoc);
    Element queryLocatorElement = sourceDoc.createElementNS(this.NS, CloudUtil.getQualifiedName("urn", "queryLocator"));
    queryLocatorElement.appendChild(sourceDoc.createTextNode(queryLocator));
    sourceRootElement.appendChild(queryLocatorElement);
  }

  private void removeQueryParametersNodesFromRequest(Document sourceDoc)
  {
	  System.out.println("Executing method "+Thread.currentThread().getStackTrace()[1]);
    String AdapterNamespace = getAdapterNamespace(this.context.getCloudOperationProperties(), false);
    Element sourceRootElement = sourceDoc.getDocumentElement();
    NodeList queryParametersNodeList = sourceDoc.getElementsByTagNameNS(AdapterNamespace, "QueryParameters");
    if (queryParametersNodeList.getLength() != 0) {
      Node nodeToRemove = queryParametersNodeList.item(0);
      sourceRootElement.removeChild(nodeToRemove);
    }
  }

  private void verifyIdsPrefix(Document sourceDoc, String targetOperationName, NodeList idNodeList)
    throws CloudInvocationException
  {
	  System.out.println("Executing method "+Thread.currentThread().getStackTrace()[1]);
    if (!this.selectedObjectsList.isEmpty()) {
      String idPrefix = null;
      String id = null;
      Node idNode = null;
      for (int i = 0; i < idNodeList.getLength(); i++) {
        idNode = idNodeList.item(i);
        if (idNode.getChildNodes().getLength() != 0) {
          id = idNode.getChildNodes().item(0).getNodeValue();
          if (id.length() >= 4) {
            idPrefix = id.substring(0, 3);
            String objName = prop.getProperty(idPrefix);
            List<String> objList = new ArrayList();
            if (objName != null) {
              while (objName.contains(",")) {
                objList.add(objName.substring(0, objName.indexOf(",")));
                objName = objName.substring(objName.indexOf(",") + 1, objName.length());
              }
              if (!objName.contains(",")) {
                objList.add(objName);
              }
            }
            
            boolean isIdInvalid = false;
            for (String obj : objList) {
              obj = obj.trim();
              if (!this.selectedObjectsList.contains(obj)) {
                isIdInvalid = true;
              } else {
                isIdInvalid = false;
                break;
              }
            }
            
            if (isIdInvalid) {
              throw new CloudInvocationException(CloudRuntimeUtil.getResourceBundle().getString("INVALID_ID"));
            }
          }
        }
      }
    }
    else
    {
      throw new CloudInvocationException(CloudRuntimeUtil.getResourceBundle().getString("SELECTED_OBJECTS_LIST_EMPTY"));
    }
  }

  private String handleBindParameters(String query, Document sourceDoc) throws CloudInvocationException
  {
	  System.out.println("Executing method "+Thread.currentThread().getStackTrace()[1]);
    Set parameterSet = getBindParameterSet(query);
    Map<String, String> bindVariables = getBindVariablesFromInput(sourceDoc);
    removeQueryParametersNodesFromRequest(sourceDoc);
    String fullQuery = processQueryParameters(query, bindVariables, parameterSet);
    fullQuery = unescapeQuery(fullQuery);

    return fullQuery;
  }
  
  private String unescapeQuery(String query) {
    query = query.replace("&&", "&");
    return query;
  }
  
  private Map<String, String> getBindVariablesFromInput(Document sourceDoc) throws CloudInvocationException
  {
	  System.out.println("Executing method "+Thread.currentThread().getStackTrace()[1]);
    String AdapterNamespace = getAdapterNamespace(this.context.getCloudOperationProperties(), false);
    Map<String, String> bindVariableMap = new HashMap();
    NodeList queryParametersNodeList = sourceDoc.getElementsByTagNameNS(AdapterNamespace, "QueryParameters");
    if (queryParametersNodeList.getLength() != 0) {
      NodeList childNodeList = queryParametersNodeList.item(0).getChildNodes();
      String bindVariableName = null;
      String bindVariableValue = null;
      for (int i = 0; i < childNodeList.getLength(); i++) {
        Node node = childNodeList.item(i);
        if (node.getNodeType() == 1) {
          bindVariableName = node.getLocalName();
          if (node.getChildNodes().getLength() != 0) {
            bindVariableValue = node.getChildNodes().item(0).getNodeValue();
            
            if (bindVariableValue.length() < 1) {
              throw new CloudInvocationException(CloudRuntimeUtil.getResourceBundle().getString("BIND_VARIABLES_VALUE_EMPTY"));
            }
          }
          else
          {
            throw new CloudInvocationException(CloudRuntimeUtil.getResourceBundle().getString("BIND_VARIABLES_VALUE_EMPTY"));
          }
          bindVariableMap.put(bindVariableName, bindVariableValue);
        }
      }
    }
    return bindVariableMap;
  }
  
  private String processQueryParameters(String query, Map<String, String> bindVariablesMap, Set bindVariablesSet)
    throws CloudInvocationException
  {
	  System.out.println("Executing method "+Thread.currentThread().getStackTrace()[1]);
    String param = null;
    String value = null;
    String fullQuery = query;
    if (!bindVariablesSet.isEmpty()) {
      Iterator iter = bindVariablesSet.iterator();
      while (iter.hasNext()) {
        param = (String)iter.next();
        if (bindVariablesMap.containsKey(param)) {
          value = (String)bindVariablesMap.get(param);
          param = fixBindVariable(param);
          fullQuery = fullQuery.replace(param, value);
        } else {
          throw new CloudInvocationException(CloudRuntimeUtil.getResourceBundle().getString("BIND_VARIABLES_VALUE_MISSING"));
        }
      }
    }
    return fullQuery;
  }
  
  private String fixBindVariable(String variable) {
    return !variable.startsWith("&") ? "&" + variable : variable;
  }
  
  private Set<String> getBindParameterSet(String query) {
	  System.out.println("Executing method "+Thread.currentThread().getStackTrace()[1]);
    Matcher matcher = this.bindParameterPattern.matcher(query);
    Set parameterSet = new HashSet();
    while (matcher.find()) {
      String parameter = query.substring(matcher.start(), matcher.end());
      int prefixIndex = parameter.indexOf(getBindParameterPrefix());
      parameterSet.add(parameter.substring(prefixIndex + 1));
    }
    return parameterSet;
  }
  
  private String getBindParameterPrefix() {
    return "&";
  }

  private void transformResponseMessage(Document sourceDoc, CloudInvocationContext context) throws CloudInvocationException
  {
	  System.out.println("Executing method "+Thread.currentThread().getStackTrace()[1]);
    Element rootElement = sourceDoc.getDocumentElement();
    String operationResponse = rootElement.getLocalName();
    String AdapterNamespace = getAdapterNamespace(context.getCloudOperationProperties(), true);
    boolean boolNew = isNew(AdapterNamespace);
    if (boolNew) {
      AdapterNamespace = getAdapterNamespace(context.getCloudOperationProperties(), false);
    }
    if (operationResponse.equalsIgnoreCase("searchResponse"))
    {
      modifyElementsNameInResponse(sourceDoc, context, "record", AdapterNamespace, boolNew);
    }
    if (operationResponse.equalsIgnoreCase("retrieveResponse"))
    {
      modifyElementsNameInResponse(sourceDoc, context, "result", AdapterNamespace, boolNew);
    }
    denormalizeElements(context, sourceDoc, AdapterNamespace);
  }
  
  private void transformResponseMessageForCustomWSDL(Document sourceDoc, CloudInvocationContext context)
    throws CloudInvocationException
  {
	  System.out.println("Executing method "+Thread.currentThread().getStackTrace()[1]);
    String AdapterNamespace = getAdapterNamespace(context.getCloudOperationProperties(), true);
    boolean boolNew = isNew(AdapterNamespace);
    if (boolNew) {
      AdapterNamespace = getAdapterNamespace(context.getCloudOperationProperties(), false);
    }
    
    Element rootElement = sourceDoc.getDocumentElement();
    String namespacePrefix = context.getNamespaceManager().getOrCreatePrefix(AdapterNamespace);
    rootElement = (Element)sourceDoc.renameNode(rootElement, AdapterNamespace, CloudUtil.getQualifiedName(namespacePrefix, rootElement.getLocalName()));
    Node resultNode = rootElement.getFirstChild();
    if (resultNode != null) {
      resultNode = sourceDoc.renameNode(resultNode, AdapterNamespace, CloudUtil.getQualifiedName(namespacePrefix, resultNode.getLocalName()));
    }
  }

  private boolean isNew(String AdapterNamespace)
  {
    if (AdapterNamespace.contains("#new")) {
      return true;
    }
    return false;
  }

  private void modifyElementsNameInResponse(Document sourceDoc, CloudInvocationContext context, String nodeToRename, String AdapterNamespace, boolean boolNew)
  {
	  System.out.println("Executing method "+Thread.currentThread().getStackTrace()[1]);
    Element rootElement = sourceDoc.getDocumentElement();
    NodeList recordNodeList = rootElement.getElementsByTagName(nodeToRename);
    if (recordNodeList.getLength() != 0) {
      for (int i = 0; i < recordNodeList.getLength(); i++) {
        Node recordNode = recordNodeList.item(i);
        if (recordNode.getAttributes() != null) {
          Node attributeNode = recordNode.getAttributes().getNamedItemNS("http://www.w3.org/2001/XMLSchema-instance", "type");
          if (attributeNode != null) {
            String objectName = attributeNode.getNodeValue();
            if (objectName.contains(":")) {
              objectName = objectName.substring(objectName.indexOf(":") + 1, objectName.length());
            }
            nodeToRename = nodeToRename.substring(0, 1).toUpperCase() + nodeToRename.substring(1);
            if (AdapterNamespace != null) {
              String AdapterNamespacePrefix = context.getNamespaceManager().getOrCreatePrefix(AdapterNamespace);
              sourceDoc.renameNode(recordNode, AdapterNamespace, CloudUtil.getQualifiedName(AdapterNamespacePrefix, objectName + nodeToRename));
              if (boolNew) {
                String operationResponse = rootElement.getLocalName();
                if (operationResponse.equalsIgnoreCase("searchResponse"))
                {
                  CloudUtil.renameNamespaceRecursive(sourceDoc, recordNode, false, AdapterNamespace, AdapterNamespacePrefix);
                }
              }
            }
            else
            {
              sourceDoc.renameNode(recordNode, "http://api.zuora.com", objectName + nodeToRename);
            }
          }
        }
      }
    }
    removeSearchRecordsNodes(rootElement, context);
  }

  private void removeSearchRecordsNodes(Element rootElement, CloudInvocationContext context)
  {
	  System.out.println("Executing method "+Thread.currentThread().getStackTrace()[1]);
    NodeList searchRecordsNodelist = rootElement.getElementsByTagNameNS("*", "searchRecords");
    if (searchRecordsNodelist.getLength() != 0) {
      List<Node> listToRemove = new ArrayList();
      for (int i = 0; i < searchRecordsNodelist.getLength(); i++) {
        Node searchRecordNode = searchRecordsNodelist.item(i);
        listToRemove.add(searchRecordNode);
        Node parentNode = searchRecordNode.getParentNode();
        if (parentNode != null) {
          NodeList childNodeList = searchRecordNode.getChildNodes();
          if (childNodeList.getLength() != 0) {
            parentNode.appendChild(childNodeList.item(0));
          }
        }
      }
      for (Node nodeToRemove : listToRemove) {
        nodeToRemove.getParentNode().removeChild(nodeToRemove);
      }
    }
  }

  private void denormalizeElements(CloudInvocationContext context, Document sourceDoc, String AdapterNamespace)
    throws CloudInvocationException
  {
	  System.out.println("Executing method "+Thread.currentThread().getStackTrace()[1]);
    Element rootElement = sourceDoc.getDocumentElement();
    String responseElementName = rootElement.getLocalName();
    Map<String, String> jcaProperties = context.getCloudOperationProperties();
    String operationName = getOperationResponseElementName(context.getIntegrationWSDL(), context);
    sourceDoc.renameNode(rootElement, rootElement.getNamespaceURI(), operationName);

    if (AdapterNamespace != null) {
      modifyNamespacesInResponse(AdapterNamespace, responseElementName, sourceDoc, context);
    }
  }

  private String getAdapterNamespace(Map<String, String> jcaProperties, boolean actualNS)
  {
	  System.out.println("Executing method "+Thread.currentThread().getStackTrace()[1]);
    String AdapterNamespace = (String)jcaProperties.get("AdapterNamespace");
    if (actualNS) {
      return AdapterNamespace;
    }
    AdapterNamespace = AdapterNamespace.replace("#new", "");
    return AdapterNamespace;
  }
  
  private void modifyNamespacesInResponse(String AdapterNamespace, String responseElementName, Document sourceDoc, CloudInvocationContext context)
  {
	  System.out.println("Executing method "+Thread.currentThread().getStackTrace()[1]);
    Element rootElement = sourceDoc.getDocumentElement();
    String AdapterNamespacePrefix = context.getNamespaceManager().getOrCreatePrefix(AdapterNamespace);
    if (rootElement.getLocalName().equalsIgnoreCase("queryMoreResponse"))
    {
      sourceDoc.renameNode(rootElement, AdapterNamespace, CloudUtil.getQualifiedName(AdapterNamespacePrefix, "queryResponse"));
    }
    else
    {
      sourceDoc.renameNode(rootElement, AdapterNamespace, CloudUtil.getQualifiedName(AdapterNamespacePrefix, rootElement.getLocalName()));
    }

    Element resultElement = null;
    
    NodeList resultNodeList = rootElement.getElementsByTagName("result");
    
    for (int i = 0; i < resultNodeList.getLength(); i++) {
      resultElement = (Element)resultNodeList.item(i);
      try {
        String resultElementName = OperationResult.valueOf(responseElementName).toString();
        
        sourceDoc.renameNode(resultElement, AdapterNamespace, CloudUtil.getQualifiedName(AdapterNamespacePrefix, resultElementName));
      }
      catch (IllegalArgumentException e)
      {
        System.out.println("Exception caught in modifyNamespacesInResponse() method: IllegalArgumentException for " + responseElementName + " element. " + "Not able to find the corresponding value in OperationResult enum.");
      }
    }

    if ((responseElementName.equalsIgnoreCase("queryResponse")) || (responseElementName.equalsIgnoreCase("QueryAllResponse")) || (responseElementName.equalsIgnoreCase("queryMoreResponse")))
    {
      String namespacePrefix = context.getNamespaceManager().getOrCreatePrefix(AdapterNamespace);
      CloudUtil.renameNamespaceRecursive(sourceDoc, rootElement, true, AdapterNamespace, namespacePrefix);
    }
  }

  private void normalizeElement(Element childElement, Document sourceDoc, String targetOperationName)
  {
	  System.out.println("Executing method "+Thread.currentThread().getStackTrace()[1]);
    String localName = childElement.getLocalName();
    String prefix = childElement.getPrefix();
    String sfObjectPrefix = this.context.getNamespaceManager().getOrCreatePrefix(this.NS);
    
    if (childElement.lookupNamespaceURI(sfObjectPrefix) == null) {
      childElement.setAttributeNS("http://www.w3.org/2000/xmlns/", CloudUtil.getQualifiedName("xmlns", sfObjectPrefix), this.NS);
    }

    Element normalizedNode = null;
    if (targetOperationName.equalsIgnoreCase("process")) {
      normalizedNode = (Element)sourceDoc.renameNode(childElement, this.NS, CloudUtil.getQualifiedName(sfObjectPrefix, "actions"));
    }
    else
    {
      normalizedNode = (Element)sourceDoc.renameNode(childElement, this.NS, CloudUtil.getQualifiedName(sfObjectPrefix, "sObjects"));
    }
    normalizedNode.setAttributeNS("http://www.w3.org/2001/XMLSchema-instance", CloudUtil.getQualifiedName("xsi", "type"), CloudUtil.getQualifiedName(prefix, localName));
    normalizedNode.normalize();
  }

  public boolean handleResponseMessage(CloudInvocationContext context, CloudMessage message)
    throws CloudInvocationException
  {
	  System.out.println("Executing method "+Thread.currentThread().getStackTrace()[1]);

    try
    {
      Document doc = message.getMessagePayloadAsDocument();
      
      boolean isCustomWSDL = false;
      if ("Custom WSDL".equals(context.getCloudOperationProperties().get("outboundWSDLType")))
      {
        isCustomWSDL = true;
      }
      
      boolean useVoid = suppressResponse(context, doc);
      if (!useVoid) {
        if (isCustomWSDL) {
          transformResponseMessageForCustomWSDL(doc, context);
        } else {
          transformResponseMessage(doc, context);
        }
      }
      processResponseHeaders(message, context);
    }
    catch (CloudInvocationException e) {
      System.out.println("Exception caught in handleResponseMessage() method: CloudInvocationExcpetion- " + e.getMessage());
      throw new CloudInvocationException(e);
    } catch (Exception e) {
      System.out.println("Exception caught in handleResponseMessage() method: Unhandled Exception- " + e.getMessage());
      System.out.println(CloudRuntimeUtil.getResourceBundle().getString("UNKNOWN_EXCEPTION_RESPONSE"));
      throw new CloudInvocationException(CloudRuntimeUtil.getResourceBundle().getString("UNKNOWN_EXCEPTION_RESPONSE"));
    }
    return true;
  }
  
  private void processResponseHeaders(CloudMessage message, CloudInvocationContext context)
  {
	  System.out.println("Executing method "+Thread.currentThread().getStackTrace()[1]);
    List<MessageHeader> responseHeaders = message.getHeaders();
    if ((responseHeaders != null) && (responseHeaders.size() > 0)) {
      Map<String, String> headerProperties = new HashMap();
      
      for (int i = 0; i < responseHeaders.size(); i++) {
        MessageHeader header = (MessageHeader)responseHeaders.get(i);
        if (header.getHeaderName().equalsIgnoreCase("DebuggingInfo"))
        {
          List<MessageHeader> childHeaders = header.getChildren();
          if ((childHeaders != null) && (childHeaders.size() > 0)) {
            headerProperties.put("jca.zuora.response.debugLog", (String)((MessageHeader)childHeaders.get(0)).getValue());
          }
        }

        if (header.getHeaderName().equalsIgnoreCase("LimitInfoHeader"))
        {
          List<MessageHeader> childHeaders = header.getChildren();
          if ((childHeaders != null) && (childHeaders.size() > 0)) {
            List<MessageHeader> limitInfoChildHeaders = ((MessageHeader)childHeaders.get(0)).getChildren();
            
            if ((limitInfoChildHeaders != null) && (limitInfoChildHeaders.size() > 0))
            {
              for (int j = 0; j < limitInfoChildHeaders.size(); j++) {
                header = (MessageHeader)limitInfoChildHeaders.get(j);
                if (header.getHeaderName().equalsIgnoreCase("current"))
                {
                  headerProperties.put("jca.zuora.response.limitInfo.current", (String)header.getValue());

                }
                else if (header.getHeaderName().equalsIgnoreCase("limit"))
                {
                  headerProperties.put("jca.zuora.response.limitInfo.limit", (String)header.getValue());
                }
              }
            }
          }
        }
      }
      message.setProperties(headerProperties);
    }
  }

  private boolean suppressResponse(CloudInvocationContext context, Document responseDocument)
    throws CloudInvocationException
  {
	  System.out.println("Executing method "+Thread.currentThread().getStackTrace()[1]);
    CloudAdapterLoggingService loggingService = context.getLoggingService();
    String suppressResponse = (String)context.getCloudOperationProperties().get("suppressResponse");
    boolean useVoid = suppressResponse != null ? Boolean.parseBoolean(suppressResponse) : false;
    if (useVoid) {
      createVoidResponse(context, responseDocument);
    }
    return useVoid;
  }

  protected void createVoidResponse(CloudInvocationContext context, Document responseDocument)
  {
	  System.out.println("Executing method "+Thread.currentThread().getStackTrace()[1]);
    Element element = responseDocument.getDocumentElement();
    NodeList nl = element.getChildNodes();
    List<Node> listToRemove = new ArrayList();
    for (int i = 0; i < nl.getLength(); i++) {
      Node nodeToBeRemoved = nl.item(i);
      listToRemove.add(nodeToBeRemoved);
    }
    for (Node nodeToRemove : listToRemove) {
      nodeToRemove.getParentNode().removeChild(nodeToRemove);
    }
  }

  private String getOperationResponseElementName(Definition definition, CloudInvocationContext context)
    throws CloudInvocationException
  {
	  System.out.println("Executing method "+Thread.currentThread().getStackTrace()[1]);
    String operationName = getOperationName(definition, context);
    operationName = operationName + "Response";
    return operationName;
  }
  
  private String getOperationName(Definition definition, CloudInvocationContext context)
    throws CloudInvocationException
  {
	  System.out.println("Executing method "+Thread.currentThread().getStackTrace()[1]);
    String operationName = null;
    Map portTypes = definition.getPortTypes();
    PortType portType = null;
    Iterator i = portTypes.keySet().iterator(); if (i.hasNext()) { Object key = i.next();
      portType = (PortType)portTypes.get(key);
    }
    
    if (portType != null) {
      List operations = portType.getOperations();
      operationName = ((Operation)operations.get(0)).getName();
    }
    if (operationName == null) {
      throw new CloudInvocationException(CloudRuntimeUtil.getResourceBundle().getString("UNABLE_TO_FIND_OPERATION_NAME"));
    }
    return operationName;
  }

  public boolean handleErrorMessage(CloudInvocationContext context, CloudMessage message) throws CloudInvocationException
  {
	  System.out.println("Executing method "+Thread.currentThread().getStackTrace()[1]);
    Object runtimeException = context.getContextObject("runtimeException");
    if ((runtimeException instanceof RemoteApplicationException))
    {
      Document doc = message.getMessagePayloadAsDocument();
      NodeList faultNodeList = doc.getElementsByTagNameNS("*", "exceptionCode");
      
      if (faultNodeList.getLength() != 0) {
        String faultCode = faultNodeList.item(0).getChildNodes().item(0).getNodeValue();
        
        if (faultCode.contains("INVALID_SESSION_ID")) {
          System.out.println("Got Invalid Session Id fault, retrying...");
          
          CloudRuntimeSession cloudSession = (CloudRuntimeSession)context.getSession();
          cloudSession.setSessionLastUsed(0L);
          throw new RemoteApplicationException(message, CloudRuntimeUtil.getResourceBundle().getString("INVALID_SESSION"), true);
        }

        RemoteApplicationException remoteException = (RemoteApplicationException)runtimeException;
        if (isRetryableException(remoteException)) {
          throw new RemoteApplicationException(message, remoteException.getMessage(), true);
        }
      }
    }
    
    Element errorElement = (Element)message.getMessagePayload();
    if (errorElement != null) {
      String localName = errorElement.getLocalName();
      if (localName == null) {
        localName = "UnexpectedErrorFault";
      }
      context.setContextObject("errorCode", localName);
    }
    return false;
  }
  
  private boolean isRetryableException(Exception ex) {
    if ((ex instanceof IOException)) {
      return true;
    }
    
    Throwable t = ex.getCause();
    while (t != null) {
      if ((ex instanceof IOException)) {
        return true;
      }
      t = t.getCause();
    }
    
    return false;
  }
}