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

import java.lang.reflect.Constructor;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Denis Lebedev
 */
public class FGContainerBase
{
    //private static IFGModule fgModule_;

    private static Map<String, IFGContainer> nameToContainer_ = new HashMap<>();

    //private static String currentContainerName_;

    //private static Class<? extends IFGContainer> containerClass_ = null;

    // TODO temporary
    private static IFGContainer refContainer_;

    private FGContainerBase()
    {
    }

//    public static synchronized void setContainerClass(Class<? extends IFGContainer> containerClass)
//    {
//        containerClass_ = containerClass;
//    }

//    public static synchronized void setFGModule(IFGModule fgModule)
//    {
//        fgModule_ = fgModule;
//    }


    // TODO this is temporary
    public static synchronized void setRefContainer(IFGContainer container)
    {
        refContainer_ = container;
    }

    // TODO this is temporary
    public static synchronized IFGContainer getCurrentContainer()
    {
        if (refContainer_ != null)
        {
            return refContainer_;
        }

        if (nameToContainer_.size() == 0)
        {
            System.out.println("-DLTEMP- FGContainerBase.getCurrentContainer CAUTION!!!  getCurrentContainer returns null");
        }

        return nameToContainer_.size() > 0 ? nameToContainer_.values().iterator().next() : null;
    }

//    public static synchronized IFGContainer getCurrentContainer()
//    {
//        return getContainer(currentContainerName_);
//    }
//
//    public static synchronized String getCurrentContainerName()
//    {
//        return currentContainerName_;
//    }

//    public static synchronized boolean isInitialized()
//    {
//        return fgModule_ != null;
//    }



//    public static synchronized IFGContainer getContainer(String containerName)
//    {
//        IFGContainer container = nameToContainer_.get(containerName);
//        if (container == null)
//        {
//            if (fgModule_ == null)
//            {
//                throw new IllegalStateException(IFGModule.class.getName() + " not initialized.");
//            }
//            if (containerClass_ == null)
//            {
//                container = new FGContainer(fgModule_);
//            }
//            else
//            {
//                try
//                {
//                    Constructor<? extends IFGContainer> c = containerClass_.getConstructor(new Class[]{IFGModule.class});
//                    container = c.newInstance(fgModule_);
//                }
//                catch (Exception e)
//                {
//                    e.printStackTrace();
//                }
//            }
//            nameToContainer_.put(containerName, container);
//        }
//        return container;
//    }

    public static synchronized void registerContainer(String containerName, IFGContainer container)
    {
        nameToContainer_.put(containerName, container);
    }

    public static synchronized void initializeContainer(String containerName)
    {
        nameToContainer_.get(containerName).initialize();
    }

    public static synchronized boolean removeContainer(String containerName)
    {
        return nameToContainer_.remove(containerName) != null;
    }

//    public static synchronized void setCurrentContainerName(String containerName)
//    {
//        currentContainerName_ = containerName;
//    }
}
