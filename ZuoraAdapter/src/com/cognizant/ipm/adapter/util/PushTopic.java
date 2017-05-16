package com.cognizant.ipm.adapter.util;

import java.io.ByteArrayOutputStream;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Map;
import java.util.Properties;

import javax.xml.soap.SOAPMessage;

import oracle.cloud.connector.api.CloudAdapterLoggingService;
import oracle.cloud.connector.api.CloudInvocationContext;
import oracle.cloud.connector.api.CloudInvocationException;
import oracle.tip.tools.ide.adapters.cloud.api.plugin.AdapterPluginContext;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class PushTopic
{
  private String topicName;
  private String query;
  private String apiVersion;
  private String notificationsForOperations;
  private String inboundRequestFields;
  private String sessionId;
  private String notifyForOperationCreate = "false";
  private String notifyForOperationDelete = "false";
  private String notifyForOperationUndelete = "false";
  private String notifyForOperationUpdate = "false";
  private String notifyForFields;
  private String endpoint;
  private String selectedObject;
  CloudAdapterLoggingService m_logger;
  Properties connProps = null;
  AdapterPluginContext pluginContext;
  ArrayList<PushtopicDetails> pushtopicObjectsListInzuora;
  private PushtopicDetails currentPushtopicObjectDetails;
  
  public PushTopic(CloudInvocationContext m_cloudInvocationContext, CloudSessionData sessionData, AdapterPluginContext pluginContext) {
    this.m_logger = m_cloudInvocationContext.getLoggingService();
    this.connProps = new Properties();
    this.pluginContext = pluginContext;
    this.connProps.putAll(m_cloudInvocationContext.getCloudConnectionProperties());
    String loginSessionId = sessionData.getSessionId();
    String serverURL = sessionData.getServerURL();
    Map cloudOperationProperties = m_cloudInvocationContext.getCloudOperationProperties();
    this.query = ((String)cloudOperationProperties.get("query"));
    this.m_logger.logDebug("query:" + this.query);
    this.selectedObject = getSelectedObjectFromPushtopicQuery();
    this.notificationsForOperations = ((String)cloudOperationProperties.get("notifyForOperations"));
    this.m_logger.logDebug("notificationsForOperations:" + this.notificationsForOperations);
    this.inboundRequestFields = ((String)cloudOperationProperties.get("inboundRequestFields"));
    this.m_logger.logDebug("inboundRequestFields:" + this.inboundRequestFields);
    this.notifyForFields = ((String)cloudOperationProperties.get("notifyForFields"));
    this.m_logger.logDebug("notifyForFields:" + this.notifyForFields);
    this.sessionId = loginSessionId;
    this.endpoint = serverURL;
    this.apiVersion = m_cloudInvocationContext.getCloudConnectionProperties().get("applicationVersion").toString();
    this.m_logger.logDebug("apiVersion:" + this.apiVersion);
    setNotification(this.notificationsForOperations);
    this.currentPushtopicObjectDetails = new PushtopicDetails();
    this.currentPushtopicObjectDetails.setApiVersion(this.apiVersion);
    this.currentPushtopicObjectDetails.setNotifyForFields(this.notifyForFields);
    this.currentPushtopicObjectDetails.setNotifyForOperationCreate(this.notifyForOperationCreate);
    this.currentPushtopicObjectDetails.setNotifyForOperationDelete(this.notifyForOperationDelete);
    this.currentPushtopicObjectDetails.setNotifyForOperationUndelete(this.notifyForOperationUndelete);
    this.currentPushtopicObjectDetails.setNotifyForOperationUpdate(this.notifyForOperationUpdate);
    this.currentPushtopicObjectDetails.setQuery(this.query);
  }
  
  private String getPushtopicNameToBeCreated()
  {
    String pushtopicNameToBeCreated = "Oracle_" + this.selectedObject.substring(0, 3);
    String topicNameInzuora = null;
    String suffixedNumberInExistingPushtopic = null;
    long maxNumber = 0L;
    int numberLength = -1;
    for (PushtopicDetails pushtopicDetails : this.pushtopicObjectsListInzuora) {
      topicNameInzuora = pushtopicDetails.getName();
      if (topicNameInzuora.startsWith(pushtopicNameToBeCreated)) {
        suffixedNumberInExistingPushtopic = topicNameInzuora.substring(pushtopicNameToBeCreated.length());
        this.m_logger.logDebug("suffixed value in existing pushtopic:" + suffixedNumberInExistingPushtopic);

        if (suffixedNumberInExistingPushtopic.length() != 25) {
          boolean isValidNumber = true;
          long number = 0L;
          try {
            number = Integer.parseInt(suffixedNumberInExistingPushtopic);
          }
          catch (NumberFormatException e)
          {
            isValidNumber = false;
          }
          if ((isValidNumber) && 
            (number > maxNumber)) {
            numberLength = suffixedNumberInExistingPushtopic.length();
            
            maxNumber = number;
          }
        }
      }
    }

    if (maxNumber != 0L) {
      this.m_logger.logDebug("maximum suffixed number in existing pushtopics:" + maxNumber);
      String intValueToBeAppended = CloudUtil.next(String.valueOf(maxNumber));
      intValueToBeAppended = String.format("%0" + numberLength + "d", new Object[] { Long.valueOf(intValueToBeAppended) });
      pushtopicNameToBeCreated = pushtopicNameToBeCreated + intValueToBeAppended;
    }
    else {
      pushtopicNameToBeCreated = pushtopicNameToBeCreated + "00001";
    }
    
    this.m_logger.logDebug("pushtopic name to be created:" + pushtopicNameToBeCreated);
    return pushtopicNameToBeCreated;
  }
  
  private String getSelectedObjectFromPushtopicQuery()
  {
    String queryAfterFrom = this.query.substring(this.query.indexOf(" FROM ") + " FROM ".length());
    if (queryAfterFrom.contains(" ")) {
      return queryAfterFrom.substring(0, queryAfterFrom.indexOf(" "));
    }
    return queryAfterFrom;
  }

  public void setNotification(String OpNotification)
  {
    if (OpNotification != null) {
      if (OpNotification.equalsIgnoreCase("Create"))
      {
        this.notifyForOperationCreate = "true";
      } else if (OpNotification.equalsIgnoreCase("Update"))
      {
        this.notifyForOperationUpdate = "true";
      } else if (OpNotification.equalsIgnoreCase("Delete"))
      {
        this.notifyForOperationDelete = "true";
      } else if (OpNotification.equalsIgnoreCase("Undelete"))
      {
        this.notifyForOperationUndelete = "true"; }
    }
  }
  

  public String createPushtopicInzuora()
    throws CloudInvocationException, Exception
  {
    String status = null;
    String topicName = getPushtopicNameToBeCreated();
    setTopicName(topicName);
    status = sendRequestToCreatePushtopic();
    
    this.m_logger.logDebug("Response of create operation:" + status);
    
    while (status.equals("DUPLICATE_VALUE")) {
      String suffixedIntValueInPushtopicName = topicName.substring(10);
      int number = 1;
      int suffixedNumberLength = suffixedIntValueInPushtopicName.length();
      try {
        number = Integer.parseInt(suffixedIntValueInPushtopicName);
      } catch (NumberFormatException e) {
        suffixedNumberLength = "000001".length();
      }
      number += 1;
      topicName = topicName.substring(0, 10) + String.format(new StringBuilder().append("%0").append(suffixedNumberLength).append("d").toString(), new Object[] { Long.valueOf(number) });
      setTopicName(topicName);
      status = sendRequestToCreatePushtopic();
      this.m_logger.logDebug("Response of create operation:" + status);
    }
    return status;
  }

  private String sendRequestToCreatePushtopic()
    throws CloudInvocationException, Exception
  {
    SOAPMessage createRequestMessage = SOAPUtil.loadSOAPMessage(getSoapXmlForCreatingPushTopic());
    SOAPMessage createResponseMessage = SOAPUtil.invoke(createRequestMessage, new URL(this.endpoint), this.connProps, this.pluginContext);
    if (createResponseMessage == null) {
      this.m_logger.logError("Unable to get create() response from zuora.");
      throw new CloudInvocationException(CloudUtil.getResourceBundle().getString("EMPTY_SOAP_RESPONSE_ERROR"));
    }
    Element bodyElement = SOAPUtil.getFirstBodyElement(createResponseMessage.getSOAPBody());
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    Document responseDoc = SOAPUtil.processElement(bodyElement, baos);
    NodeList createResponseNodelist = responseDoc.getElementsByTagName("createResponse");
    if (createResponseNodelist.getLength() > 0) {
      NodeList successNodeList = responseDoc.getElementsByTagName("success");
      NodeList messageNodeList = responseDoc.getElementsByTagName("message");
      NodeList statusCodeNodelist = responseDoc.getElementsByTagName("statusCode");
      String success = successNodeList.item(0).getChildNodes().item(0).getNodeValue().toString();

      if (success.equalsIgnoreCase("true")) {
        this.m_logger.logDebug("Pushtopic created. Success:" + success);
        return "SUCCESS";
      }
      String statusCode = statusCodeNodelist.item(0).getChildNodes().item(0).getNodeValue().toString();
      String errorMessage = messageNodeList.item(0).getChildNodes().item(0).getNodeValue().toString();
      this.m_logger.logDebug("Pushtopic not created. Message:" + errorMessage);
      if (statusCode.equalsIgnoreCase("DUPLICATE_VALUE"))
      {
        return "DUPLICATE_VALUE";
      }
      return errorMessage;
    }

    String fault = null;
    NodeList faultStringNodeList = responseDoc.getElementsByTagName("faultstring");
    
    if (faultStringNodeList.getLength() > 0) {
      fault = faultStringNodeList.item(0).getChildNodes().item(0).getNodeValue();
      
      this.m_logger.logDebug("Received Fault while creating pushtopic: " + fault);
    }
    return fault;
  }
  

  private byte[] getSoapXmlForCreatingPushTopic()
    throws UnsupportedEncodingException
  {
    String queryString = this.query;
    queryString = CloudStringUtil.escapeXML(queryString, "encode");
    return ("").getBytes("UTF-8");
  }

  private byte[] getSoapXmlToQueryPushTopicDetails()
    throws UnsupportedEncodingException
  {
    return ("").getBytes("UTF-8");
  }

  private byte[] getSoapXmlToUpdatePushTopic(String id)
    throws UnsupportedEncodingException
  {
    return ("").getBytes("UTF-8");
  }

  public String getTopicName()
  {
    return this.topicName;
  }
  
  public void setTopicName(String newTopicName) {
    this.topicName = newTopicName;
  }
  
  private ArrayList<PushtopicDetails> queryExistingPushtopics()
    throws CloudInvocationException, Exception
  {
    this.pushtopicObjectsListInzuora = new ArrayList();
    SOAPMessage queryRequestMessage = SOAPUtil.loadSOAPMessage(getSoapXmlToQueryPushTopicDetails());
    SOAPMessage queryResponseMessage = SOAPUtil.invoke(queryRequestMessage, new URL(this.endpoint), this.connProps, this.pluginContext);
    if (queryResponseMessage == null) {
      this.m_logger.logError("Unable to get query() response.");
      throw new CloudInvocationException(CloudUtil.getResourceBundle().getString("EMPTY_SOAP_RESPONSE_ERROR"));
    }
    Element bodyElement = SOAPUtil.getFirstBodyElement(queryResponseMessage.getSOAPBody());
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    Document doc = SOAPUtil.processElement(bodyElement, baos);
    NodeList queryResponseNodelist = doc.getElementsByTagName("queryResponse");
    
    if (queryResponseNodelist.getLength() > 0) {
      NodeList recordsNodeList = doc.getElementsByTagName("records");
      if (recordsNodeList.getLength() > 0) {
        for (int i = 0; i < recordsNodeList.getLength(); i++) {
          Node record = recordsNodeList.item(i);
          NodeList recordChilds = record.getChildNodes();
          String fieldName = null;
          String fieldValue = null;
          PushtopicDetails sfPushtopicDetails = new PushtopicDetails();
          for (int j = 0; j < recordChilds.getLength(); j++) {
            fieldName = recordChilds.item(j).getNodeName();
            fieldName = fieldName.split(":")[1];
            fieldValue = recordChilds.item(j).getChildNodes().item(0).getNodeValue();
            
            if (fieldName.equalsIgnoreCase("apiVersion"))
            {
              sfPushtopicDetails.setApiVersion(fieldValue);
            } else if (fieldName.equalsIgnoreCase("id"))
            {
              sfPushtopicDetails.setId(fieldValue);
            } else if (fieldName.equalsIgnoreCase("isActive"))
            {
              sfPushtopicDetails.setActive(fieldValue);
            } else if (fieldName.equalsIgnoreCase("name"))
            {
              sfPushtopicDetails.setName(fieldValue);
            } else if (fieldName.equalsIgnoreCase("notifyForFields"))
            {
              sfPushtopicDetails.setNotifyForFields(fieldValue);
            } else if (fieldName.equalsIgnoreCase("notifyForOperationCreate"))
            {
              sfPushtopicDetails.setNotifyForOperationCreate(fieldValue);
            }
            else if (fieldName.equalsIgnoreCase("notifyForOperationDelete"))
            {
              sfPushtopicDetails.setNotifyForOperationDelete(fieldValue);
            }
            else if (fieldName.equalsIgnoreCase("notifyForOperationUndelete"))
            {
              sfPushtopicDetails.setNotifyForOperationUndelete(fieldValue);
            }
            else if (fieldName.equalsIgnoreCase("notifyForOperationUpdate"))
            {
              sfPushtopicDetails.setNotifyForOperationUpdate(fieldValue);
            }
            else if (fieldName.equalsIgnoreCase("query"))
            {
              sfPushtopicDetails.setQuery(fieldValue);
            }
          }
          this.pushtopicObjectsListInzuora.add(sfPushtopicDetails);
        }
      }
    } else {
      String fault = null;
      NodeList faultStringNodeList = doc.getElementsByTagName("faultstring");
      
      if (faultStringNodeList.getLength() > 0) {
        fault = faultStringNodeList.item(0).getChildNodes().item(0).getNodeValue();
      }
      
      this.m_logger.logError("Fault received while querying pushtopics : " + fault);
      MessageFormat msgformat = new MessageFormat(CloudUtil.getResourceBundle().getString("UNABLE_TO_ACTIVATE_FLOW"));
      String[] obj = { fault };
      throw new CloudInvocationException(msgformat.format(obj));
    }
    return this.pushtopicObjectsListInzuora;
  }
  
  private String activatePushtopic(String id) throws CloudInvocationException, Exception
  {
    SOAPMessage updateRequestMessage = SOAPUtil.loadSOAPMessage(getSoapXmlToUpdatePushTopic(id));
    SOAPMessage updateResponseMessage = SOAPUtil.invoke(updateRequestMessage, new URL(this.endpoint), this.connProps, this.pluginContext);
    if (updateResponseMessage == null) {
      this.m_logger.logError("Unable to get update() response from zuora.");
      throw new CloudInvocationException(CloudUtil.getResourceBundle().getString("EMPTY_SOAP_RESPONSE_ERROR"));
    }
    Element bodyElement = SOAPUtil.getFirstBodyElement(updateResponseMessage.getSOAPBody());
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    Document doc = SOAPUtil.processElement(bodyElement, baos);
    NodeList successNode = doc.getElementsByTagName("success");
    NodeList messageNodeList = doc.getElementsByTagName("message");
    if (successNode.getLength() > 0) {
      String success = successNode.item(0).getChildNodes().item(0).getNodeValue().toString();
      this.m_logger.logDebug("pushtopic updation status : " + success);
      if (success.equalsIgnoreCase("true"))
      {
        return "SUCCESS";
      }
      String errorMessage = messageNodeList.item(0).getChildNodes().item(0).getNodeValue().toString();
      return errorMessage;
    }
    String fault = null;
    NodeList faultStringNodeList = doc.getElementsByTagName("faultstring");
    
    if (faultStringNodeList.getLength() > 0) {
      fault = faultStringNodeList.item(0).getChildNodes().item(0).getNodeValue();
      
      this.m_logger.logError("Received fault while activating pushtopic: " + fault);
    }
    return fault;
  }
  
  public String getExistingPushtopicNameWithSameCriteria()
    throws CloudInvocationException, Exception
  {
    this.pushtopicObjectsListInzuora = queryExistingPushtopics();
    String existingPushtopicNameToUse = null;

    for (PushtopicDetails pushtopicInzuora : this.pushtopicObjectsListInzuora) {
      if (this.currentPushtopicObjectDetails.equals(pushtopicInzuora)) {
        if (pushtopicInzuora.IsActive().equalsIgnoreCase("false")) {
          this.m_logger.logDebug("pushtopic exists but inactive : " + pushtopicInzuora.getName());
          String pushtopicId = pushtopicInzuora.getId();
          this.m_logger.logDebug("activating pushtopic: " + pushtopicInzuora.getName());
          String pushtopicUpdateStatus = activatePushtopic(pushtopicId);
          if (!pushtopicUpdateStatus.equalsIgnoreCase("SUCCESS"))
          {
            this.m_logger.logError("Pushtopic not activated: " + pushtopicInzuora.getName() + "statusMessage: " + pushtopicUpdateStatus);
            MessageFormat msgformat = new MessageFormat(CloudUtil.getResourceBundle().getString("UNABLE_TO_ACTIVATE_FLOW"));
            String[] obj = { pushtopicUpdateStatus };
            throw new CloudInvocationException(msgformat.format(obj));
          }
        }
        existingPushtopicNameToUse = pushtopicInzuora.getName();
        this.m_logger.logDebug("Using existing pushtopic: " + existingPushtopicNameToUse);
        
        break;
      }
    }
    return existingPushtopicNameToUse;
  }
}