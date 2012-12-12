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
import java.util.List;

import org.exoplatform.commons.settings.api.SettingService;
import org.exoplatform.commons.settings.api.SettingValue;
import org.exoplatform.commons.settings.model.api.Context;
import org.exoplatform.commons.settings.model.api.Scope;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.organization.Group;
import org.exoplatform.services.organization.OrganizationService;
import org.exoplatform.social.common.utils.GroupNode;
import org.exoplatform.social.common.utils.GroupTree;

/**
 * Created by The eXo Platform SAS
 * Author : eXoPlatform
 *          exo@exoplatform.com
 * Oct 24, 2012  
 */
public class GroupPrefs {
  private static String ON_RESTRICTED_KEY = "SOCIAL_SPACE_ADMIN_ON_RESTRICTED_KEY";
  private static String GROUP_RESTRICTED_KEY = "SOCIAL_SPACE_ADMIN_GROUPS_RESTRICTED_KEY";
  
  private SettingService settingService = null;
  private OrganizationService orgSrv = null;
  
  private static final Log LOG = ExoLogger.getLogger(GroupPrefs.class);
  private static GroupTree treeAllGroups = GroupTree.createInstance();
  private static GroupTree treeRestrictedGroups = GroupTree.createInstance();
  
  private boolean isOnRestricted;
  
  public GroupPrefs(SettingService settingService) {
    this.settingService = settingService;
    this.orgSrv = SpaceUtils.getOrganizationService();
    loadSetting();
  }
  
  private void loadSetting() {
    SettingValue<?>  value = this.settingService.get(Context.GLOBAL, Scope.PORTAL, ON_RESTRICTED_KEY);
    
    //isRestricted value
    if (value != null) {
      Boolean boolValue = (Boolean) value.getValue();
      isOnRestricted = boolValue.booleanValue();
    }
    
    //restricted groups
    value = this.settingService.get(Context.GLOBAL, Scope.PORTAL, GROUP_RESTRICTED_KEY);
    
    if (value != null) {
      //
      String[] groupIds = value.getValue().toString().split(",");
      try {
        for(String id : groupIds) {
          Group currentGroup = orgSrv.getGroupHandler().findGroupById(id);
          Group parentGroup = orgSrv.getGroupHandler().findGroupById(currentGroup.getParentId());
          Collection children = orgSrv.getGroupHandler().findGroups(currentGroup);
          
          treeRestrictedGroups.addSibilings(buildGroupNode(parentGroup, currentGroup, children));
        }
      } catch (Exception e) {
        // 
        LOG.warn("Cannot get all groups.");
      }
    }
    
    //all groups
    Collection<?> allGroups = null;
    try {
      allGroups = orgSrv.getGroupHandler().findGroups(null);
      
      for (Object group : allGroups) {
        if (group instanceof Group) {
          Group grp = (Group) group;
          Group parentGroup = grp.getParentId() != null ? orgSrv.getGroupHandler().findGroupById(grp.getParentId()) : null;
          Collection children = orgSrv.getGroupHandler().findGroups(grp);
          treeAllGroups.addSibilings(buildGroupNode(parentGroup, grp, children));
        }
      }
      
    } catch (Exception e) {
      // 
      LOG.warn("Cannot get all groups.");
    }
  }
  
  private GroupNode buildGroupNode(Group parentGroup, Group currentGroup, Collection children) {
    GroupNode currentNode = null;
    if (currentGroup != null) {
      currentNode = GroupNode.createInstance(currentGroup.getId(), currentGroup.getLabel());
      
      //
      if (parentGroup != null)
        currentNode.setParent(GroupNode.createInstance(parentGroup.getId(), parentGroup.getLabel()));
      
      //
      if (children != null) {
        for (Object g : children) {
          if (g instanceof Group) {
            Group grp = (Group) g;
            currentNode.addChildren(GroupNode.createInstance(grp.getId(), grp.getLabel()));
          }
        }
      }
      
    }
    
    return currentNode;
  }
  
  public GroupTree getGroups() {
    return treeAllGroups;  
  }
  
  public static GroupTree getRestrictedGroups() {
    return treeRestrictedGroups;  
  }
  
  public void addRestrictedGroups(String groupId) {
    try {
      Group group = orgSrv.getGroupHandler().findGroupById(groupId);
      treeRestrictedGroups.addSibilings(GroupNode.createInstance(group.getId(), group.getLabel()));
      
      //
      this.settingService.set(Context.GLOBAL, Scope.PORTAL, GROUP_RESTRICTED_KEY, SettingValue.create(treeRestrictedGroups.toValue()));
    } catch (Exception e) {
      // 
      LOG.warn("Cannot get group for '" + groupId + "'");
    }
  }
  
  public void removeRestrictedGroups(String groupId) {
    treeRestrictedGroups.remove(groupId);
  }
  
  public boolean isOnRestricted() {
    return isOnRestricted;
  }

  public void setOnRestricted(boolean isOnRestricted) {
    this.isOnRestricted = isOnRestricted;
    this.settingService.set(Context.GLOBAL, Scope.PORTAL, ON_RESTRICTED_KEY, SettingValue.create(isOnRestricted));
  }
  
  public void downLevel(String groupKey, GroupTree tree) {
    GroupNode node = tree.getNode(groupKey);
    try {
      if (node != null) {
        List<GroupNode> myChildren = node.getChirldren();
        if (node.getChirldren().size() > 0) {
          tree.clear();
        }
        
        for (GroupNode groupNode : myChildren) {
          Group parentGroup = groupNode.getParent() != null ? orgSrv.getGroupHandler().findGroupById(groupNode.getParent().getId()) : null;
          Group group = orgSrv.getGroupHandler().findGroupById(groupNode.getId());
          Collection children = orgSrv.getGroupHandler().findGroups(group);
          tree.addSibilings(buildGroupNode(parentGroup, group, children));
        }
        
      }
    } catch (Exception e) {
      // 
      LOG.warn("Cannot down level of node.");
    }
  }

  
  public void upLevel(GroupTree tree) {
    GroupNode groupNode = tree.getSibilings().size() > 0 ? tree.getSibilings().get(0) : null;
    try {
      if (groupNode != null) {
        GroupNode myParent = groupNode.getParent();
        if (myParent != null) {
          
          //get parent
          Group parentGroup = orgSrv.getGroupHandler().findGroupById(myParent.getId());
          if (parentGroup.getParentId() != null) {
            tree.clear();
            
            //get ancestor
            Group ancestorGroup = orgSrv.getGroupHandler().findGroupById(parentGroup.getParentId());
            Collection<?> children = orgSrv.getGroupHandler().findGroups(ancestorGroup);
            
            //get children of ancestor, then push into tree
            for (Object group : children) {
              if (group instanceof Group) {
                Group grp = (Group) group;
                Group newParentGroup = grp.getParentId() != null ? orgSrv.getGroupHandler().findGroupById(grp.getParentId()) : null;
                Collection newchildren = orgSrv.getGroupHandler().findGroups(grp);
                tree.addSibilings(buildGroupNode(newParentGroup, grp, newchildren));
              }
            }
          }
        }
        
      }
    } catch (Exception e) {
      // 
      LOG.warn("Cannot down level of node.");
    }
  }
  
}
