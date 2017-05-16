/**
 * CloudUtil.java
 * @created Jan 4, 2017
 * @author upendra
 * 
 */
package com.cognizant.ipm.adapter.util;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Properties;
import java.util.ResourceBundle;
import java.util.Set;

import javax.xml.namespace.QName;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.soap.MessageFactory;
import javax.xml.soap.SOAPBody;
import javax.xml.soap.SOAPBodyElement;
import javax.xml.soap.SOAPElement;
import javax.xml.soap.SOAPEnvelope;
import javax.xml.soap.SOAPException;
import javax.xml.soap.SOAPFault;
import javax.xml.soap.SOAPHeader;
import javax.xml.soap.SOAPHeaderElement;
import javax.xml.soap.SOAPMessage;
import javax.xml.soap.SOAPPart;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import oracle.ide.Ide;
import oracle.ide.net.URLFactory;
import oracle.tip.tools.ide.adapters.cloud.api.connection.CloudConnection;
import oracle.tip.tools.ide.adapters.cloud.api.connection.PingStatus;
import oracle.tip.tools.ide.adapters.cloud.api.generation.DefaultRuntimeGenerationContext;
import oracle.tip.tools.ide.adapters.cloud.api.metadata.OperationMapping;
import oracle.tip.tools.ide.adapters.cloud.api.metadata.TypeMapping;
import oracle.tip.tools.ide.adapters.cloud.api.model.CloudDataObjectNode;
import oracle.tip.tools.ide.adapters.cloud.api.model.DataType;
import oracle.tip.tools.ide.adapters.cloud.api.model.Field;
import oracle.tip.tools.ide.adapters.cloud.api.model.ObjectCategory;
import oracle.tip.tools.ide.adapters.cloud.api.model.TransformationModel;
import oracle.tip.tools.ide.adapters.cloud.api.plugin.AdapterPluginContext;
import oracle.tip.tools.ide.adapters.cloud.api.plugin.CloudApplicationAdapterException;
import oracle.tip.tools.ide.adapters.cloud.api.service.AdapterPluginServiceException;
import oracle.tip.tools.ide.adapters.cloud.api.service.SOAPHelperService;
import oracle.tip.tools.ide.adapters.cloud.api.service.WSDLHelperService;
import oracle.tip.tools.ide.adapters.cloud.impl.metadata.model.CloudDataObjectNodeImpl;
import oracle.tip.tools.ide.adapters.cloud.impl.metadata.model.FieldImpl;
import oracle.tip.tools.ide.adapters.designtime.adapter.jca.JcaAdapterContext;
import oracle.tip.tools.presentation.uiobjects.sdk.EditField;
import oracle.tip.tools.presentation.uiobjects.sdk.UIFactory;
import oracle.tip.tools.presentation.uiobjects.sdk.UIObject;
import oracle.xml.parser.v2.DOMParser;

import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import com.cognizant.ipm.adapter.query.CloudRelationships;
import com.cognizant.ipm.adapter.query.RelationshipDataObject;

/**
 * @author upendra
 *
 */
public class CloudUtil {
	
	public static CloudDataObjectNode getBusinessObjectByName(AdapterPluginContext adapterPluginContext, String objname) {
		   System.out.println("starting method : "+new Object(){}.getClass().getEnclosingMethod().getName());
		List nlist = adapterPluginContext.getCloudApplicationModel().getDataObjects();
		ListIterator niter = nlist.listIterator();
		while (niter.hasNext()) {
			CloudDataObjectNode nnode = (CloudDataObjectNode) niter.next();
			String nname = getCloudDataObjectNodeName(nnode);
			if ((nname != null) && (nname.equalsIgnoreCase(objname))) {
				return nnode;
			}
		}
		return null;
	}
	
	public static void createOrUpdateField(LinkedList<EditField> editFields,String fieldName, String fieldLabel, String desc, boolean required,UIObject uiObj, EditField.LabelFieldLayout layout, String helpText,int rowIndex, EditField.LabelFieldAlignment labelFieldAlignment) {
		   System.out.println("starting method : "+new Object(){}.getClass().getEnclosingMethod().getName());
		   EditField editField = null;
		Map<String, EditField> fieldsMap = EditField.getFieldMap((EditField[]) editFields.toArray(new EditField[editFields.size()]));
		if (fieldsMap != null) {
			editField = (EditField) fieldsMap.get(fieldName);
		}

		if (editField != null) {
			int index = editFields.indexOf(editField);
			editFields.remove(index);
			editFields.add(index, UIFactory.createEditField(fieldName,fieldLabel, desc, required, false, uiObj, layout, helpText,rowIndex, labelFieldAlignment));
		} else {
			editFields.add(UIFactory.createEditField(fieldName, fieldLabel,desc, required, false, uiObj, layout, helpText, rowIndex,labelFieldAlignment));
		}
	}
	
