package com.cognizant.ipm.adapter.query;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.namespace.QName;

import oracle.tip.tools.adapters.cloud.utils.CloudAdapterUtils;
import oracle.tip.tools.ide.adapters.cloud.api.metadata.TypeMapping;
import oracle.tip.tools.ide.adapters.cloud.api.metadata.TypeMask;
import oracle.tip.tools.ide.adapters.cloud.api.model.CloudDataObjectNode;
import oracle.tip.tools.ide.adapters.cloud.api.model.CloudOperationNode;
import oracle.tip.tools.ide.adapters.cloud.api.model.DataType;
import oracle.tip.tools.ide.adapters.cloud.api.model.Field;
import oracle.tip.tools.ide.adapters.cloud.api.model.ObjectCategory;
import oracle.tip.tools.ide.adapters.cloud.api.plugin.AdapterPluginContext;
import oracle.tip.tools.ide.adapters.cloud.api.plugin.CloudApplicationAdapterException;
import oracle.tip.tools.ide.adapters.cloud.impl.metadata.model.CloudDataObjectNodeImpl;
import oracle.tip.tools.ide.adapters.cloud.impl.metadata.model.FieldImpl;

import org.apache.commons.lang3.StringUtils;

import com.cognizant.ipm.adapter.util.CloudUtil;
import com.cognizant.ipm.adapter.util.CloudValidationError;
import com.cognizant.ipm.adapter.util.ValidationKeyword;

public class CloudQueryParserImpl extends AbstractCloudQueryParser
{
   Pattern patternTypeOfQuery = Pattern.compile("\\b(when|typeof|else|end)\\b", 2);
   
   Pattern patternInnerQuery = Pattern.compile("\\(\\s*select.*?[^A-Z]?\\)");
   Pattern patternCommaComma = Pattern.compile("\\,\\s*,");
   Pattern patternCommaFrom = Pattern.compile("\\,\\s*(\\s*from\\s*s)", 2);
   
   Pattern patternBlankCount = Pattern.compile("count\\s*\\(\\s*\\)\\s*", 2);
   
 
   private static String curNS = "";
   private static String curQDrivingObjName = "";
   private static String mainObjAlias = "";
   private static String curObjAlias = "";
   
   private static final String STR_INNER_QUERY = "innerquery";
   private static final String STR_MAIN_QUERY = "mainQuery";
   private String qType = "";
   Set<String> usedTypeNames = null;
   Set<String> usedTableAliasNames = null;
   
   Set<String> usedFieldAliasNames = null;
   
   int nameCounter = 1;
 
   private Map<String, String> actualDrivingNameOfInnerQMap = null;
   private Map<String, CloudQueryModel> innerQModelsMap = null;
   List<String[]> fieldsPropList = null;
   private static String alias = null;
   private static final String expr = "expr";
   private static int count = 0;
   private String currentQUnderProcess;
   
   public CloudQueryParserImpl(AdapterPluginContext p_adapterPluginContext, String p_serviceTargetNameSpace)
   {
     super(p_adapterPluginContext, p_serviceTargetNameSpace);
	  System.out.println("Executing method "+Thread.currentThread().getStackTrace()[1]);
     adapterPluginContext = p_adapterPluginContext;
   }
   
 
   public String removeExtraFormatting(String queryString)
   {
		  System.out.println("Executing method "+Thread.currentThread().getStackTrace()[1]);
     queryString = queryString.replaceAll("\\n|\\r", " ");
     queryString = queryString.replaceAll("\\s+", " ");
     queryString = queryString.replaceAll("\\s*,\\s*", " , ");
     queryString = queryString.replaceAll("\\s*\\(\\s*", " ( ");
     queryString = queryString.replaceAll("\\s*\\)\\s*", " ) ");
     return queryString;
   }
 
   public void processBasicValidations(String queryStatement)
     throws CloudApplicationAdapterException
   {
		  System.out.println("Executing method "+Thread.currentThread().getStackTrace()[1]);
     queryStatement = queryStatement.trim();
     
     if (!queryStatement.startsWith("select ")) {
       List<ValidationKeyword> keywords = new ArrayList();
       CloudValidationError cloudError = new CloudValidationError("OP_INVALID_SELECT_ERROR", keywords);
       
       this.errorList.add(cloudError);
       throw new CloudApplicationAdapterException("Validation Failed");
     }
 
     if (queryStatement.contains("from"))
     {
       if (queryStatement.endsWith(" from")) {
         List<ValidationKeyword> keywords = new ArrayList();
         CloudValidationError cloudError = new CloudValidationError("OP_EMPTY_OBJECT_ERROR", keywords);
         
         this.errorList.add(cloudError);
         throw new CloudApplicationAdapterException("Validation Failed");
       }
       
       if (!queryStatement.contains(" from "))
       {
         List<ValidationKeyword> keywords = new ArrayList();
         CloudValidationError cloudError = new CloudValidationError("OP_INVALID_FROM_ERROR", keywords);
         
         this.errorList.add(cloudError);
         throw new CloudApplicationAdapterException("Validation Failed");
       }
     } else {
       List<ValidationKeyword> keywords = new ArrayList();
       CloudValidationError cloudError = new CloudValidationError("OP_INVALID_FROM_ERROR", keywords);
       
       this.errorList.add(cloudError);
       throw new CloudApplicationAdapterException("Validation Failed");
     }
     if (queryStatement.endsWith(" where")) {
       List<ValidationKeyword> keywords = new ArrayList();
       CloudValidationError cloudError = new CloudValidationError("OP_NO_CRITERIA_AFTER_WHERE_ERROR", keywords);
       
       this.errorList.add(cloudError);
       throw new CloudApplicationAdapterException("Validation Failed");
     }
   }
 
   public CloudQueryModel buildQueryModel(String queryString)
     throws CloudApplicationAdapterException
   {
		  System.out.println("Executing method "+Thread.currentThread().getStackTrace()[1]);
     try
     {
       this.usedTypeNames = new HashSet();
       this.usedTableAliasNames = new HashSet();
       this.usedFieldAliasNames = new HashSet();
       this.nameCounter = 1;
       count = 0;
       this.actualDrivingNameOfInnerQMap = new HashMap();
       this.errorList = new ArrayList();
       processBasicValidations(queryString);
       curNS = this.serviceTargetNameSpace;
       Matcher typeOfMatcher = this.patternTypeOfQuery.matcher(queryString);
       boolean isPolymorphicQuery = typeOfMatcher.find();
       if (isPolymorphicQuery) {
         return buildQueryModelForPolyQuery(queryString);
       }
       return buildQueryModelForNormalQuery(queryString);
     }
     catch (CloudApplicationAdapterException caae)
     {
       CloudQueryModel qModel = new CloudQueryModel();
       qModel.setErrorList(this.errorList);
       return qModel;
     }
   }
 
   private CloudQueryModel buildQueryModelForNormalQuery(String p_queryStatement)
     throws CloudApplicationAdapterException
   {
		  System.out.println("Executing method "+Thread.currentThread().getStackTrace()[1]);
     Map<String, String> queryMap = getQueriesMap(p_queryStatement);
     if ((queryMap != null) && (queryMap.containsKey("mainQuery")))
     {
       String mainQuery = (String)queryMap.get("mainQuery");
       CloudDataObjectNode mainObj = null;
       processBasicValidations(mainQuery);
       String[] tablePropArr = extractDrivingObjPropFromQuery(mainQuery);
       mainObj = validateObject(tablePropArr[1], true);
       mainObjAlias = tablePropArr[2];
       
       Map.Entry<String, String> mainQueryEntry = null;
       Iterator<Map.Entry<String, String>> entries = queryMap.entrySet().iterator();
       
       while (entries.hasNext()) {
         Map.Entry<String, String> entry = (Map.Entry)entries.next();
         if (((String)entry.getKey()).equalsIgnoreCase("mainQuery"))
         {
           mainQueryEntry = entry;
         }
         else if (doesInnerQueryRepresentsField(mainQuery, (String)entry.getKey())) {
           this.queryModel = new CloudQueryModel();
           this.queryModel.setMainQDrivingObject(mainObj);
           processQuery(entry);
         }
       }
       
       if (mainQueryEntry != null) {
         this.queryModel = new CloudQueryModel();
         this.queryModel.setMainQDrivingObject(mainObj);
         processQuery(mainQueryEntry);
       }
     }
     return this.queryModel;
   }
   
 
   private void processQuery(Map.Entry<String, String> entryParam)
     throws CloudApplicationAdapterException
   {
		  System.out.println("Executing method "+Thread.currentThread().getStackTrace()[1]);
     this.fieldsPropList = new ArrayList();
     this.currentQUnderProcess = "";
     
     String queryStatement = (String)entryParam.getValue();
     processBasicValidations(queryStatement);
     this.qType = ((String)entryParam.getKey());
     this.currentQUnderProcess = queryStatement.toLowerCase();
     String strBetweenSelectAndFrom = extractStringBtwSelectFrom(queryStatement);
     if (strBetweenSelectAndFrom.equals("")) {
       List<ValidationKeyword> keywords = new ArrayList();
       CloudValidationError cloudError = new CloudValidationError("OP_EMPTY_FIELDS_AFTER_SELECT_ERROR", keywords);
       this.errorList.add(cloudError);
       throw new CloudApplicationAdapterException("Validation Failed");
     }
     processTables(queryStatement);
     String fieldsStr = queryStatement.toLowerCase().substring(queryStatement.toLowerCase().indexOf("select"), queryStatement.toLowerCase().indexOf("from"));
     fieldsStr = fieldsStr.substring(queryStatement.indexOf(" "), fieldsStr.length()).trim();
     processFields(fieldsStr);
   }
   
   private void processFields(String queryString) throws CloudApplicationAdapterException
   {
     populateFieldsPropList(queryString);
     processFieldsPropList();
   }
 
   private void processFieldsPropList()
     throws CloudApplicationAdapterException
   {
		  System.out.println("Executing method "+Thread.currentThread().getStackTrace()[1]);
     CloudDataObjectNode dummyObject = null;
     boolean isRecursiveObjects = false;
     if ((!this.qType.equalsIgnoreCase("mainQuery")) && 
       (this.queryModel.getCurQDrivingObject().getName().equalsIgnoreCase(this.queryModel.getMainQDrivingObject().getName())))
     {
       isRecursiveObjects = true;
       String dummyObjName = (String)this.actualDrivingNameOfInnerQMap.get(this.qType);
       dummyObject = new CloudDataObjectNodeImpl(null, new QName(this.queryModel.getCurQDrivingObject().getQualifiedName().getNamespaceURI(), dummyObjName), this.queryModel.getCurQDrivingObject().getObjectCategory(), this.queryModel.getCurQDrivingObject().getDataType());
     }
 
     boolean isIDFieldAdded = false;
     for (String[] fieldPropArr : this.fieldsPropList) {
       String isAgr = fieldPropArr[0];
       String fields = fieldPropArr[1];
       String aliasName = fieldPropArr[2];
       if (fields.contains(","))
       {
         String[] multiFields = fields.split(",");
         for (String field : multiFields) {
           if (fields.contains(".")) {
             Field tmpf = processFieldsDotRelations(fields, false, aliasName);
             
             if (tmpf.getName().toLowerCase().equalsIgnoreCase("id")) {
               isIDFieldAdded = true;
             }
           } else {
             validateField(this.queryModel.getCurQDrivingObject(), field, true, 2);
           }
         }
         Field aggrField = new FieldImpl(aliasName, new CloudDataObjectNodeImpl(null, new QName("http://www.w3.org/2001/XMLSchema", "double"), ObjectCategory.BUILTIN, DataType.DOUBLE), false, false, true);
         if (isRecursiveObjects)
         {
           addFieldToQueryModel("", aggrField, aliasName);
         }
         else
         {
           addFieldToQueryModel("", aggrField, aliasName);
         }
         
       }
       else if (fields.contains("."))
       {
         Field field = processFieldsDotRelations(fields, true, aliasName);
         
         if (field.getName().toLowerCase().equalsIgnoreCase("id")) {
           isIDFieldAdded = true;
         }
       }
       else if (fields.contains("innerquery"))
       {
         CloudQueryModel innerQueryModel = (CloudQueryModel)this.innerQModelsMap.get(fields);
         Set<Field> responseInnerFields = null;
         CloudDataObjectNode responseNode = ((CloudOperationNode)adapterPluginContext.getCloudApplicationModel().findOperations(adapterPluginContext.getContextObject("cloudOperation").toString()).get(0)).getResponse().getResponseObject();
         Set<Field> responseFields = null;
         try {
           responseFields = responseNode.getFields();
         } catch (CloudApplicationAdapterException e) {
           List<ValidationKeyword> keywords = new ArrayList();
           CloudValidationError cloudError = new CloudValidationError("OP_QUERY_METADATA_ERROR", keywords);
           
           this.errorList.add(cloudError);
           throw new CloudApplicationAdapterException("Validation Failed");
         }
         
         for (Field responseField : responseFields) {
           CloudDataObjectNode responseFieldTypeNode = responseField.getFieldType();
           
           if (responseField.getFieldType().getName().endsWith("esult")) {
             try
             {
               responseInnerFields = responseField.getFieldType().getFields();
             }
             catch (CloudApplicationAdapterException e) {
               List<ValidationKeyword> keywords = new ArrayList();
               CloudValidationError cloudError = new CloudValidationError("OP_QUERY_METADATA_ERROR", keywords);
               
 
               this.errorList.add(cloudError);
               throw new CloudApplicationAdapterException("Validation Failed");
             }
           }
         }
         
         String fieldParentName = innerQueryModel.getCurQDrivingObject().getName();
         
         QueryItemMetadata queryItem = innerQueryModel.getQueryItemByObjectName(fieldParentName);
         
         CloudDataObjectNode tmpNode = queryItem.getCustomObjectNode();
         
         Set<Field> fieldSet = tmpNode.getFields();
         CloudDataObjectNode finalInnerNode = new CloudDataObjectNodeImpl(null, tmpNode.getQualifiedName(), tmpNode.getObjectCategory(), tmpNode.getDataType(), tmpNode.isAnonymous());
         
 
 
         for (Field responseInnerField : responseInnerFields) {
           if (responseInnerField.getFieldType().getName().equalsIgnoreCase("zObject"))
           {
 
 
 
             CloudDataObjectNode appendedFieldRecordsNode = new CloudDataObjectNodeImpl(null, new QName(curNS, tmpNode.getName() + "Records"), responseInnerField.getFieldType().getObjectCategory(), responseInnerField.getFieldType().getDataType());
             
 
 
 
 
 
             appendedFieldRecordsNode.addFields(fieldSet);
             Field appendedFieldRecordsField = new FieldImpl(responseInnerField.getName(), appendedFieldRecordsNode, true, false, true);
             
 
 
             finalInnerNode.addField(appendedFieldRecordsField);
           }
           else {
             finalInnerNode.addField(responseInnerField);
           }
         }
         
 
 
 
         String innerAlias = (String)this.actualDrivingNameOfInnerQMap.get(fields);
         
 
 
         Field innerQueryAsField = new FieldImpl(innerAlias, finalInnerNode, true, false, true);
         
 
 
         if (isRecursiveObjects)
         {
 
           addFieldToQueryModel("", innerQueryAsField, innerAlias);
 
 
         }
         else
         {
 
           addFieldToQueryModel("", innerQueryAsField, innerAlias);
        }
      }
      else {
        Field field = validateField(this.queryModel.getCurQDrivingObject(), fields, true, 2);
        

        if (field.getName().toLowerCase().equalsIgnoreCase("id"))
        {
          isIDFieldAdded = true;
        }
        if (aliasName.equalsIgnoreCase(field.getName()))
        {
          if (isRecursiveObjects)
          {


            addFieldToQueryModel("", field, aliasName);

          }
          else
          {

            addFieldToQueryModel("", field, aliasName);
          }
        } else {
          Field aggrField = null;
          if ((isAgr != null) && (!isAgr.isEmpty())) {
            if (isAgr.equals("1"))
            {
              aggrField = new FieldImpl(aliasName, new CloudDataObjectNodeImpl(null, new QName("http://www.w3.org/2001/XMLSchema", "double"), ObjectCategory.BUILTIN, DataType.DOUBLE), false, false, true);




            }
            else
            {



              aggrField = new FieldImpl(aliasName, field.getFieldType(), false, false, true);
            }
          }
          



          if (isRecursiveObjects)
          {


            addFieldToQueryModel("", aggrField, aliasName);

          }
          else
          {
            addFieldToQueryModel("", aggrField, aliasName);
          }
        }
      }
    }
    

    if (!isIDFieldAdded) {
      addFieldToQueryModel("", CloudUtil.getIDField("http://api.zuora.com"), "Id");
    }
  }
  


