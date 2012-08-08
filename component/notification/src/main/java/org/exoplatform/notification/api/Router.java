package org.exoplatform.notification.api;

public interface Router {

  /**
   * Specify mail headers available in {@link NotificationService}
   */
  public enum MailHeaders {
     FROM, TO, REPLY_TO, SUBJECT
  };
  
  /**
   * Specify Stream headers available in {@link NotificationService}
   */
  public enum StreamHeaders {
     FROM, TO, REPLY_TO, SUBJECT
  };
  
  /**
   * Specify Center headers available in {@link NotificationService}
   */
  public enum CenterHeaders {
     FROM, TO, REPLY_TO, SUBJECT
  };
  
  /**
   * Specify Center headers available in {@link NotificationService}
   */
  public enum MobileHeaders {
     FROM, TO, REPLY_TO, SUBJECT
  };
}