	public static void createOrUpdateField(LinkedList<EditField> editFields, String fieldName, String fieldLabel, String desc, boolean required, boolean disabled, UIObject uiObj, EditField.LabelFieldLayout layout, String helpText, int rowIndex, EditField.LabelFieldAlignment labelFieldAlignment)
	{
		  System.out.println("starting method : "+new Object(){}.getClass().getEnclosingMethod().getName());
		  EditField editField = null;
		  Map fieldsMap = EditField.getFieldMap((EditField[])editFields.toArray(new EditField[editFields.size()]));
		  
		  if (fieldsMap != null) {
		    editField = (EditField)fieldsMap.get(fieldName);
		  }
		  
		  if (editField != null) {
		    int index = editFields.indexOf(editField);
		    editFields.remove(index);
		    editFields.add(index, UIFactory.createEditField(fieldName, fieldLabel, desc, required, disabled, uiObj, layout, helpText, rowIndex, labelFieldAlignment));
		  }
		  else
		  {
		    editFields.add(UIFactory.createEditField(fieldName, fieldLabel, desc, required, disabled, uiObj, layout, helpText, rowIndex, labelFieldAlignment));
		  }
	}
	
	public static String getCloudDataObjectNodeName(CloudDataObjectNode nnode) {
		   System.out.println("starting method : "+new Object(){}.getClass().getEnclosingMethod().getName());
		String nname = nnode.getName();
		if (nname == null) {
			QName qname = nnode.getQualifiedName();
			if (qname != null) {
				nname = qname.getLocalPart();
			}
		}
		return nname;
	}
	
	public static String getSingularObjectName(String objName) {
		   System.out.println("starting method : "+new Object(){}.getClass().getEnclosingMethod().getName());
		if (objName.endsWith("s")) {
			if (objName.endsWith("ies")) {
				objName = objName.substring(0, objName.length() - 3) + "y";
			} else {
				objName = objName.substring(0, objName.length() - 1);
			}
		}
		return objName;
	}
	
	public static SOAPMessage createRequestSOAPMessage(boolean iscloudLoginCall, String sessionHeader, String NS) throws CloudApplicationAdapterException {
		try {
			   System.out.println("starting method : "+new Object(){}.getClass().getEnclosingMethod().getName());
			MessageFactory factory = MessageFactory.newInstance();
			SOAPMessage message = factory.createMessage();

			SOAPPart soapPart = message.getSOAPPart();
			SOAPEnvelope envelope = soapPart.getEnvelope();
			envelope.addNamespaceDeclaration("urn", NS);
			if (!iscloudLoginCall) {
				createSessionHeader(message, NS, sessionHeader);
			}
			return message;
		} catch (SOAPException soape) {
			throw new CloudApplicationAdapterException(soape);
		}
	}

	/**
	 * creates the session header value
	 * @param message
	 * @param NS
	 * @param sessionHeader
	 * @throws SOAPException
	 */
	public static SOAPHeader createSessionHeader(SOAPMessage message, String NS, String sessionHeader) throws SOAPException {
		SOAPHeader header = message.getSOAPHeader();
		QName sessionHeaderName = new QName(NS, "SessionHeader", "urn");
		QName childName = new QName(NS, "session", "urn");
		SOAPHeaderElement headerElement = header.addHeaderElement(sessionHeaderName);
		SOAPElement sessionIdElement = headerElement.addChildElement(childName);
		sessionIdElement.addTextNode(sessionHeader);
		
		return header;
	}

	/**
	 * creates login soap message request
	 * @param cloudConnection
	 * @param isEnterprise
	 * @return
	 * @throws CloudApplicationAdapterException
	 */
	public static SOAPMessage createLoginSoapMessage(CloudConnection cloudConnection, boolean isEnterprise) throws CloudApplicationAdapterException {
		String NS = null;
		String userName = cloudConnection.getAuthenticationScheme().getAuthenticationProperties().getProperty("username");
		String password = cloudConnection.getAuthenticationScheme().getAuthenticationProperties().getProperty("password");
		if (userName == null) {
			userName = cloudConnection.getConnectionProperties().getProperty("username");
		}

		if (password == null) {
			password = cloudConnection.getConnectionProperties().getProperty("password");
		}

		if (isEnterprise) {
			NS = "http://api.zuora.com";
		}

		try {
			SOAPMessage message = createRequestSOAPMessage(true, "", NS);
			SOAPBody body = message.getSOAPBody();
			QName qname = new QName(NS, "login", "urn");
			SOAPBodyElement bodyElement = body.addBodyElement(qname);
			QName childName = new QName(NS, "username", "urn");
			SOAPElement userNameElement = bodyElement.addChildElement(childName);
			userNameElement.addTextNode(userName);
			childName = new QName(NS, "password", "urn");
			SOAPElement passwordElement = bodyElement.addChildElement(childName);
			passwordElement.addTextNode(password);
			return message;
		} catch (SOAPException soape) {
			throw new CloudApplicationAdapterException(soape);
		}
	}