  private void populateFieldsPropList(String fieldsStr)
    throws CloudApplicationAdapterException
  {
    while (fieldsStr.contains("(")) {
      if (!fieldsStr.contains(")")) {
        List<ValidationKeyword> keywords = new ArrayList();
        CloudValidationError cloudError = new CloudValidationError("OP_MISSING_BRACKET_ERROR", keywords);
        
        this.errorList.add(cloudError);
        throw new CloudApplicationAdapterException("Validation Failed");
      }
      

      int startIndex = fieldsStr.indexOf("(");
      int endIndex = 0;
      String tempStr = "";
      endIndex = findClosingBracketIndex(startIndex, fieldsStr);
      if (fieldsStr.substring(0, startIndex).contains(",")) {
        startIndex = findComma(fieldsStr, startIndex, -1);
      } else if (!fieldsStr.substring(0, startIndex).trim().isEmpty()) {
        if ((fieldsStr.substring(startIndex + 1).trim().startsWith("innerquery")) || (fieldsStr.substring(0, startIndex).trim().contains(" ")))
        {


          List<ValidationKeyword> keywords = new ArrayList();
          keywords.add(new ValidationKeyword(fieldsStr.substring(0, startIndex).trim(), true));
          
          CloudValidationError cloudError = new CloudValidationError("OP_QUERY_MISSING_COMMA", keywords);
          
          this.errorList.add(cloudError);
          throw new CloudApplicationAdapterException("Validation Failed");
        }
        
        if (!validateFunction(fieldsStr.substring(0, startIndex).trim()))
        {
          List<ValidationKeyword> keywords = new ArrayList();
          keywords.add(new ValidationKeyword(fieldsStr.substring(0, startIndex).trim(), true));
          
          CloudValidationError cloudError = new CloudValidationError("OP_INVALID_AGR_FUNC_ERROR", keywords);
          
          this.errorList.add(cloudError);
          throw new CloudApplicationAdapterException("Validation Failed");
        }
        
        startIndex = 0;
      }
      





      endIndex = fieldsStr.substring(endIndex, fieldsStr.length()).contains(",") ? endIndex + fieldsStr.substring(endIndex, fieldsStr.length()).indexOf(",") + 1 : fieldsStr.length();
      


      tempStr = fieldsStr.substring(startIndex, endIndex);
      if ((tempStr.trim().startsWith(",")) && (tempStr.trim().endsWith(","))) {
        tempStr = tempStr.trim().substring(1);
      }
      
      if (tempStr.substring(tempStr.indexOf("(") + 1, tempStr.lastIndexOf(")")).trim().isEmpty())
      {
        List<ValidationKeyword> keywords = new ArrayList();
        CloudValidationError cloudError = new CloudValidationError("OP_BLANK_BRACKET_ERROR", keywords);
        
        this.errorList.add(cloudError);
        throw new CloudApplicationAdapterException("Validation Failed");
      }
      
      String aggrFunc = "";
      if (!tempStr.contains("innerquery")) {
        if (tempStr.trim().indexOf("(") == 0) {
          List<ValidationKeyword> keywords = new ArrayList();
          keywords.add(new ValidationKeyword("(", true));
          CloudValidationError cloudError = new CloudValidationError("OP_FUNC_CLAUSE_MISSING", keywords);
          
          this.errorList.add(cloudError);
          alias = null;
          throw new CloudApplicationAdapterException("Validation Failed");
        }
        
        if (tempStr.trim().startsWith(",")) {
          aggrFunc = tempStr.trim().substring(1, tempStr.indexOf("(") - 1).trim();
        }
        else {
          aggrFunc = tempStr.trim().substring(0, tempStr.indexOf("(") - 1).trim();
        }
        

        if (!validateFunction(aggrFunc)) {
          List<ValidationKeyword> keywords = new ArrayList();
          keywords.add(new ValidationKeyword(aggrFunc, true));
          CloudValidationError cloudError = new CloudValidationError("OP_INVALID_AGR_FUNC_ERROR", keywords);
          
          this.errorList.add(cloudError);
          alias = null;
          throw new CloudApplicationAdapterException("Validation Failed");
        }
        

        if (!this.qType.equalsIgnoreCase("mainQuery")) {
          if (validateAgrFunction(aggrFunc)) {
            List<ValidationKeyword> keywords = new ArrayList();
            keywords.add(new ValidationKeyword(aggrFunc, true));
            CloudValidationError cloudError = new CloudValidationError("OP_INNER_QUERY_AGGR_EXPR", keywords);
            
            this.errorList.add(cloudError);
            throw new CloudApplicationAdapterException("Validation Failed");
          }
          
          if (validateDateFunction(aggrFunc)) {
            List<ValidationKeyword> keywords = new ArrayList();
            keywords.add(new ValidationKeyword(aggrFunc, true));
            CloudValidationError cloudError = new CloudValidationError("OP_INNER_QUERY_DATE_FUNC_ERR", keywords);
            
            this.errorList.add(cloudError);
            throw new CloudApplicationAdapterException("Validation Failed");
          }
        }
      }
      

      fieldsStr = fieldsStr.replace(tempStr, "");
      if (tempStr.trim().startsWith(",")) {
        tempStr = tempStr.trim().substring(1);
      } else if (tempStr.trim().endsWith(",")) {
        tempStr = tempStr.trim().substring(0, tempStr.length() - 2);
      }
      if (alias == null) {
        if (tempStr.substring(tempStr.lastIndexOf(")") + 1).trim().length() > 0)
        {
          alias = tempStr.substring(tempStr.lastIndexOf(")") + 1).trim();
          

          int k = this.queryLowerCase.indexOf(alias);
          alias = this.queryDefaultCase.substring(k, k + alias.length());

        }
        else if (!tempStr.contains("innerquery")) {
          if ((aggrFunc.equalsIgnoreCase("convertcurrency")) || (aggrFunc.equalsIgnoreCase("tolabel")))
          {


            alias = tempStr.substring(tempStr.indexOf("(") + 1, tempStr.lastIndexOf(")")).trim();
            
            if (alias.contains(".")) {
              alias = alias.substring(alias.lastIndexOf(".") + 1);
            }
          }
          else {
            alias = "expr" + count;
            count += 1;
          }
        } else {
          alias = "expr" + count;
          count += 1;
        }
      }
      

      tempStr = tempStr.substring(tempStr.indexOf("(") + 1, tempStr.lastIndexOf(")"));
      
      if (tempStr.contains("(")) {
        populateFieldsPropList(tempStr);
      }
      else {
        String[] values = new String[2];
        values[0] = "1";
        values[1] = tempStr;
        

        boolean ok = CloudAdapterUtils.isNCName(alias);
        if (!ok) {
          List<ValidationKeyword> keywords = new ArrayList();
          keywords.add(new ValidationKeyword(alias, true));
          CloudValidationError cloudError = new CloudValidationError("OP_ALIAS_NAME_NOT_ACC_TO_NCNAME", keywords);
          
          this.errorList.add(cloudError);
          alias = null;
          throw new CloudApplicationAdapterException("Validation Failed");
        }
        

        if (aggrFunc.equalsIgnoreCase(" count".trim()))
        {
          if (tempStr.contains(",")) {
            List<ValidationKeyword> keywords = new ArrayList();
            keywords.add(new ValidationKeyword(",", true));
            CloudValidationError cloudError = new CloudValidationError("OP_QUERY_INVALID_TOKEN_FOUND", keywords);
            
            this.errorList.add(cloudError);
            throw new CloudApplicationAdapterException("Validation Failed");
          }
        }
        


        String[] fieldPropArr = new String[3];
        fieldPropArr[0] = values[0];
        fieldPropArr[1] = values[1];
        fieldPropArr[2] = alias;
        this.fieldsPropList.add(fieldPropArr);
        alias = null;
      }
    }
    if ((!fieldsStr.trim().equalsIgnoreCase("")) && (fieldsStr.trim().length() > 0))
    {
      fieldsStr = fieldsStr.trim();
      checkDoubleComma(fieldsStr);
      if ((fieldsStr.startsWith(",")) || (fieldsStr.endsWith(","))) {
        List<ValidationKeyword> keywords = new ArrayList();
        keywords.add(new ValidationKeyword(",", true));
        CloudValidationError cloudError = new CloudValidationError("OP_QUERY_INVALID_TOKEN_FOUND", keywords);
        
        this.errorList.add(cloudError);
        throw new CloudApplicationAdapterException("Validation Failed");
      }
      String[] fieldList = fieldsStr.split("\\,");
      for (int i = 0; i < fieldList.length; i++) {
        String fieldString = fieldList[i].trim();
        if ((fieldString != null) && (!fieldString.equalsIgnoreCase("")) && (fieldString.length() > 0))
        {
          if (fieldString.contains(" ")) {
            String[] fieldProp = fieldString.split(" ");
            
            String[] fieldPropArr = new String[3];
            fieldPropArr[0] = "0";
            fieldPropArr[1] = fieldProp[0];
            if ((!fieldProp[1].equalsIgnoreCase("")) && (fieldProp[1].length() > 0))
            {

              if (isAliasReservedKeyword(fieldProp[1])) {
                List<ValidationKeyword> keywords = new ArrayList();
                keywords.add(new ValidationKeyword(fieldProp[1], true));
                
                CloudValidationError cloudError = new CloudValidationError("OP_QUERY_ALIAS_RESERVED", keywords);
                
                this.errorList.add(cloudError);
                throw new CloudApplicationAdapterException("Validation Failed");
              }
              

              if (!checkFieldGrouping(fieldProp[0].trim())) {
                List<ValidationKeyword> keywords = new ArrayList();
                keywords.add(new ValidationKeyword(fieldProp[1], true));
                
                CloudValidationError cloudError = new CloudValidationError("OP_QUERY_ALIAS_ERROR", keywords);
                
                this.errorList.add(cloudError);
                throw new CloudApplicationAdapterException("Validation Failed");
              }
              










              if (!this.usedFieldAliasNames.add(fieldProp[1])) {
                List<ValidationKeyword> keywords = new ArrayList();
                keywords.add(new ValidationKeyword(fieldProp[1], true));
                
                CloudValidationError cloudError = new CloudValidationError("OP_DUPLICATE_ALIAS", keywords);
                
                this.errorList.add(cloudError);
                throw new CloudApplicationAdapterException("Validation Failed");
              }
              
              fieldPropArr[2] = fieldProp[1];
            } else {
              fieldPropArr[2] = fieldProp[0];
            }
            this.fieldsPropList.add(fieldPropArr);
          }
          else {
            String[] fieldPropArr = new String[3];
            fieldPropArr[0] = "0";
            fieldPropArr[1] = fieldString;
            String tmpString = fieldString.toLowerCase();
            if (tmpString.contains(".")) {
              tmpString = tmpString.substring(tmpString.lastIndexOf(".") + 1, tmpString.length());
            }
            

            fieldPropArr[2] = tmpString;
            



            if (isFieldAlreadyAdded(fieldPropArr))
            {
              List<ValidationKeyword> keywords = new ArrayList();
              keywords.add(new ValidationKeyword(fieldString, true));
              
              CloudValidationError cloudError = new CloudValidationError("OP_QUERY_DUPLICATE_FIELD", keywords);
              
              this.errorList.add(cloudError);
              throw new CloudApplicationAdapterException("Validation Failed");
            }
            
            this.fieldsPropList.add(fieldPropArr);
          }
        }
      }
    }
  }
  
