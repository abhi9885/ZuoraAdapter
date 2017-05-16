package com.cognizant.ipm.adapter.query;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.namespace.QName;
import javax.xml.soap.SOAPBody;
import javax.xml.soap.SOAPBodyElement;
import javax.xml.soap.SOAPElement;
import javax.xml.soap.SOAPException;
import javax.xml.soap.SOAPFault;
import javax.xml.soap.SOAPMessage;

import oracle.tip.tools.ide.adapters.cloud.api.connection.CloudConnection;
import oracle.tip.tools.ide.adapters.cloud.api.model.CloudDataObjectNode;
import oracle.tip.tools.ide.adapters.cloud.api.model.CloudOperationNode;
import oracle.tip.tools.ide.adapters.cloud.api.model.CloudQueryParameters;
import oracle.tip.tools.ide.adapters.cloud.api.model.Field;
import oracle.tip.tools.ide.adapters.cloud.api.plugin.AdapterPluginContext;
import oracle.tip.tools.ide.adapters.cloud.api.plugin.CloudApplicationAdapterException;
import oracle.tip.tools.ide.adapters.cloud.api.plugin.ErrorDetail;
import oracle.tip.tools.ide.adapters.cloud.api.query.QueryKeyword;
import oracle.tip.tools.ide.adapters.cloud.api.query.QueryValidationResult;
import oracle.tip.tools.ide.adapters.cloud.api.service.AdapterPluginServiceException;
import oracle.tip.tools.ide.adapters.cloud.api.service.LoggerService;
import oracle.tip.tools.ide.adapters.cloud.api.service.SOAPHelperService;
import oracle.tip.tools.ide.adapters.cloud.api.service.WSDLHelperService;
import oracle.tip.tools.ide.adapters.cloud.api.service.XMLHelperService;
import oracle.tip.tools.ide.adapters.cloud.impl.query.AbstractQuerySupport;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.cognizant.ipm.adapter.util.CloudSessionValidator;
import com.cognizant.ipm.adapter.util.CloudUtil;

public class CloudQuerySupport extends AbstractQuerySupport
{
  private AdapterPluginContext context;
  LoggerService logger;
  private List<CloudDataObjectNode> cloudDataObjectNodes;
  private static String S_OPERATION_QUERY_OBJECT = "query";
  private static String S_OPERATION_SEARCH_OBJECT = "search";
  private static String S_OPERATION_QUERYALL_OBJECT = "queryAll";
  private Pattern bindParameterPattern;
  private Map<String, RelationshipDataObject> objectRelMap;
  private static Map<String, ArrayList<String>> functionDatatypesMap;
  private static Map<String, String> functionPropertyMap;
  private String objTypeToFilterRelationshipList;
  private boolean ignoreQueryableFlag;
  private String currentOperation;
  protected static int PRIMARY_OBJECTS_LIST_ID = 99999;
  protected static int PARENT_RELATIONSHIPS_ID = 55555;
  protected static int CHILD_RELATIONSHIPS_ID = 66666;

  public CloudQuerySupport(AdapterPluginContext context)
  {
	  System.out.println("Executing method "+Thread.currentThread().getStackTrace()[1]);
    this.context = context;
    this.logger = ((LoggerService)context.getServiceRegistry().getService(LoggerService.class));
    this.bindParameterPattern = Pattern.compile("[^&]&{1}\\b[A-Z|a-z|0-9][\\w|\\W][^\\s|'|\\\"|)|}]*");
  }

  protected boolean supportsAutoQuoting()
  {
    return false;
  }
  
  void setObjTypeToFilterRelationshipList(String objType) {
    this.objTypeToFilterRelationshipList = objType;
  }
  
  void setIgnoreQueryableFlag(boolean ignoreQueryableFlag) {
    this.ignoreQueryableFlag = ignoreQueryableFlag;
  }

