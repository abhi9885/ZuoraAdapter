/*    */ package com.cognizant.ipm.adapter.util;
/*    */ 
/*    */import java.util.ResourceBundle;
/*    */ 
/*    */ public class AdapterResourceBundle
/*    */ {
/*  7 */   private static final ResourceBundle rb = ResourceBundle.getBundle("com.cognizant.ipm.adapter.ResourceBundle");
/*    */   
/*    */ 
/*    */ 
/*    */ 
/*    */   public static String getValue(String key)
/*    */   {
/* 14 */     return rb.getString(key);
/*    */   }
/*    */   
/*    */   public static void main(String[] args) {
/* 18 */     System.out.println(getValue("EXTENSION_NAME"));
/*    */   }
/*    */ }