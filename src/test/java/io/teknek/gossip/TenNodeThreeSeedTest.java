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
package io.teknek.gossip;

import io.teknek.tunit.TUnit;

import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;


import org.apache.log4j.Logger;
import org.junit.Test;

import com.google.code.gossip.GossipMember;
import com.google.code.gossip.GossipService;
import com.google.code.gossip.GossipSettings;
import com.google.code.gossip.RemoteGossipMember;
import com.google.code.gossip.event.GossipListener;
import com.google.code.gossip.event.GossipState;

public class TenNodeThreeSeedTest {
  private static final Logger log = Logger.getLogger( TenNodeThreeSeedTest.class );

  @Test
  public void test() throws UnknownHostException, InterruptedException{
    abc();
  }

  @Test
  public void testAgain() throws UnknownHostException, InterruptedException{
    abc();
  }

  public void abc() throws InterruptedException, UnknownHostException{
    GossipSettings settings = new GossipSettings();
    String cluster = UUID.randomUUID().toString();

    log.info( "Adding seed nodes" );
    int seedNodes = 3;
    List<GossipMember> startupMembers = new ArrayList<>();
    for (int i = 1; i < seedNodes+1; ++i) {
      startupMembers.add(new RemoteGossipMember(cluster, "127.0.0.1", 50000 + i, i + ""));
    }

    log.info( "Adding clients" );
    final List<GossipService> clients = new ArrayList<>();
    final int clusterMembers = 5;
    for (int i = 1; i < clusterMembers+1; ++i) {
      GossipService gossipService = new GossipService(cluster, "127.0.0.1", 50000 + i, i + "",
              startupMembers, settings,
              new GossipListener(){
        @Override
        public void gossipEvent(GossipMember member, GossipState state) {
          log.info(member+" "+ state);
        }
      });
      clients.add(gossipService);
      gossipService.start();
      gossipService.get_gossipManager().getMemberList();
    }
    TUnit.assertThat(new Callable<Integer> (){
      public Integer call() throws Exception {
        int total = 0;
        for (int i = 0; i < clusterMembers; ++i) {
          total += clients.get(i).get_gossipManager().getMemberList().size();
        }
        return total;
      }}).afterWaitingAtMost(10, TimeUnit.SECONDS).isEqualTo(20);
    
    for (int i = 0; i < clusterMembers; ++i) {
      clients.get(i).shutdown();
    }
  }
}