  private boolean isFieldAlreadyAdded(String[] p_fieldProp)
  {
    for (String[] fieldPropArr : this.fieldsPropList) {
      if ((fieldPropArr[2].equalsIgnoreCase(p_fieldProp[2])) && (fieldPropArr[1].equalsIgnoreCase(p_fieldProp[1])))
      {
        return true;
      }
    }
    return false;
  }
  







  private Map<String, String> getQueriesMap(String queryString)
    throws CloudApplicationAdapterException
  {
    Map<String, String> queryMap = new HashMap();
    



    int counter = 0;
    
    Matcher matchInner = this.patternInnerQuery.matcher(queryString.toLowerCase());
    
    boolean success = matchInner.find();
    
    while (success) {
      String innerQuery = matchInner.group(0);
      String tmpStr = innerQuery;
      innerQuery = innerQuery.substring(innerQuery.indexOf("(") + 1);
      innerQuery = innerQuery.substring(0, innerQuery.lastIndexOf(")"));
      if (innerQuery.contains("(")) {
        int bracCloseIndex = findClosingBracketIndex(queryString.indexOf(tmpStr), queryString);
        
        innerQuery = queryString.substring(queryString.indexOf(tmpStr), bracCloseIndex + 1);
        
        queryString = queryString.toLowerCase().replace(innerQuery, "innerquery" + counter);
        
        innerQuery = innerQuery.substring(innerQuery.indexOf("(") + 1);
        innerQuery = innerQuery.substring(0, innerQuery.lastIndexOf(")"));
        
        Matcher matchBlankCount = this.patternBlankCount.matcher(innerQuery.toLowerCase());
        
        if (matchBlankCount.find()) {
          List<ValidationKeyword> keywords = new ArrayList();
          CloudValidationError cloudError = new CloudValidationError("OP_INNER_QUERY_COUNT()", keywords);
          
          this.errorList.add(cloudError);
          throw new CloudApplicationAdapterException("Validation Failed");
        }
        

        String btwSelectFrom = extractStringBtwSelectFrom(innerQuery);
        if (btwSelectFrom.contains("(")) {
          int innerBracCloseIndex = findClosingBracketIndex(btwSelectFrom.indexOf("("), btwSelectFrom);
          

          String tmpStrForAgr = " " + btwSelectFrom;
          String afrFuncName = tmpStrForAgr.substring(tmpStrForAgr.substring(0, tmpStrForAgr.indexOf("(")).lastIndexOf(" ") + 1, tmpStrForAgr.indexOf("(")).trim();
          





          btwSelectFrom = btwSelectFrom.substring(btwSelectFrom.indexOf("(") + 1, innerBracCloseIndex);
          

          if (btwSelectFrom.toLowerCase().contains("select "))
          {
            List<ValidationKeyword> keywords = new ArrayList();
            CloudValidationError cloudError = new CloudValidationError("OP_INNER_QUERY_MAX_LEVEL", keywords);
            
            this.errorList.add(cloudError);
            throw new CloudApplicationAdapterException("Validation Failed");




          }
          




        }
        





      }
      else
      {





        queryString = queryString.toLowerCase().replace(innerQuery, "innerquery" + counter);
      }
      
      innerQuery = innerQuery.trim();
      queryMap.put("innerquery" + counter, innerQuery);
      Matcher matchCommaComma = this.patternCommaComma.matcher(queryString.toLowerCase());
      
      if (matchCommaComma.find()) {
        String commaStr = matchCommaComma.group(0);
        queryString = queryString.toLowerCase().replace(commaStr, ",");
      }
      Matcher matchCommaFrom = this.patternCommaFrom.matcher(queryString.toLowerCase());
      
      if (matchCommaFrom.find())
      {
        String commaStr = matchCommaFrom.group(0);
        String fromStr = commaStr.substring(commaStr.indexOf(",") + 1);
        
        queryString = queryString.toLowerCase().replace(commaStr, fromStr);
      }
      try
      {
        success = matchInner.find();
        counter++;
      } catch (Exception e) {
        e.printStackTrace();
      }
    }
    


    Matcher matchBlankCountMain = this.patternBlankCount.matcher(queryString.toLowerCase());
    
    if (matchBlankCountMain.find()) {
      String strBetweenSelectAndFrom = extractStringBtwSelectFrom(queryString);
      if (((queryMap != null) && (queryMap.size() > 0)) || (strBetweenSelectAndFrom.contains(",")))
      {
        List<ValidationKeyword> keywords = new ArrayList();
        keywords.add(new ValidationKeyword(",", true));
        CloudValidationError cloudError = new CloudValidationError("OP_QUERY_INVALID_TOKEN_FOUND", keywords);
        
        this.errorList.add(cloudError);
        throw new CloudApplicationAdapterException("Validation Failed");
      }
    }
    else {
      queryString = queryString.trim();
      queryMap.put("mainQuery", queryString);
    }
    return queryMap;
  }
  
  private int findClosingBracketIndex(int startIndex, String stringToProcess) throws CloudApplicationAdapterException
  {
    int bracketCount = 1;
    int endIndex = 0;
    for (int i = startIndex + 1; i < stringToProcess.length(); i++) {
      if (stringToProcess.charAt(i) == '(') {
        bracketCount++;
      } else if (stringToProcess.charAt(i) == ')') {
        bracketCount--;
      }
      if (bracketCount == 0) {
        endIndex = i;
        break;
      }
    }
    if (bracketCount > 0) {
      List<ValidationKeyword> keywords = new ArrayList();
      CloudValidationError cloudError = new CloudValidationError("OP_MISSING_BRACKET_ERROR", keywords);
      
      this.errorList.add(cloudError);
      throw new CloudApplicationAdapterException("Validation Failed");
    }
    
    return endIndex;
  }
  
  private String extractStringBtwSelectFrom(String queryString) throws CloudApplicationAdapterException
  {
    String strBetweenSelectAndFrom = null;
    processBasicValidations(queryString);
    strBetweenSelectAndFrom = queryString.substring("select ".length(), queryString.toLowerCase().indexOf("from")).trim();
    


    if (strBetweenSelectAndFrom.equals("")) {
      List<ValidationKeyword> keywords = new ArrayList();
      CloudValidationError cloudError = new CloudValidationError("OP_EMPTY_FIELDS_AFTER_SELECT_ERROR", keywords);
      
      this.errorList.add(cloudError);
      throw new CloudApplicationAdapterException("Validation Failed");
    }
    
    return strBetweenSelectAndFrom;
  }
  
  private void checkDoubleComma(String str) throws CloudApplicationAdapterException
  {
    Pattern patternCommaComma = Pattern.compile("\\,\\s*,");
    Matcher matchCommaComma = patternCommaComma.matcher(str.toLowerCase());
    if (matchCommaComma.find()) {
      String commaStr = matchCommaComma.group(0);
      List<ValidationKeyword> keywords = new ArrayList();
      keywords.add(new ValidationKeyword(commaStr, true));
      CloudValidationError cloudError = new CloudValidationError("OP_QUERY_INVALID_TOKEN_FOUND", keywords);
      
      this.errorList.add(cloudError);
      throw new CloudApplicationAdapterException("Validation Failed");
    }
  }
  
  private String extractTableNamesFromQuery(String queryString)
    throws CloudApplicationAdapterException
  {
    String firstPartStr = "";
    int endIndex = -1;
    
    boolean isClauseFound = false;
    int startIndex = queryString.toLowerCase().indexOf("from");
    if (queryString.toLowerCase().contains(" where ")) {
      isClauseFound = true;
      endIndex = queryString.toLowerCase().indexOf("where");
      if (startIndex > endIndex) {
        List<ValidationKeyword> keywords = new ArrayList();
        keywords.add(new ValidationKeyword("where", true));
        CloudValidationError cloudError = new CloudValidationError("OP_QUERY_INVALID_TOKEN_FOUND", keywords);
        
        this.errorList.add(cloudError);
        throw new CloudApplicationAdapterException("Validation Failed");
      }
    } else if (queryString.toLowerCase().contains(" with ")) {
      endIndex = queryString.toLowerCase().indexOf("with");
      if (startIndex > endIndex) {
        List<ValidationKeyword> keywords = new ArrayList();
        keywords.add(new ValidationKeyword("with", true));
        CloudValidationError cloudError = new CloudValidationError("OP_QUERY_INVALID_TOKEN_FOUND", keywords);
        
        this.errorList.add(cloudError);
        throw new CloudApplicationAdapterException("Validation Failed");
      }
      isClauseFound = true;
    } else if (queryString.toLowerCase().contains(" group by ")) {
      endIndex = queryString.toLowerCase().indexOf("group by");
      if (startIndex > endIndex) {
        List<ValidationKeyword> keywords = new ArrayList();
        keywords.add(new ValidationKeyword("group by", true));
        CloudValidationError cloudError = new CloudValidationError("OP_QUERY_INVALID_TOKEN_FOUND", keywords);
        
        this.errorList.add(cloudError);
        throw new CloudApplicationAdapterException("Validation Failed");
      }
      isClauseFound = true;
    } else if (queryString.toLowerCase().contains(" having ")) {
      endIndex = queryString.toLowerCase().indexOf("having");
      if (startIndex > endIndex) {
        List<ValidationKeyword> keywords = new ArrayList();
        keywords.add(new ValidationKeyword("having", true));
        CloudValidationError cloudError = new CloudValidationError("OP_QUERY_INVALID_TOKEN_FOUND", keywords);
        
        this.errorList.add(cloudError);
        throw new CloudApplicationAdapterException("Validation Failed");
      }
      isClauseFound = true;
    } else if (queryString.toLowerCase().contains(" order by ")) {
      endIndex = queryString.toLowerCase().indexOf("order by");
      if (startIndex > endIndex) {
        List<ValidationKeyword> keywords = new ArrayList();
        keywords.add(new ValidationKeyword("order by", true));
        CloudValidationError cloudError = new CloudValidationError("OP_QUERY_INVALID_TOKEN_FOUND", keywords);
        
        this.errorList.add(cloudError);
        throw new CloudApplicationAdapterException("Validation Failed");
      }
      isClauseFound = true;
    } else if (queryString.toLowerCase().contains(" limit ")) {
      endIndex = queryString.toLowerCase().indexOf("limit");
      if (startIndex > endIndex) {
        List<ValidationKeyword> keywords = new ArrayList();
        keywords.add(new ValidationKeyword("limit", true));
        CloudValidationError cloudError = new CloudValidationError("OP_QUERY_INVALID_TOKEN_FOUND", keywords);
        
        this.errorList.add(cloudError);
        throw new CloudApplicationAdapterException("Validation Failed");
      }
      isClauseFound = true;
    } else if (queryString.toLowerCase().contains(" for view ")) {
      endIndex = queryString.toLowerCase().indexOf("for view");
      if (startIndex > endIndex) {
        List<ValidationKeyword> keywords = new ArrayList();
        keywords.add(new ValidationKeyword("for view", true));
        CloudValidationError cloudError = new CloudValidationError("OP_QUERY_INVALID_TOKEN_FOUND", keywords);
        
        this.errorList.add(cloudError);
        throw new CloudApplicationAdapterException("Validation Failed");
      }
      isClauseFound = true;
    } else if (queryString.toLowerCase().contains(" for reference ")) {
      endIndex = queryString.toLowerCase().indexOf("for reference");
      if (startIndex > endIndex) {
        List<ValidationKeyword> keywords = new ArrayList();
        keywords.add(new ValidationKeyword("for reference", true));
        CloudValidationError cloudError = new CloudValidationError("OP_QUERY_INVALID_TOKEN_FOUND", keywords);
        
        this.errorList.add(cloudError);
        throw new CloudApplicationAdapterException("Validation Failed");
      }
      isClauseFound = true;
    } else if (queryString.toLowerCase().contains(" offset ")) {
      endIndex = queryString.toLowerCase().indexOf("offset");
      if (startIndex > endIndex) {
        List<ValidationKeyword> keywords = new ArrayList();
        keywords.add(new ValidationKeyword("offset", true));
        CloudValidationError cloudError = new CloudValidationError("OP_QUERY_INVALID_TOKEN_FOUND", keywords);
        
        this.errorList.add(cloudError);
        throw new CloudApplicationAdapterException("Validation Failed");
      }
      isClauseFound = true;
    } else if (queryString.toLowerCase().contains(" update viewstat ")) {
      endIndex = queryString.toLowerCase().indexOf("update viewstat");
      if (startIndex > endIndex) {
        List<ValidationKeyword> keywords = new ArrayList();
        keywords.add(new ValidationKeyword("update viewstat", true));
        CloudValidationError cloudError = new CloudValidationError("OP_QUERY_INVALID_TOKEN_FOUND", keywords);
        
        this.errorList.add(cloudError);
        throw new CloudApplicationAdapterException("Validation Failed");
      }
      isClauseFound = true;
    }
    
    if (isClauseFound) {
      firstPartStr = queryString.toLowerCase().substring(startIndex, endIndex);
    }
    else {
      firstPartStr = queryString.substring(startIndex, queryString.length());
    }
    

    firstPartStr = firstPartStr.substring(5, firstPartStr.length());
    return firstPartStr.trim();
  }
  







