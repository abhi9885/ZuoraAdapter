package com.cognizant.ipm.adapter.query;

import oracle.tip.tools.ide.adapters.cloud.api.query.QueryKeyword;

public class CloudQueryKeyword extends QueryKeyword
{
  private String identifier;
  
  public CloudQueryKeyword(String fieldName, boolean isSuggestable, boolean isFunction, String identifier) {
    super(fieldName, isSuggestable, isFunction);
    this.identifier = identifier;
  }
  
  public String getIdentifier() {
    return this.identifier;
  }
}