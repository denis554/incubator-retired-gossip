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
package org.apache.gossip;

import io.teknek.tunit.TUnit;
import org.apache.gossip.crdt.GrowOnlyCounter;
import org.apache.gossip.manager.GossipManager;
import org.apache.gossip.manager.GossipManagerBuilder;
import org.apache.gossip.model.SharedDataMessage;
import org.junit.Assert;
import org.junit.Test;
import org.junit.platform.runner.JUnitPlatform;
import org.junit.runner.RunWith;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

@RunWith(JUnitPlatform.class)
public class SharedDataEventTest extends AbstractIntegrationBase {
  
  private String receivedKey = "";
  private Object receivingNodeDataNewValue = "";
  private Object receivingNodeDataOldValue = "";
  private String gCounterKey = "gCounter";
  private Semaphore lock = new Semaphore(0);
  
  @Test
  public void sharedDataEventTest()
          throws InterruptedException, UnknownHostException, URISyntaxException {
    GossipSettings settings = new GossipSettings();
    settings.setPersistRingState(false);
    settings.setPersistDataState(false);
    String cluster = UUID.randomUUID().toString();
    int seedNodes = 1;
    List<Member> startupMembers = new ArrayList<>();
    for (int i = 1; i < seedNodes + 1; ++i) {
      URI uri = new URI("udp://" + "127.0.0.1" + ":" + (50000 + i));
      startupMembers.add(new RemoteMember(cluster, uri, i + ""));
    }
    final List<GossipManager> clients = new ArrayList<>();
    final int clusterMembers = 2;
    for (int i = 1; i < clusterMembers + 1; ++i) {
      URI uri = new URI("udp://" + "127.0.0.1" + ":" + (50000 + i));
      GossipManager gossipService = GossipManagerBuilder.newBuilder().cluster(cluster).uri(uri)
              .id(i + "").gossipMembers(startupMembers).gossipSettings(settings).build();
      clients.add(gossipService);
      gossipService.init();
      register(gossipService);
    }
    
    // check whether the members are discovered
    TUnit.assertThat(() -> {
      int total = 0;
      for (int i = 0; i < clusterMembers; ++i) {
        total += clients.get(i).getLiveMembers().size();
      }
      return total;
    }).afterWaitingAtMost(20, TimeUnit.SECONDS).isEqualTo(2);
    
    // Adding new data to Node 1
    clients.get(0).gossipSharedData(sharedNodeData("category", "distributed"));
    
    // Node 2 is interested in data changes for the key "organization" and "category"
    clients.get(1).registerSharedDataSubscriber((key, oldValue, newValue) -> {
      if (!key.equals("organization") && !key.equals("category"))
        return;
      receivedKey = key;
      receivingNodeDataOldValue = oldValue;
      receivingNodeDataNewValue = newValue;
      lock.release();
    });
    
    // Node 2 first time gets shared data
    lock.tryAcquire(10, TimeUnit.SECONDS);
    Assert.assertEquals("category", receivedKey);
    Assert.assertEquals(null, receivingNodeDataOldValue);
    Assert.assertEquals("distributed", receivingNodeDataNewValue);
    
    // Node 1 adds new per node data
    clients.get(0).gossipSharedData(sharedNodeData("organization", "apache"));
    // Node 2 adds new shared data
    lock.tryAcquire(10, TimeUnit.SECONDS);
    Assert.assertEquals("organization", receivedKey);
    Assert.assertEquals(null, receivingNodeDataOldValue);
    Assert.assertEquals("apache", receivingNodeDataNewValue);
    
    // Node 1 updates its value
    clients.get(0).gossipSharedData(sharedNodeData("organization", "apache-gossip"));
    
    // Node 2 updates existing value
    lock.tryAcquire(10, TimeUnit.SECONDS);
    Assert.assertEquals("organization", receivedKey);
    Assert.assertEquals("apache", receivingNodeDataOldValue);
    Assert.assertEquals("apache-gossip", receivingNodeDataNewValue);
    
  }
  