	public static SOAPMessage createDescribeSobjectsCallMessageFromNodes(
			String sessionId, boolean isEnterprise,
			List<CloudDataObjectNode> selectedObjList) throws Exception {
		List<String> objectsForCallList = new ArrayList();
		Iterator itr = selectedObjList.iterator();
		while (itr.hasNext()) {
			String selectedObjName = ((CloudDataObjectNode) itr.next())
					.getName();

			objectsForCallList.add(selectedObjName);
		}

		return createDescribeSobjectsCallMessage(sessionId, isEnterprise, objectsForCallList);
	}

	public static SOAPMessage createDescribeSobjectsCallMessage(
			String sessionId, boolean isEnterprise,
			List<String> objectsForCallList)
			throws CloudApplicationAdapterException {
		try {
			String NS = "";
			if (isEnterprise) {
				NS = "http://api.zuora.com";
			}

			SOAPMessage message = createRequestSOAPMessage(false, sessionId, NS);

			SOAPBody body = message.getSOAPBody();
			DocumentBuilderFactory docFactory = DocumentBuilderFactory
					.newInstance();

			DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
			Document doc = docBuilder.newDocument();
			Element rootElement = doc.createElementNS(NS,
					"urn:describeSObjects");

			doc.appendChild(rootElement);
			Element sObjectTypeElement = null;

			for (String objectName : objectsForCallList) {
				sObjectTypeElement = doc.createElementNS(NS, "urn:sObjectType");

				rootElement.appendChild(sObjectTypeElement);
				sObjectTypeElement.appendChild(doc.createTextNode(objectName));
			}
			body.addDocument(doc);
			return message;
		} catch (DOMException dome) {
			throw new CloudApplicationAdapterException(dome);
		} catch (ParserConfigurationException pce) {
			throw new CloudApplicationAdapterException(pce);
		} catch (SOAPException soape) {
			throw new CloudApplicationAdapterException(soape);
		}
	}

	public static List<CloudDataObjectNode> getSelectedObjList(
			AdapterPluginContext context) {
		   System.out.println("starting method : "+new Object(){}.getClass().getEnclosingMethod().getName());
		List<CloudDataObjectNode> selectedObjectList = null;
		selectedObjectList = (List) context
				.getContextObject("selectedObjectList");

		if ((selectedObjectList != null) && (selectedObjectList.size() > 0)) {
			return selectedObjectList;
		}
		selectedObjectList = new ArrayList();
		DefaultRuntimeGenerationContext defaultRuntimeGenerationContext = new DefaultRuntimeGenerationContext(
				context);

		TransformationModel transformationModel = defaultRuntimeGenerationContext
				.getTransformationModel();

		List<OperationMapping> operationMap = transformationModel
				.getOperationMappings();

		OperationMapping operationMapping = (OperationMapping) operationMap
				.get(0);
		List<TypeMapping> tmList = operationMapping.getRequestObjectMappings();

		for (TypeMapping tm : tmList) {
			selectedObjectList.add(tm.getTargetDataObject());
		}
		return selectedObjectList;
	}

	public static PingStatus getPingStatusFromResponse(SOAPMessage response)
			throws SOAPException {
		PingStatus status = null;

		SOAPBody soapBody = response.getSOAPBody();
		SOAPFault soapFault = soapBody.getFault();

		if (soapFault != null) {
			String errorMessage = soapFault.getFaultString();
			String faultCode = soapFault.getFaultCode();
			status = new PingStatus(errorMessage, faultCode);
		} else {
			status = PingStatus.SUCCESS_STATUS;
		}
		return status;
	}

	public static String getOracleHomeDirPath() {
		String oracleHomeDir = null;
		if (JcaAdapterContext.IS_STANDALONE_TESTING) {
			oracleHomeDir = "C:\\ztemp\\";
		} else {
			oracleHomeDir = Ide.getOracleHomeDirectory();

			if (oracleHomeDir.endsWith(File.separator)) {
				oracleHomeDir = oracleHomeDir.substring(0,
						oracleHomeDir.length() - 1);
			}

			File soaOracleHome = new File(
					new File(oracleHomeDir).getParentFile(), "soa");

			oracleHomeDir = soaOracleHome.getAbsolutePath();
		}
		return oracleHomeDir;
	}

