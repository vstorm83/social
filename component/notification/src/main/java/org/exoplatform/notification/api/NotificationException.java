package org.exoplatform.notification.api;

public class NotificationException extends Exception {

  protected final int       status;

  private static final long serialVersionUID = -8469342047074780247L;

  public NotificationException(int status, String message) {
    super(message);
    this.status = status;
  }

  public NotificationException(int status, String message, Throwable cause) {
    super(message, cause);
    this.status = status;
  }

  public NotificationException(String message) {
    super(message);
    this.status = 500;
  }

  public NotificationException(String message, Throwable cause) {
    super(message, cause);
    this.status = 500;
  }

  public int getStatus() {
    return status;
  }

}
