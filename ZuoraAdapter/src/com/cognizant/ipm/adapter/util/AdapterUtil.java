package com.cognizant.ipm.adapter.util;

import java.io.ByteArrayInputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import javax.xml.namespace.QName;
import javax.xml.soap.MessageFactory;
import javax.xml.soap.MimeHeaders;
import javax.xml.soap.SOAPException;
import javax.xml.soap.SOAPMessage;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMResult;

import oracle.tip.tools.adapters.cloud.api.CloudAdapterCallerContext;
import oracle.tip.tools.adapters.cloud.api.CloudAdapterException;
import oracle.tip.tools.adapters.cloud.api.CloudAdapterFilter;
import oracle.tip.tools.adapters.cloud.utils.CloudAdapterUtils;
import oracle.tip.tools.ide.adapters.cloud.api.connection.AbstractCloudConnection;
import oracle.tip.tools.ide.adapters.cloud.api.connection.CloudConnection;
import oracle.tip.tools.ide.adapters.cloud.api.metadata.CloudMetadataBrowser;
import oracle.tip.tools.ide.adapters.cloud.api.metadata.ObjectGrouping;
import oracle.tip.tools.ide.adapters.cloud.api.metadata.OperationMapping;
import oracle.tip.tools.ide.adapters.cloud.api.metadata.TypeMapping;
import oracle.tip.tools.ide.adapters.cloud.api.model.CloudAPINode;
import oracle.tip.tools.ide.adapters.cloud.api.model.CloudApplicationModel;
import oracle.tip.tools.ide.adapters.cloud.api.model.CloudDataObjectNode;
import oracle.tip.tools.ide.adapters.cloud.api.model.CloudOperationNode;
import oracle.tip.tools.ide.adapters.cloud.api.model.CloudQueryParameters;
import oracle.tip.tools.ide.adapters.cloud.api.model.Field;
import oracle.tip.tools.ide.adapters.cloud.api.model.Header;
import oracle.tip.tools.ide.adapters.cloud.api.model.ObjectCategory;
import oracle.tip.tools.ide.adapters.cloud.api.model.RequestParameter;
import oracle.tip.tools.ide.adapters.cloud.api.model.TransformationModel;
import oracle.tip.tools.ide.adapters.cloud.api.plugin.AdapterPluginContext;
import oracle.tip.tools.ide.adapters.cloud.api.plugin.CloudApplicationAdapterException;
import oracle.tip.tools.ide.adapters.cloud.api.query.QuerySupport;
import oracle.tip.tools.ide.adapters.cloud.api.service.AdapterPluginServiceException;
import oracle.tip.tools.ide.adapters.cloud.api.service.LoggerService;
import oracle.tip.tools.ide.adapters.cloud.api.service.SOAPHelperService;
import oracle.tip.tools.ide.adapters.cloud.impl.metadata.model.CloudDataObjectNodeImpl;
import oracle.tip.tools.ide.adapters.cloud.impl.metadata.model.FieldImpl;
import oracle.tip.tools.ide.adapters.cloud.impl.metadata.model.TransformationModelBuilder;
import oracle.tip.tools.ide.adapters.cloud.impl.plugin.AbstractCloudApplicationAdapter;
import oracle.tip.tools.presentation.uiobjects.sdk.UIFactory;
import oracle.tip.tools.presentation.uiobjects.sdk.UIObject;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.cognizant.ipm.adapter.plugin.metadata.AdapterMetadataBrowser;
import com.cognizant.ipm.adapter.query.CloudFieldPropertyMap;
import com.cognizant.ipm.adapter.query.RelationshipDataObject;

public class AdapterUtil extends CloudAdapterUtils
{
	
