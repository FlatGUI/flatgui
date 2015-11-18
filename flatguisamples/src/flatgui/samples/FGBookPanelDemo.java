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
import flatgui.core.awt.HostComponent;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.InputStream;
import java.util.Scanner;

/**
 * @author Denis Lebedev
 */
public class FGBookPanelDemo
{
    public static final String CONTAINER_NS = "bookpanelmain";
    public static final String CONTAINER_VAR_NAME = "bookpanel";

    public static void main(String[] args)
    {
        EventQueue.invokeLater(() -> {
            try
            {
                Image logoIcon = ImageIO.read(ClassLoader.getSystemResource("flatgui/samples/images/icon_FlatGUI_32x32.png"));

                Frame frame = new Frame("FlatGUI Demo - BookPanel");
                frame.setSize(1600, 1200);
                frame.setLocation(0, 0);
                frame.setLayout(new BorderLayout());
                if (logoIcon != null)
                {
                    frame.setIconImage(logoIcon);
                }

//                JFrame controlCenterFrame = new ControlCenterFrame(
//                        flatGui.getContainerStateProvider(APP_NAME,
//                            CONTAINER_NAME, null));
//                controlCenterFrame.setSize(1200, 900);
//                controlCenterFrame.setLocation(100, 200);
//                controlCenterFrame.setVisible(true);
//                controlCenterFrame.setState(Frame.ICONIFIED);
//                if (logoIcon != null)
//                {
//                    controlCenterFrame.setIconImage(logoIcon);
//                }

                InputStream is = FGCompoundDemoServer.class.getClassLoader().getResourceAsStream("flatgui/samples/forms/bookpanelmain.clj");
                String sourceCode = new Scanner(is).useDelimiter("\\Z").next();

                IFGTemplate appTemplate = new FGTemplate(sourceCode, CONTAINER_NS, CONTAINER_VAR_NAME);

                HostComponent hc = new HostComponent();
                IFGContainer appInstance = new FGContainer(appTemplate, hc.getInterop());
                appInstance.initialize();
                IFGContainerHost<Component> awtHost = new FGAWTContainerHost(hc);
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

                awtComponent.repaint();
            }
            catch (Exception ex)
            {
                ex.printStackTrace();
            }
        });
    }
}
