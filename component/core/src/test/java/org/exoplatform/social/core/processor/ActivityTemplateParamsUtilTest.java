package org.exoplatform.social.core.processor;

import java.util.LinkedHashMap;
import java.util.Map;

import junit.framework.TestCase;

import org.exoplatform.social.core.activity.model.ExoSocialActivity;
import org.exoplatform.social.core.activity.model.ExoSocialActivityImpl;


public class ActivityTemplateParamsUtilTest extends TestCase {
  public void testSetTemplateParamsToProcess() throws Exception {
    //Template params is null
    ExoSocialActivity docActivity = new ExoSocialActivityImpl();
    ActivityTemplateParamsUtil.setTemplateParamsToProcess(docActivity, "ABC","DEF");
    assertEquals("ABC|DEF", docActivity.getTemplateParams().get(ActivityTemplateParamsUtil.REGISTERED_KEYS_FOR_PROCESSOR));

    //Template params is not null
    ActivityTemplateParamsUtil.setTemplateParamsToProcess(docActivity, "ABC","DEF");
    assertEquals("ABC|DEF", docActivity.getTemplateParams().get(ActivityTemplateParamsUtil.REGISTERED_KEYS_FOR_PROCESSOR));
  }

  public void testSetActivityEncodeStatus() throws Exception {
    ExoSocialActivity docActivity = new ExoSocialActivityImpl();

    ActivityTemplateParamsUtil.setActivityEncodeStatus(docActivity, true);
    assertEquals(Boolean.TRUE.toString(), docActivity.getTemplateParams().get(ActivityTemplateParamsUtil.TEMPLATE_PARAM_ALREADY_ENCODED));
  }

  public void testGetKeysForProcessing() throws Exception {
    ExoSocialActivity docActivity = new ExoSocialActivityImpl();
    Map<String, String> templateParams = new LinkedHashMap<String, String>();
    templateParams.put("ABC", "testtesttest");
    templateParams.put("DEF", "testtesttest");
    docActivity.setTemplateParams(templateParams);
    ActivityTemplateParamsUtil.setTemplateParamsToProcess(docActivity, "ABC","DEF");

    String[] keys = ActivityTemplateParamsUtil.getKeysForProcessing(docActivity);
    assertEquals(2, keys.length);
    assertEquals("ABC", keys[0]);
    assertEquals("DEF", keys[1]);
  }

  public void testIsActivityEncoded() throws Exception {
    ExoSocialActivity docActivity = new ExoSocialActivityImpl();

    ActivityTemplateParamsUtil.setActivityEncodeStatus(docActivity, true);
    assertEquals(true, ActivityTemplateParamsUtil.isActivityEncoded(docActivity));

  }
}
