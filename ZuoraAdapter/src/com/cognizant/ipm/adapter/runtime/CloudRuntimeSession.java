package com.cognizant.ipm.adapter.runtime;
import java.util.Set;

import oracle.cloud.connector.api.CloudInvocationContext;
import oracle.cloud.connector.api.CloudInvocationException;
import oracle.cloud.connector.api.Session;
import oracle.cloud.connector.api.SessionManager;
import oracle.cloud.connector.impl.ConcurrentSession;

public class CloudRuntimeSession extends ConcurrentSession
{
  private long nextLoginTime;
  private int sessionLength;
  private long sessionLastUsed;
  private CloudInvocationContext context;
  
  public CloudRuntimeSession(CloudRuntimeSessionManager zuoraSessionManager, String sessionID)
  {
    super(zuoraSessionManager, sessionID);
    this.context = zuoraSessionManager.getContext();
    setSessionLastUsed(getEstablishedTime());
  }

  private long getnextLoginTime()
  {
    String sessionStr = (String)getSessionProperty("sessionSecondsValid");
    
    if (sessionStr != null) {
      this.sessionLength = Integer.parseInt(sessionStr);
      this.nextLoginTime = (getSessionLastUsed() + this.sessionLength * 1000);
    }
    return this.nextLoginTime;
  }

  private boolean isConnected()
  {
    this.nextLoginTime = getnextLoginTime();
    return System.currentTimeMillis() < this.nextLoginTime;
  }

  public long getSessionLastUsed()
  {
    return this.sessionLastUsed;
  }

  public void setSessionLastUsed(long sessionLastUsed)
  {
    this.sessionLastUsed = sessionLastUsed;
  }
  
  protected boolean internalInvalidate(SessionManager manager)
    throws CloudInvocationException
  {
    return true;
  }
  
  protected String internalRenew(SessionManager manager)
    throws CloudInvocationException
  {
    Session session = manager.establishSession();
    Set<String> sessionKeys = session.getSessionPropertyKeys();
    for (String key : sessionKeys) {
      setSessionProperty(key, session.getSessionProperty(key));
    }
    String newSessionID = session.getSessionID();
    
    setSessionLastUsed(getEstablishedTime());
    return newSessionID;
  }
  
  protected boolean internalIsValid()
  {
    boolean isConnected = isConnected();
    return isConnected;
  }
}