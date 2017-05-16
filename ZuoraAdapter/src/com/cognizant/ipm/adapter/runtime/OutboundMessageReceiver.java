package com.cognizant.ipm.adapter.runtime;

import java.io.StringReader;
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import javax.wsdl.Definition;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.soap.MessageFactory;
import javax.xml.soap.SOAPHeader;
import javax.xml.soap.SOAPMessage;

import oracle.cloud.connector.api.CloudApplicationConnectionFactory;
import oracle.cloud.connector.api.CloudConnectorException;
import oracle.cloud.connector.api.CloudEndpoint;
import oracle.cloud.connector.api.CloudEndpointFactory;
import oracle.cloud.connector.api.CloudInvocationContext;
import oracle.cloud.connector.api.CloudInvocationException;
import oracle.cloud.connector.api.CloudMessage;
import oracle.cloud.connector.api.MessageHeader;
import oracle.cloud.connector.api.NamespaceManager;
import oracle.cloud.connector.impl.NamespaceManagerImpl;
import oracle.cloud.connector.impl.SOAPHeaderBuilder;
import oracle.cloud.connector.impl.soap.BaseSOAPMessageReceiver;
import oracle.tip.adapter.cloud.CloudAdapterUtil;
import oracle.tip.tools.ide.adapters.cloud.api.plugin.AdapterPluginContext;
import oracle.tip.tools.ide.adapters.cloud.api.plugin.CloudApplicationAdapterException;
import oracle.tip.tools.ide.adapters.cloud.api.plugin.Repository;
import oracle.tip.tools.ide.adapters.cloud.impl.plugin.DefaultAdapterPluginContext;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import com.cognizant.ipm.adapter.util.CloudRuntimeUtil;
import com.cognizant.ipm.adapter.util.CloudSessionData;
import com.cognizant.ipm.adapter.util.CloudUtil;
import com.cognizant.ipm.adapter.util.SoapLoginUtil;

public class OutboundMessageReceiver extends BaseSOAPMessageReceiver
{
  private CloudApplicationConnectionFactory connectionFactory;
  private CloudInvocationContext cloudInvocationContext;
  private CloudEndpointFactory cloudEndpointFactory;
  private String organizationID = null;
  private CloudEndpoint cloudEndpoint;
  private boolean isReleased = false;
  private String loginEndpointURL = null;
  private AdapterPluginContext pluginContext = null;
  private CloudSessionData sessionData = null;
  static final String NS = "http://api.zuora.com";
  
  public OutboundMessageReceiver(CloudApplicationConnectionFactory connectionFactory, CloudEndpointFactory cloudEndpointFactory)
    throws CloudConnectorException, CloudInvocationException
  {
	  System.out.println("Executing method "+Thread.currentThread().getStackTrace()[1]);
    this.connectionFactory = connectionFactory;
    this.cloudEndpointFactory = cloudEndpointFactory;
    this.cloudEndpoint = cloudEndpointFactory.createEndpoint();
    this.cloudInvocationContext = cloudEndpointFactory.getCloudInvocationContext();
    Definition intgWsdlDef = cloudEndpointFactory.getCloudInvocationContext().getIntegrationWSDL();
    setExternalWSDL(intgWsdlDef);
    this.loginEndpointURL = CloudRuntimeUtil.getEndpointURLFromWSDL(this.cloudInvocationContext);
    
    String pluginID = "ZuoraAdapter";
    String bindingName = "bindingName";
    Repository repository = null;
    try {
      this.pluginContext = new DefaultAdapterPluginContext(pluginID, bindingName, repository);
    }
    catch (CloudApplicationAdapterException e) {
      throw new CloudInvocationException(e);
    }
    this.sessionData = SoapLoginUtil.login(this.cloudInvocationContext, this.loginEndpointURL, this.pluginContext);
    this.organizationID = this.sessionData.getOrganizationID();
  }