   public static SOAPMessage createRequestSOAPMessage(String username, String password) throws Exception
   {
	   System.out.println("starting method : "+new Object(){}.getClass().getEnclosingMethod().getName());
     MessageFactory factory = MessageFactory.newInstance();
     String request = "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:api=\"http://api.zuora.com/\">\n" + 
				"   <soapenv:Header/>\n" + 
				"   <soapenv:Body>\n" + 
				"      <api:login>\n" + 
				"         <!--Optional:-->\n" + 
				"         <api:username>"+username+"</api:username>\n" + 
				"         <!--Optional:-->\n" + 
				"         <api:password>"+password+"</api:password>\n" + 
				"      </api:login>\n" + 
				"   </soapenv:Body>\n" + 
				"</soapenv:Envelope>";

    SOAPMessage message = factory.createMessage(new MimeHeaders(), new ByteArrayInputStream(request.getBytes(Charset.forName("UTF-8"))));
    return message;
  }
	   
  public static void setOperationMappingsForGenerator(AdapterPluginContext adapterPluginContext, Map<String, UIObject> map, CloudMetadataBrowser browser, Properties props, OperationMapping opMapping, TransformationModelBuilder modelBuilder, boolean isSelectedOperationChanged)
  {
    String cloudOpName = UIFactory.getStringValue(map, "cloudOperation");
    CloudOperationNode selectedOperationNode = browser.getOperation(cloudOpName);
    
    String prevOpName = "";
    if (opMapping != null) {
      prevOpName = opMapping.getTargetOperation().getName();
    }
    props.put("targetOperation", cloudOpName);
    String wsdlOpName = UIFactory.getStringValue(map, "cloudWsdlOperation");
    if (wsdlOpName == null) {
      wsdlOpName = cloudOpName;
    }
    if (opMapping == null) {
      opMapping = new OperationMapping(selectedOperationNode, ObjectGrouping.ORDERED, wsdlOpName);
      

      modelBuilder.addOperationMapping(opMapping);
    } else {
      opMapping.setTargetOperation(selectedOperationNode);
      opMapping.setNewOperationName(wsdlOpName);
    }
    
    String cloudApi = UIFactory.getStringValue(map, "cloudApi");
    List reqDataObjects = null;
    List<TypeMapping> reqTypeMapping = null;
    List resDataObjects = new ArrayList();
    List<TypeMapping> resTypeMapping = null;
    AbstractCloudConnection connection = (AbstractCloudConnection)adapterPluginContext.getContextObject("CA_UI_connection");
    
    if (!"ZOQL".equals(cloudApi)) {
      List dataObjects = browser.getDataObjectNodes(selectedOperationNode);
      

      List selectedDataObjList = null;
      if (map.containsKey("cloudBizObj")) {
        selectedDataObjList = UIFactory.getStringValues(map, "cloudBizObj");
      }
      
      if ((selectedDataObjList != null) && (!selectedDataObjList.isEmpty())) {
        reqDataObjects = getDataObjectNodeList(dataObjects, (String[])selectedDataObjList.get(0));
      }
      else {
        reqDataObjects = new ArrayList();
        reqDataObjects.addAll(dataObjects);
      }
      
      if (reqDataObjects != null)
        reqTypeMapping = getRequestTypeMappings(reqDataObjects);
    } else {
      reqTypeMapping = new ArrayList();
      


      QuerySupport querySupport = (QuerySupport)adapterPluginContext.getContextObject("UIQuerySupport");
      
      if (querySupport == null) {
        AbstractCloudApplicationAdapter cloudApplicationAdapter = (AbstractCloudApplicationAdapter)adapterPluginContext.getContextObject("applicationAdapter");
        

        querySupport = cloudApplicationAdapter.getQuerySupport(connection);
      }
      
      CloudQueryParameters parameters = querySupport.getQueryParameters((String)adapterPluginContext.getContextObject("zoql.queryString"));
      

      if (parameters != null) {
        TypeMapping queryMapping = new TypeMapping(parameters, false, false);
        
        reqTypeMapping.add(queryMapping);
      }
    }
    

    opMapping.setRequestObjectMappings(reqTypeMapping);
    


    try
    {
      Set<Field> fields = selectedOperationNode.getResponse().getResponseObject().getFields();
      
      for (Field field : fields) {
        resDataObjects.add(field.getFieldType());
      }
      

      resTypeMapping = getRequestTypeMappings(resDataObjects);
    }
    catch (CloudApplicationAdapterException e) {}
    





    opMapping.setResponseObjectMapping(resTypeMapping);
    
    if ((props != null) && (!props.isEmpty())) {
      if ((opMapping != null) && (cloudOpName.equalsIgnoreCase(prevOpName))) {
        props = mergeProperties(props, opMapping.getOperationProperties());
      }
      
      opMapping.setOperationProperties(props);
    }
    else if (!cloudOpName.equalsIgnoreCase(prevOpName)) {
      opMapping.setOperationProperties(props);
    }
    


    if (isSelectedOperationChanged) {
      List<Header> headers = opMapping.getTargetOperation().getRequestHeaders();
      
      for (Header header : headers) {
        if (header.getName().equalsIgnoreCase("AllOrNoneHeader"))
        {
          String allOrNoneHeaderProperty = "AllOrNoneHeader.allOrNone";
          
          opMapping.setOperationProperty(allOrNoneHeaderProperty, "true");
          
          break;
        }
      }
    }
    
    List<CloudAPINode> apis = browser.getAPIs();
    CloudAPINode cloudAPINode = CloudAdapterUtils.getCloudAPINodeFromList(apis, cloudApi);
    
    Properties connProps = connection.getConnectionProperties();
    if ((connProps != null) && (cloudAPINode != null)) {
      connProps.setProperty("applicationVersion", cloudAPINode.getVersion());
    }
  }
  