  private boolean doesInnerQueryRepresentsField(String mainQuery, String innerQueryKey)
    throws CloudApplicationAdapterException
  {
    String queryBtwSelectFrom = extractStringBtwSelectFrom(mainQuery);
    if (queryBtwSelectFrom.contains(innerQueryKey)) {
      return true;
    }
    
    return false;
  }
  
  private String[] extractDrivingObjPropFromQuery(String queryStatement)
    throws CloudApplicationAdapterException
  {
    String[] tablePropArr = new String[3];
    tablePropArr[0] = "";
    tablePropArr[2] = "";
    String tableNames = extractTableNamesFromQuery(queryStatement);
    

    String drivingSobjectName = "";
    if (tableNames.contains(",")) {
      String[] splitMultipleTables = tableNames.split(",");
      drivingSobjectName = splitMultipleTables[0];
    } else {
      drivingSobjectName = tableNames;
    }
    drivingSobjectName = drivingSobjectName.trim();
    

    if (drivingSobjectName.contains(" ")) {
      String[] splitMultipleTables = drivingSobjectName.split(" ");
      drivingSobjectName = splitMultipleTables[0];
      tablePropArr[2] = splitMultipleTables[1];
    }
    tablePropArr[1] = drivingSobjectName;
    
    return tablePropArr;
  }
  
  public CloudDataObjectNode validateObject(String objectName, boolean raiseError)
    throws CloudApplicationAdapterException
  {
    CloudDataObjectNode tableNode = null;
    boolean isTableNodeFound = false;
    if (this.queryModel != null) {
      QueryItemMetadata queryItem = this.queryModel.getTablesMetadataByObjectName(objectName);
      
      if (null != queryItem) {
        tableNode = queryItem.getModelObjectNode();
        isTableNodeFound = true;
      }
    }
    if (!isTableNodeFound) {
      tableNode = validateFromSObjectList(objectName, true);
      if ((tableNode == null) && (raiseError)) {
        List<ValidationKeyword> keywords = new ArrayList();
        keywords.add(new ValidationKeyword(objectName, true));
        CloudValidationError cloudError = new CloudValidationError("OP_INVALID_OBJECT_ERROR", keywords);
        
        this.errorList.add(cloudError);
        throw new CloudApplicationAdapterException("Validation Failed");
      }
    }
    
    return tableNode;
  }
  
  private CloudDataObjectNode validateFromSObjectList(String objectName, boolean chkQueryable) throws CloudApplicationAdapterException
  {
    CloudDataObjectNode tableNode = null;
    ListIterator<CloudDataObjectNode> niter = getSObjectDescendents().listIterator();
    
    while (niter.hasNext()) {
      CloudDataObjectNode nnode = (CloudDataObjectNode)niter.next();
      if (nnode.getName().equalsIgnoreCase(objectName)) {
        tableNode = nnode;
        if ((tableNode.getNodeAttributes().size() > 0) && (null != tableNode.getNodeAttributes().get("queryable")) && (chkQueryable))
        {

          if (CloudUtil.returnBooleanValue(tableNode.getNodeAttributes().get("queryable").toString()))
          {
            return nnode;
          }
          


          if (!this.qType.equalsIgnoreCase("mainQuery"))
          {

            return nnode;
          }
          List<ValidationKeyword> keywords = new ArrayList();
          keywords.add(new ValidationKeyword(tableNode.getName(), true));
          
          CloudValidationError cloudError = new CloudValidationError("OP_OBJECT_NOT_QUERYABLE", keywords);
          
          this.errorList.add(cloudError);
          throw new CloudApplicationAdapterException("Validation Failed");
        }
        


        return nnode;
      }
    }
    
    return tableNode;
  }
  
  public Field validateField(CloudDataObjectNode cdonode, String fieldName, boolean isCustomReturn, int relCheckLevel)
    throws CloudApplicationAdapterException
  {
    String idField = "Id";
    Field field = null;
    if ((fieldName.length() == 0) || (fieldName.equals(""))) {
      List<ValidationKeyword> keywords = new ArrayList();
      keywords.add(new ValidationKeyword(cdonode.getName(), true));
      CloudValidationError cloudError = new CloudValidationError("OP_BLANK_FIELD_ERROR", keywords);
      
      this.errorList.add(cloudError);
      throw new CloudApplicationAdapterException("Validation Failed");
    }
    
    fieldName = fieldName.trim();
    if (!fieldName.toLowerCase().equalsIgnoreCase(idField)) {
      try {
        String relatedDataObjName = CloudUtil.getTypeOfObjectRelationship(fieldName, cdonode.getName(), relCheckLevel, adapterPluginContext);
        


        if ((null != relatedDataObjName) && (!relatedDataObjName.isEmpty())) {
          field = CloudUtil.getBusinessObjectFieldByName(cdonode, fieldName);
          
          if (field == null) {
            CloudDataObjectNode tmp = validateFromSObjectList(cdonode.getName(), false);
            
            if (tmp != null) {
              field = CloudUtil.getBusinessObjectFieldByName(tmp, fieldName);
            }
            
          }
        }
      }
      catch (CloudApplicationAdapterException e)
      {
        List<ValidationKeyword> keywords = new ArrayList();
        keywords.add(new ValidationKeyword(e.getLocalizedMessage(), false));
        
        CloudValidationError cloudError = new CloudValidationError("OP_QUERY_METADATA_ERROR", keywords);
        
        this.errorList.add(cloudError);
        throw new CloudApplicationAdapterException("Validation Failed");
      }
      
      if (field == null) {
        List<ValidationKeyword> keywords = new ArrayList();
        keywords.add(new ValidationKeyword(fieldName, true));
        keywords.add(new ValidationKeyword(cdonode.getName(), false));
        CloudValidationError cloudError = new CloudValidationError("OP_INVALID_FIELD_ERROR", keywords);
        
        this.errorList.add(cloudError);
        throw new CloudApplicationAdapterException("Validation Failed");
      }
      
      if (isCustomReturn) {
        CloudDataObjectNode fNode = new CloudDataObjectNodeImpl(null, field.getFieldType().getQualifiedName(), field.getFieldType().getObjectCategory(), field.getFieldType().getDataType());
        


        field = new FieldImpl(field.getName(), fNode, field.isArray(), field.isRequired(), field.isNullAllowed());
      }
      

    }
    else
    {
      field = CloudUtil.getIDField("http://api.zuora.com");
    }
    

    return field;
  }
  
  public List<TypeMapping> getResponseTypeMappings()
    throws CloudApplicationAdapterException
  {
    CloudDataObjectNode tmNode = null;
    TypeMapping tm = null;
    List<TypeMapping> tmList = new ArrayList();
    
    String serviceTargetNS = this.serviceTargetNameSpace;
    List<CloudOperationNode> nodeList = adapterPluginContext.getCloudApplicationModel().findOperations(adapterPluginContext.getContextObject("cloudOperation").toString());
    



    CloudDataObjectNode responseNode = ((CloudOperationNode)nodeList.get(0)).getResponse().getResponseObject();
    
    Set<Field> responseFields = null;
    try {
      responseFields = responseNode.getFields();
    } catch (CloudApplicationAdapterException e) {
      List<ValidationKeyword> keywords = new ArrayList();
      CloudValidationError cloudError = new CloudValidationError("OP_METADATA_ERROR_TITLE", keywords);
      
      this.errorList.add(cloudError);
      throw new CloudApplicationAdapterException("Validation Failed");
    }
    
    for (Field responseField : responseFields) {
      CloudDataObjectNode responseFieldTypeNode = responseField.getFieldType();
      
      if (responseField.getFieldType().getName().endsWith("esult")) {
        Set<Field> responseInnerFields = null;
        try {
          responseInnerFields = responseField.getFieldType().getFields();
        }
        catch (CloudApplicationAdapterException e) {
          List<ValidationKeyword> keywords = new ArrayList();
          CloudValidationError cloudError = new CloudValidationError("OP_METADATA_ERROR_TITLE", keywords);
          
          this.errorList.add(cloudError);
          throw new CloudApplicationAdapterException("Validation Failed");
        }
        
        tmNode = new CloudDataObjectNodeImpl(null, new QName(serviceTargetNS, "QueryResults"), responseFieldTypeNode.getObjectCategory(), responseFieldTypeNode.getDataType());
        





        tm = new TypeMapping(tmNode);
        for (Field responseInnerField : responseInnerFields)
        {

          if ((responseInnerField.getFieldType().getName().equalsIgnoreCase("zObject")) && (null != this.queryModel.getMainQDrivingObject()))
          {




            Set<Field> fields = null;
            CloudDataObjectNode recordsNode = new CloudDataObjectNodeImpl(null, new QName(serviceTargetNS, this.queryModel.getMainQDrivingObject().getName() + "s"), responseInnerField.getFieldType().getObjectCategory(), responseInnerField.getFieldType().getDataType());
            









            CloudDataObjectNode dataNode = this.queryModel.getMainQDrivingObject();
            
            Set<Field> appendedFields = null;
            if ((this.queryModel != null) && (this.queryModel.getQueryItems().size() > 0))
            {
              fields = this.queryModel.getQueryItemByObjectName(dataNode.getName()).getCustomObjectNode().getFields();
              


              if ((fields != null) && (fields.size() > 0)) {
                appendedFields = fields;
              }
            }
            if ((appendedFields == null) || (appendedFields.size() < 1))
            {
              try {
                recordsNode.addFields(dataNode.getFields());
              }
              catch (CloudApplicationAdapterException e) {}
            } else {
              recordsNode.addFields(appendedFields);
              recordsNode = addIDFieldToEachSObject(recordsNode);
            }
            


















            Field recordsField = new FieldImpl(responseInnerField.getName(), recordsNode, true, false, true);
            

            TypeMask mask1 = new TypeMask();
            Set<Field> rescordSet = new HashSet();
            rescordSet.add(recordsField);
            mask1.setAppendedFields(rescordSet);
            tm.setMask(mask1);
          }
          else {
            tmNode.addField(responseInnerField);
          }
        }
        tmList.add(tm);
      }
    }
    return tmList;
  }

  private void processTables(String queryString)
    throws CloudApplicationAdapterException
  {
    List<String[]> tablesPropList = getTablesPropList(queryString);
    processTablesPropList(tablesPropList);
  }
  
