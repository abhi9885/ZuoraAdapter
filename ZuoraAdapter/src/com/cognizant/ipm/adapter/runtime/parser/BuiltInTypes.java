package com.cognizant.ipm.adapter.runtime.parser;

import java.util.HashMap;
import java.util.Map;

import javax.xml.namespace.QName;

public class BuiltInTypes
{
  public static final String XML_NS = "http://www.w3.org/2001/XMLSchema";
  public static final QName INT_TYPE = new QName("http://www.w3.org/2001/XMLSchema", "int");
  
  public static final QName LONG_TYPE = new QName("http://www.w3.org/2001/XMLSchema", "long");
  
  public static final QName INTEGER_TYPE = new QName("http://www.w3.org/2001/XMLSchema", "integer");
  
  public static final QName FLOAT_TYPE = new QName("http://www.w3.org/2001/XMLSchema", "float");
  
  public static final QName DOUBLE_TYPE = new QName("http://www.w3.org/2001/XMLSchema", "double");
  
  public static final QName DECIMAL_TYPE = new QName("http://www.w3.org/2001/XMLSchema", "decimal");
  
  public static final QName SHORT_TYPE = new QName("http://www.w3.org/2001/XMLSchema", "short");
  
  public static final QName DATE_TYPE = new QName("http://www.w3.org/2001/XMLSchema", "date");
  
  public static final QName DATETIME_TYPE = new QName("http://www.w3.org/2001/XMLSchema", "dateTime");
  
  public static final QName TIME_TYPE = new QName("http://www.w3.org/2001/XMLSchema", "time");
  
  public static final QName BOOLEAN_TYPE = new QName("http://www.w3.org/2001/XMLSchema", "boolean");
  
  public static final QName STRING_TYPE = new QName("http://www.w3.org/2001/XMLSchema", "string");
  
  public static final QName ID_TYPE = new QName("http://www.w3.org/2001/XMLSchema", "ID");
  
  public static final QName IDREF_TYPE = new QName("http://www.w3.org/2001/XMLSchema", "IDREF");
  
  public static final QName HEXB_TYPE = new QName("http://www.w3.org/2001/XMLSchema", "hexBinary");
  
  public static final QName DURATION_TYPE = new QName("http://www.w3.org/2001/XMLSchema", "duration");
  
  public static final QName GYEARMONTH_TYPE = new QName("http://www.w3.org/2001/XMLSchema", "gYearMonth");
  
  public static final QName GYEAR_TYPE = new QName("http://www.w3.org/2001/XMLSchema", "gYear");
  
  public static final QName GMONTHDAY_TYPE = new QName("http://www.w3.org/2001/XMLSchema", "gMonthDay");
  
  public static final QName GDAY_TYPE = new QName("http://www.w3.org/2001/XMLSchema", "gDay");
  
  public static final QName GMONTH_TYPE = new QName("http://www.w3.org/2001/XMLSchema", "gMonth");
  
  public static final QName BASE64B_TYPE = new QName("http://www.w3.org/2001/XMLSchema", "base64Binary");
  
  public static final QName ANYURI_TYPE = new QName("http://www.w3.org/2001/XMLSchema", "anyURI");
  
  public static final QName QNAME_TYPE = new QName("http://www.w3.org/2001/XMLSchema", "QName");
  
  public static final QName NOTATION_TYPE = new QName("http://www.w3.org/2001/XMLSchema", "NOTATION");
  
  public static final QName NORMSTRING_TYPE = new QName("http://www.w3.org/2001/XMLSchema", "normalizedString");
  
  public static final QName TOKEN_TYPE = new QName("http://www.w3.org/2001/XMLSchema", "token");
  
  public static final QName LANGUAGE_TYPE = new QName("http://www.w3.org/2001/XMLSchema", "language");
  
  public static final QName NAME_TYPE = new QName("http://www.w3.org/2001/XMLSchema", "Name");
  
  public static final QName NMTOKEN_TYPE = new QName("http://www.w3.org/2001/XMLSchema", "NMTOKEN");
  
  public static final QName NMTOKENS_TYPE = new QName("http://www.w3.org/2001/XMLSchema", "NMTOKENS");
  
