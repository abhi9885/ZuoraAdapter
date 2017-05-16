package com.cognizant.ipm.adapter.runtime;

import java.io.IOException;
import java.io.StringReader;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeoutException;

import javax.resource.ResourceException;

import oracle.cloud.connector.api.CloudAdapterLoggingService;
import oracle.cloud.connector.api.CloudApplicationConnectionFactory;
import oracle.cloud.connector.api.CloudConnectorException;
import oracle.cloud.connector.api.CloudEndpoint;
import oracle.cloud.connector.api.CloudEndpointFactory;
import oracle.cloud.connector.api.CloudInvocationContext;
import oracle.cloud.connector.api.CloudInvocationException;
import oracle.cloud.connector.api.CloudMessage;
import oracle.cloud.connector.api.CloudMessageFactory;
import oracle.cloud.connector.api.CloudMessageReceiver;
import oracle.cloud.connector.impl.CloudAdapterUtil;
import oracle.tip.tools.ide.adapters.cloud.api.plugin.AdapterPluginContext;
import oracle.tip.tools.ide.adapters.cloud.api.plugin.Repository;
import oracle.tip.tools.ide.adapters.cloud.impl.plugin.DefaultAdapterPluginContext;
import oracle.xml.parser.v2.DOMParser;
import oracle.xml.parser.v2.XMLParseException;

import org.cometd.bayeux.Message;
import org.cometd.bayeux.client.ClientSessionChannel;
import org.cometd.client.BayeuxClient;
import org.cometd.client.transport.LongPollingTransport;
import org.eclipse.jetty.client.Address;
import org.eclipse.jetty.client.ContentExchange;
import org.eclipse.jetty.client.HttpClient;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import com.cognizant.ipm.adapter.runtime.parser.JsonXmlTransformation;
import com.cognizant.ipm.adapter.util.CloudSessionData;
import com.cognizant.ipm.adapter.util.CloudUtil;
import com.cognizant.ipm.adapter.util.PopulateJSONXML;
import com.cognizant.ipm.adapter.util.PushTopic;
import com.cognizant.ipm.adapter.util.SoapLoginUtil;

public class InboundMessageReceiver implements CloudMessageReceiver
{
  private StreamingBayeuxClient client;
  JsonXmlTransformation bconeparser = null;
  PopulateJSONXML jsonXMLConverter;
  private Document doc = null;
  private ClientSessionChannel.MessageListener connectListener;
  private ClientSessionChannel.MessageListener messageListener;
  private String channelName = null;
  private boolean topicExists = false;
  private PushTopic pushtopic;
  private static final int CONNECTION_TIMEOUT = 20000;
  private static final int READ_TIMEOUT = 120000;
  private CloudApplicationConnectionFactory m_connectionFactory;
  
  public InboundMessageReceiver(CloudApplicationConnectionFactory connectionFactory, CloudEndpointFactory cloudEndpointFactory) throws CloudConnectorException {
    this.m_connectionFactory = connectionFactory;
    this.m_cloudEndpointFactory = cloudEndpointFactory;
    this.m_cloudEndpoint = cloudEndpointFactory.createEndpoint();
    this.m_logger = cloudEndpointFactory.getCloudInvocationContext().getLoggingService();
    
    this.jsonXMLConverter = new PopulateJSONXML(this.m_logger);
    this.m_pollingInterval = 5000;
  }

  public void release()
  {
    this.m_logger.logDebug("Cloud Adapter: release() has been invoked.");
    this.m_Enabled = false;
    this.client.getChannel("/meta/connect").removeListener(this.connectListener);
    this.client.getChannel(this.channelName).removeListener(this.messageListener);
    this.client.disconnect();
    this.m_logger.logInfo("Client disconnected!");
  }

  public void run()
  {
    this.m_Enabled = true;
    this.m_logger.logDebug("Cloud Adapter entering polling loop");
    try {
      processEvents();
    }
    catch (Exception e) {
      this.m_logger.logError("Exception caught in run() method:" + e.getMessage(), e);
      
      this.m_cloudEndpoint.onException(e);
    }
  }
  
