/*
 * Copyright (c) 2015 Denys Lebediev and contributors. All rights reserved.
 * The use and distribution terms for this software are covered by the
 * Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
 * which can be found in the file LICENSE at the root of this distribution.
 * By using this software in any fashion, you are agreeing to be bound by
 * the terms of this license.
 * You must not remove this notice, or any other, from this software.
 */

package flatgui.samples;

import flatgui.core.engine.ui.FGAWTAppContainer;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.InputStream;

/**
 * @author Denis Lebedev
 */
public class FGSliderDemo
{
    public static final String CONTAINER_NS = "sliderdemo";
    public static final String CONTAINER_VAR_NAME = "sliderdemopanel";

    public static void main(String[] args)
    {
        EventQueue.invokeLater(() -> {
            try
            {
                Image logoIcon = ImageIO.read(ClassLoader.getSystemResource("flatgui/samples/images/icon_FlatGUI_32x32.png"));

                Frame frame = new Frame("FlatGUI Demo - Text Editor");
                frame.setSize(600, 500);
                frame.setLocation(300, 300);
                frame.setLayout(new BorderLayout());
                if (logoIcon != null)
                {
                    frame.setIconImage(logoIcon);
                }

                InputStream is = FGCompoundDemoServer.class.getClassLoader().getResourceAsStream("flatgui/samples/forms/sliderdemo.clj");
                FGAWTAppContainer appContainer = FGAWTAppContainer.loadSourceCreateAndInit(is, CONTAINER_NS, CONTAINER_VAR_NAME);
                Component awtComponent = appContainer.getComponent();

                frame.add(awtComponent, BorderLayout.CENTER);
                frame.addWindowListener(new WindowAdapter()
                {
                    public void windowClosing(WindowEvent we)
                    {
                        appContainer.unInitialize();
                        System.exit(0);
                    }
                });
                frame.setVisible(true);
                awtComponent.requestFocusInWindow();

                awtComponent.repaint();
            }
            catch(Exception ex)
            {
                ex.printStackTrace();
            }
        });
    }
}
