/**
 * CloudRuntimeUtil.java
 * @created Jan 19, 2017
 * @author upendra
 * 
 */
package com.cognizant.ipm.adapter.util;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Set;

import javax.wsdl.Definition;
import javax.wsdl.Port;
import javax.wsdl.Service;
import javax.wsdl.extensions.soap.SOAPAddress;
import javax.wsdl.extensions.soap12.SOAP12Address;
import javax.xml.namespace.QName;

import oracle.cloud.connector.api.CloudAdapterLoggingService;
import oracle.cloud.connector.api.CloudInvocationContext;
import oracle.cloud.connector.api.CloudInvocationException;
import oracle.cloud.connector.api.MessageHeader;
import oracle.cloud.connector.api.Session;
import oracle.cloud.connector.impl.CloudAdapterUtil;
import oracle.cloud.connector.impl.SOAPHeaderBuilder;

/**
 * @author upendra
 *
 */
public class CloudRuntimeUtil {
	
	  public static String getNamespace(CloudInvocationContext context) throws CloudInvocationException
	  {
	    String endpointServerUrl = null;
	    endpointServerUrl = getEndpointURLFromWSDL(context);
	    String NS; 
	  if (endpointServerUrl.contains("/c/"))
	    {
	      NS = getNamespace("Enterprise");
	    } else {
	      NS = getNamespace("Partner");
	    }
	    return NS;
	  }
	  
	  public static String getNamespace(String WSDLTypeStr)
	  {
		  return "http://api.zuora.com";
	  }
	  
	  public static ResourceBundle getResourceBundle()
	  {
	    ResourceBundle resourceBundle = ResourceBundle.getBundle("com.cognizant.ipm.adapter.runtime.ConnectorResourceBundle");
	    return resourceBundle;
	  }

	  public static String getEndpointURLFromWSDL(CloudInvocationContext context)
	    throws CloudInvocationException
	  {
	    CloudAdapterLoggingService loggingService = context.getLoggingService();
	    Definition def = getTargetDefinition(context);
	    Service targetService = getTargetWSDLService(def, context);
	    if (targetService == null) {
	      loggingService.logError(getResourceBundle().getString("UNABLE_TO_FIND_SERVICE"));
	      
	      throw new CloudInvocationException(getResourceBundle().getString("UNABLE_TO_FIND_SERVICE"));
	    }
	    
	    Port targetPort = getTargetWSDLPort(def, targetService, context);
	    if (targetPort == null) {
	      loggingService.logError(getResourceBundle().getString("UNABLE_TO_FIND_PORT") + targetService.getQName().toString());
	      throw new CloudInvocationException(getResourceBundle().getString("UNABLE_TO_FIND_PORT") + targetService.getQName().toString());
	    }

	    String address = null;
	    List extensions = targetPort.getExtensibilityElements();
	    Iterator i$ = extensions.iterator();
	    
	    while (i$.hasNext())
	    {
	      Object o = i$.next();
	      if ((o instanceof SOAPAddress)) {
	        address = ((SOAPAddress)o).getLocationURI();
	        break;
	      }
	      if ((o instanceof SOAP12Address))
	        address = ((SOAP12Address)o).getLocationURI();
	    }
	    return address;
	  }

	  private static Port getTargetWSDLPort(Definition def, Service service, CloudInvocationContext context)
	  {
	    CloudAdapterLoggingService loggingService = context.getLoggingService();
	    Port targetPort = null;
	    String portName = (String)context.getCloudConnectionProperties().get("targetPort");
	    
	    if (portName != null) {
	      targetPort = service.getPort(portName);
	    } else {
	      Map allPorts = service.getPorts();
	      if ((allPorts != null) && (allPorts.size() > 0)) {
	        portName = (String)allPorts.keySet().toArray()[0];
	        targetPort = service.getPort(portName);
	      }
	    }
	    return targetPort;
	  }

	  private static Service getTargetWSDLService(Definition def, CloudInvocationContext context)
	  {
	    CloudAdapterLoggingService loggingService = context.getLoggingService();
	    Service targetService = null;
	    String serviceName = (String)context.getCloudConnectionProperties().get("targetService");
	    
	    if (serviceName != null) {
	      QName serviceQName = new QName(def.getTargetNamespace(), serviceName);
	      
	      targetService = def.getService(serviceQName);
	    } else {
	      Map services = def.getServices();
	      if ((services != null) && (services.size() > 0)) {
	        Set serviceKeys = services.keySet();
	        QName serviceQName = (QName)serviceKeys.toArray()[0];
	        targetService = def.getService(serviceQName);
	      }
	    }
	    return targetService;
	  }

	  private static Definition getTargetDefinition(CloudInvocationContext context)
	    throws CloudInvocationException
	  {
	    Definition def = (Definition)context.getContextObject("wsdlDefinition");
	    if (def == null) {
	      def = CloudAdapterUtil.parseDefinition((String)context.getCloudConnectionProperties().get("targetWSDLURL"), context);
	      context.setContextObject("wsdlDefinition", def);
	    }
	    return def;
	  }
	  
	  public static MessageHeader createSessionHeader(String NS, CloudInvocationContext context) throws CloudInvocationException
	  {
	    SOAPHeaderBuilder builder = null;
	    QName sessionIdHeaderName = new QName(NS, "SessionHeader", "urn");
	    QName sessionId = new QName(NS, "sessionId");
	    Session session = context.getSession();
	    String sessionIdValue = session.getSessionID();
	    builder = new SOAPHeaderBuilder(sessionIdHeaderName, true, null);
	    MessageHeader messageHeader = builder.createHeader(sessionId, sessionIdValue);
	    builder.addChild(messageHeader);
	    
	    return builder.build();
	  }
}
