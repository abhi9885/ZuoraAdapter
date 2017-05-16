package com.cognizant.ipm.adapter.plugin.generator;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import javax.xml.namespace.QName;

import oracle.tip.tools.ide.adapters.cloud.api.metadata.OperationMapping;
import oracle.tip.tools.ide.adapters.cloud.api.metadata.TypeMapping;
import oracle.tip.tools.ide.adapters.cloud.api.metadata.TypeMask;
import oracle.tip.tools.ide.adapters.cloud.api.model.CloudDataObjectNode;
import oracle.tip.tools.ide.adapters.cloud.api.model.CloudOperationNode;
import oracle.tip.tools.ide.adapters.cloud.api.model.DataType;
import oracle.tip.tools.ide.adapters.cloud.api.model.Field;
import oracle.tip.tools.ide.adapters.cloud.api.model.ObjectCategory;
import oracle.tip.tools.ide.adapters.cloud.api.model.RequestParameter;
import oracle.tip.tools.ide.adapters.cloud.api.model.TransformationModel;
import oracle.tip.tools.ide.adapters.cloud.api.plugin.AdapterPluginContext;
import oracle.tip.tools.ide.adapters.cloud.api.plugin.CloudApplicationAdapterException;
import oracle.tip.tools.ide.adapters.cloud.impl.metadata.model.CloudDataObjectNodeImpl;
import oracle.tip.tools.ide.adapters.cloud.impl.metadata.model.FieldImpl;

import org.apache.commons.lang3.StringUtils;

import com.cognizant.ipm.adapter.util.CloudUtil;

public class CloudWsdlRmgHelper extends AbstractRmgHelper
{
  public CloudWsdlRmgHelper(AdapterPluginContext p_context, TransformationModel p_model)
  {
    super(p_context, p_model);
    this.currentFlow = API_FLOWS.ENTERPRISE_WSDL.toString();
  }
  
  public void setJCAProps()
  {
    super.setJCAProps();
  }
  
  protected void processResponseMappings()
  {
    List<TypeMapping> tmList = new ArrayList<TypeMapping>();
    CloudDataObjectNode responseNode = ((CloudOperationNode)this.applicationModel.findOperations(this.targetOperation.getName()).get(0)).getResponse().getResponseObject();

    Set<Field> responseFields = null;
    try {
      responseFields = responseNode.getFields();
    } catch (CloudApplicationAdapterException e) {
      if (this.logger != null) {
        this.logger.logSevere("Exception caught while processing response for Enterprise WSDL" + e.getMessage());
      }
      e.printStackTrace();
    }
    
    if ((this.targetOperation.getName().equalsIgnoreCase("query")) || (this.targetOperation.getName().equalsIgnoreCase("queryMore")))
    {
      for (Field responseField : responseFields) {
        if (responseField.getFieldType().getName().endsWith("esult"))
        {
          tmList = (List)this.context.getContextObject("SELECTED_OBJECT_TYPE_MAPPINGS_LIST");
        }
      }
      String queryOrStatement = this.context.getContextObject("zoql.queryString").toString();
      ((OperationMapping)this.model.getOperationMappings().get(0)).getOperationProperties().put("zoql.queryString", queryOrStatement);
    }
    else
    {
      for (Field field : responseFields) {
        if (field.getFieldType().getName().endsWith("esult")) {
          tmList.addAll(((OperationMapping)this.model.getOperationMappings().get(0)).getResponseObjectMapping());
        }
        else if (field.getFieldType().getName().equalsIgnoreCase("zObject"))
        {

          if (this.targetOperation.getName().equalsIgnoreCase("retrieve")) {
            TypeMapping tmRtrv = new TypeMapping(((TypeMapping)this.selectedReqObjects.get(0)).getTargetDataObject());
            tmRtrv.setOverridingName(((TypeMapping)this.selectedReqObjects.get(0)).getTargetDataObject().getName() + StringUtils.capitalize(field.getName()));
            tmList.add(tmRtrv);
          } else {
            tmList.addAll(this.selectedReqObjects);
          }
        }
      }
    }
    
    ((OperationMapping)this.model.getOperationMappings().get(0)).setResponseObjectMapping(tmList);
  }

