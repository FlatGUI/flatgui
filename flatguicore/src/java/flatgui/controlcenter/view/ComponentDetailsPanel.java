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

import flatgui.controlcenter.componenttable.ComponentPropertiesTableModel;
import flatgui.controlcenter.componenttable.PropertyValueRenderer;

import javax.swing.*;
import java.awt.*;
import java.util.Map;

/**
 * @author Denis Lebedev
 */
public class ComponentDetailsPanel implements IControlCenterPanel
{
    static final String NAME = "Component Details";

    private JPanel mainPanel_;
    private ComponentPropertiesTableModel tableModel_;
    private JTable componentDetailsTable_;

    ComponentDetailsPanel()
    {
        mainPanel_ = new JPanel();
        tableModel_ = new ComponentPropertiesTableModel();
        componentDetailsTable_ = new JTable();
        componentDetailsTable_.setRowHeight(componentDetailsTable_.getRowHeight()*3);

        mainPanel_.setLayout(new BorderLayout());
        mainPanel_.add(new JScrollPane(componentDetailsTable_), BorderLayout.CENTER);
    }

    @Override
    public String getName()
    {
        return NAME;
    }

    @Override
    public JButton[] getToolButtons()
    {
        return new JButton[0];
    }

    @Override
    public JComponent getContentComponent()
    {
        return mainPanel_;
    }

    public void setProperties(Map<String, Object> properties)
    {
        tableModel_ = new ComponentPropertiesTableModel(properties);
        componentDetailsTable_.setModel(tableModel_);
        componentDetailsTable_.getColumn(componentDetailsTable_.getColumnName(1)).setCellRenderer(new PropertyValueRenderer());
    }
}