  private void raiseEvent(String payloadBytes)
    throws ResourceException, Exception
  {
    CloudMessageFactory cloudMessageFactory = this.m_cloudEndpointFactory.getCloudMessageFactory();
    Element payloadData = deserializeDocument(payloadBytes);
    CloudMessage cloudRequestMessage = cloudMessageFactory.createCloudMessage(payloadData);
    this.m_cloudEndpoint.raiseEvent(cloudRequestMessage);
  }

  private void processEvents() throws Exception
  {
    this.connectListener = new ClientSessionChannel.MessageListener()
    {
      public void onMessage(ClientSessionChannel channel, Message message)
      {
        boolean success = message.isSuccessful();
        if (!success) {
          String error = (String)message.get("error");

          if ((!message.isSuccessful()) && ((InboundMessageReceiver.this.client.getBayeuxState() == BayeuxClient.State.REHANDSHAKING) || (InboundMessageReceiver.this.client.getBayeuxState() == BayeuxClient.State.UNCONNECTED)))
          {
            try
            {
            	InboundMessageReceiver.this.resubscribe();
            }
            catch (Exception e)
            {
            	InboundMessageReceiver.this.m_logger.logError("Exception caught while resubscribing to the channel:" + e.getMessage(), e);
            }
          }
        }
      }
    };
    this.m_logger.logDebug("Subscribing for channel:" + this.channelName);
    this.messageListener = new ClientSessionChannel.MessageListener()
    {
      public void onMessage(ClientSessionChannel channel, Message message)
      {
        try
        {
          if (InboundMessageReceiver.this.m_Enabled)
          {
        	  InboundMessageReceiver.this.processEvent(message);
          } else {
        	  InboundMessageReceiver.this.m_logger.logDebug("m_Enabled:" + InboundMessageReceiver.this.m_Enabled);
          }
        } catch (Exception e) {
        	InboundMessageReceiver.this.m_logger.logError("Exception caught during processing the event:" + e.getMessage(), e);

        }
      }
    };
    this.client.getChannel(this.channelName).subscribe(this.messageListener);
    this.m_logger.logDebug("Waiting for streamed data from your organization!!");
    this.m_logger.logDebug("[ META_CONNECT Connection Check ]:" + this.client.getChannel("/meta/connect").getSession().isConnected());
    this.m_logger.logDebug("[" + this.channelName + " Connection Check ]  " + this.client.getChannel(this.channelName).getSession().isConnected());
  }

  private String getChannelName()
  {
    StringBuffer channelStringBuffer = new StringBuffer();
    channelStringBuffer.append("/topic/" + this.pushtopic.getTopicName());
    return channelStringBuffer.toString();
  }

  private void processEvent(Message inboundMessageFromzuora)
    throws Exception
  {
    String payloadData = null;
    
    Map<String, String> jsonMap = new HashMap();
    try
    {
      PopulateJSONXML.generateJSONMap(PopulateJSONXML.fetchJsonRoot(inboundMessageFromzuora, this.m_logger), jsonMap, this.m_logger);
      this.jsonXMLConverter = new PopulateJSONXML(this.m_logger);
      StringBuffer docwithJSONData = new StringBuffer(CloudUtil.convertDoctoString(getDoc()));
      docwithJSONData = this.jsonXMLConverter.dockJSONData(jsonMap, docwithJSONData);
      payloadData = docwithJSONData.toString();
      raiseEvent(payloadData);
    }
    catch (ResourceException re)
    {
      this.m_logger.logError("Cloud Adapter failed to send inbound message due to: " + re.getMessage(), re);
      

      this.m_cloudEndpoint.onReject(payloadData, re);
    }
    catch (Exception e) {
      this.m_logger.logError("Cloud Adapter failed to send inbound message due to: " + e.getMessage(), e);
      

      this.m_cloudEndpoint.onReject(payloadData, new ResourceException(e));
    }
  }

  private Element deserializeDocument(String nativeInstance)
    throws XMLParseException, SAXException, IOException
  {
    DOMParser parser = new DOMParser();
    parser.parse(new StringReader(nativeInstance));
    Document doc = parser.getDocument();
    
    return doc.getDocumentElement();
  }
  
