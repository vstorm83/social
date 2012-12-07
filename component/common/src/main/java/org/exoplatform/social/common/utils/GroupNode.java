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
public class GroupNode {

  private List<GroupNode> chirldren;
  private String id;
  private String label;
  private GroupNode parent;
  
  private GroupNode(String id, String label) {
    chirldren = new LinkedList<GroupNode>();
    this.id = id;
    this.label = label;
    this.parent = null;
  }
  
  private GroupNode(GroupNode parent, String id, String label) {
    chirldren = new LinkedList<GroupNode>();
    this.id = id;
    this.label = label;
    this.parent = parent;
  }
  
  public static GroupNode createInstance(String id, String label) {
    return new GroupNode(id, label);
  }
  
  public static GroupNode createInstance(GroupNode parent, String id, String label) {
    return new GroupNode(parent, id, label);
  }
  
  public String getId() {
    return id;
  }
  
  public String getLabel() {
    return label;
  }
  
  public boolean hasParent() {
    return this.parent != null;
  }
  
  public void setParent(GroupNode parent) {
    this.parent = parent;
  }
  
  public GroupNode getParent() {
    return parent;
  }

  public List<GroupNode> getChirldren() {
    return chirldren;
  }

  public GroupNode addChildren(GroupNode ... nodes) {
    this.chirldren.addAll(Arrays.asList(nodes));
    return this;
  }
  
  public GroupNode addChildren(List<GroupNode> sibilings) {
    this.chirldren.addAll(sibilings);
    return this;
  }

  public int size() {
    return this.chirldren.size();
  }
  
  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    
    if (!(obj instanceof GroupNode)) {
      return false;
    }

    GroupNode node = (GroupNode) obj;

    if (id != null ? !id.equals(node.id) : node.id != null) {
      return false;
    }
    
    if (label != null ? !label.equals(node.label) : node.label != null) {
      return false;
    }
    
    return true;
  }
  
  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    boolean hasChild = size() > 0;
    
    sb.append(String.format("{id = %s, label = %s", id, label));
    if (hasChild) {
      sb.append(", children = [");
      for(GroupNode node : getChirldren()) {
        sb.append(node.toString());
      }
      sb.append("] ");
    }
    
    sb.append("} ");
    return sb.toString();
  }
  
}