  private void processTablesPropList(List<String[]> tablesPropList) throws CloudApplicationAdapterException
  {
    if (!this.qType.equalsIgnoreCase("mainQuery")) {
      for (String[] tablePropArr : tablesPropList) {
        String absRelationshipString = tablePropArr[0];
        String relRelationshipString = tablePropArr[0];
        String objectName = tablePropArr[1];
        String alias = tablePropArr[2];
        String tmpLeadStr = "";
        boolean isCurDriving = CloudUtil.returnBooleanValue(tablePropArr[3]);
        
        boolean isUserDefinedAlias = CloudUtil.returnBooleanValue(tablePropArr[4]);
        
        boolean ok = CloudAdapterUtils.isNCName(alias);
        if (!ok) {
          List<ValidationKeyword> keywords = new ArrayList();
          keywords.add(new ValidationKeyword(alias, true));
          CloudValidationError cloudError = new CloudValidationError("OP_ALIAS_NAME_NOT_ACC_TO_NCNAME", keywords);
          
          this.errorList.add(cloudError);
          alias = null;
          throw new CloudApplicationAdapterException("Validation Failed");
        }
        
        if ((relRelationshipString != null) && (!relRelationshipString.equalsIgnoreCase("")))
        {
          tmpLeadStr = getLeadingString(relRelationshipString);
          if ((isCurDriving) && (!tmpLeadStr.equalsIgnoreCase(this.queryModel.getMainQDrivingObject().getName().toLowerCase())) && (!tmpLeadStr.equalsIgnoreCase(mainObjAlias)))
          {



            List<ValidationKeyword> keywords = new ArrayList();
            keywords.add(new ValidationKeyword(relRelationshipString + objectName, true));
            
            CloudValidationError cloudError = new CloudValidationError("OP_INVALID_INNER_DRIVING_OBJECT", keywords);
            
            this.errorList.add(cloudError);
            throw new CloudApplicationAdapterException("Validation Failed");
          }
          if ((!isCurDriving) && (!tmpLeadStr.equalsIgnoreCase(this.queryModel.getCurQDrivingObject().getName().toLowerCase())) && (!tmpLeadStr.equalsIgnoreCase(CloudUtil.getSingularObjectName(this.queryModel.getCurQDrivingObject().getName()).toLowerCase())) && (!tmpLeadStr.equalsIgnoreCase(curObjAlias)))
          {







            QueryItemMetadata tmpQItem = this.queryModel.getTablesMetadataByObjectName(tmpLeadStr);
            
            if (null != tmpQItem) {
              absRelationshipString = getAbsRelationship(tmpQItem);
            } else {
              QueryItemMetadata qItem = this.queryModel.getQueryItemByObjectName(this.queryModel.getCurQDrivingObject().getName());
              

              String curObjAliasFromModel = qItem.isUserDefinedAlias() ? qItem.getAlias() : qItem.getObjectName();
              

              if (!absRelationshipString.startsWith(curObjAliasFromModel))
              {
                absRelationshipString = curObjAliasFromModel + "." + absRelationshipString;
              }
            }
          }
        }

        String validatedName = "";
        CloudDataObjectNode cdonode = null;
        Field tableNodeField = null;
        CloudDataObjectNode validationObject = null;
        if (isCurDriving) {
          validationObject = this.queryModel.getMainQDrivingObject();
          tableNodeField = validateField(validationObject, objectName, true, 0);
        }
        else {
          if ((!tmpLeadStr.equalsIgnoreCase(this.queryModel.getCurQDrivingObject().getName().toLowerCase())) && (!tmpLeadStr.equalsIgnoreCase(CloudUtil.getSingularObjectName(this.queryModel.getCurQDrivingObject().getName()).toLowerCase())) && (!tmpLeadStr.equalsIgnoreCase(curObjAlias)))
          {
            QueryItemMetadata qItem = this.queryModel.getTablesMetadataByObjectName(tmpLeadStr);
            
            if (qItem == null) {
              validationObject = getModelFinalChildNodeOfRelString(absRelationshipString);
            } else {
              validationObject = qItem.getModelObjectNode();
            }
          }
          else {
            validationObject = this.queryModel.getCurQDrivingObject();
          }
          tableNodeField = processTablesDotRelations(relRelationshipString + objectName);
        }
        

        validatedName = tableNodeField.getName();
        String relatedDataObjName = "";
        int relCheckLvl = 2;
        if (isCurDriving) {
          relatedDataObjName = CloudUtil.getTypeOfObjectRelationship(validatedName, validationObject.getName(), 0, adapterPluginContext);
          relCheckLvl = 0;
        } else {
          relatedDataObjName = CloudUtil.getTypeOfObjectRelationship(validatedName, validationObject.getName(), 2, adapterPluginContext);
        }
        cdonode = validateObject(relatedDataObjName, false);
        if (cdonode == null) {
          if (validationObject.getName().toLowerCase().endsWith("__c"))
          {
            cdonode = validateObject(validationObject.getName().substring(0, validationObject.getName().length() - 1) + relatedDataObjName, false);
          }
          else if ((validationObject.getName().equalsIgnoreCase("opportunity")) && (relatedDataObjName.equalsIgnoreCase("history")))
          {
            cdonode = validateObject("OpportunityFieldHistory", false);
          }
          else
          {
            cdonode = validateObject(validationObject.getName() + relatedDataObjName, false);
          }
        }

        if (cdonode == null) {
          Field validatedField = validateField(validationObject, relatedDataObjName, true, relCheckLvl);
          
          cdonode = validatedField.getFieldType();
        }
        QueryItemMetadata tableMetadata = new QueryItemMetadata(validatedName, alias, isUserDefinedAlias, getBlankReplica(cdonode), cdonode, relRelationshipString, absRelationshipString);
        


        this.queryModel.addTableMetadata(tableMetadata);
        if (curQDrivingObjName.equalsIgnoreCase(validatedName))
        {
          this.queryModel.setCurQDrivingObject(cdonode);
          this.actualDrivingNameOfInnerQMap.put(this.qType, validatedName);
          this.queryModel.addQueryItem(tableMetadata);
        }
      }
      if (this.innerQModelsMap == null) {
        this.innerQModelsMap = new HashMap();
      }
      this.innerQModelsMap.put(this.qType, this.queryModel);
    } else {
      for (String[] tablePropArr : tablesPropList) {
        String relRelationshipString = tablePropArr[0];
        String absRelationshipString = tablePropArr[0];
        String objectName = tablePropArr[1];
        String alias = tablePropArr[2];
        String tmpLeadStr = "";
        boolean isCurDriving = CloudUtil.returnBooleanValue(tablePropArr[3]);
        
        boolean isUserDefinedAlias = CloudUtil.returnBooleanValue(tablePropArr[4]);
        

        boolean ok = CloudAdapterUtils.isNCName(alias);
        if (!ok) {
          List<ValidationKeyword> keywords = new ArrayList();
          keywords.add(new ValidationKeyword(alias, true));
          CloudValidationError cloudError = new CloudValidationError("OP_ALIAS_NAME_NOT_ACC_TO_NCNAME", keywords);
          
          this.errorList.add(cloudError);
          alias = null;
          throw new CloudApplicationAdapterException("Validation Failed");
        }
        
        CloudDataObjectNode cdonode = null;
        if ((relRelationshipString != null) && (!relRelationshipString.equalsIgnoreCase("")))
        {
          tmpLeadStr = getLeadingString(relRelationshipString);
          if ((isCurDriving) && (!tmpLeadStr.equalsIgnoreCase(this.queryModel.getMainQDrivingObject().getName().toLowerCase())) && (!tmpLeadStr.equalsIgnoreCase(mainObjAlias)))
          {



            List<ValidationKeyword> keywords = new ArrayList();
            keywords.add(new ValidationKeyword(relRelationshipString + objectName, true));
            
            CloudValidationError cloudError = new CloudValidationError("OP_INVALID_INNER_DRIVING_OBJECT", keywords);
            
            this.errorList.add(cloudError);
            throw new CloudApplicationAdapterException("Validation Failed");
          }
          if ((!isCurDriving) && (!tmpLeadStr.equalsIgnoreCase(this.queryModel.getCurQDrivingObject().getName().toLowerCase())) && (!tmpLeadStr.equalsIgnoreCase(CloudUtil.getSingularObjectName(this.queryModel.getCurQDrivingObject().getName()).toLowerCase())) && (!tmpLeadStr.equalsIgnoreCase(curObjAlias)))
          {








            QueryItemMetadata tmpQItem = this.queryModel.getTablesMetadataByObjectName(tmpLeadStr);
            
            if (null == tmpQItem)
            {
              absRelationshipString = this.queryModel.getQueryItemByObjectName(this.queryModel.getCurQDrivingObject().getName()).getAlias() + "." + absRelationshipString;







            }
            else
            {






              absRelationshipString = getAbsRelationship(tmpQItem);
            }
          }
          Field returnField = processTablesDotRelations(relRelationshipString + objectName);
          
          CloudDataObjectNode returnNode = returnField.getFieldType();
          if (!returnNode.getObjectCategory().equals(ObjectCategory.BUILTIN))
          {
            cdonode = returnNode;
            objectName = returnField.getName();
          } else {
            List<ValidationKeyword> keywords = new ArrayList();
            keywords.add(new ValidationKeyword(relRelationshipString + objectName, true));
            
            CloudValidationError cloudError = new CloudValidationError("OP_INVALID_RELATED_TABLE", keywords);
            
            this.errorList.add(cloudError);
            throw new CloudApplicationAdapterException("Validation Failed");
          }
        }
        else {
          cdonode = validateObject(objectName, true);
          if (curQDrivingObjName.equalsIgnoreCase(cdonode.getName())) {
            this.queryModel.setCurQDrivingObject(cdonode);
          }
          objectName = cdonode.getName();
        }
        CloudDataObjectNode customObj = getBlankReplica(cdonode);
        QueryItemMetadata tableMetadata = new QueryItemMetadata(objectName, alias, isUserDefinedAlias, customObj, cdonode, relRelationshipString, absRelationshipString);
        

        this.queryModel.addTableMetadata(tableMetadata);
        if (((relRelationshipString == null) || (relRelationshipString.equalsIgnoreCase(""))) && (curQDrivingObjName.equalsIgnoreCase(cdonode.getName())))
        {


          this.queryModel.addQueryItem(tableMetadata);
        }
      }
    }
  }
  
  private Field processTablesDotRelations(String fieldStr) throws CloudApplicationAdapterException
  {
    String fieldName = fieldStr.substring(fieldStr.lastIndexOf(".") + 1).trim();
    
    String mainTableName = "";
    fieldStr = fieldStr.substring(0, fieldStr.lastIndexOf(".")).trim();
    String[] relTables = fieldStr.split("\\.");
    Field returnField = null;
    CloudDataObjectNode tableNode = null;
    Field field = null;
    
    CloudDataObjectNode customObject = null;
    Field customField = null;
    for (int i = 0; i < relTables.length; i++) {
      String tableName = relTables[i];
      if (tableNode == null) {
        if (tableName.equalsIgnoreCase(this.queryModel.getCurQDrivingObject().getName()))
        {
          tableNode = this.queryModel.getCurQDrivingObject();
        } else {
          QueryItemMetadata tmpQItem = this.queryModel.getTablesMetadataByObjectName(tableName);
          
          if (null != tmpQItem) {
            tableNode = tmpQItem.getModelObjectNode();
          }
        }
        mainTableName = tableName;
        if (tableNode == null) {
          Field tableNodeField = validateField(this.queryModel.getCurQDrivingObject(), tableName, true, 2);
          

          tableNode = tableNodeField.getFieldType();
        }
      }
      if (i < relTables.length - 1)
      {



        String tableColumn = relTables[(i + 1)];
        field = validateField(tableNode, tableColumn, true, 2);
        tableNode = field.getFieldType();
      } else {
        field = null;
        field = validateField(tableNode, fieldName, true, 2);
        returnField = field;
      }
    }
    return returnField;
  }
  
