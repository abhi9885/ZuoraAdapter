package com.cognizant.ipm.adapter.wizard;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import oracle.tip.tools.adapters.cloud.api.CloudAdapterException;
import oracle.tip.tools.adapters.cloud.api.CloudAdapterPageState;
import oracle.tip.tools.adapters.cloud.api.ICloudAdapterPage;
import oracle.tip.tools.adapters.cloud.impl.CloudAdapterOperationsPage;
import oracle.tip.tools.adapters.cloud.l10n.CloudAdapterText;
import oracle.tip.tools.adapters.cloud.utils.CloudAdapterUtils;
import oracle.tip.tools.adapters.cloud.utils.CloudQueryValidator;
import oracle.tip.tools.ide.adapters.cloud.api.connection.AbstractCloudConnection;
import oracle.tip.tools.ide.adapters.cloud.api.metadata.CloudMetadataBrowser;
import oracle.tip.tools.ide.adapters.cloud.api.metadata.ObjectGrouping;
import oracle.tip.tools.ide.adapters.cloud.api.metadata.OperationMapping;
import oracle.tip.tools.ide.adapters.cloud.api.metadata.TypeMapping;
import oracle.tip.tools.ide.adapters.cloud.api.model.CloudAPINode;
import oracle.tip.tools.ide.adapters.cloud.api.model.CloudDataObjectNode;
import oracle.tip.tools.ide.adapters.cloud.api.model.CloudOperationNode;
import oracle.tip.tools.ide.adapters.cloud.api.model.CloudQueryParameters;
import oracle.tip.tools.ide.adapters.cloud.api.model.Field;
import oracle.tip.tools.ide.adapters.cloud.api.model.Header;
import oracle.tip.tools.ide.adapters.cloud.api.model.ObjectCategory;
import oracle.tip.tools.ide.adapters.cloud.api.model.RequestParameter;
import oracle.tip.tools.ide.adapters.cloud.api.model.TransformationModel;
import oracle.tip.tools.ide.adapters.cloud.api.plugin.AdapterPluginContext;
import oracle.tip.tools.ide.adapters.cloud.api.plugin.CloudApplicationAdapterException;
import oracle.tip.tools.ide.adapters.cloud.api.query.QuerySupport;
import oracle.tip.tools.ide.adapters.cloud.api.query.QueryValidationResult;
import oracle.tip.tools.ide.adapters.cloud.impl.metadata.model.TransformationModelBuilder;
import oracle.tip.tools.ide.adapters.cloud.impl.plugin.AbstractCloudApplicationAdapter;
import oracle.tip.tools.presentation.uiobjects.sdk.ButtonObject;
import oracle.tip.tools.presentation.uiobjects.sdk.EditField;
import oracle.tip.tools.presentation.uiobjects.sdk.IGroupObject;
import oracle.tip.tools.presentation.uiobjects.sdk.ISelectItem;
import oracle.tip.tools.presentation.uiobjects.sdk.ISelectObject;
import oracle.tip.tools.presentation.uiobjects.sdk.ITextAreaObject;
import oracle.tip.tools.presentation.uiobjects.sdk.ITextBoxObject;
import oracle.tip.tools.presentation.uiobjects.sdk.Option;
import oracle.tip.tools.presentation.uiobjects.sdk.SelectObject;
import oracle.tip.tools.presentation.uiobjects.sdk.ShuttleObject;
import oracle.tip.tools.presentation.uiobjects.sdk.TextAreaObject;
import oracle.tip.tools.presentation.uiobjects.sdk.TextBoxObject;
import oracle.tip.tools.presentation.uiobjects.sdk.UIFactory;
import oracle.tip.tools.presentation.uiobjects.sdk.UIObject;

import com.cognizant.ipm.adapter.query.AbstractCloudQueryParser;
import com.cognizant.ipm.adapter.util.AdapterResourceBundle;
import com.cognizant.ipm.adapter.util.AdapterUtil;
import com.cognizant.ipm.adapter.util.CloudUtil;
import com.cognizant.ipm.adapter.util.CloudValidationError;
import com.cognizant.ipm.adapter.util.ValidationKeyword;

public class AdapterOperationsPage extends CloudAdapterOperationsPage {
	private AdapterPluginContext adapterPluginContext;
	private Locale locale = null;
	//private CloudMetadataBrowser browser = null;
	private String prevGroupFileterVal = "";
	private boolean isSuccess = true;

	public AdapterOperationsPage(AdapterPluginContext adapterPluginContext) {
		super(adapterPluginContext);
		this.adapterPluginContext = adapterPluginContext;
		this.locale = CloudAdapterUtils.getLocale(this.adapterPluginContext);
		//this.browser = AdapterUtil.getMetadataBrowser(this.adapterPluginContext);
		System.out.println("Executing method "+Thread.currentThread().getStackTrace()[1]);
	}

	public String getWelcomeText() {
		  System.out.println("Executing method "+Thread.currentThread().getStackTrace()[1]);
		return AdapterResourceBundle.getValue("cloudadapter.operations.page.welcome.text");
	}

	public LinkedList<EditField> getPageEditFields()
	{
		  System.out.println("Executing method "+Thread.currentThread().getStackTrace()[1]);
		  LinkedList<EditField> currentPageFields = super.getPageEditFields();
		  EditField opModeField = null;
		  EditField processingField = null;
		  for (EditField edf : currentPageFields) {
		    if (edf.getName().equalsIgnoreCase("operationMode")) {
		      opModeField = edf;
		    } else if (edf.getName().equalsIgnoreCase("processingOptions"))
		    {
		      processingField = edf;
		    }
		  }
		  if (opModeField != null) {
		    currentPageFields.remove(opModeField);
		  }
		  if (processingField != null) {
		    currentPageFields.remove(processingField);
		  }
		  try {
		    populateCloudAPIs(currentPageFields, getSelectedOperation());
		  } catch (Exception ex) {
		    throw new CloudAdapterException(ex.getMessage(), ex);
		  }
		  return currentPageFields;
	}

