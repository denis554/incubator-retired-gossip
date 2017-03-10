/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.gossip.crdt;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.SortedSet;
import java.util.TreeSet;

import org.apache.gossip.GossipSettings;
import org.apache.gossip.manager.GossipManager;
import org.apache.gossip.manager.GossipManagerBuilder;
import org.junit.Assert;
import org.junit.Test;

public class OrSetTest {

  @Test
  public void atest() {
    OrSet<Integer> i = new OrSet<>(new OrSet.Builder<Integer>().add(4).add(5).add(6).remove(5));
    Assert.assertArrayEquals(Arrays.asList(4, 6).toArray(), i.value().toArray());
  }
    
  @Test
  public void mergeTest(){
    OrSet<Integer> i = new OrSet<>(new OrSet.Builder<Integer>().add(4).add(5).add(6).remove(5));
    Assert.assertArrayEquals(Arrays.asList(4, 6).toArray(), i.value().toArray());
    OrSet<Integer> j = new OrSet<>(new OrSet.Builder<Integer>().add(9).add(4).add(5).remove(6));
    OrSet<Integer> h = i.merge(j);
    Assert.assertEquals(new OrSet<Integer>(4,6,9,5), h);
  }
  
  @Test
  public void mergeTest2(){
    OrSet<Integer> i = new OrSet<>(new OrSet.Builder<Integer>().add(5).add(4).remove(4).add(6));
    Assert.assertEquals(new OrSet<Integer>(5,6), i);
    SortedSet<Integer> tree = new TreeSet<>();
    for (Integer in: i.value()){
      tree.add(in);
    }
    TreeSet<Integer> compare = new TreeSet<>();
    compare.add(5);
    compare.add(6);
    Assert.assertEquals(tree, compare);
  }
  
  @Test
  public void mergeTest4() {
    Assert.assertArrayEquals(new Integer[] {},
            new OrSet<Integer>(new OrSet.Builder<Integer>().add(1).remove(1)).toArray());
  }
  
  @Test
  public void mergeTest3(){
    OrSet<Integer> i = new OrSet<>(1);
    OrSet<Integer> j = new OrSet<>(2);
    OrSet<Integer> k = new OrSet<>(i.merge(j),  new OrSet.Builder<Integer>().remove(1));
    Assert.assertArrayEquals(new Integer[] { 2 }, i.merge(j).merge(k).toArray());
    Assert.assertArrayEquals(new Integer[] { 2 }, j.merge(i).merge(k).toArray());
    Assert.assertArrayEquals(new Integer[] { 2 }, k.merge(i).merge(j).toArray());
    Assert.assertArrayEquals(new Integer[] { 2 }, k.merge(j).merge(i).toArray());
    Assert.assertEquals(j , i.merge(j.merge(k)));
  }
  
  @Test
  public void mergeTest9(){
    OrSet<Integer> i = new OrSet<>(19);
    OrSet<Integer> j = i.merge(i);
    Assert.assertEquals(i.value(), j.value());
  }
  
  @Test
  public void serialTest() throws InterruptedException, URISyntaxException, IOException {
    GossipManager gossipService2 = GossipManagerBuilder.newBuilder()
            .cluster("a")
            .uri(new URI("udp://" + "127.0.0.1" + ":" + (29000 + 1)))
            .id("1")
            .gossipSettings(new GossipSettings())
            .build();
    OrSet<Integer> i = new OrSet<Integer>(new OrSet.Builder<Integer>().add(1).remove(1));
    String s = gossipService2.getObjectMapper().writeValueAsString(i);
    @SuppressWarnings("unchecked")
    OrSet<Integer> back = gossipService2.getObjectMapper().readValue(s, OrSet.class);
    Assert.assertEquals(back, i);
  }
  
  @Test
  public void mergeTestSame() {
    OrSet<Integer> i = new OrSet<>(19);
    OrSet<Integer> j = new OrSet<>(19);
    OrSet<Integer> k = i.merge(j);
    Assert.assertEquals(2, k.getElements().get(19).size());
    OrSet<Integer> y = new OrSet<>(k, new OrSet.Builder<Integer>().remove(19));
    Assert.assertEquals(2, y.getTombstones().get(19).size());
    Assert.assertEquals(2, y.getElements().get(19).size());
    Assert.assertEquals(new OrSet<Integer>().value(), y.value());
  }
}