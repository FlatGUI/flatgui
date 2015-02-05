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
import java.io.File;
import java.net.URL;
import java.util.Scanner;

/**
 * @author Denis Lebedev
 */
public class FGCompoundDemo
{
    public static final String CONTAINER_NS = "compound";
    public static final String CONTAINER_VAR_NAME = "compoundpanel";

    public static void main(String[] args)
    {
        EventQueue.invokeLater(() -> {
            try
            {
                Image logoIcon = ImageIO.read(ClassLoader.getSystemResource("flatgui/samples/images/icon_FlatGUI_32x32.png"));

                Frame frame = new Frame("FlatGUI Demo - Compound Sample");
                frame.setSize(1024, 768);
                frame.setLocation(10, 10);
                frame.setLayout(new BorderLayout());
                if (logoIcon != null)
                {
                    frame.setIconImage(logoIcon);
                }
                URL formUrl = ClassLoader.getSystemResource("flatgui/samples/forms/compound.clj");
                String sourceCode = new Scanner(new File(formUrl.toURI())).useDelimiter("\\Z").next();

                IFGTemplate appTemplate = new FGTemplate(sourceCode, CONTAINER_NS, CONTAINER_VAR_NAME);

                IFGContainer appInstance = new FGContainer(appTemplate);

                appInstance.initialize();

                IFGContainerHost<Component> awtHost = new FGAWTContainerHost();
                Component awtComponent = awtHost.hostContainer(appInstance);

                frame.add(awtComponent, BorderLayout.CENTER);
                frame.addWindowListener(new WindowAdapter()
                {
                    public void windowClosing(WindowEvent we)
                    {
                        appInstance.unInitialize();
                        System.exit(0);
                    }
                });
                frame.setVisible(true);
                awtComponent.requestFocusInWindow();
            }
            catch(Exception ex)
            {
                ex.printStackTrace();
            }
        });
    }
}
