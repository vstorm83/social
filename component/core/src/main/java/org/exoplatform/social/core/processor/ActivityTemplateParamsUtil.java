/*
 * Copyright (C) 2003-2011 eXo Platform SAS.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.exoplatform.social.core.processor;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.exoplatform.social.core.activity.model.ExoSocialActivity;

/**
 * Provides/ Registers Activity template parameters for processors working.
 * @author phuonglm
 * @since 1.2.9
 */
public class ActivityTemplateParamsUtil {
  public static final String REGISTERED_KEYS_FOR_PROCESSOR = "registeredKeysForProcessor";
  public static final String TEMPLATE_PARAM_ALREADY_ENCODED = "isAlreadyEncoded";

  public static final String TEMPLATE_PARAM_LIST_DELIM = "|";


  /**
   * Sets List of activity's template params to be process by activity processor
   * @param activity
   * @param templateParamArray array of template params key.
   */
  public static void setTemplateParamsToProcess(ExoSocialActivity activity, String... templateParamArray) {
    StringBuilder stringBuilder = new StringBuilder();
    for (String templateParam : templateParamArray) {
      stringBuilder.append(templateParam);
      stringBuilder.append(TEMPLATE_PARAM_LIST_DELIM);
    }
    int templateListLength = stringBuilder.length();
    if(templateListLength > 1){
      setTemplateParam(activity, REGISTERED_KEYS_FOR_PROCESSOR, stringBuilder.substring(0, templateListLength - 1));
    }
  }

  /**
   *
   * @param activity
   * @return
   */
  public static String[] getKeysForProcessing(ExoSocialActivity activity) {
    Map<String, String> templateParams = activity.getTemplateParams();
    List<String> keys = new ArrayList<String>();

    if (templateParams != null
        && templateParams.containsKey(ActivityTemplateParamsUtil.REGISTERED_KEYS_FOR_PROCESSOR)) {
      String[] templateParamKeys = activity.getTemplateParams()
                                           .get(ActivityTemplateParamsUtil.REGISTERED_KEYS_FOR_PROCESSOR)
                                           .split("\\|");

      for (String key : templateParamKeys) {
        if (templateParams.containsKey(key)) {
          keys.add(key);
        }
      }
    }

    return keys.toArray(new String[keys.size()]);
  }
  /**
   * Specifies that given Activity will be processed by Processor or not.
   * @param activity
   * @return TRUE: don't need to process; FALSE: need to be process
   */
  public static boolean isActivityEncoded(ExoSocialActivity activity) {
    Map<String, String> templateParams = activity.getTemplateParams();

    if (templateParams == null) {
      return false;
    }

    return templateParams.containsKey(TEMPLATE_PARAM_ALREADY_ENCODED)
        && activity.getTemplateParams()
                   .get(TEMPLATE_PARAM_ALREADY_ENCODED)
                   .equals(Boolean.TRUE.toString());
  }

  /**
   * Sets the activity encode status, if true the activity won't be process by activity processor
   * @param activity
   * @param isEncoded True if this activity already encoded
   */
  public static void setActivityEncodeStatus(ExoSocialActivity activity, Boolean isEncoded){
    setTemplateParam(activity, TEMPLATE_PARAM_ALREADY_ENCODED, isEncoded.toString());
  }

  /**
   * Adds one param with input key/value into activity template parameters.
   * @param activity activity to be added
   * @param key key of entry
   * @param value value of entry
   */
  private static void setTemplateParam(ExoSocialActivity activity, String key, String value){
    Map<String, String> templateParams = activity.getTemplateParams();
    if(templateParams == null){
      templateParams = new LinkedHashMap<String, String>();
    }
    templateParams.put(key, value);
    activity.setTemplateParams(templateParams);
  }
}
