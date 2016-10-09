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
package org.apache.gossip.manager;

import java.io.IOException;
import java.net.DatagramSocket;
import java.util.List;

import java.util.Map.Entry;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.apache.gossip.LocalGossipMember;
import org.apache.gossip.model.ActiveGossipOk;
import org.apache.gossip.model.GossipDataMessage;
import org.apache.gossip.model.GossipMember;
import org.apache.gossip.model.Response;
import org.apache.gossip.model.SharedGossipDataMessage;
import org.apache.gossip.udp.UdpActiveGossipMessage;
import org.apache.gossip.udp.UdpGossipDataMessage;
import org.apache.gossip.udp.UdpSharedGossipDataMessage;
import org.apache.log4j.Logger;
import org.codehaus.jackson.map.ObjectMapper;

/**
 * [The active thread: periodically send gossip request.] The class handles gossiping the membership
 * list. This information is important to maintaining a common state among all the nodes, and is
 * important for detecting failures.
 */
public class ActiveGossipThread {

  private static final Logger LOGGER = Logger.getLogger(ActiveGossipThread.class);
  
  private final GossipManager gossipManager;
  private final Random random;
  private final GossipCore gossipCore;
  private ScheduledExecutorService scheduledExecutorService;
  private ObjectMapper MAPPER = new ObjectMapper();

  public ActiveGossipThread(GossipManager gossipManager, GossipCore gossipCore) {
    this.gossipManager = gossipManager;
    random = new Random();
    this.gossipCore = gossipCore;
    this.scheduledExecutorService = Executors.newScheduledThreadPool(2);
  }
 
  public void init() {
    scheduledExecutorService.scheduleAtFixedRate(
            () -> sendMembershipList(gossipManager.getMyself(), gossipManager.getLiveMembers()), 0,
            gossipManager.getSettings().getGossipInterval(), TimeUnit.MILLISECONDS);
    scheduledExecutorService.scheduleAtFixedRate(
            () -> sendMembershipList(gossipManager.getMyself(), gossipManager.getDeadMembers()), 0,
            gossipManager.getSettings().getGossipInterval(), TimeUnit.MILLISECONDS);
    scheduledExecutorService.scheduleAtFixedRate(
            () -> sendPerNodeData(gossipManager.getMyself(), gossipManager.getLiveMembers()), 0,
            gossipManager.getSettings().getGossipInterval(), TimeUnit.MILLISECONDS);
    scheduledExecutorService.scheduleAtFixedRate(
            () -> sendSharedData(gossipManager.getMyself(), gossipManager.getLiveMembers()), 0,
            gossipManager.getSettings().getGossipInterval(), TimeUnit.MILLISECONDS);
  }
  
  public void shutdown() {
    scheduledExecutorService.shutdown();
    try {
      scheduledExecutorService.awaitTermination(5, TimeUnit.SECONDS);
    } catch (InterruptedException e) {
      LOGGER.debug("Issue during shurdown" + e);
    }
  }

  public void sendSharedData(LocalGossipMember me, List<LocalGossipMember> memberList){
    LocalGossipMember member = selectPartner(memberList);
    if (member == null) {
      LOGGER.debug("Send sendMembershipList() is called without action");
      return;
    }
    try (DatagramSocket socket = new DatagramSocket()) {
      socket.setSoTimeout(gossipManager.getSettings().getGossipInterval());
      for (Entry<String, SharedGossipDataMessage> innerEntry : this.gossipCore.getSharedData().entrySet()){
          UdpSharedGossipDataMessage message = new UdpSharedGossipDataMessage();
          message.setUuid(UUID.randomUUID().toString());
          message.setUriFrom(me.getId());
          message.setExpireAt(innerEntry.getValue().getExpireAt());
          message.setKey(innerEntry.getValue().getKey());
          message.setNodeId(innerEntry.getValue().getNodeId());
          message.setTimestamp(innerEntry.getValue().getTimestamp());
          message.setPayload(innerEntry.getValue().getPayload());
          message.setTimestamp(innerEntry.getValue().getTimestamp());
          byte[] json_bytes = MAPPER.writeValueAsString(message).getBytes();
          int packet_length = json_bytes.length;
          if (packet_length < GossipManager.MAX_PACKET_SIZE) {
            gossipCore.sendOneWay(message, member.getUri());
          } else {
            LOGGER.error("The length of the to be send message is too large ("
                    + packet_length + " > " + GossipManager.MAX_PACKET_SIZE + ").");
          }
      }
    } catch (IOException e1) {
      LOGGER.warn(e1);
    }
  }
  
