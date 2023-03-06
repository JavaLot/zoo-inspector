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
package org.apache.zookeeper.inspector.gui.nodeviewer;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.SwingWorker;

import org.apache.zookeeper.inspector.logger.LoggerFactory;
import org.apache.zookeeper.inspector.manager.ZooInspectorNodeManager;

/**
 * A node viewer for displaying the ACLs currently applied to the selected node
 */
public class NodeViewerACL extends ZooInspectorNodeViewer {
    private ZooInspectorNodeManager zooInspectorManager;
    private final JPanel aclDataPanel;
    private String selectedNode;

    public NodeViewerACL() {
        this.setLayout(new BorderLayout());
        this.aclDataPanel = new JPanel();
        this.aclDataPanel.setBackground(Color.WHITE);
        JScrollPane scroller = new JScrollPane(this.aclDataPanel);
        this.add(scroller, BorderLayout.CENTER);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.apache.zookeeper.inspector.gui.nodeviewer.ZooInspectorNodeViewer#getTitle()
     */
    @Override
    public String getTitle() {
        return "Node ACLs";
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.apache.zookeeper.inspector.gui.nodeviewer.ZooInspectorNodeViewer#
     * nodeSelectionChanged(java.util.Set)
     */
    @Override
    public void nodeSelectionChanged(List<String> selectedNodes) {
        this.aclDataPanel.removeAll();
        if (selectedNodes.size() > 0) {
            this.selectedNode = selectedNodes.get(0);
            SwingWorker<List<Map<String, String>>, Void> worker = new SwingWorker<List<Map<String, String>>, Void>() {

                @Override
                protected List<Map<String, String>> doInBackground() {
                    return zooInspectorManager.getACLs(selectedNode);
                }

                @Override
                protected void done() {
                    List<Map<String, String>> acls;
                    try {
                        acls = get();
                    } catch (InterruptedException | ExecutionException e) {
                        acls = new ArrayList<>();
                        LoggerFactory.getLogger().error(
                                "Error retrieving ACL Information for node: "
                                        + selectedNode, e);
                    }
                    aclDataPanel.setLayout(new GridBagLayout());
                    int j = 0;
                    for (Map<String, String> data : acls) {
                        int rowPos = 2 * j + 1;
                        JPanel aclPanel = new JPanel();
                        aclPanel.setBorder(BorderFactory.createLineBorder(Color.BLACK));
                        aclPanel.setBackground(Color.WHITE);
                        aclPanel.setLayout(new GridBagLayout());
                        int i = 0;
                        for (Map.Entry<String, String> entry : data.entrySet()) {
                            int rowPosACL = 2 * i + 1;
                            JLabel label = new JLabel(entry.getKey());
                            JTextField text = new JTextField(entry.getValue());
                            text.setEditable(false);
                            GridBagConstraints c1 = gbcs(1, rowPosACL,0, 0, GridBagConstraints.BOTH);
                            aclPanel.add(label, c1);
                            GridBagConstraints c2 = gbcs(3, rowPosACL,0, 0, GridBagConstraints.BOTH);
                            aclPanel.add(text, c2);
                            i++;
                        }

                        GridBagConstraints c = gbcs(1, rowPos,1, 1, GridBagConstraints.NONE);
                        aclDataPanel.add(aclPanel, c);
                        j++;
                    }
                    NodeViewerACL.this.aclDataPanel.revalidate();
                    NodeViewerACL.this.aclDataPanel.repaint();
                }
            };
            worker.execute();
        }
    }

    private GridBagConstraints gbcs(int gridX, int rowPosACL, int weightX, int weightY, int fill) {
        GridBagConstraints result = new GridBagConstraints();
        result.gridx = gridX;
        result.gridy = rowPosACL;
        result.gridwidth = 1;
        result.gridheight = 1;
        result.weightx = weightX;
        result.weighty = weightY;
        result.anchor = GridBagConstraints.NORTHWEST;
        result.fill = fill;
        result.insets = new Insets(5, 5, 5, 5);
        result.ipadx = 0;
        result.ipady = 0;

        return result;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.apache.zookeeper.inspector.gui.nodeviewer.ZooInspectorNodeViewer#setZooInspectorManager
     * (org.apache.zookeeper.inspector.manager.ZooInspectorNodeManager)
     */
    @Override
    public void setZooInspectorManager(ZooInspectorNodeManager zooInspectorManager) {
        this.zooInspectorManager = zooInspectorManager;
    }
}
