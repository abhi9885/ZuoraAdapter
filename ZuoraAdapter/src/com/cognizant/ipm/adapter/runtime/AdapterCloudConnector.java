package com.cognizant.ipm.adapter.runtime;

import oracle.cloud.connector.api.CloudApplicationConnectionFactory;
import oracle.cloud.connector.api.CloudConnector;
import oracle.cloud.connector.api.CloudEndpointFactory;
import oracle.cloud.connector.api.CloudMessageReceiver;

public class AdapterCloudConnector implements CloudConnector
{
  public String getCloudConnectorName()
  {
    return "zuora Cloud Connector";
  }

  public String getCloudConnectorDescription()
  {
    return "zuora Cloud Connector";
  }

  public String getCloudConnectorVersion()
  {
    return "1.0.0";
  }

  public String getCloudConnectorVendor()
  {
    return "Oracle";
  }

  public String getCloudConnectorBackendName()
  {
    return "zuora";
  }

  public String getCloudConnectorBackendVersions()
  {
    return "1,2,3";
  }

  public String getCloudConnectorBackendVendor()
  {
    return "zuora";
  }
  
  public CloudMessageReceiver eventListenerActivation(CloudApplicationConnectionFactory connectionFactory, CloudEndpointFactory endpointFactory)
    throws Exception
  {
	  System.out.println("Executing method "+Thread.currentThread().getStackTrace()[1]);
	  System.out.println("endpointFactory.getCloudInvocationContext().getTargetOperationName() = "+endpointFactory.getCloudInvocationContext().getTargetOperationName());
    CloudMessageReceiver messageReceiver = null;
    if ("notifications".equalsIgnoreCase(endpointFactory.getCloudInvocationContext().getTargetOperationName()))
    {
      OutboundMessageReceiver outboundMessageReceiver = new OutboundMessageReceiver(connectionFactory, endpointFactory);
      outboundMessageReceiver.setInboundSecurityPolicyDisabled(true);
      messageReceiver = outboundMessageReceiver;
    }
    else {
    	messageReceiver = new InboundMessageReceiver(connectionFactory, endpointFactory);
      ((InboundMessageReceiver)messageReceiver).preSetupForEventSubscription();
    }
    
    endpointFactory.getCloudWorkManager().start(messageReceiver);
    return messageReceiver;
  }

  public void eventListenerDeactivation(CloudMessageReceiver messageReceiver)
  {
	  System.out.println("Executing method "+Thread.currentThread().getStackTrace()[1]);
	  messageReceiver.release();
  }
}