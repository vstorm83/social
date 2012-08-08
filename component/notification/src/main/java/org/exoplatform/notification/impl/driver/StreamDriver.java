package org.exoplatform.notification.impl.driver;

import org.exoplatform.notification.api.Channel;
import org.exoplatform.notification.api.NotificationDriver;
import org.exoplatform.notification.api.NotificationException;
import org.exoplatform.notification.impl.driver.router.StreamRouter;
import org.exoplatform.notification.impl.driver.transport.StreamTransporter;

public class StreamDriver implements NotificationDriver<StreamRouter, StreamTransporter> {

  //
  private StreamRouter router;
  
  //
  private StreamTransporter transporter;
  
  public StreamDriver(StreamRouter streamRouter, StreamTransporter transporter) {
    this.router = streamRouter;
    this.transporter = transporter;
  }
  
  @Override
  public StreamRouter getRouter() {
    return router;
  }

  @Override
  public void send() throws NotificationException {
    transporter.send(this.router);
  }

  @Override
  public Channel getChannel() {
    return Channel.STREAM;
  }

  @Override
  public void setRouter(StreamRouter router) {
    this.router = router;
    
  }
}
