package org.exoplatform.social.portlet.spacemanagement;

import juzu.Action;
import juzu.Path;
import juzu.Resource;
import juzu.View;
import juzu.plugin.ajax.Ajax;
import juzu.template.Template;

import javax.inject.Inject;

import org.exoplatform.social.core.space.GroupPrefs;
import org.exoplatform.social.portlet.spacemanagement.templates.main;
import org.exoplatform.social.portlet.spacemanagement.templates.groupSelector;
//import org.exoplatform.social.portlet.spacemanagement.templates.moreGroup;
import org.exoplatform.social.portlet.spacemanagement.templates.item;
import org.exoplatform.social.portlet.spacemanagement.templates.restrictedGroups;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Controller {

  @Inject @Path("main.gtmpl") main main;
  @Inject @Path("groupSelector.gtmpl") groupSelector groupSelector;
  //@Inject @Path("moreGroup.gtmpl") moreGroup moreGroup;
  @Inject @Path("item.gtmpl") item item;
  @Inject @Path("restrictedGroups.gtmpl") restrictedGroups restrictedGroups;
  
  @Inject GroupPrefs groupPrefs;
  
  List<String> moreRestrictedGroups = new ArrayList<String>();

  @View
  public void index() throws Exception {
    main.with()
        .isRestricted(groupPrefs.isOnRestricted())
        .restrictedGroups(groupPrefs.getRestrictedGroups())
        .render();
  }
  
  @Ajax
  @Resource
  public void doAddGroup() throws Exception {
    groupSelector.with()
                 .allGroups(groupPrefs.getGroups())
                 .render();
  }
  
  
  @Ajax
  @Resource
  public void removeGroup(String groupId) throws Exception {
    groupPrefs.removeRestrictedGroups(groupId);
    
    restrictedGroups.with()
                    .restrictedGroups(groupPrefs.getRestrictedGroups())
                    .render();
  }
  
  @Ajax
  @Resource
  public void doSelectGroup(String groupId, String groupName) throws Exception {
    // temp process, return if group is selected
    if (groupPrefs.getRestrictedGroups().hasNode(groupId)) return;
    
    // store selected group
    groupPrefs.addRestrictedGroups(groupId);
    
    //
    item.with()
             .groupName(groupName)
             .render();
  }
  
  
  @Ajax
  @Resource
  public void switchMode() throws Exception {
    // change status
    groupPrefs.setOnRestricted(!groupPrefs.isOnRestricted());
  }
  
}
