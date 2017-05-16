package com.cognizant.ipm.adapter.plugin.generator;

import java.util.List;

import oracle.tip.tools.adapters.cloud.api.CloudAdapterFilter;
import oracle.tip.tools.ide.adapters.cloud.api.generation.ArtifactGenerator;
import oracle.tip.tools.ide.adapters.cloud.api.generation.RuntimeGenerationContext;
import oracle.tip.tools.ide.adapters.cloud.api.generation.RuntimeMetadataGenerator;
import oracle.tip.tools.ide.adapters.cloud.api.model.TransformationModel;
import oracle.tip.tools.ide.adapters.cloud.api.plugin.AdapterPluginContext;
import oracle.tip.tools.ide.adapters.cloud.impl.generation.AbstractRuntimeMetadataGenerator;

public class AdapterRuntimeMetadataGenerator extends AbstractRuntimeMetadataGenerator
{
  AdapterPluginContext context = null;
  RmgHelper rmgHelper;

  public AdapterRuntimeMetadataGenerator(AdapterPluginContext config)
  {
	  super(config);
	  this.context = config;
	  System.out.println("Executing method "+Thread.currentThread().getStackTrace()[1]);
  }

  protected void internalAddArtifactGenerators(List<ArtifactGenerator> generators, RuntimeGenerationContext context)
  {
	  System.out.println("Executing method "+Thread.currentThread().getStackTrace()[1]);
	  initializeContext(context);
  }

  protected void initializeContext(RuntimeGenerationContext context)
  {
	  System.out.println("Executing method "+Thread.currentThread().getStackTrace()[1]);
	  context.setContextObject("wsdlDefinition", context.getContextObject("wsdlDefinition"));
	  context.setContextObject("runtimeConnectionFactory", "com.cognizant.ipm.adapter.runtime.CloudRuntimeConnectionFactory");
	  context.setContextObject("integrationSchemaNamespace", context.getTransformationModel().getTargetNamespace());
  }

  public RuntimeMetadataGenerator setTransformationModel(TransformationModel model)
  {
	  System.out.println("Executing method "+Thread.currentThread().getStackTrace()[1]);
    CloudAdapterFilter cloudAdapterFilter = (CloudAdapterFilter)this.context.getContextObject("UICloudAdapterFilter");
    if (cloudAdapterFilter.isInbound())
    {
      this.rmgHelper = new OutboundMessagingWsdlRmgHelper(this.context, model);
      this.rmgHelper.setJCAProps();

    }
    else if (!model.getOperationMappings().isEmpty()) {
      this.rmgHelper = new CloudWsdlRmgHelper(this.context, model);
    }
    
    this.rmgHelper.processOperationMappings();
    return super.setTransformationModel(model);
  }
}