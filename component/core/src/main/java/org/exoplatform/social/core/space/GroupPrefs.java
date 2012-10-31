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

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.organization.Group;
import org.exoplatform.services.organization.OrganizationService;

/**
 * Created by The eXo Platform SAS
 * Author : eXoPlatform
 *          exo@exoplatform.com
 * Oct 24, 2012  
 */
public class GroupPrefs {
  private static final Log LOG = ExoLogger.getLogger(GroupPrefs.class);
  private static Map<String, Map<String, String>> groups = new LinkedHashMap<String, Map<String, String>>();
  private static Map<String, String> restrictedGroups = new LinkedHashMap<String, String>();
  private static Map<String, String> platformGroup = new HashMap<String, String>();
  
  private static boolean isOnRestricted;
  
  private static String PLATFORM_GROUP = "/platform";
  
  public static Map<String, Map<String, String>> getGroups() {
    return groups;  
  }
  
  public static Map<String, String> getRestrictedGroups() {
    return restrictedGroups;  
  }
  
  public static void addRestrictedGroups(String groupId) {
    
    restrictedGroups.put(groupId, platformGroup.get(groupId));  
    
    // remove added item to source list
//    groups.get("Platform").remove(groupId);
  }
  
  public static void removeRestrictedGroups(String groupId) {
    restrictedGroups.remove(groupId);
    
    // re-add removed item to source list
//    groups.get("Platform").put(groupId, platformGroup.get(groupId));
  }
  
  public static boolean isOnRestricted() {
    return isOnRestricted;
  }

  public static void setOnRestricted(boolean isOnRestricted) {
    GroupPrefs.isOnRestricted = isOnRestricted;
  }


  static {
    OrganizationService orgSrv = SpaceUtils.getOrganizationService();
    Collection<Object> allGroups = null;
    try {
      allGroups = orgSrv.getGroupHandler().getAllGroups();
    } catch (Exception e) {
      // 
      LOG.warn("Cannot get all groups.");
    }
    
    platformGroup.clear();
    
    for (Object group : allGroups) {
      Group grp = (Group) group;
      
      // Platform
      if ( grp.getId().contains(PLATFORM_GROUP) && !grp.getId().equals(PLATFORM_GROUP)) {
        platformGroup.put(grp.getId(), grp.getGroupName() + ":" + grp.getLabel());
      }
      
    }
    
    groups.put("Platform", platformGroup);
  }
}