  public static final QName IDREFS_TYPE = new QName("http://www.w3.org/2001/XMLSchema", "IDREFS");
  
  public static final QName ENTITY_TYPE = new QName("http://www.w3.org/2001/XMLSchema", "ENTITY");
  
  public static final QName ENTITIES_TYPE = new QName("http://www.w3.org/2001/XMLSchema", "ENTITIES");
  
  public static final QName NCNAME_TYPE = new QName("http://www.w3.org/2001/XMLSchema", "NCNAME");
  
  public static final QName NEGINT_TYPE = new QName("http://www.w3.org/2001/XMLSchema", "negativeInteger");
  
  public static final QName ULONG_TYPE = new QName("http://www.w3.org/2001/XMLSchema", "unsignedLong");
  
  public static final QName POSITIVEINT_TYPE = new QName("http://www.w3.org/2001/XMLSchema", "positiveInteger");
  
  public static final QName UINT_TYPE = new QName("http://www.w3.org/2001/XMLSchema", "uint");
  
  public static final QName USHORT_TYPE = new QName("http://www.w3.org/2001/XMLSchema", "unsignedShort");
  
  public static final QName BYTE_TYPE = new QName("http://www.w3.org/2001/XMLSchema", "byte");
  
  public static final QName UBYTE_TYPE = new QName("http://www.w3.org/2001/XMLSchema", "unsignedByte");
  
  public static final QName NONNEGINT_TYPE = new QName("http://www.w3.org/2001/XMLSchema", "nonNegativeInteger");
  
  public static final QName NONPOSINT_TYPE = new QName("http://www.w3.org/2001/XMLSchema", "nonPositiveInteger");
  
  public static final QName ANYTYPE_TYPE = new QName("http://www.w3.org/2001/XMLSchema", "anyType");
  
  public static final QName ANYSIMPLE_TYPE = new QName("http://www.w3.org/2001/XMLSchema", "anySimpleType");
  
  public static final QName ANY_TYPE = new QName("http://www.w3.org/2001/XMLSchema", "any");
  

  public static DataObjectNode INT = builtIn(INT_TYPE, DataType.INT);
  public static DataObjectNode LONG = builtIn(LONG_TYPE, DataType.LONG);
  public static DataObjectNode INTEGER = builtIn(INTEGER_TYPE, DataType.INTEGER);
  
  public static DataObjectNode SHORT = builtIn(SHORT_TYPE, DataType.SHORT);
  public static DataObjectNode BOOLEAN = builtIn(BOOLEAN_TYPE, DataType.BOOLEAN);
  
  public static DataObjectNode NONNEGATIVEINT = builtIn(NONNEGINT_TYPE, DataType.INT);
  
  public static DataObjectNode NONPOSITIVEINT = builtIn(NONPOSINT_TYPE, DataType.INT);
  
  public static DataObjectNode UNSIGNEDSHORT = builtIn(USHORT_TYPE, DataType.SHORT);
  
  public static DataObjectNode ANY = builtIn(ANY_TYPE, DataType.OBJECT);
  public static DataObjectNode FLOAT = builtIn(FLOAT_TYPE, DataType.FLOAT);
  public static DataObjectNode DOUBLE = builtIn(DOUBLE_TYPE, DataType.DOUBLE);
  public static DataObjectNode DECIMAL = builtIn(DECIMAL_TYPE, DataType.DECIMAL);
  
  public static DataObjectNode DATE = builtIn(DATE_TYPE, DataType.DATE);
  public static DataObjectNode DATETIME = builtIn(DATETIME_TYPE, DataType.DATETIME);
  
  public static DataObjectNode TIME = builtIn(TIME_TYPE, DataType.TIME);
  public static DataObjectNode STRING = builtIn(STRING_TYPE, DataType.STRING);
  public static DataObjectNode ID = builtIn(ID_TYPE, DataType.ID);
  public static DataObjectNode IDREF = builtIn(IDREF_TYPE, DataType.OBJECT);
  public static DataObjectNode HEXBINARY = builtIn(HEXB_TYPE, DataType.OBJECT);
  public static DataObjectNode DURATION = builtIn(DURATION_TYPE, DataType.OBJECT);
  
