package com.cognizant.ipm.adapter.util;

import java.io.ByteArrayOutputStream;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.MessageFormat;
import java.util.Map;
import java.util.Properties;

import javax.xml.soap.SOAPMessage;

import oracle.cloud.connector.api.CloudApplicationConnectionFactory;
import oracle.cloud.connector.api.CloudInvocationContext;
import oracle.cloud.connector.api.CloudInvocationException;
import oracle.tip.tools.ide.adapters.cloud.api.plugin.AdapterPluginContext;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

public final class SoapLoginUtil
{
  
  private static byte[] soapXmlForLogin(String username, String password)
    throws UnsupportedEncodingException
  {
	  System.out.println("Executing method "+Thread.currentThread().getStackTrace()[1]);
    return ("<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:api=\"http://api.zuora.com/\">\n" + 
			"   <soapenv:Header/>\n" + 
			"   <soapenv:Body>\n" + 
			"      <api:login>\n" + 
			"         <!--Optional:-->\n" + 
			"         <api:username>"+username+"</api:username>\n" + 
			"         <!--Optional:-->\n" + 
			"         <api:password>"+password+"</api:password>\n" + 
			"      </api:login>\n" + 
			"   </soapenv:Body>\n" + 
			"</soapenv:Envelope>").getBytes("UTF-8");
  }

  public static CloudSessionData login(CloudInvocationContext m_cloudInvocationContext, String endpointURL, AdapterPluginContext pluginContext)
    throws CloudInvocationException
  {
	  System.out.println("Executing method "+Thread.currentThread().getStackTrace()[1]);
    try
    {
      SOAPMessage loginResponseMessage = sendLoginRequest(m_cloudInvocationContext, endpointURL, pluginContext);
      CloudSessionData sessionData = new CloudSessionData();
      Element bodyElement = SOAPUtil.getFirstBodyElement(loginResponseMessage.getSOAPBody());

      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      Document responseDoc = SOAPUtil.processElement(bodyElement, baos);

      String sessionId = null;
      String serverUrl = null;
      String exceptionMessage = null;
      NodeList serverUrlNode = responseDoc.getElementsByTagName("ServerUrl");
      NodeList sessionIdNode = responseDoc.getElementsByTagName("Session");
      NodeList exceptionMessageNodeList = responseDoc.getElementsByTagName("FaultMessage");

      if (sessionIdNode != null && sessionIdNode.getLength() != 0)
      {
        sessionId = sessionIdNode.item(0).getChildNodes().item(0).getNodeValue();
      }
      
      if (serverUrlNode != null && serverUrlNode.getLength() != 0) {
        serverUrl = serverUrlNode.item(0).getChildNodes().item(0).getNodeValue();
      }
      
      if (exceptionMessageNodeList != null && exceptionMessageNodeList.getLength() != 0)
      {
        exceptionMessage = exceptionMessageNodeList.item(0).getChildNodes().item(0).getNodeValue();
      }
      
      if ((sessionId == null) || (serverUrl == null))
      {
        if (exceptionMessage != null) {
        	System.out.println("Login Failed!\n" + exceptionMessage);
        	MessageFormat msgformat = new MessageFormat(CloudRuntimeUtil.getResourceBundle().getString("LOGIN_FAILED"));
        	String[] obj = { exceptionMessage };
        	throw new CloudInvocationException(msgformat.format(obj));
        }
        System.out.println("Login Failed!");
        throw new CloudInvocationException(CloudRuntimeUtil.getResourceBundle().getString("LOGIN_FAILED"));
      }
      URL soapEndpoint = new URL(serverUrl);
      StringBuilder endpoint = new StringBuilder().append(soapEndpoint.getProtocol()).append("://").append(soapEndpoint.getHost());
      if (soapEndpoint.getPort() > 0) {
        endpoint.append(":").append(soapEndpoint.getPort());
      }
      sessionData.setSessionId(sessionId);
      sessionData.setServerURL(soapEndpoint.toString());
      sessionData.setEndpoint(endpoint.toString());
      
      return sessionData;
    } catch (CloudInvocationException e) {
    	throw e;
    } catch (Exception e) {
    	throw new CloudInvocationException(e);
    }
  }

  /**
   * sends the login request
   * @param m_cloudInvocationContext
   * @param endpoint
   * @param pluginContext
   * @return
   * @throws CloudInvocationException
   * @throws Exception
   */
  private static SOAPMessage sendLoginRequest(CloudInvocationContext m_cloudInvocationContext, String endpoint, AdapterPluginContext pluginContext)
    throws CloudInvocationException, Exception
  {
	  System.out.println("Executing method "+Thread.currentThread().getStackTrace()[1]);
    URL endpointURL = new URL(endpoint);
    Map authenticationProperties = m_cloudInvocationContext.getAuthenticationManager().getAuthenticationProperties();
    String username = (String)authenticationProperties.get("username");
    String password = (String)authenticationProperties.get("password");
    if (username == null) {
    	System.out.println(CloudRuntimeUtil.getResourceBundle().getString("UNABLE_TO_GET_USERNAME"));
    	throw new CloudInvocationException(CloudRuntimeUtil.getResourceBundle().getString("UNABLE_TO_GET_USERNAME"));
    }
    
    if (password == null) {
    	System.out.println(CloudRuntimeUtil.getResourceBundle().getString("UNABLE_TO_GET_PASSWORD"));
    	throw new CloudInvocationException(CloudRuntimeUtil.getResourceBundle().getString("UNABLE_TO_GET_PASSWORD"));
    }
    
    Properties connectionProperties = new Properties();
    connectionProperties.putAll(m_cloudInvocationContext.getCloudConnectionProperties());
    SOAPMessage loginRequestMessage = SOAPUtil.loadSOAPMessage(soapXmlForLogin(username, password));
    SOAPMessage loginResponseMessage = SOAPUtil.invoke(loginRequestMessage, endpointURL, connectionProperties, pluginContext);

    if (loginResponseMessage == null) {
    	System.out.println("Unable to get login response from application.");
    	throw new CloudInvocationException(CloudRuntimeUtil.getResourceBundle().getString("EMPTY_SOAP_RESPONSE_ERROR"));
    }
    return loginResponseMessage;
  }

  public static String getSoapURL(CloudInvocationContext m_cloudInvocationContext, CloudApplicationConnectionFactory m_connectionFactory)
    throws MalformedURLException, CloudInvocationException
  {
	  System.out.println("Executing method "+Thread.currentThread().getStackTrace()[1]);
	  String endpointURL = (String)m_connectionFactory.getConnectionFactoryProperties().get("endpointURL");
	  System.out.println("Endpoint URL:" + endpointURL);
	  if (endpointURL == null) {
		  System.out.println("Unable to find endpoint URL in the jca file.");
		  throw new CloudInvocationException(CloudRuntimeUtil.getResourceBundle().getString("UNABLE_TO_FIND_ENDPOINT_URL_PROPERTY"));
	  }
    
	  return new URL(endpointURL).toExternalForm();
  	}
}