package org.exoplatform.notification.impl.driver.router;

import static org.exoplatform.notification.impl.MailConfiguration.NOTIFICATION_MAIL_AUTH;
import static org.exoplatform.notification.impl.MailConfiguration.NOTIFICATION_MAIL_HOST;
import static org.exoplatform.notification.impl.MailConfiguration.NOTIFICATION_MAIL_PORT;
import static org.exoplatform.notification.impl.MailConfiguration.NOTIFICATION_MAIL_SMTP_AUTH_PASSWORD;
import static org.exoplatform.notification.impl.MailConfiguration.NOTIFICATION_MAIL_SMTP_AUTH_USERNAME;
import static org.exoplatform.notification.impl.MailConfiguration.NOTIFICATION_MAIL_SMTP_SOCKETFACTORY_CLASS;
import static org.exoplatform.notification.impl.MailConfiguration.NOTIFICATION_MAIL_SMTP_SOCKETFACTORY_FALLBACK;
import static org.exoplatform.notification.impl.MailConfiguration.NOTIFICATION_MAIL_SMTP_SOCKETFACTORY_PORT;
import static org.exoplatform.notification.impl.MailConfiguration.NOTIFICATION_MAIL_TRANSPORT_PROTOCOL;
import static org.exoplatform.notification.impl.MailConfiguration.NOTIFICATION_MAIL_SENDER;


import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import javax.mail.Authenticator;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import org.apache.commons.configuration.Configuration;
import org.exoplatform.notification.api.NotificationException;
import org.exoplatform.notification.api.Router;
import org.exoplatform.notification.impl.MessageContext;
import org.exoplatform.notification.impl.utils.Deserializer;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

public class MailRouter implements Router {

  private Map<MailHeaders, String> mailHeaders;

  private Map<String, String>      templateProperties;

  private String                   template;

  private MessageContext           context;

  private static Log               LOG = ExoLogger.getLogger(MailRouter.class);

  public MailRouter(MessageContext context) {
    mailHeaders = new HashMap<MailHeaders, String>();
    this.context = context;
  }

  public MailRouter to(String to) {
    mailHeaders.put(MailHeaders.TO, to);
    return this;
  }

  public MailRouter from(String from) {
    mailHeaders.put(MailHeaders.FROM, from);
    return this;
  }

  public MailRouter subject(String subject) {
    mailHeaders.put(MailHeaders.SUBJECT, subject);
    return this;
  }

  public MailRouter replyTo(String replyTo) {
    mailHeaders.put(MailHeaders.REPLY_TO, replyTo);
    return this;
  }

  public MailRouter template(String template) {
    this.template = template;
    return this;
  }

  public MailRouter templateProperties(Map<String, String> templateProperties) {
    this.templateProperties = templateProperties;
    return this;
  }

  public MimeMessage getMessage() throws NotificationException {
    try {

      String body = resolveMessageBody(this.template, this.templateProperties);
      Session mailSession = getMailSession();
      return prepareMailMessage(body, mailSession);

    } catch (IOException e) {
      LOG.error("Error during sending mail", e);
      throw new NotificationException(500, "Unable to send mail. Please contact support.", e);
    } catch (MessagingException e) {
      LOG.error("Error during sending mail", e);
      throw new NotificationException(500, "Unable to send mail. Please contact support.", e);
    }
  }

  private MimeMessage prepareMailMessage(String body, Session mailSession) throws MessagingException,
                                                                          AddressException {
    String subject = mailHeaders.get(MailHeaders.SUBJECT);
    String from = mailHeaders.containsKey(MailHeaders.FROM) ? mailHeaders.get(MailHeaders.FROM)
                                                           : context.getConfiguration()
                                                                    .getString(NOTIFICATION_MAIL_SENDER);
    String to = mailHeaders.get(MailHeaders.TO);
    String replyTo = mailHeaders.get(MailHeaders.REPLY_TO);

    MimeMessage message = new MimeMessage(mailSession);
    message.setContent(body, "text/html");
    message.setSubject(subject);
    message.setFrom(new InternetAddress(from));
    message.addRecipients(Message.RecipientType.TO, InternetAddress.parse(to));

    if (replyTo != null) {
      message.setReplyTo(InternetAddress.parse(replyTo));
    }
    return message;
  }

  private String resolveMessageBody(String mailTemplateFile, Map<String, String> templateProperties) throws IOException,
                                                                                                    NotificationException {
    if (mailTemplateFile == null) {
      throw new NotificationException(500,
                                      "Mail template configuration not found. Please contact support.");
    }
    String templateContent = Deserializer.getResourceContent(mailTemplateFile);

    if (templateContent == null) {
      throw new NotificationException(500, "Mail template from resource " + mailTemplateFile
          + "not found. Please contact support.");
    }
    return Deserializer.resolveTemplate(templateContent, templateProperties);
  }

  private Session getMailSession() {
    Properties props = new Properties();
    Configuration configuration = this.context.getConfiguration();

    // SMTP protocol properties
    props.put("mail.transport.protocol",
              configuration.getString(NOTIFICATION_MAIL_TRANSPORT_PROTOCOL));
    props.put("mail.smtp.host", configuration.getString(NOTIFICATION_MAIL_HOST));
    props.put("mail.smtp.port", configuration.getString(NOTIFICATION_MAIL_PORT));
    props.put("mail.smtp.auth", configuration.getString(NOTIFICATION_MAIL_AUTH));

    if (Boolean.parseBoolean(props.getProperty("mail.smtp.auth"))) {
      props.put("mail.smtp.socketFactory.port",
                configuration.getProperty(NOTIFICATION_MAIL_SMTP_SOCKETFACTORY_PORT));
      props.put("mail.smtp.socketFactory.class",
                configuration.getProperty(NOTIFICATION_MAIL_SMTP_SOCKETFACTORY_CLASS));
      props.put("mail.smtp.socketFactory.fallback",
                configuration.getProperty(NOTIFICATION_MAIL_SMTP_SOCKETFACTORY_FALLBACK));

      final String mailUserName = configuration.getString(NOTIFICATION_MAIL_SMTP_AUTH_USERNAME);
      final String mailPassword = configuration.getString(NOTIFICATION_MAIL_SMTP_AUTH_PASSWORD);

      return Session.getInstance(props, new Authenticator() {
        @Override
        protected PasswordAuthentication getPasswordAuthentication() {
          return new PasswordAuthentication(mailUserName, mailPassword);
        }
      });
    } else {
      return Session.getInstance(props);
    }
  }
}