	/**
	 * populates the API datamodel on the wizard
	 * @param editFields
	 * @param selectedOperation
	 */
	private void populateCloudAPIs(LinkedList<EditField> editFields, OperationMapping selectedOperation) {
		  System.out.println("Executing method "+Thread.currentThread().getStackTrace()[1]);
		CloudAPINode apiNode = null;
		CloudOperationNode selectedOpNode = null;
	     try {
	       List<CloudAPINode> apis = getBrowserFromContext().getAPIs();
	       List<ISelectItem> apiItems = new ArrayList<ISelectItem>();
	       String[] apiNames = new String[apis.size()];
	       for (int i = 0; i < apis.size(); i++) {
	         apiNames[i] = ((CloudAPINode)apis.get(i)).getName();
	       }
	       Arrays.sort(apiNames);
	       String firstApi = apiNames[1];
	       
	       for (int i = 0; i < apiNames.length; i++) {
	         if (apiNames[i].equalsIgnoreCase("CORE")) {
	           apiItems.add(UIFactory.createSelectItem(apiNames[i], "Represents all core operations supported by the zuora application"));
	         }
	         else if (apiNames[i].equalsIgnoreCase("CRUD")) {
	           apiItems.add(UIFactory.createSelectItem(apiNames[i], "Represents all CRUD operations supported by the zuora application"));
	         }
	         else if (apiNames[i].equalsIgnoreCase("MISC")) {
	           apiItems.add(UIFactory.createSelectItem(apiNames[i], "Represents all MISC operations supported by the zuora application"));
	         }
	         else if (apiNames[i].equalsIgnoreCase("ZOQL")) {
	           apiItems.add(UIFactory.createSelectItem(apiNames[i], "Represents all ZOQL operations supported by the zuora application"));
	         }
	       }
	       apiNode = CloudAdapterUtils.getCloudAPINodeFromList(apis, firstApi);
	       System.out.println("selectedOperation "+selectedOperation);
	       if (selectedOperation != null) {
	         Properties opProps = selectedOperation.getOperationProperties();
	         String qString = opProps.getProperty("zoql.queryString");
	         
	         if ((qString != null) && (!qString.isEmpty())) {
	           apiNode = CloudAdapterUtils.getCloudAPINodeFromList(apis, "ZOQL");
	           selectedOpNode = selectedOperation.getTargetOperation();
	         } 
	         else if (apis != null) {
	           for (CloudAPINode searchNode : apis) {
	             List<CloudOperationNode> operations = this.getBrowserFromContext().getOperations(searchNode);
	             ListIterator<CloudOperationNode> opit = operations.listIterator();
	             while (opit.hasNext()) {
	               CloudOperationNode operationNode = (CloudOperationNode)opit.next();
	               String opName = operationNode.getName();
	               if (opName.equals(selectedOperation.getTargetOperation().getName()))
	               {
	                 firstApi = searchNode.getName();
	                 apiNode = searchNode;
	                 selectedOpNode = selectedOperation.getTargetOperation();
	                 break;
	               }
	             }
	           }
	         }
	       }
	       
	       Option[] options = new Option[apiItems.size()];
	       for (int i = 0; i < apiItems.size(); i++) {
	         options[i] = ((Option)apiItems.get(i));
	       }
	       UIObject apisObj = new SelectObject(options, apiNode.getName(), 0, true, false, "", null, true, "", null);
	       CloudUtil.createOrUpdateField(
	    		   editFields, "cloudApi", 
	    		   CloudAdapterText.getString(locale, "operations.page.cloudApiLabel"),
	    		   CloudAdapterText.getString(locale, "operations.page.cloudApiDesc"), 
	    		   false, 
	    		   false, 
	    		   apisObj, 
	    		   EditField.LabelFieldLayout.ONE_ROW_LAYOUT, 
	    		   CloudAdapterText.getString(locale, "operations.page.cloudApiDesc"), 
	    		   3, 
	    		   EditField.LabelFieldAlignment.LEFT_LEFT);
	     }
	     catch (Exception ex)
	     {
	       throw new CloudAdapterException(ex.getMessage(), ex);
	     }
	     handleCloudAPIChange(editFields, apiNode.getName(), locale, selectedOpNode, selectedOperation);
   }
	
	private void handleCloudAPIChange(LinkedList<EditField> currentPageFields, String newValue, Locale locale, CloudOperationNode selectedOpNode, OperationMapping selectedOperation)
	{
		  System.out.println("Executing method "+Thread.currentThread().getStackTrace()[1]);
		  System.out.println("newValue = "+newValue);
		  System.out.println("selectedOpNode "+ selectedOpNode);
		  List<CloudAPINode> apis = this.getBrowserFromContext().getAPIs();
		  CloudAPINode cloudAPINode = CloudAdapterUtils.getCloudAPINodeFromList(apis, newValue);
		  List<CloudOperationNode> operationNodes = getBrowserFromContext().getOperations(cloudAPINode);
		  selectedOpNode = selectedOpNode == null ? getDefaultSelectedOperation(operationNodes) : selectedOpNode;
		  populateOperations(currentPageFields, cloudAPINode, selectedOpNode);
		  if ("ZOQL".equals(newValue)) {
		    populateQueryField(currentPageFields, locale, selectedOpNode, selectedOperation);
		  }
		  else {
		    populateBusinessObjects(currentPageFields, selectedOpNode, selectedOperation, null);
		  }
	}
	
