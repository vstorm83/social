package org.exoplatform.social.extras.injection;

import java.lang.reflect.UndeclaredThrowableException;
import java.util.HashMap;
import java.util.Random;

import org.exoplatform.commons.utils.ListAccess;
import org.exoplatform.container.PortalContainer;
import org.exoplatform.services.bench.DataInjector;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.organization.OrganizationService;
import org.exoplatform.services.organization.UserHandler;
import org.exoplatform.social.core.identity.model.Identity;
import org.exoplatform.social.core.identity.provider.OrganizationIdentityProvider;
import org.exoplatform.social.core.manager.ActivityManager;
import org.exoplatform.social.core.manager.IdentityManager;
import org.exoplatform.social.core.manager.RelationshipManager;
import org.exoplatform.social.core.profile.ProfileFilter;
import org.exoplatform.social.core.space.SpaceFilter;
import org.exoplatform.social.core.space.spi.SpaceService;
import org.exoplatform.social.core.storage.api.IdentityStorage;
import org.exoplatform.social.core.storage.impl.AbstractStorage;
import org.exoplatform.social.extras.injection.utils.LoremIpsum4J;
import org.exoplatform.social.extras.injection.utils.NameGenerator;

/**
 * @author <a href="mailto:alain.defrance@exoplatform.com">Alain Defrance</a>
 * @version $Revision$
 */
public abstract class AbstractSocialInjector extends DataInjector {

  /** . */
  private static Log LOG = ExoLogger.getLogger(IdentityInjector.class);
  
  /** . */
  private final static String DEFAULT_USER_BASE = "bench.user";

  /** . */
  private final static String DEFAULT_SPACE_BASE = "bench.space";

  /** . */
  protected final String password;

  /** . */
  protected final static String DOMAIN = "exoplatform.int";
  
  /** . */
  protected static int spaceSuffixValue = -1;
  
  /** . */
  protected static int userSuffixValue = -1;
  
  /** . */
  protected String spaceSuffixPattern = null;
  
  /** . */
  protected String userSuffixPattern = null;
  
  /** . */
  protected String userBase;

  /** . */
  protected String spaceBase;

  /** . */
  protected String spacePrettyBase ;

  /** . */
  protected int userNumber;

  /** . */
  protected int spaceNumber;

  /** . */
  protected final IdentityManager identityManager;

  /** . */
  protected final IdentityStorage identityStorage;

  /** . */
  protected final RelationshipManager relationshipManager;

  /** . */
  protected final ActivityManager activityManager;

  /** . */
  protected final OrganizationService organizationService;

  /** . */
  protected final SpaceService spaceService;

  /** . */
  protected final UserHandler userHandler;

  /** . */
  protected final Random random;

  /** . */
  protected NameGenerator nameGenerator;

  /** . */
  protected LoremIpsum4J lorem;

  protected PortalContainer container;
  
  public AbstractSocialInjector(PatternInjectorConfig config) {

    this.container = PortalContainer.getInstance();
    this.identityManager = (IdentityManager) container.getComponentInstanceOfType(IdentityManager.class);
    this.identityStorage = (IdentityStorage) container.getComponentInstanceOfType(IdentityStorage.class);
    this.relationshipManager = (RelationshipManager) container.getComponentInstanceOfType(RelationshipManager.class);
    this.activityManager = (ActivityManager) container.getComponentInstanceOfType(ActivityManager.class);
    this.spaceService = (SpaceService) container.getComponentInstanceOfType(SpaceService.class);
    this.organizationService = (OrganizationService) container.getComponentInstanceOfType(OrganizationService.class);

    //
    this.userHandler = organizationService.getUserHandler();
    this.nameGenerator = new NameGenerator();
    this.random = new Random();
    this.lorem = new LoremIpsum4J();
    
    this.spaceSuffixValue = config.getSpaceSuffixValue();
    this.userSuffixValue = config.getUserSuffixValue();
    this.password = config.getUserPasswordValue();
  }

