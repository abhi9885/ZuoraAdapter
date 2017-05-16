package com.cognizant.ipm.adapter.runtime.parser;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.namespace.QName;

public class ApplicationModel
{
  private List<DataObjectNode> dataObjectNodes;
  private Map<QName, DataObjectNode> qnameToObjectMap;
  private List<APINode> apis;
  private Set<OperationNode> operationList;
  private Map<QName, DataObjectReference> referenceMap;
  private Map<String, DataObjectNode> nameToObjectMap;
  private List<ModelObserver> observers;
  
  public ApplicationModel()
  {
    this.dataObjectNodes = new ArrayList();
    this.apis = new ArrayList();
    this.qnameToObjectMap = new HashMap();
    this.operationList = new HashSet();
    this.referenceMap = new HashMap();
    this.nameToObjectMap = new HashMap();
  }
  
  public void addOperation(OperationNode operation) {
    this.operationList.add(operation);
    notifyObservers(operation);
  }
  
  public void addOperations(List<OperationNode> ops) {
    this.operationList.addAll(ops);
    notifyOperationObservers(ops);
  }
  
  public void addDataObject(DataObjectNode dataObject) {
    QName name = dataObject.getQualifiedName();
    if (name != null) {
      this.qnameToObjectMap.put(name, dataObject);
    }
    String simpleName = dataObject.getName();
    if (simpleName != null) {
      this.nameToObjectMap.put(simpleName, dataObject);
    }
    this.dataObjectNodes.add(dataObject);
    
    notifyObservers(dataObject);
  }
  
  public void addDataObjects(List<DataObjectNode> dataObjects) {
    for (DataObjectNode dataObject : dataObjects) {
      QName name = dataObject.getQualifiedName();
      
      if (name != null) {
        this.qnameToObjectMap.put(name, dataObject);
      }
    }
    this.dataObjectNodes.addAll(dataObjects);
    notifyDataObjectObservers(dataObjects);
  }
  
  public List<OperationNode> findOperations(String name) {
    List matchingOperations = new ArrayList();
    for (OperationNode operation : this.operationList) {
      if (operation.getName().equals(name)) {
        matchingOperations.add(operation);
      }
    }
    
    return matchingOperations;
  }
  
  public List<OperationNode> findOperations(APINode apiNode) {
    List operationNodes = new ArrayList();
    Set<String> operationNames = apiNode.getOperationNames();
    if ((operationNames != null) && (!operationNames.isEmpty())) {
      for (String name : operationNames) {
        operationNodes.addAll(findOperations(name));
      }
    }
    return operationNodes;
  }
  
  public List<DataObjectNode> getDataObjects() {
    return this.dataObjectNodes;
  }
  
  public DataObjectNode findDataObject(QName qualifiedName)
  {
    return (DataObjectNode)this.qnameToObjectMap.get(qualifiedName);
  }
  
  public List<OperationNode> getOperations()
  {
    List opList = new ArrayList();
    opList.addAll(this.operationList);
    return opList;
  }
  
  public DataObjectReference getDataObjectReference(QName qualifiedName) {
    return (DataObjectReference)this.referenceMap.get(qualifiedName);
  }
  
  public void addDataObjectReference(DataObjectReference ref) {
    this.referenceMap.put(ref.getQualifiedName(), ref);
    notifyObservers(ref);
  }
  
  public void getDataObjectDecendants(DataObjectNode dataObjectNode, boolean recursive, Set<DataObjectNode> decendents)
  {
    List<DataObjectNode> children = dataObjectNode.getDescendants();
    if (children != null) {
      decendents.addAll(children);
      if (recursive)
        for (DataObjectNode child : children)
          getDataObjectDecendants(child, recursive, decendents);
    }
  }
  
  private void notifyObservers(DataObjectNode changed) {
    if (this.observers != null)
      for (ModelObserver observer : this.observers)
        observer.dataObjectAdded(this, changed);
  }
  
  private void notifyObservers(OperationNode changed) {
    if (this.observers != null)
      for (ModelObserver observer : this.observers)
        observer.operationAdded(this, changed);
  }
  
  private void notifyOperationObservers(List<OperationNode> changed) {
    if (this.observers != null)
      for (OperationNode operationNode : changed)
        notifyObservers(operationNode);
  }
  
  private void notifyDataObjectObservers(List<DataObjectNode> changed) {
    if (this.observers != null)
      for (DataObjectNode dataObject : changed)
        notifyObservers(dataObject);
  }
  
  private void notifyObservers(DataObjectReference reference) {
    if (this.observers != null)
      for (ModelObserver observer : this.observers)
        observer.dataObjectReferenceAdded(this, reference);
  }
  
  public void addModelObserver(ModelObserver observer) {
    if (this.observers == null) {
      this.observers = new ArrayList();
    }
    this.observers.add(observer);
  }
  
  public List<ModelObserver> getObservers() {
    return this.observers;
  }
  
  public DataObjectNode findDataObject(String name) {
    return (DataObjectNode)this.nameToObjectMap.get(name);
  }
}