	/**
	 * populates business object upon selection of cloud operation type and cloud operation
	 * @param editFields
	 * @param operationNode
	 * @param selectedOperation
	 * @param groupFilterValue
	 */
	private void populateBusinessObjects(LinkedList<EditField> editFields, CloudOperationNode operationNode, OperationMapping selectedOperation, String groupFilterValue)
	{
		  System.out.println("Executing method "+Thread.currentThread().getStackTrace()[1]);
		  try
		  {
		    if ((null != this.prevGroupFileterVal) && (!this.prevGroupFileterVal.isEmpty()) && ((groupFilterValue == null) || (groupFilterValue.isEmpty()) || (groupFilterValue.equalsIgnoreCase(this.prevGroupFileterVal))))
		    {
		      groupFilterValue = this.prevGroupFileterVal;
		    }
		    System.out.println("groupFilterValue = "+groupFilterValue);
		    this.prevGroupFileterVal = groupFilterValue;
		    removeQueryFields(editFields);
		    List<CloudDataObjectNode> dataObjects = getBrowserFromContext().getDataObjectNodes(operationNode);
		    System.out.println("dataObjects = "+dataObjects);
		    System.out.println("selectedOperation = "+selectedOperation);
		    List<String> selectedDataObjects = new ArrayList<String>();
		    String[] bizObjNames = new String[dataObjects.size()];
		    for (int i = 0; i < dataObjects.size(); i++) {
		      bizObjNames[i] = ((CloudDataObjectNode)dataObjects.get(i)).getName();
		    }
		    Arrays.sort(bizObjNames, String.CASE_INSENSITIVE_ORDER);
		    System.out.println("operationNode.getName() = "+operationNode.getName());
		    if ((selectedOperation != null) && (!"QueryObjects".equals(operationNode.getName())))
		    {
		      if ((operationNode.getName() != null) && 
		    		  ((operationNode.getName().equalsIgnoreCase("generate")) || 
					  (operationNode.getName().equalsIgnoreCase("amend")) || 
					  (operationNode.getName().equalsIgnoreCase("execute")) || 
					  (operationNode.getName().equalsIgnoreCase("create")) || 
					  (operationNode.getName().equalsIgnoreCase("delete")) || 
					  (operationNode.getName().equalsIgnoreCase("update"))))
		      {
		        Properties properties = selectedOperation.getOperationProperties();
		        Set<String> opprops = properties.stringPropertyNames();
		        if (opprops.contains("selectedObjects"))
		        {
		          String val = properties.getProperty("selectedObjects");
		          if ((val != null) && (val.length() > 0)) {
		            String[] objects = val.split("\\,");
		            for (int i = 0; i < objects.length; i++) {
		              String objName = objects[i];
		              CloudDataObjectNode dataNode = CloudUtil.getBusinessObjectByName(this.adapterPluginContext, objName);
		              if (dataNode != null) {
		                selectedDataObjects.add(dataNode.getName());
		              }
		            }
		          }
		        }
		        else {
		          List<TypeMapping> selectedObjects = selectedOperation.getRequestObjectMappings();
		          System.out.println("selectedOperation.getRequestObjectMappings() ="+selectedOperation.getRequestObjectMappings());
		          for (TypeMapping dataNodeTM : selectedObjects) {
		            CloudDataObjectNode dataNode = CloudUtil.getBusinessObjectByName(this.adapterPluginContext, dataNodeTM.getTargetDataObject().getQualifiedName().getLocalPart());
	
		            if (dataNode != null) {
		              selectedDataObjects.add(dataNode.getName());
		            }
		          }
		        }
		      }
		      else {
		    	  System.out.println("selectedOperation.getRequestObjectMappings() = "+selectedOperation.getRequestObjectMappings());
		    	  for (TypeMapping typeMapping : selectedOperation.getRequestObjectMappings())
		    	  {
		    		  selectedDataObjects.add(typeMapping.getTargetDataObject().getName());
		    	  }
		      	}
		    }
		    else {
		      Map<String, UIObject> map = EditField.getObjectMap((EditField[])editFields.toArray(new EditField[editFields.size()]));
		      if (map.containsKey("cloudBizObj")) {
		        UIObject uiobject = (UIObject)map.get("cloudBizObj");
		        if ((uiobject instanceof ShuttleObject)) {
		          List valuesList = ((ShuttleObject)uiobject).getSelectedValues();
		          if ((valuesList != null) && (valuesList.size() > 0)) {
		            String[] selObjArr = (String[])valuesList.toArray(new String[valuesList.size()]);
		            if ((selObjArr != null) && (selObjArr.length > 0)) {
		              List<String> selList = Arrays.asList(bizObjNames);
		              for (String str : selObjArr) {
		                if (selList.contains(str)) {
		      		      System.out.println("map = "+map);
		                  selectedDataObjects.add(str);
		                }
		              }
		            }
		          }
		        }
		      }
		    }
		    
		    String[] groupValues = new String[ObjectCategory.values().length];
		    groupValues[0] = "ALL";
		    int i = 1;
		    for (ObjectCategory obj : ObjectCategory.values())
		      if (!obj.name().equalsIgnoreCase("builtin"))
		      {
		        groupValues[i] = obj.name();
		        i++;
		      }
		    List<ISelectItem> grpItems = new ArrayList();
		    for (int k = 0; k < groupValues.length; k++) {
		      if (groupValues[k].equalsIgnoreCase("ALL")) {
		        grpItems.add(UIFactory.createSelectItem(groupValues[k], "Displays all business objects"));
	
		      }
		      else if (groupValues[k].equalsIgnoreCase("CUSTOM"))
		      {
		        grpItems.add(UIFactory.createSelectItem(groupValues[k], "Displays business objects you created. Custom business objects are appended with an _c"));
		      }
		      else if (groupValues[k].equalsIgnoreCase("STANDARD"))
		      {
		        grpItems.add(UIFactory.createSelectItem(groupValues[k], "Displays objects delivered as part of the zuora application"));
		      }
		    }
		    Option[] options = new Option[grpItems.size()];
		    for (int p = 0; p < grpItems.size(); p++) {
		      options[p] = ((Option)grpItems.get(p));
		    }
		    UIObject groupFilter = new SelectObject(options, groupFilterValue, 0, true, false, "", null, true, "", null);
		    
		    List<CloudAPINode> apis = getBrowserFromContext().getAPIs();
		    String cloudApi = ((ISelectObject)((EditField)EditField.getFieldMap((EditField[])editFields.toArray(new EditField[editFields.size()])).get("cloudApi")).getObject()).getSelectedValue();
		    CloudAPINode cloudAPINode = CloudAdapterUtils.getCloudAPINodeFromList(apis, cloudApi);
		    dataObjects = sortObjects(dataObjects);
		    List<ISelectItem> selectItems = createSelectItems(dataObjects, groupFilterValue);
		    UIObject bizObjNamesObj = null;
		    if ((selectItems != null) && (selectItems.size() == 1) && (selectedDataObjects.size() == 0))
		    {
		      selectedDataObjects.add(((ISelectItem)selectItems.get(0)).getFormattedValue());
		    }

		    System.out.println("selectedDataObjects = "+selectedDataObjects);
		    System.out.println("bizObjNames = "+bizObjNames);
		    if (bizObjNames.length > 0) {
		      bizObjNamesObj = UIFactory.createShuttleObject(selectItems, 
		    		  "Select Business Objects", 
		    		  selectedDataObjects, 
		    		  "Your Selected Business Objects", 
		    		  "Filter By object name", 
		    		  false, 
		    		  CloudAdapterText.getString(locale, "operations.page.cloudBizObj.itemdesc.label"), 
		    		  groupFilter);
		      this.createOrReplaceField(editFields, 
		    		  "cloudBizObj", 
		    		  null, 
		    		  "Business Objects", 
		    		  true, 
		    		  bizObjNamesObj, 
		    		  "queryEditor", 
		    		  EditField.LabelFieldLayout.TWO_ROW_LAYOUT, 
		    		  -1);
		    }
		    else
		    {
		      bizObjNamesObj = UIFactory.createTextBox("No objects available", true);
		      this.createOrReplaceField(editFields, 
		    		  "cloudBizObj", 
		    		  CloudAdapterText.getString(locale, "operations.page.cloudBizObjLabel"), 
		    		  "Move the selected business objects to the Your Selected Business Objects column", 
		    		  false, 
		    		  bizObjNamesObj, 
		    		  "queryEditor", 
		    		  EditField.LabelFieldLayout.TWO_ROW_LAYOUT, 
		    		  -1);
		    }
	
		    if ((operationNode != null) && ("process".equals(operationNode.getName())))
		    {
	
		      String[] processOptions = new String[2];
		      processOptions[0] = "ProcessSubmitRequest";
		      processOptions[1] = "ProcessWorkitemRequest";
		      
		      String selected = processOptions[0];
		      if (selectedOperation != null) {
		        Properties properties = selectedOperation.getOperationProperties();
		        
		        Set<String> opprops = properties.stringPropertyNames();
		        if (opprops.contains("processType")) {
		          selected = properties.getProperty("processType");
		        }
		      }
		      
		      UIObject processOptObj = UIFactory.createSelectObject(processOptions, processOptions, selected, 2, Boolean.TRUE.booleanValue());
		      CloudUtil.createOrUpdateField(editFields, 
		    		  "processOptions", 
		    		  "Process mode", 
		    		  "Process mode", 
		    		  true, 
		    		  false, 
		    		  processOptObj, 
		    		  EditField.LabelFieldLayout.ONE_ROW_LAYOUT, 
		    		  "ProcessSubmitRequest: Submit an array of objects to the approval process. Objects cannot already be in an approval process when submitted" + 
		    		  "\nProcessWorkitemRequest: Process an object that has been submitted to the approval process by performing an approval action (Approve or Reject)", 
		    		  -1, 
		    		  EditField.LabelFieldAlignment.LEFT_LEFT);
		    }
		    else
		    {
	    	  Map<String, EditField> fieldsMap = EditField.getFieldMap((EditField[])editFields.toArray(new EditField[editFields.size()]));
	    	  EditField tobeReplacedField = (EditField)fieldsMap.get("processOptions");
	    	  int index = -1;
	    	  if (tobeReplacedField != null) {
	    	    index = editFields.indexOf(tobeReplacedField);
	    	    if (index > -1) {
	    	      editFields.remove(index);
	    	    }
	    	  }
		    }
		  }
		  catch (Exception ex) {
		    throw new CloudAdapterException(ex.getMessage(), ex);
		  }
	}
	
	private List<CloudDataObjectNode> sortObjects(List<CloudDataObjectNode> unsortedObjects)
	{
		  System.out.println("Executing method "+Thread.currentThread().getStackTrace()[1]);
		  Collections.sort(unsortedObjects, new CloudDataObjectNodeComparer());
		  return unsortedObjects;
	}


