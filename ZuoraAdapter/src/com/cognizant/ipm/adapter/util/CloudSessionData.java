package com.cognizant.ipm.adapter.util;
public class CloudSessionData { 
  private String sessionId;
  private String serverURL;
  public void setSessionId(String sessionId) { this.sessionId = sessionId; }
  private String endpoint;
  private String organizationID;
  public String getSessionId() { return this.sessionId; }
  
  public void setServerURL(String serverURL)
  {
    this.serverURL = serverURL;
  }
  
  public String getServerURL() {
    return this.serverURL;
  }
  
  public void setEndpoint(String endpoint) {
    this.endpoint = endpoint;
  }
  
  public String getEndpoint() {
    return this.endpoint;
  }
  
  public void setOrganizationID(String organizationID) {
    this.organizationID = organizationID;
  }
  
  public String getOrganizationID() {
    return this.organizationID;
  }
}