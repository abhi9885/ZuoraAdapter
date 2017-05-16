package com.cognizant.ipm.adapter.plugin;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Properties;

import javax.xml.soap.SOAPBody;
import javax.xml.soap.SOAPException;
import javax.xml.soap.SOAPFault;
import javax.xml.soap.SOAPMessage;

import oracle.tip.tools.ide.adapters.cloud.api.connection.AbstractCloudConnection;
import oracle.tip.tools.ide.adapters.cloud.api.connection.AuthenticationScheme;
import oracle.tip.tools.ide.adapters.cloud.api.connection.PingStatus;
import oracle.tip.tools.ide.adapters.cloud.api.connection.UsernamePasswordScheme;
import oracle.tip.tools.ide.adapters.cloud.api.plugin.AdapterPluginContext;
import oracle.tip.tools.ide.adapters.cloud.api.service.SOAPHelperService;
import oracle.tip.tools.ide.adapters.cloud.api.service.WSDLHelperService;

import com.cognizant.ipm.adapter.util.AdapterUtil;

public class CloudAdapterConnection extends AbstractCloudConnection
{
  private AdapterPluginContext context;
  private UsernamePasswordScheme authenticationScheme;
  
  public CloudAdapterConnection(String string)
  {
	  super(string);
	  System.out.println("Executing method "+Thread.currentThread().getStackTrace()[1]);
  }
  
  public CloudAdapterConnection(AdapterPluginContext context) {
	  super(context.getReferenceBindingName());
	  this.context = context;
	  System.out.println("Executing method "+Thread.currentThread().getStackTrace()[1]);
  }
  
  public AuthenticationScheme getAuthenticationScheme()
  {
	  System.out.println("Executing method "+Thread.currentThread().getStackTrace()[1]);
	  if (this.authenticationScheme == null) {
		  this.authenticationScheme = new UsernamePasswordScheme(this.context, this);
	  }
	  return this.authenticationScheme;
  }
  
  public PingStatus ping()
  {
	  System.out.println("Executing method "+Thread.currentThread().getStackTrace()[1]);
    try {
      SOAPHelperService soapHelper = (SOAPHelperService)this.context.getServiceRegistry().getService(SOAPHelperService.class);
      WSDLHelperService wsdlHelper = (WSDLHelperService)this.context.getServiceRegistry().getService(WSDLHelperService.class);
      
      String wsdlURL = getConnectionProperties().getProperty("targetWSDLURL");
      URL endpointURL = (URL)this.context.getContextObject("connectionUrl");
      
      if (endpointURL == null) {
        endpointURL = wsdlHelper.getEndpointAddressFromWSDL(new URL(wsdlURL), getConnectionProperties());
        this.context.setContextObject("connectionUrl", endpointURL);
      }
      
      SOAPMessage message = createRequestPingMessage(soapHelper);
      SOAPMessage response = soapHelper.sendSOAP(message, getConnectionProperties(), endpointURL);
      
      return getPingStatusFromResponse(response);
    }
    catch (MalformedURLException e) {
      return new PingStatus(e);
    } catch (IOException e) {
      return new PingStatus(e);
    } catch (SOAPException e) {
      e.printStackTrace();
      return new PingStatus(e);
    } catch (Exception e) {
      return new PingStatus(e);
    }
  }

  /**
   * create sample request for your service to ping
   * @param service
   * @return
   * @throws Exception
   */
  private SOAPMessage createRequestPingMessage(SOAPHelperService service) throws Exception
  {
	  System.out.println("Executing method "+Thread.currentThread().getStackTrace()[1]);
	  if (this.authenticationScheme == null) {
		  throw new Exception("No authentication properties set.");
	  }
	  String username = this.authenticationScheme.getUserName();
	  String password = this.authenticationScheme.getPassword();
	  Properties props = getConnectionProperties();
	  SOAPMessage message = null;
	  try {
		  message = AdapterUtil.createRequestSOAPMessage(username, password);
	  	} catch (Exception e) { e.printStackTrace();
	  }
	  return message;
  }

  private PingStatus getPingStatusFromResponse(SOAPMessage response)
    throws SOAPException
  {
	  System.out.println("Executing method "+Thread.currentThread().getStackTrace()[1]);
    SOAPBody soapBody = response.getSOAPBody();
    SOAPFault soapFault = soapBody.getFault();
    
    PingStatus status = null;
    if (soapFault != null) {
      String errorMessage = soapFault.getFaultString();
      String faultCode = soapFault.getFaultCode();
      status = new PingStatus(errorMessage, faultCode);
    } else {
      status = PingStatus.SUCCESS_STATUS;
    }
    return status;
  }
}