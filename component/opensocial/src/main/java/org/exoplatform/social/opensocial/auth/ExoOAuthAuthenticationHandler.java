package org.exoplatform.social.opensocial.auth;

import javax.servlet.http.HttpServletRequest;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import net.oauth.SimpleOAuthValidator;

import org.apache.shindig.auth.AbstractSecurityToken.Keys;
import org.apache.shindig.auth.SecurityToken;
import org.apache.shindig.common.crypto.BasicBlobCrypter;
import org.apache.shindig.common.util.TimeSource;
import org.apache.shindig.social.core.oauth.OAuthAuthenticationHandler;
import org.apache.shindig.social.opensocial.oauth.OAuthDataStore;
import org.exoplatform.commons.utils.PropertyManager;
import org.exoplatform.container.PortalContainer;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

import com.google.inject.Inject;
import com.google.inject.name.Named;


/**
 * Created by IntelliJ IDEA.
 * User: zun
 * Date: Jul 7, 2010
 * Time: 5:34:35 PM
 */
public class ExoOAuthAuthenticationHandler extends OAuthAuthenticationHandler {

  /**
   * The logger.
   */
  private static final Log LOG = ExoLogger.getLogger(ExoOAuthAuthenticationHandler.class);

  private String portalContainerName;

  @Inject
  public ExoOAuthAuthenticationHandler(OAuthDataStore store,
                                       @Named("shindig.oauth.legacy-body-signing") boolean allowLegacyBodySigning) {
    // TODO Check the side effects as if we remove allowLegacyBodySigning from constructor.
    super(store, new SimpleOAuthValidator());
  }

  public String getName() {
    return super.getName();
  }

  public String getPortalContainerName() {
    if (portalContainerName == null) {
      RestPortalContainerNameConfig containerNameConfigRest = (RestPortalContainerNameConfig) PortalContainer.
              getInstance().
              getComponentInstanceOfType(RestPortalContainerNameConfig.class);
      portalContainerName = containerNameConfigRest.getContainerName();
    }

    return portalContainerName;
  }

  public SecurityToken getSecurityTokenFromRequest(HttpServletRequest request) throws InvalidAuthenticationException {
    final SecurityToken securityToken = super.getSecurityTokenFromRequest(request);

    final BasicBlobCrypter crypter;
    final String portalContainer;
    final String domain;
    try {
      String keyFile = getKeyFilePath();
      crypter = new BasicBlobCrypter(new File(keyFile));
      crypter.timeSource = new TimeSource();

      portalContainer = getPortalContainerName();
      domain = securityToken.getDomain();
    } catch (Exception e) {
      LOG.warn("Failed to get security token from request", e);
      return null;
    }

    Map<String, String> values = new HashMap<String, String>();
    values.put(Keys.APP_URL.getKey(), securityToken.getAppUrl());
    values.put(Keys.MODULE_ID.getKey(), Long.toString(securityToken.getModuleId()));
    values.put(Keys.OWNER.getKey(), securityToken.getOwnerId());
    values.put(Keys.VIEWER.getKey(), securityToken.getViewerId());
    values.put(Keys.TRUSTED_JSON.getKey(), securityToken.getTrustedJson());
    
    final ExoBlobCrypterSecurityToken crypterSecurityToken = new ExoBlobCrypterSecurityToken(portalContainer, domain, securityToken.getActiveUrl(), values);
    crypterSecurityToken.setPortalContainer(portalContainer);

    return crypterSecurityToken;
  }

  public String getWWWAuthenticateHeader(String realm) {
    return super.getWWWAuthenticateHeader(realm);
  }

  /**
   * Method returns a path to the file containing the encryption key
   */
  private String getKeyFilePath() {

    String keyPath = PropertyManager.getProperty("gatein.gadgets.securitytokenkeyfile");

    File tokenKeyFile = null;
    if (keyPath == null) {
       LOG.warn("The gadgets token key is not configured. The default key.txt file in /bin will be used");
       tokenKeyFile = new File("key.txt");
    }
    else {
       tokenKeyFile = new File(keyPath);
    }

    return tokenKeyFile.getAbsolutePath();
    
  }
  
}