  public QueryValidationResult internalValidateQuery(String query, CloudOperationNode operation)
    throws CloudApplicationAdapterException
  {
	  System.out.println("Executing method "+Thread.currentThread().getStackTrace()[1]);
    SOAPFault soapFault = null;
    NodeList serverUrlNode = null;
    NodeList sessionIdNode = null;
    SOAPMessage requestMessage = null;
    
    Properties prop = new Properties();
    SOAPHelperService helperService = (SOAPHelperService)this.context.getServiceRegistry().getService(SOAPHelperService.class);
    WSDLHelperService wsdlHelper = (WSDLHelperService)this.context.getServiceRegistry().getService(WSDLHelperService.class);
    CloudConnection connection = (CloudConnection)this.context.getContextObject("adapterConnection");
    String wsdlURL = connection.getConnectionProperties().getProperty("targetWSDLURL");
    URL endpointURL = (URL)this.context.getContextObject("connectionUrl");
    if ((endpointURL == null) && 
      (endpointURL == null)) {
      try {
        endpointURL = wsdlHelper.getEndpointAddressFromWSDL(new URL(wsdlURL), connection.getConnectionProperties());
      }
      catch (MalformedURLException e)
      {
        if (this.logger.isLevel(LoggerService.Level.SEVERE)) {
          this.logger.logInfo("MalformedURLException: " + e.getMessage());
        }
        
        throw new CloudApplicationAdapterException(e);
      }
      this.context.setContextObject("connectionUrl", endpointURL);
    }

    CloudSessionValidator.validateSession(this.context, connection);
    String sessionId = this.context.getContextObject("sessionId").toString();
    String serverUrl = this.context.getContextObject("serverUrl").toString();
    boolean isEnterprise = this.context.getContextObject("WSDL").toString().equals("enterprise");
    query = updateQueryLimit(query);
    try
    {
      if ((operation.getName().equals(S_OPERATION_QUERY_OBJECT)) || (operation.getName().equals(S_OPERATION_QUERYALL_OBJECT)))
      {
        requestMessage = createQueryMessage(sessionId, isEnterprise, query, operation.getName());
      }
      
      if (operation.getName().equals(S_OPERATION_SEARCH_OBJECT)) {
        requestMessage = createSearchMessage(sessionId, isEnterprise, query);
      }
      
      requestMessage.writeTo(System.out);
      ByteArrayOutputStream baosRequestMessage = new ByteArrayOutputStream();
      try {
        requestMessage.writeTo(baosRequestMessage);
      } catch (IOException e) {
        this.logger.logSevere("IOException: " + e.getMessage());
      } catch (SOAPException e) {
        this.logger.logSevere("SOAPException: " + e.getMessage());
      }
      this.logger.logInfo("Query or Search request----------------------\n");
      this.logger.logInfo(new String(baosRequestMessage.toByteArray()));
      this.logger.logInfo("Redirect Server Url----------------------\n");
      this.logger.logInfo(serverUrl);
      SOAPMessage responseMessage = helperService.sendSOAP(requestMessage, connection.getConnectionProperties(), new URL(serverUrl));

      ByteArrayOutputStream baosResponseMessage = new ByteArrayOutputStream();
      try {
        responseMessage.writeTo(baosResponseMessage);
      } catch (IOException e) {
        this.logger.logSevere("IOException: " + e.getMessage());
      } catch (SOAPException e) {
        this.logger.logSevere("SOAPException: " + e.getMessage());
      }
      this.logger.logInfo("Query or Search response----------------------\n");
      this.logger.logInfo(new String(baosResponseMessage.toByteArray()));
      soapFault = responseMessage.getSOAPBody().getFault();
      if ((soapFault != null) && (soapFault.getFaultCode().equals("sf:INVALID_SESSION_ID")))
      {
        SOAPMessage loginRequestMessage = CloudUtil.createLoginSoapMessage(connection, isEnterprise);
        ByteArrayOutputStream baosLoginRequestMessage = new ByteArrayOutputStream();
        try {
          loginRequestMessage.writeTo(baosLoginRequestMessage);
        } catch (IOException e) {
          this.logger.logSevere("IOException: " + e.getMessage());
        } catch (SOAPException e) {
          this.logger.logSevere("SOAPException: " + e.getMessage());
        }
        this.logger.logInfo("Login request----------------------\n");
        this.logger.logInfo(new String(baosLoginRequestMessage.toByteArray()));
        SOAPMessage loginResponseMessage = helperService.sendSOAP(loginRequestMessage, prop, endpointURL);
        
        ByteArrayOutputStream baosLoginResponseMessage = new ByteArrayOutputStream();
        try {
          loginResponseMessage.writeTo(baosLoginResponseMessage);
        } catch (IOException e) {
          this.logger.logSevere("IOException: " + e.getMessage());
        } catch (SOAPException e) {
          this.logger.logSevere("SOAPException: " + e.getMessage());
        }
        this.logger.logInfo("Login request----------------------\n");
        this.logger.logInfo(new String(baosLoginResponseMessage.toByteArray()));
        
        soapFault = loginResponseMessage.getSOAPBody().getFault();
        if (soapFault == null) {
          try {
            serverUrlNode = loginResponseMessage.getSOAPBody().getElementsByTagName("serverUrl");
            sessionIdNode = loginResponseMessage.getSOAPBody().getElementsByTagName("sessionId");
          }
          catch (SOAPException e)
          {
            this.logger.logSevere("SOAPException: " + e.getMessage());
          }
          String newSessionId = sessionIdNode.item(0).getChildNodes().item(0).getNodeValue();
          this.context.setContextObject("sessionId", newSessionId);
          String serverURL = serverUrlNode.item(0).getChildNodes().item(0).getNodeValue();
          this.context.setContextObject("serverUrl", serverURL);
          return internalValidateQuery(query, operation);
        }
        ByteArrayOutputStream responseStream = new ByteArrayOutputStream();
        loginResponseMessage.writeTo(responseStream);
        return processQueryResponse(responseMessage, query, operation);
      }
      ByteArrayOutputStream responseStream = new ByteArrayOutputStream();
      responseMessage.writeTo(responseStream);
      return processQueryResponse(responseMessage, query, operation);
    }
    catch (Exception e) {
      throw new CloudApplicationAdapterException(e.getCause());
    }
  }

  private QueryValidationResult processQueryResponse(SOAPMessage queryResponse, String query, CloudOperationNode operation)
    throws CloudApplicationAdapterException
  {
	  System.out.println("Executing method "+Thread.currentThread().getStackTrace()[1]);
    QueryValidationResult validationResult = null;
    LoggerService logger = (LoggerService)this.context.getServiceRegistry().getService(LoggerService.class);
    ByteArrayOutputStream responseStream = new ByteArrayOutputStream();
    try {
      queryResponse.writeTo(responseStream);
      ErrorDetail error = processQueryFault(queryResponse);
      if (error != null) {
        validationResult = new QueryValidationResult(query, error);
      } else {
        int totalRecords = getTotalRecords(queryResponse, operation);
        String results = "No objects returned";
        if (totalRecords > 0) {
          results = getQueryObjects(queryResponse);
        }
        return new QueryValidationResult(query, results, totalRecords);
      }
      logger.logInfo("Query Response ----------------------\n");
      logger.logInfo(new String(responseStream.toByteArray()));
    } catch (Exception e) {
      logger.logSevere("Exception: " + e.getMessage());
      throw new CloudApplicationAdapterException(e.getCause());
    }
    return validationResult;
  }

