package org.apache.zookeeper.inspector.gui.actions;

import org.apache.zookeeper.inspector.gui.ZooInspectorTreeViewer;
import org.apache.zookeeper.inspector.manager.ZooInspectorManager;

import javax.swing.AbstractAction;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingWorker;
import java.awt.event.ActionEvent;
import java.util.List;

public class DeleteNodeAction extends AbstractAction {

    private final JPanel parentPanel;
    private final ZooInspectorTreeViewer treeViewer;
    private final ZooInspectorManager zooInspectorManager;

    public DeleteNodeAction(JPanel parentPanel,
                            ZooInspectorTreeViewer treeViewer,
                            ZooInspectorManager zooInspectorManager) {
        this.parentPanel = parentPanel;
        this.treeViewer = treeViewer;
        this.zooInspectorManager = zooInspectorManager;
       // putValue(MNEMONIC_KEY, KeyEvent.VK_D);
    }


    public void actionPerformed(ActionEvent e) {
        final List<String> selectedNodes = treeViewer
                .getSelectedNodes();
        if (selectedNodes.size() == 0) {
            JOptionPane.showMessageDialog(parentPanel,
                    "Please select at least 1 node to be deleted");
        } else {
            int answer = JOptionPane.showConfirmDialog(
                    parentPanel,
                    "Are you sure you want to delete the selected nodes?"
                            + "(This action cannot be reverted)",
                    "Confirm Delete", JOptionPane.YES_NO_OPTION,
                    JOptionPane.WARNING_MESSAGE
            );
            if (answer == JOptionPane.YES_OPTION) {
                SwingWorker<Boolean, Void> worker = new SwingWorker<Boolean, Void>() {

                    @Override
                    protected Boolean doInBackground() {
                        for (String nodePath : selectedNodes) {
                            zooInspectorManager.deleteNode(nodePath);
                        }
                        return true;
                    }

                    @Override
                    protected void done() {
                        treeViewer.refreshView();
                    }
                };

                worker.execute();
            }
        }
    }
}
