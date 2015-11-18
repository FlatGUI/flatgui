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

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * @author Denis Lebedev
 */
public class ControlCenterFrame extends JFrame
{

    private JToolBar toolBar_;
    private JSplitPane splitPane_;

    private IFGStateProvider fgStateProvider_;

    private Map<String, IControlCenterPanel> panels_;

    public ControlCenterFrame(IFGStateProvider fgStateProvider)
    {
        super("FlatGUI Control Center");
        fgStateProvider_ = fgStateProvider;
        prepare();
    }

    private void prepare()
    {
        setLayout(new BorderLayout());
        toolBar_ = new JToolBar();
        splitPane_ = new JSplitPane();
        splitPane_.setDividerLocation(300);
        add(toolBar_, BorderLayout.NORTH);
        add(splitPane_, BorderLayout.CENTER);

        panels_ = createPanels();

        placePanels();

        setupPanelToolButtons();
    }

    private Map<String, IControlCenterPanel> createPanels()
    {
        Map<String, IControlCenterPanel> panels = new LinkedHashMap<>();

        ContainerTreePanel treePanel = new ContainerTreePanel(fgStateProvider_);
        panels.put(treePanel.getName(), treePanel);

        ComponentDetailsPanel componentDetailsPanel = new ComponentDetailsPanel();
        panels.put(componentDetailsPanel.getName(), componentDetailsPanel);

        TreeTableRelation.setup(treePanel, componentDetailsPanel);

        return panels;
    }

    private void placePanels()
    {
        IControlCenterPanel treePanel = panels_.get(ContainerTreePanel.NAME);
        IControlCenterPanel componentDetailsPanel = panels_.get(ComponentDetailsPanel.NAME);

        splitPane_.setLeftComponent(createPanelComponent(treePanel));
        splitPane_.setRightComponent(createPanelComponent(componentDetailsPanel));
    }

    private JComponent createPanelComponent(IControlCenterPanel panel)
    {
        JPanel holder = new JPanel(new BorderLayout());
        holder.add(panel.getContentComponent(), BorderLayout.CENTER);
        holder.setBorder(new TitledBorder(panel.getName()));
        return holder;
    }

    private void setupPanelToolButtons()
    {
        for (IControlCenterPanel panel : panels_.values())
        {
            if (toolBar_.getComponentCount() > 0)
            {
                toolBar_.add(new JSeparator(SwingConstants.VERTICAL));
            }
            JButton[] toolButtons = panel.getToolButtons();
            for (JButton button : toolButtons)
            {
                toolBar_.add(button);
            }
        }
    }
}