  private ErrorDetail processQueryFault(SOAPMessage queryResponse)
    throws AdapterPluginServiceException
  {
	  System.out.println("Executing method "+Thread.currentThread().getStackTrace()[1]);
    SOAPHelperService soapService = (SOAPHelperService)this.context.getServiceRegistry().getService(SOAPHelperService.class);
    ErrorDetail error = soapService.getErrorDetail(queryResponse);
    return error;
  }

  private String getQueryObjects(SOAPMessage queryResponse)
    throws AdapterPluginServiceException, SOAPException
  {
	  System.out.println("Executing method "+Thread.currentThread().getStackTrace()[1]);
    XMLHelperService xmlHelper = (XMLHelperService)this.context.getServiceRegistry().getService(XMLHelperService.class);
    try
    {
      queryResponse.writeTo(System.out);
    }
    catch (IOException e) {}
    NodeList results = queryResponse.getSOAPBody().getElementsByTagName("result");
    if ((results != null) && (results.getLength() > 0)) {
      try {
        queryResponse.writeTo(System.out);
      }
      catch (IOException e) {}
      return xmlHelper.printXMLNode(results.item(0));
    }
    return "No data found";
  }

  private int getTotalRecords(SOAPMessage queryResponse, CloudOperationNode operation)
    throws CloudApplicationAdapterException
  {
	  System.out.println("Executing method "+Thread.currentThread().getStackTrace()[1]);
    if ((operation.getName().equals(S_OPERATION_QUERY_OBJECT)) || (operation.getName().equals(S_OPERATION_QUERYALL_OBJECT)))
    {
      NodeList queryRecordNodeList = null;
      String queryRecordSize = "0";
      
      try
      {
        queryRecordSize = queryResponse.getSOAPBody().getFirstChild().getChildNodes().item(0).getLastChild().getFirstChild().getNodeValue();
      }
      catch (SOAPException e)
      {
        throw new CloudApplicationAdapterException(e.getCause());
      }
      
      return Integer.parseInt(queryRecordSize);
    }
    NodeList queryRecordNodeList = null;
    int recordSize = 0;
    try {
      queryRecordNodeList = queryResponse.getSOAPBody().getElementsByTagName("searchRecords");
    }
    catch (SOAPException e)
    {
      this.logger.logSevere("SOAPException: " + e.getMessage());
      throw new CloudApplicationAdapterException(e.getCause());
    }
    for (int i = 0; i < queryRecordNodeList.getLength(); i++) {
      Node queryRecordNode = queryRecordNodeList.item(i);
      if (queryRecordNode.getNodeType() == 1) {
        recordSize++;
      }
    }
    return recordSize;
  }

  private SOAPMessage createQueryMessage(String sessionId, boolean isEnterprise, String queryString, String opName)
    throws CloudApplicationAdapterException
  {
	  System.out.println("Executing method "+Thread.currentThread().getStackTrace()[1]);
    String NS = "";
    
    if (isEnterprise) {
      NS = "http://api.zuora.com";
    }
    
    SOAPMessage message;
    
    try
    {
      message = CloudUtil.createRequestSOAPMessage(false, sessionId, NS);
      SOAPBody body = message.getSOAPBody();
      QName qname = new QName(NS, opName, "urn");
      SOAPBodyElement bodyElement = body.addBodyElement(qname);
      QName childName = new QName(NS, "queryString", "urn");
      SOAPElement userNameElement = bodyElement.addChildElement(childName);
      userNameElement.addTextNode(queryString);
    } catch (Exception e) {
      this.logger.logSevere("Exception: " + e.getMessage());
      throw new CloudApplicationAdapterException(e.getCause());
    }
    return message;
  }

  private SOAPMessage createSearchMessage(String sessionId, boolean isEnterprise, String searchString)
    throws CloudApplicationAdapterException
  {
	  System.out.println("Executing method "+Thread.currentThread().getStackTrace()[1]);
    String NS = "";
    if (isEnterprise) {
      NS = "http://api.zuora.com";
    }
    
    SOAPMessage message;
    
    try
    {
      message = CloudUtil.createRequestSOAPMessage(false, sessionId, NS);
      SOAPBody body = message.getSOAPBody();
      QName qname = new QName(NS, "search", "urn");
      SOAPBodyElement bodyElement = body.addBodyElement(qname);
      QName childName = new QName(NS, "searchString", "urn");
      SOAPElement userNameElement = bodyElement.addChildElement(childName);
      userNameElement.addTextNode(searchString);
    } catch (Exception e) {
      this.logger.logSevere("Exception: " + e.getMessage());
      throw new CloudApplicationAdapterException(e.getCause());
    }
    return message;
  }
  

