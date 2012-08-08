package org.exoplatform.notification.impl.driver.transport;

import javax.mail.MessagingException;
import javax.mail.Transport;
import javax.mail.internet.MimeMessage;

import org.exoplatform.notification.api.NotificationException;
import org.exoplatform.notification.api.Transporter;
import org.exoplatform.notification.impl.driver.router.MailRouter;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

public class MailTransporter implements Transporter<MailRouter> {

  private static Log LOG = ExoLogger.getLogger(MailTransporter.class);
  
  @Override
  public void send(MailRouter router) throws NotificationException {
    
    try {

      MimeMessage msg = router.getMessage();
      Transport.send(msg);

    } catch (MessagingException e) {
      LOG.error("Error during sending mail", e);
      throw new NotificationException(500, "Unable to send mail. Please contact support.", e);
    }
  }

}
