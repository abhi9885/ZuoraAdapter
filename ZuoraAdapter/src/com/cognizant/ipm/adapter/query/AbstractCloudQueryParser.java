package com.cognizant.ipm.adapter.query;

import java.util.ArrayList;
import java.util.List;

import oracle.tip.tools.adapters.cloud.api.CloudAdapterCallerContext;
import oracle.tip.tools.adapters.cloud.api.CloudAdapterFilter;
import oracle.tip.tools.ide.adapters.cloud.api.model.CloudDataObjectNode;
import oracle.tip.tools.ide.adapters.cloud.api.plugin.AdapterPluginContext;
import oracle.tip.tools.ide.adapters.cloud.api.plugin.CloudApplicationAdapterException;

import com.cognizant.ipm.adapter.util.CloudUtil;
import com.cognizant.ipm.adapter.util.CloudValidationError;

public abstract class AbstractCloudQueryParser implements CloudQueryParser
{
  protected static AdapterPluginContext adapterPluginContext = null;
  protected String serviceTargetNameSpace = null;
  protected CloudQueryModel queryModel = null;
  protected List<CloudDataObjectNode> sObjectDescendents = null;
  protected String curNS = "";
  protected String queryDefaultCase = "";
  protected String queryLowerCase = "";
  protected List<CloudValidationError> errorList = null;

  public AbstractCloudQueryParser(AdapterPluginContext p_adapterPluginContext, String p_serviceTargetNameSpace)
  {
	  System.out.println("Executing method "+Thread.currentThread().getStackTrace()[1]);
    this.serviceTargetNameSpace = p_serviceTargetNameSpace;
    CloudAdapterFilter cloudAdapterFilter = (CloudAdapterFilter)p_adapterPluginContext.getContextObject("UICloudAdapterFilter");
    CloudAdapterCallerContext cloudAdapterCallerContext = cloudAdapterFilter.getCloudAdapterCallerContext();
    if ((cloudAdapterCallerContext == null) || (CloudAdapterCallerContext.PRODUCT.ICS_WEB.equals(cloudAdapterCallerContext.getProduct())))
    {
      this.serviceTargetNameSpace = (p_serviceTargetNameSpace + "_REQUEST");
    }

    adapterPluginContext = p_adapterPluginContext;
    this.queryModel = new CloudQueryModel();
    this.curNS = "http://api.zuora.com";
  }

  public boolean isReservedKeyword(String word)
  {
	  System.out.println("Executing method "+Thread.currentThread().getStackTrace()[1]);
    if ((word.equalsIgnoreCase(" where".trim())) || (word.equalsIgnoreCase("limit".trim())) || (word.equalsIgnoreCase("with".trim())) || (word.equalsIgnoreCase(" order ".trim())))
    {
      return true;
    }
    return false;
  }
  
  public CloudQueryModel parse(String queryString, boolean isRaiseErr) throws CloudApplicationAdapterException
  {
	  System.out.println("Executing method "+Thread.currentThread().getStackTrace()[1]);
    queryString = removeExtraFormatting(queryString);
    this.queryDefaultCase = queryString;
    queryString = queryString.toLowerCase().trim();
    this.queryLowerCase = queryString;
    this.queryModel = buildQueryModel(queryString);
    return this.queryModel;
  }
  
  protected List<CloudDataObjectNode> getSObjectDescendents() {
	  System.out.println("Executing method "+Thread.currentThread().getStackTrace()[1]);
    if ((this.sObjectDescendents == null) || (this.sObjectDescendents.size() < 1)) {
      CloudDataObjectNode cdonode = CloudUtil.getBusinessObjectByName(adapterPluginContext, "zObject");
      this.sObjectDescendents = cdonode.getDescendants();
    }
    return this.sObjectDescendents;
  }
  

  public static CloudQueryModel getQueryModel(String queryStatement, String p_targetNameSpace, AdapterPluginContext p_adapterPluginContext)
    throws CloudApplicationAdapterException
  {
	  System.out.println("Executing method "+Thread.currentThread().getStackTrace()[1]);
    CloudQueryModel queryModel = null;
    CloudQueryParser cloudQueryParser = new CloudQueryParserImpl(p_adapterPluginContext, p_targetNameSpace);
    
    if (cloudQueryParser != null) {
      queryModel = cloudQueryParser.parse(queryStatement, true);
      if ((null == queryModel.getErrorList()) || ((null != queryModel.getErrorList()) && (queryModel.getErrorList().size() < 1)))
      {
        adapterPluginContext.setContextObject("SELECTED_OBJECT_TYPE_MAPPINGS_LIST", cloudQueryParser.getResponseTypeMappings());
      }
    }

    return queryModel;
  }

  /**
   * validates the query after parsing it
   * @param queryStatement
   * @param p_adapterPluginContext
   * @param p_targetNameSpace
   * @param canConnectTozuora
   * @param isGenerateResponseStructure
   * @return
   * @throws CloudApplicationAdapterException
   */
  public static List<CloudValidationError> validateQuery(String queryStatement, AdapterPluginContext p_adapterPluginContext, String p_targetNameSpace, boolean canConnectTozuora, boolean isGenerateResponseStructure)
    throws CloudApplicationAdapterException
  {
	  System.out.println("Executing method "+Thread.currentThread().getStackTrace()[1]);
    CloudQueryModel queryModel = null;
    CloudQueryParser cloudQueryParser = null;
    cloudQueryParser = new CloudQueryParserImpl(p_adapterPluginContext, p_targetNameSpace);
    
    if (cloudQueryParser != null) {
      queryModel = cloudQueryParser.parse(queryStatement, false);
      if ((isGenerateResponseStructure) && ((null == queryModel.getErrorList()) || (queryModel.getErrorList().size() < 1)))
      {
        adapterPluginContext.setContextObject("SELECTED_OBJECT_TYPE_MAPPINGS_LIST", cloudQueryParser.getResponseTypeMappings());
      }
      // this need to be taken care
      adapterPluginContext.setContextObject("SEL_OBJECTS_FOR_FINISH_PAGE", queryModel.getSelectedModelObjectsList().toString());
      return queryModel.getErrorList();
    }
    
    return new ArrayList<CloudValidationError>();
  }
}