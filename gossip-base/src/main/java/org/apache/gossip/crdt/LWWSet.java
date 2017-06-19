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

import org.apache.gossip.manager.Clock;
import org.apache.gossip.manager.SystemClock;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class LWWSet<ElementType> implements CrdtSet<ElementType, Set<ElementType>, LWWSet<ElementType>> {
  static private Clock clock = new SystemClock();

  private final Map<ElementType, Timestamps> struct;

  static class Timestamps {
    private final long latestAdd;
    private final long latestRemove;

    Timestamps(){
      latestAdd = 0;
      latestRemove = 0;
    }

    Timestamps(long add, long remove){
      latestAdd = add;
      latestRemove = remove;
    }

    long getLatestAdd() {
      return latestAdd;
    }

    long getLatestRemove() {
      return latestRemove;
    }

    // consider element present when addTime >= removeTime, so we prefer add to remove
    boolean isPresent(){
      return latestAdd >= latestRemove;
    }

    Timestamps updateAdd(){
      return new Timestamps(clock.nanoTime(), latestRemove);
    }

    Timestamps updateRemove(){
      return new Timestamps(latestAdd, clock.nanoTime());
    }

    Timestamps merge(Timestamps other){
      if (other == null){
        return this;
      }
      return new Timestamps(Math.max(latestAdd, other.latestAdd), Math.max(latestRemove, other.latestRemove));
    }
  }


  public LWWSet(){
    struct = new HashMap<>();
  }

  @SafeVarargs
  public LWWSet(ElementType... elements){
    this(new HashSet<>(Arrays.asList(elements)));
  }

  public LWWSet(Set<ElementType> set){
    struct = new HashMap<>();
    for (ElementType e : set){
      struct.put(e, new Timestamps().updateAdd());
    }
  }

  public LWWSet(LWWSet<ElementType> first, LWWSet<ElementType> second){
    Function<ElementType, Timestamps> timestampsFor = p -> {
      Timestamps firstTs = first.struct.get(p);
      Timestamps secondTs = second.struct.get(p);
      if (firstTs == null){
        return secondTs;
      }
      return firstTs.merge(secondTs);
    };
    struct = Stream.concat(first.struct.keySet().stream(), second.struct.keySet().stream())
        .distinct().collect(Collectors.toMap(p -> p, timestampsFor));
  }

  public LWWSet<ElementType> add(ElementType e){
    return this.merge(new LWWSet<>(e));
  }

  // for serialization
  LWWSet(Map<ElementType, Timestamps> struct){
    this.struct = struct;
  }

  Map<ElementType, Timestamps> getStruct() {
    return struct;
  }


  public LWWSet<ElementType> remove(ElementType e){
    Timestamps eTimestamps = struct.get(e);
    if (eTimestamps == null || !eTimestamps.isPresent()){
      return this;
    }
    Map<ElementType, Timestamps> changeMap = new HashMap<>();
    changeMap.put(e, eTimestamps.updateRemove());
    return this.merge(new LWWSet<>(changeMap));
  }

  @Override
  public LWWSet<ElementType> merge(LWWSet<ElementType> other){
    return new LWWSet<>(this, other);
  }

  @Override
  public Set<ElementType> value(){
    return struct.entrySet().stream()
        .filter(entry -> entry.getValue().isPresent())
        .map(Map.Entry::getKey)
        .collect(Collectors.toSet());
  }

  @Override
  public LWWSet<ElementType> optimize(){
    return this;
  }

  @Override
  public boolean equals(Object obj){
    return this == obj || (obj != null && getClass() == obj.getClass() && value().equals(((LWWSet) obj).value()));
  }
}