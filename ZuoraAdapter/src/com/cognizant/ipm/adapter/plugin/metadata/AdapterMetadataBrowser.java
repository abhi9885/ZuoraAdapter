package com.cognizant.ipm.adapter.plugin.metadata;

import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.namespace.QName;

import oracle.tip.tools.ide.adapters.cloud.api.connection.CloudConnection;
import oracle.tip.tools.ide.adapters.cloud.api.metadata.CloudMetadataDataSource;
import oracle.tip.tools.ide.adapters.cloud.api.metadata.MetadataParser;
import oracle.tip.tools.ide.adapters.cloud.api.metadata.MetadataParserRegistry;
import oracle.tip.tools.ide.adapters.cloud.api.metadata.WSDLMetadataDataSource;
import oracle.tip.tools.ide.adapters.cloud.api.model.CloudAPINode;
import oracle.tip.tools.ide.adapters.cloud.api.model.CloudApplicationModel;
import oracle.tip.tools.ide.adapters.cloud.api.model.CloudDataObjectNode;
import oracle.tip.tools.ide.adapters.cloud.api.model.CloudOperationNode;
import oracle.tip.tools.ide.adapters.cloud.api.model.MetadataDownloadHandler;
import oracle.tip.tools.ide.adapters.cloud.api.plugin.AdapterPluginContext;
import oracle.tip.tools.ide.adapters.cloud.api.plugin.CloudApplicationAdapterException;
import oracle.tip.tools.ide.adapters.cloud.impl.metadata.model.CloudAPINodeImpl;
import oracle.tip.tools.ide.adapters.cloud.impl.metadata.wsdl.AbstractMetadataBrowser;
import oracle.tip.tools.ide.adapters.cloud.impl.util.ws.soap.SOAPHelper;
import oracle.xml.parser.v2.DOMParser;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import com.cognizant.ipm.adapter.util.AdapterUtil;


public class AdapterMetadataBrowser extends AbstractMetadataBrowser implements MetadataDownloadHandler
{
  private CloudConnection connection;
  private List<CloudAPINode> apiNodeList = new ArrayList<CloudAPINode>();
  private AdapterPluginContext context = null;
    
  public AdapterMetadataBrowser(CloudConnection cloudConnection, AdapterPluginContext adapterPluginContext) {
	  super(cloudConnection, adapterPluginContext);
	  this.context = adapterPluginContext;
	  this.connection = getConnection();
	  System.out.println("Executing method "+Thread.currentThread().getStackTrace()[1]);
  }
  
  protected void preProcess()
  {
	  System.out.println("Executing method "+Thread.currentThread().getStackTrace()[1]);
    System.out.println("MetadataBrowser:preprocess");   
    this.apiNodeList.clear();
    String namespace = null;
    try
    {
      String endPointURL = SOAPHelper.getEndpointAddressFromWSDL(this.connection.getConnectionProperties()).toString();
      System.out.println("endPointURL = " + endPointURL);
      this.context.setContextObject("WSDL", "zuoracrm");
      namespace = "http://api.zuora.com";
    }
    catch (Exception e)
    {
      e.printStackTrace();
    }
    Map<String, Set<String>> operationMap = getOperationsList();
    Iterator keyIterator = operationMap.keySet().iterator();
    CloudAPINode api = null;
    while(keyIterator.hasNext()) {
        String type = (String) keyIterator.next();
        api = new CloudAPINodeImpl(type, namespace, getVersion(), operationMap.get(type));
        this.apiNodeList.add(api);
    }
    Collections.sort(apiNodeList, new Comparator<CloudAPINode>() {
        public int compare(CloudAPINode obj1, CloudAPINode obj2) {
          return obj1.getName().compareToIgnoreCase(obj2.getName());
        }
        
      });
    super.preProcess();
  }
  
  public List<CloudAPINode> getAPINodes()
  {
	  System.out.println("Executing method "+Thread.currentThread().getStackTrace()[1]);
      System.out.println("MetadataBrowser:getAPINodes");
      return this.apiNodeList;
  }
  
  public List<CloudDataObjectNode> getDataObjectNodes(CloudOperationNode scope)
  {
	  	System.out.println("Executing method "+Thread.currentThread().getStackTrace()[1]);
	    QName sfObject = null;
	    sfObject = new QName("http://object.api.zuora.com/", "zObject");
	    
	    Set<CloudDataObjectNode> dataObjectNodes = new HashSet<CloudDataObjectNode>();
	    CloudApplicationModel metadataStore = getModel();
	    CloudDataObjectNode rootObject = metadataStore.findDataObject(sfObject);
	    System.out.println("rootObject = "+rootObject);
	    metadataStore.getDataObjectDecendants(rootObject, true, dataObjectNodes);
	    System.out.println("dataObjectNodes = "+dataObjectNodes);
	    List<CloudDataObjectNode> dataObjectList = new ArrayList<CloudDataObjectNode>(dataObjectNodes);
	    Collections.sort(dataObjectList, new Comparator<CloudDataObjectNode>() {
	        public int compare(CloudDataObjectNode obj1, CloudDataObjectNode obj2) {
	        	return obj1.getName().compareToIgnoreCase(obj2.getName());
	        }
	    });
	    return dataObjectList;
  }
  
