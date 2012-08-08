package org.exoplatform.notification.impl.utils;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;

import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;


public class Deserializer {

  private static Log LOG = ExoLogger.getLogger(Deserializer.class);

  /**
   * @param input
   *    - input stream
   * @return String from io
   */
  public static String readFromInputStream(InputStream input) throws IOException
  {
     ByteArrayOutputStream output = new ByteArrayOutputStream();

     byte[] buffer = new byte[4 * 1024];
     int n = 0;
     while (-1 != (n = input.read(buffer)))
     {
        output.write(buffer, 0, n);
     }

     return output.toString();
  }

  public static String getResourceContent(String resource) throws IOException
  {
     File file = new File(resource);
     if (file.exists() && file.isFile())
     {
        FileInputStream stream = new FileInputStream(file);
        try
        {
           return readFromInputStream(stream);
        }
        finally
        {
           stream.close();
        }
     }

     URL url = Thread.currentThread().getContextClassLoader().getResource(resource);
     if (url != null)
     {
        LOG.debug("loading {}", url.toString());
        InputStream inputStream = null;
        try
        {
           inputStream = url.openConnection().getInputStream();
           if (inputStream != null)
           {
              return readFromInputStream(inputStream);
           }
        }
        finally
        {
           if (inputStream != null)
           {
              inputStream.close();
           }
        }
     }

     return null;
  }

  /**
   * Resolve mail template
   *
   * @param templateContent
   *    -  content of template
   * @param properties
   *    - properties for template resolving
   * @return
   */
  public static String resolveTemplate(String templateContent, Map<String, String> properties)
  {
     for (Entry<String, String> property : properties.entrySet())
     {
        templateContent = templateContent.replace("${" + property.getKey() + "}", property.getValue());
     }
     return templateContent;
  }

  public static String resolveVariables(String input, Properties props)
  {
     final int NORMAL = 0;
     final int SEEN_DOLLAR = 1;
     final int IN_BRACKET = 2;
     if (input == null)
     {
        return input;
     }
     char[] chars = input.toCharArray();
     StringBuffer buffer = new StringBuffer();
     boolean properties = false;
     int state = NORMAL;
     int start = 0;
     for (int i = 0; i < chars.length; ++i)
     {
        char c = chars[i];
        if (c == '$' && state != IN_BRACKET)
        {
           state = SEEN_DOLLAR;
        }
        else if (c == '{' && state == SEEN_DOLLAR)
        {
           buffer.append(input.substring(start, i - 1));
           state = IN_BRACKET;
           start = i - 1;
        }
        else if (state == SEEN_DOLLAR)
        {
           state = NORMAL;
        }
        else if (c == '}' && state == IN_BRACKET)
        {
           if (start + 2 == i)
           {
              buffer.append("${}");
           }
           else
           {
              String value = null;
              String key = input.substring(start + 2, i);

              if (props != null)
              {
                 // Some parameters have been given thus we need to check
                 // inside first
                 String sValue = props.getProperty(key);
                 value = sValue == null || sValue.length() == 0 ? null : sValue;
              }
              if (value == null)
              {
                 // try to get it from the system properties
                 value = System.getProperty(key);
              }

              if (value != null)
              {
                 properties = true;
                 buffer.append(value);
              }
           }
           start = i + 1;
           state = NORMAL;
        }
     }
     if (properties == false)
     {
        return input;
     }
     if (start != chars.length)
     {
        buffer.append(input.substring(start, chars.length));
     }
     return buffer.toString();

  }
}