	public class CloudDataObjectNodeComparer implements Comparator<CloudDataObjectNode>
	{
	  public CloudDataObjectNodeComparer() {}
	  
	  public int compare(CloudDataObjectNode node1, CloudDataObjectNode node2)
	  {
	    String name1 = node1.getName();
	    String name2 = node2.getName();
	    return name1.compareToIgnoreCase(name2);
	  }
	}
	

	/**
	 * removes query fields from wizard
	 * @param editFields
	 */
	private void removeQueryFields(LinkedList<EditField> editFields)
	{
		  System.out.println("Executing method "+Thread.currentThread().getStackTrace()[1]);
	  Map<String, EditField> fieldsMap = EditField.getFieldMap((EditField[])editFields.toArray(new EditField[editFields.size()]));
	  
	  EditField tobeReplacedField = (EditField)fieldsMap.get("separator1");
	  int index = -1;
	  if (tobeReplacedField != null) {
	    index = editFields.indexOf(tobeReplacedField);
	    if (index > -1) {
	      editFields.remove(index);
	    }
	  }
	  tobeReplacedField = (EditField)fieldsMap.get("resObjectCount");
	  
	  if (tobeReplacedField != null) {
	    index = editFields.indexOf(tobeReplacedField);
	    if (index > -1) {
	      editFields.remove(index);
	    }
	  }
	  
	  tobeReplacedField = (EditField)fieldsMap.get("testQuery");
	  if (tobeReplacedField != null) {
	    index = editFields.indexOf(tobeReplacedField);
	    if (index > -1) {
	      editFields.remove(index);
	    }
	  }
	  
	  tobeReplacedField = (EditField)fieldsMap.get("queryResults");
	  if (tobeReplacedField != null) {
	    index = editFields.indexOf(tobeReplacedField);
	    if (index > -1) {
	      editFields.remove(index);
	    }
	  }
	  
	  tobeReplacedField = (EditField)fieldsMap.get("params");
	  if (tobeReplacedField != null) {
	    index = editFields.indexOf(tobeReplacedField);
	    if (index > -1) {
	      editFields.remove(index);
	    }
	  }
	  
	  tobeReplacedField = (EditField)fieldsMap.get("restxt");
	  if (tobeReplacedField != null) {
	    index = editFields.indexOf(tobeReplacedField);
	    if (index > -1) {
	      editFields.remove(index);
	    }
	  }
	  
	  tobeReplacedField = (EditField)fieldsMap.get("Refresh");
	  
	  if (tobeReplacedField != null) {
	    index = editFields.indexOf(tobeReplacedField);
	    if (index > -1) {
	      editFields.remove(index);
	    }
	  }
	}
	
	
	/**
	 * populates query field values
	 * @param editFields
	 * @param locale
	 * @param selectedOpNode
	 * @param selectedOperation
	 */
	private void populateQueryField(LinkedList<EditField> editFields, Locale locale, CloudOperationNode selectedOpNode, OperationMapping selectedOperation)
	{
		  System.out.println("Executing method "+Thread.currentThread().getStackTrace()[1]);
		  LinkedList<EditField> toolBarFields = new LinkedList();
		  Map<String, EditField> fieldsMap = EditField.getFieldMap((EditField[])editFields.toArray(new EditField[editFields.size()]));
		  EditField tobeReplacedField = (EditField) fieldsMap.get("processOptions");
		  System.out.println("fieldsMap "+fieldsMap);
		  int index = -1;
		  if (tobeReplacedField != null) {
		    index = editFields.indexOf(tobeReplacedField);
		    if (index > -1) {
		      editFields.remove(index);
		    }
		  }
		  String value = "";
		  if ((selectedOperation != null) && (selectedOperation.getOperationProperties() != null))
		  {
		    value = selectedOperation.getOperationProperties().getProperty("zoql.queryString");
		  }
		  
		  if (value.isEmpty()) {
		    String curop = selectedOpNode.getName();
		    value = "SELECT firstName,lastName,fax FROM Contact WHERE fax='2' or (firstName='Billy' and lastName='A') or (fax='5' and firstName='Jerome')";
		  }
	
		  String labelStr = "";
		  String toolTip = "";
		  if (selectedOpNode.getName().equals("query"))
		  {
		    labelStr = "Query Statement";
		    toolTip = "Enter a query statement and click Test My Query. A sample query is provided based on the operation you select";
		  }
		  else if (selectedOpNode.getName().equals("queryMore"))
		  {
		    labelStr = "Query Statement";
		    toolTip = "Enter a queryMore statement and click Test My Query. A sample query is provided based on the operation you select";
		  }
		  
		  TextAreaObject queryEditor = null;
		  if ((selectedOperation != null) && (selectedOperation.getOperationProperties() != null))
		  {
			  queryEditor = UIFactory.createTextArea(true, value, 70, 8, false, "");
		  }
		  else {
			  queryEditor = UIFactory.createTextArea(true, value, 70, 8, false, value);
		  }
		  
		  this.createOrReplaceField(editFields, "queryEditor", labelStr, toolTip, true, queryEditor, "cloudBizObj", EditField.LabelFieldLayout.TWO_ROW_LAYOUT, 5);
		  ButtonObject refreshButton = UIFactory.createButtonObject(true, "Refresh");
		  boolean isRefreshButtonExists = false;
		  for (EditField e : editFields) {
		    if (e.getName().equals("Refresh"))
		    {
		      isRefreshButtonExists = true;
		      break;
		    }
		  }
		  
		  EditField queryEditorField = (EditField)fieldsMap.get("queryEditor");
		  int row = 1;
		  if(queryEditorField != null) {
			  queryEditorField.getRowIdentifier();
		  }
		  if (!isRefreshButtonExists) {
		    editFields.add(UIFactory.createEditField("Refresh", null, "Refresh", false, false, refreshButton, EditField.LabelFieldLayout.ONE_ROW_LAYOUT, null, row + 1, EditField.LabelFieldAlignment.RIGHT_LEFT));
		  }
		  refreshBindingParams(value, editFields);
		  UIObject separatorObject = UIFactory.createSeparatorObject();
		  this.createOrReplaceField(editFields, "separator1", "", "", false, separatorObject, null, EditField.LabelFieldLayout.ONE_ROW_LAYOUT, 0);
		  ButtonObject testQueryButton = UIFactory.createButtonObject(true, "Test My Query");
		  String testQToolTip = "Click to validate your query against the zuora application. Query results are displayed.";
		  this.createOrReplaceField(editFields, "testQuery", "", testQToolTip, false, testQueryButton, null, EditField.LabelFieldLayout.ONE_ROW_LAYOUT, 7);
		  TextAreaObject textArea = UIFactory.createTextArea("No Result", 100, 10, false);
		  this.createOrReplaceField(editFields, "queryResults", "", CloudAdapterText.getString(locale, "query.test.page.queryResultsDesc"), false, textArea, null, EditField.LabelFieldLayout.ONE_ROW_LAYOUT, -1);
	}
	