  private Field processFieldsDotRelations(String p_fieldStr, boolean populateTypeMask, String fieldAlias)
    throws CloudApplicationAdapterException
  {
    String fieldStr = p_fieldStr;
    String caseSensitiveFieldStr = "";
    String fieldName = fieldStr.substring(fieldStr.lastIndexOf(".") + 1).trim();
    
    String mainTableName = "";
    fieldStr = fieldStr.substring(0, fieldStr.lastIndexOf(".")).trim();
    String[] relTables = fieldStr.split("\\.");
    Field returnField = null;
    CloudDataObjectNode tableNode = null;
    CloudDataObjectNode drivingNodeType = null;
    String drivingNodeName = "";
    Field field = null;
    
    CloudDataObjectNode customObject = null;
    Field customField = null;
    boolean dotCheckForCaseSensString = false;
    for (int i = 0; i < relTables.length; i++) {
      String tableName = relTables[i];
      if (tableNode == null)
      {
        if (tableName.equalsIgnoreCase(this.queryModel.getCurQDrivingObject().getName()))
        {
          tableNode = this.queryModel.getCurQDrivingObject();
          QueryItemMetadata queryItem = this.queryModel.getQueryItemByObjectName(tableNode.getName());
          
          if (!this.qType.equalsIgnoreCase("mainQuery")) {
            caseSensitiveFieldStr = caseSensitiveFieldStr + CloudUtil.getSingularObjectName(queryItem.getObjectName());
          }
          else
          {
            caseSensitiveFieldStr = caseSensitiveFieldStr + queryItem.getObjectName();
          }
          
          dotCheckForCaseSensString = true;
        } else {
          QueryItemMetadata tmpQItem = this.queryModel.getTablesMetadataByObjectName(tableName);
          
          if (null != tmpQItem) {
            tableNode = tmpQItem.getModelObjectNode();
            caseSensitiveFieldStr = caseSensitiveFieldStr + tmpQItem.getObjectName();
            
            dotCheckForCaseSensString = true;
          }
        }
        mainTableName = tableName;
        if (tableNode == null) {
          Field tableNodeField = validateField(this.queryModel.getCurQDrivingObject(), tableName, true, 2);
          

          tableNode = tableNodeField.getFieldType();
          caseSensitiveFieldStr = caseSensitiveFieldStr + tableNodeField.getName();
          
          dotCheckForCaseSensString = true;
        }
        drivingNodeName = tableNode.getName();
        if ((tableName.equalsIgnoreCase("who")) || (tableName.equalsIgnoreCase("what")))
        {
          drivingNodeName = tableName;
        }
        drivingNodeType = new CloudDataObjectNodeImpl(null, new QName(curNS, drivingNodeName), tableNode.getObjectCategory(), tableNode.getDataType());
      }
      if (i < relTables.length - 1)
      {
        String tableColumn = relTables[(i + 1)];
        field = validateField(tableNode, tableColumn, true, 2);
        
        String customObjName = field.getName() + field.getFieldType().getName();
        
        customObjName = customObjName.toLowerCase();
        if (!this.usedTypeNames.add(customObjName)) {
          customObjName = field.getName() + this.nameCounter + field.getFieldType().getName();
          
          this.nameCounter += 1;
        }
        if (customField == null) {
          customObject = new CloudDataObjectNodeImpl(null, new QName(curNS, customObjName), ObjectCategory.CUSTOM, DataType.OBJECT);
          customField = new FieldImpl(field.getName(), customObject, false, true, false);
        }
        else {
          CloudDataObjectNode newCustomObject = new CloudDataObjectNodeImpl(null, new QName(curNS, customObjName), ObjectCategory.CUSTOM, DataType.OBJECT);
          Field newCustomField = new FieldImpl(field.getName(), newCustomObject, false, true, false);
          customObject.addField(newCustomField);
          customObject = newCustomObject;
        }
        
        tableNode = field.getFieldType();
        if (dotCheckForCaseSensString) {
          caseSensitiveFieldStr = caseSensitiveFieldStr + ".";
        }
        caseSensitiveFieldStr = caseSensitiveFieldStr + field.getName();
        dotCheckForCaseSensString = true;
      } else {
        field = null;
        if ((drivingNodeName.equalsIgnoreCase("who")) || (drivingNodeName.equalsIgnoreCase("what")))
        {
          if (fieldName.equalsIgnoreCase("id")) {
            field = CloudUtil.getIDField("http://api.zuora.com");
          }
          else {
            field = new FieldImpl(StringUtils.capitalize(fieldName), new CloudDataObjectNodeImpl(null, new QName("http://www.w3.org/2001/XMLSchema", "string"), ObjectCategory.BUILTIN, DataType.STRING), false, false, true);

          }
          


        }
        else
        {
          field = validateField(tableNode, fieldName, true, 2);
        }
        returnField = field;
      }
    }

    String absRelationshipString = "";
    String tmpLeadStr = getLeadingString(p_fieldStr);
    if ((tmpLeadStr.equalsIgnoreCase(this.queryModel.getCurQDrivingObject().getName())) || (tmpLeadStr.equalsIgnoreCase(CloudUtil.getSingularObjectName(this.queryModel.getCurQDrivingObject().getName()))) || (tmpLeadStr.equalsIgnoreCase(curObjAlias)) || (tmpLeadStr.equalsIgnoreCase(CloudUtil.getSingularObjectName(curObjAlias))))
    {
      absRelationshipString = caseSensitiveFieldStr;
    } else {
      QueryItemMetadata tmpQItem = this.queryModel.getTablesMetadataByObjectName(tmpLeadStr);
      
      if (null == tmpQItem)
      {
        Field tmpField = validateField(this.queryModel.getCurQDrivingObject(), tmpLeadStr, true, 2);
        String fieldParentName = this.queryModel.getCurQDrivingObject().getName();
        tmpQItem = this.queryModel.getQueryItemByObjectName(fieldParentName);
        absRelationshipString = tmpQItem.getObjectName() + "." + caseSensitiveFieldStr;
      }
      else {
        absRelationshipString = getAbsRelationship(tmpQItem);
        if (caseSensitiveFieldStr.contains(".")) {
          caseSensitiveFieldStr = caseSensitiveFieldStr.substring(caseSensitiveFieldStr.indexOf(".") + 1);
          
          absRelationshipString = absRelationshipString + caseSensitiveFieldStr;
        }
      }
    }
    

    addFieldToQueryModel(absRelationshipString, field, fieldAlias);
    
    return returnField;
  }
  
  private void addFieldToQueryModel(String location, Field field, String fieldAlias) throws CloudApplicationAdapterException
  {
    if (!field.getName().equalsIgnoreCase(fieldAlias)) {
      field = new FieldImpl(fieldAlias, field.getFieldType(), field.isArray(), field.isRequired(), field.isNullAllowed());
    }
    
    if ((null != location) && (!location.equalsIgnoreCase(""))) {
      CloudDataObjectNode fieldsParentNode = getCustomFinalChildNodeOfRelString(location);
      if ((null != fieldsParentNode) && 
        (!fieldsParentNode.addField(field))) {
        Field mergedField = null;
        Field toRemoveField = null;
        Iterator<Field> entries = fieldsParentNode.getFields().iterator();
        
        while (entries.hasNext()) {
          Field itField = (Field)entries.next();
          if (field.getName().equalsIgnoreCase(itField.getName())) {
            mergedField = CloudUtil.mergeFields(field, itField, curNS, adapterPluginContext);
            
            toRemoveField = itField;
            break;
          }
        }
        if ((toRemoveField != null) && (mergedField != null)) {
          fieldsParentNode.removeField(toRemoveField);
          fieldsParentNode.addField(mergedField);
        }
      }
    }
    else {
      String fieldParentName = this.queryModel.getCurQDrivingObject().getName();
      
      QueryItemMetadata queryItem = this.queryModel.getQueryItemByObjectName(fieldParentName);
      

      queryItem.getCustomObjectNode().addField(field);
    }
  }
  
  private List<String[]> getTablesPropList(String queryString) throws CloudApplicationAdapterException
  {
    List<String[]> tablesPropList = new ArrayList();
    String tablesStr = extractTableNamesFromQuery(queryString);
    String[] splitMultipleTables = tablesStr.split(",");
    boolean yetToSetDrivingSObject = true;
    curObjAlias = "";
    if (splitMultipleTables.length < 1) {
      List<ValidationKeyword> keywords = new ArrayList();
      CloudValidationError cloudError = new CloudValidationError("OP_QUERY_MISSING_TABLE", keywords);
      
      this.errorList.add(cloudError);
      throw new CloudApplicationAdapterException("Validation Failed");
    }
    
    for (int i = 0; i < splitMultipleTables.length; i++) {
      String relationShipString = "";
      String isCurDriving = "false";
      String isAliasUserSupplied = "false";
      String[] tableSplits = splitMultipleTables[i].trim().split(" ");
      String tableName = tableSplits[0].trim();
      String aliasName = "";
      
      if (tableSplits.length > 1) {
        aliasName = tableSplits[1].trim();
        if (!this.usedTableAliasNames.add(aliasName)) {
          List<ValidationKeyword> keywords = new ArrayList();
          keywords.add(new ValidationKeyword(aliasName, true));
          CloudValidationError cloudError = new CloudValidationError("OP_DUPLICATE_ALIAS", keywords);
          
          this.errorList.add(cloudError);
          throw new CloudApplicationAdapterException("Validation Failed");
        }
        
        isAliasUserSupplied = "true";
      } else {
        aliasName = tableSplits[0].trim();
        if (aliasName.contains(".")) {
          aliasName = aliasName.substring(aliasName.lastIndexOf(".") + 1, aliasName.length());
        }
      }
      

      if (tableName.contains("."))
      {
        relationShipString = tableName.substring(0, tableName.lastIndexOf(".") + 1);
        
        tableName = tableName.substring(tableName.lastIndexOf(".") + 1, tableName.length());
        
        if (yetToSetDrivingSObject) {
          yetToSetDrivingSObject = false;
          isCurDriving = "true";
          curQDrivingObjName = tableName;
          curObjAlias = aliasName;
        }
      }
      else if (yetToSetDrivingSObject) {
        yetToSetDrivingSObject = false;
        isCurDriving = "true";
        curQDrivingObjName = tableName;
        curObjAlias = aliasName;
        

        if (this.qType.equalsIgnoreCase("mainQuery")) {
          CloudDataObjectNode tmpNode = validateObject(curQDrivingObjName, true);
          
          if (curQDrivingObjName.equalsIgnoreCase(tmpNode.getName()))
          {

            this.queryModel.setMainQDrivingObject(tmpNode);
          }
        }
      } else {
        List<ValidationKeyword> keywords = new ArrayList();
        keywords.add(new ValidationKeyword(tableName, true));
        keywords.add(new ValidationKeyword(curQDrivingObjName, false));
        
        CloudValidationError cloudError = new CloudValidationError("OP_INVALID_MULTI_DRIVING_OBJECT_ERROR", keywords);
        

        this.errorList.add(cloudError);
        throw new CloudApplicationAdapterException("Validation Failed");
      }
      String[] tablePropArr = new String[5];
      tablePropArr[0] = relationShipString;
      tablePropArr[1] = tableName;
      tablePropArr[2] = aliasName;
      tablePropArr[3] = isCurDriving;
      tablePropArr[4] = isAliasUserSupplied;
      tablesPropList.add(tablePropArr);
    }
    
    return tablesPropList;
  }