	public static String getXmlConfigurationFilePath() {
		String oracleHomeDir = getOracleHomeDirPath();

		String cloudAdapterConfigFilePath = oracleHomeDir;

		if (!JcaAdapterContext.IS_STANDALONE_TESTING) {
			cloudAdapterConfigFilePath = cloudAdapterConfigFilePath
					+ File.separatorChar + "integration" + File.separatorChar
					+ "seed" + File.separatorChar + "soa" + File.separatorChar
					+ "configuration" + File.separatorChar;

			cloudAdapterConfigFilePath = cloudAdapterConfigFilePath.replace(
					"jdeveloper", "soa");
		} else {
			cloudAdapterConfigFilePath = cloudAdapterConfigFilePath + "soa"
					+ File.separatorChar;
		}

		return cloudAdapterConfigFilePath;
	}

	public static Document getConfigDoc(File configFile) {
		Document xmlConfig = null;
		if ((configFile != null) && (configFile.exists())) {
			DOMParser parser = new DOMParser();
			try {
				parser.parse(URLFactory.newFileURL(configFile));
				xmlConfig = parser.getDocument();
			} catch (Exception exp1) {
				exp1.printStackTrace();
			}
		}
		return xmlConfig;
	}

	public static Document getConfigDoc(InputStream configFile) {
		Document xmlConfig = null;
		if (configFile != null) {
			DOMParser parser = new DOMParser();
			try {
				parser.parse(configFile);
				xmlConfig = parser.getDocument();
			} catch (Exception exp1) {
				exp1.printStackTrace();
			}
		}
		return xmlConfig;
	}

	public static boolean returnBooleanValue(String string) {
		if (string.equalsIgnoreCase("true")) {
			return true;
		}
		return false;
	}

	public static String getChecksum(String filePath, Properties connectionProperties)
    throws CloudApplicationAdapterException
  {
    MessageDigest md = null;
    String checkSum = null;
    InputStream fis = null;
    try {
      URL artifactURL = new URL(filePath);
      md = MessageDigest.getInstance("SHA-1");
      if ((artifactURL.getProtocol() != null) && (!artifactURL.getProtocol().startsWith("http")))
      {
        fis = artifactURL.openStream();
      }
      
      byte[] dataBytes = new byte[1024];
      
      int nread = 0;
      while ((nread = fis.read(dataBytes)) != -1) {
        md.update(dataBytes, 0, nread);
      }
      fis.close();
      byte[] mdbytes = md.digest();
      

      StringBuffer sb = new StringBuffer();
      for (int i = 0; i < mdbytes.length; i++) {
        sb.append(Integer.toString((mdbytes[i] & 0xFF) + 256, 16).substring(1));
        
        checkSum = sb.toString();
      }
      
      return checkSum;
    }
    catch (NoSuchAlgorithmException e)
    {
      throw new CloudApplicationAdapterException(e.getMessage());
    } catch (FileNotFoundException e) {
      throw new CloudApplicationAdapterException(e.getMessage());
    } catch (IOException e) {
      throw new CloudApplicationAdapterException(e.getMessage());
    } finally {
      try {
        if (fis != null) {
          fis.close();
        }
      } catch (Exception e) {
        throw new CloudApplicationAdapterException(e.getMessage());
      }
    }
  }

	public static File getXmlConfigurationFile() {
		String cloudAdapterConfigFilePath = getXmlConfigurationFilePath();

		String cloudAdapterConfigFile = cloudAdapterConfigFilePath
				+ "cloudAdapter-config-" + "zuora" + "_operationsSupported.xml";

		return new File(cloudAdapterConfigFile);
	}

	public static String createCustomKeyForCache(AdapterPluginContext context)
			throws CloudApplicationAdapterException {
		String entperiseWsdlVersion = null;
		StringBuilder sb = new StringBuilder("");
		CloudConnection connection = (CloudConnection) context
				.getContextObject("adapterConnection");

		String wsdlURL = connection.getConnectionProperties().getProperty(
				"targetWSDLURL");

		SOAPHelperService soapHelper = (SOAPHelperService) context
				.getServiceRegistry().getService(SOAPHelperService.class);

		WSDLHelperService wsdlHelper = (WSDLHelperService) context
				.getServiceRegistry().getService(WSDLHelperService.class);

		URL endpointURL = null;
		try {
			endpointURL = wsdlHelper.getEndpointAddressFromWSDL(
					new URL(wsdlURL), connection.getConnectionProperties());

			entperiseWsdlVersion = getVersionFromURL(endpointURL.toString(),
					context.getContextObject("WSDL").toString());

			sb.append(entperiseWsdlVersion + "-");
			String userName = connection.getAuthenticationScheme()
					.getAuthenticationProperties().getProperty("username");

			if (userName == null) {
				userName = connection.getConnectionProperties().getProperty(
						"username");
			}

			sb.append(userName);
		} catch (MalformedURLException e) {
			throw new CloudApplicationAdapterException(e);
		} catch (AdapterPluginServiceException e) {
			throw new CloudApplicationAdapterException(e);
		}
		return sb.toString();
	}