  public static Properties mergeProperties(Properties newProps, Properties oldProps)
  {
    if (oldProps == null) {
      return newProps;
    }
    Enumeration e = newProps.propertyNames();
    
    while (e.hasMoreElements()) {
      String key = (String)e.nextElement();
      if (oldProps.containsKey(key)) {
        oldProps.remove(key);
        oldProps.put(key, newProps.getProperty(key));
      } else {
        oldProps.put(key, newProps.getProperty(key));
      }
    }
    return oldProps;
  }
  

  public static Set<Field> getFieldsForSelectedPushtopicObject(String selectedObject, AdapterPluginContext adapterPluginContext)
  {
    Set<Field> objFields = new HashSet();
    


    if ((selectedObject == null) || (selectedObject.equals(""))) {
      return objFields;
    }
    
    CloudApplicationModel model = adapterPluginContext.getCloudApplicationModel();
    
    List<CloudDataObjectNode> nodes = model.getDataObjects();
    for (CloudDataObjectNode node : nodes) {
      if (node.getName().equalsIgnoreCase(selectedObject)) {
        try {
          objFields = node.getFields();
          objFields = CloudUtil.getFieldsWithBasicDataType(objFields);
        }
        catch (CloudApplicationAdapterException e) {
          e.printStackTrace();
        }
      }
    }

    return objFields;
  }
  
  public static boolean isFieldSupported(ArrayList<String> unsupportedFields, Field currentField)
  {
    String currentFieldName = currentField.getName();
    for (String field : unsupportedFields) {
      if (field.equalsIgnoreCase(currentFieldName)) {
        return false;
      }
    }
    return true;
  }
  
  public static String getValueForNotifyForFields(String selectedObject) {
    return "any field of " + selectedObject + " object changes";
  }

  private static SOAPMessage getResponseDocument(SOAPMessage reqMessage, AdapterPluginContext context)
    throws MalformedURLException, AdapterPluginServiceException
  {
    SOAPHelperService soapHelper = (SOAPHelperService)context.getServiceRegistry().getService(SOAPHelperService.class);
    
    CloudConnection connection = (CloudConnection)context.getContextObject("adapterConnection");
    
    Properties prop = connection.getConnectionProperties();
    URL endpointURL = new URL((String)context.getContextObject("serverUrl"));
    

    SOAPMessage sObjectsResponseMessage = soapHelper.sendSOAP(reqMessage, prop, endpointURL);
    
    return sObjectsResponseMessage;
  }
  
