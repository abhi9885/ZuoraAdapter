package com.cognizant.ipm.adapter.runtime;

import java.util.ArrayList;
import java.util.List;

import oracle.cloud.connector.api.CloudApplicationConnectionFactory;
import oracle.cloud.connector.api.CloudMessageHandler;
import oracle.cloud.connector.api.SessionManager;
import oracle.cloud.connector.impl.AbstractCloudApplicationConnection;

/**
 * handles outbound operations
 * @author Upendar Reddy
 *
 */
public class CloudRuntimeConnection extends AbstractCloudApplicationConnection
{
  private List<CloudMessageHandler> messageHandlers;
  private boolean m_closed;
  private CloudApplicationConnectionFactory m_connectionFactory;
  
  public CloudRuntimeConnection(CloudApplicationConnectionFactory connectionFactory)
  {
	  System.out.println("Executing method "+Thread.currentThread().getStackTrace()[1]);
	  this.m_connectionFactory = connectionFactory;
  }

  public SessionManager getSessionManager()
  {
	  System.out.println("Executing method "+Thread.currentThread().getStackTrace()[1]);
	  return null; // new CloudRuntimeSessionManager();
  }
  
  public String getEndpointType(String targetOperation)
  {
    return "SOAP";
  }

  /**
   * gets the outbound message handlers
   */
  protected List<CloudMessageHandler> getMessageHandlers()
  {
	  System.out.println("Executing method "+Thread.currentThread().getStackTrace()[1]);
    if (this.messageHandlers == null) {
      this.messageHandlers = new ArrayList();
      this.messageHandlers.add(new CloudRuntimeMessageHandler());
    }
    return this.messageHandlers;
  }

  public void close()
  {
	  System.out.println("Executing method "+Thread.currentThread().getStackTrace()[1]);
    this.m_closed = true;
  }

  public boolean isValid()
  {
	  System.out.println("Executing method "+Thread.currentThread().getStackTrace()[1]);
    return !this.m_closed;
  }
}