	public static Document loadDocument(InputStream is)
			throws ParserConfigurationException, SAXException, IOException {
		DocumentBuilder builder = DocumentBuilderFactory.newInstance()
				.newDocumentBuilder();

		return builder.parse(is);
	}

	public static Field getIDField(String ns) {
		Field field = null;
		String idField = "Id";
		CloudDataObjectNode typeMapNode = new CloudDataObjectNodeImpl(null,
				new QName(ns, idField.toUpperCase()), ObjectCategory.STANDARD);

		field = new FieldImpl(idField, typeMapNode, false, false, false);
		return field;
	}

	public static boolean isEnterpriseWSDL(String endPointURL) {
		boolean flag = false;
		if (endPointURL.contains("/c/"))
			flag = true;
		return flag;
	}

	public static boolean isPartnerWSDL(String endPointURL) {
		boolean flag = false;
		if (endPointURL.contains("/u/"))
			flag = true;
		return flag;
	}

	public static String getVersionFromURL(String endPointURL, String wsdlType) {
		String version = null;

		if (wsdlType.equals("enterprise")) {
			int startIndex = endPointURL.indexOf("/c/") + 3;
			int endIndex;
			if (endPointURL.lastIndexOf("/") > startIndex) {
				endIndex = endPointURL.indexOf("/", startIndex);
			} else {
				endIndex = endPointURL.length();
			}
			version = endPointURL.substring(startIndex, endIndex);
		}

		return version;
	}

	public static void handleClearCache(CloudConnection connection,
			AdapterPluginContext context) {
		if ((!Boolean.parseBoolean(connection.getConnectionProperties()
				.getProperty("useCache")))
				|| (returnBooleanValue(context.getContextObject(
						"CHECKBOX_INVOCATION").toString()) == true)) {

			String userName = connection.getAuthenticationScheme()
					.getAuthenticationProperties().getProperty("username");

			if (userName == null) {
				userName = connection.getConnectionProperties().getProperty(
						"username");
			}

			File folder = new File(context.getContextObject("CACHE_DIRECTORY")
					.toString());

			ArrayList<String> arrayList = new ArrayList();
			File[] listOfFiles = folder.listFiles();
			for (int i = 0; i < listOfFiles.length; i++) {
				if ((listOfFiles[i].isFile())
						&& (listOfFiles[i].getName().contains(userName))) {
					arrayList.add(listOfFiles[i].getName());
				}
			}
			for (String name : arrayList) {
				context.getRepository().delete(name);
			}
		}
	}

	public static Field getBusinessObjectFieldByName(
			CloudDataObjectNode cdonode, String fieldName)
			throws CloudApplicationAdapterException {
		Set fields = cdonode.getFields();
		Iterator niter = fields.iterator();
		while (niter.hasNext()) {
			Field field = (Field) niter.next();
			if (field.getName().equalsIgnoreCase(fieldName)) {
				return field;
			}
		}
		return null;
	}

	public static int findClosingBracketIndex(int startIndex,
			String stringToProcess) {
		int bracketCount = 1;
		int endIndex = 0;
		for (int i = startIndex + 1; i < stringToProcess.length(); i++) {
			if (stringToProcess.charAt(i) == '(') {
				bracketCount++;
			} else if (stringToProcess.charAt(i) == ')') {
				bracketCount--;
			}
			if (bracketCount == 0) {
				endIndex = i;
				break;
			}
		}
		if (bracketCount > 0) {
			return -1;
		}
		return endIndex;
	}