  private StreamingBayeuxClient makeClient() throws CloudInvocationException, Exception
  {
    String pluginID = "zuora";
    String bindingName = "bindingName";
    Repository repository = null;
    AdapterPluginContext pluginContext = new DefaultAdapterPluginContext(pluginID, bindingName, repository);
    String endpointURL = SoapLoginUtil.getSoapURL(this.m_cloudInvocationContext, this.m_connectionFactory);
    CloudSessionData zuoraSessionData = SoapLoginUtil.login(this.m_cloudInvocationContext, endpointURL, pluginContext);
    final String sessionid = zuoraSessionData.getSessionId();
    String endpoint = zuoraSessionData.getEndpoint();
    this.m_logger.logDebug("Login successful!!");
    

    if (!this.topicExists)
    {
      this.pushtopic = new PushTopic(this.m_cloudInvocationContext, zuoraSessionData, pluginContext);
      String existingPushtopicName = this.pushtopic.getExistingPushtopicNameWithSameCriteria();
      if (existingPushtopicName != null) {
        this.topicExists = true;
        this.m_logger.logDebug("Using pushtopic: " + existingPushtopicName);
        this.pushtopic.setTopicName(existingPushtopicName);
      } else {
        String status = this.pushtopic.createPushtopicInzuora();
        
        if (status.equalsIgnoreCase("SUCCESS")) {
          this.m_logger.logDebug("topic created:" + this.pushtopic.getTopicName());
          
          this.topicExists = true;
        } else {
          this.m_logger.logError("Exception while creating the pushtopic: " + status);
          MessageFormat msgformat = new MessageFormat(CloudUtil.getResourceBundle().getString("UNABLE_TO_ACTIVATE_FLOW"));
          String[] obj = { status };
          throw new CloudInvocationException(msgformat.format(obj));
        }
      }
    }
    String zuoraStreamingEndpoint = zuoraStreamingEndpoint(endpoint);
    String proxyHost = CloudAdapterUtil.getProxyHostForURL(zuoraStreamingEndpoint);
    String proxyPort = CloudAdapterUtil.getProxyPortForURL(zuoraStreamingEndpoint);
    HttpClient httpClient = new HttpClient();
    httpClient.setIdleTimeout(5000L);
    httpClient.setConnectorType(2);
    httpClient.setMaxConnectionsPerAddress(32768);
    if ((proxyHost != null) && (proxyPort != null)) {
      httpClient.setProxy(new Address(proxyHost, Integer.parseInt(proxyPort)));
    }
    
    httpClient.start();

    Map<String, Object> options = new HashMap();
    options.put("timeout", Integer.valueOf(120000));
    LongPollingTransport transport = new LongPollingTransport(options, httpClient)
    {
      protected void customize(ContentExchange exchange)
      {
        super.customize(exchange);
        exchange.addRequestHeader("Authorization", "OAuth " + sessionid);
      }
      
    };
    StreamingBayeuxClient client = new StreamingBayeuxClient(zuoraStreamingEndpoint, transport, this.m_logger)
    {

      public void onFailure(Throwable x, Message[] messages)
      {
        super.onFailure(x, messages);
        if (((x instanceof ProtocolException)) || ((x instanceof TimeoutException)))
        {

          try
          {
        	  InboundMessageReceiver.this.resubscribe();
          }
          catch (Exception e)
          {
        	  InboundMessageReceiver.this.m_logger.logError("Exception caught while resubscribing to the channel:" + e.getMessage(), e);
          }
        }
      }

    };
    return client;
  }
  
  private String zuoraStreamingEndpoint(String endpoint)
    throws MalformedURLException
  {
    String cometdEndpoint = new URL(endpoint + getStringEndpointURI()).toExternalForm();
    this.m_logger.logDebug("Endpoint for Cometd :" + cometdEndpoint);
    return cometdEndpoint;
  }
  
  private String getStringEndpointURI()
  {
    Map<String, String> m_connectionProperties = this.m_connectionFactory.getConnectionFactoryProperties();
    String apiVersion = (String)m_connectionProperties.get("applicationVersion");
    StringBuffer endpointURI = new StringBuffer("/cometd/");
    endpointURI.append(apiVersion);
    return endpointURI.toString();
  }
  private CloudEndpoint m_cloudEndpoint;
  private CloudEndpointFactory m_cloudEndpointFactory;
  private CloudInvocationContext m_cloudInvocationContext;
  private CloudAdapterLoggingService m_logger;
  private boolean m_Enabled;
  public void convertJSON2XML(Document doc)
    throws Exception
  {
    this.jsonXMLConverter.transformDocforJSON(doc.getElementsByTagName("eventData").item(0).getChildNodes());
    setDoc(doc);
  }