  private String updateQueryLimit(String query) throws CloudApplicationAdapterException
  {
	  System.out.println("Executing method "+Thread.currentThread().getStackTrace()[1]);
    if ((query.toLowerCase().contains(" avg")) || (query.toLowerCase().contains(" count")) || (query.toLowerCase().contains(" count_distinct")) || (query.toLowerCase().contains(" min")) || (query.toLowerCase().contains(" max")) || (query.toLowerCase().contains(" sum")))
    {
      return query;
    }
    int maxLimit = 200;
    String updatedQuery = "";
    query = query.replaceAll("\\n|\\r", " ");
    query = query.replaceAll("\\s+", " ");
    Map<String, String> queryMap = matcherPatterNestedQuery(query);
    Iterator<Map.Entry<String, String>> entries = queryMap.entrySet().iterator();
    
    while (entries.hasNext()) {
      Map.Entry<String, String> entry = (Map.Entry)entries.next();
      if (((String)entry.getKey()).contains("innerquery"))
      {
        queryMap.put(entry.getKey(), entry.getValue());
      }
      else {
        String mapQuery = (String)entry.getValue();
        String[] strArr = mapQuery.split(" ");
        int curLimit = -1;
        boolean isLimitFound = false;
        for (int i = 0; i < strArr.length; i++) {
          if (strArr[i].equalsIgnoreCase("limit"))
          {
            isLimitFound = true;
            try {
              curLimit = Integer.parseInt(strArr[(i + 1)]);
              if ((curLimit != -1) && 
                (curLimit > maxLimit)) {
                strArr[(i + 1)] = ("" + maxLimit);
              }
            }
            catch (NumberFormatException e) {}
          }
          
          if ((!isLimitFound) && (strArr[i].equalsIgnoreCase("offset")))
          {
            updatedQuery = updatedQuery + " " + "limit" + " " + maxLimit;
            isLimitFound = true;
          }
          curLimit = -1;
          updatedQuery = updatedQuery + " " + strArr[i];
        }
        
        if (!isLimitFound) {
          updatedQuery = updatedQuery + " " + "limit" + " " + maxLimit;
        }
        
        queryMap.put(entry.getKey(), updatedQuery);
        updatedQuery = "";
      } }
    String mainQuery = (String)queryMap.get("mainQuery");
    entries = queryMap.entrySet().iterator();
    while (entries.hasNext()) {
      Map.Entry<String, String> entry = (Map.Entry)entries.next();
      mainQuery = mainQuery.replace((CharSequence)entry.getKey(), (CharSequence)entry.getValue());
    }
    mainQuery = mainQuery.trim();
    return mainQuery;
  }
  
  public CloudQueryParameters getQueryParameters(String query)
  {
	  System.out.println("Executing method "+Thread.currentThread().getStackTrace()[1]);
	  System.out.println("query = "+query);
    Set<String> parameters = getParameterSet(query, false);
    CloudQueryParameters queryParameters = null;
    if (parameters.size() > 0)
    {
      String serviceName = this.context.getContextObject("bindingName").toString();
      QName TYPE_NAME = new QName("http://xml.oracle.com/types/" + serviceName, "QueryParameters");
      queryParameters = new CloudQueryParametersImpl(TYPE_NAME);
      for (String param : parameters) {
        queryParameters.addParameterName(param);
      }
    }
    return queryParameters;
  }
  
  public Set<String> getParameterSet(String string, boolean b)
  {
	  System.out.println("Executing method "+Thread.currentThread().getStackTrace()[1]);
    setBindParameterPattern(this.bindParameterPattern);
    return super.getParameterSet(string, false);
  }
  
  private Map<String, String> matcherPatterNestedQuery(String queryString) throws CloudApplicationAdapterException
  {
	  System.out.println("Executing method "+Thread.currentThread().getStackTrace()[1]);
    Map<String, String> queryMap = new HashMap();
    String patternIn = "\\(\\s*select.*?[^A-Z]?\\)";
    Pattern patternInner = Pattern.compile(patternIn, 2);
    
    Pattern patternCommaComma = Pattern.compile("\\,\\s*,");
    Pattern patternCommaFrom = Pattern.compile("\\,\\s*(from)", 2);
    
    Matcher matchInner = patternInner.matcher(queryString);
    boolean success = matchInner.find();
    int counter = 0;
    while (success) {
      String innerQuery = matchInner.group(0);
      innerQuery = innerQuery.substring(innerQuery.indexOf("(") + 1);
      innerQuery = innerQuery.substring(0, innerQuery.lastIndexOf(")"));
      queryString = queryString.replace(innerQuery, "innerquery" + counter);
      
      innerQuery = innerQuery.trim();
      queryMap.put("innerquery" + counter, innerQuery);
      Matcher matchCommaComma = patternCommaComma.matcher(queryString);
      if (matchCommaComma.find()) {
        String commaStr = matchCommaComma.group(0);
        queryString = queryString.replace(commaStr, ",");
      }
      Matcher matchCommaFrom = patternCommaFrom.matcher(queryString);
      if (matchCommaFrom.find()) {
        String commaStr = matchCommaFrom.group(0);
        String fromStr = commaStr.substring(commaStr.indexOf(",") + 1);
        queryString = queryString.replace(commaStr, fromStr);
      }
      try {
        success = matchInner.find();
        counter++;
      } catch (Exception e) {
        e.printStackTrace();
      }
    }
    queryString = queryString.trim();
    queryMap.put("mainQuery", queryString);
    return queryMap;
  }
  