  public void sendPerNodeData(LocalGossipMember me, List<LocalGossipMember> memberList){
    LocalGossipMember member = selectPartner(memberList);
    if (member == null) {
      LOGGER.debug("Send sendMembershipList() is called without action");
      return;
    }
    try (DatagramSocket socket = new DatagramSocket()) {
      socket.setSoTimeout(gossipManager.getSettings().getGossipInterval());
      for (Entry<String, ConcurrentHashMap<String, GossipDataMessage>> entry : gossipCore.getPerNodeData().entrySet()){
        for (Entry<String, GossipDataMessage> innerEntry : entry.getValue().entrySet()){
          UdpGossipDataMessage message = new UdpGossipDataMessage();
          message.setUuid(UUID.randomUUID().toString());
          message.setUriFrom(me.getId());
          message.setExpireAt(innerEntry.getValue().getExpireAt());
          message.setKey(innerEntry.getValue().getKey());
          message.setNodeId(innerEntry.getValue().getNodeId());
          message.setTimestamp(innerEntry.getValue().getTimestamp());
          message.setPayload(innerEntry.getValue().getPayload());
          message.setTimestamp(innerEntry.getValue().getTimestamp());
          byte[] json_bytes = MAPPER.writeValueAsString(message).getBytes();
          int packet_length = json_bytes.length;
          if (packet_length < GossipManager.MAX_PACKET_SIZE) {
            gossipCore.sendOneWay(message, member.getUri());
          } else {
            LOGGER.error("The length of the to be send message is too large ("
                    + packet_length + " > " + GossipManager.MAX_PACKET_SIZE + ").");
          }
        }
      }
    } catch (IOException e1) {
      LOGGER.warn(e1);
    }
  }
  
  /**
   * Performs the sending of the membership list, after we have incremented our own heartbeat.
   */
  protected void sendMembershipList(LocalGossipMember me, List<LocalGossipMember> memberList) {  
    me.setHeartbeat(System.currentTimeMillis());
    LocalGossipMember member = selectPartner(memberList);
    if (member == null) {
      LOGGER.debug("Send sendMembershipList() is called without action");
      return;
    } else {
      LOGGER.debug("Send sendMembershipList() is called to " + member.toString());
    }
    
    try (DatagramSocket socket = new DatagramSocket()) {
      socket.setSoTimeout(gossipManager.getSettings().getGossipInterval());
      UdpActiveGossipMessage message = new UdpActiveGossipMessage();
      message.setUriFrom(gossipManager.getMyself().getUri().toASCIIString());
      message.setUuid(UUID.randomUUID().toString());
      message.getMembers().add(convert(me));
      for (LocalGossipMember other : memberList) {
        message.getMembers().add(convert(other));
      }
      byte[] json_bytes = MAPPER.writeValueAsString(message).getBytes();
      int packet_length = json_bytes.length;
      if (packet_length < GossipManager.MAX_PACKET_SIZE) {
        Response r = gossipCore.send(message, member.getUri());
        if (r instanceof ActiveGossipOk){
          //maybe count metrics here
        } else {
          LOGGER.warn("Message "+ message + " generated response "+ r);
        }
      } else {
        LOGGER.error("The length of the to be send message is too large ("
                + packet_length + " > " + GossipManager.MAX_PACKET_SIZE + ").");
      }
    } catch (IOException e1) {
      LOGGER.warn(e1);
    }
  }
  
  /**
   * 
   * @param memberList
   *          The list of members which are stored in the local list of members.
   * @return The chosen LocalGossipMember to gossip with.
   */
  protected LocalGossipMember selectPartner(List<LocalGossipMember> memberList) {
    LocalGossipMember member = null;
    if (memberList.size() > 0) {
      int randomNeighborIndex = random.nextInt(memberList.size());
      member = memberList.get(randomNeighborIndex);
    } else {
      LOGGER.debug("I am alone in this world.");
    }
    return member;
  }
  
  private GossipMember convert(LocalGossipMember member){
    GossipMember gm = new GossipMember();
    gm.setCluster(member.getClusterName());
    gm.setHeartbeat(member.getHeartbeat());
    gm.setUri(member.getUri().toASCIIString());
    gm.setId(member.getId());
    return gm;
  }
}
