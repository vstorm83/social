package org.exoplatform.notification.impl;

import java.net.URL;

import org.apache.commons.configuration.CompositeConfiguration;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

public class InitNotificationSystem {

  private final MessageContext context;

  private final String configurationFile;
  private static Log LOG = ExoLogger.getLogger(NotificationServiceImpl.class);

  public InitNotificationSystem(InitParams params) {
    configurationFile = params.getValueParam("configuration-file").getValue();
    try {
      
      URL url = Thread.currentThread().getContextClassLoader().getResource(configurationFile);
      CompositeConfiguration configuration = new CompositeConfiguration();
      configuration.addConfiguration(new PropertiesConfiguration(url));
      configuration.setThrowExceptionOnMissing(true);

      context = new MessageContext(configuration);
    } catch (ConfigurationException e) {
      LOG.error(e.getLocalizedMessage(), e);
      throw new RuntimeException(e.getLocalizedMessage(), e);
    }

  }

  public final MessageContext getContext() {
    return context;
  }

}
