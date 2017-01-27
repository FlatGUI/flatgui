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

import clojure.lang.Keyword;
import flatgui.core.IFGEvolveConsumer;
import flatgui.core.engine.ui.FGAWTAppContainer;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.InputStream;
import java.util.*;
import java.util.List;

/**
 * @author Denis Lebedev
 */
public class FGChildGenDemo
{
    public static final String CONTAINER_NS = "childgen";
    public static final String CONTAINER_VAR_NAME = "cgpanel";

    public static void main(String[] args)
    {
        EventQueue.invokeLater(() -> {
            try
            {
                Image logoIcon = ImageIO.read(ClassLoader.getSystemResource("flatgui/samples/images/icon_FlatGUI_32x32.png"));

                Frame frame = new Frame("FlatGUI Demo - Dynamic children generation");
                frame.setSize(1200, 800);
                frame.setLocation(10, 10);
                frame.setLayout(new BorderLayout());
                if (logoIcon != null)
                {
                    frame.setIconImage(logoIcon);
                }

                InputStream is = FGCompoundDemoServer.class.getClassLoader().getResourceAsStream("flatgui/samples/forms/childgen.clj");
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

    private static class DemoConsumer implements IFGEvolveConsumer
    {
        private final Collection<List<Keyword>> paths_;

        DemoConsumer()
        {
            List<Keyword> path = Arrays.asList(
                    Keyword.intern("main"),
                    Keyword.intern("hello"),
                    Keyword.intern("greeting"));
            paths_ = Collections.unmodifiableList(Arrays.asList(path));
        }


        @Override
        public Collection<List<Keyword>> getTargetPaths()
        {
            return paths_;
        }

        @Override
        public Collection<Keyword> getTargetProperties(List<Keyword> path)
        {
            return Arrays.asList(Keyword.intern("text"));
        }

        @Override
        public void acceptEvolveResult(Object sessionId, Object containerObject)
        {
            try
            {
                Map<Object, Object> mainChildren = (Map<Object, Object>) ((Map<Object, Object>) containerObject).get(Keyword.intern("children"));
                Map<Object, Object> helloWindow = (Map<Object, Object>) mainChildren.get(Keyword.intern("hello"));
                Map<Object, Object> helloChildren = (Map<Object, Object>) helloWindow.get(Keyword.intern("children"));
                Map<Object, Object> greetingLabel = (Map<Object, Object>) helloChildren.get(Keyword.intern("greeting"));
                String labelText = (String) greetingLabel.get(Keyword.intern("text"));

                System.out.println("Label text is " + labelText);
            }
            catch (Exception ex)
            {
                ex.printStackTrace();
            }
        }
    }
}