  public CloudMessage onMessage(CloudMessage cloudRequestMessage) throws Exception
  {
	  System.out.println("Executing method "+Thread.currentThread().getStackTrace()[1]);
    if (this.isReleased) {
      return null;
    }
    Definition intgWsdlDef = this.cloudInvocationContext.getIntegrationWSDL();
    Document incomingDoc = cloudRequestMessage.getMessagePayloadAsDocument();
    System.out.println("OrganizationID: " + this.organizationID);
    CloudInvocationContext context = this.cloudEndpointFactory.getCloudInvocationContext();
    NodeList organizationIdNode = incomingDoc.getElementsByTagName("OrganizationId");
    String notificationOrganizationId = organizationIdNode.item(0).getChildNodes().item(0).getNodeValue();

    if (this.organizationID != null && this.organizationID.equals(notificationOrganizationId)) {
    	System.out.println("Organization Id Check Passed.");
    } else {
    	System.out.println("Unexpected Organization Id received in Outbound Message.");
    	String errorMessage = CloudUtil.getResourceBundle().getString("ORGID_MISMATCH_IN_OUTBOUND_MESSAGE_ERROR");
    	//throw new CloudInvocationException(errorMessage);
    }

    String AdapterNamespace = intgWsdlDef.getTargetNamespace();
    if (AdapterNamespace == null) {
      AdapterNamespace = context.getIntegrationWSDL().getTargetNamespace();
    }
    
    Element sourceRootElement = incomingDoc.getDocumentElement();
    System.out.println("sourceRootElement: " + sourceRootElement);
    System.out.println("sourceRootElement:localname " + sourceRootElement.getLocalName());
    
    NamespaceManager nsManager = context.getNamespaceManager();
    if (nsManager == null) {
      nsManager = new NamespaceManagerImpl();
      context.setNamespaceManager(nsManager);
    }
    
    nsManager.loadPrefixes(sourceRootElement);
    String AdapterNamespacePrefix = context.getNamespaceManager().getOrCreatePrefix(AdapterNamespace);
    System.out.println("prefix ->" + AdapterNamespacePrefix + ",typeNS ->" + AdapterNamespace);
    NodeList rootElement = sourceRootElement.getChildNodes();
    sourceRootElement = (Element)incomingDoc.renameNode(sourceRootElement, AdapterNamespace, AdapterNamespacePrefix + ":" + sourceRootElement.getLocalName());

    if (rootElement != null) {
      transformNotificationsElements(rootElement, incomingDoc, AdapterNamespace, AdapterNamespacePrefix);
    }
    transformSObjectElements(incomingDoc, AdapterNamespace, AdapterNamespacePrefix, context);
    
    MessageFactory factory = MessageFactory.newInstance();
	SOAPMessage message = factory.createMessage();
	SOAPHeader soapHeader = CloudUtil.createSessionHeader(message, NS, this.sessionData.getSessionId());
	List<MessageHeader> messageHeaderList = SOAPHeaderBuilder.buildFromSOAPHeader(soapHeader);
    cloudRequestMessage.setMessagePayload(sourceRootElement);
    cloudRequestMessage.addAllHeaders(messageHeaderList);
    CloudMessage cloudResponseMessage = null;
    try {
      cloudResponseMessage = this.cloudEndpoint.raiseEvent(cloudRequestMessage);
    }
    catch (Exception e) {
    	System.out.println("Exception caught during processing the Outbound Messaging event:" + e.getMessage());
    }

    try
    {
      cloudRequestMessage.setMessagePayload(handleEventResponse());
    }
    catch (Exception e) {
    	System.out.println("Exception caught while constructing notification response:" + e.getMessage());
    	throw e;
    }
    
    return cloudRequestMessage;
  }

  private static void transformSObjectElements(Document incomingDoc, String AdapterNamespace, String AdapterNamespacePrefix, CloudInvocationContext context)
  {
    Element rootElement = incomingDoc.getDocumentElement();
    NodeList sObjectNodeList = rootElement.getElementsByTagName("zObject");
    if (sObjectNodeList.getLength() != 0) {
      for (int i = 0; i < sObjectNodeList.getLength(); i++) {
        Node sObjectNode = sObjectNodeList.item(i);
        if (sObjectNode.getAttributes() != null) {
          Node attributeNode = sObjectNode.getAttributes().getNamedItemNS("http://www.w3.org/2001/XMLSchema-instance", "type");
          if (attributeNode != null) {
            String objectName = attributeNode.getNodeValue();

            if (objectName.contains(":")) {
              objectName = objectName.substring(objectName.indexOf(":") + 1, objectName.length());
            }
            
            if (AdapterNamespace != null)
            {
              incomingDoc.renameNode(sObjectNode, AdapterNamespace, getQualifiedName(AdapterNamespacePrefix, objectName));
            }
          }
        }

        Map<String, String> jcaProperties = context.getCloudOperationProperties();
        
        String backwardCompatibilityPropertyValue = (String)jcaProperties.get("backwardCompatibility");
        
        if (backwardCompatibilityPropertyValue != null) {
          List<String> parts = Arrays.asList(backwardCompatibilityPropertyValue.split(","));
          if (parts.contains("nsin")) {
            CloudUtil.renameNamespaceRecursive(incomingDoc, sObjectNode, false, AdapterNamespace, AdapterNamespacePrefix);
          }
        }
      }
    }
  }

