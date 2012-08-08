package org.exoplatform.notification.impl.driver;

import org.exoplatform.notification.api.Channel;
import org.exoplatform.notification.api.NotificationDriver;
import org.exoplatform.notification.api.NotificationException;
import org.exoplatform.notification.impl.driver.router.MailRouter;
import org.exoplatform.notification.impl.driver.transport.MailTransporter;

public class MailDriver implements NotificationDriver<MailRouter, MailTransporter> {

  //
  private MailRouter router;
  
  //
  private MailTransporter transporter;
  
  public MailDriver(MailRouter router) {
    this.router = router;
    transporter = new MailTransporter();
  }
  
  @Override
  public MailRouter getRouter() {
    return router;
  }

  @Override
  public void send() throws NotificationException {
    transporter.send(this.router);
  }
 
  @Override
  public Channel getChannel() {
    return Channel.MAIL;
  }

  @Override
  public void setRouter(MailRouter router) {
    this.router = router; 
  }
}
