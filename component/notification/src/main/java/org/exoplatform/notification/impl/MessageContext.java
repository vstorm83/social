package org.exoplatform.notification.impl;

import org.apache.commons.configuration.Configuration;

public class MessageContext {

  private final Configuration notificationConfiguration;
  
  public MessageContext(Configuration notificationConfiguration) {
    this.notificationConfiguration = notificationConfiguration;
  }
  
  public final Configuration getConfiguration() {
    return notificationConfiguration;
  }
  
  public final Configuration setConfiguration() {
    return null;
  }
  
}