  public void init(String userPrefix, String spacePrefix) {

    //
    userBase = (userPrefix == null ? DEFAULT_USER_BASE : userPrefix);
    spaceBase = (spacePrefix == null ? DEFAULT_SPACE_BASE : spacePrefix);
    spacePrettyBase = spaceBase.replace(".", "");

    //
    userNumber = 0;
    spaceNumber = 0;

    boolean started = AbstractStorage.startSynchronization();
    
    try {
      userNumber = userNumber(userBase);
      spaceNumber = spaceNumber(spaceBase);
    }
    catch (UndeclaredThrowableException e) {
      LOG.info("No users/spaces existing.");
      // If no user is existing, set keep 0 as value.
    }

    AbstractStorage.stopSynchronization(started);
    
    //
    LOG.info("Initial user number : " + userNumber);
    LOG.info("Initial space number : " + spaceNumber);
    
  }
  
  public void init(String userPrefix, String spacePrefix, int userSuffixLength, int spaceSuffixLength) {
    init(userPrefix, spacePrefix);
    //
    if (spaceSuffixLength > 0) {
      spaceSuffixPattern = "%s%0" + spaceSuffixLength + "d";
      LOG.info("Initial space suffix pattern : " + spaceSuffixPattern);
    } else {
      spaceSuffixPattern = null;
    }
    
    if (userSuffixLength > 0) {
      userSuffixPattern = "%s%0" + userSuffixLength + "d";
      LOG.info("Initial user suffix pattern : " + userSuffixLength);
    } else {
      userSuffixPattern = null;
    }
    
  }
  
  @Override
  public Log getLog() {
    return ExoLogger.getExoLogger(this.getClass());
  }

  @Override
  public Object execute(HashMap<String, String> stringStringHashMap) throws Exception {
    return null;
  }

  @Override
  public void reject(HashMap<String, String> stringStringHashMap) throws Exception {
  }

  private int userNumber(String base) {
    ListAccess<Identity> identities = identityManager.getIdentitiesByProfileFilter(OrganizationIdentityProvider.NAME, new ProfileFilter(), false);
    
    int num = 0, size = 0;
    try {
      size = identities.getSize();
    } catch (Exception e) {
      LOG.error(e);
    }
    if (size > 0) {
      int begin = 0, end = size > 100 ? 100 : size;
      while (begin < end && end <= size) {       
        try {
          for (Identity identity : identities.load(begin, end)) {          
            if (identity.getRemoteId().toLowerCase().startsWith(base.toLowerCase())) {
              num++;
            }
          }          
        } catch (Exception e) {
          LOG.error(e);
          break;
        }
        begin = end;
        end += 100;
        end = end > size ? size : end;
      }
    }
    return num;
  }
  
  private int spaceNumber(String base) {
    SpaceFilter filter = new SpaceFilter();
    filter.setSpaceNameSearchCondition(base);
    
    try {
      return spaceService.getAllSpacesByFilter(filter).getSize();      
    } catch (Exception e) {
      LOG.error(e);
      return 0;
    }
  }

  protected String userName() {
    return userSuffixPattern != null ? userNameSuffixPattern() : userBase + userNumber;
  }

  protected String spaceName() {
    return spaceSuffixPattern != null ? spaceNameSuffixPattern() : spaceBase + spaceNumber;
  }
  
  private String userNameSuffixPattern() {
    return String.format(userSuffixPattern, userBase, userNumber);
  }
  /**
   * Using for Unit testing to get the identity
   * @param userNumber
   * @return
   */
  protected String userNameSuffixPattern(int userNumber) {
    String result;
    if (userSuffixPattern != null) {
      result = String.format(userSuffixPattern, userBase, userNumber);
    } else {
      result = userBase + userNumber;
    }
    return result;
  }
  
  protected String spaceNameSuffixPattern() {
    return String.format(spaceSuffixPattern, spaceBase, spaceNumber);
  }
  
  protected String spaceNameSuffixPattern(int spaceNumber) {
    String result;
    if (spaceSuffixPattern != null) {
      result = String.format(spaceSuffixPattern, spacePrettyBase, spaceNumber);
    } else {
      result = spacePrettyBase + spaceNumber;
    }
    
    return result;
    
  }
  
  protected int param(HashMap<String, String> params, String name) {

    //
    if (params == null) {
      throw new NullPointerException();
    }

    //
    if (name == null) {
      throw new NullPointerException();
    }

    //
    try {
      String value = params.get(name);
      if (value != null) {
        return Integer.valueOf(value);
      }
    } catch (NumberFormatException e) {
      LOG.warn("Integer number expected for property " + name);
    }
    return 0;
    
  }
  
  
  
}
