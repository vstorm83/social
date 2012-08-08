package org.exoplatform.notification.api;

public interface Transporter<T extends Router> {

  void send(T router) throws NotificationException;
}