	/**
	 * refresh binding params
	 * @param query
	 * @param currentPageFields
	 */
	private void refreshBindingParams(String query, LinkedList<EditField> currentPageFields)
	{
		  System.out.println("Executing method "+Thread.currentThread().getStackTrace()[1]);
	  LinkedList<EditField> paramFields = new LinkedList();
	  List<String> params = null;
	  TextBoxObject textObj = null;
	  if (query != null) {
	    params = CloudQueryValidator.getQueryParams(this.adapterPluginContext, query);
	    if ((params == null) || (params.isEmpty())) {
	      textObj = UIFactory.createTextBox("No Parameters", true);
	      paramFields.add(UIFactory.createEditField("", "", "", false, false, textObj, EditField.LabelFieldLayout.ONE_ROW_LAYOUT));
	    }
	    else
	    {
	      LinkedList<EditField> paramsFields = new LinkedList();
	      for (String param : params) {
	        textObj = UIFactory.createTextBox("");
	        paramFields.add(UIFactory.createEditField(param, param, "", false, false, textObj, EditField.LabelFieldLayout.ONE_ROW_LAYOUT, null, -1, EditField.LabelFieldAlignment.LEFT_LEFT));
	      }
	    }
	  }
	  else
	  {
	    textObj = UIFactory.createTextBox("No Parameters", true);
	    paramFields.add(UIFactory.createEditField("", "", "", false, false, textObj, EditField.LabelFieldLayout.ONE_ROW_LAYOUT));
	  }
	  
	  paramFields = sortEditFields(paramFields);
	  textObj = UIFactory.createTextBox("", true);
	  paramFields.add(0, UIFactory.createEditField("bindingParameters", "Binding Parameters", "", false, false, textObj, EditField.LabelFieldLayout.ONE_ROW_LAYOUT, "Displays any binding parameters included in the specified query", EditField.LabelFieldAlignment.LEFT_LEFT));
	  IGroupObject secHeader = UIFactory.createGroupObject(paramFields);
	  CloudUtil.createOrUpdateField(currentPageFields, "params", "Parameter Bindings", "Parameter Bindings", false, false, secHeader, EditField.LabelFieldLayout.ONE_ROW_LAYOUT, null, -1, EditField.LabelFieldAlignment.LEFT_LEFT);
	}
	
	private LinkedList<EditField> sortEditFields(LinkedList<EditField> unsortedObjects)
	{
	  Collections.sort(unsortedObjects, new EditFieldComparer());
	  return unsortedObjects;
	}

	public class EditFieldComparer implements Comparator<EditField>
	{
	  public EditFieldComparer() {}
	  
	  public int compare(EditField node1, EditField node2) {
	    String name1 = node1.getName();
	    String name2 = node2.getName();
	    return name1.compareToIgnoreCase(name2);
	  }
	}
	
	private void populateOperations(LinkedList<EditField> editFields, CloudAPINode firstApiNode, CloudOperationNode selectedOpNode)
	{
		  System.out.println("Executing method "+Thread.currentThread().getStackTrace()[1]);
		  String targetOp = null;
		  try {
		    List<CloudOperationNode> operationNodes = this.getBrowserFromContext().getOperations(firstApiNode);
		    String[] operationNames = getSortedOperation(operationNodes);
		    targetOp = selectedOpNode == null ? getDefaultSelectedOperation(operationNodes).getName() : selectedOpNode.getName();
		    System.out.println("targetOp = "+targetOp);
		    System.out.println("operationNames = "+operationNames);
		    UIObject operationsObj = UIFactory.createSelectObject(operationNames, operationNames, targetOp, 0, Boolean.TRUE.booleanValue());
		    CloudUtil.createOrUpdateField(
		    		editFields, 
		    		"cloudOperation", 
		    		null, 
		    		CloudAdapterText.getString(this.locale, "operations.page.cloudOperationDesc"), 
		    		false, 
		    		false, 
		    		operationsObj, 
		    		EditField.LabelFieldLayout.ONE_ROW_LAYOUT, 
		    		null, 
		    		3, 
		    		EditField.LabelFieldAlignment.LEFT_LEFT);
		  }
		  catch (Exception ex)
		  {
		    ex.printStackTrace();
		  }
	}

	/**
	 * this needs to be updated
	 
	public CloudAdapterPageState updateBackEndModel(LinkedHashMap<String, ICloudAdapterPage> wizardPages, LinkedList<EditField> currentPageFields) {
		System.out.println("Executing method "+Thread.currentThread().getStackTrace()[1]);
		Properties props = new Properties();
		Map map = EditField.getObjectMap((EditField[]) currentPageFields.toArray(new EditField[currentPageFields.size()]));
		CloudAdapterPageState state = new CloudAdapterPageState(false,wizardPages, currentPageFields);
		transferUIDataToModel(props, map);
		return state;
	}*/
	
	protected TransformationModelBuilder getTransformationModelBuilder()
	{
		return (TransformationModelBuilder)this.adapterPluginContext.getContextObject("_ui_ModelBuilder");
	}
	
	public CloudAdapterPageState updateBackEndModel(LinkedHashMap<String, ICloudAdapterPage> wizardPages, LinkedList<EditField> currentPageFields)
	{
		System.out.println("Executing method "+Thread.currentThread().getStackTrace()[1]);
	  Properties props = new Properties();
	  //props.put("outboundWSDLType", "Enterprise WSDL");
	  Map<String, UIObject> map = EditField.getObjectMap((EditField[])currentPageFields.toArray(new EditField[currentPageFields.size()]));
	  String mode = UIFactory.getStringValue(map, "operationMode");
	  CloudAdapterPageState state = new CloudAdapterPageState(false, wizardPages, currentPageFields);
	  transferUIDataToModel(props, map);
	  String curOp = UIFactory.getStringValue(map, "cloudOperation");
	  System.out.println("curOp = "+curOp);
	  if (curOp.equalsIgnoreCase("process"))
	  {
	    TransformationModelBuilder tmb = getTransformationModelBuilder();
	    TransformationModel tmodel = tmb.build();
	    OperationMapping opMapping = null;
	    if (tmodel.getOperationMappings().size() > 0) {
	      opMapping = (OperationMapping)tmodel.getOperationMappings().get(0);
	    }
	    Properties p = new Properties();
	    SelectObject field = (SelectObject)map.get("processOptions");
	    if (field.getSelectedValue().equals("ProcessSubmitRequest"))
	    {
	      this.adapterPluginContext.setContextObject("processType", "ProcessSubmitRequest");
	      p.setProperty("processType", "ProcessSubmitRequest");
	    }
	    else
	    {
	      this.adapterPluginContext.setContextObject("processType", "ProcessWorkitemRequest");
	      p.setProperty("processType", "ProcessWorkitemRequest");
	    }
	    
	    if (opMapping != null) {
	      p = AdapterUtil.mergeProperties(p, opMapping.getOperationProperties());
	      opMapping.setOperationProperties(p);
	    }
	  }
	  state.setRefreshParentPage(true);
	  return state;
	}
	
	protected void transferUIDataToModel(Properties props, Map<String, UIObject> map)
	{
		System.out.println("Executing method "+Thread.currentThread().getStackTrace()[1]);
	  String value = UIFactory.getStringValue(map, "cloudApi");
	  if ("ZOQL".equals(value)) {
	    String query = UIFactory.getStringValue(map, "zoqlQueryEditor");
	    if (query != null) {
	      props.put("zoql.queryString", query);
	    }
	  }
	  CloudMetadataBrowser browser = CloudAdapterUtils.getMetadataBrowser(this.adapterPluginContext);
	  String cloudOpName = UIFactory.getStringValue(map, "cloudOperation");
	  setOperationMappingsForGenerator(this.adapterPluginContext, map, browser, props, getSelectedOperation(), getTransformationModelBuilder(), isSelectedOperationChanged(cloudOpName));
	}
	