  public List<QueryKeyword> getKeywords()
  {
	  System.out.println("Executing method "+Thread.currentThread().getStackTrace()[1]);
    List<QueryKeyword> queryKeywords = new ArrayList();
    queryKeywords.add(new CloudQueryKeyword("SELECT", false, false, "STANDARD"));
    queryKeywords.add(new CloudQueryKeyword("FROM", false, false, "STANDARD"));
    queryKeywords.add(new CloudQueryKeyword("WHERE", false, false, "STANDARD"));
    queryKeywords.add(new CloudQueryKeyword("FIND", false, false, "STANDARD"));
    queryKeywords.add(new CloudQueryKeyword("IN", true, false, "COMPARISON_OPERATOR"));
    queryKeywords.add(new CloudQueryKeyword("RETURNING", false, false, "STANDARD"));
    queryKeywords.add(new CloudQueryKeyword("NULL", false, false, "STANDARD"));
    queryKeywords.add(new CloudQueryKeyword("=", true, false, "COMPARISON_OPERATOR"));
    queryKeywords.add(new CloudQueryKeyword("!=", true, false, "COMPARISON_OPERATOR"));
    queryKeywords.add(new CloudQueryKeyword("<", true, false, "COMPARISON_OPERATOR"));
    queryKeywords.add(new CloudQueryKeyword("<=", true, false, "COMPARISON_OPERATOR"));
    queryKeywords.add(new CloudQueryKeyword(">", true, false, "COMPARISON_OPERATOR"));
    queryKeywords.add(new CloudQueryKeyword(">=", true, false, "COMPARISON_OPERATOR"));
    queryKeywords.add(new CloudQueryKeyword("LIKE", true, false, "COMPARISON_OPERATOR"));
    queryKeywords.add(new CloudQueryKeyword("NOT IN", true, false, "COMPARISON_OPERATOR"));
    queryKeywords.add(new CloudQueryKeyword("INCLUDES", true, false, "COMPARISON_OPERATOR"));
    queryKeywords.add(new CloudQueryKeyword("EXCLUDES", true, false, "COMPARISON_OPERATOR"));
    queryKeywords.add(new CloudQueryKeyword("AND", true, false, "LOGICAL_OPERATOR"));
    queryKeywords.add(new CloudQueryKeyword("OR", true, false, "LOGICAL_OPERATOR"));
    queryKeywords.add(new CloudQueryKeyword("NOT", false, false, "STANDARD"));
    queryKeywords.add(new CloudQueryKeyword("LIMIT", false, false, "STANDARD"));
    queryKeywords.add(new CloudQueryKeyword("OFFSET", false, false, "STANDARD"));
    queryKeywords.add(new CloudQueryKeyword("COUNT", true, true, "AGGREGATE_FUNCTION"));
    queryKeywords.add(new CloudQueryKeyword("AVG", true, true, "AGGREGATE_FUNCTION"));
    queryKeywords.add(new CloudQueryKeyword("COUNT_DISTINCT", true, true, "AGGREGATE_FUNCTION"));
    queryKeywords.add(new CloudQueryKeyword("MIN", true, true, "AGGREGATE_FUNCTION"));
    queryKeywords.add(new CloudQueryKeyword("MAX", true, true, "AGGREGATE_FUNCTION"));
    queryKeywords.add(new CloudQueryKeyword("SUM", true, true, "AGGREGATE_FUNCTION"));
    queryKeywords.add(new CloudQueryKeyword("GROUPING", true, true, "AGGREGATE_FUNCTION"));
    queryKeywords.add(new CloudQueryKeyword("CALENDAR_MONTH", true, true, "DATE_FUNCTION"));
    queryKeywords.add(new CloudQueryKeyword("CALENDAR_QUARTER", true, true, "DATE_FUNCTION"));
    queryKeywords.add(new CloudQueryKeyword("CALENDAR_YEAR", true, true, "DATE_FUNCTION"));
    queryKeywords.add(new CloudQueryKeyword("DAY_IN_MONTH", true, true, "DATE_FUNCTION"));
    queryKeywords.add(new CloudQueryKeyword("DAY_IN_WEEK", true, true, "DATE_FUNCTION"));
    queryKeywords.add(new CloudQueryKeyword("DAY_IN_YEAR", true, true, "DATE_FUNCTION"));
    queryKeywords.add(new CloudQueryKeyword("DAY_ONLY", true, true, "DATE_FUNCTION"));
    queryKeywords.add(new CloudQueryKeyword("FISCAL_MONTH", true, true, "DATE_FUNCTION"));
    queryKeywords.add(new CloudQueryKeyword("FISCAL_QUARTER", true, true, "DATE_FUNCTION"));
    queryKeywords.add(new CloudQueryKeyword("FISCAL_YEAR", true, true, "DATE_FUNCTION"));
    queryKeywords.add(new CloudQueryKeyword("HOUR_IN_DAY", true, true, "DATE_FUNCTION"));
    queryKeywords.add(new CloudQueryKeyword("WEEK_IN_MONTH", true, true, "DATE_FUNCTION"));
    queryKeywords.add(new CloudQueryKeyword("WEEK_IN_YEAR", true, true, "DATE_FUNCTION"));
    queryKeywords.add(new CloudQueryKeyword("ConvertTimezone", true, true, "CONVERT_TIMEZONE_FUNCTION"));
    queryKeywords.add(new CloudQueryKeyword("convertCurrency", true, true, "UTILITY_FUNCTION"));
    queryKeywords.add(new CloudQueryKeyword("toLabel", true, true, "UTILITY_FUNCTION"));
    queryKeywords.add(new CloudQueryKeyword("TYPEOF", false, false, "STANDARD"));
    queryKeywords.add(new CloudQueryKeyword("WITH", false, false, "STANDARD"));
    queryKeywords.add(new CloudQueryKeyword("ROLLUP", false, false, "STANDARD"));
    queryKeywords.add(new CloudQueryKeyword("CUBE", false, false, "STANDARD"));
    queryKeywords.add(new CloudQueryKeyword("HAVING", false, false, "STANDARD"));
    queryKeywords.add(new CloudQueryKeyword("ASC", false, false, "STANDARD"));
    queryKeywords.add(new CloudQueryKeyword("DESC", false, false, "STANDARD"));
    queryKeywords.add(new CloudQueryKeyword("NULLS", false, false, "STANDARD"));
    queryKeywords.add(new CloudQueryKeyword("FIRST", false, false, "STANDARD"));
    queryKeywords.add(new CloudQueryKeyword("LAST", false, false, "STANDARD"));
    queryKeywords.add(new CloudQueryKeyword("NETWORK", false, false, "STANDARD"));
    queryKeywords.add(new CloudQueryKeyword("DIVISION", false, false, "STANDARD"));
    queryKeywords.add(new CloudQueryKeyword("DATA", false, false, "STANDARD"));
    queryKeywords.add(new CloudQueryKeyword("CATEGORY", false, false, "STANDARD"));
    queryKeywords.add(new CloudQueryKeyword("GROUP", false, false, "STANDARD"));
    queryKeywords.add(new CloudQueryKeyword("BY", false, false, "STANDARD"));
    queryKeywords.add(new CloudQueryKeyword("ORDER", false, false, "STANDARD"));
    queryKeywords.add(new CloudQueryKeyword("FOR", false, false, "STANDARD"));
    queryKeywords.add(new CloudQueryKeyword("VIEW", false, false, "STANDARD"));
    queryKeywords.add(new CloudQueryKeyword("REFERENCE", false, false, "STANDARD"));
    queryKeywords.add(new CloudQueryKeyword("UPDATE", false, false, "STANDARD"));
    queryKeywords.add(new CloudQueryKeyword("TRACKING", false, false, "STANDARD"));
    queryKeywords.add(new CloudQueryKeyword("VIEWSTAT", false, false, "STANDARD"));
    queryKeywords.add(new CloudQueryKeyword("USING", false, false, "STANDARD"));
    queryKeywords.add(new CloudQueryKeyword("SCOPE", false, false, "STANDARD"));
    return queryKeywords;
  }
  
