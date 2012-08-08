package org.exoplatform.notification.impl.driver.transport;

import org.exoplatform.notification.api.NotificationException;
import org.exoplatform.notification.api.Transporter;
import org.exoplatform.notification.impl.driver.router.StreamRouter;
import org.exoplatform.social.core.manager.ActivityManager;

public class StreamTransporter implements Transporter<StreamRouter> {

  private ActivityManager activityManager;
  
  public StreamTransporter(ActivityManager activityManager) {
    this.activityManager = activityManager;
  }
  
  @Override
  public void send(StreamRouter router) throws NotificationException {
    activityManager.saveActivityNoReturn(router.getIdentity(), router.getMessage());
  }

}
