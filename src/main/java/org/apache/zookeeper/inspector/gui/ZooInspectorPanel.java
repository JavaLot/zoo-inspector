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
package org.apache.zookeeper.inspector.gui;

import org.apache.zookeeper.inspector.gui.actions.AddNodeAction;
import org.apache.zookeeper.inspector.gui.actions.DeleteNodeAction;
import org.apache.zookeeper.inspector.gui.nodeviewer.ZooInspectorNodeViewer;
import org.apache.zookeeper.inspector.logger.LoggerFactory;
import org.apache.zookeeper.inspector.manager.ZooInspectorManager;

import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.SwingWorker;
import java.awt.BorderLayout;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.ExecutionException;

/**
 * The parent {@link JPanel} for the whole application
 */
public class ZooInspectorPanel extends JPanel implements NodeViewersChangeListener {
    private final Toolbar toolbar;
    private final ZooInspectorNodeViewersPanel nodeViewersPanel;
    private final ZooInspectorTreeViewer treeViewer;
    private final ZooInspectorManager zooInspectorManager;

    private final List<NodeViewersChangeListener> listeners = new ArrayList<>();

    {
        listeners.add(this);
    }

    /**
     * @param zooInspectorManager - the {@link ZooInspectorManager} for the application
     */
    public ZooInspectorPanel(final ZooInspectorManager zooInspectorManager, final IconResource iconResource) {
        this.zooInspectorManager = zooInspectorManager;
        toolbar = new Toolbar(iconResource);
        final ArrayList<ZooInspectorNodeViewer> nodeViewers = new ArrayList<>();
        try {
            List<String> defaultNodeViewersClassNames = this.zooInspectorManager
                    .getDefaultNodeViewerConfiguration();
            for (String className : defaultNodeViewersClassNames) {
                nodeViewers.add((ZooInspectorNodeViewer) Class.forName(
                        className).newInstance());
            }
        } catch (Exception ex) {
            LoggerFactory.getLogger().error(
                    "Error loading default node viewers.", ex);
            JOptionPane.showMessageDialog(ZooInspectorPanel.this,
                    "Error loading default node viewers: " + ex.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
        nodeViewersPanel = new ZooInspectorNodeViewersPanel(zooInspectorManager, nodeViewers);
        treeViewer = new ZooInspectorTreeViewer(zooInspectorManager, nodeViewersPanel, iconResource);
        this.setLayout(new BorderLayout());

        toolbar.addActionListener(Toolbar.Button.connect, e -> {
            ZooInspectorConnectionPropertiesDialog zicpd = new ZooInspectorConnectionPropertiesDialog(
                    zooInspectorManager.getLastConnectionProps(),
                    zooInspectorManager.getConnectionPropertiesTemplate(),
                    ZooInspectorPanel.this);
            zicpd.setLocationRelativeTo(this);
            zicpd.setVisible(true);
        });
        toolbar.addActionListener(Toolbar.Button.disconnect, e -> disconnect());
        toolbar.addActionListener(Toolbar.Button.refresh, e -> treeViewer.refreshView());
        toolbar.addActionListener(Toolbar.Button.addNode, new AddNodeAction(this, treeViewer, zooInspectorManager));
        toolbar.addActionListener(Toolbar.Button.deleteNode, new DeleteNodeAction(this, treeViewer, zooInspectorManager));


        toolbar.addActionListener(Toolbar.Button.nodeViewers, e -> {
            ZooInspectorNodeViewersDialog nvd = new ZooInspectorNodeViewersDialog(
                    JOptionPane.getRootFrame(), nodeViewers, listeners,
                    zooInspectorManager, iconResource);
            nvd.setLocationRelativeTo(this);
            nvd.setVisible(true);
        });
        toolbar.addActionListener(Toolbar.Button.about, e -> {
            ZooInspectorAboutDialog zicpd = new ZooInspectorAboutDialog(JOptionPane.getRootFrame(), iconResource);
            zicpd.setLocationRelativeTo(this);
            zicpd.setVisible(true);
        });
        JScrollPane treeScroller = new JScrollPane(treeViewer);
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, treeScroller, nodeViewersPanel);
        splitPane.setResizeWeight(0.25);
        this.add(splitPane, BorderLayout.CENTER);
        this.add(toolbar.getJToolBar(), BorderLayout.NORTH);
    }

    /**
     * @param connectionProps the {@link Properties} for connecting to the zookeeper instance
     */
    public void connect(final Properties connectionProps) {
        SwingWorker<Boolean, Void> worker = new SwingWorker<Boolean, Void>() {

            @Override
            protected Boolean doInBackground() {
                zooInspectorManager.setLastConnectionProps(connectionProps);
                return zooInspectorManager.connect(connectionProps);
            }

            @Override
            protected void done() {
                try {
                    if (get()) {
                        treeViewer.refreshView();
                        toolbar.toggleButtons(true);
                    } else {
                        JOptionPane.showMessageDialog(ZooInspectorPanel.this,
                                "Unable to connect to zookeeper", "Error",
                                JOptionPane.ERROR_MESSAGE);
                    }
                } catch (InterruptedException | ExecutionException e) {
                    LoggerFactory
                            .getLogger()
                            .error("Error occurred while connecting to ZooKeeper server", e);
                }
            }

        };
        worker.execute();
    }

    /**
     *
     */
    public void disconnect() {
        disconnect(false);
    }

    /**
     * @param wait - set this to true if the method should only return once the
     *             application has successfully disconnected
     */
    public void disconnect(boolean wait) {
        SwingWorker<Boolean, Void> worker = new SwingWorker<Boolean, Void>() {

            @Override
            protected Boolean doInBackground() {
                return ZooInspectorPanel.this.zooInspectorManager.disconnect();
            }

            @Override
            protected void done() {
                try {
                    if (get()) {
                        treeViewer.clearView();
                        toolbar.toggleButtons(false);
                    }
                } catch (InterruptedException | ExecutionException e) {
                    LoggerFactory
                            .getLogger()
                            .error("Error occurred while disconnecting from ZooKeeper server", e);
                }
            }

        };
        worker.execute();

        if (wait) {
            try {
                if(worker.get()) {
                    LoggerFactory
                            .getLogger()
                            .info("ZooInspector disconnected from ZooKeeper server");
                }
            } catch (InterruptedException | ExecutionException e) {
                    LoggerFactory
                            .getLogger()
                            .error("Error occurred while disconnecting from ZooKeeper server", e);
            }
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @seeorg.apache.zookeeper.inspector.gui.NodeViewersChangeListener#
     * nodeViewersChanged(java.util.List)
     */
    public void nodeViewersChanged(List<ZooInspectorNodeViewer> newViewers) {
        this.nodeViewersPanel.setNodeViewers(newViewers);
    }

    /**
     * @param connectionProps the {@link Properties} for connecting to the zookeeper instance
     * @throws IOException on error while save
     */
    public void setDefaultConnectionProps(Properties connectionProps) throws IOException {
        this.zooInspectorManager.saveDefaultConnectionFile(connectionProps);
    }
}