	public static Map<String, RelationshipDataObject> getObjRelMapFromContext(
			AdapterPluginContext context) {
		Map<String, RelationshipDataObject> objectRelMap = new HashMap();

		if (context.getContextObject("Object_Rel_Map") != null) {
			objectRelMap = (HashMap) context.getContextObject("Object_Rel_Map");

		} else {

			List<CloudDataObjectNode> nodes = context
					.getCloudApplicationModel().getDataObjects();

			for (int i = 0; i < nodes.size(); i++) {
				String nodeParent = null;
				CloudDataObjectNode node = (CloudDataObjectNode) nodes.get(i);
				if (node.getParent() != null) {
					nodeParent = node.getParent().getName();
				}

				if ((nodeParent != null)
						&& (nodeParent.equalsIgnoreCase("zObject"))) {

					String objName = node.getName().trim();
					Set<Field> allFields = new HashSet();
					try {
						allFields = node.getFields();
					} catch (CloudApplicationAdapterException e) {
						e.printStackTrace();
					}
					RelationshipDataObject relationshipDataObject = getObjectFieldsAndRelationshipsFromFieldList(allFields);

					boolean isQueryable = false;
					boolean isSearchable = false;
					if (node.getNodeAttributes().get("queryable") != null) {
						isQueryable = ((Boolean) node.getNodeAttributes().get(
								"queryable")).booleanValue();
					}

					if (node.getNodeAttributes().get("searchable") != null) {
						isSearchable = ((Boolean) node.getNodeAttributes().get(
								"searchable")).booleanValue();
					}

					relationshipDataObject.setQueryable(isQueryable);
					relationshipDataObject.setSearchable(isSearchable);
					objectRelMap.put(objName, relationshipDataObject);
				}
			}
			context.setContextObject("Object_Rel_Map", objectRelMap);
		}

		return objectRelMap;
	}

	public static Set<Field> getFieldsWithBasicDataType(Set<Field> fieldSet) {
		Set<Field> fieldSetNew = new HashSet();
		for (Field field : fieldSet) {
			if (isBasicDataType(field)) {
				fieldSetNew.add(field);
			}
		}

		Field idField = getIDField("http://www.w3.org/2001/XMLSchema");
		fieldSetNew.add(idField);
		return fieldSetNew;
	}

	private static RelationshipDataObject getObjectFieldsAndRelationshipsFromFieldList(
			Set<Field> fieldSet) {
		ArrayList<CloudRelationships> childRelationships = new ArrayList();
		ArrayList<CloudRelationships> parentRelationships = new ArrayList();
		ArrayList<Field> fieldList = new ArrayList();

		for (Field field : fieldSet) {
			String fieldType = field.getFieldType().getName();
			String parent = null;
			if (field.getFieldType().getParent() != null) {
				parent = field.getFieldType().getParent().getName();
			}

			CloudRelationships cloudRelationship = new CloudRelationships();
			cloudRelationship.setRelationshipName(field.getName());
			cloudRelationship.setRelatedObjectName(field.getFieldType()
					.getName());

			if (("zObject".equalsIgnoreCase(parent))
					|| (fieldType.equalsIgnoreCase("zObject"))) {

				parentRelationships.add(cloudRelationship);
			} else if (fieldType.equalsIgnoreCase("QueryResult")) {
				childRelationships.add(cloudRelationship);
			} else if (isBasicDataType(field)) {
				fieldList.add(field);
			}
		}
		if (!fieldList.isEmpty()) {
			Field idField = getIDField("http://www.w3.org/2001/XMLSchema");
			fieldList.add(idField);
		}
		RelationshipDataObject relationshipDataObject = new RelationshipDataObject();
		relationshipDataObject.setParentRelationships(parentRelationships);
		relationshipDataObject.setChildRelationships(childRelationships);
		relationshipDataObject.setFields(fieldList);

		return relationshipDataObject;
	}

	private static boolean isBasicDataType(Field field) {
		String fieldType = field.getFieldType().getName();
		if ((fieldType.equalsIgnoreCase("anyType"))
				|| (fieldType.equalsIgnoreCase("base64Binary"))) {
			return true;
		}
		for (DataType d : DataType.values()) {
			if (d.getDataType().equalsIgnoreCase(fieldType)) {
				return true;
			}
		}

		return false;
	}

	public static String getExactObjName(String objName,
			AdapterPluginContext context) {
		List<CloudDataObjectNode> objListInModel = context
				.getCloudApplicationModel().getDataObjects();

		for (CloudDataObjectNode node : objListInModel) {
			if (node.getName().equalsIgnoreCase(objName)) {
				return node.getName();
			}
		}
		return null;
	}