  protected void processRequestMappings()
  {
    List<TypeMapping> tmList = new ArrayList();
    List<TypeMapping> selectedObjects = ((OperationMapping)this.model.getOperationMappings().get(0)).getRequestObjectMappings();
    
    if (this.targetOperation.getName().equalsIgnoreCase("process")) {
      QName qname = null;
      CloudDataObjectNode processNode = null;
      String processType = null;
      
      Properties operationProperties = ((OperationMapping)this.model.getOperationMappings().get(0)).getOperationProperties();
      
      if ((operationProperties != null) && (operationProperties.size() > 0) && 
        (operationProperties.containsKey("processType")))
      {
        processType = operationProperties.getProperty("processType");
      }
      

      if (processType == null) {
        processType = (String)this.context.getContextObject("processType");
      }
      ((OperationMapping)this.model.getOperationMappings().get(0)).getOperationProperties().put("processType", processType);
      qname = new QName("http://api.zuora.com", processType);
      processNode = this.applicationModel.findDataObject(qname);
      Set<Field> fields = null;
      try {
        fields = processNode.getFields();
      } catch (CloudApplicationAdapterException e) {
        if (this.logger != null) {
          this.logger.logSevere("Exception caught while processing request for Enterprise WSDL" + e.getMessage());
        }
        e.printStackTrace();
      }
      Set<Field> includedFields = new HashSet();
      for (Field field : fields) {
        if (field.getFieldType().getName().equalsIgnoreCase("ID")) {
          Field newField = CloudUtil.getIDField(qname.getNamespaceURI());
          
          includedFields.add(newField);
        } else {
          includedFields.add(field);
        }
      }
      CloudDataObjectNodeImpl typeMapNode = new CloudDataObjectNodeImpl(processNode.getParent(), processNode.getQualifiedName(), processNode.getObjectCategory(), processNode.getDataType(), false);
      typeMapNode.addFields(includedFields);
      tmList.add(new TypeMapping(typeMapNode));
    } else {
      Set<Field> fields = null;
      try {
        fields = ((RequestParameter)((CloudOperationNode)this.applicationModel.findOperations(this.targetOperation.getName()).get(0)).getRequestParameters().get(0)).getDataType().getFields();

      }
      catch (CloudApplicationAdapterException e)
      {
        if (this.logger != null) {
          this.logger.logSevere("Exception caught while processing request parameters for Enterprise WSDL" + e.getMessage());
        }
        e.printStackTrace();
      }
      for (Field field : fields) {
        CloudDataObjectNode fieldTypeNode = (CloudDataObjectNodeImpl)field.getFieldType();
        
        if (fieldTypeNode.getObjectCategory().equals(ObjectCategory.BUILTIN))
        {
          if ((field.getName().equalsIgnoreCase("zObjectType")) || (field.getName().equalsIgnoreCase("queryString")) || (field.getName().equalsIgnoreCase("searchString")))
          {

            for (TypeMapping dataNodeTM : selectedObjects) {
              CloudDataObjectNode dataNode = dataNodeTM.getTargetDataObject();
              
              if (dataNode.getName().equalsIgnoreCase("QueryParameters"))
              {
                tmList.add(dataNodeTM);
              }
            }
          } else {
            CloudDataObjectNodeImpl typeMapNode = new CloudDataObjectNodeImpl(fieldTypeNode.getParent(), new QName("http://www.w3.org/2001/XMLSchema", field.getName()), fieldTypeNode.getObjectCategory(), fieldTypeNode.getDataType(), false);
            TypeMapping tm = new TypeMapping(typeMapNode);
            tm.setOverridingName(field.getName());
            tmList.add(tm);
          }
        } else if (fieldTypeNode.getObjectCategory().equals(ObjectCategory.STANDARD))
        {
          if (field.getFieldType().getName().equalsIgnoreCase("zObject"))
          {
            tmList.addAll(selectedObjects);
          } else if (field.getFieldType().getName().equalsIgnoreCase("ID"))
          {
            CloudDataObjectNode typeMapNode = new CloudDataObjectNodeImpl(null, new QName("http://www.w3.org/2001/XMLSchema", field.getName()), ObjectCategory.BUILTIN, DataType.ID, false);
            typeMapNode.addField(field);
            TypeMapping tm = new TypeMapping(typeMapNode);
            tm.setOverridingName(field.getName());
            tmList.add(tm);
          } else if (fieldTypeNode.getName().equalsIgnoreCase("MergeRequest"))
          {
            Set<Field> innerFields = null;
            try {
              innerFields = fieldTypeNode.getFields();
            } catch (CloudApplicationAdapterException e) {
              if (this.logger != null) {
                this.logger.logSevere("Exception caught while processing MergeRequest for Enterprise WSDL" + e.getMessage());
              }
              e.printStackTrace();
            }
            Set<Field> includedFields = new HashSet();
            for (Field innerField : innerFields) {
              if (innerField.getName().equalsIgnoreCase("masterRecord"))
              {

                for (TypeMapping dataNodeTM : selectedObjects) {
                  includedFields.add(new FieldImpl(innerField.getName(), dataNodeTM.getTargetDataObject(), false, false, false));
                }
                

              }
              else if (innerField.getName().equalsIgnoreCase("recordToMergeIds"))
              {
                CloudDataObjectNode typeMapNode = new CloudDataObjectNodeImpl(null, new QName("http://www.w3.org/2001/XMLSchema", innerField.getFieldType().getName()), ObjectCategory.BUILTIN, DataType.ID, false);
                Field newField = new FieldImpl(innerField.getName(), typeMapNode, false, false, innerField.isNullAllowed());
                includedFields.add(newField);
              }
              else {
                includedFields.add(innerField);
              }
            }
            TypeMask mask = new TypeMask();
            mask.setIncludedFields(includedFields);
            if (this.context.getContextObject("WSDL").toString().equals("enterprise"))
            {
              mask.setNamespaceOverride("http://api.zuora.com");
            }
            
            mask.setNamespaceOverride(this.targetNamespace);
            CloudDataObjectNodeImpl typeMapNode = new CloudDataObjectNodeImpl(fieldTypeNode.getParent(), fieldTypeNode.getQualifiedName(), fieldTypeNode.getObjectCategory(), fieldTypeNode.getDataType(), false);
            typeMapNode.addFields(includedFields);
            TypeMapping typeMap = new TypeMapping(typeMapNode);
            typeMap.setMask(mask);
            tmList.add(typeMap);
          }
          else {
            tmList.add(new TypeMapping(fieldTypeNode));
          }
        }
      }
    }
    

    ((OperationMapping)this.model.getOperationMappings().get(0)).setRequestObjectMappings(tmList);
  }
}