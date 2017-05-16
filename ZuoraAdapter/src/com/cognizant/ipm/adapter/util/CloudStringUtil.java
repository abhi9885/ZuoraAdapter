package com.cognizant.ipm.adapter.util;

import java.text.StringCharacterIterator;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CloudStringUtil
{
  static final char[] hex = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F' };
  static final char _errChar = '$';
  
  public static String encodeName(String s)
  {
    if (s == null) {
      return null;
    }
    int len = s.length();
    if (len < 1) {
      return s;
    }
    char[] buf = new char[len * 5];
    int idx = 0;
    for (int i = 0; i < len; i++) {
      char c = s.charAt(i);
      if (((c >= 'A') && (c <= 'Z')) || ((c >= 'a') && (c <= 'z')) || (c == '_'))
      {
        buf[(idx++)] = c;
      } else if (((c >= '0') && (c <= '9')) || (c == '.'))
      {
        if (i == 0) {
          buf[(idx++)] = '_';
          buf[(idx++)] = '-';
          buf[(idx++)] = '-';
          buf[(idx++)] = '3';
        }
        buf[(idx++)] = c;
      } else if (c == '/') {
        buf[(idx++)] = '_';
        buf[(idx++)] = '-';
      } else {
        buf[(idx++)] = '_';
        buf[(idx++)] = '-';
        buf[(idx++)] = '-';
        buf[(idx++)] = hex[(c >> '\004' & 0xF)];
        buf[(idx++)] = hex[(c >> '\000' & 0xF)];
      }
    }
    
    return new String(buf, 0, idx);
  }
  
  public static String decodeName(String s) {
    if (s == null) {
      return null;
    }
    
    int len = s.length();
    if (len < 1) {
      return s;
    }
    char[] buf = new char[len * 5];
    int bi = 0;int si = 0;
    for (; si < len; bi++) {
      char c = s.charAt(si);
      if (((c >= 'A') && (c <= 'Z')) || ((c >= 'a') && (c <= 'z')))
      {



        buf[bi] = c;
      } else if (((c >= '0') && (c <= '9')) || (c == '.'))
      {
        buf[bi] = c;
      } else if (c == '_') {
        char c1 = s.charAt(si + 1);
        if (c1 == '-') {
          char c2 = s.charAt(si + 2);
          if (c2 != '-') {
            buf[bi] = '/';
            si++;
          } else if (c2 == '-') {
            char c3 = s.charAt(si + 3);
            
            if (c3 == '3') {
              char c4 = s.charAt(si + 4);
              
              buf[bi] = c4;
              si += 4;
            }
          } else {
            char x;
            try {
              x = (char)Integer.parseInt(s.substring(si + 3, si + 5), 16);

            }
            catch (Exception e)
            {
              x = '$';
            }
            buf[bi] = x;
            si += 4;
          }
        } else {
          buf[bi] = c;
        }
      }
      si++;
    }
    
    return new String(buf, 0, bi);
  }
  













  public static String escapeXML(String in, String userInput)
  {
    Map<String, String> entityMap = new HashMap();
    
    entityMap.put("&", "&amp;");
    entityMap.put("'", "&apos;");
    entityMap.put("\"", "&quot;");
    entityMap.put(">", "&gt;");
    entityMap.put("<", "&lt;");
    






    StringBuffer out = new StringBuffer(in.length() * 2);
    

    int encodedStatus = 0;
    



    if ((in == null) || ("".equals(in))) {
      return "";
    }
    for (int i = 0; i < in.length(); i++) {
      char current = in.charAt(i);
      



      if ((encodedStatus = isEncoded(in, current, i).intValue()) == 0)
      {
        String entityValue = (String)entityMap.get(Character.toString(current));
        if (entityValue != null)
        {
          out.append(entityValue);






        }
        else if (current < ' ') {
          if (userInput.equals("encode"))
          {



            out.append("&#" + current + ";");
          }
          if ((!userInput.equals("remove")) || 
          




            (userInput.equals("space")))
          {



            out.append(" ");

          }
          


        }
        else if (current > '~')
        {
          out.append("&#" + current + ";");
        } else {
          out.append(current);

        }
        

      }
      else
      {

        out.append(in.substring(i, encodedStatus));
        i = encodedStatus - 1;
      }
    }
    return out.toString();
  }
  










  public static Integer isEncoded(String in, char a, int index)
  {
    if (a == '&') {
      if (in.length() >= index + 5)
      {
        String str = in.subSequence(index, index + 5).toString();
        if (str.equals("&amp;")) {
          return Integer.valueOf(index += "&amp;".length());
        }
      }
      



      if ((in.length() > index + 2) && (in.substring(index, index + 2).equals("&#")))
      {

        return Integer.valueOf(in.indexOf(";", index));
      }
      if ((in.length() > index + 4) && (in.subSequence(index, index + 4).equals("&gt;")))
      {
        return Integer.valueOf(index + 4);
      }
      
      if ((in.length() > index + 4) && (in.subSequence(index, index + 4).equals("&lt;")))
      {
        return Integer.valueOf(index + 4);
      }
      if ((in.length() > index + 6) && (in.subSequence(index, index + 6).equals("&apos;")))
      {

        return Integer.valueOf(index);
      }
      if ((in.length() > index + 6) && (in.subSequence(index, index + 6).equals("&quot;")))
      {
        return Integer.valueOf(index);
      }
    }
    
    return Integer.valueOf(0);
  }
  

  public static String unescapeXml(String in)
  {
    StringBuffer unescapedXml = new StringBuffer(in.length() * 2);
    

    if ((in.isEmpty()) || (in == null)) {
      return "";
    }
    for (int i = 0; i < in.length();)
    {
      char a = in.charAt(i);
      
      if (a == '&')
      {
        if ((in.length() >= i + 5) && (in.subSequence(i, i + 5).equals("&amp;")))
        {
          unescapedXml.append("&");
          i += 5;

        }
        else if ((in.length() >= i + 2) && (in.substring(i, i + 2).equals("&#")))
        {

          String encodedStr = in.substring(i + 2, in.indexOf(';', i));
          
          unescapedXml.append(Character.toChars(Integer.parseInt(encodedStr)));
          
          i = i + encodedStr.length() + 3;

        }
        else if ((in.length() >= i + 4) && (in.subSequence(i, i + 4).equals("&gt;")))
        {
          unescapedXml.append(">");
          i += 4;
        }
        else if ((in.length() >= i + 4) && (in.subSequence(i, i + 4).equals("&lt;")))
        {
          unescapedXml.append("<");
          i += 4;


        }
        else if ((in.length() >= i + 6) && (in.subSequence(i, i + 6).equals("&apos;")))
        {
          unescapedXml.append("'");
          i += 6;

        }
        else if ((in.length() >= i + 6) && (in.subSequence(i, i + 6).equals("&quot;")))
        {
          unescapedXml.append("\"");
          i += 6;
        } else {
          unescapedXml.append(Character.toString(a));
          i += 1;
        }
      } else {
        unescapedXml.append(Character.toString(a));
        i += 1;
      }
    }
    
    return unescapedXml.toString();
  }
  
  public static class MyComparator implements Comparator<String>
  {
    private int maxLen;
    private static final String REGEX = "[0-9]+";
    
    public MyComparator(int maxLen) {
      this.maxLen = maxLen;
    }
    

    public int compare(String obj1, String obj2)
    {
      String o1 = obj1;
      String o2 = obj2;
      
      if ((o1.matches("[1-9]+")) && (o2.matches("[1-9]+"))) {
        Integer integer1 = Integer.valueOf(o1);
        Integer integer2 = Integer.valueOf(o2);
        return integer1.compareTo(integer2);
      }
      

      if ((o1.matches("[a-zA-Z]+")) && (o2.matches("[a-zA-Z]+"))) {
        return o1.compareTo(o2);
      }
      
      Pattern p = Pattern.compile("[0-9]+");
      Matcher m1 = p.matcher(o1);
      Matcher m2 = p.matcher(o2);
      
      List<String> list = new ArrayList();
      while (m1.find()) {
        list.add(m1.group());
      }
      for (String string : list) {
        o1.replaceFirst(string, CloudStringUtil.leftPad(string, "0", Integer.valueOf(this.maxLen)));
      }
      
      list.clear();
      
      while (m2.find()) {
        list.add(m2.group());
      }
      for (String string : list) {
        o2.replaceFirst(string, CloudStringUtil.leftPad(string, "0", Integer.valueOf(this.maxLen)));
      }
      return o1.compareTo(o2);
    }
  }
  

  public static String leftPad(String stringToPad, String padder, Integer size)
  {
    StringBuilder strb = new StringBuilder(size.intValue());
    StringCharacterIterator sci = new StringCharacterIterator(padder);
    
    while (strb.length() < size.intValue() - stringToPad.length()) {
      for (char ch = sci.first(); ch != 65535; ch = sci.next())
      {
        if (strb.length() < size.intValue() - stringToPad.length()) {
          strb.insert(strb.length(), String.valueOf(ch));
        }
      }
    }
    
    return stringToPad;
  }
}
