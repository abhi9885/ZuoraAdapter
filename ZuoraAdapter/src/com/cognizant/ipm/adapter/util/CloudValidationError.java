package com.cognizant.ipm.adapter.util;

import java.util.ArrayList;
import java.util.List;

public class CloudValidationError
{
  private String errorCode;
  private List<ValidationKeyword> keywordList = new ArrayList<ValidationKeyword>();
  
  public CloudValidationError(String errorCode, List<ValidationKeyword> keywords)
  {
    this.errorCode = errorCode;
    this.keywordList.addAll(keywords);
  }
  
  public String getErrorCode() {
    return this.errorCode;
  }
  
  public List<ValidationKeyword> getKeywordsList() {
    return this.keywordList;
  }
}