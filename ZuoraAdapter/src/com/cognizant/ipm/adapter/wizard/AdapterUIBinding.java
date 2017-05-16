package com.cognizant.ipm.adapter.wizard;

import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import oracle.tip.tools.adapters.cloud.api.ByteArrayResourceCreationStrategy;
import oracle.tip.tools.adapters.cloud.api.CloudAdapterArtifact;
import oracle.tip.tools.adapters.cloud.api.CloudAdapterArtifacts;
import oracle.tip.tools.adapters.cloud.api.CloudAdapterCallerContext;
import oracle.tip.tools.adapters.cloud.api.CloudAdapterException;
import oracle.tip.tools.adapters.cloud.api.CloudAdapterFilter;
import oracle.tip.tools.adapters.cloud.api.ICloudAdapterPage;
import oracle.tip.tools.adapters.cloud.impl.AbstractCloudAdapterUIBinding;
import oracle.tip.tools.adapters.cloud.impl.CloudAdapterConnectionPage;
import oracle.tip.tools.adapters.cloud.utils.CloudAdapterUtils;
import oracle.tip.tools.ide.adapters.cloud.api.connection.AbstractCloudConnection;
import oracle.tip.tools.ide.adapters.cloud.api.generation.RuntimeMetadata;
import oracle.tip.tools.ide.adapters.cloud.api.generation.RuntimeMetadataGenerator;
import oracle.tip.tools.ide.adapters.cloud.api.model.TransformationModel;
import oracle.tip.tools.ide.adapters.cloud.api.plugin.AdapterPluginContext;
import oracle.tip.tools.ide.adapters.cloud.api.plugin.CloudApplicationAdapterException;
import oracle.tip.tools.ide.adapters.cloud.impl.metadata.model.TransformationModelBuilder;
import oracle.tip.tools.ide.adapters.cloud.impl.plugin.AbstractCloudApplicationAdapter;
import oracle.tip.tools.presentation.uiobjects.sdk.EditField;

import com.cognizant.ipm.adapter.plugin.CloudAdapter;

public class AdapterUIBinding extends AbstractCloudAdapterUIBinding
{
  public AdapterUIBinding(CloudAdapterFilter cloudAdapterFilter, Locale locale) throws CloudAdapterException
  {
	  super(cloudAdapterFilter, locale, "ZuoraCRM Adapter");
	  System.out.println("Executing method "+Thread.currentThread().getStackTrace()[1]);
	  try {
		  if (cloudAdapterFilter.getApplicationInstanceId() == null) {
			  AbstractCloudApplicationAdapter cloudApplicationAdapter = new CloudAdapter(this.context);
			  this.context.setContextObject("applicationAdapter", cloudApplicationAdapter);
			  super.useApplicationAdapterToInitialise(cloudApplicationAdapter);
		  }
		  if (cloudAdapterFilter.isInbound()) {
			  Map modelBuilderMap = (Map)this.context.getContextObject("UI_MODEL_BUILDER_MAP");

			  if (modelBuilderMap == null) {
				  modelBuilderMap = new HashMap();
				  this.context.setContextObject("UI_MODEL_BUILDER_MAP", modelBuilderMap);
			  }
        
			  TransformationModelBuilder tmb = null;
			  for (ARTIFACT_ID artifactId : ARTIFACT_ID.values()) {
				  tmb = getTransformationModelBuilder(cloudAdapterFilter, artifactId.name());
          
				  if (tmb != null) {
					  modelBuilderMap.put(artifactId.name(), tmb);
				  }
			  }
		  }
	  } catch (Exception e) {
		  e.printStackTrace();
		  throw new CloudAdapterException(e.getMessage());
	  }
    this.context.setContextObject("IS_JDEV", "false");
  }
  