  private void resubscribe()
    throws CloudInvocationException, Exception
  {
    this.client.getChannel("/meta/connect").removeListener(this.connectListener);
    this.client.getChannel(this.channelName).removeListener(this.messageListener);
    this.client.disconnect();
    this.m_logger.logDebug("Re-Subscribing to channel:" + this.client.getChannel(this.channelName));
    this.client = null;
    this.client = makeClient();
    this.client.getChannel("/meta/connect").addListener(this.connectListener);
    this.client.handshake();
    this.m_logger.logDebug("Waiting for Re-handshake!!");
    boolean handshaken = this.client.waitFor(35000L, BayeuxClient.State.CONNECTED, new BayeuxClient.State[0]);
    if (!handshaken) {
      this.m_logger.logError("Failed to handshake:" + this.client);
      throw new CloudInvocationException(CloudUtil.getResourceBundle().getString("HANDSHAKE_FAILED"));
    }
    
    this.client.getChannel(this.channelName).subscribe(this.messageListener);
  }
  private int m_pollingInterval = 5000;
  public Document getDoc() {
    return this.doc;
  }
  
  public void setDoc(Document document) {
    this.doc = document;
  }
  
  void preSetupForEventSubscription() throws CloudInvocationException, Exception
  {
    this.m_cloudInvocationContext = this.m_cloudEndpointFactory.getCloudInvocationContext();
    this.client = makeClient();
    if (this.client != null) {
      this.m_logger.logDebug("Running streaming client !!");
      this.channelName = getChannelName();
      this.m_logger.logDebug("Channel Name:" + this.channelName);
      this.bconeparser = new JsonXmlTransformation(this.m_logger);
      convertJSON2XML(this.bconeparser.cachXMLDOC(this.m_cloudInvocationContext.getIntegrationWSDL()));
      this.m_logger.logDebug("XML DOC Primary Caching DONE");
      this.client.getChannel("/meta/connect").addListener(this.connectListener);
      this.client.getChannel("/meta/handshake").addListener(new ClientSessionChannel.MessageListener()
      {
        public void onMessage(ClientSessionChannel channel, Message message)
        {
          InboundMessageReceiver.this.m_logger.logDebug("[CHANNEL:META_HANDSHAKE]: " + message);
          boolean success = message.isSuccessful();
          if (!success) {
            String error = (String)message.get("error");
            if (error != null) {
              InboundMessageReceiver.this.m_logger.logError("Error during HANDSHAKE: " + error);
            }
            Exception exception = (Exception)message.get("exception");
            if (exception != null) {
              InboundMessageReceiver.this.m_logger.logError(exception.getMessage(), exception);
            }
          }
        }
      });
      this.client.getChannel("/meta/connect").addListener(new ClientSessionChannel.MessageListener()
      {

        public void onMessage(ClientSessionChannel channel, Message message)
        {
          InboundMessageReceiver.this.m_logger.logDebug("[CHANNEL:META_CONNECT]: " + message);
          boolean success = message.isSuccessful();
          if (!success) {
            String error = (String)message.get("error");
            if (error != null) {
              InboundMessageReceiver.this.m_logger.logError("Error during CONNECT: " + error);
            }
            
          }
          
        }
        
      });
      this.client.getChannel("/meta/subscribe").addListener(new ClientSessionChannel.MessageListener()
      {
        public void onMessage(ClientSessionChannel channel, Message message)
        {
          InboundMessageReceiver.this.m_logger.logDebug("[CHANNEL:META_SUBSCRIBE]: " + message);
          boolean success = message.isSuccessful();
          if (!success) {
            String error = (String)message.get("error");
            if (error != null) {
              InboundMessageReceiver.this.m_logger.logError("Error during SUBSCRIBE: " + error);
            }
          }
        }
      });
      this.client.handshake();
      this.m_logger.logDebug("Waiting for handshake!!");
      boolean handshaken = this.client.waitFor(35000L, BayeuxClient.State.CONNECTED, new BayeuxClient.State[0]);
      if (!handshaken) {
        this.m_logger.logError("Failed to handshake:" + this.client);
        throw new CloudInvocationException(CloudUtil.getResourceBundle().getString("HANDSHAKE_FAILED"));
      }
    }
  }
}