package org.exoplatform.notification.impl.driver;

import org.exoplatform.notification.api.Channel;
import org.exoplatform.notification.api.NotificationDriver;
import org.exoplatform.notification.api.NotificationException;
import org.exoplatform.notification.api.Router;
import org.exoplatform.notification.api.Transporter;

public class CenterDriver implements NotificationDriver<Router, Transporter<Router>> {

  //
  private Router router;
  
  //
  private Transporter<Router> transporter;
  
  @Override
  public void send() throws NotificationException {
    transporter.send(this.router);
  }

  @Override
  public Router getRouter() {
    return router;
  }

  @Override
  public Channel getChannel() {
    return Channel.CENTER;
  }

  @Override
  public void setRouter(Router router) {
    this.router = router;        
  }

}