  /**
   * transformation builder
   * 
   * @param cloudAdapterFilter
   * @param artifactId
   * @return
   * @throws IOException
   * @throws CloudApplicationAdapterException
   * @throws IOException
   * @throws CloudApplicationAdapterException
   */
  private TransformationModelBuilder getTransformationModelBuilder(CloudAdapterFilter cloudAdapterFilter, String artifactId)
    throws IOException, CloudApplicationAdapterException, IOException, CloudApplicationAdapterException
  {
	  System.out.println("Executing method "+Thread.currentThread().getStackTrace()[1]);
    CloudAdapterArtifacts artifacts = cloudAdapterFilter.getCloudAdapterArtifacts();
    
    TransformationModelBuilder transformationModel = null;
    if (artifacts != null) {
      String jcaKey = artifactId + "_" + "JCA";
      byte[] jcaBytes = null;
      if ((artifacts.getGeneratedArtifacts() != null) && (artifacts.getGeneratedArtifacts().get("JCA") != null))
      {
        List<CloudAdapterArtifact> oList = (List)artifacts.getGeneratedArtifacts().get("JCA");
        
        if ((oList != null) && (!oList.isEmpty())) {
          for (CloudAdapterArtifact caa : oList) {
            if (jcaKey.equals(caa.getKeyName())) {
              jcaBytes = caa.getBytes();
              break;
            }
          }
        }
      }
      
      String wsdlKey = artifactId + "_" + "WSDL";
      byte[] wsdlBytes = null;
      if ((artifacts.getGeneratedArtifacts() != null) && (artifacts.getGeneratedArtifacts().get("WSDL") != null))
      {
        List<CloudAdapterArtifact> oList = artifacts.getGeneratedArtifacts().get("WSDL");
        
        if ((oList != null) && (!oList.isEmpty())) {
          for (CloudAdapterArtifact caa : oList) {
            if (wsdlKey.equals(caa.getKeyName())) {
              wsdlBytes = caa.getBytes();
              break;
            }
          }
        }
      }
      if ((jcaBytes != null) && (wsdlBytes != null)) {
    	  transformationModel = new TransformationModelBuilder(this.context);
    	  transformationModel.rebuild(wsdlBytes, jcaBytes);
    	  this.context.setContextObject("UIReEntrantMode", Boolean.valueOf(true));
      }
      
      this.context.setContextObject("cloudAdapterDescriptionArtifact", artifacts.getDescription());
    }
    
    return transformationModel;
  }
  
  public static enum ARTIFACT_ID {
    REQUEST,  CALLBACK_SUC,  CALLBACK_FAIL;
    private ARTIFACT_ID() {}
  }
  
  public LinkedHashMap<String, ICloudAdapterPage> getEditPages(Object object)
  {
	  System.out.println("Executing method "+Thread.currentThread().getStackTrace()[1]);
    LinkedHashMap<String, ICloudAdapterPage> editPages = new LinkedHashMap();
    String referenceName = this.context.getReferenceBindingName();
    CloudAdapterFilter cloudAdapterFilter = (CloudAdapterFilter)this.context.getContextObject("UICloudAdapterFilter");
    
    CloudAdapterCallerContext cloudAdapterCallerContext = cloudAdapterFilter.getCloudAdapterCallerContext();
    if ((cloudAdapterCallerContext == null) || (CloudAdapterCallerContext.PRODUCT.ICS_WEB.equals(cloudAdapterCallerContext.getProduct())))
    {
    	this.context.setContextObject("generatePartnerLink", "false");
    }
    
    editPages.put("welcome", new AdapterWelcomePage(this.context));
    if (cloudAdapterFilter.getApplicationInstanceId() == null) {
      editPages.put("connection", new CloudAdapterConnectionPage(this.context));
    }
    if (!cloudAdapterFilter.isInbound())
    {
        String pageIdToRemove = getPageIdToRemoveInEditMode();
        if (!"operations".equals(pageIdToRemove))
        {
        	editPages.put("operations", new AdapterOperationsPage(this.context));
        }
    }
    else {
    }
    
    editPages.put("summary", new AdapterSummaryPage(this.context));
    
    return editPages;
  }
    
    private String getPageIdToRemoveInEditMode()
    {
      return "";
    }
  
