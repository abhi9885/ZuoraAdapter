package com.cognizant.ipm.adapter.runtime;

import java.util.Map;

import oracle.cloud.connector.api.CloudApplicationConnection;
import oracle.cloud.connector.api.CloudApplicationConnectionFactory;
import oracle.cloud.connector.api.CloudConnectorException;

public class CloudRuntimeConnectionFactory implements CloudApplicationConnectionFactory
{
  private Map<String, String> m_connectionFactoryProperties;
  
  /**
   * outbound message connector class
   */
  public CloudApplicationConnection getConnection() throws CloudConnectorException
  {
	  System.out.println("Executing method "+Thread.currentThread().getStackTrace()[1]);
    return new CloudRuntimeConnection(this);
  }

  /**
   * Inbound message receiver class
   */
  public String getCloudConnectorClassName()
  {
	  System.out.println("Executing method "+Thread.currentThread().getStackTrace()[1]);
    return AdapterCloudConnector.class.getName();
  }

  public void setConnectionFactoryProperties(Map<String, String> properties)
  {
	  System.out.println("Executing method "+Thread.currentThread().getStackTrace()[1]);
	  System.out.println("properties = "+properties);
	  if(m_connectionFactoryProperties == null) {
		  this.m_connectionFactoryProperties = properties;
	  } else {
		  this.m_connectionFactoryProperties.putAll(properties);
	  }
  }

  public Map<String, String> getConnectionFactoryProperties()
  {
	  System.out.println("Executing method "+Thread.currentThread().getStackTrace()[1]);
    return this.m_connectionFactoryProperties;
  }
}