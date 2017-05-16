package com.cognizant.ipm.adapter.plugin.generator;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Properties;

import oracle.tip.tools.adapters.cloud.api.CloudAdapterException;
import oracle.tip.tools.ide.adapters.cloud.api.connection.AbstractCloudConnection;
import oracle.tip.tools.ide.adapters.cloud.api.metadata.OperationMapping;
import oracle.tip.tools.ide.adapters.cloud.api.model.TransformationModel;
import oracle.tip.tools.ide.adapters.cloud.api.plugin.AdapterPluginContext;
import oracle.tip.tools.ide.adapters.cloud.api.plugin.CloudApplicationAdapterException;
import oracle.tip.tools.ide.adapters.cloud.api.service.AdapterPluginServiceException;
import oracle.tip.tools.ide.adapters.cloud.api.service.WSDLHelperService;

import com.cognizant.ipm.adapter.util.CloudSessionValidator;
import com.cognizant.ipm.adapter.util.CloudUtil;

public class OutboundMessagingWsdlRmgHelper extends AbstractRmgHelper
{
  public OutboundMessagingWsdlRmgHelper(AdapterPluginContext p_context, TransformationModel p_model)
  {
    super(p_context, p_model);
    this.currentFlow = API_FLOWS.OUTBOUND_MESSAGING.toString();
  }

  public void processOperationMappings() throws CloudAdapterException
  {
    if (!this.context.getContextObject("INBOUND_MODEL_UNDER_PROCESS").toString().equals("REQUEST"))
    {
      CloudWsdlRmgHelper outboundHelper = new CloudWsdlRmgHelper(this.context, this.model);
      outboundHelper.processOperationMappings();
    }
  }

  public void setJCAProps() throws CloudAdapterException
  {
	  System.out.println("Executing method "+Thread.currentThread().getStackTrace()[1]);
    AbstractCloudConnection cloudConnection = (AbstractCloudConnection)this.context.getContextObject("CA_UI_connection");
    Properties connectionProperties = cloudConnection.getConnectionProperties();
    try
    {
      try
      {
        CloudSessionValidator.validateSession(this.context, cloudConnection);
      }
      catch (CloudApplicationAdapterException e) {
        e.printStackTrace();
        throw new CloudAdapterException(e);
      }
      connectionProperties.setProperty("organizationId", this.context.getContextObject("organizationId").toString());
      URL endpointURL = getEndpointUrlFromWSDL();
      String applicationVersion = CloudUtil.getVersionFromURL(endpointURL.toString(), "enterprise");
      connectionProperties.setProperty("applicationVersion", applicationVersion);
      ((OperationMapping)this.model.getOperationMappings().get(0)).getOperationProperties().put("backwardCompatibility", "nsin");
    }
    catch (MalformedURLException e)
    {

      if (this.logger != null) {
        this.logger.logSevere("MalformedURLException caught in RuntimeMetadataGenerator.setTransformationModel() : " + e.getMessage());
      }
      e.printStackTrace();
      throw new CloudAdapterException("zuora Outbound Messaging ArtifactGenerationException", e);
    }
    catch (AdapterPluginServiceException e)
    {
      if (this.logger != null) {
        this.logger.logSevere("AdapterPluginServiceException caught in RuntimeMetadataGenerator => setTransformationModel() => setJCAProps : " + e.getMessage());
      }
      e.printStackTrace();
      throw new CloudAdapterException("zuora Outbound Messaging ArtifactGenerationException", e);
    }
    catch (Exception e)
    {
      if (this.logger != null) {
        this.logger.logSevere("Exception caught in RuntimeMetadataGenerator => setTransformationModel() => setJCAProps : " + e.getMessage());
      }
      throw new CloudAdapterException("zuora Outbound Messaging ArtifactGenerationException", e);
    }
  }

  protected void processResponseMappings() {}
  protected void processRequestMappings() {}
  
  protected URL getEndpointUrlFromWSDL() throws AdapterPluginServiceException, MalformedURLException
  {
	  System.out.println("Executing method "+Thread.currentThread().getStackTrace()[1]);
    WSDLHelperService wsdlHelper = (WSDLHelperService)this.context.getServiceRegistry().getService(WSDLHelperService.class);
    AbstractCloudConnection cloudConnection = (AbstractCloudConnection)this.context.getContextObject("CA_UI_connection");
    Properties connectionProperties = cloudConnection.getConnectionProperties();
    String wsdlURL = connectionProperties.getProperty("targetWSDLURL");
    URL endpointURL = wsdlHelper.getEndpointAddressFromWSDL(new URL(wsdlURL), connectionProperties);
    return endpointURL;
  }
}