  public static DataObjectNode GYEARMONTH = builtIn(GYEARMONTH_TYPE, DataType.OBJECT);
  
  public static DataObjectNode GMONTHDAY = builtIn(GMONTHDAY_TYPE, DataType.OBJECT);
  
  public static DataObjectNode GDAY = builtIn(GDAY_TYPE, DataType.OBJECT);
  public static DataObjectNode GMONTH = builtIn(GMONTH_TYPE, DataType.OBJECT);
  public static DataObjectNode BASE64 = builtIn(BASE64B_TYPE, DataType.STRING);
  public static DataObjectNode ANYURI = builtIn(ANYURI_TYPE, DataType.STRING);
  public static DataObjectNode QNAME = builtIn(QNAME_TYPE, DataType.STRING);
  public static DataObjectNode NOTATION = builtIn(NOTATION_TYPE, DataType.STRING);
  
  public static DataObjectNode NORMALIZEDSTRING = builtIn(NORMSTRING_TYPE, DataType.STRING);
  
  public static DataObjectNode TOKEN = builtIn(TOKEN_TYPE, DataType.STRING);
  public static DataObjectNode LANGUAGE = builtIn(LANGUAGE_TYPE, DataType.STRING);
  
  public static DataObjectNode NAME = builtIn(NAME_TYPE, DataType.STRING);
  public static DataObjectNode NMTOKEN = builtIn(NMTOKEN_TYPE, DataType.STRING);
  
  public static DataObjectNode NMTOKENS = builtIn(NMTOKENS_TYPE, DataType.OBJECT);
  
  public static DataObjectNode IDREFS = builtIn(IDREFS_TYPE, DataType.OBJECT);
  public static DataObjectNode ENTITY = builtIn(ENTITY_TYPE, DataType.STRING);
  public static DataObjectNode ENTITIES = builtIn(ENTITIES_TYPE, DataType.OBJECT);
  
  public static DataObjectNode NCNAME = builtIn(NCNAME_TYPE, DataType.STRING);
  public static DataObjectNode NEGATIVEINT = builtIn(NEGINT_TYPE, DataType.INT);
  
  public static DataObjectNode ULONG = builtIn(ULONG_TYPE, DataType.LONG);
  public static DataObjectNode POSITIVEINT = builtIn(POSITIVEINT_TYPE, DataType.INT);
  
  public static DataObjectNode UINT = builtIn(UINT_TYPE, DataType.INT);
  public static DataObjectNode USHORT = builtIn(USHORT_TYPE, DataType.SHORT);
  public static DataObjectNode BYTE = builtIn(BYTE_TYPE, DataType.BYTE);
  public static DataObjectNode UBYTE = builtIn(UBYTE_TYPE, DataType.BYTE);
  public static DataObjectNode ANYTYPE = builtIn(ANYTYPE_TYPE, DataType.OBJECT);
  
  public static DataObjectNode ANYSIMPLETYPE = builtIn(ANYSIMPLE_TYPE, DataType.STRING);
  

  private static Map<QName, DataObjectNode> TYPE_MAP = new HashMap();
  
  public static DataObjectNode getBuiltIn(QName typeName) {
    return (DataObjectNode)TYPE_MAP.get(typeName);
  }
  
  public static QName lookupDataType(DataType type) {
    switch (DataType(type.ordinal())) {
    case 1: 
      return BOOLEAN_TYPE;
    case 2: 
      return BYTE_TYPE;
    case 3: 
      return DATE_TYPE;
    case 4: 
      return DATETIME_TYPE;
    case 5: 
      return DECIMAL_TYPE;
    case 6: 
      return DOUBLE_TYPE;
    case 7: 
      return FLOAT_TYPE;
    case 8: 
      return ID_TYPE;
    case 9: 
      return INT_TYPE;
    case 10: 
      return INTEGER_TYPE;
    case 11: 
      return LONG_TYPE;
    case 12: 
      return ANY_TYPE;
    case 13: 
      return QNAME_TYPE;
    case 14: 
      return SHORT_TYPE;
    case 15: 
      return STRING_TYPE;
    case 16: 
      return TIME_TYPE;
    }
    return STRING_TYPE;
  }
  
