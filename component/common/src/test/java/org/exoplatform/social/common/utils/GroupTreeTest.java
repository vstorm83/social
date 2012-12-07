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

import junit.framework.TestCase;

/**
 * Created by The eXo Platform SAS
 * Author : thanh_vucong
 *          thanh_vucong@exoplatform.com
 * Dec 7, 2012  
 */
public class GroupTreeTest extends TestCase {

  @Override
  protected void setUp() throws Exception {
    super.setUp();
  }
  
  @Override
  protected void tearDown() throws Exception {
    super.tearDown();
  }
  
  private GroupTree makeTestData() {
    GroupTree tree = GroupTree.createInstance();
    GroupNode node = GroupNode.createInstance("id1", "labe1");
    GroupNode child1 = GroupNode.createInstance("id11", "Child-1");
    GroupNode child2 = GroupNode.createInstance("id12", "Child-2");
    
    node.addChildren(child1, child2);
    tree.addSibilings(node);
    return tree;
  }
  
  public void testInitTree() throws Exception {
    GroupTree tree = makeTestData();
    assertNotNull(tree);
    assertEquals(1, tree.size());
    GroupNode node = tree.getNode("id1");
    assertNotNull(node);
    
    assertFalse(tree.isAllowUp());
    //clear
    tree.clear();
    assertEquals(0, tree.size());
    
  }
  
  public void testParentNode() throws Exception {
    GroupTree tree = makeTestData();
    assertNotNull(tree);
    assertEquals(1, tree.size());
    GroupNode parent = GroupNode.createInstance("rootid", "Root-Node");
    GroupNode node = tree.getNode("id1");
    assertNotNull(node);
    
    node.setParent(parent);
    
    assertTrue(tree.isAllowUp());
  }
  
  public void testValues() throws Exception {
    GroupTree tree = makeTestData();
    assertNotNull(tree);
    assertEquals("id1", tree.toValue());
    
    tree.addSibilings(GroupNode.createInstance("id2", "Label2"));
    assertEquals("id1,id2", tree.toValue());
  }
  
  public void testHasNode() throws Exception {
    GroupTree tree = makeTestData();
    assertNotNull(tree);
    
    assertTrue(tree.hasNode("id1"));
  }
  
  public void testRemove() throws Exception {
    GroupTree tree = makeTestData();
    assertNotNull(tree);
    assertEquals(1, tree.size());
    GroupNode node = tree.getNode("id1");
    assertNotNull(node);
    
    assertFalse(tree.isAllowUp());
    //remove
    tree.remove("id1");
    assertEquals(0, tree.size());
    
  }
  
  public void testToString() throws Exception {
    GroupTree tree = makeTestData();
    assertNotNull(tree);
    assertEquals("{id = id1, label = labe1, children = [{id = id11, label = Child-1} {id = id12, label = Child-2} ] } ", tree.toString());
    
  }
}