  public List<String> getQueryHints(String queryPattern, int typeID)
  {
	  System.out.println("Executing method "+Thread.currentThread().getStackTrace()[1]);
    String currentOper = (String)this.context.getContextObject("cloudOperation");
    
    if (currentOper.equalsIgnoreCase(S_OPERATION_SEARCH_OBJECT)) {
      this.currentOperation = S_OPERATION_SEARCH_OBJECT;
    } else if ((currentOper.equalsIgnoreCase(S_OPERATION_QUERY_OBJECT)) || (currentOper.equalsIgnoreCase(S_OPERATION_QUERYALL_OBJECT)))
    {
      this.currentOperation = S_OPERATION_QUERY_OBJECT;
    }
    
    if (this.objectRelMap == null) {
      return null;
    }
    List<String> hints = null;
    


    if (typeID == PRIMARY_OBJECTS_LIST_ID) {
      hints = getPrimaryObjectsList(queryPattern);
    } else if (typeID == PARENT_RELATIONSHIPS_ID) {
      hints = getRelationShips(queryPattern, false);
    } else if (typeID == CHILD_RELATIONSHIPS_ID) {
      hints = getRelationShips(queryPattern, true);

    }
    else if (typeID == FIELDS_OF_PRIMARYOBJECTS_ID)
    {
      if (queryPattern == null) {
        hints = getFieldHintsBasedOnObjName(this.fromStringPattern);
      } else {
        hints = getFilteredFieldListForKeywords(queryPattern, this.fromStringPattern);
      }
    }
    

    return hints;
  }
  
  private ArrayList<String> getFilteredFieldListForKeywords(String functionName, String objName)
  {
	  System.out.println("Executing method "+Thread.currentThread().getStackTrace()[1]);
    if (objName == null) {
      return null;
    }
    
    ArrayList<Field> fields = new ArrayList();
    ArrayList<String> a = new ArrayList();
    

    ArrayList<String> supportedDataTypes = new ArrayList();
    String propertyToGetFieldList; if (functionDatatypesMap.containsKey(functionName.toUpperCase())) {
      supportedDataTypes.addAll((Collection)functionDatatypesMap.get(functionName.toUpperCase()));
      
      fields = getFieldsOfPrimaryObject(objName);
      Iterator itr = fields.iterator();
      Field f; while (itr.hasNext()) {
        f = (Field)itr.next();
        
        for (String dataType : supportedDataTypes) {
          if (f.getFieldType().getName().equalsIgnoreCase(dataType)) {
            a.add(f.getName());
            break;
          }
        }
      }
    }
    else if (functionPropertyMap.containsKey(functionName.toUpperCase())) {
      ArrayList<CloudFieldPropertyMap> fieldPropMapList = getFieldPropertyMap(objName);
      propertyToGetFieldList = (String)functionPropertyMap.get(functionName.toUpperCase());
      

      if ((fieldPropMapList != null) && (!fieldPropMapList.isEmpty())) {
        for (CloudFieldPropertyMap fieldPropertyMap : fieldPropMapList) {
          if (fieldPropertyMap.getPropertyName().equalsIgnoreCase(propertyToGetFieldList))
          {
            a.addAll(fieldPropertyMap.getFieldList());
            break;
          }
        }
      }
    }
    
    return a;
  }
  
  private ArrayList<String> getPrimaryObjectsList(String initials)
  {
	  System.out.println("Executing method "+Thread.currentThread().getStackTrace()[1]);
    Set<String> keySet = this.objectRelMap.keySet();
    Iterator it = keySet.iterator();
    ArrayList<String> keysList = new ArrayList();
    
    String namePattrn = initials != null ? initials : "";
    while (it.hasNext())
    {
      String objName = (String)it.next();
      RelationshipDataObject relationshipDataObject = (RelationshipDataObject)this.objectRelMap.get(objName);
      if (this.currentOperation.equals(S_OPERATION_SEARCH_OBJECT)) {
        if ((objName.toUpperCase().startsWith(namePattrn.toUpperCase())) && (relationshipDataObject.isSearchable()))
        {
          keysList.add(objName);
        }
      } else if ((this.currentOperation.equals(S_OPERATION_QUERY_OBJECT)) && 
        (objName.toUpperCase().startsWith(namePattrn.toUpperCase())) && (relationshipDataObject.isQueryable()))
      {
        keysList.add(objName);
      }
    }
    

    if ((keysList != null) && (keysList.size() > 0))
      Collections.sort(keysList);
    return keysList;
  }
  