  @Test
  public void CrdtDataChangeEventTest()
          throws InterruptedException, UnknownHostException, URISyntaxException {
    GossipSettings settings = new GossipSettings();
    settings.setPersistRingState(false);
    settings.setPersistDataState(false);
    String cluster = UUID.randomUUID().toString();
    int seedNodes = 1;
    List<Member> startupMembers = new ArrayList<>();
    for (int i = 1; i < seedNodes + 1; ++i) {
      URI uri = new URI("udp://" + "127.0.0.1" + ":" + (50000 + i));
      startupMembers.add(new RemoteMember(cluster, uri, i + ""));
    }
    final List<GossipManager> clients = new ArrayList<>();
    final int clusterMembers = 3;
    for (int i = 1; i < clusterMembers + 1; ++i) {
      URI uri = new URI("udp://" + "127.0.0.1" + ":" + (50000 + i));
      GossipManager gossipService = GossipManagerBuilder.newBuilder().cluster(cluster).uri(uri)
              .id(i + "").gossipMembers(startupMembers).gossipSettings(settings).build();
      clients.add(gossipService);
      gossipService.init();
      register(gossipService);
    }
    
    // check whether the members are discovered
    TUnit.assertThat(() -> {
      int total = 0;
      for (int i = 0; i < clusterMembers; ++i) {
        total += clients.get(i).getLiveMembers().size();
      }
      return total;
    }).afterWaitingAtMost(20, TimeUnit.SECONDS).isEqualTo(2);
    
    clients.get(1).registerSharedDataSubscriber((key, oldValue, newValue) -> {
      receivedKey = key;
      receivingNodeDataOldValue = oldValue;
      receivingNodeDataNewValue = newValue;
      lock.release();
    });
    
    // Add initial gCounter to Node 1
    SharedDataMessage d = new SharedDataMessage();
    d.setKey(gCounterKey);
    d.setPayload(new GrowOnlyCounter(new GrowOnlyCounter.Builder(clients.get(0)).increment(1L)));
    d.setExpireAt(Long.MAX_VALUE);
    d.setTimestamp(System.currentTimeMillis());
    clients.get(0).merge(d);
    
    // Check if initial Crdt received
    lock.tryAcquire(10, TimeUnit.SECONDS);
    Assert.assertEquals("gCounter", receivedKey);
    Assert.assertEquals(null, receivingNodeDataOldValue);
    Assert.assertTrue(receivingNodeDataNewValue instanceof GrowOnlyCounter);
    Assert.assertEquals(1, ((GrowOnlyCounter) receivingNodeDataNewValue).value().longValue());
  
    // Node 3 Updates the gCounter by 4
    GrowOnlyCounter gc = (GrowOnlyCounter) clients.get(2).findCrdt(gCounterKey);
    GrowOnlyCounter gcNew = new GrowOnlyCounter(gc,
            new GrowOnlyCounter.Builder(clients.get(2)).increment(4L));
  
    d = new SharedDataMessage();
    d.setKey(gCounterKey);
    d.setPayload(gcNew);
    d.setExpireAt(Long.MAX_VALUE);
    d.setTimestamp(System.currentTimeMillis());
    clients.get(2).merge(d);
  
    // Check if Node 3's Crdt update is received in Node 2 event handler
    lock.tryAcquire(10, TimeUnit.SECONDS);
    Assert.assertEquals("gCounter", receivedKey);
    Assert.assertTrue(receivingNodeDataOldValue instanceof GrowOnlyCounter);
    Assert.assertEquals(1, ((GrowOnlyCounter) receivingNodeDataOldValue).value().longValue());
    Assert.assertTrue(receivingNodeDataNewValue instanceof GrowOnlyCounter);
    Assert.assertEquals(5, ((GrowOnlyCounter) receivingNodeDataNewValue).value().longValue());
    
  }
  
  private SharedDataMessage sharedNodeData(String key, String value) {
    SharedDataMessage g = new SharedDataMessage();
    g.setExpireAt(Long.MAX_VALUE);
    g.setKey(key);
    g.setPayload(value);
    g.setTimestamp(System.currentTimeMillis());
    return g;
  }
  
}
