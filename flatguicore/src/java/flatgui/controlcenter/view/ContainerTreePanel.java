/*
 * Copyright (c) 1998-2014 InfoReach, Inc. All Rights Reserved.
 *
 * This software is the confidential and proprietary information of
 * InfoReach ("Confidential Information").  You shall not
 * disclose such Confidential Information and shall use it only in
 * accordance with the terms of the license agreement you entered into
 * with InfoReach.
 *
 * CopyrightVersion 2.0
 *
 */
package flatgui.controlcenter.view;

import flatgui.controlcenter.IFGStateProvider;
import flatgui.controlcenter.containertree.ContainerPropertyMapTreeNode;

import javax.swing.*;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Collections;
import java.util.Map;

/**
 * @author Denis Lebedev
 */
public class ContainerTreePanel implements IControlCenterPanel
{
    static final String NAME = "Container";

    private JButton refreshButton_;
    private JTree tree_;

    private IFGStateProvider stateProvider_;

    ContainerTreePanel(IFGStateProvider stateProvider)
    {
        stateProvider_ = stateProvider;
        prepare();
    }

    @Override
    public String getName()
    {
        return NAME;
    }

    @Override
    public JButton[] getToolButtons()
    {
        return new JButton[]{refreshButton_};
    }

    @Override
    public JComponent getContentComponent()
    {
        return new JScrollPane(tree_);
    }

    void addTreeSelectionListener(TreeSelectionListener tsl)
    {
        tree_.addTreeSelectionListener(tsl);
    }

    Map<String, Object> getPropertiesFromSelectedNode()
    {
        TreePath selectionPath = tree_.getSelectionPath();
        if (selectionPath != null)
        {
            return ((ContainerPropertyMapTreeNode)selectionPath.getLastPathComponent()).getNodeProperties();
        }
        else
        {
            return Collections.EMPTY_MAP;
        }
    }

    private void prepare()
    {
        refreshButton_ = createRefreshButton();

        tree_ = new JTree();
        tree_.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
    }

    private JButton createRefreshButton()
    {
        JButton button = new JButton("Refresh");

        button.addActionListener(new ActionListener()
        {
            @Override
            public void actionPerformed(ActionEvent e)
            {
                refresh();
            }
        });

        return button;
    }

    private void refresh()
    {
        Object containerObj = stateProvider_.getContainer();
        Map<Object, Object> container = (Map<Object, Object>)containerObj;

        ContainerPropertyMapTreeNode root = new ContainerPropertyMapTreeNode(null, container);
        root.initialize();

        DefaultTreeModel treeModel = new DefaultTreeModel(root);
        TreePath selection = tree_.getSelectionPath();
        tree_.setModel(treeModel);
        if (selection != null)
        {
            tree_.setSelectionPath(selection);
        }
    }
}