	public static Field mergeFields(Field field1, Field field2, String curNS,
			AdapterPluginContext adapterPluginContext)
			throws CloudApplicationAdapterException {
		CloudDataObjectNode mergedFieldType = null;
		if (field1.getFieldType() == null) {
			mergedFieldType = field2.getFieldType();
		} else if (field2.getFieldType() == null) {
			mergedFieldType = field1.getFieldType();
		} else {
			mergedFieldType = new CloudDataObjectNodeImpl(
					getBusinessObjectByName(adapterPluginContext, "zObject"),
					new QName(curNS, field1.getFieldType().getName()),
					ObjectCategory.CUSTOM, DataType.OBJECT);

			Set<Field> mergedFieldsSet = new HashSet();
			Set<Field> fieldSet1 = field1.getFieldType().getFields();
			Set<Field> fieldSet2 = field2.getFieldType().getFields();
			Iterator<Field> itf1 = fieldSet1.iterator();
			while (itf1.hasNext()) {
				Field it1Field = (Field) itf1.next();
				Iterator<Field> itf2 = fieldSet2.iterator();
				while (itf2.hasNext()) {
					Field it2Field = (Field) itf2.next();
					if (it1Field.getName().equalsIgnoreCase(it2Field.getName())) {
						it1Field = mergeFields(it1Field, it2Field, curNS,
								adapterPluginContext);

						break;
					}
					mergedFieldsSet.add(it2Field);
				}

				if (!mergedFieldsSet.add(it1Field)) {
					Field toRemoveField = null;
					Iterator<Field> entries = mergedFieldsSet.iterator();
					while (entries.hasNext()) {
						Field itField = (Field) entries.next();
						if (it1Field.getName().equalsIgnoreCase(
								itField.getName())) {
							toRemoveField = itField;
							break;
						}
					}
					if (toRemoveField != null) {
						mergedFieldsSet.remove(toRemoveField);
						mergedFieldsSet.add(it1Field);
					}
				}
			}
			mergedFieldType.addFields(mergedFieldsSet);
		}
		Field mergedField = new FieldImpl(field1.getName(), mergedFieldType,
				false, false, true);

		return mergedField;
	}

	public static String getResourceKeyFromFaultCode(String faultCode) {
		String key = "fault." + faultCode.toLowerCase().replace("_", ".");
		return key;
	}
	
	public static String getTypeOfObjectRelationship(String objNameToGetType,
			String mainObjName, int relChecklevel, AdapterPluginContext context) {
		String relatedDataObjName = null;

		RelationshipDataObject relatedDataObjs = null;
		Map<String, RelationshipDataObject> objectRelMap = getObjRelMapFromContext(context);

		String key = "";
		Set<String> keysSet = objectRelMap.keySet();
		Iterator it = keysSet.iterator();
		while (it.hasNext()) {
			String name = (String) it.next();
			if (mainObjName.equalsIgnoreCase(name)) {
				key = name;
				break;
			}
		}
		relatedDataObjs = (RelationshipDataObject) objectRelMap.get(key);

		if (relatedDataObjs != null) {
			ArrayList<CloudRelationships> cloudRelationships = null;

			if (relChecklevel == 0) {
				cloudRelationships = relatedDataObjs.getChildRelationships();
			} else if ((relChecklevel == 1) || (relChecklevel == 2)) {
				cloudRelationships = relatedDataObjs.getParentRelationships();
			}

			for (CloudRelationships rel : cloudRelationships) {
				if ((rel.getRelationshipName() != null)
						&& (rel.getRelationshipName()
								.equalsIgnoreCase(objNameToGetType))) {

					relatedDataObjName = rel.getRelatedObjectName();
					break;
				}
			}
			if ((relatedDataObjName == null) && (relChecklevel == 2)) {
				for (Field field : relatedDataObjs.getFields()) {
					if ((field.getName() != null)
							&& (field.getName()
									.equalsIgnoreCase(objNameToGetType))) {

						relatedDataObjName = field.getName();
						break;
					}
				}
			}
		}

		if ((relChecklevel == 0) && (relatedDataObjName != null)
				&& (relatedDataObjName.equalsIgnoreCase("QueryResult"))) {

			mainObjName = getExactObjName(mainObjName, context);
			if (mainObjName == null) {
				return null;
			}

			CloudDataObjectNode cNode = context.getCloudApplicationModel()
					.findDataObject(mainObjName);

			if (cNode == null) {
				return null;
			}
			try {
				context.setContextObject("QUERY_RELATIONSHIP_VALIDATION_FLAG",
						"true");

				//relatedDataObjName = getObjectNameFromRelName(cNode,objNameToGetType, context);

				return relatedDataObjName;
			} catch (Exception e) {
				e.printStackTrace();
			} finally {
				context.setContextObject("QUERY_RELATIONSHIP_VALIDATION_FLAG",
						"false");
			}
		}

		return relatedDataObjName;
	}
	


  public static String getNamespace(String WSDLTypeStr)
  {
	  return "http://api.zuora.com";
  }

