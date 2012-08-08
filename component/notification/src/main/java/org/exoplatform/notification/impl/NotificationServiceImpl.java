package org.exoplatform.notification.impl;

import org.exoplatform.notification.api.NotificationDriver;
import org.exoplatform.notification.api.NotificationException;
import org.exoplatform.notification.api.NotificationService;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

public class NotificationServiceImpl implements NotificationService {

  private static Log LOG = ExoLogger.getLogger(NotificationServiceImpl.class);

  @Override
  public void publish(NotificationDriver<?, ?>... drivers) throws NotificationException {
    for (NotificationDriver<?, ?> driver : drivers) {
      driver.send();
    }
  }

}
