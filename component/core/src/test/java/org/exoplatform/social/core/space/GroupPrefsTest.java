/*
 * Copyright (C) 2003-2012 eXo Platform SAS.
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
package org.exoplatform.social.core.space;

import java.util.ArrayList;
import java.util.List;

import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.social.core.identity.model.Identity;
import org.exoplatform.social.core.identity.provider.OrganizationIdentityProvider;
import org.exoplatform.social.core.identity.provider.SpaceIdentityProvider;
import org.exoplatform.social.core.space.model.Space;
import org.exoplatform.social.core.space.spi.SpaceService;
import org.exoplatform.social.core.storage.api.IdentityStorage;
import org.exoplatform.social.core.test.AbstractCoreTest;

/**
 * Created by The eXo Platform SAS
 * Author : thanh_vucong
 *          thanh_vucong@exoplatform.com
 * Dec 7, 2012  
 */
public class GroupPrefsTest extends AbstractCoreTest {
  private SpaceService spaceService;
  private IdentityStorage identityStorage;
  private GroupPrefs prefers;
  private List<Space> tearDownSpaceList;
  private List<Identity> tearDownUserList;

  private final Log LOG = ExoLogger.getLogger(GroupPrefsTest.class);

  private Identity demo;
  private Identity tom;
  private Identity raul;
  private Identity ghost;
  private Identity dragon;
  private Identity john;
  private Identity mary;
  private Identity root;

  @Override
  public void setUp() throws Exception {
    super.setUp();
    spaceService = (SpaceService) getContainer().getComponentInstanceOfType(SpaceService.class);
    identityStorage = (IdentityStorage) getContainer().getComponentInstanceOfType(IdentityStorage.class);
    prefers = (GroupPrefs) getContainer().getComponentInstanceOfType(GroupPrefs.class);
    
    tearDownSpaceList = new ArrayList<Space>();
    tearDownUserList = new ArrayList<Identity>();
    
    demo = new Identity(OrganizationIdentityProvider.NAME, "demo");
    tom = new Identity(OrganizationIdentityProvider.NAME, "tom");
    raul = new Identity(OrganizationIdentityProvider.NAME, "raul");
    ghost = new Identity(OrganizationIdentityProvider.NAME, "ghost");
    dragon = new Identity(OrganizationIdentityProvider.NAME, "dragon");
    mary = new Identity(OrganizationIdentityProvider.NAME, "mary");
    john = new Identity(OrganizationIdentityProvider.NAME, "john");
    root = new Identity(OrganizationIdentityProvider.NAME, "root");

    identityStorage.saveIdentity(demo);
    identityStorage.saveIdentity(tom);
    identityStorage.saveIdentity(raul);
    identityStorage.saveIdentity(ghost);
    identityStorage.saveIdentity(dragon);
    identityStorage.saveIdentity(mary);
    identityStorage.saveIdentity(john);
    identityStorage.saveIdentity(root);

    tearDownUserList = new ArrayList<Identity>();
    tearDownUserList.add(demo);
    tearDownUserList.add(tom);
    tearDownUserList.add(raul);
    tearDownUserList.add(ghost);
    tearDownUserList.add(dragon);
    tearDownUserList.add(mary);
    tearDownUserList.add(john);
    tearDownUserList.add(root);
  }

  @Override
  public void tearDown() throws Exception {
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
  
  public void testInitGroupPrefers() throws Exception {
    assertNotNull(prefers);
    assertFalse(prefers.isOnRestricted());
    assertTrue(prefers.getGroups().size() > 0);
  }
  
}
