package org.exoplatform.notification.test;

import static org.exoplatform.notification.impl.MailConfiguration.NOTIFICATION_MAIL_AUTH;
import static org.exoplatform.notification.impl.MailConfiguration.NOTIFICATION_MAIL_HOST;
import static org.exoplatform.notification.impl.MailConfiguration.NOTIFICATION_MAIL_PORT;
import static org.exoplatform.notification.impl.MailConfiguration.NOTIFICATION_MAIL_SENDER;
import static org.exoplatform.notification.impl.MailConfiguration.NOTIFICATION_MAIL_TRANSPORT_PROTOCOL;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.configuration.Configuration;
import org.exoplatform.notification.api.NotificationException;
import org.exoplatform.notification.api.NotificationService;
import org.exoplatform.notification.impl.InitNotificationSystem;
import org.exoplatform.notification.impl.MessageContext;
import org.exoplatform.notification.impl.driver.StreamDriver;
import org.exoplatform.notification.impl.driver.router.StreamRouter;
import org.exoplatform.notification.impl.driver.transport.StreamTransporter;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.social.common.RealtimeListAccess;
import org.exoplatform.social.core.activity.model.ExoSocialActivity;
import org.exoplatform.social.core.identity.model.Identity;
import org.exoplatform.social.core.identity.provider.OrganizationIdentityProvider;
import org.exoplatform.social.core.manager.ActivityManager;
import org.exoplatform.social.core.manager.IdentityManager;


public class StreamNotificationTest extends AbstractCoreTest {

  private final Log LOG = ExoLogger.getLogger(StreamNotificationTest.class);
  private List<ExoSocialActivity> tearDownActivityList;
  private Identity rootIdentity;
  private Identity johnIdentity;
  private Identity maryIdentity;
  private Identity demoIdentity;
  

  private IdentityManager identityManager;
  private ActivityManager activityManager;
  
  protected MessageContext context;
  private StreamDriver streamDriver;
  
  //
  private InitNotificationSystem notificationSystem;
  private NotificationService notificationService;

  @Override
  public void setUp() throws Exception {
    super.setUp();
    identityManager = (IdentityManager) getContainer().getComponentInstanceOfType(IdentityManager.class);
    activityManager =  (ActivityManager) getContainer().getComponentInstanceOfType(ActivityManager.class);
    notificationSystem =  (InitNotificationSystem) getContainer().getComponentInstanceOfType(InitNotificationSystem.class);
    notificationService =  (NotificationService) getContainer().getComponentInstanceOfType(NotificationService.class);
    
    tearDownActivityList = new ArrayList<ExoSocialActivity>();
    rootIdentity = identityManager.getOrCreateIdentity(OrganizationIdentityProvider.NAME, "root", false);
    johnIdentity = identityManager.getOrCreateIdentity(OrganizationIdentityProvider.NAME, "john", false);
    maryIdentity = identityManager.getOrCreateIdentity(OrganizationIdentityProvider.NAME, "mary", false);
    demoIdentity = identityManager.getOrCreateIdentity(OrganizationIdentityProvider.NAME, "demo", false);
    
    assertNotNull(notificationSystem);
    assertNotNull(notificationSystem.getContext());
    context = notificationSystem.getContext();
    initStreamDriver();
    assertNotNull(streamDriver);
    assertNotNull(streamDriver.getRouter());
    
  }

  @Override
  public void tearDown() throws Exception {
    for (ExoSocialActivity activity : tearDownActivityList) {
      try {
        activityManager.deleteActivity(activity.getId());
      } catch (Exception e) {
        LOG.warn("can not delete activity with id: " + activity.getId());
      }
    }
    identityManager.deleteIdentity(rootIdentity);
    identityManager.deleteIdentity(johnIdentity);
    identityManager.deleteIdentity(maryIdentity);
    identityManager.deleteIdentity(demoIdentity);
   
    context = null;
    streamDriver = null;
    super.tearDown();
  }
  
  public void testMessageContext() throws Exception {
    Configuration config = context.getConfiguration();
    
    assertEquals("smtp", config.getString(NOTIFICATION_MAIL_TRANSPORT_PROTOCOL));
    assertEquals("smtp.gmail.com", config.getString(NOTIFICATION_MAIL_HOST));
    assertEquals("465", config.getString(NOTIFICATION_MAIL_PORT));
    assertEquals("false", config.getString(NOTIFICATION_MAIL_AUTH));
    assertEquals("eXo Platform Account<noreply@exoplatform.com>", config.getString(NOTIFICATION_MAIL_SENDER));
  }
  
  private void initStreamDriver() {
    StreamRouter router = new StreamRouter(context);
    this.streamDriver = new StreamDriver(router, new StreamTransporter(activityManager));

  }
  
  private void createActivity() {
    Map<String, String> templateParams = new HashMap<String, String>();
    templateParams.put("ForumId", "forumId");
    templateParams.put("CateId", "categoryId");
    templateParams.put("TopicId", "topicId");
    templateParams.put("ActivityType", "activity-type");
    
    StreamRouter router = this.streamDriver.getRouter();
    router.reset();
    router.title("stream title").body("body of stream").type("ks-forum:spaces").templateParams(templateParams);
  }
  
  public void testStreamNotification() throws Exception {
    {
      performIdentityNotification(rootIdentity);
      RealtimeListAccess<ExoSocialActivity> listAccess = activityManager.getActivitiesWithListAccess(rootIdentity);
      assertEquals(1, listAccess.getSize());
      List<ExoSocialActivity> list1 = listAccess.loadAsList(0, 20);
      tearDownActivityList.addAll(list1);
      
    }
    
    {
      performIdentityNotification(johnIdentity);
      
      RealtimeListAccess<ExoSocialActivity> listAccess = activityManager.getActivitiesWithListAccess(johnIdentity);
      assertEquals(1, listAccess.getSize());
      List<ExoSocialActivity> list2 = listAccess.loadAsList(0, 20);
      tearDownActivityList.addAll(list2);
      
    }
    
    {
      performIdentityNotification(maryIdentity);
      
      RealtimeListAccess<ExoSocialActivity> listAccess = activityManager.getActivitiesWithListAccess(maryIdentity);
      assertEquals(1, listAccess.getSize());
      List<ExoSocialActivity> list3 = listAccess.loadAsList(0, 20);
      tearDownActivityList.addAll(list3);
    }
    
    {
      performIdentityNotification(demoIdentity);
      RealtimeListAccess<ExoSocialActivity> listAccess = activityManager.getActivitiesWithListAccess(demoIdentity);
      assertEquals(1, listAccess.getSize());
      List<ExoSocialActivity> list4 = listAccess.loadAsList(0, 20);
      tearDownActivityList.addAll(list4);
    }
    
    
  }
  
  
  private void performIdentityNotification(Identity identity) throws NotificationException {
    StreamRouter router = streamDriver.getRouter();
    createActivity();
    router.identity(identity);
    notificationService.publish(streamDriver);
  }
  
}
