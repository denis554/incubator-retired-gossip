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

import java.net.InetAddress;
import java.net.URI;
import java.net.UnknownHostException;
import java.util.List;

import org.apache.gossip.event.GossipListener;
import org.apache.gossip.manager.GossipManager;
import org.apache.gossip.manager.random.RandomGossipManager;
import org.apache.log4j.Logger;

/**
 * This object represents the service which is responsible for gossiping with other gossip members.
 * 
 * @author joshclemm, harmenw
 */
public class GossipService {

  public static final Logger LOGGER = Logger.getLogger(GossipService.class);

  private GossipManager gossipManager;

  /**
   * Constructor with the default settings.
   * 
   * @throws InterruptedException
   * @throws UnknownHostException
   */
  public GossipService(StartupSettings startupSettings) throws InterruptedException,
          UnknownHostException {
    this(startupSettings.getCluster(), startupSettings.getUri()
            , startupSettings.getId(), startupSettings.getGossipMembers(),
            startupSettings.getGossipSettings(), null);
  }

  /**
   * Setup the client's lists, gossiping parameters, and parse the startup config file.
   * 
   * @throws InterruptedException
   * @throws UnknownHostException
   */
  public GossipService(String cluster, URI uri, String id,
          List<GossipMember> gossipMembers, GossipSettings settings, GossipListener listener)
          throws InterruptedException, UnknownHostException {
<<<<<<< HEAD
    gossipManager = new RandomGossipManager(cluster, uri, id, settings, gossipMembers,
            listener);
=======
    gossipManager = RandomGossipManager.newBuilder()
        .withId(id)
        .cluster(cluster)
        .address(ipAddress)
        .port(port)
        .settings(settings)
        .gossipMembers(gossipMembers)
        .listener(listener)
        .build();
>>>>>>> fe196cd... GOSSIP-4: Use builder to create RandomGossipManager (Jaideep Dhok via EGC)
  }

  public void start() {
    LOGGER.debug("Starting: " + gossipManager.getName() + " - " + get_gossipManager().getMyself().getUri());
    gossipManager.start();
  }

  public void shutdown() {
    gossipManager.shutdown();
  }

  public GossipManager get_gossipManager() {
    return gossipManager;
  }

  public void set_gossipManager(GossipManager _gossipManager) {
    this.gossipManager = _gossipManager;
  }

}
