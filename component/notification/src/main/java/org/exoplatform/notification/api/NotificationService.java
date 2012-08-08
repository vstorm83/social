package org.exoplatform.notification.api;


public interface NotificationService {
  
  void publish(NotificationDriver<?,?>...drivers) throws NotificationException;

}