	/**
	 * check if selected operation changed
	 * @param cloudOpName
	 * @return
	 */
	private boolean isSelectedOperationChanged(String cloudOpName)
	{
		System.out.println("Executing method "+Thread.currentThread().getStackTrace()[1]);
	  boolean isSelectedOpChanged = true;
	  String pageId = getPageId();
	  String previousSelectedOp = "";
	  if (pageId.equalsIgnoreCase("sfdcInboundResponseSuccessChildPageId"))
	  {
	    previousSelectedOp = (String)this.adapterPluginContext.getContextObject("CALLBACK_SUCCESS_PREVIOUS_SELECTED_OPERATION");
	    if ((previousSelectedOp != null) && (previousSelectedOp.equalsIgnoreCase(cloudOpName)))
	    {
	      isSelectedOpChanged = false; }
	    this.adapterPluginContext.setContextObject("CALLBACK_SUCCESS_PREVIOUS_SELECTED_OPERATION", cloudOpName);
	  }
	  else if (pageId.equalsIgnoreCase("sfdcInboundResponseFailChildPageId"))
	  {
	    previousSelectedOp = (String)this.adapterPluginContext.getContextObject("CALLBACK_FAILURE_PREVIOUS_SELECTED_OPERATION");
	    if ((previousSelectedOp != null) && (previousSelectedOp.equalsIgnoreCase(cloudOpName)))
	    {
	      isSelectedOpChanged = false; }
	    this.adapterPluginContext.setContextObject("CALLBACK_FAILURE_PREVIOUS_SELECTED_OPERATION", cloudOpName);
	  }
	  else
	  {
	    previousSelectedOp = (String)this.adapterPluginContext.getContextObject("OUTBOUND_PREVIOUS_SELECTED_OPERATION");
	    if ((previousSelectedOp != null) && (previousSelectedOp.equalsIgnoreCase(cloudOpName)))
	    {
	      isSelectedOpChanged = false; }
	    this.adapterPluginContext.setContextObject("OUTBOUND_PREVIOUS_SELECTED_OPERATION", cloudOpName);
	  }
	  return isSelectedOpChanged;
	}
	
	/**
	 * 
	 * @param adapterPluginContext
	 * @param map
	 * @param browser
	 * @param props
	 * @param opMapping
	 * @param modelBuilder
	 * @param isSelectedOperationChanged
	 */
	public static void setOperationMappingsForGenerator(AdapterPluginContext adapterPluginContext, Map<String, UIObject> map, CloudMetadataBrowser browser, Properties props, OperationMapping opMapping, TransformationModelBuilder modelBuilder, boolean isSelectedOperationChanged)
	{
		System.out.println("Executing method "+Thread.currentThread().getStackTrace()[1]);
	  String cloudOpName = UIFactory.getStringValue(map, "cloudOperation");
	  System.out.println("cloudOpName = "+cloudOpName);
	  CloudOperationNode selectedOperationNode = browser.getOperation(cloudOpName);
	  System.out.println("selectedOperationNode = "+selectedOperationNode);
	  String prevOpName = "";
	  if (opMapping != null) {
	    prevOpName = opMapping.getTargetOperation().getName();
	  }
	  props.put("targetOperation", cloudOpName);
	  String wsdlOpName = UIFactory.getStringValue(map, "cloudWsdlOperation");
	  System.out.println("wsdlOpName = "+wsdlOpName);
	  if (wsdlOpName == null) {
	    wsdlOpName = cloudOpName;
	  }
	  if (opMapping == null) {
	    opMapping = new OperationMapping(selectedOperationNode, ObjectGrouping.ORDERED, wsdlOpName);
	    modelBuilder.addOperationMapping(opMapping);
	  } else {
	    opMapping.setTargetOperation(selectedOperationNode);
	    opMapping.setNewOperationName(wsdlOpName);
	  }
	  
	  String cloudApi = UIFactory.getStringValue(map, "cloudApi");
	  System.out.println("cloudApi = "+cloudApi);
	  List reqDataObjects = null;
	  List<TypeMapping> reqTypeMapping = null;
	  List resDataObjects = new ArrayList();
	  List<TypeMapping> resTypeMapping = null;
	  AbstractCloudConnection connection = (AbstractCloudConnection)adapterPluginContext.getContextObject("CA_UI_connection");
	  
	  if (!"ZOQL".equals(cloudApi)) {
	    List dataObjects = browser.getDataObjectNodes(selectedOperationNode);
	    

	    List selectedDataObjList = null;
	    if (map.containsKey("cloudBizObj")) {
	      selectedDataObjList = UIFactory.getStringValues(map, "cloudBizObj");
	    }
	    
	    if ((selectedDataObjList != null) && (!selectedDataObjList.isEmpty())) {
	      reqDataObjects = CloudAdapterUtils.getDataObjectNodeList(dataObjects, (String[])selectedDataObjList.get(0));
	    }
	    else {
	      reqDataObjects = new ArrayList();
	      reqDataObjects.addAll(dataObjects);
	    }
	    if (reqDataObjects != null)
	      reqTypeMapping = CloudAdapterUtils.getRequestTypeMappings(map, adapterPluginContext, browser);
	  } else {
	    reqTypeMapping = new ArrayList();
	    QuerySupport querySupport = (QuerySupport)adapterPluginContext.getContextObject("UIQuerySupport");
	    if (querySupport == null) {
	      AbstractCloudApplicationAdapter cloudApplicationAdapter = (AbstractCloudApplicationAdapter)adapterPluginContext.getContextObject("applicationAdapter");
	      querySupport = cloudApplicationAdapter.getQuerySupport(connection);
	    }
	    CloudQueryParameters parameters = querySupport.getQueryParameters((String)adapterPluginContext.getContextObject("zoql.queryString"));
	    if (parameters != null) {
	      TypeMapping queryMapping = new TypeMapping(parameters, false, false);
	      reqTypeMapping.add(queryMapping);
	    }
	  }
	  if(reqTypeMapping != null) {
		  System.out.println("reqTypeMapping = "+reqTypeMapping.get(0));
	  }
	  opMapping.setRequestObjectMappings(reqTypeMapping);
	  try
	  {
	    Set<Field> fields = selectedOperationNode.getResponse().getResponseObject().getFields();
	    for (Field field : fields) {
	      resDataObjects.add(field.getFieldType());
	    }
	    resTypeMapping = CloudAdapterUtils.getRequestTypeMappings(map, adapterPluginContext, browser);
	  }
	  catch (CloudApplicationAdapterException e) {}
	  opMapping.setResponseObjectMapping(resTypeMapping);
	  if ((props != null) && (!props.isEmpty())) {
	    if ((opMapping != null) && (cloudOpName.equalsIgnoreCase(prevOpName))) {
	      props = mergeProperties(props, opMapping.getOperationProperties());
	    }
	    opMapping.setOperationProperties(props);
	  }
	  else if (!cloudOpName.equalsIgnoreCase(prevOpName)) {
	    opMapping.setOperationProperties(props);
	  }
	  if (isSelectedOperationChanged) {
	    List<Header> headers = opMapping.getTargetOperation().getRequestHeaders();
	    for (Header header : headers) {
	      if (header.getName().equalsIgnoreCase("AllOrNoneHeader"))
	      {
	        String allOrNoneHeaderProperty = "AllOrNoneHeader.allOrNone";
	        opMapping.setOperationProperty(allOrNoneHeaderProperty, "true");
	        break;
	      }
	    }
	  }
	  
	  List<CloudAPINode> apis = browser.getAPIs();
	  CloudAPINode cloudAPINode = CloudAdapterUtils.getCloudAPINodeFromList(apis, cloudApi);
	  Properties connProps = connection.getConnectionProperties();
	  if ((connProps != null) && (cloudAPINode != null)) {
	    connProps.setProperty("applicationVersion", cloudAPINode.getVersion());
	  }
	}

