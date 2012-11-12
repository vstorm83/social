/*
 * Copyright (C) 2003-2011 eXo Platform SAS.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.exoplatform.social.core.space.spi;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import org.apache.commons.lang.ArrayUtils;
import org.exoplatform.commons.utils.ListAccess;
import org.exoplatform.commons.utils.PageList;
import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.organization.OrganizationService;
import org.exoplatform.services.organization.User;
import org.exoplatform.social.common.RealtimeListAccess;
import org.exoplatform.social.core.activity.model.ExoSocialActivity;
import org.exoplatform.social.core.identity.model.Identity;
import org.exoplatform.social.core.identity.provider.OrganizationIdentityProvider;
import org.exoplatform.social.core.identity.provider.SpaceIdentityProvider;
import org.exoplatform.social.core.manager.ActivityManager;
import org.exoplatform.social.core.manager.IdentityManager;
import org.exoplatform.social.core.model.AvatarAttachment;
import org.exoplatform.social.core.service.LinkProvider;
import org.exoplatform.social.core.space.SpaceException;
import org.exoplatform.social.core.space.SpaceFilter;
import org.exoplatform.social.core.space.SpaceUtils;
import org.exoplatform.social.core.space.impl.DefaultSpaceApplicationHandler;
import org.exoplatform.social.core.space.model.Space;
import org.exoplatform.social.core.storage.api.IdentityStorage;
import org.exoplatform.social.core.test.AbstractCoreTest;

public class SpaceServiceTest extends AbstractCoreTest {

  private final Log           LOG              = ExoLogger.getLogger(SpaceServiceTest.class);

  private static final String DEMO             = "demo";
  private static final String TOM              = "tom";
  private static final String RAUL             = "raul";
  private static final String GHOST            = "ghost";
  private static final String DRAGON           = "dragon";
  private static final String REGISTER_1        = "register1";
  private static final String JOHN             = "john";
  private static final String MARY             = "mary";
  private static final String HARRY            = "harry";
  private static final String ROOT             = "root";
  private static final String JAME             = "jame";
  private static final String PAUL             = "paul";
  private static final String HACKER           = "hacker";
  private static final String HEAR_BREAKER     = "hearBreaker";
  private static final String NEW_INVITED_USER = "newInvitedUser";
  private static final String NEW_PENDING_USER = "newPendingUser";
  private static final String USER_NEW         = "user-new";
  private static final String USER_NEW_1       = "user-new.1";
  private static final String USER_NEW_DOT     = "user.new";
  private static final String ANONYMOUS        = "anonymous";
  
  private Identity            demo             = new Identity(OrganizationIdentityProvider.NAME, DEMO);
  private Identity            tom              = new Identity(OrganizationIdentityProvider.NAME, TOM);
  private Identity            raul             = new Identity(OrganizationIdentityProvider.NAME, RAUL);
  private Identity            ghost            = new Identity(OrganizationIdentityProvider.NAME, GHOST);
  private Identity            dragon           = new Identity(OrganizationIdentityProvider.NAME, DRAGON);
  private Identity            register1        = new Identity(OrganizationIdentityProvider.NAME, REGISTER_1);
  private Identity            john             = new Identity(OrganizationIdentityProvider.NAME, JOHN);
  private Identity            mary             = new Identity(OrganizationIdentityProvider.NAME, MARY);
  private Identity            harry            = new Identity(OrganizationIdentityProvider.NAME, HARRY);
  private Identity            root             = new Identity(OrganizationIdentityProvider.NAME, ROOT);
  private Identity            jame             = new Identity(OrganizationIdentityProvider.NAME, JAME);
  private Identity            paul             = new Identity(OrganizationIdentityProvider.NAME, PAUL);
  private Identity            hacker           = new Identity(OrganizationIdentityProvider.NAME, HACKER);
  private Identity            hearBreaker      = new Identity(OrganizationIdentityProvider.NAME, HEAR_BREAKER);
  private Identity            newInvitedUser   = new Identity(OrganizationIdentityProvider.NAME, NEW_INVITED_USER);
  private Identity            newPendingUser   = new Identity(OrganizationIdentityProvider.NAME, NEW_PENDING_USER);
  private Identity            user_new         = new Identity(OrganizationIdentityProvider.NAME, USER_NEW);
  private Identity            user_new_1       = new Identity(OrganizationIdentityProvider.NAME, USER_NEW_1);
  private Identity            user_new_dot     = new Identity(OrganizationIdentityProvider.NAME, USER_NEW_DOT);
  
  private SpaceService        spaceService;
  private IdentityStorage     identityStorage;
  private List<Space>         tearDownSpaceList;
  private List<Identity>      tearDownUserList;

  @Override
  public void setUp() throws Exception {
    super.setUp();
    spaceService = (SpaceService) getContainer().getComponentInstanceOfType(SpaceService.class);
    identityStorage = (IdentityStorage) getContainer().getComponentInstanceOfType(IdentityStorage.class);
    tearDownSpaceList = new ArrayList<Space>();
    tearDownUserList = new ArrayList<Identity>();
  }

  @Override
  public void tearDown() throws Exception {
    end();
    begin();

    for (Identity identity : tearDownUserList) {
      identityStorage.deleteIdentity(identity);
    }
    for (Space space : tearDownSpaceList) {
      Identity spaceIdentity = identityStorage.findIdentity(SpaceIdentityProvider.NAME, space.getPrettyName());
      if (spaceIdentity != null) {
        identityStorage.deleteIdentity(spaceIdentity);
      }
      spaceService.deleteSpace(space);
    }
    super.tearDown();
  }

  /**
   * Test {@link SpaceService#getAllSpaces()}
   *
   * @throws Exception
   */
  public void testGetAllSpaces() throws Exception {
    //
    createIdentities(new Identity[] { root, demo, john, mary, tom, harry });
    tearDownSpaceList.add(populateData());
    tearDownSpaceList.add(createMoreSpace("Space2"));

    //
    assertEquals(2, spaceService.getAllSpacesWithListAccess().getSize());
  }

  /**
   * Test {@link SpaceService#getAllSpacesWithListAccess()}
   *
   * @throws Exception
   * @since 1.2.0-GA
   */
  public void testGetAllSpacesWithListAccess() throws Exception {
    //
    createIdentities(new Identity[] { demo, tom, raul, ghost, dragon, register1, mary, jame, paul, hacker });
    for (int i = 0; i < 20; i ++) {
      tearDownSpaceList.add(this.getSpaceInstance(i));
    }

    //
    ListAccess<Space> allSpaces = spaceService.getAllSpacesWithListAccess();
    assertNotNull(allSpaces);
    assertEquals(20, allSpaces.getSize());
    assertEquals(1, allSpaces.load(0, 1).length);
    assertEquals(20, allSpaces.load(0, 20).length);
  }

  /**
   * Test {@link SpaceService#getSpaces(String)}
   *
   * @throws Exception
   * @since 1.2.0-GA
   */
  public void testGetSpacesByUserId() throws Exception {
    //
    createIdentities(new Identity[] { demo, tom, raul, ghost, dragon, register1, mary, jame, paul, hacker });
    for (int i = 0; i < 20; i ++) {
      tearDownSpaceList.add(this.getSpaceInstance(i));
    }

    ListAccess<Space> memberSpaces = spaceService.getMemberSpaces(RAUL);
    assertNotNull(memberSpaces);
    assertEquals(20, memberSpaces.getSize());

    memberSpaces = spaceService.getMemberSpaces(GHOST);
    assertNotNull(memberSpaces);
    assertEquals(20, memberSpaces.getSize());

    memberSpaces = spaceService.getMemberSpaces(DRAGON);
    assertNotNull(memberSpaces);
    assertEquals(20, memberSpaces.getSize());

    memberSpaces = spaceService.getMemberSpaces(ANONYMOUS);
    assertNotNull(memberSpaces);
    assertEquals(0, memberSpaces.getSize());
  }

  /**
   * Test {@link SpaceService#getSpaceByDisplayName(String)}
   *
   * @throws Exception
   */
  public void testGetSpaceByDisplayName() throws Exception {
    //
    createIdentities(new Identity[] { root, demo, john, mary, tom, harry });
    Space space = populateData();
    tearDownSpaceList.add(space);

    //
    Space got = spaceService.getSpaceByDisplayName("Space1");
    assertNotNull(got);
    assertEquals(space.getDisplayName(), got.getDisplayName());
  }

  public void testGetSpaceByName() throws Exception {
    //
    createIdentities(new Identity[] { demo, tom, raul, ghost, dragon, register1, mary, jame, paul, hacker });
    for (int i = 0; i < 20; i ++) {
      tearDownSpaceList.add(this.getSpaceInstance(i));
    }

    //
    Space got = spaceService.getSpaceByPrettyName("my_space_10");
    assertNotNull(got);
    assertEquals("my space 10", got.getDisplayName());
    assertEquals("my_space_10", got.getPrettyName());

    //
    got = spaceService.getSpaceByPrettyName("my_space_0");
    assertNotNull(got);
    assertEquals("my space 0", got.getDisplayName());
    assertEquals("my_space_0", got.getPrettyName());

    //
    got = spaceService.getSpaceByPrettyName("my_space_20");
    assertNull(got);
  }

  /**
   * Test {@link SpaceService#getSpaceByPrettyName(String)}
   *
   * @throws Exception
   * @since 1.2.0-GA
   */
  public void testGetSpaceByPrettyName() throws Exception {
    //
    createIdentities(new Identity[] { demo, tom, raul, ghost, dragon, register1, mary, jame, paul, hacker });
    for (int i = 0; i < 20; i ++) {
      tearDownSpaceList.add(this.getSpaceInstance(i));
    }

    //
    Space got = spaceService.getSpaceByPrettyName("my_space_10");
    assertNotNull(got);
    assertEquals("my space 10", got.getDisplayName());
    assertEquals("my_space_10", got.getPrettyName());

    //
    got = spaceService.getSpaceByPrettyName("my_space_0");
    assertNotNull(got);
    assertEquals("my space 0", got.getDisplayName());
    assertEquals("my_space_0", got.getPrettyName());

    //
    got = spaceService.getSpaceByPrettyName("my_space_20");
    assertNull(got);
  }

  /**
   * Test {@link SpaceService#getAllSpacesByFilter(SpaceFilter)}Improve Space Unit Test in Social 4.x
   *
   * @throws Exception
   * @since 1.2.0-GA
   */
  public void testGetAllSpacesByFilterWithFirstCharacterOfSpaceName() throws Exception {
    //
    createIdentities(new Identity[] { demo, tom, raul, ghost, dragon, register1, mary, jame, paul, hacker });
    for (int i = 0; i < 20; i ++) {
      tearDownSpaceList.add(this.getSpaceInstance(i));
    }

    //
    ListAccess<Space> got = spaceService.getAllSpacesByFilter(new SpaceFilter('m'));
    assertNotNull(got);
    assertEquals(20, got.getSize());

    //
    got = spaceService.getAllSpacesByFilter(new SpaceFilter('M'));
    assertNotNull(got);
    assertEquals(20, got.getSize());
    assertEquals(1, got.load(0, 1).length);
    assertEquals(20, got.load(0, 20).length);
    got = spaceService.getAllSpacesByFilter(new SpaceFilter('H'));
    assertNotNull(got);
    assertEquals(0, got.getSize());

    //
    got = spaceService.getAllSpacesByFilter(new SpaceFilter('k'));
    assertNotNull(got);
    assertEquals(0, got.getSize());

    //
    got = spaceService.getAllSpacesByFilter(new SpaceFilter('*'));
    assertNotNull(got);
    assertEquals(0, got.getSize());
  }

  /**
   * Test {@link SpaceService#getAllSpacesByFilter(SpaceFilter)}
   *
   * @throws Exception
   * @since 1.2.0-GA
   */
  public void testGetAllSpacesByFilterWithSpaceNameSearchCondition() throws Exception {
    //
    createIdentities( new Identity[] { demo, tom, raul, ghost, dragon, register1, mary, jame, paul, hacker });
    for (int i = 0; i < 20; i ++) {
      tearDownSpaceList.add(this.getSpaceInstance(i));
    }

    //
    ListAccess<Space> got = spaceService.getAllSpacesByFilter(new SpaceFilter("my space"));
    assertNotNull(got);
    assertEquals(20, got.getSize());
    assertEquals(1, got.load(0, 1).length);
    assertEquals(20, got.load(0, 20).length);

    //
    got = spaceService.getAllSpacesByFilter(new SpaceFilter("1"));
    assertEquals(11, got.getSize());
    assertEquals(10, got.load(0, 10).length);

    //
    got = spaceService.getAllSpacesByFilter(new SpaceFilter("add new space"));
    assertNotNull(got);
    assertEquals(20, got.getSize());

    //
    got = spaceService.getAllSpacesByFilter(new SpaceFilter("space"));
    assertNotNull(got);
    assertEquals(20, got.getSize());

    //
    got = spaceService.getAllSpacesByFilter(new SpaceFilter("*space"));
    assertNotNull(got);
    assertEquals(20, got.getSize());

    //
    got = spaceService.getAllSpacesByFilter(new SpaceFilter("*space*"));
    assertNotNull(got);
    assertEquals(20, got.getSize());

    //
    got = spaceService.getAllSpacesByFilter(new SpaceFilter("*a*e*"));
    assertNotNull(got);
    assertEquals(20, got.getSize());

    //
    got = spaceService.getAllSpacesByFilter(new SpaceFilter("*a*e"));
    assertNotNull(got);
    assertEquals(20, got.getSize());

    //
    got = spaceService.getAllSpacesByFilter(new SpaceFilter("a*e"));
    assertNotNull(got);
    assertEquals(20, got.getSize());

    //
    got = spaceService.getAllSpacesByFilter(new SpaceFilter("a*"));
    assertNotNull(got);
    assertEquals(20, got.getSize());

    //
    got = spaceService.getAllSpacesByFilter(new SpaceFilter("%a%e%"));
    assertNotNull(got);
    assertEquals(20, got.getSize());

    //
    got = spaceService.getAllSpacesByFilter(new SpaceFilter("%a*e%"));
    assertNotNull(got);
    assertEquals(20, got.getSize());

    //
    got = spaceService.getAllSpacesByFilter(new SpaceFilter("%a*e*"));
    assertNotNull(got);
    assertEquals(20, got.getSize());

    //
    got = spaceService.getAllSpacesByFilter(new SpaceFilter("***"));
    assertNotNull(got);
    assertEquals(0, got.getSize());

    //
    got = spaceService.getAllSpacesByFilter(new SpaceFilter("%%%%%"));
    assertNotNull(got);
    assertEquals(0, got.getSize());

    //
    got = spaceService.getAllSpacesByFilter(new SpaceFilter("new"));
    assertNotNull(got);
    assertEquals(20, got.getSize());
    
    //
    got = spaceService.getAllSpacesByFilter(new SpaceFilter("<new>new(\"new\")</new>"));
    assertNotNull(got);
    assertEquals(20, got.getSize());

    //
    got = spaceService.getAllSpacesByFilter(new SpaceFilter("what new space add"));
    assertNotNull(got);
    assertEquals(0, got.getSize());
  }

  /**
   * Test {@link SpaceService#getSpaceByGroupId(String)}
   *
   * @throws Exception
   * @since 1.2.0-GA
   */
  public void testGetSpaceByGroupId() throws Exception {
    //
    createIdentities(new Identity[] { demo, tom, raul, ghost, dragon, register1, mary, jame, paul, hacker });
    for (int i = 0; i < 20; i ++) {
      tearDownSpaceList.add(this.getSpaceInstance(i));
    }

    //
    Space got = spaceService.getSpaceByGroupId("/space/space0");
    assertNotNull(got);
    assertEquals("my space 0", got.getDisplayName());
    assertEquals("/space/space0", got.getGroupId());
  }

  /**
   * Test {@link SpaceService#getSpaceById(String)}
   *
   * @throws Exception
   */
  public void testGetSpaceById() throws Exception {
    //
    createIdentities(new Identity[] { root, demo, john, mary, tom, harry });
    Space space = populateData();
    tearDownSpaceList.add(space);
    tearDownSpaceList.add(createMoreSpace("Space2"));

    //
    assertEquals(space.getDisplayName(), spaceService.getSpaceById(space.getId()).getDisplayName());
  }

  /**
   * Test {@link SpaceService#getSpaceByUrl(String)}
   *
   * @throws Exception
   */
  public void testGetSpaceByUrl() throws Exception {
    //
    createIdentities(new Identity[] { root, demo, john, mary, tom, harry });
    Space space = populateData();
    tearDownSpaceList.add(space);

    //
    assertEquals(space.getDisplayName(), spaceService.getSpaceByUrl("space1").getDisplayName());
  }

  /**
   * Test {@link SpaceService#getSettingableSpaces(String))}
   *
   * @throws Exception
   * @since 1.2.0-GA
   */
  public void testGetSettingableSpaces() throws Exception {
    //
    createIdentities(new Identity[] { demo, tom, raul, ghost, dragon, register1, mary, jame, paul, hacker });
    for (int i = 0; i < 20; i ++) {
      tearDownSpaceList.add(this.getSpaceInstance(i));
    }

    //
    ListAccess<Space> got = spaceService.getSettingableSpaces(DEMO);
    assertNotNull(got);
    assertEquals(20, got.getSize());
    assertEquals(1, got.load(0, 1).length);
    assertEquals(20, got.load(0, 20).length);

    //
    got = spaceService.getSettingableSpaces(TOM);
    assertNotNull(got);
    assertEquals(20, got.getSize());

    got = spaceService.getSettingableSpaces(ROOT);
    assertNotNull(got);
    assertEquals(20, got.getSize());

    got = spaceService.getSettingableSpaces(RAUL);
    assertNotNull(got);
    assertEquals(0, got.getSize());

    got = spaceService.getSettingableSpaces(GHOST);
    assertNotNull(got);
    assertEquals(0, got.getSize());
  }

  /**
   * Test {@link SpaceService#getSettingabledSpacesByFilter(String, SpaceFilter)}
   *
   * @throws Exception
   * @since 1.2.0-GA
   */
  public void testGetSettingableSpacesByFilter() throws Exception {
    //
    createIdentities(new Identity[] { demo, tom, raul, ghost, dragon, register1, mary, jame, paul, hacker });
    for (int i = 0; i < 20; i ++) {
      tearDownSpaceList.add(this.getSpaceInstance(i));
    }

    //
    ListAccess<Space> got = spaceService.getSettingabledSpacesByFilter(DEMO, new SpaceFilter("add"));
    assertNotNull(got);
    assertEquals(20, got.getSize());
    assertEquals(1, got.load(0, 1).length);
    assertEquals(20, got.load(0, 20).length);
    got = spaceService.getSettingabledSpacesByFilter(demo.getRemoteId(), new SpaceFilter("19"));
    assertEquals(1, got.getSize());
    assertEquals(1, got.load(0, 1).length);

    //
    got = spaceService.getSettingabledSpacesByFilter(DEMO, new SpaceFilter("my"));
    assertNotNull(got);
    assertEquals(20, got.getSize());

    //
    got = spaceService.getSettingabledSpacesByFilter(DEMO, new SpaceFilter("new"));
    assertNotNull(got);
    assertEquals(20, got.getSize());

    //
    got = spaceService.getSettingabledSpacesByFilter(DEMO, new SpaceFilter('m'));
    assertNotNull(got);
    assertEquals(20, got.getSize());

    //
    got = spaceService.getSettingabledSpacesByFilter(DEMO, new SpaceFilter('M'));
    assertNotNull(got);
    assertEquals(20, got.getSize());

    //
    got = spaceService.getSettingabledSpacesByFilter(DEMO, new SpaceFilter('k'));
    assertNotNull(got);
    assertEquals(0, got.getSize());

    //
    got = spaceService.getSettingabledSpacesByFilter(TOM, new SpaceFilter("new"));
    assertNotNull(got);
    assertEquals(20, got.getSize());

    //
    got = spaceService.getSettingabledSpacesByFilter(ROOT, new SpaceFilter("space"));
    assertNotNull(got);
    assertEquals(20, got.getSize());

    //
    got = spaceService.getSettingabledSpacesByFilter(RAUL, new SpaceFilter("my"));
    assertNotNull(got);
    assertEquals(0, got.getSize());

    //
    got = spaceService.getSettingabledSpacesByFilter(GHOST, new SpaceFilter("space"));
    assertNotNull(got);
    assertEquals(0, got.getSize());
  }

  /**
   * Test {@link SpaceService#getInvitedSpacesWithListAccess(String)}
   *
   * @throws Exception
   * @since 1.2.0-GA
   */
  public void testGetInvitedSpacesWithListAccess() throws Exception {
    //
    createIdentities(new Identity[] { demo, tom, raul, ghost, dragon, register1, mary, jame, paul, hacker });
    for (int i = 0; i < 20; i ++) {
      tearDownSpaceList.add(this.getSpaceInstance(i));
    }

    //
    ListAccess<Space> got = spaceService.getInvitedSpacesWithListAccess(REGISTER_1);
    assertNotNull(got);
    assertEquals(20, got.getSize());
    assertEquals(1, got.load(0, 1).length);
    assertEquals(20, got.load(0, 20).length);

    //
    got = spaceService.getInvitedSpacesWithListAccess(MARY);
    assertNotNull(got);
    assertEquals(20, got.getSize());

    //
    got = spaceService.getInvitedSpacesWithListAccess(DEMO);
    assertNotNull(got);
    assertEquals(0, got.getSize());
  }

  /**
   * Test {@link SpaceService#getInvitedSpacesByFilter(String, SpaceFilter)}
   *
   * @throws Exception
   * @since 1.2.0-GA
   */
  public void testGetInvitedSpacesByFilterWithSpaceNameSearchCondition() throws Exception {
    //
    createIdentities(new Identity[] { demo, tom, raul, ghost, dragon, register1, mary, jame, paul, hacker });
    for (int i = 0; i < 20; i ++) {
      tearDownSpaceList.add(this.getSpaceInstance(i));
    }
    
    //
    ListAccess<Space> got = spaceService.getInvitedSpacesByFilter(REGISTER_1, new SpaceFilter("my space"));
    assertNotNull(got);
    assertEquals(20, got.getSize());

    //
    got = spaceService.getInvitedSpacesByFilter(register1.getRemoteId(), new SpaceFilter("12"));
    assertEquals(1, got.getSize());
    assertEquals(1, got.load(0, 1).length);

    //
    got = spaceService.getInvitedSpacesByFilter(MARY, new SpaceFilter("my"));
    assertNotNull(got);
    assertEquals(20, got.getSize());
    assertEquals(1, got.load(0, 1).length);
    assertEquals(20, got.load(0, 20).length);

    //
    got = spaceService.getInvitedSpacesByFilter(MARY, new SpaceFilter("*my"));
    assertNotNull(got);
    assertEquals(20, got.getSize());

    //
    got = spaceService.getInvitedSpacesByFilter(MARY, new SpaceFilter("*my*"));
    assertNotNull(got);
    assertEquals(20, got.getSize());

    //
    got = spaceService.getInvitedSpacesByFilter(MARY, new SpaceFilter("*my*e*"));
    assertNotNull(got);
    assertEquals(20, got.getSize());

    //
    got = spaceService.getInvitedSpacesByFilter(MARY, new SpaceFilter("%my%e%"));
    assertNotNull(got);
    assertEquals(20, got.getSize());

    //
    got = spaceService.getInvitedSpacesByFilter(MARY, new SpaceFilter("%my%e"));
    assertNotNull(got);
    assertEquals(20, got.getSize());

    //
    got = spaceService.getInvitedSpacesByFilter(MARY, new SpaceFilter("%my*e%"));
    assertNotNull(got);
    assertEquals(20, got.getSize());

    //
    got = spaceService.getInvitedSpacesByFilter(MARY, new SpaceFilter("%my*e*"));
    assertNotNull(got);
    assertEquals(20, got.getSize());

    //
    got = spaceService.getInvitedSpacesByFilter(MARY, new SpaceFilter("****"));
    assertNotNull(got);
    assertEquals(0, got.getSize());

    //
    got = spaceService.getInvitedSpacesByFilter(MARY, new SpaceFilter("%%%%%"));
    assertNotNull(got);
    assertEquals(0, got.getSize());

    //
    got = spaceService.getInvitedSpacesByFilter(MARY, new SpaceFilter("my space"));
    assertNotNull(got);
    assertEquals(20, got.getSize());

    //
    got = spaceService.getInvitedSpacesByFilter(DEMO, new SpaceFilter("add new"));
    assertNotNull(got);
    assertEquals(0, got.getSize());

    //
    got = spaceService.getInvitedSpacesByFilter(JOHN, new SpaceFilter("space"));
    assertNotNull(got);
    assertEquals(0, got.getSize());
  }

  /**
   * Test {@link SpaceService#getInvitedSpacesByFilter(String, SpaceFilter)}
   *
   * @throws Exception
   * @since 1.2.0-GA
   */
  public void testGetInvitedSpacesByFilterWithFirstCharacterOfSpaceName() throws Exception {
    //
    createIdentities(new Identity[] { demo, tom, raul, ghost, dragon, register1, mary, jame, paul, hacker });
    for (int i = 0; i < 20; i ++) {
      tearDownSpaceList.add(this.getSpaceInstance(i));
    }

    //
    ListAccess<Space> got = spaceService.getInvitedSpacesByFilter(REGISTER_1, new SpaceFilter('m'));
    assertNotNull(got);
    assertEquals(20, got.getSize());
    assertEquals(1, got.load(0, 1).length);
    assertEquals(20, got.load(0, 20).length);

    //
    got = spaceService.getInvitedSpacesByFilter(MARY, new SpaceFilter('M'));
    assertNotNull(got);
    assertEquals(20, got.getSize());

    //
    got = spaceService.getInvitedSpacesByFilter(MARY, new SpaceFilter('H'));
    assertNotNull(got);
    assertEquals(0, got.getSize());

    //
    got = spaceService.getInvitedSpacesByFilter(DEMO, new SpaceFilter('m'));
    assertNotNull(got);
    assertEquals(0, got.getSize());

    //
    got = spaceService.getInvitedSpacesByFilter(JOHN, new SpaceFilter('M'));
    assertNotNull(got);
    assertEquals(0, got.getSize());
  }

  /**
   * Test {@link SpaceService#getPublicSpacesWithListAccess(String)}
   *
   * @throws Exception
   * @since 1.2.0-GA
   */
  public void testGetPublicSpacesWithListAccess() throws Exception {
    //
    createIdentities(new Identity[] { demo, tom, raul, ghost, dragon, register1, mary, jame, paul, hacker });
    for (int i = 0; i < 20; i ++) {
      tearDownSpaceList.add(this.getSpaceInstance(i));
    }

    //
    ListAccess<Space> got = spaceService.getPublicSpacesWithListAccess(TOM);
    assertNotNull(got);
    assertEquals(0, got.getSize());

    //
    got = spaceService.getPublicSpacesWithListAccess(HACKER);
    assertNotNull(got);
    assertEquals(0, got.getSize());

    //
    got = spaceService.getPublicSpacesWithListAccess(MARY);
    assertNotNull(got);
    assertEquals(0, got.getSize());

    //
    got = spaceService.getPublicSpacesWithListAccess(ROOT);
    assertNotNull(got);
    assertEquals(0, got.getSize());

    //
    got = spaceService.getPublicSpacesWithListAccess(ANONYMOUS);
    assertNotNull(got);
    assertEquals(20, got.getSize());
    assertEquals(1, got.load(0, 1).length);
    assertEquals(20, got.load(0, 20).length);

    //
    got = spaceService.getPublicSpacesWithListAccess("bluray");
    assertNotNull(got);
    assertEquals(20, got.getSize());
  }

  /**
   * Test {@link SpaceService#getPublicSpacesByFilter(String, SpaceFilter)}
   *
   * @throws Exception
   * @since 1.2.0-GA
   */
  public void testGetPublicSpacesByFilterWithSpaceNameSearchCondition() throws Exception {
    //
    createIdentities(new Identity[] { demo, tom, raul, ghost, dragon, register1, mary, jame, paul, hacker, hearBreaker });
    for (int i = 0; i < 20; i ++) {
      tearDownSpaceList.add(this.getSpaceInstance(i));
    }

    //
    String nameSpace = "my space";
    ListAccess<Space> got = spaceService.getPublicSpacesByFilter(TOM, new SpaceFilter(nameSpace));
    assertNotNull(got);
    assertEquals(0, got.getSize());

    //
    got = spaceService.getPublicSpacesByFilter(HEAR_BREAKER, new SpaceFilter(nameSpace));
    assertNotNull(got);
    assertEquals(20, got.getSize());
    assertEquals(1, got.load(0, 1).length);
    assertEquals(20, got.load(0, 20).length);

    //
    got = spaceService.getPublicSpacesByFilter(HEAR_BREAKER, new SpaceFilter("*m"));
    assertNotNull(got);
    assertEquals(20, got.getSize());

    //
    got = spaceService.getPublicSpacesByFilter(HEAR_BREAKER, new SpaceFilter("m*"));
    assertNotNull(got);
    assertEquals(20, got.getSize());

    //
    got = spaceService.getPublicSpacesByFilter(HEAR_BREAKER, new SpaceFilter("*my*"));
    assertNotNull(got);
    assertEquals(20, got.getSize());

    //
    got = spaceService.getPublicSpacesByFilter(HEAR_BREAKER, new SpaceFilter("*my*e"));
    assertNotNull(got);
    assertEquals(20, got.getSize());

    //
    got = spaceService.getPublicSpacesByFilter(HEAR_BREAKER, new SpaceFilter("*my*e*"));
    assertNotNull(got);
    assertEquals(20, got.getSize());

    //
    got = spaceService.getPublicSpacesByFilter(HEAR_BREAKER, new SpaceFilter("%my%e%"));
    assertNotNull(got);
    assertEquals(20, got.getSize());

    //
    got = spaceService.getPublicSpacesByFilter(HEAR_BREAKER, new SpaceFilter("%my*e%"));
    assertNotNull(got);
    assertEquals(20, got.getSize());

    //
    got = spaceService.getPublicSpacesByFilter(HEAR_BREAKER, new SpaceFilter("*my%e%"));
    assertNotNull(got);
    assertEquals(20, got.getSize());

    //
    got = spaceService.getPublicSpacesByFilter(HEAR_BREAKER, new SpaceFilter("***"));
    assertNotNull(got);
    assertEquals(0, got.getSize());

    //
    got = spaceService.getPublicSpacesByFilter(HEAR_BREAKER, new SpaceFilter("%%%"));
    assertNotNull(got);
    assertEquals(0, got.getSize());

    //
    nameSpace = "my space 1";
    got = spaceService.getPublicSpacesByFilter(ANONYMOUS, new SpaceFilter(""));
    assertNotNull(got);
    assertEquals(0, got.getSize());

    //
    nameSpace = "my space 20";
    got = spaceService.getPublicSpacesByFilter(HEAR_BREAKER, new SpaceFilter(nameSpace));
    assertNotNull(got);
    assertEquals(0, got.getSize());
  }

  /**
   * Test {@link SpaceService#getPublicSpacesByFilter(String, SpaceFilter)}
   *
   * @throws Exception
   * @since 1.2.0-GA
   */
  public void testGetPublicSpacesByFilterWithFirstCharacterOfSpaceName() throws Exception {
    //
    createIdentities(new Identity[] { demo, tom, raul, ghost, dragon, register1, mary, jame, paul, hacker, john });
    for (int i = 0; i < 10; i ++) {
      tearDownSpaceList.add(this.getSpaceInstance(i));
    }

    //
    ListAccess<Space> got = spaceService.getPublicSpacesByFilter(ANONYMOUS, new SpaceFilter('m'));
    assertNotNull(got);
    assertEquals(10, got.getSize());

    //
    got = spaceService.getPublicSpacesByFilter(ANONYMOUS, new SpaceFilter('M'));
    assertNotNull(got);
    assertEquals(10, got.getSize());

    //
    got = spaceService.getPublicSpacesByFilter(ROOT, new SpaceFilter('M'));
    assertNotNull(got);
    assertEquals(0, got.getSize());

    //
    got = spaceService.getPublicSpacesByFilter(ANONYMOUS, new SpaceFilter('*'));
    assertNotNull(got);
    assertEquals(0, got.getSize());

    //
    got = spaceService.getPublicSpacesByFilter(TOM, new SpaceFilter('M'));
    assertNotNull(got);
    assertEquals(0, got.getSize());

    //
    got = spaceService.getPublicSpacesByFilter(ANONYMOUS, new SpaceFilter('y'));
    assertNotNull(got);
    assertEquals(0, got.getSize());

    //
    got = spaceService.getPublicSpacesByFilter(ANONYMOUS, new SpaceFilter('H'));
    assertNotNull(got);
    assertEquals(0, got.getSize());

    //
    ListAccess<Space> johnPublicSpaces = spaceService.getPublicSpacesByFilter(JOHN, new SpaceFilter('m'));
    assertEquals(10, johnPublicSpaces.getSize());
    assertEquals(1, johnPublicSpaces.load(0, 1).length);
    Space[] johnPublicSpacesArray = johnPublicSpaces.load(0, 10);
    assertEquals(10, johnPublicSpacesArray.length);
    assertNotNull(johnPublicSpacesArray[0].getId());
    assertNotNull(johnPublicSpacesArray[0].getPrettyName());
  }

  /**
   * Test {@link SpaceService#getAccessibleSpacesWithListAccess(String)}
   *
   * @throws Exception
   * @since 1.2.0-GA
   */
  public void testGetAccessibleSpacesWithListAccess() throws Exception {
    //
    createIdentities(new Identity[] { demo, tom, raul, ghost, dragon, register1, mary, jame, paul, hacker });
    for (int i = 0; i < 20; i ++) {
      tearDownSpaceList.add(this.getSpaceInstance(i));
    }

    //
    ListAccess<Space> got = spaceService.getAccessibleSpacesWithListAccess(DEMO);
    assertNotNull(got);
    assertEquals(20, got.getSize());
    assertEquals(1, got.load(0, 1).length);
    assertEquals(20, got.load(0, 20).length);

    //
    got = spaceService.getAccessibleSpacesWithListAccess(TOM);
    assertNotNull(got);
    assertEquals(20, got.getSize());

    //
    got = spaceService.getAccessibleSpacesWithListAccess(ROOT);
    assertNotNull(got);
    assertEquals(20, got.getSize());

    //
    got = spaceService.getAccessibleSpacesWithListAccess(DRAGON);
    assertNotNull(got);
    assertEquals(20, got.getSize());

    //
    got = spaceService.getAccessibleSpacesWithListAccess(GHOST);
    assertNotNull(got);
    assertEquals(20, got.getSize());

    //
    got = spaceService.getAccessibleSpacesWithListAccess(RAUL);
    assertNotNull(got);
    assertEquals(20, got.getSize());

    //
    got = spaceService.getAccessibleSpacesWithListAccess(MARY);
    assertNotNull(got);
    assertEquals(0, got.getSize());

    //
    got = spaceService.getAccessibleSpacesWithListAccess(JOHN);
    assertNotNull(got);
    assertEquals(0, got.getSize());
  }

  /**
   * Test {@link SpaceService#getAccessibleSpacesByFilter(String, SpaceFilter)}
   *
   * @throws Exception
   * @since 1.2.0-GA
   */
  public void testGetAccessibleSpacesByFilterWithSpaceNameSearchCondition() throws Exception {
    //
    createIdentities(new Identity[] { demo, tom, raul, ghost, dragon, register1, mary, jame, paul, hacker });
    for (int i = 0; i < 20; i ++) {
      tearDownSpaceList.add(this.getSpaceInstance(i));
    }

    //
    ListAccess<Space> got = spaceService.getAccessibleSpacesByFilter(DEMO, new SpaceFilter("my"));
    assertNotNull(got);
    assertEquals(20, got.getSize());
    assertEquals(1, got.load(0, 1).length);
    assertEquals(20, got.load(0, 20).length);

    //
    got = spaceService.getAccessibleSpacesByFilter(TOM, new SpaceFilter("space"));
    assertNotNull(got);
    assertEquals(20, got.getSize());

    //
    got = spaceService.getAccessibleSpacesByFilter(ROOT, new SpaceFilter("space"));
    assertNotNull(got);
    assertEquals(20, got.getSize());

    //
    got = spaceService.getAccessibleSpacesByFilter(ROOT, new SpaceFilter("*space"));
    assertNotNull(got);
    assertEquals(20, got.getSize());

    //
    got = spaceService.getAccessibleSpacesByFilter(ROOT, new SpaceFilter("space*"));
    assertNotNull(got);
    assertEquals(20, got.getSize());

    //
    got = spaceService.getAccessibleSpacesByFilter(ROOT, new SpaceFilter("*space*"));
    assertNotNull(got);
    assertEquals(20, got.getSize());

    //
    got = spaceService.getAccessibleSpacesByFilter(ROOT, new SpaceFilter("*a*e*"));
    assertNotNull(got);
    assertEquals(20, got.getSize());

    //
    got = spaceService.getAccessibleSpacesByFilter(ROOT, new SpaceFilter("%a%e%"));
    assertNotNull(got);
    assertEquals(20, got.getSize());

    //
    got = spaceService.getAccessibleSpacesByFilter(ROOT, new SpaceFilter("%a*e%"));
    assertNotNull(got);
    assertEquals(20, got.getSize());

    //
    got = spaceService.getAccessibleSpacesByFilter(ROOT, new SpaceFilter("%a*e*"));
    assertNotNull(got);
    assertEquals(20, got.getSize());

    //
    got = spaceService.getAccessibleSpacesByFilter(ROOT, new SpaceFilter("*****"));
    assertNotNull(got);
    assertEquals(0, got.getSize());

    //
    got = spaceService.getAccessibleSpacesByFilter(ROOT, new SpaceFilter("%%%%%%%"));
    assertNotNull(got);
    assertEquals(0, got.getSize());

    //
    got = spaceService.getAccessibleSpacesByFilter(ROOT, new SpaceFilter("add new"));
    assertNotNull(got);
    assertEquals(20, got.getSize());

    //
    got = spaceService.getAccessibleSpacesByFilter(DRAGON, new SpaceFilter("my space"));
    assertNotNull(got);
    assertEquals(20, got.getSize());

    //
    got = spaceService.getAccessibleSpacesByFilter(DRAGON, new SpaceFilter("add new"));
    assertNotNull(got);
    assertEquals(20, got.getSize());

    //
    got = spaceService.getAccessibleSpacesByFilter(GHOST, new SpaceFilter("my space "));
    assertNotNull(got);
    assertEquals(20, got.getSize());

    //
    got = spaceService.getAccessibleSpacesByFilter(ANONYMOUS, new SpaceFilter("my space"));
    assertNotNull(got);
    assertEquals(0, got.getSize());
  }

  /**
   * Test {@link SpaceService#getAccessibleSpacesByFilter(String, SpaceFilter)}
   *
   * @throws Exception
   * @since 1.2.0-GA
   */
  public void testGetAccessibleSpacesByFilterWithFirstCharacterOfSpaceName() throws Exception {
    //
    createIdentities(new Identity[] { demo, tom, raul, ghost, dragon, register1, mary, jame, paul, hacker });
    for (int i = 0; i < 20; i ++) {
      tearDownSpaceList.add(this.getSpaceInstance(i));
    }

    //
    ListAccess<Space> got = spaceService.getAccessibleSpacesByFilter(DEMO, new SpaceFilter('m'));
    assertNotNull(got);
    assertEquals(20, got.getSize());
    assertEquals(1, got.load(0, 1).length);
    assertEquals(20, got.load(0, 20).length);

    //
    got = spaceService.getAccessibleSpacesByFilter(TOM, new SpaceFilter('M'));
    assertNotNull(got);
    assertEquals(20, got.getSize());

    //
    got = spaceService.getAccessibleSpacesByFilter(ROOT, new SpaceFilter('M'));
    assertNotNull(got);
    assertEquals(20, got.getSize());

    //
    got = spaceService.getAccessibleSpacesByFilter(ROOT, new SpaceFilter('*'));
    assertNotNull(got);
    assertEquals(0, got.getSize());

    //
    got = spaceService.getAccessibleSpacesByFilter(TOM, new SpaceFilter('h'));
    assertNotNull(got);
    assertEquals(0, got.getSize());

    //
    got = spaceService.getAccessibleSpacesByFilter(DRAGON, new SpaceFilter('m'));
    assertNotNull(got);
    assertEquals(20, got.getSize());

    //
    got = spaceService.getAccessibleSpacesByFilter(GHOST, new SpaceFilter('M'));
    assertNotNull(got);
    assertEquals(20, got.getSize());

    //
    got = spaceService.getAccessibleSpacesByFilter(ANONYMOUS, new SpaceFilter('m'));
    assertNotNull(got);
    assertEquals(0, got.getSize());
  }

  /**
   * Test {@link SpaceService#getMemberSpaces(String)}
   *
   * @throws Exception
   * @since 1.2.0-GA
   */
  public void testGetMemberSpaces() throws Exception {
    //
    createIdentities(new Identity[] { demo, tom, raul, ghost, dragon, register1, mary, jame, paul, hacker });
    for (int i = 0; i < 20; i ++) {
      tearDownSpaceList.add(this.getSpaceInstance(i));
    }

    //
    ListAccess<Space> got = spaceService.getMemberSpaces(RAUL);
    assertNotNull(got);
    assertEquals(20, got.getSize());
    assertEquals(1, got.load(0, 1).length);
    assertEquals(20, got.load(0, 20).length);

    //
    got = spaceService.getMemberSpaces(GHOST);
    assertNotNull(got);
    assertEquals(20, got.getSize());

    //
    got = spaceService.getMemberSpaces(DRAGON);
    assertNotNull(got);
    assertEquals(20, got.getSize());

    //
    got = spaceService.getMemberSpaces(ROOT);
    assertNotNull(got);
    assertEquals(0, got.getSize());
  }

  /**
   * Test {@link SpaceService#getMemberSpacesByFilter(String, SpaceFilter)}
   *
   * @throws Exception
   * @since 1.2.0-GA
   */
  public void testGetMemberSpacesByFilter() throws Exception {
    //
    createIdentities(new Identity[] { demo, tom, raul, ghost, dragon, register1, mary, jame, paul, hacker });
    for (int i = 0; i < 20; i ++) {
      tearDownSpaceList.add(this.getSpaceInstance(i));
    }

    //
    ListAccess<Space> got = spaceService.getMemberSpacesByFilter(RAUL, new SpaceFilter("add"));
    assertNotNull(got);
    assertEquals(20, got.getSize());
    assertEquals(1, got.load(0, 1).length);
    assertEquals(20, got.load(0, 20).length);

    //
    got = spaceService.getMemberSpacesByFilter(RAUL, new SpaceFilter("new"));
    assertNotNull(got);
    assertEquals(20, got.getSize());

    //
    got = spaceService.getMemberSpacesByFilter(RAUL, new SpaceFilter("space"));
    assertNotNull(got);
    assertEquals(20, got.getSize());

    //
    got = spaceService.getMemberSpacesByFilter(RAUL, new SpaceFilter("my"));
    assertNotNull(got);
    assertEquals(20, got.getSize());

    //
    got = spaceService.getMemberSpacesByFilter(RAUL, new SpaceFilter('m'));
    assertNotNull(got);
    assertEquals(20, got.getSize());

    //
    got = spaceService.getMemberSpacesByFilter(RAUL, new SpaceFilter('M'));
    assertNotNull(got);
    assertEquals(20, got.getSize());

    //
    got = spaceService.getMemberSpacesByFilter(RAUL, new SpaceFilter('k'));
    assertNotNull(got);
    assertEquals(0, got.getSize());

    //
    got = spaceService.getMemberSpacesByFilter(GHOST, new SpaceFilter("my"));
    assertNotNull(got);
    assertEquals(20, got.getSize());

    //
    got = spaceService.getMemberSpacesByFilter(DRAGON, new SpaceFilter("space"));
    assertNotNull(got);
    assertEquals(20, got.getSize());

    //
    got = spaceService.getMemberSpacesByFilter(ROOT, new SpaceFilter("my space"));
    assertNotNull(got);
    assertEquals(0, got.getSize());
  }

  /**
   * Test {@link SpaceService#getPendingSpaces(String)}
   *
   * @throws Exception
   */
  public void testGetPendingSpaces() throws Exception {
    //
    createIdentities(new Identity[] { root, demo, john, mary, tom, harry });
    tearDownSpaceList.add(populateData());
    
    //
    Space got = spaceService.getSpaceByDisplayName("Space1");
    spaceService.addPendingUser(got, DEMO);
    assertEquals(true, spaceService.isPendingUser(got, DEMO));
  }

  /**
   * Test {@link SpaceService#getPendingSpacesWithListAccess(String)}
   *
   * @throws Exception
   * @since 1.2.0-GA
   */
  public void testGetPendingSpacesWithListAccess() throws Exception {
    //
    createIdentities(new Identity[] { demo, tom, raul, ghost, dragon, register1, mary, jame, paul, hacker });
    for (int i = 0; i < 20; i ++) {
      tearDownSpaceList.add(this.getSpaceInstance(i));
    }

    //
    ListAccess<Space> got = spaceService.getPendingSpacesWithListAccess(JAME);
    assertNotNull(got);
    assertEquals(20, got.getSize());
    assertEquals(1, got.load(0, 1).length);
    assertEquals(20, got.load(0, 20).length);
    
    //
    got = spaceService.getPendingSpacesWithListAccess(PAUL);
    assertNotNull(got);
    assertEquals(20, got.getSize());
    
    //
    got = spaceService.getPendingSpacesWithListAccess(HACKER);
    assertNotNull(got);
    assertEquals(20, got.getSize());
    
    //
    got = spaceService.getPendingSpacesWithListAccess(GHOST);
    assertNotNull(got);
    assertEquals(0, got.getSize());
    
    //
    got = spaceService.getPendingSpacesWithListAccess(ANONYMOUS);
    assertNotNull(got);
    assertEquals(0, got.getSize());
  }

  /**
   * Test {@link SpaceService#getPendingSpacesByFilter(String, SpaceFilter))}
   *
   * @throws Exception
   * @since 1.2.0-GA
   */
  public void testGetPendingSpacesByFilterWithSpaceNameSearchCondition() throws Exception {
    //
    createIdentities(new Identity[] { demo, tom, raul, ghost, dragon, register1, mary, jame, paul, hacker });
    for (int i = 0; i < 20; i ++) {
      tearDownSpaceList.add(this.getSpaceInstance(i));
    }

    //
    String nameSpace = "my space";
    ListAccess<Space> got = spaceService.getPendingSpacesByFilter(JAME, new SpaceFilter(nameSpace));
    assertNotNull(got);
    assertEquals(20, got.getSize());
    assertEquals(1, got.load(0, 1).length);
    assertEquals(20, got.load(0, 20).length);

    //
    got = spaceService.getPendingSpacesByFilter(PAUL, new SpaceFilter(nameSpace));
    assertNotNull(got);
    assertEquals(20, got.getSize());

    //
    got = spaceService.getPendingSpacesByFilter(HACKER, new SpaceFilter("space"));
    assertNotNull(got);
    assertEquals(20, got.getSize());

    //
    got = spaceService.getPendingSpacesByFilter(JAME, new SpaceFilter("add new"));
    assertNotNull(got);
    assertEquals(20, got.getSize());

    //
    got = spaceService.getPendingSpacesByFilter(JAME, new SpaceFilter("add*"));
    assertNotNull(got);
    assertEquals(20, got.getSize());

    //
    got = spaceService.getPendingSpacesByFilter(JAME, new SpaceFilter("*add*"));
    assertNotNull(got);
    assertEquals(20, got.getSize());

    //
    got = spaceService.getPendingSpacesByFilter(JAME, new SpaceFilter("*add"));
    assertNotNull(got);
    assertEquals(20, got.getSize());

    //
    got = spaceService.getPendingSpacesByFilter(JAME, new SpaceFilter("*add*e"));
    assertNotNull(got);
    assertEquals(20, got.getSize());

    //
    got = spaceService.getPendingSpacesByFilter(JAME, new SpaceFilter("*add*e*"));
    assertNotNull(got);
    assertEquals(20, got.getSize());

    //
    got = spaceService.getPendingSpacesByFilter(JAME, new SpaceFilter("%add%e%"));
    assertNotNull(got);
    assertEquals(20, got.getSize());

    //
    got = spaceService.getPendingSpacesByFilter(JAME, new SpaceFilter("%add*e%"));
    assertNotNull(got);
    assertEquals(20, got.getSize());

    //
    got = spaceService.getPendingSpacesByFilter(JAME, new SpaceFilter("%add*e*"));
    assertNotNull(got);
    assertEquals(20, got.getSize());

    //
    got = spaceService.getPendingSpacesByFilter(JAME, new SpaceFilter("no space"));
    assertNotNull(got);
    assertEquals(0, got.getSize());
  }

  /**
   * Test {@link SpaceService#getPendingSpacesByFilter(String, SpaceFilter)}
   *
   * @throws Exception
   * @since 1.2.0-GA
   */
  public void testGetPendingSpacesByFilterWithFirstCharacterOfSpaceName() throws Exception {
    //
    createIdentities(new Identity[] { demo, tom, raul, ghost, dragon, register1, mary, jame, paul, hacker });
    for (int i = 0; i < 20; i ++) {
      tearDownSpaceList.add(this.getSpaceInstance(i));
    }

    //
    ListAccess<Space> got = spaceService.getPendingSpacesByFilter(JAME, new SpaceFilter('m'));
    assertNotNull(got);
    assertEquals(20, got.getSize());
    assertEquals(1, got.load(0, 1).length);
    assertEquals(20, got.load(0, 20).length);

    //
    got = spaceService.getPendingSpacesByFilter(PAUL, new SpaceFilter('M'));
    assertNotNull(got);
    assertEquals(20, got.getSize());

    //
    got = spaceService.getPendingSpacesByFilter(JAME, new SpaceFilter('*'));
    assertNotNull(got);
    assertEquals(0, got.getSize());

    //
    got = spaceService.getPendingSpacesByFilter(JAME, new SpaceFilter('H'));
    assertNotNull(got);
    assertEquals(0, got.getSize());

    //
    got = spaceService.getPendingSpacesByFilter(JAME, new SpaceFilter('k'));
    assertNotNull(got);
    assertEquals(0, got.getSize());
  }

  /**
   * Test {@link SpaceService#createSpace(Space, String)}
   *
   * @throws Exception
   */
  public void testCreateSpace() throws Exception {
    //
    createIdentities(new Identity[] { root, demo, john, mary, tom, harry });
    tearDownSpaceList.add(populateData());
    tearDownSpaceList.add(createMoreSpace("Space2"));

    //
    ListAccess<Space> got = spaceService.getAllSpacesWithListAccess();
    assertNotNull(got);
    assertEquals(2, got.getSize());
  }

  /**
   * Test {@link SpaceService#saveSpace(Space, boolean)}
   *
   * @throws Exception
   * @since 1.2.0-GA
   */
  public void testSaveSpace() throws Exception {
    //
    createIdentities(new Identity[] { demo, tom, raul, ghost, dragon, register1, mary, jame, paul, hacker });
    Space space = this.getSpaceInstance(0);
    tearDownSpaceList.add(space);

    //
    Space got = spaceService.getSpaceByDisplayName(space.getDisplayName());
    assertNotNull(got);
    assertEquals(space.getDisplayName(), got.getDisplayName());
    assertEquals(space.getDescription(), got.getDescription());
    assertEquals(space.getGroupId(), got.getGroupId());
    assertEquals(null, got.getAvatarUrl());
  }

  /**
   * Test {@link SpaceService#renameSpace(Space, String)}
   *
   * @throws Exception
   * @since 1.2.8
   */
  public void testRenameSpace() throws Exception {
    //
    createIdentities(new Identity[] { demo, tom, raul, ghost, dragon, register1, mary, jame, paul, hacker });
    Space space = this.getSpaceInstance(0);
    tearDownSpaceList.add(space);

    //
    Identity identity = new Identity(SpaceIdentityProvider.NAME, space.getPrettyName());
    identityStorage.saveIdentity(identity);
    tearDownUserList.add(identity);

    //
    String newDisplayName = "new display name";
    spaceService.renameSpace(space, newDisplayName);
    Space got = spaceService.getSpaceById(space.getId());
    assertEquals(newDisplayName, got.getDisplayName());
    Identity savedIdentity = identityStorage.findIdentity(SpaceIdentityProvider.NAME, space.getPrettyName());
    assertNotNull(savedIdentity);
  }

  /**
   * Test {@link SpaceService#saveSpace(Space, boolean)}
   *
   * @throws Exception
   * @since 1.2.0-GA
   */
  public void testUpdateSpaceAvatar() throws Exception {
    //
    createIdentities(new Identity[] { demo, tom, raul, ghost, dragon, register1, mary, jame, paul, hacker });
    Space space = this.getSpaceInstance(0);
    Identity spaceIdentity = new Identity(SpaceIdentityProvider.NAME, space.getPrettyName());
    identityStorage.saveIdentity(spaceIdentity);
    tearDownSpaceList.add(space);
    tearDownUserList.add(spaceIdentity);

    //
    InputStream inputStream = getClass().getResourceAsStream("/eXo-Social.png");
    AvatarAttachment avatarAttachment = new AvatarAttachment(null, "avatar", "png", inputStream, null, System.currentTimeMillis());
    space.setAvatarAttachment(avatarAttachment);
    spaceService.updateSpaceAvatar(space);
    spaceService.updateSpace(space);

    //
    Space got = spaceService.getSpaceById(space.getId());
    assertFalse(got.getAvatarUrl() == null);
    String avatarRandomURL = got.getAvatarUrl();
    int indexOfRandomVar = avatarRandomURL.indexOf("/?upd=");
    String avatarURL = null;
    if(indexOfRandomVar != -1){
      avatarURL = avatarRandomURL.substring(0,indexOfRandomVar);
    } else {
      avatarURL = avatarRandomURL;
    }
    assertEquals(LinkProvider.escapeJCRSpecialCharacters(String.format("/rest/jcr/repository/portal-test/production/soc:providers/soc:space/soc:%s/soc:profile/soc:avatar",
                                                                       space.getPrettyName())),
                                                         avatarURL);
  }

  /**
   * Test {@link SpaceService#deleteSpace(Space)}
   *
   * @throws Exception
   * @since 1.2.0-GA
   */
  public void testDeleteSpace() throws Exception {
    //
    createIdentities(new Identity[] { demo, tom, raul, ghost, dragon, register1, mary, jame, paul, hacker });
    Space space = this.getSpaceInstance(0);

    //
    String spaceDisplayName = space.getDisplayName();
    Space got = spaceService.getSpaceByDisplayName(spaceDisplayName);
    assertNotNull(got);
    assertEquals(spaceDisplayName, got.getDisplayName());
    assertEquals(space.getDescription(), got.getDescription());
    assertEquals(space.getGroupId(), got.getGroupId());

    //
    spaceService.deleteSpace(space);
    got = spaceService.getSpaceByDisplayName(spaceDisplayName);
    assertNull(got);
  }

  /**
   * Test {@link SpaceService#updateSpace(Space)}
   *
   * @throws Exception
   * @since 1.2.0-GA
   */
  public void testUpdateSpace() throws Exception {
    //
    createIdentities(new Identity[] { demo, tom, raul, ghost, dragon, register1, mary, jame, paul, hacker });
    Space space = this.getSpaceInstance(0);
    tearDownSpaceList.add(space);

    //
    String spaceDisplayName = space.getDisplayName();
    String spaceDescription = space.getDescription();
    String groupId = space.getGroupId();
    Space got = spaceService.getSpaceByDisplayName(spaceDisplayName);
    assertNotNull(got);
    assertEquals(spaceDisplayName, got.getDisplayName());
    assertEquals(spaceDescription, got.getDescription());
    assertEquals(groupId, got.getGroupId());

    //
    String updateSpaceDisplayName = "update new space display name";
    space.setDisplayName(updateSpaceDisplayName);
    space.setPrettyName(space.getDisplayName());
    spaceService.updateSpace(space);
    got = spaceService.getSpaceByDisplayName(updateSpaceDisplayName);
    assertNotNull(got);
    assertEquals(updateSpaceDisplayName, got.getDisplayName());
    assertEquals(spaceDescription, got.getDescription());
    assertEquals(groupId, got.getGroupId());
  }

  /**
   * Test {@link SpaceService#addPendingUser(Space, String)}
   *
   * @throws Exception
   * @since 1.2.0-GA
   */
  public void testAddPendingUser() throws Exception {
    //
    createIdentities(new Identity[] { demo, tom, raul, ghost, dragon, register1, mary, jame, paul, hacker, newPendingUser });
    Space space = this.getSpaceInstance(0);
    tearDownSpaceList.add(space);

    //
    int pendingUsersCount = space.getPendingUsers().length;
    assertFalse(ArrayUtils.contains(space.getPendingUsers(), newPendingUser.getRemoteId()));
    spaceService.addPendingUser(space, newPendingUser.getRemoteId());
    space = spaceService.getSpaceByDisplayName(space.getDisplayName());
    assertEquals(pendingUsersCount + 1, space.getPendingUsers().length);
    assertTrue(ArrayUtils.contains(space.getPendingUsers(), newPendingUser.getRemoteId()));
  }

  /**
   * Test {@link SpaceService#removePendingUser(Space, String)}
   *
   * @throws Exception
   * @since 1.2.0-GA
   */
  public void testRemovePendingUser() throws Exception {
    //
    createIdentities(new Identity[] { demo, tom, raul, ghost, dragon, register1, mary, jame, paul, hacker, newPendingUser });
    Space space = this.getSpaceInstance(0);
    tearDownSpaceList.add(space);

    //
    int pendingUsersCount = space.getPendingUsers().length;
    assertFalse(ArrayUtils.contains(space.getPendingUsers(), newPendingUser.getRemoteId()));
    spaceService.addPendingUser(space, newPendingUser.getRemoteId());
    space = spaceService.getSpaceByDisplayName(space.getDisplayName());
    assertEquals(pendingUsersCount + 1, space.getPendingUsers().length);
    assertTrue(ArrayUtils.contains(space.getPendingUsers(), newPendingUser.getRemoteId()));

    //
    spaceService.removePendingUser(space, newPendingUser.getRemoteId());
    space = spaceService.getSpaceByDisplayName(space.getDisplayName());
    assertEquals(pendingUsersCount, space.getPendingUsers().length);
    assertFalse(ArrayUtils.contains(space.getPendingUsers(), newPendingUser.getRemoteId()));
  }

  /**
   * Test {@link SpaceService#isPendingUser(Space, String)}
   *
   * @throws Exception@since 1.2.0-GA
   * @since 1.2.0-GA
   */
  public void testIsPendingUser() throws Exception {
    //
    createIdentities(new Identity[] { demo, tom, raul, ghost, dragon, register1, mary, jame, paul, hacker });
    Space space = this.getSpaceInstance(0);
    tearDownSpaceList.add(space);

    //
    Space got = spaceService.getSpaceByDisplayName(space.getDisplayName());
    assertNotNull(got);
    assertTrue(spaceService.isPendingUser(got, JAME));
    assertTrue(spaceService.isPendingUser(got, PAUL));
    assertTrue(spaceService.isPendingUser(got, HACKER));
    assertFalse(spaceService.isPendingUser(got, NEW_PENDING_USER));
  }

  /**
   * Test {@link SpaceService#addInvitedUser(Space, String)}
   *
   * @throws Exception
   *
   */
  public void testAddInvitedUser() throws Exception {
    //
    createIdentities(new Identity[] { demo, tom, raul, ghost, dragon, register1, mary, jame, paul, hacker, newInvitedUser });
    Space space = this.getSpaceInstance(0);
    tearDownSpaceList.add(space);

    //
    Space got = spaceService.getSpaceByDisplayName(space.getDisplayName());
    assertNotNull(got);
    int invitedUsersCount = got.getInvitedUsers().length;
    assertFalse(ArrayUtils.contains(got.getInvitedUsers(), newInvitedUser.getRemoteId()));
    spaceService.addInvitedUser(got, newInvitedUser.getRemoteId());
    got = spaceService.getSpaceByDisplayName(space.getDisplayName());
    assertEquals(invitedUsersCount + 1, got.getInvitedUsers().length);
    assertTrue(ArrayUtils.contains(got.getInvitedUsers(), newInvitedUser.getRemoteId()));
  }

  /**
   * Test {@link SpaceService#removeInvitedUser(Space, String)}
   *
   * @throws Exception
   * @since 1.2.0-GA
   */
  public void testRemoveInvitedUser() throws Exception {
    //
    createIdentities(new Identity[] { demo, tom, raul, ghost, dragon, register1, mary, jame, paul, hacker, newInvitedUser });
    Space space = this.getSpaceInstance(0);
    tearDownSpaceList.add(space);

    //
    Space got = spaceService.getSpaceByDisplayName(space.getDisplayName());
    assertNotNull(got);
    int invitedUsersCount = got.getInvitedUsers().length;
    assertFalse(ArrayUtils.contains(got.getInvitedUsers(), newInvitedUser.getRemoteId()));
    spaceService.addInvitedUser(got, newInvitedUser.getRemoteId());
    got = spaceService.getSpaceByDisplayName(space.getDisplayName());
    assertEquals(invitedUsersCount + 1, got.getInvitedUsers().length);
    assertTrue(ArrayUtils.contains(got.getInvitedUsers(), newInvitedUser.getRemoteId()));
    spaceService.removeInvitedUser(got, newInvitedUser.getRemoteId());
    got = spaceService.getSpaceByDisplayName(space.getDisplayName());
    assertEquals(invitedUsersCount, got.getInvitedUsers().length);
    assertFalse(ArrayUtils.contains(got.getInvitedUsers(), newInvitedUser.getRemoteId()));
  }

  /**
   * Test {@link SpaceService#isInvitedUser(Space, String)}
   *
   * @throws Exception
   * @since 1.2.0-GA
   */
  public void testIsInvitedUser() throws Exception {
    //
    createIdentities(new Identity[] { demo, tom, raul, ghost, dragon, register1, mary, jame, paul, hacker });
    Space space = this.getSpaceInstance(0);
    tearDownSpaceList.add(space);

    //
    Space got = spaceService.getSpaceByDisplayName(space.getDisplayName());
    assertNotNull(got);
    assertTrue(spaceService.isInvitedUser(got, REGISTER_1));
    assertTrue(spaceService.isInvitedUser(got, MARY));
    assertFalse(spaceService.isInvitedUser(got, HACKER));
    assertFalse(spaceService.isInvitedUser(got, ANONYMOUS));
  }

  /**
   * Test {@link SpaceService#setManager(Space, String, boolean)}
   *
   * @throws Exception
   * @since 1.2.0-GA
   */
  public void testSetManager() throws Exception {
    //
    createIdentities(new Identity[] { demo, tom, raul, ghost, dragon, register1, mary, jame, paul, hacker, john });

    //
    int number = 0;
    Space space = new Space();
    space.setDisplayName("my space " + number);
    space.setPrettyName(space.getDisplayName());
    space.setRegistration(Space.OPEN);
    space.setDescription("add new space " + number);
    space.setType(DefaultSpaceApplicationHandler.NAME);
    space.setVisibility(Space.PUBLIC);
    space.setRegistration(Space.VALIDATION);
    space.setPriority(Space.INTERMEDIATE_PRIORITY);
    space.setGroupId("/space/space" + number);
    space.setUrl(space.getPrettyName());
    String[] spaceManagers = new String[] {DEMO, TOM};
    String[] members = new String[] {RAUL, GHOST, DRAGON};
    String[] invitedUsers = new String[] {REGISTER_1, MARY};
    String[] pendingUsers = new String[] {JAME, PAUL, HACKER};
    space.setInvitedUsers(invitedUsers);
    space.setPendingUsers(pendingUsers);
    space.setManagers(spaceManagers);
    space.setMembers(members);
    space = this.createSpaceNonInitApps(space, DEMO, null);
    tearDownSpaceList.add(space);

    //
    Space got = spaceService.getSpaceByDisplayName(space.getDisplayName());
    assertNotNull(got);
    int managers = got.getManagers().length;
    spaceService.setManager(got, DEMO, true);
    got = spaceService.getSpaceByDisplayName(space.getDisplayName());
    assertEquals(managers, got.getManagers().length);

    //
    spaceService.setManager(got, JOHN, true);
    got = spaceService.getSpaceByDisplayName(space.getDisplayName());
    assertEquals(managers + 1, got.getManagers().length);

    //
    spaceService.setManager(got, DEMO, false);
    got = spaceService.getSpaceByDisplayName(space.getDisplayName());
    assertEquals(managers, got.getManagers().length);

    // Wait 3 secs to have activity stored
    try {
      IdentityManager identityManager = (IdentityManager) getContainer().getComponentInstanceOfType(IdentityManager.class);
      ActivityManager activityManager = (ActivityManager) getContainer().getComponentInstanceOfType(ActivityManager.class);
      Thread.sleep(3000);
      RealtimeListAccess<ExoSocialActivity> broadCastActivities = activityManager.getActivitiesWithListAccess(identityManager.getOrCreateIdentity(SpaceIdentityProvider.NAME, got.getPrettyName(), false));
      List<ExoSocialActivity> listBroadCastActivities = broadCastActivities.loadAsList(0, 10);
      for (ExoSocialActivity activity : listBroadCastActivities) {
        activityManager.deleteActivity(activity);
      }
    } catch (InterruptedException e) {
      LOG.error(e.getMessage(), e);
    }
  }

  /**
   * Test {@link SpaceService#isManager(Space, String)}
   *
   * @throws Exception
   * @since 1.2.0-GA
   */
  public void testIsManager() throws Exception {
    //
    createIdentities(new Identity[] { demo, tom, raul, ghost, dragon, register1, mary, jame, paul, hacker });
    Space space = this.getSpaceInstance(0);
    tearDownSpaceList.add(space);

    //
    Space got = spaceService.getSpaceByDisplayName(space.getDisplayName());
    assertNotNull(got);
    assertTrue(spaceService.isManager(got, DEMO));
    assertTrue(spaceService.isManager(got, TOM));
    assertFalse(spaceService.isManager(got, MARY));
    assertFalse(spaceService.isManager(got, JOHN));
  }

  /**
   * Test {@link SpaceService#isOnlyManager(Space, String)}
   *
   * @throws Exception
   * @since 1.2.0-GA
   */
  public void testIsOnlyManager() throws Exception {
    //
    createIdentities(new Identity[] { demo, tom, raul, ghost, dragon, register1, mary, jame, paul, hacker });
    Space space = this.getSpaceInstance(0);
    tearDownSpaceList.add(space);

    //
    Space got = spaceService.getSpaceByDisplayName(space.getDisplayName());
    assertNotNull(got);
    assertFalse(spaceService.isOnlyManager(got, TOM));
    assertFalse(spaceService.isOnlyManager(got, DEMO));

    //
    got.setManagers(new String[] { DEMO });
    spaceService.updateSpace(got);
    assertTrue(spaceService.isOnlyManager(got, DEMO));
    assertFalse(spaceService.isOnlyManager(got, TOM));

    //
    got.setManagers(new String[] { TOM });
    spaceService.updateSpace(got);
    assertFalse(spaceService.isOnlyManager(got, DEMO));
    assertTrue(spaceService.isOnlyManager(got, TOM));
  }

  /**
   * Test {@link SpaceService#hasSettingPermission(Space, String)}
   *
   * @throws Exception
   * @since 1.2.0-GA
   */
  public void testHasSettingPermission() throws Exception {
    //
    createIdentities(new Identity[] { demo, tom, raul, ghost, dragon, register1, mary, jame, paul, hacker });
    Space space = this.getSpaceInstance(0);
    tearDownSpaceList.add(space);

    //
    Space got = spaceService.getSpaceByDisplayName(space.getDisplayName());
    assertNotNull(got);
    assertTrue(spaceService.hasSettingPermission(got, DEMO));
    assertTrue(spaceService.hasSettingPermission(got, TOM));
    assertTrue(spaceService.hasSettingPermission(got, ROOT));
    assertFalse(spaceService.hasSettingPermission(got, MARY));
    assertFalse(spaceService.hasSettingPermission(got, JOHN));
  }

  /**
   * Test {@link SpaceService#registerSpaceListenerPlugin(org.exoplatform.social.core.space.SpaceListenerPlugin)}
   *
   * @throws Exception
   * @since 1.2.0-GA
   */
  public void testRegisterSpaceListenerPlugin() throws Exception {
    //TODO
  }

  /**
   * Test {@link SpaceService#unregisterSpaceListenerPlugin(org.exoplatform.social.core.space.SpaceListenerPlugin)}
   *
   * @throws Exception
   * @since 1.2.0-GA
   */
  public void testUnregisterSpaceListenerPlugin() throws Exception {
    //TODO
  }

  /**
   * Test {@link SpaceService#addMember(Space, String)}
   *
   * @throws Exception
   * @since 1.2.0-GA
   */
  public void testAddMember() throws Exception {
    //
    createIdentities(new Identity[] { demo, tom, raul, ghost, dragon, register1, mary, jame, paul, hacker, root, john });

    //
    int number = 0;
    Space space = new Space();
    space.setDisplayName("my space " + number);
    space.setPrettyName(space.getDisplayName());
    space.setRegistration(Space.OPEN);
    space.setDescription("add new space " + number);
    space.setType(DefaultSpaceApplicationHandler.NAME);
    space.setVisibility(Space.PUBLIC);
    space.setRegistration(Space.VALIDATION);
    space.setPriority(Space.INTERMEDIATE_PRIORITY);
    space.setGroupId("/space/space" + number);
    space.setUrl(space.getPrettyName());
    String[] spaceManagers = new String[] { DEMO };
    String[] members = new String[] {};
    String[] invitedUsers = new String[] { REGISTER_1, MARY };
    String[] pendingUsers = new String[] { JAME, PAUL, HACKER };
    space.setInvitedUsers(invitedUsers);
    space.setPendingUsers(pendingUsers);
    space.setManagers(spaceManagers);
    space.setMembers(members);

    //
    space = this.createSpaceNonInitApps(space, DEMO, null);
    tearDownSpaceList.add(space);

    //
    Space got = spaceService.getSpaceByDisplayName(space.getDisplayName());
    assertNotNull(got);
    spaceService.addMember(got, ROOT);
    spaceService.addMember(got, MARY);
    spaceService.addMember(got, JOHN);
    got = spaceService.getSpaceByDisplayName(space.getDisplayName());
    assertEquals(4, got.getMembers().length);

    // Wait 3 secs to have activity stored
    try {
      IdentityManager identityManager = (IdentityManager) getContainer().getComponentInstanceOfType(IdentityManager.class);
      ActivityManager activityManager = (ActivityManager) getContainer().getComponentInstanceOfType(ActivityManager.class);
      Thread.sleep(3000);
      RealtimeListAccess<ExoSocialActivity> broadCastActivities = activityManager.getActivitiesWithListAccess(identityManager.getOrCreateIdentity(SpaceIdentityProvider.NAME, got.getPrettyName(), false));
      List<ExoSocialActivity> listBroadCastActivities = broadCastActivities.loadAsList(0, 10);
      for (ExoSocialActivity activity : listBroadCastActivities) {
        activityManager.deleteActivity(activity);
      }
    } catch (InterruptedException e) {
      LOG.error(e.getMessage(), e);
    }
  }

  /**
   * Test {@link SpaceService#addMember(Space, String)}
   *
   * @throws Exception
   * @since 1.2.0-GA
   */
  public void testAddMemberSpecialCharacter() throws Exception {
    //
    String reg = "^\\p{L}[\\p{L}\\d\\s._,-]+$";
    Pattern pattern = Pattern.compile(reg);
    assertTrue(pattern.matcher("user-new.1").matches());
    assertTrue(pattern.matcher("user.new").matches());
    assertTrue(pattern.matcher("user-new").matches());

    //
    createIdentities(new Identity[] { demo, tom, raul, ghost, dragon, register1, mary, jame, paul, hacker, user_new_1, user_new, user_new_dot });

    //
    int number = 0;
    Space space = new Space();
    space.setDisplayName("my space " + number);
    space.setPrettyName(space.getDisplayName());
    space.setRegistration(Space.OPEN);
    space.setDescription("add new space " + number);
    space.setType(DefaultSpaceApplicationHandler.NAME);
    space.setVisibility(Space.PUBLIC);
    space.setRegistration(Space.VALIDATION);
    space.setPriority(Space.INTERMEDIATE_PRIORITY);
    space.setGroupId("/space/space" + number);
    space.setUrl(space.getPrettyName());
    String[] spaceManagers = new String[] { DEMO };
    String[] members = new String[] {};
    String[] invitedUsers = new String[] { REGISTER_1, MARY };
    String[] pendingUsers = new String[] { JAME, PAUL, HACKER };
    space.setInvitedUsers(invitedUsers);
    space.setPendingUsers(pendingUsers);
    space.setManagers(spaceManagers);
    space.setMembers(members);
    space = this.createSpaceNonInitApps(space, DEMO, null);
    tearDownSpaceList.add(space);

    //
    Space got = spaceService.getSpaceByDisplayName(space.getDisplayName());
    assertNotNull(got);
    spaceService.addMember(got, "user-new.1");
    spaceService.addMember(got, "user.new");
    spaceService.addMember(got, "user-new");
    got = spaceService.getSpaceByDisplayName(space.getDisplayName());
    assertEquals(4, got.getMembers().length);
  }

  /**
   * Test {@link SpaceService#removeMember(Space, String)}
   *
   * @throws Exception
   * @since 1.2.0-GA
   */
  public void testRemoveMember() throws Exception {
    //
    createIdentities(new Identity[] { demo, tom, raul, ghost, dragon, register1, mary, jame, paul, hacker, root, john });

    //
    int number = 0;
    Space space = new Space();
    space.setDisplayName("my space " + number);
    space.setPrettyName(space.getDisplayName());
    space.setRegistration(Space.OPEN);
    space.setDescription("add new space " + number);
    space.setType(DefaultSpaceApplicationHandler.NAME);
    space.setVisibility(Space.PUBLIC);
    space.setRegistration(Space.VALIDATION);
    space.setPriority(Space.INTERMEDIATE_PRIORITY);
    space.setGroupId("/space/space" + number);
    space.setUrl(space.getPrettyName());
    String[] spaceManagers = new String[] { DEMO };
    String[] members = new String[] {};
    String[] invitedUsers = new String[] { REGISTER_1, MARY };
    String[] pendingUsers = new String[] { JAME, PAUL, HACKER };
    space.setInvitedUsers(invitedUsers);
    space.setPendingUsers(pendingUsers);
    space.setManagers(spaceManagers);
    space.setMembers(members);
    space = this.createSpaceNonInitApps(space, DEMO, null);
    tearDownSpaceList.add(space);

    //
    Space got = spaceService.getSpaceByDisplayName(space.getDisplayName());
    assertNotNull(got);
    spaceService.addMember(got, ROOT);
    spaceService.addMember(got, MARY);
    spaceService.addMember(got, JOHN);
    got = spaceService.getSpaceByDisplayName(space.getDisplayName());
    assertEquals(4, got.getMembers().length);
    spaceService.removeMember(got, ROOT);
    spaceService.removeMember(got, MARY);
    spaceService.removeMember(got, JOHN);
    assertEquals(1, got.getMembers().length);

    // Wait 3 secs to have activity stored
    try {
      IdentityManager identityManager = (IdentityManager) getContainer().getComponentInstanceOfType(IdentityManager.class);
      ActivityManager activityManager = (ActivityManager) getContainer().getComponentInstanceOfType(ActivityManager.class);
      Thread.sleep(3000);
      RealtimeListAccess<ExoSocialActivity> broadCastActivities = activityManager.getActivitiesWithListAccess(identityManager.getOrCreateIdentity(SpaceIdentityProvider.NAME, got.getPrettyName(), false));
      List<ExoSocialActivity> listBroadCastActivities = broadCastActivities.loadAsList(0, 10);
      for (ExoSocialActivity activity : listBroadCastActivities) {
        activityManager.deleteActivity(activity);
      }
    } catch (InterruptedException e) {
      LOG.error(e.getMessage(), e);
    }
  }

  /**
   * Test {@link SpaceService#isMember(Space, String)}
   *
   * @throws Exception
   * @since 1.2.0-GA
   */
  public void testIsMember() throws Exception {
    //
    createIdentities(new Identity[] { demo, tom, raul, ghost, dragon, register1, mary, jame, paul, hacker });
    Space space = this.getSpaceInstance(0);
    tearDownSpaceList.add(space);

    //
    Space got = spaceService.getSpaceByDisplayName(space.getDisplayName());
    assertNotNull(got);
    assertTrue(spaceService.isMember(got, RAUL));
    assertTrue(spaceService.isMember(got, GHOST));
    assertTrue(spaceService.isMember(got, DRAGON));
    assertFalse(spaceService.isMember(got, ANONYMOUS));
  }

  /**
   * Test {@link SpaceService#hasAccessPermission(Space, String)}
   *
   * @throws Exception
   * @since 1.2.0-GA
   */
  public void testHasAccessPermission() throws Exception {
    //
    createIdentities(new Identity[] { demo, tom, raul, ghost, dragon, register1, mary, jame, paul, hacker });
    Space space = this.getSpaceInstance(0);
    tearDownSpaceList.add(space);

    //
    Space got = spaceService.getSpaceByDisplayName(space.getDisplayName());
    assertNotNull(got);
    assertTrue(spaceService.hasAccessPermission(got, RAUL));
    assertTrue(spaceService.hasAccessPermission(got, GHOST));
    assertTrue(spaceService.hasAccessPermission(got, DRAGON));
    assertTrue(spaceService.hasAccessPermission(got, TOM));
    assertTrue(spaceService.hasAccessPermission(got, DEMO));
    assertTrue(spaceService.hasAccessPermission(got, ROOT));
    assertFalse(spaceService.hasAccessPermission(got, MARY));
    assertFalse(spaceService.hasAccessPermission(got, JOHN));
  }

  /**
   * Test {@link SpaceService#installApplication(Space, String)}
   *
   * @throws Exception
   * @since 1.2.0-GA
   */
  public void testInstallApplication() throws Exception {
    //TODO Complete this
  }

  /**
   * Test {@link SpaceService#activateApplication(Space, String)}
   *
   * @throws Exception
   * @since 1.2.0-GA
   */
  public void testActivateApplication() throws Exception {
    //TODO Complete this
  }

  /**
   * Test {@link SpaceService#deactivateApplication(Space, String)}
   *
   * @throws Exception
   * @since 1.2.0-GA
   */
  public void testDeactivateApplication() throws Exception {
    //TODO Complete this
  }

  /**
   * Test {@link SpaceService#removeApplication(Space, String, String)}
   *
   * @throws Exception
   * @since 1.2.0-GA
   */
  public void testRemoveApplication() throws Exception {
    //TODO Complete this
  }

  /**
   * Test {@link SpaceService#setSpaceApplicationConfigPlugin(org.exoplatform.social.core.space.SpaceApplicationConfigPlugin)}
   *
   * @throws Exception
   * @since 1.2.0-GA
   */
  public void testSetSpaceApplicationConfigPlugin() throws Exception {
    //TODO Complete this
  }

  /**
   * Test {@link SpaceService}
   *
   * @throws Exception
   * @since 1.2.0-GA
   */
  public void testGetSpaceApplicationConfigPlugin() throws Exception {
    //TODO Complete this
  }
  
  /**
   * Test {@link org.exoplatform.social.core.storage.SpaceStorage#getVisibleSpaces(String)}
   *
   * @throws Exception
   * @since 1.2.5-GA
   */
  public void testGetVisibleSpaces() throws Exception {
    //
    createIdentities(new Identity[] { demo });

    //
    int countSpace = 10;
    Space []listSpace = new Space[10];

    //there are 6 spaces with visible = 'private'
    for (int i = 0; i < countSpace; i ++) {
      if (i < 6)
         //[0->5] :: there are 6 spaces with visible = 'private'
        listSpace[i] = this.getSpaceInstance(i, Space.PRIVATE, Space.OPEN, DEMO);
      else
        //[6->9]:: there are 4 spaces with visible = 'hidden'
        listSpace[i] = this.getSpaceInstance(i, Space.HIDDEN, Space.OPEN, DEMO);
      
      spaceService.saveSpace(listSpace[i], true);
      tearDownSpaceList.add(listSpace[i]);
    }
    
    //visible with remoteId = 'demo'  return 10 spaces
    {
      List<Space> visibleAllSpaces = spaceService.getVisibleSpaces(DEMO, null);
      assertNotNull(visibleAllSpaces);
      assertEquals(countSpace, visibleAllSpaces.size());
    }    
  }

  /**
   * Test {@link org.exoplatform.social.core.storage.SpaceStorage#getVisibleSpaces(String)}
   *
   * @throws Exception
   * @since 1.2.5-GA
   */
  public void testGetVisibleSpacesCloseRegistration() throws Exception {
    //
    createIdentities(new Identity[] { demo });

    //
    int countSpace = 10;
    Space []listSpace = new Space[10];
    
    //there are 6 spaces with visible = 'private'
    for (int i = 0; i < countSpace; i ++) {
      if (i < 6)
         //[0->5] :: there are 6 spaces with visible = 'private'
        listSpace[i] = this.getSpaceInstance(i, Space.PRIVATE, Space.CLOSE, DEMO);
      else
        //[6->9]:: there are 4 spaces with visible = 'hidden'
        listSpace[i] = this.getSpaceInstance(i, Space.HIDDEN, Space.CLOSE, DEMO);
      
      spaceService.saveSpace(listSpace[i], true);
      tearDownSpaceList.add(listSpace[i]);
    }
    
    //visible with remoteId = 'demo'  return 10 spaces
    {
      List<Space> visibleAllSpaces = spaceService.getVisibleSpaces(DEMO, null);
      assertNotNull(visibleAllSpaces);
      assertEquals(countSpace, visibleAllSpaces.size());
    }
    
    //visible with remoteId = 'mary'  return 6 spaces: can see
    {
      int registrationCloseSpaceCount = 6;
      List<Space> registrationCloseSpaces = spaceService.getVisibleSpaces(MARY, null);
      assertNotNull(registrationCloseSpaces);
      assertEquals(registrationCloseSpaceCount, registrationCloseSpaces.size());
    }
  }

  /**
   * Test {@link org.exoplatform.social.core.storage.SpaceStorage#getVisibleSpaces(String)}
   *
   * @throws Exception
   * @since 1.2.5-GA
   */
  public void testGetVisibleSpacesInvitedMember() throws Exception {
    //
    createIdentities(new Identity[] { demo, mary, hacker });

    //
    int countSpace = 10;
    Space[] listSpace = new Space[10];
    
    //there are 6 spaces with visible = 'private'
    for (int i = 0; i < countSpace; i ++) {
      if (i < 6)
         //[0->5] :: there are 6 spaces with visible = 'private'
        listSpace[i] = this.getSpaceInstanceInvitedMember(i, Space.PRIVATE, Space.CLOSE, new String[] { MARY, HACKER }, DEMO);
      else
        //[6->9]:: there are 4 spaces with visible = 'hidden'
        listSpace[i] = this.getSpaceInstance(i, Space.HIDDEN, Space.CLOSE, DEMO);
      
      spaceService.saveSpace(listSpace[i], true);
      tearDownSpaceList.add(listSpace[i]);
    }
    
    //visible with remoteId = 'demo'  return 10 spaces
    {
      List<Space> visibleAllSpaces = spaceService.getVisibleSpaces(DEMO, null);
      assertNotNull(visibleAllSpaces);
      assertEquals(countSpace, visibleAllSpaces.size());
    }
    
    //visible with invited = 'mary'  return 6 spaces
    {
      int invitedSpaceCount1 = 6;
      List<Space> invitedSpaces1 = spaceService.getVisibleSpaces(MARY, null);
      assertNotNull(invitedSpaces1);
      assertEquals(invitedSpaceCount1, invitedSpaces1.size());
    }
    
    //visible with invited = 'hacker'  return 6 spaces
    {
      int invitedSpaceCount1 = 6;
      List<Space> invitedSpaces1 = spaceService.getVisibleSpaces(HACKER, null);
      assertNotNull(invitedSpaces1);
      assertEquals(invitedSpaceCount1, invitedSpaces1.size());
    }
    
    //visible with invited = 'paul'  return 6 spaces
    {
      int invitedSpaceCount2 = 6;
      List<Space> invitedSpaces2 = spaceService.getVisibleSpaces(PAUL, null);
      assertNotNull(invitedSpaces2);
      assertEquals(invitedSpaceCount2, invitedSpaces2.size());
    }
  }

  private Space populateData() throws Exception {
    //
    String spaceDisplayName = "Space1";
    Space space1 = new Space();
    space1.setApp("Calendar;FileSharing");
    space1.setDisplayName(spaceDisplayName);
    space1.setPrettyName(space1.getDisplayName());
    String shortName = SpaceUtils.cleanString(spaceDisplayName);
    space1.setGroupId("/spaces/" + shortName);
    space1.setUrl(shortName);
    space1.setRegistration("validation");
    space1.setDescription("This is my first space for testing");
    space1.setType("classic");
    space1.setVisibility("public");
    space1.setPriority("2");
    String[] manager = new String []{ ROOT };
    String[] members = new String []{ DEMO, JOHN, MARY, TOM, HARRY };
    space1.setManagers(manager);
    space1.setMembers(members);
    spaceService.saveSpace(space1, true);

    //
    return space1;
  }

  /**
   * Gets an instance of the space.
   *
   * @param number
   * @return
   * @throws Exception
   * @since 1.2.0-GA
   */
  private Space getSpaceInstance(int number) throws Exception {
    //
    Space space = new Space();
    space.setDisplayName("my space " + number);
    space.setPrettyName(space.getDisplayName());
    space.setRegistration(Space.OPEN);
    space.setDescription("add new space " + number);
    space.setType(DefaultSpaceApplicationHandler.NAME);
    space.setVisibility(Space.PUBLIC);
    space.setRegistration(Space.VALIDATION);
    space.setPriority(Space.INTERMEDIATE_PRIORITY);
    space.setGroupId("/space/space" + number);
    String[] managers = new String[] { DEMO, TOM };
    String[] members = new String[] { RAUL, GHOST, DRAGON };
    String[] invitedUsers = new String[] { REGISTER_1, MARY };
    String[] pendingUsers = new String[] { JAME, PAUL, HACKER };
    space.setInvitedUsers(invitedUsers);
    space.setPendingUsers(pendingUsers);
    space.setManagers(managers);
    space.setMembers(members);
    space.setUrl(space.getPrettyName());
    spaceService.saveSpace(space, true);

    //
    return space;
  }
  
  /**
   * Gets an instance of Space.
   *
   * @param number
   * @return an instance of space
   */
  private Space getSpaceInstance(int number, String visible, String registration, String manager, String...members) {
    Space space = new Space();
    space.setApp("app");
    space.setDisplayName("my space " + number);
    space.setPrettyName(space.getDisplayName());
    space.setRegistration(registration);
    space.setDescription("add new space " + number);
    space.setType(DefaultSpaceApplicationHandler.NAME);
    space.setVisibility(visible);
    space.setPriority(Space.INTERMEDIATE_PRIORITY);
    space.setGroupId("/spaces/space" + number);
    String[] managers = new String[] {manager};
    String[] invitedUsers = new String[] {};
    String[] pendingUsers = new String[] {};
    space.setInvitedUsers(invitedUsers);
    space.setPendingUsers(pendingUsers);
    space.setManagers(managers);
    space.setMembers(members);
    space.setUrl(space.getPrettyName());
    return space;
  }
  
  /**
   * Gets an instance of Space.
   *
   * @param number
   * @return an instance of space
   */
  private Space getSpaceInstanceInvitedMember(int number, String visible, String registration, String[] invitedMember, String manager, String...members) {
    Space space = new Space();
    space.setApp("app");
    space.setDisplayName("my space " + number);
    space.setPrettyName(space.getDisplayName());
    space.setRegistration(registration);
    space.setDescription("add new space " + number);
    space.setType(DefaultSpaceApplicationHandler.NAME);
    space.setVisibility(visible);
    space.setPriority(Space.INTERMEDIATE_PRIORITY);
    space.setGroupId("/spaces/space" + number);
    space.setUrl(space.getPrettyName());
    String[] managers = new String[] {manager};
    //String[] invitedUsers = new String[] {invitedMember};
    String[] pendingUsers = new String[] {};
    space.setInvitedUsers(invitedMember);
    space.setPendingUsers(pendingUsers);
    space.setManagers(managers);
    space.setMembers(members);
    return space;
  }

  private Space createMoreSpace(String spaceName) throws Exception {
    Space space2 = new Space();
    space2.setApp("Contact,Forum");
    space2.setDisplayName(spaceName);
    space2.setPrettyName(space2.getDisplayName());
    String shortName = SpaceUtils.cleanString(spaceName);
    space2.setGroupId("/spaces/" + shortName );
    space2.setUrl(shortName);
    space2.setRegistration("open");
    space2.setDescription("This is my second space for testing");
    space2.setType("classic");
    space2.setVisibility("public");
    space2.setPriority("2");

    spaceService.saveSpace(space2, true);

    return space2;
  }

  /**
   * Creates new space with out init apps.
   *
   * @param space
   * @param creator
   * @param invitedGroupId
   * @return
   * @since 1.2.0-GA
   */
  private Space createSpaceNonInitApps(Space space, String creator, String invitedGroupId) {
    // Creates new space by creating new group
    String groupId = null;
    try {
      groupId = SpaceUtils.createGroup(space.getDisplayName(), creator);
    } catch (SpaceException e) {
      LOG.error("Error while creating group", e);
    }

    if (invitedGroupId != null) {
      // Invites user in group join to new created space.
      // Gets users in group and then invites user to join into space.
      OrganizationService org = (OrganizationService) ExoContainerContext.getCurrentContainer().getComponentInstanceOfType(OrganizationService.class);
      try {
        PageList<User> groupMembersAccess = org.getUserHandler().findUsersByGroup(invitedGroupId);
        List<User> users = groupMembersAccess.getAll();

        for (User user : users) {
          String userId = user.getUserName();
          if (!userId.equals(creator)) {
            String[] invitedUsers = space.getInvitedUsers();
            if (!ArrayUtils.contains(invitedUsers, userId)) {
              invitedUsers = (String[]) ArrayUtils.add(invitedUsers, userId);
              space.setInvitedUsers(invitedUsers);
            }
          }
        }
      } catch (Exception e) {
        LOG.error("Failed to invite users from group " + invitedGroupId, e);
      }
    }
    String[] managers = new String[] { creator };
    space.setManagers(managers);
    space.setGroupId(groupId);
    space.setUrl(space.getPrettyName());
    try {
      spaceService.saveSpace(space, true);
    } catch (SpaceException e) {
      LOG.warn("Error while saving space", e);
    }
    return space;
  }

  private void createIdentities(Identity[] identities) throws Exception {
    for (Identity identity : identities) {
      identityStorage.saveIdentity(identity);
      tearDownUserList.add(identity);
    }
  }
}