/*
 * Copyright Denys Lebediev
 */
package flatgui.controlcenter.view;

import javax.swing.*;

/**
 * @author Denys Lebediev
 *         Date: 3/1/14
 *         Time: 4:14 PM
 */
public interface IControlCenterPanel
{
    public String getName();

    public JButton[] getToolButtons();

    public JComponent getContentComponent();

}
