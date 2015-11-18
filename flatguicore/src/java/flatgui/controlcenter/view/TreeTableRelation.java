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

import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;

/**
 * @author Denis Lebedev
 */
public class TreeTableRelation
{
    static void setup(ContainerTreePanel treePanel, ComponentDetailsPanel tablePanel)
    {
        treePanel.addTreeSelectionListener(new TreeSelectionListener()
        {
            @Override
            public void valueChanged(TreeSelectionEvent e)
            {
                tablePanel.setProperties(treePanel.getPropertiesFromSelectedNode());
            }
        });
    }
}
