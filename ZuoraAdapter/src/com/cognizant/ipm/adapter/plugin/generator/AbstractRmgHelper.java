package com.cognizant.ipm.adapter.plugin.generator;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.Properties;

import oracle.tip.tools.adapters.cloud.api.CloudAdapterFilter;
import oracle.tip.tools.ide.adapters.cloud.api.connection.AbstractCloudConnection;
import oracle.tip.tools.ide.adapters.cloud.api.metadata.OperationMapping;
import oracle.tip.tools.ide.adapters.cloud.api.metadata.TypeMapping;
import oracle.tip.tools.ide.adapters.cloud.api.model.CloudApplicationModel;
import oracle.tip.tools.ide.adapters.cloud.api.model.CloudOperationNode;
import oracle.tip.tools.ide.adapters.cloud.api.model.TransformationModel;
import oracle.tip.tools.ide.adapters.cloud.api.plugin.AdapterPluginContext;
import oracle.tip.tools.ide.adapters.cloud.api.service.AdapterPluginServiceException;
import oracle.tip.tools.ide.adapters.cloud.api.service.LoggerService;
import oracle.tip.tools.ide.adapters.cloud.api.service.WSDLHelperService;

public abstract class AbstractRmgHelper implements RmgHelper
{
  AdapterPluginContext context = null;
  LoggerService logger;
  TransformationModel model;
  String targetNamespace;
  String currentFlow;
  boolean isInboundFlow = false;
  List<TypeMapping> selectedReqObjects;
  CloudApplicationModel applicationModel;
  CloudOperationNode targetOperation;
  
  public AbstractRmgHelper(AdapterPluginContext p_context, TransformationModel p_model)
  {
    this.context = p_context;
    this.logger = ((LoggerService)this.context.getServiceRegistry().getService(LoggerService.class));
    this.model = p_model;
    this.targetNamespace = this.model.getTargetNamespace();
    CloudAdapterFilter cloudAdapterFilter = (CloudAdapterFilter)this.context.getContextObject("UICloudAdapterFilter");
    
    this.isInboundFlow = cloudAdapterFilter.isInbound();
    if (!this.model.getOperationMappings().isEmpty()) {
      this.selectedReqObjects = ((OperationMapping)this.model.getOperationMappings().get(0)).getRequestObjectMappings();
    }
    
    this.applicationModel = this.context.getCloudApplicationModel();
    this.targetOperation = ((OperationMapping)this.model.getOperationMappings().get(0)).getTargetOperation();
  }

  public void setJCAProps()
  {
    if (!this.currentFlow.equals(API_FLOWS.STREAMING_API)) {
      if (!this.currentFlow.equals(API_FLOWS.CUSTOM_WSDL)) {
        StringBuffer selectedObjString = new StringBuffer();
        boolean chkComma = false;
        for (TypeMapping dataNodeTM : this.selectedReqObjects) {
          if (chkComma) {
            selectedObjString.append(",");
          }
          selectedObjString.append(dataNodeTM.getTargetDataObject().getQualifiedName().getLocalPart());
          
          chkComma = true;
        }
        ((OperationMapping)this.model.getOperationMappings().get(0)).getOperationProperties().put("selectedObjects", selectedObjString.toString());
      }
      ((OperationMapping)this.model.getOperationMappings().get(0)).getOperationProperties().put("oracle.cloud.AdapterNamespace", this.model.getTargetNamespace() + "#new");
    }
  }

  protected URL getEndpointUrlFromWSDL() throws AdapterPluginServiceException, MalformedURLException
  {
    WSDLHelperService wsdlHelper = (WSDLHelperService)this.context.getServiceRegistry().getService(WSDLHelperService.class);
    AbstractCloudConnection cloudConnection = (AbstractCloudConnection)this.context.getContextObject("CA_UI_connection");
    Properties connectionProperties = cloudConnection.getConnectionProperties();
    String wsdlURL = connectionProperties.getProperty("targetWSDLURL");
    URL endpointURL = wsdlHelper.getEndpointAddressFromWSDL(new URL(wsdlURL), connectionProperties);
    return endpointURL;
  }
  
  public void processOperationMappings()
  {
    processRequestMappings();
    processResponseMappings();
    setJCAProps();
  }
  
  protected abstract void processResponseMappings();
  protected abstract void processRequestMappings();
}