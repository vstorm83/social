package org.exoplatform.notification.api;

public interface NotificationDriver<R extends Router, T extends Transporter<R>> {

  /**
   * 
   * @return
   */
  R getRouter();
  
  /**
   * 
   * @param router
   */
  void setRouter(R router);
  /**
   * 
   * @return
   */
  Channel getChannel();
  
  /**
   * 
   * @throws NotificationException
   */
  void send() throws NotificationException;
}
