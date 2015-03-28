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

import java.awt.event.ActionListener;
import java.util.Collection;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.function.Consumer;

/**
 * @author Denys Lebediev
 *         Date: 10/1/13
 *         Time: 9:27 PM
 */
public interface IFGContainer
{
    // TODO
    public static final int UNIT_SIZE_PX = 64;
    public static final String GENERAL_PROPERTY_UNIT_SIZE = "UnitSizePx";


    public String getId();

    public void initialize();

    public void unInitialize();

    public void addEvolveConsumer(IFGEvolveConsumer consumer);

    public IFGModule getFGModule();

    public Consumer<Object> connect(ActionListener eventFedCallback, Object hostContext);

    public <T> Future<T> submitTask(Callable<T> callable);

    public void feedEvent(Object repaintReason);

    public void feedTargetedEvent(Collection<Object> targetCellIdPath, Object repaintReason);

    // TODO following methods do not belong to here

    public Object getGeneralProperty(String propertyName);

    public Object getAWTUtil();

}
