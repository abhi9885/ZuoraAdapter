package com.cognizant.ipm.adapter.plugin;

import oracle.tip.tools.ide.adapters.cloud.api.connection.CloudConnection;
import oracle.tip.tools.ide.adapters.cloud.api.generation.RuntimeMetadataGenerator;
import oracle.tip.tools.ide.adapters.cloud.api.metadata.CloudMetadataBrowser;
import oracle.tip.tools.ide.adapters.cloud.api.plugin.AdapterPluginContext;
import oracle.tip.tools.ide.adapters.cloud.api.query.QuerySupport;
import oracle.tip.tools.ide.adapters.cloud.impl.plugin.AbstractCloudApplicationAdapter;

import com.cognizant.ipm.adapter.plugin.generator.AdapterRuntimeMetadataGenerator;
import com.cognizant.ipm.adapter.plugin.metadata.AdapterMetadataBrowser;
import com.cognizant.ipm.adapter.query.CloudQuerySupport;

public class CloudAdapter extends AbstractCloudApplicationAdapter
{
  private static final long serialVersionUID = -122425254381376789L;
  
  public CloudAdapter(AdapterPluginContext adapterPluginContext)
  {
	  super(adapterPluginContext);
	  System.out.println("Executing method "+Thread.currentThread().getStackTrace()[1]);
  }
  
  public CloudConnection getConnection()
  {
	  System.out.println("Executing method "+Thread.currentThread().getStackTrace()[1]);
	  return new CloudAdapterConnection(getPluginContext());
  }
  
  public CloudMetadataBrowser getMetadataBrowser(CloudConnection cloudConnection)
  {
	  System.out.println("Executing method "+Thread.currentThread().getStackTrace()[1]);
	  AdapterPluginContext context = getPluginContext();
	  return new AdapterMetadataBrowser(cloudConnection, context);
  }
  
  public RuntimeMetadataGenerator getRuntimeMetadataGenerator()
  {
	  System.out.println("Executing method "+Thread.currentThread().getStackTrace()[1]);
	  return new AdapterRuntimeMetadataGenerator(getPluginContext());
  }
  
  public String getName()
  {
	  System.out.println("Executing method "+Thread.currentThread().getStackTrace()[1]);
	  return "Cloud Adapter";
  }
  
  public QuerySupport getQuerySupport(CloudConnection connection)
  {
	  System.out.println("Executing method "+Thread.currentThread().getStackTrace()[1]);
	  AdapterPluginContext context = getPluginContext();
	  context.setContextObject("adapterConnection", connection);
	  return new CloudQuerySupport(getPluginContext());
  }
}