  private static Document handleEventResponse() throws Exception
  {
	  System.out.println("Executing method "+Thread.currentThread().getStackTrace()[1]);
    Document ackResponse = null;
    ackResponse = convertStringToDocument(MessageFormat.format("change me", new Object[] { "true" }));
    
    return ackResponse;
  }
  
  private static Document convertStringToDocument(String xmlStr) throws Exception
  {
	  System.out.println("Executing method "+Thread.currentThread().getStackTrace()[1]);
    DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
    try
    {
      DocumentBuilder builder = factory.newDocumentBuilder();
      return builder.parse(new InputSource(new StringReader(xmlStr)));
    }
    catch (Exception e)
    {
      throw e;
    }
  }
  
  static void transformNotificationsElements(NodeList childNodeofparent, Document document, String AdapterNamespace, String AdapterNamespacePrefix)
  {
	  System.out.println("Executing method "+Thread.currentThread().getStackTrace()[1]);
    for (int i = 0; i < childNodeofparent.getLength(); i++)
    {
      Node node = childNodeofparent.item(i);
      renameNamespaceRecursive(document, node, AdapterNamespace, AdapterNamespacePrefix);
    }
  }

  private static void renameNamespaceRecursive(Document doc, Node node, String namespace, String namespacePrefix)
  {
	  System.out.println("Executing method "+Thread.currentThread().getStackTrace()[1]);
    if (!node.getNodeName().equalsIgnoreCase("zObject")) {
      if (node.getNodeType() == 1)
      {
        doc.renameNode(node, namespace, getQualifiedName(namespacePrefix, node.getLocalName()));
      }
      NodeList list = node.getChildNodes();
      for (int i = 0; i < list.getLength(); i++) {
        renameNamespaceRecursive(doc, list.item(i), namespace, namespacePrefix);
      }
    }
    else
    {
      doc.renameNode(node, namespace, getQualifiedName(namespacePrefix, node.getLocalName()));
    }
  }

  private static String getQualifiedName(String prefix, String localName)
  {
    String qualifiedName = localName;
    if ((prefix != null) && (prefix.trim().length() > 0)) {
      qualifiedName = prefix + ":" + qualifiedName;
    }
    return qualifiedName;
  }
  

  public void release()
  {
    this.isReleased = true;
  }

  public void run() {}

  protected static NamespaceManager getNamespaceContext(Element element)
  {
    NamespaceManager nsMgr = new NamespaceManagerImpl();
    nsMgr.loadPrefixes(element);
    return nsMgr;
  }

  public void activation()
  {
	  System.out.println("Base url: " + getBaseURL());
    System.out.println("Base SSL url: " + getBaseSSLURL());
    
    try
    {
      Definition endpointWSDL = this.cloudInvocationContext.getIntegrationWSDL();
      setSOAPBindingWSDL(getEndpointWsdl(endpointWSDL));
    }
    catch (CloudInvocationException e) {
    	System.out.println("Exception caught during project Activation:" + e.getMessage());
      this.cloudEndpoint.onException(e);
    } catch (Exception e) {
    	System.out.println("Exception caught during project Activation:" + e.getMessage());
    }
  }

  private String getEndpointWsdl(Definition endpointDef) throws Exception
  {
    endpointDef = CloudAdapterUtil.parseWSDL(endpointDef.getDocumentBaseURI(), CloudAdapterUtil.serializeWSDL(endpointDef));
    String uri = getBaseSSLURL() + getContextURI();
    if (!uri.endsWith("/")) {
      uri = uri + "/";
    }System.out.println("Endpoint URI =>" + uri);
    System.out.println("Endpoint URI =>" + uri);
    Definition modified = CloudAdapterUtil.replaceBindings(endpointDef, uri);
    String wsdl = CloudAdapterUtil.serializeWSDL(modified);
	System.out.println("Endpoint WSDL ==>");
	System.out.println(wsdl);
	System.out.println("<==");
    return wsdl;
  }
  
  public String getSOAPAction()
  {
    return "";
  }

}