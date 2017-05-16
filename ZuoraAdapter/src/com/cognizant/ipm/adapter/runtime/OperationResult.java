package com.cognizant.ipm.adapter.runtime;
public enum OperationResult
{
  upsertResponse("UpsertResult"),  undeleteResponse("UndeleteResult"),  createResponse("SaveResult"), 
  updateResponse("SaveResult"),  deleteResponse("DeleteResult"), 
  mergeResponse("MergeResult"),  processResponse("ProcessResult"), 
  convertLeadResponse("LeadConvertResult"),  getUserInfoResponse("GetUserInfoResult"), 
  searchResponse("SearchResults"),  queryResponse("QueryResults"), 
  queryAllResponse("QueryResults"),  queryMoreResponse("QueryResults"), 
  getDeletedResponse("GetDeletedResult"),  getUpdatedResponse("GetUpdatedResult");

  private final String operationResult;

  private OperationResult(String operationResult)
  {
    this.operationResult = operationResult;
  }
  
  public String toString()
  {
    return this.operationResult;
  }
}