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

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JTree;
import javax.swing.SwingWorker;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;

import org.apache.zookeeper.inspector.gui.actions.AddNodeAction;
import org.apache.zookeeper.inspector.gui.actions.DeleteNodeAction;
import org.apache.zookeeper.inspector.manager.NodeListener;
import org.apache.zookeeper.inspector.manager.ZooInspectorManager;

import com.nitido.utils.toaster.Toaster;

import static javax.swing.KeyStroke.getKeyStroke;

/**
 * A {@link JPanel} for showing the tree view of all the nodes in the zookeeper
 * instance
 */
public class ZooInspectorTreeViewer extends JPanel implements NodeListener {
    private final ZooInspectorManager zooInspectorManager;
    private final JTree tree;
    private final Toaster toasterManager;
    private final ImageIcon toasterIcon;

    /**
     * @param zooInspectorManager - the {@link ZooInspectorManager} for the application
     * @param listener - the {@link TreeSelectionListener} to listen for changes in the selected node on the node tree
     */
    public ZooInspectorTreeViewer(final ZooInspectorManager zooInspectorManager, TreeSelectionListener listener, IconResource iconResource) {

        this.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(getKeyStroke(KeyEvent.VK_D, InputEvent.CTRL_MASK), "deleteNode");

        this.getActionMap().put("deleteNode", new DeleteNodeAction(this, this, zooInspectorManager));

        this.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(getKeyStroke(KeyEvent.VK_N, InputEvent.CTRL_MASK), "addNode");

        this.getActionMap().put("addNode", new AddNodeAction(this, this, zooInspectorManager));

        this.zooInspectorManager = zooInspectorManager;
        this.setLayout(new BorderLayout());
        final JPopupMenu popupMenu = new JPopupMenu();

        final JMenuItem addNode = new JMenuItem("Add Node");
        addNode.addActionListener(new AddNodeAction(this, this, zooInspectorManager));

        final JMenuItem deleteNode = new JMenuItem("Delete Node");
        deleteNode.addActionListener(new DeleteNodeAction(this, this, zooInspectorManager));

        final JMenuItem addNotify = new JMenuItem("Add Change Notification");
        this.toasterManager = new Toaster();
        this.toasterManager.setBorderColor(Color.BLACK);
        this.toasterManager.setMessageColor(Color.BLACK);
        this.toasterManager.setToasterColor(Color.WHITE);
        toasterIcon = iconResource.get(IconResource.ICON_INFORMATION,"");
        addNotify.addActionListener(e -> {
            List<String> selectedNodes = getSelectedNodes();
            zooInspectorManager.addWatchers(selectedNodes,ZooInspectorTreeViewer.this);
        });
        final JMenuItem removeNotify = new JMenuItem("Remove Change Notification");
        removeNotify.addActionListener(e -> {
            List<String> selectedNodes = getSelectedNodes();
            zooInspectorManager.removeWatchers(selectedNodes);
        });

        tree = new JTree(new DefaultMutableTreeNode());
        tree.setCellRenderer(new ZooInspectorTreeCellRenderer(iconResource));
        tree.setEditable(false);
        tree.getSelectionModel().addTreeSelectionListener(listener);
        tree.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.isPopupTrigger() || e.getButton() == MouseEvent.BUTTON3) {
                    // TODO only show add if a selected node isn't being
                    // watched, and only show remove if a selected node is being
                    // watched
                    popupMenu.removeAll();
                    popupMenu.add(addNode);
                    popupMenu.add(deleteNode);
                    popupMenu.add(addNotify);
                    popupMenu.add(removeNotify);
                    popupMenu.show(ZooInspectorTreeViewer.this, e.getX(), e.getY());
                }
            }
        });
        this.add(tree, BorderLayout.CENTER);
    }

    /**
     * Refresh the tree view
     */
    public void refreshView() {
        final Set<TreePath> expandedNodes = new LinkedHashSet<>();
        int rowCount = tree.getRowCount();
        for (int i = 0; i < rowCount; i++) {
            TreePath path = tree.getPathForRow(i);
            if (tree.isExpanded(path)) {
                expandedNodes.add(path);
            }
        }
        final TreePath[] selectedNodes = tree.getSelectionPaths();

        SwingWorker<Boolean, Void> worker = new SwingWorker<Boolean, Void>() {
            @Override
            protected Boolean doInBackground() {
                tree.setModel(new DefaultTreeModel(new ZooInspectorTreeNode("/", null)));
                return true;
            }

            @Override
            protected void done() {
                for (TreePath path : expandedNodes) {
                    tree.expandPath(path);
                }
                tree.getSelectionModel().setSelectionPaths(selectedNodes);
            }
        };
        worker.execute();
    }

    /**
     * clear the tree view of all nodes
     */
    public void clearView() {
        tree.setModel(new DefaultTreeModel(new DefaultMutableTreeNode()));
    }

    private static class ZooInspectorTreeCellRenderer extends DefaultTreeCellRenderer {
        public ZooInspectorTreeCellRenderer(IconResource iconResource) {
            setLeafIcon(iconResource.get(IconResource.ICON_TREE_LEAF,""));
            setOpenIcon(iconResource.get(IconResource.ICON_TREE_OPEN,""));
            setClosedIcon(iconResource.get(IconResource.ICON_TREE_CLOSE,""));
        }
    }

    private class ZooInspectorTreeNode implements TreeNode {
        private final String nodePath;
        private final String nodeName;
        private final ZooInspectorTreeNode parent;

        public ZooInspectorTreeNode(String nodePath, ZooInspectorTreeNode parent) {
            this.parent = parent;
            this.nodePath = nodePath;
            int index = nodePath.lastIndexOf("/");
            if (index == -1) {
                throw new IllegalArgumentException("Invalid node path" + nodePath);
            }
            this.nodeName = nodePath.substring(index + 1);
        }

        /*
         * (non-Javadoc)
         * 
         * @see javax.swing.tree.TreeNode#children()
         */
        public Enumeration<TreeNode> children() {
            List<String> children = zooInspectorManager.getChildren(this.nodePath);
            Collections.sort(children);
            List<TreeNode> returnChildren = new ArrayList<>();
            for (String child : children) {
                returnChildren.add(new ZooInspectorTreeNode((this.nodePath
                        .equals("/") ? "" : this.nodePath)
                        + "/" + child, this));
            }
            return Collections.enumeration(returnChildren);
        }

        /*
         * (non-Javadoc)
         * 
         * @see javax.swing.tree.TreeNode#getAllowsChildren()
         */
        public boolean getAllowsChildren() {
            return zooInspectorManager.isAllowsChildren(nodePath);
        }

        /*
         * (non-Javadoc)
         * 
         * @see javax.swing.tree.TreeNode#getChildAt(int)
         */
        public TreeNode getChildAt(int childIndex) {
            String child = zooInspectorManager.getNodeChild(nodePath, childIndex);
            if (child != null) {
                return new ZooInspectorTreeNode((nodePath.equals("/") ? "": this.nodePath) + "/" + child, this);
            }
            return null;
        }

        /*
         * (non-Javadoc)
         * 
         * @see javax.swing.tree.TreeNode#getChildCount()
         */
        public int getChildCount() {
            return zooInspectorManager.getNumChildren(nodePath);
        }

        /*
         * (non-Javadoc)
         * 
         * @see javax.swing.tree.TreeNode#getIndex(javax.swing.tree.TreeNode)
         */
        public int getIndex(TreeNode node) {
            return zooInspectorManager.getNodeIndex(nodePath);
        }

        /*
         * (non-Javadoc)
         * 
         * @see javax.swing.tree.TreeNode#getParent()
         */
        public TreeNode getParent() {
            return parent;
        }

        /*
         * (non-Javadoc)
         * 
         * @see javax.swing.tree.TreeNode#isLeaf()
         */
        public boolean isLeaf() {
            return !zooInspectorManager.hasChildren(nodePath);
        }

        @Override
        public String toString() {
            return this.nodeName;
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + getOuterType().hashCode();
            result = prime * result + ((nodePath == null) ? 0 : nodePath.hashCode());
            result = prime * result + ((parent == null) ? 0 : parent.hashCode());
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (obj == null)
                return false;
            if (getClass() != obj.getClass())
                return false;
            ZooInspectorTreeNode other = (ZooInspectorTreeNode) obj;
            if (!getOuterType().equals(other.getOuterType()))
                return false;
            if (nodePath == null) {
                if (other.nodePath != null)
                    return false;
            } else if (!nodePath.equals(other.nodePath))
                return false;
            if (parent == null) {
                return other.parent == null;
            } else return parent.equals(other.parent);
        }

        private ZooInspectorTreeViewer getOuterType() {
            return ZooInspectorTreeViewer.this;
        }

    }

    /**
     * @return {@link List} of the currently selected nodes
     */
    public List<String> getSelectedNodes() {
        TreePath[] paths = tree.getSelectionPaths();
        List<String> selectedNodes = new ArrayList<>();
        if (paths != null) {
            for (TreePath path : paths) {
                StringBuilder sb = new StringBuilder();
                Object[] pathArray = path.getPath();
                for (Object o : pathArray) {
                    String nodeName = o.toString();
                    if (nodeName.length() > 0) {
                        sb.append("/");
                        sb.append(o);
                    }
                }
                selectedNodes.add(sb.toString());
            }
        }
        return selectedNodes;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.apache.zookeeper.inspector.manager.NodeListener#processEvent(java.lang.String, java.lang.String, java.util.Map)
     */
    public void processEvent(String nodePath, String eventType, Map<String, String> eventInfo) {
        StringBuilder sb = new StringBuilder();
        sb.append("Node: ");
        sb.append(nodePath);
        sb.append("\nEvent: ");
        sb.append(eventType);
        if (eventInfo != null) {
            for (Map.Entry<String, String> entry : eventInfo.entrySet()) {
                sb.append("\n");
                sb.append(entry.getKey());
                sb.append(": ");
                sb.append(entry.getValue());
            }
        }
        this.toasterManager.showToaster(toasterIcon, sb.toString());
    }
}