  public static ResourceBundle getResourceBundle()
  {
    ResourceBundle resourceBundle = ResourceBundle.getBundle("com.cognizant.ipm.adapter.ResourceBundle");
    return resourceBundle;
  }

  public static String convertDoctoString(Document doc)
    throws Exception
  {
    Transformer transformer = TransformerFactory.newInstance().newTransformer();
    
    transformer.setOutputProperty("indent", "yes");
    StreamResult result = new StreamResult(new StringWriter());
    DOMSource source = new DOMSource(doc);
    transformer.transform(source, result);
    String xmlString = result.getWriter().toString();
    return xmlString;
  }

  public static String next(String text)
  {
    int len = text.length();
    if (len == 0) {
      return text;
    }
    
    boolean alphaNum = false;
    int alphaNumPos = -1;
    for (char c : text.toCharArray()) {
      alphaNumPos++;
      if ((Character.isDigit(c)) || (Character.isLetter(c))) {
        alphaNum = true;
        break;
      }
    }
    

    StringBuilder buf = new StringBuilder(text);
    if ((!alphaNum) || (alphaNumPos == 0) || (alphaNumPos == len))
    {
      next(buf, buf.length() - 1, alphaNum);

    }
    else
    {
      String prefix = text.substring(0, alphaNumPos);
      buf = new StringBuilder(text.substring(alphaNumPos));
      next(buf, buf.length() - 1, alphaNum);
      buf.insert(0, prefix);
    }
    

    return buf.toString();
  }

  private static void next(StringBuilder buf, int pos, boolean alphaNum)
  {
    if (pos == -1) {
      char c = buf.charAt(0);
      String rep = null;
      if (Character.isDigit(c)) {
        rep = "1";
      } else if (Character.isLowerCase(c)) {
        rep = "a";
      } else if (Character.isUpperCase(c)) {
        rep = "A";
      } else
        rep = Character.toString((char)(c + '\001'));
      buf.insert(0, rep);
      return;
    }

    char c = buf.charAt(pos);
    if (Character.isDigit(c)) {
      if (c == '9') {
        buf.replace(pos, pos + 1, "0");
        next(buf, pos - 1, alphaNum);
      } else {
        buf.replace(pos, pos + 1, Character.toString((char)(c + '\001')));
      }
    } else if (Character.isLowerCase(c)) {
      if (c == 'z') {
        buf.replace(pos, pos + 1, "a");
        next(buf, pos - 1, alphaNum);
      } else {
        buf.replace(pos, pos + 1, Character.toString((char)(c + '\001')));
      }
    } else if (Character.isUpperCase(c)) {
      if (c == 'Z') {
        buf.replace(pos, pos + 1, "A");
        next(buf, pos - 1, alphaNum);
      } else {
        buf.replace(pos, pos + 1, Character.toString((char)(c + '\001')));

      }
    }
    else if (alphaNum) {
      next(buf, pos - 1, alphaNum);
    }
    else if (c == 65535) {
      buf.replace(pos, pos + 1, Character.toString('\000'));
      
      next(buf, pos - 1, alphaNum);
    } else {
      buf.replace(pos, pos + 1, Character.toString((char)(c + '\001')));
    }
  }

  public static String getQualifiedName(String prefix, String localName)
  {
    String qualifiedName = localName;
    if ((prefix != null) && (prefix.trim().length() > 0)) {
      qualifiedName = prefix + ":" + qualifiedName;
    }
    return qualifiedName;
  }

  public static void renameNamespaceRecursive(Document doc, Node node, boolean changeIdNamespace, String namespace, String namespacePrefix)
  {
    if (node.getNodeType() == 1) {
      if (!changeIdNamespace) {
        if (!node.getLocalName().equalsIgnoreCase("id"))
        {
          doc.renameNode(node, namespace, getQualifiedName(namespacePrefix, node.getLocalName()));
        }
      }
      else
      {
        doc.renameNode(node, namespace, getQualifiedName(namespacePrefix, node.getLocalName()));
      }
    }

    NodeList list = node.getChildNodes();
    for (int i = 0; i < list.getLength(); i++) {
      renameNamespaceRecursive(doc, list.item(i), changeIdNamespace, namespace, namespacePrefix);
    }
  }

  public static String getUrlDomainName(String url)
  {
    String domainName = new String(url);
    int index = domainName.indexOf("://");
    if (index != -1)
    {
      domainName = domainName.substring(index + 3);
    }
    index = domainName.indexOf('/');
    if (index != -1)
    {
      domainName = domainName.substring(0, index);
    }
    domainName = domainName.replaceFirst("^www.*?\\.", "");
    return domainName;
  }

}
