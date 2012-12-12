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
package org.exoplatform.social.common.utils;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

/**
 * Created by The eXo Platform SAS
 * Author : thanh_vucong
 *          thanh_vucong@exoplatform.com
 * Dec 7, 2012  
 */
public class GroupTree {

  private boolean allowUp = false;
  
  private List<GroupNode> sibilings = new LinkedList<GroupNode>();
  public GroupTree(List<GroupNode> sibilings) {
    sibilings.addAll(sibilings);
  }
  
  private GroupTree() {
    
  }
  
  public static GroupTree createInstance() {
    return new GroupTree();
  }
  
  public GroupTree setSibilings(List<GroupNode> sibilings) {
    this.sibilings = sibilings;
    return this;
  }
  
  public GroupTree addSibilings(GroupNode ... nodes) {
    this.sibilings.addAll(Arrays.asList(nodes));
    return this;
  }
  
  public GroupTree addSibilings(List<GroupNode> sibilings) {
    this.sibilings.addAll(sibilings);
    return this;
  }
  
  public List<GroupNode> getSibilings() {
    return sibilings;
  }
  
  public int size() {
    return sibilings.size();
  }
  
  public String toValue() {
    boolean first = false;
    StringBuilder sb = new StringBuilder();
    for(GroupNode node : sibilings) {
      if (first == false) {
        first = true;
      } else {
        sb.append(",");
      }
      
      sb.append(node.getId());
    }
    
    return sb.toString();
  }
  
  public boolean isAllowUp() {
    boolean result = false;
    for(GroupNode node : sibilings) {
      if (node.hasParent()) {
        result = true;
        break;
      }
    }
    //
    this.allowUp = result;
    return allowUp;
  }
  
  public void clear() {
    sibilings = new LinkedList<GroupNode>();
  }
  
  public GroupNode getNode(String key) {
    if (key == null) {
      throw new NullPointerException("key can not be NULL.");
    }
    
    //
    GroupNode result = null;
    for(GroupNode node : sibilings) {
      if (key.equals(node.getId())) {
        result = node;
      }
        
    }
    return result;
  }
  
  public boolean hasNode(String key) {
    GroupNode node = getNode(key);
    return node != null;
  }
  
  public GroupTree remove(String key) {
    //
    GroupNode removeNode = getNode(key);
    sibilings.remove(removeNode);
    return this;
  }
  
  @Override
  public String toString() {
    boolean first = false;
    StringBuilder sb = new StringBuilder();
    for(GroupNode node : sibilings) {
      if (first == false) {
        first = true;
      } else {
        sb.append(", ");
      }
      
      sb.append(node.toString());
    }
    
    return sb.toString();
  }
  
}
