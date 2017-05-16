package com.cognizant.ipm.adapter.util;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;
import java.util.regex.Pattern;

import oracle.cloud.connector.api.CloudAdapterLoggingService;

import org.codehaus.jackson.JsonFactory;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.JsonParser;
import org.codehaus.jackson.map.ObjectMapper;
import org.cometd.bayeux.Message;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class PopulateJSONXML
{
  private int i = 0;
  private Document doc = null;
  CloudAdapterLoggingService m_logger;
  private static JsonFactory jsonfactory = new JsonFactory();
  private static ObjectMapper mapper = new ObjectMapper(jsonfactory);
  
  private ArrayList<Object> s2Value = new ArrayList();
  
  public PopulateJSONXML(CloudAdapterLoggingService m_logger) {
    this.m_logger = m_logger;
  }
  
  public Document formatwithJSONData(JsonNode rootNode, Document document)
  {
    this.doc = document;
    try
    {
      parseNodeswithXPath(rootNode);
    }
    catch (Exception e) {
      this.m_logger.logError("Exception caught in formatwithJSONData method:" + e.getMessage(), e);
    }

    return this.doc;
  }
  
  public void getMappedJsonData(JsonNode nodes)
  {
    Iterator<Map.Entry<String, JsonNode>> fieldsIterator = nodes.getFields();
    while (fieldsIterator.hasNext())
    {
      Map.Entry<String, JsonNode> field = (Map.Entry)fieldsIterator.next();
      if (((JsonNode)field.getValue()).isObject())
      {
        getMappedJsonData((JsonNode)field.getValue());
      }
      else if (!((JsonNode)field.getValue()).isNull())
      {
        this.s2Value.add(((JsonNode)field.getValue()).getValueAsText());
      }
    }
  }

  public void parseNodeswithXPath(JsonNode rootNode)
  {
    Iterator<Map.Entry<String, JsonNode>> fieldsIterator = rootNode.getFields();
    while (fieldsIterator.hasNext())
    {
      Map.Entry<String, JsonNode> field = (Map.Entry)fieldsIterator.next();
      if (((JsonNode)field.getValue()).isObject()) {
        parseNodeswithXPath((JsonNode)field.getValue());
      }
      else {
        NodeList parentEle = this.doc.getElementsByTagName((String)field.getKey());
        if (((parentEle.getLength() != 0 ? 1 : 0) & (!((JsonNode)field.getValue()).isNull() ? 1 : 0)) != 0) {
          parentEle.item(0).setTextContent("{" + this.i + "}");
          this.i += 1;
        }
      }
    }
  }

  public void transformDocforJSON(NodeList rootNodeList)
  {
    for (int i = 0; i < rootNodeList.getLength(); i++) {
      Node node = rootNodeList.item(i);
      if (node.getChildNodes().item(0) != null)
      {
        NodeList nodechildNode = node.getChildNodes();
        transformDocforJSON(nodechildNode);
      } else {
        node.setTextContent("{" + node.getNodeName() + "}");
      }
    }
  }

  public StringBuffer dockJSONData(Map<String, String> jsonMap, StringBuffer str)
  {
    for (Map.Entry<String, String> entry : jsonMap.entrySet())
    {
      String pattern = "{" + (String)entry.getKey() + "}";
      int start; while ((start = str.indexOf(pattern)) != -1)
      {
        if (entry.getValue() == null) {
          str.replace(start, start + pattern.length(), "");
        } else {
          str.replace(start, start + pattern.length(), (String)entry.getValue());
        }
      }
    }

    for (Map.Entry<String, String> entry : jsonMap.entrySet())
    {
      if (((String)entry.getKey()).contains("type"))
      {
        if (((String)entry.getValue()).contains("delete"))
        {
          StringBuffer resendStr = new StringBuffer(Pattern.compile("(\\{).*?(\\})", 32).matcher(str).replaceAll(""));
          return resendStr;
        }
      }
    }
    str = new StringBuffer(Pattern.compile("(\\{).*?(\\})").matcher(str).replaceAll(""));
    return str;
  }

  public static void generateJSONMap(JsonNode rootNode, Map<String, String> jsonMap, CloudAdapterLoggingService m_logger)
  {
    Iterator<Map.Entry<String, JsonNode>> fieldsIterator = rootNode.getFields();
    while (fieldsIterator.hasNext())
    {
      Map.Entry<String, JsonNode> field = (Map.Entry)fieldsIterator.next();
      if (((JsonNode)field.getValue()).isObject())
      {
        generateJSONMap((JsonNode)field.getValue(), jsonMap, m_logger);
      }
      else if (!((JsonNode)field.getValue()).isNull()) {
        if (((JsonNode)field.getValue()).isTextual()) {
          jsonMap.put(field.getKey(), CloudStringUtil.escapeXML(((JsonNode)field.getValue()).getTextValue(), "encode"));
        }
        else if (((JsonNode)field.getValue()).isBigDecimal()) {
          jsonMap.put(field.getKey(), String.valueOf(((JsonNode)field.getValue()).getDecimalValue()));
        }
        else if (((JsonNode)field.getValue()).isBigInteger()) {
          jsonMap.put(field.getKey(), String.valueOf(((JsonNode)field.getValue()).getBigIntegerValue()));
        }
        else if (((JsonNode)field.getValue()).isDouble()) {
          jsonMap.put(field.getKey(), String.valueOf(((JsonNode)field.getValue()).getDoubleValue()));
        }
        else if (((JsonNode)field.getValue()).isBoolean()) {
          jsonMap.put(field.getKey(), String.valueOf(((JsonNode)field.getValue()).getBooleanValue()));
        }
        else if (((JsonNode)field.getValue()).isFloatingPointNumber()) {
          jsonMap.put(field.getKey(), String.valueOf(((JsonNode)field.getValue()).getDoubleValue()));
        }
        else if (((JsonNode)field.getValue()).isInt()) {
          jsonMap.put(field.getKey(), String.valueOf(((JsonNode)field.getValue()).getIntValue()));
        }
        else if (((JsonNode)field.getValue()).isIntegralNumber()) {
          jsonMap.put(field.getKey(), String.valueOf(((JsonNode)field.getValue()).getIntValue()));
        }
        else if (((JsonNode)field.getValue()).isLong()) {
          jsonMap.put(field.getKey(), String.valueOf(((JsonNode)field.getValue()).getLongValue()));
        }
        else if (((JsonNode)field.getValue()).isNumber()) {
          jsonMap.put(field.getKey(), String.valueOf(((JsonNode)field.getValue()).getNumberValue()));
        }
      }
    }
  }
  
  public static JsonNode fetchJsonRoot(Message inboundMessageFromzuora, CloudAdapterLoggingService m_logger)
    throws IOException, JsonParseException
  {
    JsonNode rootNode = null;
    JsonParser parser = jsonfactory.createJsonParser(inboundMessageFromzuora.getJSON());
    rootNode = mapper.readTree(parser);
    return rootNode;
  }
}