	private CloudMetadataBrowser getBrowserFromContext() {
	  if (this.adapterPluginContext.getContextObject("UIMetadataBrowser") == null) {
	    CloudAdapterUtils.getMetadataBrowser(this.adapterPluginContext);
	  }
	  return (CloudMetadataBrowser)this.adapterPluginContext.getContextObject("UIMetadataBrowser");
	}
	
	public static Properties mergeProperties(Properties newProps, Properties oldProps)
	{
		System.out.println("Executing method "+Thread.currentThread().getStackTrace()[1]);
	  if (oldProps == null) {
	    return newProps;
	  }
	  Enumeration e = newProps.propertyNames();
	  
	  while (e.hasMoreElements()) {
	    String key = (String)e.nextElement();
	    if (oldProps.containsKey(key)) {
	      oldProps.remove(key);
	      oldProps.put(key, newProps.getProperty(key));
	    } else {
	      oldProps.put(key, newProps.getProperty(key));
	    }
	  }
	  return oldProps;
	}
	
	/**
	 * get updated Edit Pages
	 */
	public CloudAdapterPageState getUpdatedEditPages(LinkedHashMap<String, ICloudAdapterPage> wizardPages, LinkedList<EditField> currentPageFields, String fieldName)
	{
	  System.out.println("Executing method "+Thread.currentThread().getStackTrace()[1]);
	  CloudAdapterPageState pageState = new CloudAdapterPageState(false, wizardPages, currentPageFields);
	  Map<String, EditField> map = EditField.getFieldMap((EditField[])currentPageFields.toArray(new EditField[currentPageFields.size()]));
	  Map<String, UIObject> objectMap = EditField.getObjectMap((EditField[])currentPageFields.toArray(new EditField[currentPageFields.size()]));
	  String newValue = UIFactory.getStringValue(objectMap, fieldName);
	  String newQuery = UIFactory.getStringValue(objectMap, "queryEditor");
	  if (fieldName.equals("processingOptions")) {
	    System.out.println("processingOptions");
	  }
	  else if (fieldName.equals("cloudApi")) {
	    handleCloudAPIChange(currentPageFields, newValue, locale, null, null);
	  } 
	  else {
	    String cloudApi;
	    if (fieldName.equals("cloudOperation"))
	    {
	      populateBusinessObjects(currentPageFields, getBrowserFromContext().getOperation(newValue), null, null);
	      if (("query".equals(newValue)) || ("queryMore".equals(newValue)))
	      {
	        populateQueryField(currentPageFields, locale, getBrowserFromContext().getOperation(newValue), null);
	      }
	      cloudApi = ((ISelectObject)((EditField)map.get("cloudApi")).getObject()).getSelectedValue();
	    }
	    else if (fieldName.equals("testQuery")) {
	      String query = ((TextAreaObject)((EditField)map.get("queryEditor")).getObject()).getValue();
	      String curOp = UIFactory.getStringValue(EditField.getObjectMap((EditField[])currentPageFields.toArray(new EditField[currentPageFields.size()])), "cloudOperation");
	      this.adapterPluginContext.setContextObject("cloudOperation", curOp);
	      EditField resultCountField = (EditField)map.get("resObjectCount");
	      if (resultCountField != null) {
	        currentPageFields.remove(resultCountField);
	      }
	      Map<String, String> bindVariables = new HashMap<String, String>();
	      List<String> paramsList = CloudQueryValidator.getQueryParams(this.adapterPluginContext, query);
	      LinkedList<EditField> paramFields = ((IGroupObject)((EditField)map.get("params")).getObject()).getEditFields();
	      Map<String, EditField> paramMap = EditField.getFieldMap((EditField[])paramFields.toArray(new EditField[paramFields.size()]));
	      for (String param : paramsList) {
	        if (paramMap.get(param) != null) {
	          String paramVal = ((ITextBoxObject)((EditField)paramMap.get(param)).getObject()).getValue();
	          bindVariables.put(param, paramVal);
	        }
	      }
	      CloudOperationNode opNode = getBrowserFromContext().getOperation((String)this.adapterPluginContext.getContextObject("cloudOperation"));
	      TransformationModelBuilder modelBuilder = (TransformationModelBuilder)this.adapterPluginContext.getContextObject("_ui_ModelBuilder");
	      TransformationModel model = modelBuilder.build();
	      String targetNameSpace = model.getTargetNamespace();
	      List<CloudValidationError> errorsFromBackendSDK = null;
	      this.adapterPluginContext.setContextObject("zoql.queryString", query.replaceAll("\\n|\\r", " "));
	      try
	      {
	        errorsFromBackendSDK = AbstractCloudQueryParser.validateQuery(query, this.adapterPluginContext, targetNameSpace, true, true);
	      }
	      catch (CloudApplicationAdapterException e) {}
	      if (errorsFromBackendSDK != null) {
	        for (int i = 0; i < errorsFromBackendSDK.size(); i++) {
	          CloudValidationError tmpError = (CloudValidationError)errorsFromBackendSDK.get(i);
	          String msgKey = tmpError.getErrorCode();
	          Object[] obj = new Object[tmpError.getKeywordsList().size()];
	          for (int j = 0; j < tmpError.getKeywordsList().size(); j++) {
	            ValidationKeyword errKeyword = (ValidationKeyword)tmpError.getKeywordsList().get(j);
	            obj[j] = errKeyword.getKeyword();
	          }
	          String err = "CloudError"; 
	          ((ITextAreaObject)((EditField)map.get("queryResults")).getObject()).setValue(err);
	        }
	      }
	      else {
	        QueryValidationResult result = CloudQueryValidator.executeQuery(this.adapterPluginContext, query, bindVariables, opNode);
	        if (result.isSuccess()) {
	          this.isSuccess = true;
	          int numOfRecords = result.getTotalRecordsReturned();
	          if (numOfRecords == 0) {
	            ((ITextAreaObject)((EditField)map.get("queryResults")).getObject()).setValue("No records found!");
	          }
	          else {
	            ((ITextAreaObject)((EditField)map.get("queryResults")).getObject()).setValue(CloudAdapterUtils.prettyFormat(result.getQueryResult()));
	          }
	          TextBoxObject resCount = UIFactory.createTextBox(String.valueOf(numOfRecords), true);
	          currentPageFields.add(UIFactory.createEditField("resObjectCount", 
	        		  "Results Found", 
	        		  "Results Found", 
	        		  false, 
	        		  resCount));
	        }
	        else
	        {
	          this.isSuccess = false;
	          ((ITextAreaObject)((EditField)map.get("queryResults")).getObject()).setValue(result.getErrorDetail().getErrorMessage());
	        }
	      }
	    }
	    else if (fieldName.endsWith("groupFilter")) {
	      String editFieldName = CloudAdapterUtils.getGroupFilterFieldName(fieldName);
	      String groupFilterValue = CloudAdapterUtils.getGroupFilterValue(fieldName);
	      if ("cloudBizObj".equals(editFieldName)) {
	        String cloudOp = UIFactory.getStringValue(objectMap, "cloudOperation");
	        populateBusinessObjects(currentPageFields, getBrowserFromContext().getOperation(cloudOp), null, groupFilterValue);
	      }
	    }
	    else if (fieldName.equals("queryEditor")) {
	      this.isSuccess = true;
	      refreshBindingParams(newValue, currentPageFields);
	    }
	    else if (fieldName.equals("Refresh"))
	    {
	      if (newQuery != null)
	        refreshBindingParams(newQuery, currentPageFields);
	    } }
	  String opMode = UIFactory.getStringValue(EditField.getObjectMap((EditField[])currentPageFields.toArray(new EditField[currentPageFields.size()])), "operationMode");
	  return pageState;
	}