  private static int DataType(int ordinal)
  {
    return 0;
  }
  
  private static DataObjectNode builtIn(QName typeName, DataType type) {
    return new DataObjectNode(null, typeName, DataObjectNode.ObjectCategory.BUILTIN, type);
  }
  
  static
  {
    TYPE_MAP.put(INT_TYPE, INT);
    TYPE_MAP.put(LONG_TYPE, LONG);
    TYPE_MAP.put(INTEGER_TYPE, INTEGER);
    TYPE_MAP.put(SHORT_TYPE, SHORT);
    TYPE_MAP.put(BOOLEAN_TYPE, BOOLEAN);
    TYPE_MAP.put(NONNEGINT_TYPE, NONNEGATIVEINT);
    TYPE_MAP.put(NONPOSINT_TYPE, NONPOSITIVEINT);
    TYPE_MAP.put(USHORT_TYPE, UNSIGNEDSHORT);
    TYPE_MAP.put(ANY_TYPE, ANY);
    TYPE_MAP.put(FLOAT_TYPE, FLOAT);
    TYPE_MAP.put(DOUBLE_TYPE, DOUBLE);
    TYPE_MAP.put(DECIMAL_TYPE, DECIMAL);
    TYPE_MAP.put(DATE_TYPE, DATE);
    TYPE_MAP.put(DATETIME_TYPE, DATETIME);
    TYPE_MAP.put(TIME_TYPE, TIME);
    TYPE_MAP.put(STRING_TYPE, STRING);
    TYPE_MAP.put(ID_TYPE, ID);
    TYPE_MAP.put(IDREF_TYPE, IDREF);
    TYPE_MAP.put(HEXB_TYPE, HEXBINARY);
    TYPE_MAP.put(DURATION_TYPE, DURATION);
    TYPE_MAP.put(GYEARMONTH_TYPE, GYEARMONTH);
    TYPE_MAP.put(GMONTHDAY_TYPE, GMONTHDAY);
    TYPE_MAP.put(GDAY_TYPE, GDAY);
    TYPE_MAP.put(GMONTH_TYPE, GMONTH);
    TYPE_MAP.put(BASE64B_TYPE, BASE64);
    TYPE_MAP.put(ANYURI_TYPE, ANYURI);
    TYPE_MAP.put(QNAME_TYPE, QNAME);
    TYPE_MAP.put(NOTATION_TYPE, NOTATION);
    TYPE_MAP.put(NORMSTRING_TYPE, NORMALIZEDSTRING);
    TYPE_MAP.put(TOKEN_TYPE, TOKEN);
    TYPE_MAP.put(LANGUAGE_TYPE, LANGUAGE);
    TYPE_MAP.put(NAME_TYPE, NAME);
    TYPE_MAP.put(NMTOKEN_TYPE, NMTOKEN);
    TYPE_MAP.put(NMTOKENS_TYPE, NMTOKENS);
    TYPE_MAP.put(IDREFS_TYPE, IDREFS);
    TYPE_MAP.put(ENTITY_TYPE, ENTITY);
    TYPE_MAP.put(ENTITIES_TYPE, ENTITIES);
    TYPE_MAP.put(NCNAME_TYPE, NCNAME);
    TYPE_MAP.put(NEGINT_TYPE, NEGATIVEINT);
    TYPE_MAP.put(ULONG_TYPE, ULONG);
    TYPE_MAP.put(POSITIVEINT_TYPE, POSITIVEINT);
    TYPE_MAP.put(UINT_TYPE, UINT);
    TYPE_MAP.put(USHORT_TYPE, USHORT);
    TYPE_MAP.put(BYTE_TYPE, BYTE);
    TYPE_MAP.put(UBYTE_TYPE, UBYTE);
    TYPE_MAP.put(ANYTYPE_TYPE, ANYTYPE);
    TYPE_MAP.put(ANYSIMPLE_TYPE, ANYSIMPLETYPE);
  }
}