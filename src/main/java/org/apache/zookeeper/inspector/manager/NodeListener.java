/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.zookeeper.inspector.manager;

import java.util.Map;

/**
 * A Listener for Events on zookeeper nodes
 */
public interface NodeListener {
    /**
     * @param nodePath
     *            - the path of the node
     * @param eventType
     *            - the event type
     * @param eventInfo
     *            - a {@link Map} containing any other information about this
     *            event
     */
    void processEvent(String nodePath, String eventType, Map<String, String> eventInfo);
}
