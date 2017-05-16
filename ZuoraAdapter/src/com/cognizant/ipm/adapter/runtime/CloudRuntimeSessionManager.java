package com.cognizant.ipm.adapter.runtime;

import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import oracle.cloud.connector.api.CloudAdapterLoggingService;
import oracle.cloud.connector.api.CloudInvocationContext;
import oracle.cloud.connector.api.CloudInvocationException;
import oracle.cloud.connector.api.CloudMessage;
import oracle.cloud.connector.api.CloudMessageFactory;
import oracle.cloud.connector.api.Session;
import oracle.cloud.connector.impl.AbstractRemoteSessionManager;
import oracle.cloud.connector.impl.HTTPHeaderBuilder;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import com.cognizant.ipm.adapter.util.CloudRuntimeUtil;
import com.cognizant.ipm.adapter.util.CloudUtil;

public class CloudRuntimeSessionManager extends AbstractRemoteSessionManager
{
  protected CloudMessage getSessionRequestMessage() throws CloudInvocationException
  {
	  System.out.println("Executing method "+Thread.currentThread().getStackTrace()[1]);
    CloudInvocationContext context = getContext();
    CloudAdapterLoggingService loggingService = context.getLoggingService();
    Map connectionProperties = context.getAuthenticationManager().getAuthenticationProperties();
	System.out.println("connectionProperties = "+connectionProperties);
    boolean isCustomWSDL = false;
    if ("Custom WSDL".equals(context.getCloudOperationProperties().get("outboundWSDLType")))
    {
      isCustomWSDL = true;
    }
    if (isCustomWSDL) {
      String serverUrl = CloudRuntimeUtil.getEndpointURLFromWSDL(context);
      context.setContextObject("endpointURL", serverUrl);
    }
    
    String userName = (String)connectionProperties.get("username");
    String password = (String)connectionProperties.get("password");
    if (userName == null) {
      loggingService.logError(CloudRuntimeUtil.getResourceBundle().getString("UNABLE_TO_GET_USERNAME"));
      throw new CloudInvocationException(CloudRuntimeUtil.getResourceBundle().getString("UNABLE_TO_GET_USERNAME"));
    }
    
    if (password == null) {
      loggingService.logError(CloudRuntimeUtil.getResourceBundle().getString("UNABLE_TO_GET_PASSWORD"));
      throw new CloudInvocationException(CloudRuntimeUtil.getResourceBundle().getString("UNABLE_TO_GET_PASSWORD"));
    }
    
    String cloudNS = CloudRuntimeUtil.getNamespace(context);
    Document doc = createLoginRequestMessage(cloudNS, userName, password, context);
    CloudMessage message = CloudMessageFactory.newInstance().createCloudMessage(doc);
    setHttpHeaderInLoginRequest(message);
    //CloudUtil.logMethodEnding(CloudRuntimeSessionManager.class.getName(), "getSessionRequestMessage()", loggingService);
    return message;
  }
  
  private void setHttpHeaderInLoginRequest(CloudMessage message) {
	  System.out.println("Executing method "+Thread.currentThread().getStackTrace()[1]);
    HTTPHeaderBuilder headerBuilder = new HTTPHeaderBuilder();
    message.addMessageHeader(headerBuilder.setName("com.sun.xml.internal.ws.request.timeout").addValue("10000").build());
  }

  private Document createLoginRequestMessage(String cloudNS, String userName, String password, CloudInvocationContext context)
    throws CloudInvocationException
  {
	  System.out.println("Executing method "+Thread.currentThread().getStackTrace()[1]);
    CloudAdapterLoggingService loggingService = context.getLoggingService();
    //CloudUtil.logMethodBeginning(CloudRuntimeSessionManager.class.getName(), "createLoginRequestMessage()", loggingService);
    DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
    DocumentBuilder docBuilder = null;
    try {
      docBuilder = docFactory.newDocumentBuilder();
    } catch (ParserConfigurationException e) {
      loggingService.logError(CloudRuntimeUtil.getResourceBundle().getString("PARSER_CONFIGURATION_ERROR_LOGIN"));
      
      throw new CloudInvocationException(CloudRuntimeUtil.getResourceBundle().getString("PARSER_CONFIGURATION_ERROR_LOGIN"));
    }

    Document doc = docBuilder.newDocument();
    Element rootElement = doc.createElementNS(cloudNS, "urn:login");
    doc.appendChild(rootElement);
    Element userNameElement = doc.createElement("username");
    userNameElement.appendChild(doc.createTextNode(userName));
    rootElement.appendChild(userNameElement);
    Element passwordElement = doc.createElement("password");
    passwordElement.appendChild(doc.createTextNode(password));
    rootElement.appendChild(passwordElement);
    return doc;
  }