  public ArrayList<String> getFieldHintsBasedOnObjName(String objectName) {
	  System.out.println("Executing method "+Thread.currentThread().getStackTrace()[1]);
    ArrayList<Field> fields = new ArrayList();
    ArrayList<String> a = new ArrayList();
    
    if ((objectName.equalsIgnoreCase("Name")) || (objectName.equalsIgnoreCase("RecordType")))
    {
      this.ignoreQueryableFlag = true;
    }
    
    if (objectName.equalsIgnoreCase("zObject")) {
      a.add("id");
    }
    else
    {
      fields = getFieldsOfPrimaryObject(objectName);
    }
    
    if ((fields != null) && (!fields.isEmpty())) {
      Iterator fieldItr = fields.iterator();
      while (fieldItr.hasNext()) {
        Field f = (Field)fieldItr.next();
        a.add(f.getName());
      }
    }
    
    if ((a != null) && (a.size() > 0))
      Collections.sort(a);
    return a;
  }
  

  private ArrayList<String> getRelationShips(String primaryObjectName, boolean isChildRelationships)
  {
	  System.out.println("Executing method "+Thread.currentThread().getStackTrace()[1]);
    ArrayList<String> relationshipList = new ArrayList();
    String primaryObjName = primaryObjectName != null ? primaryObjectName.trim() : "";

    if (primaryObjName.length() > 0) {
      RelationshipDataObject relatedDataObjs = null;

      String key = "";
      Set<String> keysSet = this.objectRelMap.keySet();
      Iterator it = keysSet.iterator();
      while (it.hasNext()) {
        String name = (String)it.next();
        if (primaryObjName.equalsIgnoreCase(name)) {
          key = name;
          break;
        }
      }
      relatedDataObjs = (RelationshipDataObject)this.objectRelMap.get(key);
      
      if (relatedDataObjs != null) {
        ArrayList<CloudRelationships> cloudRel = new ArrayList();
        if (isChildRelationships) {
          if (relatedDataObjs.getChildRelationships() != null) {
            cloudRel = relatedDataObjs.getChildRelationships();
          }
        }
        else if (relatedDataObjs.getParentRelationships() != null) {
          cloudRel = relatedDataObjs.getParentRelationships();
        }
        
        for (CloudRelationships s : cloudRel) {
          if (s.getRelationshipName() != null) {
            if ((this.objTypeToFilterRelationshipList != null) && (!this.objTypeToFilterRelationshipList.equals("")))
            {
              if (s.getRelatedObjectName().equalsIgnoreCase(this.objTypeToFilterRelationshipList))
              {
                relationshipList.add(s.getRelationshipName());
              }
            } else {
              relationshipList.add(s.getRelationshipName());
            }
          }
        }
      }
    }
    if ((relationshipList != null) && (relationshipList.size() > 0))
      Collections.sort(relationshipList);
    return relationshipList;
  }
  
  private ArrayList<Field> getFieldsOfPrimaryObject(String primaryObjectName) {
	  System.out.println("Executing method "+Thread.currentThread().getStackTrace()[1]);
    if (primaryObjectName == null) {
      return null;
    }
    
    Set<String> keySet = this.objectRelMap.keySet();
    Iterator it = keySet.iterator();
    ArrayList<Field> fields = null;

    while (it.hasNext()) {
      String objNameInMap = (String)it.next();
      RelationshipDataObject relationshipDataObject = (RelationshipDataObject)this.objectRelMap.get(objNameInMap);

      if (this.currentOperation.equals(S_OPERATION_SEARCH_OBJECT)) {
        if (primaryObjectName.trim().equalsIgnoreCase(objNameInMap)) {
          if (!this.ignoreQueryableFlag) {
            if (!relationshipDataObject.isSearchable()) break;
            fields = relationshipDataObject.getFields(); break;
          }
          
          fields = relationshipDataObject.getFields();
          break;
        }
      } else if ((this.currentOperation.equals(S_OPERATION_QUERY_OBJECT)) && 
        (primaryObjectName.trim().equalsIgnoreCase(objNameInMap))) {
        if (!this.ignoreQueryableFlag) {
          if (!relationshipDataObject.isQueryable()) break;
          fields = relationshipDataObject.getFields(); break;
        }
        
        fields = relationshipDataObject.getFields();
        
        break;
      }
    }

    return fields;
  }
  