	public Document getConfigDoc(InputStream configFile) {
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

  
  public Map<String, Set<String>> getOperationsList()
  {
	  System.out.println("Executing method "+Thread.currentThread().getStackTrace()[1]);
      Map<String, Set<String>> operationMap = new HashMap<String, Set<String>>();
      Set<String> operations;
      String operationType;
      Document configDoc = null;      
      InputStream is = AdapterUtil.class.getClassLoader().getResourceAsStream("resources/adapter-operations.xml");
      configDoc = this.getConfigDoc(is);

      if (configDoc != null) {
        Element rootE = configDoc.getDocumentElement();        
        NodeList nodes = rootE.getElementsByTagName("type");        
        int ncnt = nodes.getLength();
        for (int ii = 0; ii < ncnt; ii++) {
            operations = new HashSet<String>();
            Element operationTypeNode = (Element)nodes.item(ii);
            operationType = operationTypeNode.getAttribute("name");
            NodeList operationNodes = operationTypeNode.getElementsByTagName("operation");
            int oprCount = operationNodes.getLength();
            for(int index=0; index < oprCount; index++) {
                Element operationNode = (Element)operationNodes.item(index);
                String operation = operationNode.getTextContent();
                operations.add(operation);
            }
            operationMap.put(operationType, operations);
        }
      }
      System.out.println("operationMap = "+operationMap);
      return operationMap;
  }
  

  public Set<String> signatureKeys()
  {
	  	System.out.println("Executing method "+Thread.currentThread().getStackTrace()[1]);
	    Set<String> keys = new HashSet<String>();
	    keys.add("targetWSDLURL");
	    keys.add("username");
	    keys.add("password");
	    return keys;
  }
  
  public boolean requiresRefresh(long l) throws CloudApplicationAdapterException
  {	
	  	System.out.println("Executing method "+Thread.currentThread().getStackTrace()[1]);
	  	return false;
  }
  
  public CloudApplicationModel downloadMetadata() throws CloudApplicationAdapterException
  {
	  	System.out.println("Executing method "+Thread.currentThread().getStackTrace()[1]);
	  	AdapterPluginContext context = getContext();
    
	  	List<MetadataParser> parsers = getMetadataParsers();
	  	for (MetadataParser parser : parsers) {
	  		parseMetadata(parser, context);
	  	}
    
	  	return context.getCloudApplicationModel();
  }
  
  protected String getVersion()
  {
    return "1.0.0";
  }
  
  protected boolean filterByAPINodes()
  {
    return true;
  }
  
  /**
   * use this method to get the parser that you want to parse your meta data
   * for now we need WSDL meta data parser
   * in future we can get a JASON meta data parser
   */
  protected List<MetadataParser> getMetadataParsers()
  {
	  System.out.println("Executing method "+Thread.currentThread().getStackTrace()[1]);
    List<MetadataParser> parsers = new ArrayList<MetadataParser>();
    MetadataParser wsdlMetadataParser = MetadataParserRegistry.getParser("application/wsdl+xml");
    parsers.add(wsdlMetadataParser);
    return parsers;
  }
  

  /**
   * use this to parse the WSDL
   */
  protected void parseMetadata(MetadataParser metadataParser, AdapterPluginContext adapterPluginContext)
    throws CloudApplicationAdapterException
  {
	  System.out.println("Executing method "+Thread.currentThread().getStackTrace()[1]);
    if (metadataParser.getSupportedMediaTypes().contains("application/wsdl+xml")) {
      try {
    	    System.out.println("connection: "+connection);
    	    System.out.println("connection.getConnectionProperties()  "+connection.getConnectionProperties());
    	    String targetWsdl = connection.getConnectionProperties().getProperty("targetWSDLURL");
    	    System.out.println("targetWsdl "+targetWsdl);
    	    CloudApplicationModel model = context.getCloudApplicationModel();
    	    System.out.println("model "+model);
    	    CloudMetadataDataSource dataSource = new WSDLMetadataDataSource(context.getReferenceBindingName(), new URL(targetWsdl), connection.getConnectionProperties(), context.getRepository());
    	    metadataParser.parse(dataSource, model, connection.getConnectionProperties());
      	} catch (MalformedURLException e) {
      		e.printStackTrace();
      		throw new CloudApplicationAdapterException(e);
      	}
        catch(Exception e) {
        	e.printStackTrace();
        }
    }
  }
  
  /**
   * this will return the operation representation of the selected operation name
   */
  public CloudOperationNode getOperation(String operationName)
  {
	  	System.out.println("Executing method "+Thread.currentThread().getStackTrace()[1]);
	    List<CloudOperationNode> operations = getModel().findOperations(operationName);
	    getModel().getOperations();
	    return operations.size() > 0 ? (CloudOperationNode) operations.get(0) : null;
  }
}