/*    */ package com.cognizant.ipm.adapter.util;
/*    */ 
/*    */ import oracle.cloud.connector.api.CloudAdapterLoggingService;

/*    */ import org.cometd.bayeux.Message;
/*    */ import org.cometd.client.BayeuxClient;
/*    */ import org.cometd.client.transport.ClientTransport;
/*    */ 
/*    */ 
/*    */ 
/*    */ public class StreamingBayeuxClient extends BayeuxClient
/*    */ {
/*    */   CloudAdapterLoggingService m_logger;
/*    */   
/*    */   public StreamingBayeuxClient(String url, ClientTransport transport, CloudAdapterLoggingService m_logger)
/*    */   {
/* 20 */     super(url, transport, new ClientTransport[0]);
/* 21 */     this.m_logger = m_logger;
/*    */   }
/*    */ 
/*    */   public BayeuxClient.State getBayeuxState()
/*    */   {
/* 29 */     return getState();
/*    */   }
/*    */ 
/*    */   public void onFailure(Throwable x, Message[] messages)
/*    */   {
/* 35 */     super.onFailure(x, messages);
/* 36 */     this.m_logger.logError("Exception caught in onFailure() method:" + x.getMessage());
/*    */   }
/*    */ }