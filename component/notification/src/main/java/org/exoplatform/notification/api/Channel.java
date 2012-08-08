package org.exoplatform.notification.api;

public enum Channel {
  MAIL("Mail"),
  STREAM("Stream"),
  CENTER("Center"),
  MOBILE("Mobile");
  
  private final String name;

  private Channel(final String name) {
    this.name = name;
  }
}
