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

/**
 * A Manager for all interactions between the application and the nodes in a
 * Zookeeper instance
 * */
public interface ZooInspectorNodeManager extends ZooInspectorReadOnlyManager {
    /**
     * @param nodePath
     *            - the path to the node on which to set the data
     * @param data
     *            - the data to set on the this node
     * @return true if the data for the node was successfully updated
     */
    boolean setData(String nodePath, String data);
}