	private OperationMapping getSelectedOperation() {
		  System.out.println("Executing method "+Thread.currentThread().getStackTrace()[1]);
		OperationMapping opMap = null;
		TransformationModelBuilder transformationModelBuilder = (TransformationModelBuilder) this.adapterPluginContext.getContextObject("_ui_ModelBuilder");
		TransformationModel model = transformationModelBuilder.build();
		if (isValueSetForPopulation()) {
			Integer selectedIndex = (Integer) this.adapterPluginContext.getContextObject("operationsTable_ui_selectedRowIndex");
			opMap = (OperationMapping) model.getOperationMappings().get(selectedIndex.intValue());
		} else if (model.getOperationMappings().size() > 0) {
			System.out.println("model.getOperationMappings() = "+model.getOperationMappings());
			opMap = (OperationMapping) model.getOperationMappings().get(0);
		}
		System.out.println("Operation Map = "+opMap);
		return opMap;
	}

	private boolean isValueSetForPopulation() {
		  System.out.println("Executing method "+Thread.currentThread().getStackTrace()[1]);
		Integer selectedIndex = (Integer) this.adapterPluginContext.getContextObject("operationsTable_ui_selectedRowIndex");
		System.out.println("selectedIndex = "+selectedIndex);
		return selectedIndex != null;
	}

	private CloudOperationNode getDefaultSelectedOperation(List<CloudOperationNode> operationNodes) {
		System.out.println("Executing method "+Thread.currentThread().getStackTrace()[1]);
		String[] operationNames = getSortedOperation(operationNodes);
		CloudOperationNode selectedOpNode = null;
		for (CloudOperationNode opnode : operationNodes) {
			if (operationNames[0].equals(opnode.getName())) {
				selectedOpNode = opnode;
				break;
			}
		}
		return selectedOpNode;
	}

	private String[] getSortedOperation(List<CloudOperationNode> operationNodes) {
		System.out.println("Executing method "+Thread.currentThread().getStackTrace()[1]);
		String[] operationNames = new String[operationNodes.size()];
		for (int i = 0; i < operationNodes.size(); i++) {
			operationNames[i] = ((CloudOperationNode) operationNodes.get(i)).getName();
		}
		
		Arrays.sort(operationNames);
		return operationNames;
	}

	/**
	 * create selected items
	 * @param dataObjects
	 * @param groupFilterValue
	 * @return
	 */
	private List<ISelectItem> createSelectItems(List<CloudDataObjectNode> dataObjects, String groupFilterValue) {
		  System.out.println("Executing method "+Thread.currentThread().getStackTrace()[1]);
		List<ISelectItem> selectItems = new ArrayList<ISelectItem>(dataObjects.size());

		for (CloudDataObjectNode dataObject : dataObjects) {
			String objCategory = null;
			if (dataObject.getObjectCategory() != null)
				objCategory = dataObject.getObjectCategory().name();
			if ((groupFilterValue == null) || ("ALL".equals(groupFilterValue)) || (groupFilterValue.equals(objCategory))) {
				selectItems.add(UIFactory.createSelectItem(dataObject.getName(), dataObject.getDescription()));
			}
		}
		Collections.sort(selectItems, new Comparator<ISelectItem>() {
			public int compare(ISelectItem o1, ISelectItem o2) {
				return o1.getValue().compareTo(o2.getValue());
			}
		});
		return selectItems;
	}

	private void createOrReplaceField(LinkedList<EditField> editFields, String fieldName, String fieldLabel, String desc, boolean required, UIObject uiObj, String fieldToBeReplaced, EditField.LabelFieldLayout layout, int rowIndex) {
		  System.out.println("Executing method "+Thread.currentThread().getStackTrace()[1]);
		Map<String, EditField> fieldsMap = EditField.getFieldMap((EditField[]) editFields.toArray(new EditField[editFields.size()]));
		EditField tobeReplacedField = (EditField) fieldsMap.get(fieldToBeReplaced);
		int index = -1;
		if (tobeReplacedField != null) {
			index = editFields.indexOf(tobeReplacedField);
		}
		if (index > -1) {
			editFields.remove(index);
			EditField newField = UIFactory.createEditField(fieldName, fieldLabel, desc, required, false, uiObj, layout, null, rowIndex, EditField.LabelFieldAlignment.LEFT_LEFT);
			editFields.add(index, newField);
		} else {
			CloudUtil.createOrUpdateField(editFields, fieldName, fieldLabel, desc, required, uiObj, layout, null, -1, EditField.LabelFieldAlignment.LEFT_LEFT);
		}
	}

	private List<TypeMapping> getResponseTypeMappings(CloudOperationNode selectedOperation) throws CloudApplicationAdapterException {
		  System.out.println("Executing method "+Thread.currentThread().getStackTrace()[1]);
		List<TypeMapping> typeMappings = new ArrayList();
		if ((selectedOperation.getResponse() != null) && (selectedOperation.getResponse().getResponseObject() != null)) {
			Set<Field> fields = selectedOperation.getResponse().getResponseObject().getFields();
			Iterator<Field> it = fields.iterator();
			while (it.hasNext()) {
				Field descNode = (Field) it.next();
				CloudDataObjectNode t = descNode.getFieldType();
				TypeMapping tmap = new TypeMapping(descNode.getName(), t, true,true);
				System.out.println("Response parameter -->"+ descNode.getName());
				typeMappings.add(tmap);
			}
		}

		return typeMappings;
	}

	private List<TypeMapping> getRequestTypeMappings(CloudOperationNode selectedOperation) throws CloudApplicationAdapterException {
		  System.out.println("Executing method "+Thread.currentThread().getStackTrace()[1]);
		List<TypeMapping> typeMappings = new ArrayList();
		Iterator<RequestParameter> reqIter = selectedOperation.getRequestParameters().iterator();
		while (reqIter.hasNext()) {
			RequestParameter param = (RequestParameter) reqIter.next();
			Set<Field> fields = param.getDataType().getFields();
			Iterator<Field> fieldIter = fields.iterator();
			while (fieldIter.hasNext()) {
				Field descNode = (Field) fieldIter.next();
				CloudDataObjectNode t = descNode.getFieldType();
				TypeMapping tmap = new TypeMapping(descNode.getName(), t, true, true);
				System.out.println("Request parameter -->" + descNode.getName());
				typeMappings.add(tmap);
			}
		}
		return typeMappings;
	}
}