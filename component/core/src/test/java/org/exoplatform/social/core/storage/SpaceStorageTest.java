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

package org.exoplatform.social.core.storage;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.exoplatform.social.core.identity.model.Identity;
import org.exoplatform.social.core.identity.model.Profile;
import org.exoplatform.social.core.identity.provider.SpaceIdentityProvider;
import org.exoplatform.social.core.model.AvatarAttachment;
import org.exoplatform.social.core.service.LinkProvider;
import org.exoplatform.social.core.space.SpaceFilter;
import org.exoplatform.social.core.space.impl.DefaultSpaceApplicationHandler;
import org.exoplatform.social.core.space.model.Space;
import org.exoplatform.social.core.storage.api.IdentityStorage;
import org.exoplatform.social.core.storage.api.SpaceStorage;
import org.exoplatform.social.core.test.AbstractCoreTest;
import org.exoplatform.social.core.test.MaxQueryNumber;
import org.exoplatform.social.core.test.QueryNumberTest;

/**
 * Unit Tests for {@link org.exoplatform.social.core.storage.api.SpaceStorage}
 *
 * @since Nov 3, 2010
 * @copyright eXo SAS
 */
@QueryNumberTest
public class SpaceStorageTest extends AbstractCoreTest {

  private static final String DEMO           = "demo";
  private static final String TOM            = "tom";
  private static final String RAUL           = "raul";
  private static final String GHOST          = "ghost";
  private static final String DRAGON         = "dragon";
  private static final String REGISTER_1     = "register1";
  private static final String MARY           = "mary";
  private static final String JAME           = "jame";
  private static final String PAUL           = "paul";
  private static final String HACKER         = "hacker";
  private static final String ANONYMOUS      = "anonymous";
  private static final String ORGANIZATION   = "organization";

  private Identity        demo        = new Identity(ORGANIZATION, DEMO);
  private Identity        tom         = new Identity(ORGANIZATION, TOM);
  private Identity        raul        = new Identity(ORGANIZATION, RAUL);
  private Identity        ghost       = new Identity(ORGANIZATION, GHOST);
  private Identity        dragon      = new Identity(ORGANIZATION, DRAGON);
  private Identity        register1   = new Identity(ORGANIZATION, REGISTER_1);
  private Identity        mary        = new Identity(ORGANIZATION, MARY);
  private Identity        jame        = new Identity(ORGANIZATION, JAME);
  private Identity        paul        = new Identity(ORGANIZATION, PAUL);
  private Identity        hacker      = new Identity(ORGANIZATION, HACKER);
  private Identity        anonymous   = new Identity(ORGANIZATION, ANONYMOUS);