  protected Session createSessionInstance(CloudMessage sessionResponse)
    throws CloudInvocationException
  {
	  System.out.println("Executing method "+Thread.currentThread().getStackTrace()[1]);
    CloudInvocationContext context = getContext();
    CloudAdapterLoggingService loggingService = context.getLoggingService();

    boolean isCustomWSDL = false;
    if ("Custom WSDL".equals(context.getCloudOperationProperties().get("outboundWSDLType")))
    {

      isCustomWSDL = true;
    }
    
    NodeList serverUrlNode = null;
    NodeList sessionIdNode = null;
    NodeList sessionSecondsValidNode = null;
    Document doc = sessionResponse.getMessagePayloadAsDocument();
    serverUrlNode = doc.getElementsByTagName("ServerUrl");
    sessionIdNode = doc.getElementsByTagName("Session");
    sessionSecondsValidNode = doc.getElementsByTagName("sessionSecondsValid");
    Session session = null;
    String serverUrl = serverUrlNode.item(0).getChildNodes().item(0).getNodeValue();

    if (sessionIdNode.getLength() != 0)
    {
      String sessionId = sessionIdNode.item(0).getChildNodes().item(0).getNodeValue();
      session = new CloudRuntimeSession(this, sessionId);
      if (isCustomWSDL) {
        loggingService.logInfo("Inside custom WSDL flow");
        String endpointURLForCustomWSDL = (String)context.getCloudConnectionProperties().get("customWSDLEndpointUrl");
        String instanceDomainServerURL = CloudUtil.getUrlDomainName(serverUrl);
        String instanceDomainCustomWSDL = CloudUtil.getUrlDomainName(endpointURLForCustomWSDL);
        if ((instanceDomainServerURL != null) && (instanceDomainCustomWSDL != null))
        {
          if (!instanceDomainServerURL.equalsIgnoreCase(instanceDomainCustomWSDL))
          {
            endpointURLForCustomWSDL = endpointURLForCustomWSDL.replace(instanceDomainCustomWSDL, instanceDomainServerURL);
            loggingService.logInfo("Overriding hostname information in custom WSDL ednpoint URL from login response URL");
          }
          
          session.setSessionProperty("endpointURL", endpointURLForCustomWSDL);
          context.setContextObject("endpointURL", endpointURLForCustomWSDL);
        }
      }
      else if (serverUrlNode != null && serverUrlNode.getLength() != 0) {
        loggingService.logInfo("Inside Enterprise WSDL flow");
        session.setSessionProperty("endpointURL", serverUrl);
        context.setContextObject("endpointURL", serverUrl);
      }
      
      if (sessionSecondsValidNode != null && sessionSecondsValidNode.getLength() != 0) {
        String sessionSecondsValid = sessionSecondsValidNode.item(0).getChildNodes().item(0).getNodeValue();
        session.setSessionProperty("sessionSecondsValid", sessionSecondsValid);
      }
    }

    return session;
  }

  public boolean isGlobal()
  {
    return true;
  }

  public String getSessionKeyPrefix() throws CloudInvocationException
  {
	  System.out.println("Executing method "+Thread.currentThread().getStackTrace()[1]);
    CloudInvocationContext cloudInvocationContext = getContext();
    String endpointUrl = null;
    CloudAdapterLoggingService loggingService = cloudInvocationContext.getLoggingService();
    try
    {
      boolean isCustomWSDL = false;
      if ("Custom WSDL".equals(cloudInvocationContext.getCloudOperationProperties().get("outboundWSDLType")))
      {
        isCustomWSDL = true;
      }
      
      if (isCustomWSDL) {
        endpointUrl = (String)cloudInvocationContext.getCloudConnectionProperties().get("customWSDLEndpointUrl");
      }
      else
      {
        endpointUrl = CloudRuntimeUtil.getEndpointURLFromWSDL(getContext());
      }
    }
    catch (CloudInvocationException e)
    {
      loggingService.logError(e.getMessage(), e);
      throw e;
    }
    loggingService.logInfo("Endpoint URL for Session Key Prefix " + endpointUrl);
    
    return endpointUrl;
  }
}