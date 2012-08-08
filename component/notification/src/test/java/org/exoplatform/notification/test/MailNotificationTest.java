package org.exoplatform.notification.test;

import static org.exoplatform.notification.impl.MailConfiguration.NOTIFICATION_MAIL_AUTH;
import static org.exoplatform.notification.impl.MailConfiguration.NOTIFICATION_MAIL_HOST;
import static org.exoplatform.notification.impl.MailConfiguration.NOTIFICATION_MAIL_PORT;
import static org.exoplatform.notification.impl.MailConfiguration.NOTIFICATION_MAIL_SENDER;
import static org.exoplatform.notification.impl.MailConfiguration.NOTIFICATION_MAIL_TRANSPORT_PROTOCOL;

import java.util.Collections;
import java.util.Iterator;

import junit.framework.TestCase;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.exoplatform.notification.api.NotificationDriver;
import org.exoplatform.notification.api.NotificationService;
import org.exoplatform.notification.impl.MessageContext;
import org.exoplatform.notification.impl.NotificationServiceImpl;
import org.exoplatform.notification.impl.driver.MailDriver;
import org.exoplatform.notification.impl.driver.router.MailRouter;
import org.exoplatform.notification.impl.driver.transport.MailTransporter;
import org.exoplatform.notification.impl.utils.AvailablePortFinder;

import com.dumbster.smtp.SimpleSmtpServer;
import com.dumbster.smtp.SmtpMessage;
import com.google.common.collect.ImmutableMap;

public class MailNotificationTest extends TestCase {

  protected MessageContext                                  context;

  protected Configuration                                   adminConfiguration;

  protected NotificationDriver<MailRouter, MailTransporter> mailDriver;

  private SimpleSmtpServer                                  server;

  private NotificationService                               service;

  private String                                            template = "test-mail-sender-template.html";

  @Override
  protected void setUp() throws Exception {
    super.setUp();

    initEnvironment();
    initMailDriver();
  }

  @Override
  protected void tearDown() throws Exception {
    super.tearDown();
    cleanMails();
  }

  private void initEnvironment() {
    int smtpPort = AvailablePortFinder.getNextAvailable(2048);
    server = SimpleSmtpServer.start(smtpPort);

    adminConfiguration = new PropertiesConfiguration();
    adminConfiguration.addProperty(NOTIFICATION_MAIL_TRANSPORT_PROTOCOL, "smtp");
    adminConfiguration.addProperty(NOTIFICATION_MAIL_HOST, "localhost");
    adminConfiguration.addProperty(NOTIFICATION_MAIL_PORT, Integer.toString(smtpPort));
    adminConfiguration.addProperty(NOTIFICATION_MAIL_AUTH, "false");
    adminConfiguration.addProperty(NOTIFICATION_MAIL_SENDER, "cloud.admin@localhost");

    context = new MessageContext(adminConfiguration);

    service = new NotificationServiceImpl();
  }

  private void initMailDriver() {
    MailRouter router = new MailRouter(context);
    router.to("test@localhost")
          .subject("test")
          .template(template)
          .templateProperties(Collections.<String, String> emptyMap());

    this.mailDriver = new MailDriver(router);

  }

  private void cleanMails() {
    if (server.getReceivedEmailSize() > 0) {
      Iterator it = server.getReceivedEmail();
      while (it.hasNext()) {
        it.next();
        it.remove();
      }
    }
  }

  private SmtpMessage getLastMessage() {
    @SuppressWarnings("rawtypes")
    Iterator mailIterator = server.getReceivedEmail();
    SmtpMessage smtpMessage = (SmtpMessage) mailIterator.next();
    return smtpMessage;
  }

  public void testSendMessage() throws Exception {
    service.publish(mailDriver);
    assertEquals(1, server.getReceivedEmailSize());
    assertEquals(getLastMessage().getHeaderValue("To"), "test@localhost");
  }

  public void testSendMessageWithFrom() throws Exception {

    MailRouter router = new MailRouter(context);
    router.to("test@localhost")
          .from("admin@exoplatform.com")
          .subject("test")
          .template(template)
          .templateProperties(Collections.<String, String> emptyMap());

    mailDriver.setRouter(router);
    service.publish(mailDriver);

    // then
    assertEquals(getLastMessage().getHeaderValue("From"), "admin@exoplatform.com");
  }

  public void testSendMessageWithFormTemplate() throws Exception {
    service.publish(mailDriver);

    // then
    assertEquals(getLastMessage().getBody(), "Test template body; test parameter - ${parameter}");
  }

  public void testSendMessageWithFormParam() throws Exception {

    MailRouter router = new MailRouter(context);
    router.to("test@localhost")
          .from("admin@exoplatform.com")
          .subject("test")
          .template(template)
          .templateProperties(ImmutableMap.<String, String> of("parameter", "parameter value"));
    mailDriver.setRouter(router);
    
    service.publish(mailDriver);

    // then
    assertEquals(getLastMessage().getBody(), "Test template body; test parameter - parameter value");
  }

}
