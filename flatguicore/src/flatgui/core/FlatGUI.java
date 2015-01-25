/*
 * Copyright (c) 2015 Denys Lebediev and contributors. All rights reserved.
 * The use and distribution terms for this software are covered by the
 * Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
 * which can be found in the file LICENSE at the root of this distribution.
 * By using this software in any fashion, you are agreeing to be bound by
 * the terms of this license.
 * You must not remove this notice, or any other, from this software.
 */

package flatgui.core;

import flatgui.controlcenter.IFGStateProvider;
import flatgui.core.websocket.FGAppServer;

import java.awt.*;
import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

/**
 * @author Denis Lebedev
 */
public class FlatGUI
{
    private final Map<String, Map<String, IFGContainer>> appNameToContainerNameToContaier_;

    public FlatGUI()
    {
        appNameToContainerNameToContaier_ = new HashMap<>();
    }

    /*
     * NOTE: all current method implementations are subject to change
     */

    public void loadAppFromFile(String pathName,
                                       String applicationName,
                                       String containerName,
                                       String containerNs,
                                       String containerVarName) throws Exception
    {
        File srcFile = new File(pathName);
        String content = new Scanner(srcFile).useDelimiter("\\Z").next();

        loadAppFromString(content, applicationName, containerName, containerNs, containerVarName);
    }

    public void loadAppFromString(String content,
                                         String applicationName,
                                         String containerName,
                                         String containerNs,
                                         String containerVarName)
    {
        IFGModule fgModule = new FGModule(containerName);
        IFGContainer fgContainer = new FGContainer(fgModule);
        FGContainerBase.registerContainer(containerName, fgContainer);

        FGClientAppLoader.initializeFromContent(content);
        FGClientAppLoader.registerContainer(containerNs, containerVarName, containerName);
        FGContainerBase.initializeContainer(containerName);

        getAppContainerMap(applicationName).put(containerName, fgContainer);

        // Server support; will be refactored
        FGAppServer.setDefaultApplicationName(applicationName);
        FGAppServer.deployApplication1(containerNs, containerVarName, applicationName);
    }

    // TODO session/instance id
    public Component getContainerComponent(String applicationName,
                                                     String containerName)
    {
        IFGContainer container = getContainer(applicationName, containerName, null);

        // TODO apply the font with which it was actually initialized

        Component c = container.getContainerComponent();
        return c;
    }

    public void startContainerServer(String applicationName,
                                            String containerName,
                                            int port) throws Exception
    {
        FGAppServer.start(port);
    }

    public IFGStateProvider getContainerStateProvider(String applicationName,
                                                           String containerName,
                                                           String sessionId)

    {
        IFGContainer container = getContainer(applicationName, containerName, sessionId);
        return new FGStateProvider(container.getFGModule());

        // TODO implement for WebSocket sessions as well
    }

    // Private

    private Map<String, IFGContainer> getAppContainerMap(String appName)
    {
        Map<String, IFGContainer> map = appNameToContainerNameToContaier_.get(appName);
        if (map == null)
        {
            map = new HashMap<>();
            appNameToContainerNameToContaier_.put(appName, map);
        }
        return map;
    }

    private IFGContainer getContainer(String applicationName, String containerName, String sessionId)
    {
        // TODO by sessionId

        IFGContainer container = getAppContainerMap(applicationName).get(containerName);
        if (container == null)
        {
            throw new IllegalArgumentException(
                    "App " + applicationName + " container " + containerName + " is not registered");
        }
        return container;
    }

    // Inner classes

    private static class FGStateProvider implements IFGStateProvider
    {
        private IFGModule fgModule_;

        FGStateProvider(IFGModule fgModule)
        {
            fgModule_ = fgModule;
        }

        @Override
        public Object getContainer()
        {
            return fgModule_.getContainerObject();
        }
    }
}