  public static SOAPMessage sendDescribeSObjectsCall(AdapterPluginContext context, List<String> objectsForCall)
  {
    boolean isEnterprise = context.getContextObject("WSDL").toString().equals("enterprise");
    CloudConnection connection = (CloudConnection)context.getContextObject("adapterConnection");
    try
    {
      CloudSessionValidator.validateSession(context, connection);
    } catch (CloudApplicationAdapterException e) {
      e.printStackTrace();
    }
    String sessionId = context.getContextObject("sessionId").toString();
    
    SOAPMessage reqMessage = null;
    try {
      reqMessage = CloudUtil.createDescribeSobjectsCallMessage(sessionId, isEnterprise, objectsForCall);
    }
    catch (Exception e) {
      e.printStackTrace();
    }

    SOAPMessage responseMessage = null;
    try {
      responseMessage = getResponseDocument(reqMessage, context);
    }
    catch (AdapterPluginServiceException e) {
      e.printStackTrace();
    } catch (MalformedURLException e) {
      e.printStackTrace();
    }
    return responseMessage;
  }

  public static Document toDocument(SOAPMessage soapMsg)
    throws SOAPException, TransformerConfigurationException, TransformerException
  {
    Source src = soapMsg.getSOAPPart().getContent();
    TransformerFactory tf = TransformerFactory.newInstance();
    
    Transformer transformer = tf.newTransformer();
    DOMResult result = new DOMResult();
    transformer.transform(src, result);
    return (Document)result.getNode();
  }
  
  public static Map<String, RelationshipDataObject> getObjectRelationshipMapForSelectedObject(AdapterPluginContext context, List<String> selectedObjects)
    throws Exception
  {
    Map<String, RelationshipDataObject> objRelMap = new HashMap();
    
    SOAPMessage response = sendDescribeSObjectsCall(context, selectedObjects);
    
    if (response == null) {
      return objRelMap;
    }
    
    Document describeSObjectsResponseDocument = null;
    try {
      describeSObjectsResponseDocument = toDocument(response);
    } catch (SOAPException e) {
      e.printStackTrace();
    } catch (TransformerConfigurationException e) {
      e.printStackTrace();
    } catch (TransformerException e) {
      e.printStackTrace();
    }
    
    if (describeSObjectsResponseDocument != null) {
      NodeList resultNodeList = describeSObjectsResponseDocument.getElementsByTagName("result");
      
      NodeList faultStringNodeList = describeSObjectsResponseDocument.getElementsByTagName("faultstring");
      
      if (faultStringNodeList.getLength() > 0) {
        String fault = faultStringNodeList.item(0).getChildNodes().item(0).getNodeValue();
        
        throw new Exception(fault);
      }
      for (int i = 0; i < resultNodeList.getLength(); i++)
      {
        NodeList resultNode = resultNodeList.item(i).getChildNodes();
        String currentObject = getObjNameFromDescribeSobjectResponse(resultNode);
        ArrayList<String> textAreaFields = new ArrayList();
        ArrayList<String> formulaFields = new ArrayList();
        ArrayList<String> multipicklistFields = new ArrayList();
        

        for (int j = 0; j < resultNode.getLength(); j++)
        {
          Node childNode = resultNode.item(j);
          String el_name = "";
          if (childNode.getNodeType() == 1) {
            el_name = childNode.getNodeName();
            
            if (el_name.equalsIgnoreCase("fields")) {
              String fieldElName = null;
              String fieldName = null;
              boolean isTextareaField = false;
              boolean isFormulaField = false;
              boolean ismultipicklistField = false;
              
              NodeList fieldNodeList = childNode.getChildNodes();
              for (int l = 0; l < fieldNodeList.getLength(); l++)
              {
                Node fieldNode = fieldNodeList.item(l);
                if (fieldNode.getNodeType() == 1) {
                  fieldElName = fieldNode.getNodeName();
                  
                  if (fieldElName.equalsIgnoreCase("name")) {
                    fieldName = fieldNode.getChildNodes().item(0).getNodeValue();
                  }
                  else if (fieldElName.equalsIgnoreCase("type"))
                  {
                    if (fieldNode.getChildNodes().item(0).getNodeValue().equalsIgnoreCase("textarea"))
                    {

                      isTextareaField = true;
                    } else if (fieldNode.getChildNodes().item(0).getNodeValue().equalsIgnoreCase("multipicklist"))
                    {




                      ismultipicklistField = true;
                    }
                  } else if (fieldElName.equalsIgnoreCase("calculated"))
                  {
                    if (fieldNode.getChildNodes().item(0).getNodeValue().equalsIgnoreCase("true"))
                    {

                      isFormulaField = true;
                    }
                  }
                }
              }
              if (isTextareaField) {
                textAreaFields.add(fieldName);
              }
              if (isFormulaField) {
                formulaFields.add(fieldName);
              }
              if (ismultipicklistField) {
                multipicklistFields.add(fieldName);
              }
            }
          }
        }
        

        RelationshipDataObject relationshipDataObject = new RelationshipDataObject();
        ArrayList<CloudFieldPropertyMap> cloudFieldPropertyMapArray = new ArrayList();
        cloudFieldPropertyMapArray.add(new CloudFieldPropertyMap("textareaField", textAreaFields));
        
        cloudFieldPropertyMapArray.add(new CloudFieldPropertyMap("formulaField", formulaFields));
        
        cloudFieldPropertyMapArray.add(new CloudFieldPropertyMap("multipicklistField", multipicklistFields));
        

        relationshipDataObject.setFieldPropertyMap(cloudFieldPropertyMapArray);
        
        objRelMap.put(currentObject, relationshipDataObject);
      }
    }
    context.setContextObject("inboundObjectRelationshipMap", objRelMap);
    
    return objRelMap;
  }
  