  private void setFunctionDataTypesMap()
  {
    functionDatatypesMap = new HashMap();
    ArrayList<String> supportedDataTypes = null;
    supportedDataTypes = new ArrayList();
    supportedDataTypes.add("double");
    supportedDataTypes.add("int");
    functionDatatypesMap.put("AVG", supportedDataTypes);
    functionDatatypesMap.put("SUM", supportedDataTypes);
    supportedDataTypes = new ArrayList();
    supportedDataTypes.add("date");
    supportedDataTypes.add("dateTime");
    supportedDataTypes.add("double");
    supportedDataTypes.add("int");
    supportedDataTypes.add("string");
    supportedDataTypes.add("ID");
    functionDatatypesMap.put("COUNT", supportedDataTypes);
    functionDatatypesMap.put("COUNT_DISTINCT", supportedDataTypes);
    functionDatatypesMap.put("MIN", supportedDataTypes);
    functionDatatypesMap.put("MAX", supportedDataTypes);
    supportedDataTypes = new ArrayList();
    supportedDataTypes.add("date");
    supportedDataTypes.add("dateTime");
    functionDatatypesMap.put("CALENDAR_MONTH", supportedDataTypes);
    functionDatatypesMap.put("CALENDAR_QUARTER", supportedDataTypes);
    functionDatatypesMap.put("CALENDAR_YEAR", supportedDataTypes);
    functionDatatypesMap.put("DAY_IN_MONTH", supportedDataTypes);
    functionDatatypesMap.put("DAY_IN_WEEK", supportedDataTypes);
    functionDatatypesMap.put("DAY_IN_YEAR", supportedDataTypes);
    functionDatatypesMap.put("FISCAL_MONTH", supportedDataTypes);
    functionDatatypesMap.put("FISCAL_QUARTER", supportedDataTypes);
    functionDatatypesMap.put("FISCAL_YEAR", supportedDataTypes);
    functionDatatypesMap.put("WEEK_IN_MONTH", supportedDataTypes);
    functionDatatypesMap.put("WEEK_IN_YEAR", supportedDataTypes);
    supportedDataTypes = new ArrayList();
    supportedDataTypes.add("dateTime");
    functionDatatypesMap.put("DAY_ONLY", supportedDataTypes);
    functionDatatypesMap.put("HOUR_IN_DAY", supportedDataTypes);
    functionDatatypesMap.put("CONVERTTIMEZONE", supportedDataTypes);
  }
  
  private void setFunctionPropertyMap() {
    functionPropertyMap = new HashMap();
    functionPropertyMap.put("GROUPING", "groupable");
    functionPropertyMap.put("TOLABEL", "picklist");
    functionPropertyMap.put("CONVERTCURRENCY", "currency");
  }
  

  public void setCloudDataObjectNodes(List<CloudDataObjectNode> nodes)
  {
    if (functionDatatypesMap == null) {
      setFunctionDataTypesMap();
    }
    if (functionPropertyMap == null) {
      setFunctionPropertyMap();
    }
    
    this.cloudDataObjectNodes = nodes;
    this.objectRelMap = CloudUtil.getObjRelMapFromContext(this.context);
  }
  
  private ArrayList<CloudFieldPropertyMap> getFieldPropertyMap(String objName)
  {
	  System.out.println("Executing method "+Thread.currentThread().getStackTrace()[1]);
    if (objName == null) {
      return null;
    }
    
    ArrayList<CloudFieldPropertyMap> fieldPropMapList = null;
    fieldPropMapList = getFieldPropertyMapFromObjRelMapInContext(objName);
    if ((fieldPropMapList == null) || (fieldPropMapList.isEmpty()))
    {
      objName = getExactObjName(objName);
      if (objName == null) {
        return null;
      }
      
      CloudDataObjectNode cNode = this.context.getCloudApplicationModel().findDataObject(objName);
      
      if (cNode == null) {
        return null;
      }
      
      if (!CloudUtil.returnBooleanValue(this.context.getContextObject("OFFLINE_MODE").toString()))
      {
        CloudConnection cloudConnect = (CloudConnection)this.context.getContextObject("adapterConnection");
        
        if (cloudConnect == null) {
          return null;
        }
        try
        {
          this.context.setContextObject("QUERY_RELATIONSHIP_VALIDATION_FLAG", "true");
          //CloudUtil.sendDescribeSobjectCall(this.context, cNode);
        }
        catch (Exception e) {}finally
        {
          this.context.setContextObject("QUERY_RELATIONSHIP_VALIDATION_FLAG", "false");
        }
        fieldPropMapList = getFieldPropertyMapFromObjRelMapInContext(objName);
      }
    }
    
    return fieldPropMapList;
  }
  

  private ArrayList<CloudFieldPropertyMap> getFieldPropertyMapFromObjRelMapInContext(String objName)
  {
	  System.out.println("Executing method "+Thread.currentThread().getStackTrace()[1]);
    ArrayList<CloudFieldPropertyMap> fieldPropMapList = new ArrayList();
    Set<String> keySet = this.objectRelMap.keySet();
    Iterator it = keySet.iterator();
    while (it.hasNext()) {
      String objNameInMap = (String)it.next();
      RelationshipDataObject relationshipDataObject = (RelationshipDataObject)this.objectRelMap.get(objNameInMap);
      if (this.currentOperation.equals(S_OPERATION_SEARCH_OBJECT)) {
        if ((objName.trim().equalsIgnoreCase(objNameInMap)) && (relationshipDataObject.isSearchable()))
        {
          fieldPropMapList = relationshipDataObject.getFieldPropertyMap();
          break;
        }
      } else if ((this.currentOperation.equals(S_OPERATION_QUERY_OBJECT)) && 
        (objName.trim().equalsIgnoreCase(objNameInMap))) {
        if ((!this.ignoreQueryableFlag) && (relationshipDataObject.isQueryable()))
        {
          fieldPropMapList = relationshipDataObject.getFieldPropertyMap(); break;
        }
        
        fieldPropMapList = relationshipDataObject.getFieldPropertyMap();
        break;
      }
    }
    
    return fieldPropMapList;
  }
  

  private String getExactObjName(String objName)
  {
	  System.out.println("Executing method "+Thread.currentThread().getStackTrace()[1]);
    List<CloudDataObjectNode> objListInModel = this.context.getCloudApplicationModel().getDataObjects();
    
    for (CloudDataObjectNode node : objListInModel) {
      if (node.getName().equalsIgnoreCase(objName)) {
        return node.getName();
      }
    }
    return null;
  }
}