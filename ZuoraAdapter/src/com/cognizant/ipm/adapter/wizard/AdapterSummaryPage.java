package com.cognizant.ipm.adapter.wizard;

import java.util.Enumeration;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Properties;

import oracle.tip.tools.adapters.cloud.api.CloudAdapterCallerContext;
import oracle.tip.tools.adapters.cloud.api.CloudAdapterFilter;
import oracle.tip.tools.adapters.cloud.impl.CloudAdapterSummaryPage;
import oracle.tip.tools.adapters.cloud.utils.CloudAdapterUtils;
import oracle.tip.tools.ide.adapters.cloud.api.metadata.OperationMapping;
import oracle.tip.tools.ide.adapters.cloud.api.metadata.TypeMapping;
import oracle.tip.tools.ide.adapters.cloud.api.model.Header;
import oracle.tip.tools.ide.adapters.cloud.api.model.TransformationModel;
import oracle.tip.tools.ide.adapters.cloud.api.plugin.AdapterPluginContext;
import oracle.tip.tools.ide.adapters.cloud.impl.metadata.model.TransformationModelBuilder;
import oracle.tip.tools.presentation.uiobjects.sdk.EditField;
import oracle.tip.tools.presentation.uiobjects.sdk.ILabelObject;
import oracle.tip.tools.presentation.uiobjects.sdk.TextAreaObject;
import oracle.tip.tools.presentation.uiobjects.sdk.TextBoxObject;
import oracle.tip.tools.presentation.uiobjects.sdk.UIFactory;
import oracle.tip.tools.presentation.uiobjects.sdk.UIObject;

import com.cognizant.ipm.adapter.util.AdapterResourceBundle;
import com.cognizant.ipm.adapter.util.CloudUtil;

public class AdapterSummaryPage extends CloudAdapterSummaryPage
{
  private LinkedList<EditField> fields = null;
  int rowId = -1;
  
  public AdapterSummaryPage(AdapterPluginContext adapterPluginContext) {
    super(adapterPluginContext);
  }

  public String getWelcomeText()
  {
	  System.out.println("Executing method "+Thread.currentThread().getStackTrace()[1]);
    return AdapterResourceBundle.getValue("cloudadapter.summary.page.welcome.text");
  }

	
  public LinkedList<EditField> getPageEditFields()
  {
	  System.out.println("Executing method "+Thread.currentThread().getStackTrace()[1]);
	  this.fields = new LinkedList();
	  CloudAdapterFilter cloudAdapterFilter = (CloudAdapterFilter)this.adapterPluginContext.getContextObject("UICloudAdapterFilter");
	  if (cloudAdapterFilter.isInbound())
	  {
	    TextBoxObject wsdlTextBoxObj = UIFactory.createTextBox((String)this.adapterPluginContext.getContextObject("OutboundObjectName"), true);
	    this.fields.add(UIFactory.createEditField("WSDLObjectField", "Notification Object for Outbound Messaging", null, false, true, wsdlTextBoxObj, EditField.LabelFieldAlignment.LEFT_LEFT, EditField.LabelFieldLayout.ONE_ROW_LAYOUT));
	    addSeparator(this.fields);
	    addSummaryPageInstructions(this.fields);
	  }
	  else
	  {
	    TransformationModelBuilder modelBuilder = (TransformationModelBuilder)this.adapterPluginContext.getContextObject("_ui_ModelBuilder");
	    TransformationModel model = modelBuilder.build();
	    populateOutboundSummary(this.fields, model, "REQUEST");
	  }
	  return this.fields;
  }

  /**
   * populate outbound summary
   * @param fields
   * @param model
   * @param modelKey
   */
  private void populateOutboundSummary(LinkedList<EditField> fields, TransformationModel model, String modelKey)
  {
	  System.out.println("Executing method "+Thread.currentThread().getStackTrace()[1]);
	  int rowCount = 3;
	  int colLength = 70;
	  int lengthOfStrToDisplay = 0;
	  OperationMapping curOpMapping = (OperationMapping)model.getOperationMappings().get(0);
	  
	  String opInfo = curOpMapping.getTargetOperation().getName();
	  UIObject opInfoObj = UIFactory.createTextBox(opInfo, true);
	  CloudUtil.createOrUpdateField(fields, "opInfoField" + modelKey, 
			  "Selected Operation Name", 
			  null, 
			  false, 
			  false, 
			  opInfoObj, 
			  EditField.LabelFieldLayout.ONE_ROW_LAYOUT, 
			  null, 
			  this.rowId, 
			  null);

	  String selObjsInfo = "";
	  boolean needComma = false;
	  if ((opInfo.equalsIgnoreCase("query")) || (opInfo.equalsIgnoreCase("queryMore")))
	  {
	    selObjsInfo = (String)this.adapterPluginContext.getContextObject("SEL_OBJECTS_FOR_FINISH_PAGE");
	  }
	  else {
	    for (TypeMapping tm : curOpMapping.getRequestObjectMappings()) {
	      if (needComma) {
	        selObjsInfo = selObjsInfo + ", ";
	      }
	      selObjsInfo = selObjsInfo + tm.getTargetDataObject().getName();
	      needComma = true;
	    }
	  }
	  System.out.println("selObjsInfo = "+selObjsInfo);
	  lengthOfStrToDisplay = selObjsInfo.length();
	  rowCount = lengthOfStrToDisplay / colLength;
	  rowCount += 2;
	  if (rowCount > 3) {
	    rowCount = 3;
	  }

	  TextAreaObject selObjsInfoObj = UIFactory.createTextArea(selObjsInfo, colLength, rowCount, true);
	  CloudUtil.createOrUpdateField(fields, 
			  "selObjsInfoField" + modelKey, 
			  "Selected Object(s) Name", 
			  null, false, false, selObjsInfoObj, EditField.LabelFieldLayout.ONE_ROW_LAYOUT, null, this.rowId, null);

	  String selectedHeader = "";
	  Properties properties = curOpMapping.getOperationProperties();
	  List<Header> headers = curOpMapping.getTargetOperation().getRequestHeaders();
	  needComma = false;
	  if (properties != null && !properties.isEmpty()) {
	    Enumeration e = properties.propertyNames();
	    String key; String value; while (e.hasMoreElements()) {
	      key = (String)e.nextElement();
	      value = properties.getProperty(key);
	      for (Header header : headers) {
	        if ((key.contains(".")) && (key.substring(0, key.indexOf(".")).equalsIgnoreCase(header.getName())))
	        {
	          if (needComma) {
	            selectedHeader = selectedHeader + ", ";
	          }
	          selectedHeader = selectedHeader + key + " : " + value;
	          needComma = true;
	          break;
	        }
	      }
	    }
	  }
	  
	  if (selectedHeader.equals("")) {
	    selectedHeader = "No Header Selected";
	  }
	  lengthOfStrToDisplay = selectedHeader.length();
	  rowCount = lengthOfStrToDisplay / colLength;
	  rowCount += 2;
	  if (rowCount > 3) {
	    rowCount = 3;
	  }
	  TextAreaObject headerInfoObj = UIFactory.createTextArea(selectedHeader, colLength, rowCount, true);
	  CloudUtil.createOrUpdateField(fields, "headerInfoField" + modelKey, 
			  "Selected Header(s)", 
			  null, false, false, headerInfoObj, EditField.LabelFieldLayout.ONE_ROW_LAYOUT, null, this.rowId, null);
  }
  