  public CloudAdapterArtifacts generateMetadataArtifacts(LinkedHashMap<String, LinkedList<EditField>> wizardUpdatedEditFields)
    throws CloudAdapterException
  {
	  System.out.println("Executing method "+Thread.currentThread().getStackTrace()[1]);
    CloudAdapterFilter cloudAdapterFilter = (CloudAdapterFilter)this.context.getContextObject("UICloudAdapterFilter");
    RuntimeMetadataGenerator metadataGen = (RuntimeMetadataGenerator)this.context.getContextObject("UIRunTimeMetadataGenerator");
    CloudAdapterCallerContext cloudAdapterCallerContext = cloudAdapterFilter.getCloudAdapterCallerContext();
    if ((cloudAdapterCallerContext == null) || (CloudAdapterCallerContext.PRODUCT.ICS_WEB.equals(cloudAdapterCallerContext.getProduct())))
    {
      metadataGen.setGeneratorOption("generatePartnerLink", "false");
    }
    
    if (((cloudAdapterFilter.isInbound()) && (cloudAdapterCallerContext != null) && (!CloudAdapterCallerContext.PRODUCT.ICS_WEB.equals(cloudAdapterCallerContext.getProduct()))) || (CloudAdapterCallerContext.PRODUCT.OSB_WEB.equals(cloudAdapterCallerContext.getProduct())))
    {
      metadataGen.setGeneratorOption("generatePartnerLink", "false");
    }
    
    if (!cloudAdapterFilter.isInbound()) {
      CloudAdapterArtifacts outboundArtifacts = super.generateMetadataArtifacts(wizardUpdatedEditFields);
      
      CloudAdapterUtils.cleanGenerationContext(this.context);
      return outboundArtifacts;
    }
    
    CloudAdapterArtifacts artifacts = null;
    
    try
    {
      AbstractCloudConnection connection = (AbstractCloudConnection)this.context.getContextObject("CA_UI_connection");
      metadataGen.setConnection(connection);
      ByteArrayResourceCreationStrategy jcaLoc = new ByteArrayResourceCreationStrategy("JCA");
      ByteArrayResourceCreationStrategy wsdlLoc = new ByteArrayResourceCreationStrategy("WSDL");
      metadataGen.setJCAResourceOutput(jcaLoc);
      metadataGen.setWSDLResourceOutput(wsdlLoc);
      Map<String, TransformationModelBuilder> builders = (Map)this.context.getContextObject("UI_MODEL_BUILDER_MAP");
      TransformationModelBuilder modelBuilder = (TransformationModelBuilder)this.context.getContextObject("_ui_ModelBuilder");
      builders.put("REQUEST", modelBuilder);
      String referenceName = this.context.getReferenceBindingName();
      Map selectedOpsMap = new HashMap();
      
      for (String key : builders.keySet())
      {
        this.context.setContextObject("INBOUND_MODEL_UNDER_PROCESS", key);
        jcaLoc.setKeyName(key);
        wsdlLoc.setKeyName(key);
        this.context.setReferenceBindingName(getFormattedInternalRefName(referenceName, key, jcaLoc.getResourceType()));
        
        TransformationModel tm = ((TransformationModelBuilder)builders.get(key)).build();
        
        metadataGen.setTransformationModel(tm);
        RuntimeMetadata runtimeMetaData = metadataGen.generate();
        List selectedOps = CloudAdapterUtils.getSelectedWSDLOperations(tm);
        
        selectedOpsMap.put(key, selectedOps);
      }
      this.context.setReferenceBindingName(referenceName);
      artifacts = new CloudAdapterArtifacts(jcaLoc.getArtifacts(), wsdlLoc.getArtifacts(), referenceName);
      
      artifacts.setJcaByteArrayMap(jcaLoc.getByteArrayMap());
      artifacts.setWsdlByteArrayMap(wsdlLoc.getByteArrayMap());
      artifacts.setWsdlOperationsMap(selectedOpsMap);
      artifacts.setReferenceName(this.context.getReferenceBindingName());
      artifacts.setDescription((String)this.context.getContextObject("cloudAdapterDescriptionArtifact"));
      
      if (this.context.getContextObject("caCsfKeyPropertiesMap") != null) {
        artifacts.setCsfKeys((LinkedHashMap)this.context.getContextObject("caCsfKeyPropertiesMap"));
        artifacts.setCsfKeyStoreName((String)this.context.getContextObject("caCsfKeyStore"));
      }
    }
    catch (Exception ex)
    {
      throw new CloudAdapterException(ex.getMessage(), ex);
    }
    CloudAdapterUtils.cleanGenerationContext(this.context);
    return artifacts;
  }
  
  public AdapterPluginContext getAdapterConfiguration()
  {
	  System.out.println("Executing method "+Thread.currentThread().getStackTrace()[1]);
    return this.context;
  }
}