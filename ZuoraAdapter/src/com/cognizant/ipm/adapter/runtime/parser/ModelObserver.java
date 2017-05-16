package com.cognizant.ipm.adapter.runtime.parser;

public abstract interface ModelObserver
{
  public abstract void dataObjectAdded(ApplicationModel paramApplicationModel, DataObjectNode paramDataObjectNode);
  
  public abstract void operationAdded(ApplicationModel paramApplicationModel, OperationNode paramOperationNode);
  
  public abstract void dataObjectReferenceAdded(ApplicationModel paramApplicationModel, DataObjectReference paramDataObjectReference);
}
