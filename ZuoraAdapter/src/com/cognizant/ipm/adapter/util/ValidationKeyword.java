package com.cognizant.ipm.adapter.util;
public class ValidationKeyword
{
  private String keyword;
  
  private boolean highlight;
  

  public ValidationKeyword(String keyword, boolean highlight)
  {
    this.keyword = keyword;
    this.highlight = highlight;
  }
  
  public String getKeyword() {
    return this.keyword;
  }
  
  public boolean doHighlight() {
    return this.highlight;
  }
}