  private CloudQueryModel buildQueryModelForPolyQuery(String queryStatement)
    throws CloudApplicationAdapterException
  {
    Map<String, String> queryMap = getPolyRelQueriesMap(queryStatement);
    if ((queryMap != null) && (queryMap.containsKey("mainQuery"))) {
      String mainQuery = (String)queryMap.get("mainQuery");
      
      String drivingTableName = "";
      String curDrivingObjName = "";
      CloudDataObjectNode curDrivingObjNode = null;
      String resultTypeName = "";
      
      processBasicValidations(mainQuery.toLowerCase());
      checkPolyRelQValidations(mainQuery.toLowerCase());
      

      mainQuery = mainQuery.replace(",", " , ");
      String[] tokens = mainQuery.split(" ");
      List<String> fields = new ArrayList();
      boolean storeFields = false;
      String prevKeyWord = "";
      boolean typeOfFound = false;
      String tablesString = "";
      for (int i = 0; i < tokens.length; i++) {
        String token = tokens[i];
        String tempToken = token;
        token = token.toLowerCase();
        if (token.equalsIgnoreCase(" typeof ".trim()))
        {
          resultTypeName = tokens[(i + 1)];
          if (resultTypeName.equalsIgnoreCase("What"))
          {
            resultTypeName = "What";
          } else if (resultTypeName.equalsIgnoreCase("Who"))
          {
            resultTypeName = "Who";
          } else if (resultTypeName.equalsIgnoreCase("Owner"))
          {
            resultTypeName = "Owner";
          } else {
            List<ValidationKeyword> keywords = new ArrayList();
            CloudValidationError cloudError = new CloudValidationError("OP_QUERY_TYPEOF_WHO_WHAT_OWNER", keywords);
            
            this.errorList.add(cloudError);
            throw new CloudApplicationAdapterException("Validation Failed");
          }
          
          typeOfFound = true;
          prevKeyWord = " typeof ".trim();
        }
        if (token.equalsIgnoreCase(" end ".trim()))
        {
          if (!typeOfFound) {
            List<ValidationKeyword> keywords = new ArrayList();
            keywords.add(new ValidationKeyword("TYPEOF", false));
            CloudValidationError cloudError = new CloudValidationError("OP_QUERY_MISSING_KEYWORD", keywords);
            
            this.errorList.add(cloudError);
            throw new CloudApplicationAdapterException("Validation Failed");
          }
          
          prevKeyWord = " end ".trim();
          storeFields = false;
        }
        
        if ((storeFields) && 
          (!token.equalsIgnoreCase(" when ".trim())) && (!token.equalsIgnoreCase(" else ".trim())) && (!token.equalsIgnoreCase(" end ".trim())) && (!token.equalsIgnoreCase(" then ".trim())))
        {










          String field = tempToken;
          if (((field.trim().equals(",")) || (field.contains(",")) || (tokens[(i - 1)].contains(","))) && 
          

            (field.trim().equals(","))) {
            continue;
          }
          if (field.contains(",")) {
            field = field.replaceAll(",", "");
          }
          
          if (!prevKeyWord.equalsIgnoreCase(" else ".trim()))
          {

            Field valField = validateField(curDrivingObjNode, field, true, 2);
            
            fields.add(valField.getName());
          } else {
            fields.add(field);
          }
        }
        


        if (token.equalsIgnoreCase(" when ".trim()))
        {
          if (!typeOfFound) {
            List<ValidationKeyword> keywords = new ArrayList();
            keywords.add(new ValidationKeyword("TYPEOF", false));
            CloudValidationError cloudError = new CloudValidationError("OP_QUERY_MISSING_KEYWORD", keywords);
            
            this.errorList.add(cloudError);
            throw new CloudApplicationAdapterException("Validation Failed");
          }
          
          if ((prevKeyWord.equalsIgnoreCase(" when ".trim())) || (prevKeyWord.equalsIgnoreCase(" else ".trim())))
          {




            List<ValidationKeyword> keywords = new ArrayList();
            keywords.add(new ValidationKeyword("THEN", false));
            CloudValidationError cloudError = new CloudValidationError("OP_QUERY_MISSING_KEYWORD", keywords);
            
            this.errorList.add(cloudError);
            throw new CloudApplicationAdapterException("Validation Failed");
          }
          
          storeFields = false;
          curDrivingObjName = tokens[(i + 1)];
          if (!tokens[(i + 2)].equalsIgnoreCase(" then ".trim()))
          {

            List<ValidationKeyword> keywords = new ArrayList();
            keywords.add(new ValidationKeyword(tokens[(i + 2)], true));
            
            CloudValidationError cloudError = new CloudValidationError("OP_QUERY_INVALID_TOKEN_FOUND", keywords);
            
            this.errorList.add(cloudError);
            tablesString = "";
            throw new CloudApplicationAdapterException("Validation Failed");
          }
          
          curDrivingObjNode = null;
          tablesString = "";
          if (curDrivingObjName.contains(",")) {
            List<ValidationKeyword> keywords = new ArrayList();
            keywords.add(new ValidationKeyword(",", true));
            CloudValidationError cloudError = new CloudValidationError("OP_QUERY_INVALID_TOKEN_FOUND", keywords);
            
            this.errorList.add(cloudError);
            tablesString = "";
            throw new CloudApplicationAdapterException("Validation Failed");
          }
          
          curDrivingObjNode = validateObject(curDrivingObjName, true);
          prevKeyWord = " when ".trim();
        }
        
        if (token.equalsIgnoreCase(" else ".trim()))
        {
          if (!typeOfFound) {
            List<ValidationKeyword> keywords = new ArrayList();
            keywords.add(new ValidationKeyword("TYPEOF", false));
            CloudValidationError cloudError = new CloudValidationError("OP_QUERY_MISSING_KEYWORD", keywords);
            
            this.errorList.add(cloudError);
            throw new CloudApplicationAdapterException("Validation Failed");
          }
          
          if (prevKeyWord.equalsIgnoreCase(" when ".trim()))
          {

            List<ValidationKeyword> keywords = new ArrayList();
            keywords.add(new ValidationKeyword("THEN", false));
            CloudValidationError cloudError = new CloudValidationError("OP_QUERY_MISSING_KEYWORD", keywords);
            
            this.errorList.add(cloudError);
            throw new CloudApplicationAdapterException("Validation Failed");
          }
          
          if (prevKeyWord.equalsIgnoreCase(" else ".trim()))
          {

            List<ValidationKeyword> keywords = new ArrayList();
            keywords.add(new ValidationKeyword(token, true));
            CloudValidationError cloudError = new CloudValidationError("OP_QUERY_INVALID_TOKEN_FOUND", keywords);
            
            this.errorList.add(cloudError);
            throw new CloudApplicationAdapterException("Validation Failed");
          }
          

          storeFields = true;
          prevKeyWord = " else ".trim();
        }
        if (token.equalsIgnoreCase(" then ".trim()))
        {
          if (!typeOfFound) {
            List<ValidationKeyword> keywords = new ArrayList();
            keywords.add(new ValidationKeyword("TYPEOF", false));
            CloudValidationError cloudError = new CloudValidationError("OP_QUERY_MISSING_KEYWORD", keywords);
            
            this.errorList.add(cloudError);
            throw new CloudApplicationAdapterException("Validation Failed");
          }
          
          if (!prevKeyWord.equalsIgnoreCase(" when ".trim()))
          {

            List<ValidationKeyword> keywords = new ArrayList();
            keywords.add(new ValidationKeyword("WHEN", false));
            CloudValidationError cloudError = new CloudValidationError("OP_QUERY_MISSING_KEYWORD", keywords);
            
            this.errorList.add(cloudError);
            throw new CloudApplicationAdapterException("Validation Failed");
          }
          
          storeFields = true;
          prevKeyWord = " then ".trim();
        }
        
        if (token.equalsIgnoreCase("from")) {
          if (!typeOfFound) {
            List<ValidationKeyword> keywords = new ArrayList();
            keywords.add(new ValidationKeyword("TYPEOF", false));
            CloudValidationError cloudError = new CloudValidationError("OP_QUERY_MISSING_KEYWORD", keywords);
            
            this.errorList.add(cloudError);
            throw new CloudApplicationAdapterException("Validation Failed");
          }
          
          if (!prevKeyWord.equalsIgnoreCase(" end ".trim()))
          {

            List<ValidationKeyword> keywords = new ArrayList();
            keywords.add(new ValidationKeyword(token, true));
            CloudValidationError cloudError = new CloudValidationError("OP_QUERY_END_FROM_ERROR", keywords);
            
            this.errorList.add(cloudError);
            throw new CloudApplicationAdapterException("Validation Failed");
          }
          
          drivingTableName = tokens[(i + 1)];
          prevKeyWord = "from";
          break;
        }
      }
      


















      if ((fields != null) && (fields.size() > 0)) {
        CloudDataObjectNode resultNode = new CloudDataObjectNodeImpl(null, new QName(curNS, drivingTableName), ObjectCategory.STANDARD, DataType.OBJECT);
        

        CloudDataObjectNode polyMorphicNode = new CloudDataObjectNodeImpl(null, new QName(curNS, "PolymorphicObject"), ObjectCategory.STANDARD, DataType.OBJECT);
        

        for (String fieldName : fields) {
          Field field = null;
          if (fieldName.equalsIgnoreCase("id")) {
            field = CloudUtil.getIDField("http://api.zuora.com");
          }
          else {
            field = new FieldImpl(StringUtils.capitalize(fieldName), new CloudDataObjectNodeImpl(null, new QName("http://www.w3.org/2001/XMLSchema", "string"), ObjectCategory.BUILTIN, DataType.STRING), false, false, true);
          }
          




          polyMorphicNode.addField(field);
        }
        Field polyField = new FieldImpl(resultTypeName, polyMorphicNode, false, false, true);
        


        Map<String, Field> resFields = new HashMap();
        resFields.put(polyField.getName(), polyField);
        CloudDataObjectNode customNodeReplica = getBlankReplica(resultNode);
        customNodeReplica.addField(polyField);
        QueryItemMetadata queryItem = new QueryItemMetadata("", resultNode.getName(), "", false, customNodeReplica, resultNode);
        

        this.queryModel.setMainQDrivingObject(resultNode);
        this.queryModel.addQueryItem(queryItem);
      }
    }
    

    return this.queryModel;
  }
  
  private void checkPolyRelQValidations(String queryStatement)
    throws CloudApplicationAdapterException
  {
    if (!queryStatement.toLowerCase().contains(" when "))
    {
      List<ValidationKeyword> keywords = new ArrayList();
      CloudValidationError cloudError = new CloudValidationError("OP_INVALID_WHEN_ERROR", keywords);
      
      this.errorList.add(cloudError);
      throw new CloudApplicationAdapterException("Validation Failed");
    }
    










    if (!queryStatement.toLowerCase().contains(" end "))
    {
      List<ValidationKeyword> keywords = new ArrayList();
      CloudValidationError cloudError = new CloudValidationError("OP_INVALID_END_ERROR", keywords);
      
      this.errorList.add(cloudError);
      throw new CloudApplicationAdapterException("Validation Failed");
    }
  }
  
  private Map<String, String> getPolyRelQueriesMap(String queryString)
    throws CloudApplicationAdapterException
  {
    queryString = queryString.toLowerCase();
    Map<String, String> queryMap = new HashMap();
    
    String patternIn = "\\(\\s*.*?\\s*\\)";
    
    Pattern patternInner = Pattern.compile(patternIn);
    Pattern patternCommaComma = Pattern.compile("\\,\\s*,");
    Pattern patternBlankCount = Pattern.compile("count\\(\\s*\\)", 2);
    

    Matcher matchInner = patternInner.matcher(queryString.toLowerCase());
    
    boolean success = matchInner.find();
    int counter = 0;
    while (success) {
      String innerQuery = matchInner.group(0);
      String tmpStr = innerQuery;
      innerQuery = innerQuery.substring(innerQuery.indexOf("(") + 1);
      innerQuery = innerQuery.substring(0, innerQuery.lastIndexOf(")"));
      if (innerQuery.contains("(")) {
        int bracCloseIndex = findClosingBracketIndex(queryString.indexOf(tmpStr), queryString);
        
        innerQuery = queryString.substring(queryString.indexOf(tmpStr), bracCloseIndex + 1);
        
        queryString = queryString.toLowerCase().replace(innerQuery, "innerquery" + counter);
        
        innerQuery = innerQuery.substring(innerQuery.indexOf("(") + 1);
        innerQuery = innerQuery.substring(0, innerQuery.lastIndexOf(")"));
        
        Matcher matchBlankCount = patternBlankCount.matcher(innerQuery.toLowerCase());
        
        if (matchBlankCount.find()) {
          List<ValidationKeyword> keywords = new ArrayList();
          CloudValidationError cloudError = new CloudValidationError("OP_INNER_QUERY_COUNT", keywords);
          
          this.errorList.add(cloudError);
          throw new CloudApplicationAdapterException("Validation Failed");
        }
        

        String btwSelectFrom = extractStringBtwSelectFrom(innerQuery);
        if (btwSelectFrom.contains("(")) {
          int innerBracCloseIndex = findClosingBracketIndex(btwSelectFrom.indexOf("("), btwSelectFrom);
          
          btwSelectFrom = btwSelectFrom.substring(btwSelectFrom.indexOf("(") + 1, innerBracCloseIndex);
          

          if (btwSelectFrom.toLowerCase().contains("select "))
          {
            List<ValidationKeyword> keywords = new ArrayList();
            CloudValidationError cloudError = new CloudValidationError("OP_INNER_QUERY_MAX_LEVEL", keywords);
            
            this.errorList.add(cloudError);
            throw new CloudApplicationAdapterException("Validation Failed");
          }
          
          List<ValidationKeyword> keywords = new ArrayList();
          CloudValidationError cloudError = new CloudValidationError("OP_INNER_QUERY_AGGR_EXPR", keywords);
          
          this.errorList.add(cloudError);
          throw new CloudApplicationAdapterException("Validation Failed");
        }
        

      }
      else
      {
        queryString = queryString.toLowerCase().replace(innerQuery, "innerquery" + counter);
      }
      
      Pattern typeOfPattern = Pattern.compile("\\s*typeof\\s*", 2);
      
      Matcher typeOfMatcher = typeOfPattern.matcher(innerQuery);
      if (typeOfMatcher.find()) {
        List<ValidationKeyword> keywords = new ArrayList();
        CloudValidationError cloudError = new CloudValidationError("OP_INNER_QUERY_TYPEOF", keywords);
        
        this.errorList.add(cloudError);
        throw new CloudApplicationAdapterException("Validation Failed");
      }
      


      innerQuery = innerQuery.trim();
      queryMap.put("innerquery" + counter, innerQuery);
      Matcher matchCommaComma = patternCommaComma.matcher(queryString.toLowerCase());
      
      if (matchCommaComma.find()) {
        String commaStr = matchCommaComma.group(0);
        queryString = queryString.toLowerCase().replace(commaStr, ",");
      }
      try {
        success = matchInner.find();
        counter++;
      } catch (Exception e) {
        e.printStackTrace();
      }
    }
    


    Matcher matchBlankCountMain = patternBlankCount.matcher(queryString.toLowerCase());
    
    if (matchBlankCountMain.find()) {
      String strBetweenSelectAndFrom = extractStringBtwSelectFrom(queryString);
      if (((queryMap != null) && (queryMap.size() > 0)) || (strBetweenSelectAndFrom.contains(",")))
      {
        List<ValidationKeyword> keywords = new ArrayList();
        keywords.add(new ValidationKeyword(",", true));
        CloudValidationError cloudError = new CloudValidationError("OP_QUERY_INVALID_TOKEN_FOUND", keywords);
        
        this.errorList.add(cloudError);
        throw new CloudApplicationAdapterException("Validation Failed");
      }
    }
    else {
      String strBetweenSelectAndFrom = extractStringBtwSelectFrom(queryString);
      if (strBetweenSelectAndFrom.contains("(")) {
        List<ValidationKeyword> keywords = new ArrayList();
        keywords.add(new ValidationKeyword("(", true));
        CloudValidationError cloudError = new CloudValidationError("OP_QUERY_INVALID_TOKEN_FOUND", keywords);
        
        this.errorList.add(cloudError);
        throw new CloudApplicationAdapterException("Validation Failed");
      }
      

      if (strBetweenSelectAndFrom.contains(")")) {
        List<ValidationKeyword> keywords = new ArrayList();
        keywords.add(new ValidationKeyword(")", true));
        CloudValidationError cloudError = new CloudValidationError("OP_QUERY_INVALID_TOKEN_FOUND", keywords);
        
        this.errorList.add(cloudError);
        throw new CloudApplicationAdapterException("Validation Failed");
      }
      

      queryString = queryString.trim();
      queryMap.put("mainQuery", queryString);
    }
    return queryMap;
  }
  
  private Set<Field> mergeFieldsSets(Set<Field> primarySet, Set<Field> secandorySet) throws CloudApplicationAdapterException
  {
    if (primarySet.size() < 1)
      return secandorySet;
    if (secandorySet.size() < 1) {
      return primarySet;
    }
    Iterator<Field> secSetItr = secandorySet.iterator();
    while (secSetItr.hasNext()) {
      Field secTempField = (Field)secSetItr.next();
      if (!primarySet.add(secTempField)) {
        Field mergedField = null;
        Field toRemoveField = null;
        Iterator<Field> priSetItr = primarySet.iterator();
        while (priSetItr.hasNext()) {
          Field priTempField = (Field)priSetItr.next();
          if (priTempField.getName().equalsIgnoreCase(secTempField.getName()))
          {
            mergedField = CloudUtil.mergeFields(priTempField, secTempField, curNS, adapterPluginContext);
            
            toRemoveField = priTempField;
            break;
          }
        }
        if ((toRemoveField != null) && (mergedField != null)) {
          primarySet.remove(toRemoveField);
          primarySet.add(mergedField);
        }
      }
    }
    
    return primarySet;
  }
  