  private SpaceStorage    spaceStorage;
  private IdentityStorage identityStorage;
  private List<Space>     tearDownSpaceList;
  private List<Identity>  tearDownIdentityList;

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    spaceStorage = (SpaceStorage) this.getContainer().getComponentInstanceOfType(SpaceStorage.class);
    identityStorage = (IdentityStorage) this.getContainer().getComponentInstanceOfType(IdentityStorage.class);
    tearDownIdentityList = new ArrayList<Identity>();
    tearDownSpaceList = new ArrayList<Space>();
    createIdentities(new Identity[] { demo, tom, raul, ghost, dragon, register1, mary, jame, paul, hacker, anonymous });
  }

  /**
   * Cleans up.
   */
  @Override
  protected void tearDown() throws Exception {
    for (Space sp : tearDownSpaceList) {
      spaceStorage.deleteSpace(sp.getId());
    }

    for (Identity id : tearDownIdentityList) {
      identityStorage.deleteIdentity(id);
    }

    super.tearDown();
  }

  /**
   * Gets an instance of Space.
   *
   * @param number
   * @return an instance of space
   */
  private Space getSpaceInstance(int number) {
    Space space = new Space();
    space.setApp("app");
    space.setDisplayName("my space " + number);
    space.setPrettyName(space.getDisplayName());
    space.setRegistration(Space.OPEN);
    space.setDescription("add new space " + number);
    space.setType(DefaultSpaceApplicationHandler.NAME);
    space.setVisibility(Space.PUBLIC);
    space.setPriority(Space.INTERMEDIATE_PRIORITY);
    space.setGroupId("/spaces/space" + number);
    String[] managers = new String[] { DEMO, TOM };
    String[] members = new String[] { RAUL, GHOST, DRAGON };
    String[] invitedUsers = new String[] { REGISTER_1, MARY };
    String[] pendingUsers = new String[] { JAME, PAUL, HACKER };
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
  
  private List<Space> getSpaceWithRoot(SpaceFilter filter) {
    if (filter == null) {
      return spaceStorage.getAllSpaces();
    } else {
      return spaceStorage.getSpacesByFilter(filter, 0, 200);
    }
  }
  
  private void createIdentities(Identity[] identities) throws Exception {
    for (Identity identity : identities) {
      identityStorage.saveIdentity(identity);
      tearDownIdentityList.add(identity);
    }
  }

  /**
   * Test {@link org.exoplatform.social.core.storage.SpaceStorage#getAllSpaces()}
   *
   * @throws Exception
   */
  @MaxQueryNumber(750)
  public void testGetAllSpaces() throws Exception {
    int totalSpaces = 10;
    for (int i = 1; i <= totalSpaces; i++) {
      Space space = this.getSpaceInstance(i);
      spaceStorage.saveSpace(space, true);
      tearDownSpaceList.add(space);
    }
    assertEquals(totalSpaces, spaceStorage.getAllSpaces().size());
  }

  /**
   * Test {@link org.exoplatform.social.core.storage.SpaceStorage#getSpaces(long, long)}
   *
   * @throws Exception
   * @since 1.2.0-GA
   */
  @MaxQueryNumber(700)
  public void testGetSpaces() throws Exception {
    int totalSpaces = 10;
    for (int i = 1; i <= totalSpaces; i++) {
      Space space = this.getSpaceInstance(i);
      spaceStorage.saveSpace(space, true);
      tearDownSpaceList.add(space);
    }
    int offset = 0;
    int limit = 10;
    List<Space> spaceListAccess = spaceStorage.getSpaces(offset, limit);
    assertNotNull(spaceListAccess);
    assertEquals(totalSpaces, spaceListAccess.size());

    spaceListAccess = spaceStorage.getSpaces(offset, 5);
    assertNotNull(spaceListAccess);
    assertEquals(5, spaceListAccess.size());

    spaceListAccess = spaceStorage.getSpaces(offset, 20);
    assertNotNull(spaceListAccess);
    assertEquals(totalSpaces, spaceListAccess.size());
  }

  /**
   * Test {@link org.exoplatform.social.core.storage.SpaceStorage#getAllSpacesCount()}
   *
   * @throws Exception
   * @since 1.2.0-GA
   */
  @MaxQueryNumber(700)
  public void testGetAllSpacesCount() throws Exception {
    int totalSpaces = 10;
    for (int i = 1; i <= totalSpaces; i++) {
      Space space = this.getSpaceInstance(i);
      spaceStorage.saveSpace(space, true);
      tearDownSpaceList.add(space);
    }
    int spacesCount = spaceStorage.getAllSpacesCount();
    assertEquals(totalSpaces, spacesCount);
  }

  /**
   * Test {@link org.exoplatform.social.core.storage.SpaceStorage#getSpacesByFilter(org.exoplatform.social.core.space.SpaceFilter, long, long)}
   *
   * @throws Exception
   * @since 1.2.0-GA
   */
  @MaxQueryNumber(700)
  public void testGetSpacesByFilter() throws Exception {
    int totalSpaces = 10;
    for (int i = 1; i <= totalSpaces; i++) {
      Space space = this.getSpaceInstance(i);
      spaceStorage.saveSpace(space, true);
      tearDownSpaceList.add(space);
    }
    List<Space> foundSpaces = spaceStorage.getSpacesByFilter(new SpaceFilter("add"), 0, 10);
    assertNotNull(foundSpaces);
    assertEquals(totalSpaces, foundSpaces.size());

    foundSpaces = spaceStorage.getSpacesByFilter(new SpaceFilter("my"), 0, 10);
    assertNotNull(foundSpaces);
    assertEquals(totalSpaces, foundSpaces.size());

    foundSpaces = spaceStorage.getSpacesByFilter(new SpaceFilter("my space"), 0, 10);
    assertNotNull(foundSpaces);
    assertEquals(totalSpaces, foundSpaces.size());

    foundSpaces = spaceStorage.getSpacesByFilter(new SpaceFilter("hell gate"), 0, 10);
    assertNotNull(foundSpaces);
    assertEquals(0, foundSpaces.size());

    foundSpaces = spaceStorage.getSpacesByFilter(new SpaceFilter('m'), 0, 10);
    assertNotNull(foundSpaces);
    assertEquals(totalSpaces, foundSpaces.size());

    foundSpaces = spaceStorage.getSpacesByFilter(new SpaceFilter('M'), 0, 10);
    assertNotNull(foundSpaces);
    assertEquals(totalSpaces, foundSpaces.size());

    foundSpaces = spaceStorage.getSpacesByFilter(new SpaceFilter('k'), 0, 10);
    assertNotNull(foundSpaces);
    assertEquals(0, foundSpaces.size());
  }

  /**
   * Test {@link org.exoplatform.social.core.storage.SpaceStorage#getAllSpacesByFilterCount(org.exoplatform.social.core.space.SpaceFilter)}
   *
   * @throws Exception
   * @since 1.2.0-GA
   */
  @MaxQueryNumber(700)
  public void testGetAllSpacesByFilterCount() throws Exception {
    int totalSpaces = 10;
    for (int i = 1; i <= totalSpaces; i++) {
      Space space = this.getSpaceInstance(i);
      spaceStorage.saveSpace(space, true);
      tearDownSpaceList.add(space);
    }
    int count = spaceStorage.getAllSpacesByFilterCount(new SpaceFilter("add"));
    assertEquals(totalSpaces, count);

    count = spaceStorage.getAllSpacesByFilterCount(new SpaceFilter("my"));
    assertEquals(totalSpaces, count);

    count = spaceStorage.getAllSpacesByFilterCount(new SpaceFilter("my space"));
    assertEquals(totalSpaces, count);

    count = spaceStorage.getAllSpacesByFilterCount(new SpaceFilter("hell gate"));
    assertEquals(0, count);

    count = spaceStorage.getAllSpacesByFilterCount(new SpaceFilter('m'));
    assertEquals(totalSpaces, count);

    count = spaceStorage.getAllSpacesByFilterCount(new SpaceFilter('M'));
    assertEquals(totalSpaces, count);

    count = spaceStorage.getAllSpacesByFilterCount(new SpaceFilter('k'));
    assertEquals(0, count);
  }

  /**
   * Test {@link org.exoplatform.social.core.storage.SpaceStorage#getAccessibleSpaces(String)}
   *
   * @throws Exception
   * @since 1.2.0-GA
   */
  @MaxQueryNumber(700)
  public void testGetAccessibleSpaces() throws Exception {
    int countSpace = 10;
    Space []listSpace = new Space[10];
    for (int i = 0; i < countSpace; i ++) {
      listSpace[i] = this.getSpaceInstance(i);
      spaceStorage.saveSpace(listSpace[i], true);
      tearDownSpaceList.add(listSpace[i]);
    }
    List<Space> accessibleSpaces = spaceStorage.getAccessibleSpaces(DEMO);
    assertNotNull(accessibleSpaces);
    assertEquals(countSpace, accessibleSpaces.size());
  }

  /**
   * Test {@link org.exoplatform.social.core.storage.SpaceStorage#getAccessibleSpacesByFilter(String, org.exoplatform.social.core.space.SpaceFilter, long, long)}
   *
   * @throws Exception
   * @since 1.2.0-GA
   */
  @MaxQueryNumber(1400)
  public void testGetAccessibleSpacesByFilter() throws Exception {
    int countSpace = 20;
    Space []listSpace = new Space[countSpace];
    for (int i = 0; i < countSpace; i ++) {
      listSpace[i] = this.getSpaceInstance(i);
      spaceStorage.saveSpace(listSpace[i], true);
      tearDownSpaceList.add(listSpace[i]);
    }
    List<Space> accessibleSpacesByFilter = spaceStorage.getAccessibleSpacesByFilter(DEMO, new SpaceFilter("my space"), 0, 10);
    assertNotNull(accessibleSpacesByFilter);
    assertEquals(10, accessibleSpacesByFilter.size());

    accessibleSpacesByFilter = spaceStorage.getAccessibleSpacesByFilter(TOM, new SpaceFilter("my space"), 0, 10);
    assertNotNull(accessibleSpacesByFilter);
    assertEquals(10, accessibleSpacesByFilter.size());

    accessibleSpacesByFilter = spaceStorage.getAccessibleSpacesByFilter(GHOST, new SpaceFilter("my space"), 0, 10);
    assertNotNull(accessibleSpacesByFilter);
    assertEquals(10, accessibleSpacesByFilter.size());

    accessibleSpacesByFilter = spaceStorage.getAccessibleSpacesByFilter(DEMO, new SpaceFilter("add new"), 0, 10);
    assertNotNull(accessibleSpacesByFilter);
    assertEquals(10, accessibleSpacesByFilter.size());

    accessibleSpacesByFilter = spaceStorage.getAccessibleSpacesByFilter(DEMO, new SpaceFilter('m'), 0, 10);
    assertNotNull(accessibleSpacesByFilter);
    assertEquals(10, accessibleSpacesByFilter.size());

    accessibleSpacesByFilter = spaceStorage.getAccessibleSpacesByFilter(DEMO, new SpaceFilter('M'), 0, 10);
    assertNotNull(accessibleSpacesByFilter);
    assertEquals(10, accessibleSpacesByFilter.size());

    accessibleSpacesByFilter = spaceStorage.getAccessibleSpacesByFilter(DEMO, new SpaceFilter('K'), 0, 10);
    assertNotNull(accessibleSpacesByFilter);
    assertEquals(0, accessibleSpacesByFilter.size());

    accessibleSpacesByFilter = spaceStorage.getAccessibleSpacesByFilter("newperson", new SpaceFilter("my space"), 0, 10);
    assertNotNull(accessibleSpacesByFilter);
    assertEquals(0, accessibleSpacesByFilter.size());
  }

  /**
   * Test {@link org.exoplatform.social.core.storage.SpaceStorage#getAccessibleSpacesByFilterCount(String, org.exoplatform.social.core.space.SpaceFilter)}
   *
   * @throws Exception
   * @since 1.2.0-GA
   */
  @MaxQueryNumber(1400)
  public void testGetAccessibleSpacesByFilterCount() throws Exception {
    int countSpace = 20;
    Space []listSpace = new Space[countSpace];
    for (int i = 0; i < countSpace; i ++) {
      listSpace[i] = this.getSpaceInstance(i);
      spaceStorage.saveSpace(listSpace[i], true);
      tearDownSpaceList.add(listSpace[i]);
    }
    int accessibleSpacesByFilterCount = spaceStorage.getAccessibleSpacesByFilterCount(DEMO, new SpaceFilter("my space"));
    assertEquals(countSpace, accessibleSpacesByFilterCount);

    accessibleSpacesByFilterCount = spaceStorage.getAccessibleSpacesByFilterCount(TOM, new SpaceFilter("my space"));
    assertEquals(countSpace, accessibleSpacesByFilterCount);

    accessibleSpacesByFilterCount = spaceStorage.getAccessibleSpacesByFilterCount(TOM, new SpaceFilter('m'));
    assertEquals(countSpace, accessibleSpacesByFilterCount);

    accessibleSpacesByFilterCount = spaceStorage.getAccessibleSpacesByFilterCount(TOM, new SpaceFilter('M'));
    assertEquals(countSpace, accessibleSpacesByFilterCount);

    accessibleSpacesByFilterCount = spaceStorage.getAccessibleSpacesByFilterCount(TOM, new SpaceFilter('k'));
    assertEquals(0, accessibleSpacesByFilterCount);

    accessibleSpacesByFilterCount = spaceStorage.getAccessibleSpacesByFilterCount(GHOST, new SpaceFilter("my space"));
    assertEquals(countSpace, accessibleSpacesByFilterCount);

    accessibleSpacesByFilterCount = spaceStorage.getAccessibleSpacesByFilterCount(DEMO, new SpaceFilter("add new"));
    assertEquals(countSpace, accessibleSpacesByFilterCount);

    accessibleSpacesByFilterCount = spaceStorage.getAccessibleSpacesByFilterCount("newperson", new SpaceFilter("my space"));
    assertEquals(0, accessibleSpacesByFilterCount);
  }

  /**
   * Test {@link org.exoplatform.social.core.storage.SpaceStorage#getAccessibleSpacesCount(String)}
   *
   * @throws Exception
   * @since 1.2.0-GA
   */
  @MaxQueryNumber(700)
  public void testgetAccessibleSpacesCount() throws Exception {
    int countSpace = 10;
    Space []listSpace = new Space[10];
    for (int i = 0; i < countSpace; i ++) {
      listSpace[i] = this.getSpaceInstance(i);
      spaceStorage.saveSpace(listSpace[i], true);
      tearDownSpaceList.add(listSpace[i]);
    }
    int accessibleSpacesCount = spaceStorage.getAccessibleSpacesCount(DEMO);
    assertEquals(countSpace, accessibleSpacesCount);

    accessibleSpacesCount = spaceStorage.getAccessibleSpacesCount(DRAGON);
    assertEquals(countSpace, accessibleSpacesCount);

    accessibleSpacesCount = spaceStorage.getAccessibleSpacesCount("nobody");
    assertEquals(0, accessibleSpacesCount);
  }

  /**
   * Test {@link org.exoplatform.social.core.storage.SpaceStorage#getAccessibleSpaces(String, long, long)}
   *
   * @throws Exception
   * @since 1.2.0-GA
   */
  @MaxQueryNumber(700)
  public void testGetAccessibleSpacesWithOffset() throws Exception {
    int countSpace = 10;
    Space []listSpace = new Space[10];
    for (int i = 0; i < countSpace; i ++) {
      listSpace[i] = this.getSpaceInstance(i);
      spaceStorage.saveSpace(listSpace[i], true);
      tearDownSpaceList.add(listSpace[i]);
    }
    List<Space> accessibleSpaces = spaceStorage.getAccessibleSpaces(DEMO, 0, 5);
    assertNotNull(accessibleSpaces);
    assertEquals(5, accessibleSpaces.size());
  }

  /**
   * Test {@link org.exoplatform.social.core.storage.SpaceStorage#getEditableSpaces(String)}
   *
   * @throws Exception
   * @since 1.2.0-GA
   */
  @MaxQueryNumber(700)
  public void testGetEditableSpaces () throws Exception {
    int countSpace = 10;
    Space []listSpace = new Space[10];
    for (int i = 0; i < countSpace; i ++) {
      listSpace[i] = this.getSpaceInstance(i);
      spaceStorage.saveSpace(listSpace[i], true);
      tearDownSpaceList.add(listSpace[i]);
    }
    List<Space> editableSpaces = spaceStorage.getEditableSpaces(DEMO);
    assertNotNull(editableSpaces);
    assertEquals(countSpace, editableSpaces.size());

    editableSpaces = spaceStorage.getEditableSpaces("top");
    assertNotNull(editableSpaces);
    assertEquals(0, editableSpaces.size());

    editableSpaces = spaceStorage.getEditableSpaces(DRAGON);
    assertNotNull(editableSpaces);
    assertEquals(0, editableSpaces.size());
  }

  /**
   * Test {@link org.exoplatform.social.core.storage.SpaceStorage#getEditableSpacesByFilter(String, org.exoplatform.social.core.space.SpaceFilter, long, long)}
   *
   * @throws Exception
   * @since 1.2.0-GA
   */
  @MaxQueryNumber(700)
  public void testGetEditableSpacesByFilter() throws Exception {
    int countSpace = 10;
    Space []listSpace = new Space[10];
    for (int i = 0; i < countSpace; i ++) {
      listSpace[i] = this.getSpaceInstance(i);
      spaceStorage.saveSpace(listSpace[i], true);
      tearDownSpaceList.add(listSpace[i]);
    }
    List<Space> editableSpaces = spaceStorage.getEditableSpacesByFilter(DEMO, new SpaceFilter("add new"), 0 , 10);
    assertNotNull(editableSpaces);
    assertEquals(countSpace, editableSpaces.size());

    editableSpaces = spaceStorage.getEditableSpacesByFilter(DEMO, new SpaceFilter("m"), 0 , 10);
    assertNotNull(editableSpaces);
    assertEquals(countSpace, editableSpaces.size());

    editableSpaces = spaceStorage.getEditableSpacesByFilter(DEMO, new SpaceFilter("M"), 0 , 10);
    assertNotNull(editableSpaces);
    assertEquals(countSpace, editableSpaces.size());

    editableSpaces = spaceStorage.getEditableSpacesByFilter(DEMO, new SpaceFilter('m'), 0 , 10);
    assertNotNull(editableSpaces);
    assertEquals(countSpace, editableSpaces.size());

    editableSpaces = spaceStorage.getEditableSpacesByFilter(DEMO, new SpaceFilter('M'), 0 , 10);
    assertNotNull(editableSpaces);
    assertEquals(countSpace, editableSpaces.size());

    editableSpaces = spaceStorage.getEditableSpacesByFilter(DEMO, new SpaceFilter('K'), 0 , 10);
    assertNotNull(editableSpaces);
    assertEquals(0, editableSpaces.size());

    editableSpaces = spaceStorage.getEditableSpacesByFilter(DEMO, new SpaceFilter("add new"), 0 , 10);
    assertNotNull(editableSpaces);
    assertEquals(countSpace, editableSpaces.size());

    editableSpaces = spaceStorage.getEditableSpacesByFilter("top", new SpaceFilter("my space"), 0, 10);
    assertNotNull(editableSpaces);
    assertEquals(0, editableSpaces.size());

    editableSpaces = spaceStorage.getEditableSpacesByFilter(DRAGON, new SpaceFilter("m"), 0, 10);
    assertNotNull(editableSpaces);
    assertEquals(0, editableSpaces.size());

    editableSpaces = spaceStorage.getEditableSpacesByFilter(DRAGON, new SpaceFilter('m'), 0, 10);
    assertNotNull(editableSpaces);
    assertEquals(0, editableSpaces.size());

    editableSpaces = spaceStorage.getEditableSpacesByFilter(DRAGON, new SpaceFilter('M'), 0, 10);
    assertNotNull(editableSpaces);
    assertEquals(0, editableSpaces.size());

    editableSpaces = spaceStorage.getEditableSpacesByFilter(DRAGON, new SpaceFilter('k'), 0, 10);
    assertNotNull(editableSpaces);
    assertEquals(0, editableSpaces.size());
  }

  /**
   * Test {@link org.exoplatform.social.core.storage.SpaceStorage#getEditableSpacesByFilterCount(String, org.exoplatform.social.core.space.SpaceFilter)}
   *
   * @throws Exception
   * @since 1.2.0-GA
   */
  @MaxQueryNumber(700)
  public void testGetEditableSpacesByFilterCount() throws Exception {
    int countSpace = 10;
    Space []listSpace = new Space[10];
    for (int i = 0; i < countSpace; i ++) {
      listSpace[i] = this.getSpaceInstance(i);
      spaceStorage.saveSpace(listSpace[i], true);
      tearDownSpaceList.add(listSpace[i]);
    }
    int editableSpacesCount = spaceStorage.getEditableSpacesByFilterCount(DEMO, new SpaceFilter("add new"));
    assertEquals(countSpace, editableSpacesCount);

    editableSpacesCount = spaceStorage.getEditableSpacesByFilterCount(DEMO, new SpaceFilter("m"));
    assertEquals(countSpace, editableSpacesCount);

    editableSpacesCount = spaceStorage.getEditableSpacesByFilterCount(DEMO, new SpaceFilter("M"));
    assertEquals(countSpace, editableSpacesCount);

    editableSpacesCount = spaceStorage.getEditableSpacesByFilterCount(DEMO, new SpaceFilter('m'));
    assertEquals(countSpace, editableSpacesCount);

    editableSpacesCount = spaceStorage.getEditableSpacesByFilterCount(DEMO, new SpaceFilter('M'));
    assertEquals(countSpace, editableSpacesCount);

    editableSpacesCount = spaceStorage.getEditableSpacesByFilterCount(DEMO, new SpaceFilter('K'));
    assertEquals(0, editableSpacesCount);

    editableSpacesCount = spaceStorage.getEditableSpacesByFilterCount(TOM, new SpaceFilter("add new"));
    assertEquals(countSpace, editableSpacesCount);

    editableSpacesCount = spaceStorage.getEditableSpacesByFilterCount("top", new SpaceFilter("my space"));
    assertEquals(0, editableSpacesCount);

    editableSpacesCount = spaceStorage.getEditableSpacesByFilterCount(DRAGON, new SpaceFilter("m"));
    assertEquals(0, editableSpacesCount);

    editableSpacesCount = spaceStorage.getEditableSpacesByFilterCount(DRAGON, new SpaceFilter('m'));
    assertEquals(0, editableSpacesCount);

    editableSpacesCount = spaceStorage.getEditableSpacesByFilterCount(DRAGON, new SpaceFilter('M'));
    assertEquals(0, editableSpacesCount);

    editableSpacesCount = spaceStorage.getEditableSpacesByFilterCount(DRAGON, new SpaceFilter('k'));
    assertEquals(0, editableSpacesCount);
  }

  /**
   * Test {@link org.exoplatform.social.core.storage.SpaceStorage#getEditableSpaces(String, long, long)}
   *
   * @throws Exception
   * @since 1.2.0-GA
   */
  @MaxQueryNumber(700)
  public void testGetEditableSpacesWithListAccess() throws Exception {
    int countSpace = 10;
    Space []listSpace = new Space[10];
    for (int i = 0; i < countSpace; i ++) {
      listSpace[i] = this.getSpaceInstance(i);
      spaceStorage.saveSpace(listSpace[i], true);
      tearDownSpaceList.add(listSpace[i]);
    }
    List<Space> editableSpaces = spaceStorage.getEditableSpaces(DEMO, 0, countSpace);
    assertNotNull(editableSpaces);
    assertEquals(countSpace, editableSpaces.size());

    editableSpaces = spaceStorage.getEditableSpaces("top", 0, countSpace);
    assertNotNull(editableSpaces);
    assertEquals(0, editableSpaces.size());

    editableSpaces = spaceStorage.getEditableSpaces(DRAGON, 0, 5);
    assertNotNull(editableSpaces);
    assertEquals(0, editableSpaces.size());
  }

  /**
   * Test {@link org.exoplatform.social.core.storage.SpaceStorage#getInvitedSpaces(String)}
   *
   * @throws Exception
   * @since 1.2.0-GA
   */
  @MaxQueryNumber(700)
  public void testGetInvitedSpaces() throws Exception {
    int countSpace = 10;
    Space []listSpace = new Space[10];
    for (int i = 0; i < countSpace; i ++) {
      listSpace[i] = this.getSpaceInstance(i);
      spaceStorage.saveSpace(listSpace[i], true);
      tearDownSpaceList.add(listSpace[i]);
    }
    List<Space> invitedSpaces = spaceStorage.getInvitedSpaces(REGISTER_1);
    assertNotNull(invitedSpaces);
    assertEquals(countSpace, invitedSpaces.size());

    invitedSpaces = spaceStorage.getInvitedSpaces("register");
    assertNotNull(invitedSpaces);
    assertEquals(0, invitedSpaces.size());

    invitedSpaces = spaceStorage.getInvitedSpaces(MARY);
    assertNotNull(invitedSpaces);
    assertEquals(countSpace, invitedSpaces.size());

    invitedSpaces = spaceStorage.getInvitedSpaces(DEMO);
    assertNotNull(invitedSpaces);
    assertEquals(0, invitedSpaces.size());
  }

  /**
   * Test {@link org.exoplatform.social.core.storage.SpaceStorage#getInvitedSpacesByFilter(String, org.exoplatform.social.core.space.SpaceFilter, long, long)}
   *
   * @throws Exception
   * @since 1.2.0-GA
   */
  @MaxQueryNumber(700)
  public void testGetInvitedSpacesByFilter() throws Exception {
    int countSpace = 10;
    Space []listSpace = new Space[10];
    for (int i = 0; i < countSpace; i ++) {
      listSpace[i] = this.getSpaceInstance(i);
      spaceStorage.saveSpace(listSpace[i], true);
      tearDownSpaceList.add(listSpace[i]);
    }
    List<Space> invitedSpaces = spaceStorage.getInvitedSpacesByFilter(REGISTER_1, new SpaceFilter("add new"), 0, 10);
    assertNotNull(invitedSpaces);
    assertEquals(countSpace, invitedSpaces.size());

    invitedSpaces = spaceStorage.getInvitedSpacesByFilter(REGISTER_1, new SpaceFilter('m'), 0, 10);
    assertNotNull(invitedSpaces);
    assertEquals(countSpace, invitedSpaces.size());

    invitedSpaces = spaceStorage.getInvitedSpacesByFilter(REGISTER_1, new SpaceFilter('M'), 0, 10);
    assertNotNull(invitedSpaces);
    assertEquals(countSpace, invitedSpaces.size());

    invitedSpaces = spaceStorage.getInvitedSpacesByFilter(REGISTER_1, new SpaceFilter('k'), 0, 10);
    assertNotNull(invitedSpaces);
    assertEquals(0, invitedSpaces.size());

    invitedSpaces = spaceStorage.getInvitedSpacesByFilter("register", new SpaceFilter("my space "), 0, 10);
    assertNotNull(invitedSpaces);
    assertEquals(0, invitedSpaces.size());

    invitedSpaces = spaceStorage.getInvitedSpacesByFilter(MARY, new SpaceFilter("add"), 0, 10);
    assertNotNull(invitedSpaces);
    assertEquals(countSpace, invitedSpaces.size());

    invitedSpaces = spaceStorage.getInvitedSpacesByFilter(DEMO, new SpaceFilter("my"), 0, 10);
    assertNotNull(invitedSpaces);
    assertEquals(0, invitedSpaces.size());
  }

  /**
   * Test {@link org.exoplatform.social.core.storage.SpaceStorage#getInvitedSpacesByFilterCount(String, org.exoplatform.social.core.space.SpaceFilter)}
   *
   * @throws Exception
   * @since 1.2.0-GA
   */
  @MaxQueryNumber(700)
  public void testGetInvitedSpacesByFilterCount() throws Exception {
    int countSpace = 10;
    Space []listSpace = new Space[10];
    for (int i = 0; i < countSpace; i ++) {
      listSpace[i] = this.getSpaceInstance(i);
      spaceStorage.saveSpace(listSpace[i], true);
      tearDownSpaceList.add(listSpace[i]);
    }
    int invitedSpacesCount = spaceStorage.getInvitedSpacesByFilterCount(REGISTER_1, new SpaceFilter("add new"));
    assertEquals(countSpace, invitedSpacesCount);

    invitedSpacesCount = spaceStorage.getInvitedSpacesByFilterCount(REGISTER_1, new SpaceFilter('m'));
    assertEquals(countSpace, invitedSpacesCount);

    invitedSpacesCount = spaceStorage.getInvitedSpacesByFilterCount(REGISTER_1, new SpaceFilter('M'));
    assertEquals(countSpace, invitedSpacesCount);

    invitedSpacesCount = spaceStorage.getInvitedSpacesByFilterCount(REGISTER_1, new SpaceFilter('k'));
    assertEquals(0, invitedSpacesCount);

    invitedSpacesCount = spaceStorage.getInvitedSpacesByFilterCount("register", new SpaceFilter("my space "));
    assertEquals(0, invitedSpacesCount);

    invitedSpacesCount = spaceStorage.getInvitedSpacesByFilterCount(MARY, new SpaceFilter("add"));
    assertEquals(countSpace, invitedSpacesCount);

    invitedSpacesCount = spaceStorage.getInvitedSpacesByFilterCount(DEMO, new SpaceFilter("my"));
    assertEquals(0, invitedSpacesCount);
  }

  /**
   * Test {@link org.exoplatform.social.core.storage.SpaceStorage#getInvitedSpaces(String, long, long)}
   *
   * @throws Exception
   * @since 1.2.0-GA
   */
  @MaxQueryNumber(700)
  public void testGetInvitedSpacesWithOffset() throws Exception {
    int countSpace = 10;
    Space []listSpace = new Space[10];
    for (int i = 0; i < countSpace; i ++) {
      listSpace[i] = this.getSpaceInstance(i);
      spaceStorage.saveSpace(listSpace[i], true);
      tearDownSpaceList.add(listSpace[i]);
    }
    List<Space> invitedSpaces = spaceStorage.getInvitedSpaces(REGISTER_1, 0, 5);
    assertNotNull(invitedSpaces);
    assertEquals(5, invitedSpaces.size());

    invitedSpaces = spaceStorage.getInvitedSpaces("register", 0, 5);
    assertNotNull(invitedSpaces);
    assertEquals(0, invitedSpaces.size());

    invitedSpaces = spaceStorage.getInvitedSpaces(MARY, 0, 5);
    assertNotNull(invitedSpaces);
    assertEquals(5, invitedSpaces.size());

    invitedSpaces = spaceStorage.getInvitedSpaces(DEMO, 0, 5);
    assertNotNull(invitedSpaces);
    assertEquals(0, invitedSpaces.size());
  }

  /**
   * Test {@link org.exoplatform.social.core.storage.SpaceStorage#getInvitedSpacesCount(String)}
   *
   * @throws Exception
   * @since 1.2.0-GA
   */
  @MaxQueryNumber(700)
  public void testGetInvitedSpacesCount() throws Exception {
    int countSpace = 10;
    Space []listSpace = new Space[10];
    for (int i = 0; i < countSpace; i ++) {
      listSpace[i] = this.getSpaceInstance(i);
      spaceStorage.saveSpace(listSpace[i], true);
      tearDownSpaceList.add(listSpace[i]);
    }
    int invitedSpacesCount = spaceStorage.getInvitedSpacesCount(REGISTER_1);
    assertEquals(countSpace, invitedSpacesCount);

    invitedSpacesCount = spaceStorage.getInvitedSpacesCount(MARY);
    assertEquals(countSpace, invitedSpacesCount);

    invitedSpacesCount = spaceStorage.getInvitedSpacesCount(ANONYMOUS);
    assertEquals(0, invitedSpacesCount);
  }

  /**
   * Test {@link org.exoplatform.social.core.storage.SpaceStorage#getPendingSpaces(String)}
   *
   * @throws Exception
   * @since 1.2.0-GA
   */
  @MaxQueryNumber(700)
  public void testGetPendingSpaces() throws Exception {
    int countSpace = 10;
    Space []listSpace = new Space[10];
    for (int i = 0; i < countSpace; i ++) {
      listSpace[i] = this.getSpaceInstance(i);
      spaceStorage.saveSpace(listSpace[i], true);
      tearDownSpaceList.add(listSpace[i]);
    }
    List<Space> pendingSpaces = spaceStorage.getPendingSpaces(HACKER);
    assertNotNull(pendingSpaces);
    assertEquals(countSpace, pendingSpaces.size());

    pendingSpaces = spaceStorage.getPendingSpaces("hack");
    assertNotNull(pendingSpaces);
    assertEquals(0, pendingSpaces.size());

    pendingSpaces = spaceStorage.getPendingSpaces(PAUL);
    assertNotNull(pendingSpaces);
    assertEquals(countSpace, pendingSpaces.size());

    pendingSpaces = spaceStorage.getPendingSpaces(JAME);
    assertNotNull(pendingSpaces);
    assertEquals(countSpace, pendingSpaces.size());

    pendingSpaces = spaceStorage.getPendingSpaces("victory");
    assertNotNull(pendingSpaces);
    assertEquals(0, pendingSpaces.size());
  }

  /**
   * Test {@link org.exoplatform.social.core.storage.SpaceStorage#getPendingSpacesByFilter(String, org.exoplatform.social.core.space.SpaceFilter, long, long)}
   *
   * @throws Exception
   * @since 1.2.0-GA
   */
  @MaxQueryNumber(700)
  public void testGetPendingSpacesByFilter() throws Exception {
    int countSpace = 10;
    Space []listSpace = new Space[10];
    for (int i = 0; i < countSpace; i ++) {
      listSpace[i] = this.getSpaceInstance(i);
      spaceStorage.saveSpace(listSpace[i], true);
      tearDownSpaceList.add(listSpace[i]);
    }
    List<Space> pendingSpaces = spaceStorage.getPendingSpacesByFilter(HACKER, new SpaceFilter("add new"), 0, 10);
    assertNotNull(pendingSpaces);
    assertEquals(countSpace, pendingSpaces.size());

    pendingSpaces = spaceStorage.getPendingSpacesByFilter(HACKER, new SpaceFilter('m'), 0, 10);
    assertNotNull(pendingSpaces);
    assertEquals(countSpace, pendingSpaces.size());

    pendingSpaces = spaceStorage.getPendingSpacesByFilter(HACKER, new SpaceFilter('M'), 0, 10);
    assertNotNull(pendingSpaces);
    assertEquals(countSpace, pendingSpaces.size());

    pendingSpaces = spaceStorage.getPendingSpacesByFilter(HACKER, new SpaceFilter('k'), 0, 10);
    assertNotNull(pendingSpaces);
    assertEquals(0, pendingSpaces.size());

    pendingSpaces = spaceStorage.getPendingSpacesByFilter("hack", new SpaceFilter("my space"), 0, 10);
    assertNotNull(pendingSpaces);
    assertEquals(0, pendingSpaces.size());

    pendingSpaces = spaceStorage.getPendingSpacesByFilter("hack", new SpaceFilter('m'), 0, 10);
    assertNotNull(pendingSpaces);
    assertEquals(0, pendingSpaces.size());

    pendingSpaces = spaceStorage.getPendingSpacesByFilter("hack", new SpaceFilter('M'), 0, 10);
    assertNotNull(pendingSpaces);
    assertEquals(0, pendingSpaces.size());

    pendingSpaces = spaceStorage.getPendingSpacesByFilter("hack", new SpaceFilter('K'), 0, 10);
    assertNotNull(pendingSpaces);
    assertEquals(0, pendingSpaces.size());

    pendingSpaces = spaceStorage.getPendingSpacesByFilter(PAUL, new SpaceFilter("add"), 0, 10);
    assertNotNull(pendingSpaces);
    assertEquals(countSpace, pendingSpaces.size());

    pendingSpaces = spaceStorage.getPendingSpacesByFilter(JAME, new SpaceFilter("my"), 0, 10);
    assertNotNull(pendingSpaces);
    assertEquals(countSpace, pendingSpaces.size());

    pendingSpaces = spaceStorage.getPendingSpacesByFilter("victory", new SpaceFilter("my space "), 0, 10);
    assertNotNull(pendingSpaces);
    assertEquals(0, pendingSpaces.size());
  }

  /**
   * Test {@link org.exoplatform.social.core.storage.SpaceStorage#getPendingSpacesByFilterCount(String, org.exoplatform.social.core.space.SpaceFilter)}
   *
   * @throws Exception
   * @since 1.2.0-GA
   */
  @MaxQueryNumber(700)
  public void testGetPendingSpacesByFilterCount() throws Exception {
    int countSpace = 10;
    Space []listSpace = new Space[10];
    for (int i = 0; i < countSpace; i ++) {
      listSpace[i] = this.getSpaceInstance(i);
      spaceStorage.saveSpace(listSpace[i], true);
      tearDownSpaceList.add(listSpace[i]);
    }
    int pendingSpacesCount = spaceStorage.getPendingSpacesByFilterCount(HACKER, new SpaceFilter("add new"));
    assertEquals(countSpace, pendingSpacesCount);

    pendingSpacesCount = spaceStorage.getPendingSpacesByFilterCount(HACKER, new SpaceFilter('m'));
    assertEquals(countSpace, pendingSpacesCount);

    pendingSpacesCount = spaceStorage.getPendingSpacesByFilterCount(HACKER, new SpaceFilter('M'));
    assertEquals(countSpace, pendingSpacesCount);

    pendingSpacesCount = spaceStorage.getPendingSpacesByFilterCount(HACKER, new SpaceFilter('k'));
    assertEquals(0, pendingSpacesCount);

    pendingSpacesCount = spaceStorage.getPendingSpacesByFilterCount("hack", new SpaceFilter("my space"));
    assertEquals(0, pendingSpacesCount);

    pendingSpacesCount = spaceStorage.getPendingSpacesByFilterCount("hack", new SpaceFilter('m'));
    assertEquals(0, pendingSpacesCount);

    pendingSpacesCount = spaceStorage.getPendingSpacesByFilterCount("hack", new SpaceFilter('M'));
    assertEquals(0, pendingSpacesCount);

    pendingSpacesCount = spaceStorage.getPendingSpacesByFilterCount("hack", new SpaceFilter('K'));
    assertEquals(0, pendingSpacesCount);

    pendingSpacesCount = spaceStorage.getPendingSpacesByFilterCount(PAUL, new SpaceFilter("add"));
    assertEquals(countSpace, pendingSpacesCount);

    pendingSpacesCount = spaceStorage.getPendingSpacesByFilterCount(JAME, new SpaceFilter("my"));
    assertEquals(countSpace, pendingSpacesCount);

    pendingSpacesCount = spaceStorage.getPendingSpacesByFilterCount("victory", new SpaceFilter("my space "));
    assertEquals(0, pendingSpacesCount);
  }

  /**
   * Test {@link org.exoplatform.social.core.storage.SpaceStorage#getPendingSpaces(String, long, long)}
   *
   * @throws Exception
   * @since 1.2.0-GA
   */
  @MaxQueryNumber(700)
  public void testGetPendingSpacesWithOffset() throws Exception {
    int countSpace = 10;
    Space []listSpace = new Space[10];
    for (int i = 0; i < countSpace; i ++) {
      listSpace[i] = this.getSpaceInstance(i);
      spaceStorage.saveSpace(listSpace[i], true);
      tearDownSpaceList.add(listSpace[i]);
    }
    List<Space> pendingSpaces = spaceStorage.getPendingSpaces(HACKER, 0, 5);
    assertNotNull(pendingSpaces);
    assertEquals(5, pendingSpaces.size());

    pendingSpaces = spaceStorage.getPendingSpaces(PAUL, 0, 5);
    assertNotNull(pendingSpaces);
    assertEquals(5, pendingSpaces.size());

    pendingSpaces = spaceStorage.getPendingSpaces(JAME, 0, 5);
    assertNotNull(pendingSpaces);
    assertEquals(5, pendingSpaces.size());

    pendingSpaces = spaceStorage.getPendingSpaces("victory", 0, 5);
    assertNotNull(pendingSpaces);
    assertEquals(0, pendingSpaces.size());
  }

  /**
   * Test {@link org.exoplatform.social.core.storage.SpaceStorage#getPendingSpacesCount(String)}
   *
   * @throws Exception
   * @since 1.2.0-GA
   */
  @MaxQueryNumber(700)
  public void testGetPendingSpacesCount() throws Exception {
    int countSpace = 10;
    Space []listSpace = new Space[10];
    for (int i = 0; i < countSpace; i ++) {
      listSpace[i] = this.getSpaceInstance(i);
      spaceStorage.saveSpace(listSpace[i], true);
      tearDownSpaceList.add(listSpace[i]);
    }
    int pendingSpaceCount = spaceStorage.getPendingSpacesCount(JAME);
    assertEquals(countSpace, pendingSpaceCount);

    pendingSpaceCount = spaceStorage.getPendingSpacesCount(PAUL);
    assertEquals(countSpace, pendingSpaceCount);

    pendingSpaceCount = spaceStorage.getPendingSpacesCount(HACKER);
    assertEquals(countSpace, pendingSpaceCount);

    pendingSpaceCount = spaceStorage.getPendingSpacesCount(ANONYMOUS);
    assertEquals(0, pendingSpaceCount);
  }

  /**
   * Test {@link org.exoplatform.social.core.storage.SpaceStorage#getPublicSpaces(String)}
   *
   * @throws Exception
   * @since 1.2.0-GA
   */
  @MaxQueryNumber(700)
  public void testGetPublicSpaces() throws Exception {
    int countSpace = 10;
    Space []listSpace = new Space[10];
    for (int i = 0; i < countSpace; i ++) {
      listSpace[i] = this.getSpaceInstance(i);
      spaceStorage.saveSpace(listSpace[i], true);
      tearDownSpaceList.add(listSpace[i]);
    }
    List<Space> publicSpaces = spaceStorage.getPublicSpaces(MARY);
    assertNotNull(publicSpaces);
    assertEquals(0, publicSpaces.size());

    publicSpaces = spaceStorage.getPublicSpaces(DEMO);
    assertNotNull(publicSpaces);
    assertEquals(0, publicSpaces.size());
  }

  /**
   * Test {@link org.exoplatform.social.core.storage.SpaceStorage#getPublicSpacesByFilter(String, org.exoplatform.social.core.space.SpaceFilter, long, long)}
   *
   * @throws Exception
   * @since 1.2.0-GA
   */
  @MaxQueryNumber(700)
  public void testGetPublicSpacesByFilter() throws Exception {
    int countSpace = 10;
    Space []listSpace = new Space[10];
    for (int i = 0; i < countSpace; i ++) {
      listSpace[i] = this.getSpaceInstance(i);
      spaceStorage.saveSpace(listSpace[i], true);
      tearDownSpaceList.add(listSpace[i]);
    }
    List<Space> publicSpaces = spaceStorage.getPublicSpacesByFilter(mary.getRemoteId(), new SpaceFilter("add new"), 0, 10);
    assertNotNull(publicSpaces);
    assertEquals(0, publicSpaces.size());

    publicSpaces = spaceStorage.getPublicSpacesByFilter(mary.getRemoteId(), new SpaceFilter("my space"), 0, 10);
    assertNotNull(publicSpaces);
    assertEquals(0, publicSpaces.size());

    publicSpaces = spaceStorage.getPublicSpacesByFilter(mary.getRemoteId(), new SpaceFilter('m'), 0, 10);
    assertNotNull(publicSpaces);
    assertEquals(0, publicSpaces.size());

    publicSpaces = spaceStorage.getPublicSpacesByFilter(mary.getRemoteId(), new SpaceFilter('M'), 0, 10);
    assertNotNull(publicSpaces);
    assertEquals(0, publicSpaces.size());

    publicSpaces = spaceStorage.getPublicSpacesByFilter(demo.getRemoteId(), new SpaceFilter("my space"), 0, 10);
    assertNotNull(publicSpaces);
    assertEquals(0, publicSpaces.size());

    publicSpaces = spaceStorage.getPublicSpacesByFilter(anonymous.getRemoteId(), new SpaceFilter('m'), 0, 10);
    assertNotNull(publicSpaces);
    assertEquals(10, publicSpaces.size());

    publicSpaces = spaceStorage.getPublicSpacesByFilter(anonymous.getRemoteId(), new SpaceFilter('M'), 0, 10);
    assertNotNull(publicSpaces);
    assertEquals(10, publicSpaces.size());

    publicSpaces = spaceStorage.getPublicSpacesByFilter(anonymous.getRemoteId(), new SpaceFilter("add new "), 0, 10);
    assertNotNull(publicSpaces);
    assertEquals(10, publicSpaces.size());

    publicSpaces = spaceStorage.getPublicSpacesByFilter(anonymous.getRemoteId(), new SpaceFilter("my space "), 0, 10);
    assertNotNull(publicSpaces);
    assertEquals(10, publicSpaces.size());
  }

  /**
   * Test {@link org.exoplatform.social.core.storage.SpaceStorage#getPublicSpacesByFilterCount(String, org.exoplatform.social.core.space.SpaceFilter)}
   *
   * @throws Exception
   * @since 1.2.0-GA
   */
  @MaxQueryNumber(700)
  public void testGetPublicSpacesByFilterCount() throws Exception {
    int countSpace = 10;
    Space []listSpace = new Space[10];
    for (int i = 0; i < countSpace; i ++) {
      listSpace[i] = this.getSpaceInstance(i);
      spaceStorage.saveSpace(listSpace[i], true);
      tearDownSpaceList.add(listSpace[i]);
    }
    int publicSpacesByFilterCount = spaceStorage.getPublicSpacesByFilterCount(MARY, new SpaceFilter("add new"));
    assertEquals(0, publicSpacesByFilterCount);

    publicSpacesByFilterCount = spaceStorage.getPublicSpacesByFilterCount(MARY, new SpaceFilter("my space"));
    assertEquals(0, publicSpacesByFilterCount);

    publicSpacesByFilterCount = spaceStorage.getPublicSpacesByFilterCount(MARY, new SpaceFilter('m'));
    assertEquals(0, publicSpacesByFilterCount);

    publicSpacesByFilterCount = spaceStorage.getPublicSpacesByFilterCount(MARY, new SpaceFilter('M'));
    assertEquals(0, publicSpacesByFilterCount);

    publicSpacesByFilterCount = spaceStorage.getPublicSpacesByFilterCount(MARY, new SpaceFilter("my space"));
    assertEquals(0, publicSpacesByFilterCount);

    publicSpacesByFilterCount = spaceStorage.getPublicSpacesByFilterCount(ANONYMOUS, new SpaceFilter('m'));
    assertEquals(10, publicSpacesByFilterCount);

    publicSpacesByFilterCount = spaceStorage.getPublicSpacesByFilterCount(ANONYMOUS, new SpaceFilter('M'));
    assertEquals(10, publicSpacesByFilterCount);

    publicSpacesByFilterCount = spaceStorage.getPublicSpacesByFilterCount(ANONYMOUS, new SpaceFilter("add new "));
    assertEquals(10, publicSpacesByFilterCount);

    publicSpacesByFilterCount = spaceStorage.getPublicSpacesByFilterCount(ANONYMOUS, new SpaceFilter("my space "));
    assertEquals(10, publicSpacesByFilterCount);
  }

  /**
   * Test {@link org.exoplatform.social.core.storage.SpaceStorage#getPublicSpaces(String, long, long)}
   *
   * @throws Exception
   * @since 1.2.0-GA
   */
  @MaxQueryNumber(700)
  public void testGetPublicSpacesWithOffset() throws Exception {
    int countSpace = 10;
    Space []listSpace = new Space[10];
    for (int i = 0; i < countSpace; i ++) {
      listSpace[i] = this.getSpaceInstance(i);
      spaceStorage.saveSpace(listSpace[i], true);
      tearDownSpaceList.add(listSpace[i]);
    }
    List<Space> publicSpaces = spaceStorage.getPublicSpaces(MARY, 0, 5);
    assertNotNull(publicSpaces);
    assertEquals(0, publicSpaces.size());

    publicSpaces = spaceStorage.getPublicSpaces(DEMO, 0, 5);
    assertNotNull(publicSpaces);
    assertEquals(0, publicSpaces.size());

    publicSpaces = spaceStorage.getPublicSpaces("headshot", 0, 5);
    assertNotNull(publicSpaces);
    assertEquals(5, publicSpaces.size());

    publicSpaces = spaceStorage.getPublicSpaces("hellgate", 0, countSpace);
    assertNotNull(publicSpaces);
    assertEquals(countSpace, publicSpaces.size());
  }

  /**
   * Test {@link org.exoplatform.social.core.storage.SpaceStorage#getPublicSpacesCount(String)}
   *
   * @since 1.20.-GA
   * @throws Exception
   */
  @MaxQueryNumber(700)
  public void testGetPublicSpacesCount() throws Exception {
    int countSpace = 10;
    Space []listSpace = new Space[10];
    for (int i = 0; i < countSpace; i ++) {
      listSpace[i] = this.getSpaceInstance(i);
      spaceStorage.saveSpace(listSpace[i], true);
      tearDownSpaceList.add(listSpace[i]);
    }
    int publicSpacesCount = spaceStorage.getPublicSpacesCount(JAME);
    assertEquals(0, publicSpacesCount);

    publicSpacesCount = spaceStorage.getPublicSpacesCount(PAUL);
    assertEquals(0, publicSpacesCount);

    publicSpacesCount = spaceStorage.getPublicSpacesCount(HACKER);
    assertEquals(0, publicSpacesCount);

    publicSpacesCount = spaceStorage.getPublicSpacesCount(ANONYMOUS);
    assertEquals(countSpace, publicSpacesCount);
  }

  /**
   * Test {@link org.exoplatform.social.core.storage.SpaceStorage#getMemberSpaces(String)}
   *
   * @throws Exception
   * @since 1.2.0-GA
   */
  @MaxQueryNumber(700)
  public void testGetMemberSpaces() throws Exception {
    int countSpace = 10;
    Space []listSpace = new Space[10];
    for (int i = 0; i < countSpace; i ++) {
      listSpace[i] = this.getSpaceInstance(i);
      spaceStorage.saveSpace(listSpace[i], true);
      tearDownSpaceList.add(listSpace[i]);
    }

    List<Space> memberSpaces = spaceStorage.getMemberSpaces(RAUL);
    assertNotNull(memberSpaces);
    assertEquals(countSpace, memberSpaces.size());

    memberSpaces = spaceStorage.getMemberSpaces(GHOST);
    assertNotNull(memberSpaces);
    assertEquals(countSpace, memberSpaces.size());

    memberSpaces = spaceStorage.getMemberSpaces(DRAGON);
    assertNotNull(memberSpaces);
    assertEquals(countSpace, memberSpaces.size());

    memberSpaces = spaceStorage.getMemberSpaces(DEMO);
    assertNotNull(memberSpaces);
    assertEquals(0, memberSpaces.size());
  }

  /**
   * Test {@link org.exoplatform.social.core.storage.SpaceStorage#getMemberSpacesByFilter(String, org.exoplatform.social.core.space.SpaceFilter, long, long)}
   *
   * @throws Exception
   * @since 1.2.0-GA
   */
  @MaxQueryNumber(700)
  public void testGetMemberSpacesByFilter() throws Exception {
    int countSpace = 10;
    Space []listSpace = new Space[10];
    for (int i = 0; i < countSpace; i ++) {
      listSpace[i] = this.getSpaceInstance(i);
      spaceStorage.saveSpace(listSpace[i], true);
      tearDownSpaceList.add(listSpace[i]);
    }

    List<Space> memberSpaces = spaceStorage.getMemberSpacesByFilter(RAUL, new SpaceFilter("my space"), 0, 10);
    assertNotNull(memberSpaces);
    assertEquals(countSpace, memberSpaces.size());

    memberSpaces = spaceStorage.getMemberSpacesByFilter(GHOST, new SpaceFilter("add new"), 0, 10);
    assertNotNull(memberSpaces);
    assertEquals(countSpace, memberSpaces.size());

    memberSpaces = spaceStorage.getMemberSpacesByFilter(GHOST, new SpaceFilter("space"), 0, 10);
    assertNotNull(memberSpaces);
    assertEquals(countSpace, memberSpaces.size());

    memberSpaces = spaceStorage.getMemberSpacesByFilter(GHOST, new SpaceFilter("new"), 0, 10);
    assertNotNull(memberSpaces);
    assertEquals(countSpace, memberSpaces.size());

    memberSpaces = spaceStorage.getMemberSpacesByFilter(GHOST, new SpaceFilter('m'), 0, 10);
    assertNotNull(memberSpaces);
    assertEquals(countSpace, memberSpaces.size());

    memberSpaces = spaceStorage.getMemberSpacesByFilter(GHOST, new SpaceFilter('M'), 0, 10);
    assertNotNull(memberSpaces);
    assertEquals(countSpace, memberSpaces.size());

    memberSpaces = spaceStorage.getMemberSpacesByFilter(GHOST, new SpaceFilter('K'), 0, 10);
    assertNotNull(memberSpaces);
    assertEquals(0, memberSpaces.size());

    memberSpaces = spaceStorage.getMemberSpacesByFilter(DRAGON, new SpaceFilter("add"), 0, 10);
    assertNotNull(memberSpaces);
    assertEquals(countSpace, memberSpaces.size());

    memberSpaces = spaceStorage.getMemberSpacesByFilter(DEMO, new SpaceFilter("space"), 0, 10);
    assertNotNull(memberSpaces);
    assertEquals(0, memberSpaces.size());
  }

  /**
   * Test {@link org.exoplatform.social.core.storage.SpaceStorage#getMemberSpacesByFilterCount(String, org.exoplatform.social.core.space.SpaceFilter)}
   *
   * @throws Exception
   * @since 1.2.0-GA
   */
  @MaxQueryNumber(700)
  public void testGetMemberSpacesByFilterCount() throws Exception {
    int countSpace = 10;
    Space []listSpace = new Space[10];
    for (int i = 0; i < countSpace; i ++) {
      listSpace[i] = this.getSpaceInstance(i);
      spaceStorage.saveSpace(listSpace[i], true);
      tearDownSpaceList.add(listSpace[i]);
    }

    int memberSpacesCount = spaceStorage.getMemberSpacesByFilterCount(RAUL, new SpaceFilter("my space"));
    assertEquals(countSpace, memberSpacesCount);

    memberSpacesCount = spaceStorage.getMemberSpacesByFilterCount(GHOST, new SpaceFilter("add new"));
    assertEquals(countSpace, memberSpacesCount);

    memberSpacesCount = spaceStorage.getMemberSpacesByFilterCount(GHOST, new SpaceFilter("space"));
    assertEquals(countSpace, memberSpacesCount);

    memberSpacesCount = spaceStorage.getMemberSpacesByFilterCount(GHOST, new SpaceFilter("new"));
    assertEquals(countSpace, memberSpacesCount);

    memberSpacesCount = spaceStorage.getMemberSpacesByFilterCount(GHOST, new SpaceFilter('m'));
    assertEquals(countSpace, memberSpacesCount);

    memberSpacesCount = spaceStorage.getMemberSpacesByFilterCount(GHOST, new SpaceFilter('M'));
    assertEquals(countSpace, memberSpacesCount);

    memberSpacesCount = spaceStorage.getMemberSpacesByFilterCount(GHOST, new SpaceFilter('K'));
    assertEquals(0, memberSpacesCount);

    memberSpacesCount = spaceStorage.getMemberSpacesByFilterCount(DRAGON, new SpaceFilter("add"));
    assertEquals(countSpace, memberSpacesCount);

    memberSpacesCount = spaceStorage.getMemberSpacesByFilterCount(DEMO, new SpaceFilter("space"));
    assertEquals(0, memberSpacesCount);
  }

  /**
   * Test {@link org.exoplatform.social.core.storage.SpaceStorage#getMemberSpaces(String, long, long)}
   *
   * @throws Exception
   * @since 1.2.0-GA
   */
  @MaxQueryNumber(700)
  public void testGetMemberSpacesWithListAccess() throws Exception {
    int countSpace = 10;
    for (int i = 0; i < countSpace; i++) {
      Space space = this.getSpaceInstance(i);
      spaceStorage.saveSpace(space, true);
      tearDownSpaceList.add(space);
    }

    List<Space> memberSpaces = spaceStorage.getMemberSpaces(RAUL, 0, 5);
    assertNotNull(memberSpaces);
    assertEquals(5, memberSpaces.size());

    memberSpaces = spaceStorage.getMemberSpaces(GHOST, 0, countSpace);
    assertNotNull(memberSpaces);
    assertEquals(countSpace, memberSpaces.size());

    memberSpaces = spaceStorage.getMemberSpaces(DRAGON, 0, 6);
    assertNotNull(memberSpaces);
    assertEquals(6, memberSpaces.size());

    memberSpaces = spaceStorage.getMemberSpaces(DEMO, 0, countSpace);
    assertNotNull(memberSpaces);
    assertEquals(0, memberSpaces.size());
  }

  /**
   * Test {@link org.exoplatform.social.core.storage.SpaceStorage#getSpaceById(String)}
   *
   * @throws Exception
   */
  @MaxQueryNumber(100)
  public void testGetSpaceById() throws Exception {
    int number = 1;
    Space space = this.getSpaceInstance(number);

    spaceStorage.saveSpace(space, true);
    tearDownSpaceList.add(space);

    Space savedSpace = spaceStorage.getSpaceById(space.getId());
    assertNotNull(savedSpace);
    assertNotNull(savedSpace.getId());
    assertNotNull(savedSpace.getApp());
    assertEquals(space.getId(), savedSpace.getId());
    assertEquals(space.getPrettyName(), savedSpace.getPrettyName());
    assertEquals(space.getRegistration(), savedSpace.getRegistration());
    assertEquals(space.getDescription(), savedSpace.getDescription());
    assertEquals(space.getType(), savedSpace.getType());
    assertEquals(space.getVisibility(), savedSpace.getVisibility());
    assertEquals(space.getPriority(), savedSpace.getPriority());
  }

  /**
   * Test {@link org.exoplatform.social.core.storage.SpaceStorage#getSpaceByGroupId(String)}
   *
   * @throws Exception
   */
  @MaxQueryNumber(100)
  public void testGetSpaceByGroupId() throws Exception {
    Space space = getSpaceInstance(1);
    spaceStorage.saveSpace(space, true);

    Space savedSpace = spaceStorage.getSpaceByGroupId(space.getGroupId());

    assertNotNull(savedSpace);
    assertEquals(space.getId(), savedSpace.getId());

    tearDownSpaceList.add(savedSpace);

  }

  /**
   * Test {@link org.exoplatform.social.core.storage.SpaceStorage#getSpaceByUrl(String)}
   *
   * @throws Exception
   */
  @MaxQueryNumber(100)
  public void testGetSpaceByUrl() throws Exception {
    int number = 1;
    Space space = this.getSpaceInstance(number);
    space.setUrl("http://fake.com.vn");
    spaceStorage.saveSpace(space, true);
    tearDownSpaceList.add(space);

    // get saved space
    Space savedSpace = spaceStorage.getSpaceByUrl(space.getUrl());
    assertNotNull(savedSpace);
    assertNotNull(savedSpace.getId());
    assertEquals(space.getId(), savedSpace.getId());
    assertEquals(space.getPrettyName(), savedSpace.getPrettyName());

    //Show that getName() is the same as getPrettyname
    assertTrue(savedSpace.getPrettyName().equals(savedSpace.getPrettyName()));

    assertEquals(space.getRegistration(), savedSpace.getRegistration());
    assertEquals(space.getDescription(), savedSpace.getDescription());
    assertEquals(space.getType(), savedSpace.getType());
    assertEquals(space.getVisibility(), savedSpace.getVisibility());
    assertEquals(space.getPriority(), savedSpace.getPriority());
    assertEquals(space.getUrl(), savedSpace.getUrl());
  }

  /**
   * Test {@link org.exoplatform.social.core.storage.SpaceStorage#getSpaceByPrettyName(String)}
   *
   * @throws Exception
   */
  @MaxQueryNumber(100)
  public void testGetSpaceByPrettyName() throws Exception {
    // number for method getSpaceInstance(int number)
    int number = 1;
    // new space
    Space space = this.getSpaceInstance(number);

    // add to tearDownSpaceList
    tearDownSpaceList.add(space);
    // save to space activityStorage
    spaceStorage.saveSpace(space, true);

    // get space saved by name
    Space foundSpaceList = spaceStorage.getSpaceByPrettyName(space.getPrettyName());
    assertNotNull(foundSpaceList);
    assertNotNull(foundSpaceList.getId());
    assertEquals(space.getId(), foundSpaceList.getId());
    assertEquals(space.getPrettyName(), foundSpaceList.getPrettyName());
    assertEquals(space.getRegistration(), foundSpaceList.getRegistration());
    assertEquals(space.getDescription(), foundSpaceList.getDescription());
    assertEquals(space.getType(), foundSpaceList.getType());
    assertEquals(space.getVisibility(), foundSpaceList.getVisibility());
    assertEquals(space.getPriority(), foundSpaceList.getPriority());
  }

  /**
   * Test {@link org.exoplatform.social.core.storage.SpaceStorage#deleteSpace(String)}
   *
   * @throws Exception
   */
  @MaxQueryNumber(350)
  public void testDeleteSpace() throws Exception {
    int number = 1;
    Space space = this.getSpaceInstance(number);
    spaceStorage.saveSpace(space, true);
    spaceStorage.deleteSpace(space.getId());
  }

  /**
   * Test {@link org.exoplatform.social.core.storage.SpaceStorage#saveSpace(org.exoplatform.social.core.space.model.Space, boolean)}
   *
   * @throws Exception
   */
  @MaxQueryNumber(150)
  public void testSaveSpace() throws Exception {
    int number = 1;
    Space space = this.getSpaceInstance(number);
    tearDownSpaceList.add(space);
    spaceStorage.saveSpace(space, true);
    assertNotNull(space.getId());
    String newName = "newnamespace";
    space.setDisplayName(newName);
    space.setPrettyName(space.getDisplayName());
    spaceStorage.saveSpace(space, false);
    assertEquals(newName, spaceStorage.getSpaceById(space.getId()).getPrettyName());
    assertEquals(newName, space.getPrettyName());

    Space got = spaceStorage.getSpaceById(space.getId());
    assertEquals(null, got.getAvatarUrl());
  }

  /**
   * Test {@link SpaceStorage#renameSpace(Space, String)}
   *
   * @throws Exception
   * @since 1.2.8
   */
  @MaxQueryNumber(400)
  public void testRenameSpace() throws Exception {
    int number = 1;
    Space space = this.getSpaceInstance(number);
    tearDownSpaceList.add(space);
    spaceStorage.saveSpace(space, true);
    assertNotNull(space.getId());
    String newName = "newnamespace";
    space.setDisplayName(newName);
    space.setPrettyName(space.getDisplayName());
    spaceStorage.saveSpace(space, false);
    assertEquals(newName, spaceStorage.getSpaceById(space.getId()).getPrettyName());
    assertEquals(newName, space.getPrettyName());

    Space got = spaceStorage.getSpaceById(space.getId());
    assertEquals(null, got.getAvatarUrl());
    
    Identity spaceIdentity = new Identity(SpaceIdentityProvider.NAME, got.getPrettyName());
    identityStorage.saveIdentity(spaceIdentity);
    tearDownIdentityList.add(spaceIdentity);
    
    String newDisplayName = "new display name";
    spaceStorage.renameSpace(space, newDisplayName);
    
    got = spaceStorage.getSpaceById(space.getId());
    assertEquals(newDisplayName, got.getDisplayName());
  }
  
  /**
   * Test {@link org.exoplatform.social.core.storage.SpaceStorage#saveSpace(org.exoplatform.social.core.space.model.Space, boolean)}
   *
   * @throws Exception
   */
  @MaxQueryNumber(150)
  public void testSaveSpaceAvatar() throws Exception {
    int number = 1;
    Space space = this.getSpaceInstance(number);
    InputStream inputStream = getClass().getResourceAsStream("/eXo-Social.png");
    AvatarAttachment avatarAttachment = new AvatarAttachment(null, "avatar", "png", inputStream, null, System.currentTimeMillis());
    assertNotNull(avatarAttachment); 
    space.setAvatarAttachment(avatarAttachment);
    
    Identity identity = new Identity(SpaceIdentityProvider.NAME, space.getPrettyName());
    Profile profile = new Profile(identity);
    identity.setProfile(profile);
    profile.setProperty(Profile.AVATAR, avatarAttachment);
    identityStorage.saveIdentity(identity);
    identityStorage.saveProfile(profile);
    
    tearDownIdentityList.add(identity);
    spaceStorage.saveSpace(space, true);

    Space got = spaceStorage.getSpaceByPrettyName(space.getPrettyName());
    tearDownSpaceList.add(got);

    assertNotNull(got.getAvatarUrl());
    String avatarRandomURL = got.getAvatarUrl();
    int indexOfLastupdatedParam = avatarRandomURL.indexOf("/?upd=");
    String avatarURL = null;
    if(indexOfLastupdatedParam != -1){
      avatarURL = avatarRandomURL.substring(0,indexOfLastupdatedParam);
    } else {
      avatarURL = avatarRandomURL;
    }
    assertEquals(LinkProvider.escapeJCRSpecialCharacters(
            String.format(
              "/rest/jcr/repository/portal-test/production/soc:providers/soc:space/soc:%s/soc:profile/soc:avatar",
              space.getPrettyName())),
              avatarURL);
    
  }

  /**
   * Test {@link org.exoplatform.social.core.storage.SpaceStorage#saveSpace(org.exoplatform.social.core.space.model.Space, boolean)} with isNew is false
   *
   * @throws Exception
   */
  @MaxQueryNumber(200)
  public void testUpdateSpace() throws Exception {
    int number = 1;
    Space space = this.getSpaceInstance(number);
    InputStream inputStream = getClass().getResourceAsStream("/eXo-Social.png");
    AvatarAttachment avatarAttachment = new AvatarAttachment(null, "avatar", "png", inputStream, null, System.currentTimeMillis());
    assertNotNull(avatarAttachment);
    space.setAvatarAttachment(avatarAttachment);
    tearDownSpaceList.add(space);
    spaceStorage.saveSpace(space, true);
    
    Identity identity = new Identity(SpaceIdentityProvider.NAME, space.getPrettyName());
    Profile profile = new Profile(identity);
    identity.setProfile(profile);
    profile.setProperty(Profile.AVATAR, avatarAttachment);
    identityStorage.saveIdentity(identity);
    identityStorage.saveProfile(profile);
    tearDownIdentityList.add(identity);

    //
    Space spaceForUpdate = spaceStorage.getSpaceById(space.getId());
    spaceStorage.saveSpace(spaceForUpdate, false);

    //
    Space got = spaceStorage.getSpaceById(spaceForUpdate.getId());

    assertNotNull(got.getAvatarUrl());
  }
  
  /**
   * Test {@link org.exoplatform.social.core.storage.SpaceStorage#getVisibleSpaces(String)}
   *
   * @throws Exception
   * @since 1.2.5-GA
   */
  @MaxQueryNumber(200)
  public void testGetVisibleSpaces() throws Exception {
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
      
      spaceStorage.saveSpace(listSpace[i], true);
      tearDownSpaceList.add(listSpace[i]);
    }
    
    //visible with remoteId = 'demo'  return 10 spaces with SpaceFilter = null
    {
      int countSpace1 = 10;
      List<Space> visibleAllSpaces = spaceStorage.getVisibleSpaces(DEMO, null);
      assertNotNull(visibleAllSpaces);
      assertEquals(countSpace1, visibleAllSpaces.size());
    }
    
    //visible with remoteId = 'demo'  return 10 spaces with SpaceFilter configured firstCharacter 'M'
    {
      int countSpace2 = 10;
      List<Space> visibleAllSpaces = spaceStorage.getVisibleSpaces(DEMO, new SpaceFilter('M'));
      assertNotNull(visibleAllSpaces);
      assertEquals(countSpace2, visibleAllSpaces.size());
    }
    
    //visible with remoteId = 'demo'  return 0 spaces with SpaceFilter configured firstCharacter 'A'
    {
      int countSpace3 = 0;
      List<Space> visibleAllSpaces = spaceStorage.getVisibleSpaces(DEMO, new SpaceFilter('A'));
      assertNotNull(visibleAllSpaces);
      assertEquals(countSpace3, visibleAllSpaces.size());
    }
    
    //visible with remoteId = 'mary'  return 6 spaces with SpaceFilter = null
    {
      int privateSpace1 = 6;
      List<Space> privateSpaces = spaceStorage.getVisibleSpaces(MARY, null);
      assertNotNull(privateSpaces);
      assertEquals(privateSpace1, privateSpaces.size());
    }
    
    //visible with remoteId = 'mary'  return 6 spaces with SpaceFilter configured firstCharacter 'M'
    {
      int privateSpace2 = 6;
      List<Space> privateSpaces = spaceStorage.getVisibleSpaces(MARY, new SpaceFilter('M'));
      assertNotNull(privateSpaces);
      assertEquals(privateSpace2, privateSpaces.size());
    }
    
    //visible with remoteId = 'mary'  return 0 spaces with SpaceFilter configured firstCharacter 'A'
    {
      int privateSpace3 = 0;
      List<Space> privateSpaces = spaceStorage.getVisibleSpaces(MARY, new SpaceFilter('A'));
      assertNotNull(privateSpaces);
      assertEquals(privateSpace3, privateSpaces.size());
    }
  }
  
  /**
   * Test {@link org.exoplatform.social.core.storage.SpaceStorage#getVisibleSpaces(String)}
   *
   * @throws Exception
   * @since 1.2.5-GA
   */
  @MaxQueryNumber(200)
  public void testGetVisibleSpacesWithValidate() throws Exception {
    int countSpace = 10;
    Space []listSpace = new Space[10];
    
    //there are 6 spaces with visible = 'private'
    for (int i = 0; i < countSpace; i ++) {
    
      if (i < 6)
         //[0->5] :: there are 6 spaces with visible = 'private'
        listSpace[i] = this.getSpaceInstance(i, Space.PRIVATE, Space.VALIDATION, DEMO);
      else
        //[6->9]:: there are 4 spaces with visible = 'hidden'
        listSpace[i] = this.getSpaceInstance(i, Space.HIDDEN, Space.VALIDATION, DEMO);
      
      spaceStorage.saveSpace(listSpace[i], true);
      tearDownSpaceList.add(listSpace[i]);
    }
    
    //visible with remoteId = 'demo'  return 10 spaces with SpaceFilter = null
    {
      int countSpace1 = 10;
      List<Space> visibleAllSpaces = spaceStorage.getVisibleSpaces(DEMO, null);
      assertNotNull(visibleAllSpaces);
      assertEquals(countSpace1, visibleAllSpaces.size());
    }
    
    //visible with remoteId = 'demo'  return 10 spaces with SpaceFilter configured firstCharacter 'M'
    {
      int countSpace2 = 10;
      List<Space> visibleAllSpaces = spaceStorage.getVisibleSpaces(DEMO, new SpaceFilter('M'));
      assertNotNull(visibleAllSpaces);
      assertEquals(countSpace2, visibleAllSpaces.size());
    }
    
    //visible with remoteId = 'demo'  return 0 spaces with SpaceFilter configured firstCharacter 'A'
    {
      int countSpace3 = 0;
      List<Space> visibleAllSpaces = spaceStorage.getVisibleSpaces(DEMO, new SpaceFilter('A'));
      assertNotNull(visibleAllSpaces);
      assertEquals(countSpace3, visibleAllSpaces.size());
    }
    
    //visible with remoteId = 'mary'  return 6 spaces with SpaceFilter = null
    {
      int privateSpace1 = 6;
      List<Space> privateSpaces = spaceStorage.getVisibleSpaces(MARY, null);
      assertNotNull(privateSpaces);
      assertEquals(privateSpace1, privateSpaces.size());
    }
    
    //visible with remoteId = 'mary'  return 6 spaces with SpaceFilter configured firstCharacter 'M'
    {
      int privateSpace2 = 6;
      List<Space> privateSpaces = spaceStorage.getVisibleSpaces(MARY, new SpaceFilter('M'));
      assertNotNull(privateSpaces);
      assertEquals(privateSpace2, privateSpaces.size());
    }
    
    //visible with remoteId = 'mary'  return 0 spaces with SpaceFilter configured firstCharacter 'A'
    {
      int privateSpace3 = 0;
      List<Space> privateSpaces = spaceStorage.getVisibleSpaces(MARY, new SpaceFilter('A'));
      assertNotNull(privateSpaces);
      assertEquals(privateSpace3, privateSpaces.size());
    }
  }
  
  /**
   * Test {@link org.exoplatform.social.core.storage.SpaceStorage#getVisibleSpaces(String)}
   *
   * @throws Exception
   * @since 1.2.5-GA
   */
  @MaxQueryNumber(200)
  public void testGetVisibleSpacesFilterByName() throws Exception {
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
      
      spaceStorage.saveSpace(listSpace[i], true);
      tearDownSpaceList.add(listSpace[i]);
    }
    
    //visible with remoteId = 'demo'  return 10 spaces with SpaceFilter = null
    {
      int countSpace1 = 10;
      List<Space> visibleAllSpaces = spaceStorage.getVisibleSpaces(DEMO, null);
      assertNotNull(visibleAllSpaces);
      assertEquals(countSpace1, visibleAllSpaces.size());
    }
    
    //visible with remoteId = 'demo'  return 10 spaces with SpaceFilter configured firstCharacter 'M'
    {
      int countSpace2 = 10;
      List<Space> visibleAllSpaces = spaceStorage.getVisibleSpaces(DEMO, new SpaceFilter("my space"));
      assertNotNull(visibleAllSpaces);
      assertEquals(countSpace2, visibleAllSpaces.size());
    }
    
    //visible with remoteId = 'demo'  return 0 spaces with SpaceFilter configured firstCharacter 'A'
    {
      int countSpace3 = 0;
      List<Space> visibleAllSpaces = spaceStorage.getVisibleSpaces(DEMO, new SpaceFilter("your space"));
      assertNotNull(visibleAllSpaces);
      assertEquals(countSpace3, visibleAllSpaces.size());
    }
    
    //visible with remoteId = 'mary'  return 6 spaces with SpaceFilter = null
    {
      int privateSpace1 = 6;
      List<Space> privateSpaces = spaceStorage.getVisibleSpaces(MARY, null);
      assertNotNull(privateSpaces);
      assertEquals(privateSpace1, privateSpaces.size());
    }
    
    //visible with remoteId = 'mary'  return 6 spaces with SpaceFilter configured firstCharacter 'M'
    {
      int privateSpace2 = 6;
      List<Space> privateSpaces = spaceStorage.getVisibleSpaces(MARY, new SpaceFilter("my space"));
      assertNotNull(privateSpaces);
      assertEquals(privateSpace2, privateSpaces.size());
    }
    
    //visible with remoteId = 'mary'  return 0 spaces with SpaceFilter configured firstCharacter 'A'
    {
      int privateSpace3 = 0;
      List<Space> privateSpaces = spaceStorage.getVisibleSpaces(MARY, new SpaceFilter("your space"));
      assertNotNull(privateSpaces);
      assertEquals(privateSpace3, privateSpaces.size());
    }
  }
  
  /**
   * Test {@link org.exoplatform.social.core.storage.SpaceStorage#getVisibleSpaces(String)}
   *
   * @throws Exception
   * @since 1.2.5-GA
   */
  @MaxQueryNumber(200)
  public void testGetVisibleSpacesCloseRegistration() throws Exception {
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
      
      spaceStorage.saveSpace(listSpace[i], true);
      tearDownSpaceList.add(listSpace[i]);
    }
    
    //visible with remoteId = 'demo'  return 10 spaces
    {
      List<Space> visibleAllSpaces = spaceStorage.getVisibleSpaces(DEMO, null);
      assertNotNull(visibleAllSpaces);
      assertEquals(countSpace, visibleAllSpaces.size());
    }
    
    
    
    //visible with remoteId = 'mary'  return 6 spaces: can see
    {
      int registrationCloseSpaceCount = 6;
      List<Space> registrationCloseSpaces = spaceStorage.getVisibleSpaces(MARY, null);
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
  @MaxQueryNumber(200)
  public void testGetVisibleSpacesCloseRegistrationByFilter() throws Exception {
    int countSpace = 10;
    Space []listSpace = new Space[10];
    
    //there are 6 spaces with visible = 'private'
    for (int i = 0; i < countSpace; i ++) {
    
      if (i < 6)
         //[0->5] :: there are 6 spaces with visible = 'private' and manager = DEMO
        listSpace[i] = this.getSpaceInstance(i, Space.PRIVATE, Space.CLOSE, DEMO);
      else
        //[6->9]:: there are 4 spaces with visible = 'hidden'
        listSpace[i] = this.getSpaceInstance(i, Space.HIDDEN, Space.CLOSE, DEMO);
      
      spaceStorage.saveSpace(listSpace[i], true);
      tearDownSpaceList.add(listSpace[i]);
    }
    
    //visible with remoteId = 'demo'  return 10 spaces with SpaceFilter configured firstCharacter 'M'
    {
      int countSpace1 = 10;
      List<Space> visibleAllSpaces = spaceStorage.getVisibleSpaces(DEMO, new SpaceFilter('M'));
      assertNotNull(visibleAllSpaces);
      assertEquals(countSpace1, visibleAllSpaces.size());
    }
    
    //visible with remoteId = 'demo'  return 10 spaces with SpaceFilter configured firstCharacter 'M'
    {
      
      int countSpace2 = 0;
      List<Space> visibleAllSpaces = spaceStorage.getVisibleSpaces(DEMO, new SpaceFilter('A'));
      assertNotNull(visibleAllSpaces);
      assertEquals(countSpace2, visibleAllSpaces.size());
    }
    
    //visible with remoteId = 'demo'  return 10 spaces with SpaceFilter configured name "my space"
    {
      
      int countSpace3 = 10;
      List<Space> visibleAllSpaces = spaceStorage.getVisibleSpaces(DEMO, new SpaceFilter("my space"));
      assertNotNull(visibleAllSpaces);
      assertEquals(countSpace3, visibleAllSpaces.size());
    }
    
    //visible with remoteId = 'root'  return 10 spaces with SpaceFilter configured name "my space"
    {
      
      int countSpace4 = 10;
      List<Space> visibleAllSpaces = getSpaceWithRoot(new SpaceFilter("my space"));
      assertNotNull(visibleAllSpaces);
      assertEquals(countSpace4, visibleAllSpaces.size());
    }
    
   //visible with remoteId = 'root'  return 10 spaces with SpaceFilter is null.
    {
      
      int countSpace5 = 10;
      List<Space> visibleAllSpaces = getSpaceWithRoot(null);
      assertNotNull(visibleAllSpaces);
      assertEquals(countSpace5, visibleAllSpaces.size());
    }
    
    //visible with remoteId = 'root'  return 0 spaces with SpaceFilter configured name "my space"
    {
      
      int countSpace6 = 0;
      List<Space> visibleAllSpaces = getSpaceWithRoot(new SpaceFilter("your space"));
      assertNotNull(visibleAllSpaces);
      assertEquals(countSpace6, visibleAllSpaces.size());
    }
    
       
    //visible with remoteId = 'mary'  return 6 spaces: see although with SpaceFilter configured firstCharacter 'M'
    {
      int registrationCloseSpaceCount1 = 6;
      List<Space> registrationCloseSpaces = spaceStorage.getVisibleSpaces(MARY, new SpaceFilter('M'));
      assertNotNull(registrationCloseSpaces);
      assertEquals(registrationCloseSpaceCount1, registrationCloseSpaces.size());
    }
    
    //visible with remoteId = 'root'  return 10 spaces: see all spaces:: check at SpaceServiceImpl
    {
      int registrationCloseSpaceCount2 = 10;
      List<Space> registrationCloseSpaces1 = spaceStorage.getSpacesByFilter(new SpaceFilter('M'), 0, 200);
      assertNotNull(registrationCloseSpaces1);
      assertEquals(registrationCloseSpaceCount2, registrationCloseSpaces1.size());
    }
  }
  
  /**
   * Test {@link org.exoplatform.social.core.storage.SpaceStorage#getVisibleSpaces(String)}
   *
   * @throws Exception
   * @since 1.2.5-GA
   */
  @MaxQueryNumber(250)
  public void testGetVisibleSpacesInvitedMember() throws Exception {
    int countSpace = 10;
    Space[] listSpace = new Space[10];
    
    //there are 6 spaces with visible = 'private'
    for (int i = 0; i < countSpace; i ++) {
    
      if (i < 6)
         //[0->5] :: there are 6 spaces with visible = 'private'
        listSpace[i] = this.getSpaceInstanceInvitedMember(i, Space.PRIVATE, Space.CLOSE, new String[] {MARY, HACKER}, DEMO);
      else
        //[6->9]:: there are 4 spaces with visible = 'hidden'
        listSpace[i] = this.getSpaceInstance(i, Space.HIDDEN, Space.CLOSE, DEMO);
      
      spaceStorage.saveSpace(listSpace[i], true);
      tearDownSpaceList.add(listSpace[i]);
    }
    
    //visible with remoteId = 'demo'  return 10 spaces
    {
      List<Space> visibleAllSpaces = spaceStorage.getVisibleSpaces(DEMO, null);
      assertNotNull(visibleAllSpaces);
      assertEquals(countSpace, visibleAllSpaces.size());
    }
    
    //visible with invited = 'mary'  return 6 spaces
    {
      int invitedSpaceCount1 = 6;
      List<Space> invitedSpaces1 = spaceStorage.getVisibleSpaces(MARY, null);
      assertNotNull(invitedSpaces1);
      assertEquals(invitedSpaceCount1, invitedSpaces1.size());
    }
    
    //visible with invited = 'hacker'  return 6 spaces
    {
      int invitedSpaceCount1 = 6;
      List<Space> invitedSpaces1 = spaceStorage.getVisibleSpaces(HACKER, null);
      assertNotNull(invitedSpaces1);
      assertEquals(invitedSpaceCount1, invitedSpaces1.size());
    }
    
    //visible with invited = 'paul'  return 6 spaces
    {
      int invitedSpaceCount2 = 6;
      List<Space> invitedSpaces2 = spaceStorage.getVisibleSpaces(PAUL, null);
      assertNotNull(invitedSpaces2);
      assertEquals(invitedSpaceCount2, invitedSpaces2.size());
    }
  }
  
  // TODO : test getSpaceByGroupId without result
  // TODO : save space with null member[]
  // TODO : test space member number
  // TODO : test app data
  // TODO : test accepte invited / pending
}
