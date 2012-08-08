package org.exoplatform.notification.impl.driver.router;

import java.util.Map;

import org.exoplatform.notification.api.Router;
import org.exoplatform.notification.impl.MessageContext;
import org.exoplatform.social.core.activity.model.ExoSocialActivity;
import org.exoplatform.social.core.activity.model.ExoSocialActivityImpl;
import org.exoplatform.social.core.identity.model.Identity;

public class StreamRouter implements Router {

  private ExoSocialActivity activity = null;
  private Identity ownerStream = null;
  private MessageContext context = null;
  
  
  public StreamRouter(MessageContext context) {
    activity = new ExoSocialActivityImpl();
    this.context = context;
  }
  
  public StreamRouter title(String title) {
    activity.setTitle(title);
    return this;
  }
  
  public StreamRouter userId(String userId) {
    activity.setUserId(userId);
    return this;
  }
  
  public StreamRouter type(String type) {
    activity.setType(type);
    return this;
  }
  
  public StreamRouter body(String body) {
    activity.setBody(body);
    return this;
  }
  
  public StreamRouter templateParams(Map<String, String> templateParams) {
    activity.setTemplateParams(templateParams);
    return this;
  }
  
  public StreamRouter identity(Identity identity) {
    this.ownerStream = identity;
    return this;
  }
  
  public ExoSocialActivity getMessage() {
    return this.activity;
  }
  
  public Identity getIdentity() {
    return this.ownerStream;
  }
  
  public MessageContext getContext() {
    return this.context;
  }
  
  public void reset() {
    activity = new ExoSocialActivityImpl();
  }
  
}