  private static String getObjNameFromDescribeSobjectResponse(NodeList resultNode)
  {
    String currentObject = null;
    for (int i = 0; i < resultNode.getLength(); i++) {
      Node childNode = resultNode.item(i);
      String el_name = "";
      if (childNode.getNodeType() == 1) {
        el_name = childNode.getNodeName();
        if (el_name.equalsIgnoreCase("name")) {
          currentObject = childNode.getChildNodes().item(0).getNodeValue();
          

          return currentObject;
        }
      }
    }
    
    return currentObject;
  }
  
  public static CloudMetadataBrowser getCustomWSDLMetadataBrowser(AdapterPluginContext context) throws CloudAdapterException
  {
    CloudMetadataBrowser browser = null;
    LoggerService logger = (LoggerService)context.getServiceRegistry().getService(LoggerService.class);
    try
    {
      AbstractCloudConnection connection = (AbstractCloudConnection)context.getContextObject("CA_UI_connection");
      
      browser = new AdapterMetadataBrowser(connection, context);
      boolean refreshMetadata = false;
      Object refreshMetadataObj = context.getContextObject("refreshMetadata");
      
      if (refreshMetadataObj != null)
        refreshMetadata = ((Boolean)refreshMetadataObj).booleanValue();
      browser.loadMetadata(refreshMetadata);
      context.setContextObject("UIMetadataBrowser", browser);
    } catch (Exception ex) {
      if (logger != null) {
        logger.logSevere("Exception caught while loadMetadata Call : getCustomWSDLMetadataBrowser" + ex.getMessage());
      }
      ex.printStackTrace();
      throw new CloudAdapterException(ex.getMessage(), ex);
    }
    return browser;
  }

