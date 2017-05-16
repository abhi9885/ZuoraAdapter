package com.cognizant.ipm.adapter.wizard;

import java.util.LinkedList;

import oracle.tip.tools.adapters.cloud.impl.CloudAdapterWelcomePage;
import oracle.tip.tools.ide.adapters.cloud.api.plugin.AdapterPluginContext;
import oracle.tip.tools.presentation.uiobjects.sdk.EditField;

import com.cognizant.ipm.adapter.util.AdapterResourceBundle;

public class AdapterWelcomePage
  extends CloudAdapterWelcomePage
{
  public AdapterWelcomePage(AdapterPluginContext context)
  {
    super(context);
  }
  
  public String getHelpId() {
    return "";
  }
  
  public String getWelcomeText() {
	  System.out.println("Executing method "+Thread.currentThread().getStackTrace()[1]);
    return AdapterResourceBundle.getValue("cloudadapter.welcome.page.welcome.text");
  }
  
  public LinkedList<EditField> getPageEditFields()
  {
	  System.out.println("Executing method "+Thread.currentThread().getStackTrace()[1]);
    LinkedList<EditField> fields = super.getPageEditFields();
    return fields;
  }
}