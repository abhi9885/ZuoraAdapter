/*    */ package com.cognizant.ipm.adapter.runtime.parser;
/*    */ 
/*    */ import java.util.List;

/*    */ import javax.wsdl.Definition;
/*    */ import javax.xml.namespace.QName;
/*    */ 
/*    */ 
/*    */ 
/*    */ public class XMLTypeBuilder
/*    */ {
/*    */   public void build(Definition definition, CloudOperationMapping cloudOperationMapping, QName responseQName)
/*    */   {
/* 13 */     buildRootElement(responseQName, cloudOperationMapping.getResponseObjectMapping(), cloudOperationMapping.getDefaultResponseObjectGrouping(), definition);
/*    */   }
/*    */   
/*    */ 
/*    */ 
/*    */ 
/*    */ 
/*    */ 
/*    */   private void buildRootElement(QName responseQName, List<TypeMapping> responseObjectMapping, ObjectGrouping defaultResponseObjectGrouping, Definition definition)
/*    */   {
/* 23 */     validateTypeMappings(responseObjectMapping);
/*    */   }
/*    */   
/*    */ 
/*    */   private void validateTypeMappings(List<TypeMapping> elementObjectMappings)
/*    */   {
/* 29 */     if (elementObjectMappings != null) {
/* 30 */       for (TypeMapping typeMapping : elementObjectMappings)
/*    */       {
/* 32 */         TypeMask mask = typeMapping.getMask();
/* 33 */         if ((mask == null) || (mask.getIncludedFields() == null) || (mask.getExcludedFields().size() <= 0)) {}
/*    */       }
/*    */     }
/*    */   }
/*    */ }


/* Location:              C:\Oracle\Middleware\Oracle_Home\soa\soa\modules\oracle.cloud.adapter_12.1.3\zuora.jar!\oracle\cloud\connector\zuora\parser\XMLTypeBuilder.class
 * Java compiler version: 6 (50.0)
 * JD-Core Version:       0.7.1
 */