  /**
   * add summary page instructions
   * @param fields
   */
  private void addSummaryPageInstructions(LinkedList<EditField> fields) {
	  System.out.println("Executing method "+Thread.currentThread().getStackTrace()[1]);
	  Locale locale = CloudAdapterUtils.getLocale(this.adapterPluginContext);
	  CloudAdapterFilter cloudAdapterFilter = (CloudAdapterFilter)this.adapterPluginContext.getContextObject("UICloudAdapterFilter");
	  CloudAdapterCallerContext cloudAdapterCallerContext = cloudAdapterFilter.getCloudAdapterCallerContext();
	  if ((cloudAdapterCallerContext == null) || (CloudAdapterCallerContext.PRODUCT.ICS_WEB.equals(cloudAdapterCallerContext.getProduct())))
	  {
	    displayLabel(fields, null, "helpTitle", "After completing the development of Integration Flow, please do carry out the following tasks", ILabelObject.Alignment.LEFT);
	    displayText(fields, "1) Activate the Integration Flow", "helpc1", null, null);
	    displayText(fields, "2) Navigate to Integrations Landing Page in ICS", "helpc2", null, null);
	    displayText(fields, "3) Look for this Integration Flow", "helpc3", null, null);
	    displayText(fields, "4) Click on information icon and copy the WSDL URL", "helpc4", null, null);
	    displayText(fields, "5) Go to the Outbound Messaging section at zuora.com and replace the dummy URL you entered with the ICS endpoint URL which we got in above step", "helpc5", null, null);
	  }
	  else
	  {
	    displayLabel(fields, null, "helpTitle", "After completing the development of SOA/OSB project, please do carry out the following tasks", ILabelObject.Alignment.LEFT);
	    displayText(fields, "1) Deploy the SOA/OSB project", "helpc1", null, null);
	    displayText(fields, "c2=<html><head><title></title></head><body>2) Open the SOA/OSB diagnostic logs from the following path<br>{$Oracle_Home}/user_projects/domains/{your_domain}/servers/{server_name}/logs/{server_name}-diagnostic.log</body></html>", "helpc2", null, null);
	    displayText(fields, "c3=<html><head><title></title></head><body>3) Search and Copy the endpoint URL from the diagnostic logs. It would look something like this<br>For SOA: https://{host}:{port}/integration/flowsvc/zuora/{partition_name}/{project_name}/{service_name}/v1.0/<br>For OSB: https://{host}:{port}/integration/flowsvc/zuora/{project_name}/{service_name}/v1.0/</body></html>", "helpc3", null, null);
	    displayText(fields, "c4=<html><head><title></title></head><body>4) Go to the Outbound Messaging section at zuora.com and replace the dummy URL you entered with the<br>SOA/OSB endpoint URL which we got in above step</body></html>", "helpc4", null, null);
	  }
  }	

  private void addSeparator(List<EditField> headerFields)
  {
	  System.out.println("Executing method "+Thread.currentThread().getStackTrace()[1]);
	  headerFields.add(UIFactory.createEditField("separator", null, null, UIFactory.createSeparatorObject()));
  }
  
  private static void displayLabel(List<EditField> headerFields, String icon, String fieldName, String text, ILabelObject.Alignment align)
  {
	  System.out.println("Executing method "+Thread.currentThread().getStackTrace()[1]);
	  headerFields.add(UIFactory.createEditField(fieldName, null, null, UIFactory.createLabelObject(icon, text, align)));
  }
  
  private void displayText(List<EditField> headerFields, String text, String fieldName, String label, String desc)
  {
	  System.out.println("Executing method "+Thread.currentThread().getStackTrace()[1]);
	  TextBoxObject uiObj = UIFactory.createTextBox(text, true);
	  headerFields.add(UIFactory.createEditField(fieldName, label, desc, uiObj));
  }
}