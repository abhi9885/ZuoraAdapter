package com.cognizant.ipm.adapter.wizard;

import java.util.Locale;

import oracle.tip.tools.adapters.cloud.api.CloudAdapterException;
import oracle.tip.tools.adapters.cloud.api.CloudAdapterFilter;
import oracle.tip.tools.adapters.cloud.api.CloudAdapterUIProvider;
import oracle.tip.tools.adapters.cloud.api.ICloudAdapterUIBinding;

public class AdapterUIProvider extends CloudAdapterUIProvider
{
  public ICloudAdapterUIBinding getCloudAdapterUIBinding(CloudAdapterFilter cloudAdapterFilter, Locale locale)
    throws CloudAdapterException
  {
	  System.out.println("Executing method "+Thread.currentThread().getStackTrace()[1]);
    AdapterUIBinding uiBinding = null;
    try
    {
      uiBinding = new AdapterUIBinding(cloudAdapterFilter, locale);
    } catch (Exception ex) {
      throw new CloudAdapterException(ex.getMessage());
    }
    return uiBinding;
  }
  
  public String getLocalizedAdapterType(Locale locale)
  {
	  System.out.println("Executing method "+Thread.currentThread().getStackTrace()[1]);
	  return "CloudAdapter";
  }
}