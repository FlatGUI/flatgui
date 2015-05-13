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

import flatgui.core.*;
import flatgui.core.awt.FGAWTContainerHost;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.InputStream;
import java.util.*;

/**
 * @author Denis Lebedev
 */
public class FGFocusDemo
{
    public static final String CONTAINER_NS = "fdemo";
    public static final String CONTAINER_VAR_NAME = "fdemopanel";

    public static void main(String[] args)
    {
        EventQueue.invokeLater(() -> {
            try
            {
                Image logoIcon = ImageIO.read(ClassLoader.getSystemResource("flatgui/samples/images/icon_FlatGUI_32x32.png"));

                Frame frame = new Frame("FlatGUI Demo - Focus management");
                frame.setSize(1200, 800);
                frame.setLocation(10, 10);
                frame.setLayout(new BorderLayout());
                if (logoIcon != null)
                {
                    frame.setIconImage(logoIcon);
                }

                InputStream is = FGFocusDemo.class.getClassLoader().getResourceAsStream("flatgui/samples/forms/fdemo.clj");
                String sourceCode = new Scanner(is).useDelimiter("\\Z").next();

                IFGTemplate template = new FGTemplate(sourceCode, CONTAINER_NS, CONTAINER_VAR_NAME);

                IFGContainer instance = new FGContainer(template);

                instance.initialize();

                IFGContainerHost<Component> awtHost = new FGAWTContainerHost();
                Component awtComponent = awtHost.hostContainer(instance);

                frame.add(awtComponent, BorderLayout.CENTER);
                frame.addWindowListener(new WindowAdapter()
                {
                    public void windowClosing(WindowEvent we)
                    {
                        instance.unInitialize();
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
