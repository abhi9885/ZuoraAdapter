package com.cognizant.ipm.adapter.query;

import java.util.List;

import oracle.tip.tools.ide.adapters.cloud.api.metadata.TypeMapping;
import oracle.tip.tools.ide.adapters.cloud.api.model.CloudDataObjectNode;
import oracle.tip.tools.ide.adapters.cloud.api.plugin.CloudApplicationAdapterException;

public abstract interface CloudQueryParser
{
  public abstract void processBasicValidations(String paramString) throws CloudApplicationAdapterException;
  public abstract CloudQueryModel buildQueryModel(String paramString) throws CloudApplicationAdapterException;
  public abstract CloudDataObjectNode validateObject(String paramString, boolean paramBoolean) throws CloudApplicationAdapterException;
  public abstract CloudQueryModel parse(String paramString, boolean paramBoolean) throws CloudApplicationAdapterException;
  public abstract List<TypeMapping> getResponseTypeMappings() throws CloudApplicationAdapterException;
  public abstract String removeExtraFormatting(String paramString);
  public abstract boolean isReservedKeyword(String paramString);
}