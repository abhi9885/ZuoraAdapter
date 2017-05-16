package com.cognizant.ipm.adapter.query;

import java.util.HashSet;
import java.util.Set;

import javax.xml.namespace.QName;

import oracle.tip.tools.ide.adapters.cloud.api.metadata.ObjectGrouping;
import oracle.tip.tools.ide.adapters.cloud.api.model.CloudQueryParameters;
import oracle.tip.tools.ide.adapters.cloud.api.model.Field;
import oracle.tip.tools.ide.adapters.cloud.api.model.ObjectCategory;
import oracle.tip.tools.ide.adapters.cloud.api.plugin.CloudApplicationAdapterException;
import oracle.tip.tools.ide.adapters.cloud.impl.metadata.model.CloudDataObjectNodeImpl;
import oracle.tip.tools.ide.adapters.cloud.impl.metadata.model.QueryParameterField;

public class CloudQueryParametersImpl extends CloudDataObjectNodeImpl implements CloudQueryParameters
{
  private Set<String> parameterNames = new HashSet();
  
  public CloudQueryParametersImpl(QName TYPE_NAME) {
    super(null, TYPE_NAME, ObjectCategory.CUSTOM);
    setFieldGrouping(ObjectGrouping.UNORDERED);
  }
  
  public Set<String> getParameterNames()
  {
    return this.parameterNames;
  }
  
  public void addParameterName(String name)
  {
    addParameterName(name, false);
  }
  
  public void addParameterName(String name, boolean nullable)
  {
    QueryParameterField field = new QueryParameterField(name, nullable);
    addField(field);
    this.parameterNames.add(name);
  }
  

  public boolean removeParameter(String name)
    throws CloudApplicationAdapterException
  {
    boolean removed = false;
    if (this.parameterNames.contains(name)) {
      Set<Field> fields = getFields();
      Field toRemove = null;
      
      for (Field field : fields) {
        if (field.getName().equals(name)) {
          toRemove = field;
          break;
        }
      }
      if (toRemove != null) {
        removeField(toRemove);
        this.parameterNames.remove(name);
        removed = true;
      }
    }
    return removed;
  }
}