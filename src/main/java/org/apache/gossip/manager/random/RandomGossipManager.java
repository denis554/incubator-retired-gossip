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
package org.apache.gossip.manager.random;

import org.apache.gossip.GossipMember;
import org.apache.gossip.GossipSettings;
import org.apache.gossip.event.GossipListener;
import org.apache.gossip.manager.GossipManager;
import org.apache.gossip.manager.impl.OnlyProcessReceivedPassiveGossipThread;

import java.net.URI;
import java.util.List;

import java.util.ArrayList;

public class RandomGossipManager extends GossipManager {

  public static ManagerBuilder newBuilder() {
    return new ManagerBuilder();
  }

  public static final class ManagerBuilder {
    private String cluster;
    private URI uri;
    private String id;
    private GossipSettings settings;
    private List<GossipMember> gossipMembers;
    private GossipListener listener;

    private ManagerBuilder() {}

    private void checkArgument(boolean check, String msg) {
      if (!check) {
        throw new IllegalArgumentException(msg);
      }
    }

    public ManagerBuilder cluster(String cluster) {
      this.cluster = cluster;
      return this;
    }

    public ManagerBuilder withId(String id) {
      this.id = id;
      return this;
    }

    public ManagerBuilder settings(GossipSettings settings) {
      this.settings = settings;
      return this;
    }

    public ManagerBuilder gossipMembers(List<GossipMember> members) {
      this.gossipMembers = members;
      return this;
    }

    public ManagerBuilder listener(GossipListener listener) {
      this.listener = listener;
      return this;
    }

    public ManagerBuilder uri(URI uri){
      this.uri = uri;
      return this;
    }
    
    public RandomGossipManager build() {
      checkArgument(id != null, "You must specify an id");
      checkArgument(cluster != null, "You must specify a cluster name");
      checkArgument(settings != null, "You must specify gossip settings");
      checkArgument(uri != null, "You must specify a uri");
      if (this.gossipMembers == null) {
        this.gossipMembers = new ArrayList<>();
      }
      return new RandomGossipManager(cluster, uri, id, settings, gossipMembers, listener);
    }
  }

  private RandomGossipManager(String cluster, URI uri, String id, GossipSettings settings, 
          List<GossipMember> gossipMembers, GossipListener listener) {
    super(cluster, uri, id, settings, gossipMembers, listener);
  }
}
