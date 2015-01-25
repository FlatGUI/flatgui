/*
 * Copyright (c) 2015 Denys Lebediev and contributors. All rights reserved.
 * The use and distribution terms for this software are covered by the
 * Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
 * which can be found in the file LICENSE at the root of this distribution.
 * By using this software in any fashion, you are agreeing to be bound by
 * the terms of this license.
 * You must not remove this notice, or any other, from this software.
 */
package flatgui.samples.bookpanel;

import flatgui.core.*;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Scanner;

/**
 * @author Denis Lebedev
 */
public class FGHelloWorldDemo
{
    public static final String CONTAINER_NS = "helloworld";
    public static final String CONTAINER_VAR_NAME = "hellopanel";
    public static final String APP_NAME = "hellopanel";
    public static final String CONTAINER_NAME = "hellopanel";

    public static void main(String[] args)
    {
        EventQueue.invokeLater(() -> {
            try
            {
                Image logoIcon = ImageIO.read(ClassLoader.getSystemResource("flatgui/samples/images/icon_FlatGUI_32x32.png"));

                Frame frame = new Frame("FlatGUI Demo - Hello world");
                frame.setSize(600, 400);
                frame.setLocation(10, 10);
                frame.setLayout(new BorderLayout());
                if (logoIcon != null)
                {
                    frame.setIconImage(logoIcon);
                }
                frame.addWindowListener(new WindowAdapter()
                {
                    public void windowClosing(WindowEvent we)
                    {
                        System.exit(0);
                    }
                });

                FlatGUI flatGui = new FlatGUI();

                URL formUrl = ClassLoader.getSystemResource("flatgui/samples/forms/helloworld.clj");
                flatGui.loadAppFromString(new Scanner(new File(formUrl.toURI())).useDelimiter("\\Z").next(),
                        APP_NAME,
                        CONTAINER_NAME,
                        CONTAINER_NS,
                        CONTAINER_VAR_NAME);

                Component awtComponent = flatGui.getContainerComponent(APP_NAME, CONTAINER_NAME);

                frame.add(awtComponent, BorderLayout.CENTER);
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