  public static void setOperationMappingsForGeneratorOM(OperationMapping opMapping, TransformationModel model, AdapterPluginContext context)
  {
    String p_serviceTargetNameSpace = model.getTargetNamespace();
    List<TypeMapping> reqTypeMapping = new ArrayList();

    try
    {
      Set<Field> fields = ((RequestParameter)opMapping.getTargetOperation().getRequestParameters().get(0)).getDataType().getFields();
      CloudAdapterFilter cloudAdapterFilter = (CloudAdapterFilter)context.getContextObject("UICloudAdapterFilter");
      CloudAdapterCallerContext cloudAdapterCallerContext = cloudAdapterFilter.getCloudAdapterCallerContext();
      if ((cloudAdapterCallerContext == null) || (CloudAdapterCallerContext.PRODUCT.ICS_WEB.equals(cloudAdapterCallerContext.getProduct())) || (CloudAdapterCallerContext.PRODUCT.OSB_WEB.equals(cloudAdapterCallerContext.getProduct())))
      {
        if (!model.getTargetNamespace().endsWith("_REQUEST")) {
          p_serviceTargetNameSpace = p_serviceTargetNameSpace + "_REQUEST";
        }
      }

      for (Field field : fields) {
        CloudDataObjectNode fieldNode = field.getFieldType();
        if (fieldNode.getObjectCategory().equals(ObjectCategory.BUILTIN))
        {
          CloudDataObjectNodeImpl newFieldNode = new CloudDataObjectNodeImpl(fieldNode.getParent(), fieldNode.getQualifiedName(), fieldNode.getObjectCategory(), fieldNode.getDataType(), false);
          TypeMapping reqTypeMappingNode = new TypeMapping(newFieldNode);
          reqTypeMappingNode.setOverridingName(field.getName());
          reqTypeMapping.add(reqTypeMappingNode);
        } else if (fieldNode.getObjectCategory().equals(ObjectCategory.STANDARD))
        {
          CloudDataObjectNodeImpl newFieldNode = new CloudDataObjectNodeImpl(fieldNode.getParent(), new QName(p_serviceTargetNameSpace, fieldNode.getName()), fieldNode.getObjectCategory(), fieldNode.getDataType(), false);
          Set<Field> notificationFields = fieldNode.getFields();
          for (Field notificationField : notificationFields) {
            if (notificationField.getFieldType().getObjectCategory().equals(ObjectCategory.STANDARD))
            {
              FieldImpl newField = null;
              if (notificationField.getName().equalsIgnoreCase("Id"))
              {
                newField = new FieldImpl(notificationField.getName(), notificationField.getFieldType(), notificationField.isArray(), notificationField.isRequired(), notificationField.isNullAllowed());
              }
              else
              {
                CloudDataObjectNode notificationFieldNode = notificationField.getFieldType();
                CloudDataObjectNodeImpl notificationFieldNodeWithProjectNS = new CloudDataObjectNodeImpl(notificationFieldNode.getParent(), new QName(p_serviceTargetNameSpace, notificationFieldNode.getName()), notificationFieldNode.getObjectCategory(), notificationFieldNode.getDataType(), false);
                notificationFieldNodeWithProjectNS.addFields(notificationFieldNode.getFields());
                newField = new FieldImpl(notificationFieldNodeWithProjectNS.getName(), notificationFieldNodeWithProjectNS, notificationField.isArray(), notificationField.isRequired(), notificationField.isNullAllowed());
              }
              newFieldNode.addField(newField);
            } else {
              newFieldNode.addField(notificationField);
            }
          }
          
          TypeMapping reqTypeMappingNode = new TypeMapping(newFieldNode);
          
          reqTypeMappingNode.setOverridingName(field.getName());
          reqTypeMapping.add(reqTypeMappingNode);
        }
      }
      opMapping.setRequestObjectMappings(reqTypeMapping);
    } catch (CloudApplicationAdapterException e) {
      e.printStackTrace();
    }
  }
  
  public static CloudMetadataBrowser getMetadataBrowser(AdapterPluginContext context)
    throws CloudAdapterException
  {
    CloudMetadataBrowser browser = null;
    LoggerService logger = (LoggerService)context.getServiceRegistry().getService(LoggerService.class);
    try
    {
      AbstractCloudConnection connection = (AbstractCloudConnection)context.getContextObject("CA_UI_connection");
      
      Object key = context.getContextObject("METADATA_KEY");
      
      if (key != null) {
        String keyStr = key.toString();
        if (keyStr.equals("METADATA_OUTBOUND_MESSAGING"))
        {
          return getCustomWSDLMetadataBrowser(context); }
        if (keyStr.equals("METADATA_CUSTOM"))
        {
          return getCustomWSDLMetadataBrowser(context);
        }
      }
      browser = new AdapterMetadataBrowser(connection, context);
      boolean refreshMetadata = false;
      browser.loadMetadata(refreshMetadata);
      context.setContextObject("UIMetadataBrowser", browser);
    } catch (Exception ex) {
      if (logger != null) {
        logger.logSevere("Exception caught while loadMetada Call : getMetadataBrowser" + ex.getMessage());
      }
      ex.printStackTrace();
      throw new CloudAdapterException(ex.getMessage(), ex);
    }
    return browser;
  }
}