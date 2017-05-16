package com.cognizant.ipm.adapter.runtime;

import oracle.tip.tools.ide.adapters.cloud.api.plugin.AdapterPluginContext;
import oracle.tip.tools.ide.adapters.cloud.api.plugin.CloudApplicationAdapter;
import oracle.tip.tools.ide.adapters.cloud.api.plugin.CloudApplicationAdapterFactory;

import com.cognizant.ipm.adapter.plugin.CloudAdapter;

public class AdapterFactory implements CloudApplicationAdapterFactory
{
  public CloudApplicationAdapter createAdapter(AdapterPluginContext context)
  {
	  System.out.println("Executing method "+Thread.currentThread().getStackTrace()[1]);
	  return new CloudAdapter(context);
  }
}
