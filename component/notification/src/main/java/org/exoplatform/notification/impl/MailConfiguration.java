package org.exoplatform.notification.impl;

public interface MailConfiguration {

  // Mail sender configuration
  public final static String NOTIFICATION_MAIL_AUTH = "exo.notification.mail.smtp.auth";

  public final static String NOTIFICATION_MAIL_HOST = "exo.notification.mail.host";

  public final static String NOTIFICATION_MAIL_PORT = "exo.notification.mail.port";

  public final static String NOTIFICATION_MAIL_SMTP_AUTH_PASSWORD = "exo.notification.mail.smtp.auth.password";

  public final static String NOTIFICATION_MAIL_SMTP_AUTH_USERNAME = "exo.notification.mail.smtp.auth.username";

  public final static String NOTIFICATION_MAIL_SMTP_SOCKETFACTORY_CLASS = "exo.notification.mail.smtp.socketFactory.class";

  public final static String NOTIFICATION_MAIL_SMTP_SOCKETFACTORY_FALLBACK =
     "exo.notification.mail.smtp.socketFactory.fallback";

  public final static String NOTIFICATION_MAIL_SMTP_SOCKETFACTORY_PORT = "exo.notification.mail.smtp.socketFactory.port";

  public final static String NOTIFICATION_MAIL_TRANSPORT_PROTOCOL = "exo.notification.mail.transport.protocol";
  
  public final static String NOTIFICATION_MAIL_SENDER = "exo.notification.mail.sender";
}
