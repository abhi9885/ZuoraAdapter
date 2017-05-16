package com.cognizant.ipm.adapter.util;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URL;
import java.util.Map;
import java.util.Properties;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.soap.MessageFactory;
import javax.xml.soap.SOAPBody;
import javax.xml.soap.SOAPException;
import javax.xml.soap.SOAPMessage;

import oracle.cloud.adapter.api.util.NetworkUtil;
import oracle.tip.tools.ide.adapters.cloud.api.plugin.AdapterPluginContext;
import oracle.tip.tools.ide.adapters.cloud.api.plugin.CloudApplicationAdapterException;
import oracle.tip.tools.ide.adapters.cloud.api.service.AdapterPluginServiceRegistry;
import oracle.tip.tools.ide.adapters.cloud.api.service.LoggerService;
import oracle.tip.tools.ide.adapters.cloud.api.service.SOAPHelperService;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class SOAPUtil
{
  public static Element getFirstBodyElement(SOAPBody body)
  {
	  System.out.println("Executing method "+Thread.currentThread().getStackTrace()[1]);
    NodeList nl = body.getChildNodes();
    for (int i = 0; i < nl.getLength(); i++) {
      Node n = nl.item(i);
      if ((n instanceof Element)) {
        return (Element)n;
      }
    }
    return null;
  }
  
  public static Document processElement(Element el, OutputStream os) throws Exception
  {
	  System.out.println("Executing method "+Thread.currentThread().getStackTrace()[1]);
    Document doc = createNewDocument();
    Node imported = doc.importNode(el, true);
    doc.appendChild(imported);
    
    return doc;
  }
  
  private static Document createNewDocument() throws ParserConfigurationException
  {
	  System.out.println("Executing method "+Thread.currentThread().getStackTrace()[1]);
    DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
    return builder.newDocument();
  }
  
  public static SOAPMessage loadSOAPMessage(byte[] requestMessage) throws SOAPException
  {
	  System.out.println("Executing method "+Thread.currentThread().getStackTrace()[1]);
    MessageFactory messageFactory = MessageFactory.newInstance();
    
    SOAPMessage message = null;
    ByteArrayInputStream bis = null;
    try {
      bis = new ByteArrayInputStream(requestMessage);
      message = messageFactory.createMessage(null, bis);
    }
    catch (IOException e) {
      e.printStackTrace();
    }
    return message;
  }

  public static SOAPMessage invoke(SOAPMessage message, URL endpointURL, Properties connProps, AdapterPluginContext pluginContext)
    throws CloudApplicationAdapterException, SOAPException, IOException
  {
	  System.out.println("Executing method "+Thread.currentThread().getStackTrace()[1]);
    AdapterPluginServiceRegistry registry = pluginContext.getServiceRegistry();
    SOAPHelperService soapHelper = (SOAPHelperService)registry.getService(SOAPHelperService.class);
    System.out.println("Endpoint URL - " + endpointURL);
    NetworkUtil.updateConnectionProperties(connProps);
    StringBuffer props = new StringBuffer();
    for (Map.Entry entry : connProps.entrySet()) {
      props.append(entry.getKey().toString() + " - " + entry.getValue().toString() + "\n");
    }
    
    System.out.println("Connection Properties:\n" + props.toString());
    
    SOAPMessage response = soapHelper.sendSOAP(message, connProps, endpointURL);
    return response;
  }
}