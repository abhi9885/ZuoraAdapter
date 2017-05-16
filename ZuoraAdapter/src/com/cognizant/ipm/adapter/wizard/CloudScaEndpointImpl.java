package com.cognizant.ipm.adapter.wizard;

import java.awt.Frame;

import oracle.ide.Context;
import oracle.ide.model.Project;
import oracle.tip.tools.adapters.cloud.api.ICloudAdapterUIBinding;
import oracle.tip.tools.ide.adapters.designtime.adapter.JcaEndpointAbstract;
import oracle.tip.tools.ide.fabric.api.EndpointController;
import oracle.tip.tools.ide.fabric.api.EndpointInfo;
import oracle.tip.tools.ide.fabric.api.SCAEndpointOptions;
import oracle.tip.tools.ide.portablewizard.controller.WizardController;
import oracle.tip.tools.ide.portablewizard.controller.WizardUIHandler;
import oracle.tip.tools.ide.portablewizard.controller.WizardUIHandlerForJcaAdapters;

public class CloudScaEndpointImpl extends JcaEndpointAbstract
{
  public static final String adapterId = "CloudEndPoint";
  
  public EndpointInfo runCreateWizard(Frame frame, Project jdevProject, EndpointController endpointController, SCAEndpointOptions scaEndpointOptions, boolean isExternalReference)
    throws Exception
  {
    Context jcontext = Context.newIdeContext();
    WizardUIHandler uiHandler = new WizardUIHandlerForJcaAdapters(frame, adapterId, endpointController, this.endpointInfo, scaEndpointOptions, isExternalReference);
    ICloudAdapterUIBinding iCloudAdapterUIBinding = new AdapterUIBinding(uiHandler.getFilter(), uiHandler.getLocale());
    String serviceName = WizardController.displayDialog(frame, jcontext, jdevProject, endpointController, scaEndpointOptions, null, null, isExternalReference, iCloudAdapterUIBinding, uiHandler);

    if (serviceName == null) { 
		return null;
    }
    EndpointInfo endpointInfo = endpointController.getEndpointInfo(serviceName);
    return endpointInfo;
  }

  public EndpointInfo runUpdateWizard(Frame frame, Project jdevProject, EndpointController endpointController, SCAEndpointOptions scaEndpointOptions, boolean isExternalReference)
    throws Exception
  {
    return runCreateWizard(frame, jdevProject, endpointController, scaEndpointOptions, isExternalReference);
  }
}