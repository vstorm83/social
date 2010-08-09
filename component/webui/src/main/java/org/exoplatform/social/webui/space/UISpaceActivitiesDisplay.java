/*
 * Copyright (C) 2003-2010 eXo Platform SAS.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Affero General Public License
 * as published by the Free Software Foundation; either version 3
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, see<http://www.gnu.org/licenses/>.
 */
package org.exoplatform.social.webui.space;

import java.util.List;

import org.exoplatform.portal.webui.container.UIContainer;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.social.core.activity.model.Activity;
import org.exoplatform.social.core.identity.model.Identity;
import org.exoplatform.social.core.identity.provider.SpaceIdentityProvider;
import org.exoplatform.social.core.manager.ActivityManager;
import org.exoplatform.social.core.manager.IdentityManager;
import org.exoplatform.social.core.space.model.Space;
import org.exoplatform.social.webui.activity.UIActivitiesContainer;
import org.exoplatform.social.webui.composer.UIComposer.PostContext;
import org.exoplatform.webui.config.annotation.ComponentConfig;

/**
 * UISpaceActivitiesDisplay.java
 * <p>
 * Displays space activities and its member's activities
 *
 * @author <a href="http://hoatle.net">hoatle</a>
 * @since Apr 6, 2010
 * @copyright eXo Platform SAS
 */
@ComponentConfig(
  template = "classpath:groovy/social/webui/space/UISpaceActivitiesDisplay.gtmpl"
)
public class UISpaceActivitiesDisplay extends UIContainer {
  static private final Log LOG = ExoLogger.getLogger(UISpaceActivitiesDisplay.class);

  private Space space;
  private List<Activity> activityList;
  private UIActivitiesContainer uiActivitiesContainer;

  /**
   * Constructor
   *
   * @throws Exception
   */
  public UISpaceActivitiesDisplay() throws Exception {
  }

  /**
   * Sets space to work with
   * @param space
   * @param postContext
   * @throws Exception
   */
  public void setSpace(Space space) throws Exception {
    this.space = space;
    init();
  }

  /**
   * Returns current space to work with
   * @return
   */
  public Space getSpace() {
    return space;
  }

  /**
   * initialize
   * @throws Exception
   */
  private void init() throws Exception {
    if (space == null) {
      LOG.warn("space is null! Can not display spaceActivites");
      return;
    }

    Identity spaceIdentity = getIdentityManager().getOrCreateIdentity(SpaceIdentityProvider.NAME, space.getId());
    activityList = getActivityManager().getActivities(spaceIdentity);

    removeChild(UIActivitiesContainer.class);
    uiActivitiesContainer = addChild(UIActivitiesContainer.class, null, null);
    uiActivitiesContainer.setPostContext(PostContext.SPACE);
    uiActivitiesContainer.setSpace(space);
    uiActivitiesContainer.setActivityList(activityList);
  }

  /**
   * Gets identityManager
   * @return
   */
  private IdentityManager getIdentityManager() {
    return getApplicationComponent(IdentityManager.class);
  }

  /**
   * Gets activityManager
   * @return
   */
  private ActivityManager getActivityManager() {
    return getApplicationComponent(ActivityManager.class);
  }
}