  private String getLeadingString(String p_relationShipString)
  {
    p_relationShipString = p_relationShipString.trim();
    if (!p_relationShipString.contains(".")) {
      return p_relationShipString;
    }
    return p_relationShipString.substring(0, p_relationShipString.indexOf("."));
  }
  

  private String getAbsRelationship(QueryItemMetadata p_qItem)
  {
    return p_qItem.getAbsRelationshipString() + p_qItem.getObjectName() + ".";
  }
  







  private CloudDataObjectNode getModelFinalChildNodeOfRelString(String location)
    throws CloudApplicationAdapterException
  {
    if (location.endsWith(".")) {
      location = location.substring(0, location.lastIndexOf("."));
    }
    CloudDataObjectNode tableNode = null;
    Field field = null;
    String[] relTables = location.split("\\.");
    for (int i = 0; i < relTables.length; i++) {
      String tableName = relTables[i];
      if (tableNode == null) {
        if (tableName.equalsIgnoreCase(this.queryModel.getCurQDrivingObject().getName()))
        {
          tableNode = this.queryModel.getCurQDrivingObject();
        } else {
          QueryItemMetadata tmpQItem = this.queryModel.getTablesMetadataByObjectName(tableName);
          
          if (null != tmpQItem) {
            tableNode = tmpQItem.getModelObjectNode();
          }
        }
        if (tableNode == null) {
          Field tableNodeField = validateField(this.queryModel.getCurQDrivingObject(), tableName, false, 2);
          

          tableNode = tableNodeField.getFieldType();
        }
      }
      if (i < relTables.length - 1) {
        String tableColumn = relTables[(i + 1)];
        field = validateField(tableNode, tableColumn, false, 2);
        tableNode = field.getFieldType();
      }
    }
    return tableNode;
  }
  






  private CloudDataObjectNode getCustomFinalChildNodeOfRelString(String location)
    throws CloudApplicationAdapterException
  {
    if ((null == location) || (location.equalsIgnoreCase(""))) {
      return this.queryModel.getCurQDrivingObject();
    }
    CloudDataObjectNode toBeReturnNode = null;
    String[] relTables = location.trim().split("\\.");
    QueryItemMetadata qItem = this.queryModel.getQueryItemByObjectName(relTables[0]);
    
    toBeReturnNode = qItem.getCustomObjectNode();
    CloudDataObjectNode validationNode = validateObject(qItem.getModelObjectNode().getName(), true);
    
    Set<Field> fieldSet = null;
    try {
      fieldSet = toBeReturnNode.getFields();
    }
    catch (CloudApplicationAdapterException e) {}
    for (int i = 1; i < relTables.length; i++) {
      String tableName = relTables[i];
      
      boolean isFieldFound = false;
      for (Field field : fieldSet) {
        if ((tableName.equalsIgnoreCase(field.getName())) || (tableName.equalsIgnoreCase(field.getFieldType().getName())))
        {
          try
          {
            toBeReturnNode = field.getFieldType();
            validationNode = validateField(validationNode, field.getName(), false, 2).getFieldType();
            
            fieldSet = toBeReturnNode.getFields();
            isFieldFound = true;
          }
          catch (CloudApplicationAdapterException e) {}
        }
      }
      

      if (!isFieldFound) {
        String customObjName = tableName + "1";
        customObjName = customObjName.toLowerCase();
        if (!this.usedTypeNames.add(customObjName)) {
          customObjName = tableName + this.nameCounter;
          this.nameCounter += 1;
        }
        CloudDataObjectNode tmpNode = new CloudDataObjectNodeImpl(null, new QName(curNS, customObjName), ObjectCategory.CUSTOM, DataType.OBJECT);
        


        Field field = validateField(validationNode, tableName, false, 2);
        Field tempField = new FieldImpl(field.getName(), tmpNode, false, false, true);
        
        toBeReturnNode.addField(tempField);
        toBeReturnNode = tmpNode;
        validationNode = field.getFieldType();
        fieldSet = new HashSet();
      }
    }
    

    return toBeReturnNode;
  }
  
  private CloudDataObjectNode getBlankReplica(CloudDataObjectNode p_objNode)
  {
    String customObjName = p_objNode.getName() + "1";
    customObjName = customObjName.toLowerCase();
    if (!this.usedTypeNames.add(customObjName)) {
      customObjName = p_objNode.getName() + this.nameCounter;
      this.nameCounter += 1;
    }
    CloudDataObjectNode toBeReturnNode = new CloudDataObjectNodeImpl(null, new QName(curNS, customObjName), p_objNode.getObjectCategory(), p_objNode.getDataType(), false);
    

    return toBeReturnNode;
  }
  
  private boolean validateFunction(String func) {
    func = func.toLowerCase();
    if ((func.equalsIgnoreCase("tolabel".trim())) || (func.equalsIgnoreCase("convertcurrency".trim())) || (func.equalsIgnoreCase("converttimezone".trim())) || (func.equalsIgnoreCase("convertcurrency".trim())) || (func.equalsIgnoreCase("grouping".trim())) || (validateAgrFunction(func)) || (validateDateFunction(func)))
    {







      return true;
    }
    return false;
  }
  
  private boolean validateAgrFunction(String func) {
    func = func.toLowerCase();
    if ((func.equalsIgnoreCase(" avg".trim())) || (func.equalsIgnoreCase(" count".trim())) || (func.equalsIgnoreCase(" count_distinct".trim())) || (func.equalsIgnoreCase(" min".trim())) || (func.equalsIgnoreCase(" max".trim())) || (func.equalsIgnoreCase(" sum".trim())))
    {






      return true;
    }
    return false;
  }
  
  private boolean validateDateFunction(String func) {
    func = func.toLowerCase();
    if ((func.equalsIgnoreCase("calendar_month".trim())) || (func.equalsIgnoreCase("calendar_quarter".trim())) || (func.equalsIgnoreCase("calendar_year".trim())) || (func.equalsIgnoreCase("day_in_month".trim())) || (func.equalsIgnoreCase("day_in_week".trim())) || (func.equalsIgnoreCase("day_in_year".trim())) || (func.equalsIgnoreCase("day_only".trim())) || (func.equalsIgnoreCase("fiscal_month".trim())) || (func.equalsIgnoreCase("fiscal_quarter".trim())) || (func.equalsIgnoreCase("fiscal_year".trim())) || (func.equalsIgnoreCase("hour_in_day".trim())) || (func.equalsIgnoreCase("week_in_month".trim())) || (func.equalsIgnoreCase("week_in_year".trim())))
    {






















      return true;
    }
    return false;
  }
  
  private int findComma(String fieldsStr, int offsetIndex, int location) throws CloudApplicationAdapterException
  {
    int foundCommaIndex = -1;
    boolean isCommaMissError = false;
    StringBuilder sb = new StringBuilder();
    if (location == -1) {
      for (int i = offsetIndex - 1; i > 0; i--) {
        char c = fieldsStr.charAt(i);
        if (c == ' ') {
          sb.insert(0, c);
          if (isCommaMissError) {
            if (!validateFunction(sb.toString().trim())) break;
            isCommaMissError = false;
          }
          
        }
        else
        {
          if (c == ',') {
            if ((isCommaMissError) && 
              (validateFunction(sb.toString().trim()))) {
              isCommaMissError = false;
            }
            
            sb.insert(0, c);
            foundCommaIndex = i;
            break;
          }
          isCommaMissError = true;
          sb.insert(0, c);
        }
      }
      if (isCommaMissError) {
        String errStr = "";
        String keyword = sb.toString().trim();
        if ((fieldsStr.substring(offsetIndex + 1).trim().startsWith("innerquery")) || (keyword.contains(" ")))
        {

          errStr = "OP_QUERY_MISSING_COMMA";
        } else {
          errStr = "OP_INVALID_AGR_FUNC_ERROR";
        }
        List<ValidationKeyword> keywords = new ArrayList();
        keywords.add(new ValidationKeyword(keyword, true));
        
        CloudValidationError cloudError = new CloudValidationError(errStr, keywords);
        
        this.errorList.add(cloudError);
        throw new CloudApplicationAdapterException("Validation Failed");
      }
    }
    

    return foundCommaIndex;
  }
  
  private boolean isAliasReservedKeyword(String alias) {
    alias = alias.trim();
    if ((alias.equalsIgnoreCase("AND")) || (alias.equalsIgnoreCase("ASC")) || (alias.equalsIgnoreCase("DESC")) || (alias.equalsIgnoreCase("EXCLUDES")) || (alias.equalsIgnoreCase("FIRST")) || (alias.equalsIgnoreCase("FROM")) || (alias.equalsIgnoreCase("GROUP")) || (alias.equalsIgnoreCase("HAVING")) || (alias.equalsIgnoreCase("IN")) || (alias.equalsIgnoreCase("INCLUDES")) || (alias.equalsIgnoreCase("LAST")) || (alias.equalsIgnoreCase("LIKE")) || (alias.equalsIgnoreCase("LIMIT")) || (alias.equalsIgnoreCase("NOT")) || (alias.equalsIgnoreCase("NULL")) || (alias.equalsIgnoreCase("NULLS")) || (alias.equalsIgnoreCase("OR")) || (alias.equalsIgnoreCase("SELECT")) || (alias.equalsIgnoreCase("WHERE")) || (alias.equalsIgnoreCase("WITH")))
    {

















      return true;
    }
    return false;
  }
  
  private boolean checkFieldGrouping(String field)
    throws CloudApplicationAdapterException
  {
    Pattern patternGroupBy = Pattern.compile("\\bgroup\\s*by\\s*" + field.toLowerCase() + "\\b");
    
    Matcher matchGroupBy = patternGroupBy.matcher(this.currentQUnderProcess.toLowerCase());
    
    if (matchGroupBy.find()) {
      return true;
    }
    
    Pattern patternGroupByRollup = Pattern.compile("\\bgroup\\s*by\\s*rollup\\s*\\(\\s*.*?[^A-Z]?\\)");
    
    Matcher matchGroupByRollup = patternGroupByRollup.matcher(this.currentQUnderProcess.toLowerCase());
    
    if (matchGroupByRollup.find()) {
      String grpByRlpStr = matchGroupByRollup.group(0);
      String tmpStr = grpByRlpStr;
      grpByRlpStr = grpByRlpStr.substring(grpByRlpStr.indexOf("(") + 1, grpByRlpStr.lastIndexOf(")"));
      
      String[] grpRlpFields = grpByRlpStr.split(",");
      for (String str : grpRlpFields) {
        if (str.trim().equalsIgnoreCase(field)) {
          return true;
        }
      }
    }
    return false;
  }
  
  private boolean isDrivingAliasUserDefined(List<String[]> tablesPropList) {
    for (String[] tablePropArr : tablesPropList) {
      if (CloudUtil.returnBooleanValue(tablePropArr[3])) {
        return CloudUtil.returnBooleanValue(tablePropArr[4]);
      }
    }
    return false;
  }
  
  private CloudDataObjectNode addIDFieldToEachSObject(CloudDataObjectNode node) throws CloudApplicationAdapterException
  {
    String idFieldName = "id";
    String queryLocatorFieldName = "queryLocator";
    String doneFieldName = "done";
    String sizeFieldName = "size";
    if (node.getName().equalsIgnoreCase(idFieldName)) {
      return node;
    }
    boolean isIDPresentInRcvdSet = false;
    boolean isQueryLocatorPresentInRcvdSet = false;
    boolean isDonePresentInRcvdSet = false;
    boolean isSizePresentInRcvdSet = false;
    Set<Field> fields = new HashSet();
    for (Field field : node.getFields()) {
      if ((!isIDPresentInRcvdSet) && (field.getName().equalsIgnoreCase(idFieldName)))
      {
        isIDPresentInRcvdSet = true; }
      if ((!isQueryLocatorPresentInRcvdSet) && (field.getName().equalsIgnoreCase(queryLocatorFieldName)))
      {
        isQueryLocatorPresentInRcvdSet = true; }
      if ((!isDonePresentInRcvdSet) && (field.getName().equalsIgnoreCase(doneFieldName)))
      {
        isDonePresentInRcvdSet = true; }
      if ((!isSizePresentInRcvdSet) && (field.getName().equalsIgnoreCase(sizeFieldName)))
      {
        isSizePresentInRcvdSet = true;
      }
      if ((!field.getFieldType().getObjectCategory().equals(ObjectCategory.BUILTIN)) && (!field.getName().equalsIgnoreCase(idFieldName)))
      {

        CloudDataObjectNode tempNode = addIDFieldToEachSObject(field.getFieldType());
        
        Field tmpField = new FieldImpl(field.getName(), tempNode, field.isArray(), field.isRequired(), field.isNullAllowed());
        

        fields.add(tmpField);
      } else {
        fields.add(field);
      }
    }
    
    if ((!isIDPresentInRcvdSet) && (!isQueryLocatorPresentInRcvdSet) && (!isDonePresentInRcvdSet) && (!isSizePresentInRcvdSet))
    {
      fields.add(CloudUtil.getIDField("http://api.zuora.com"));
    }
    
    CloudDataObjectNode tempNode = new CloudDataObjectNodeImpl(null, node.getQualifiedName(), node.getObjectCategory(), node.getDataType(), node.isAnonymous());
    

    tempNode.addFields(fields);
    return tempNode;
  }
}