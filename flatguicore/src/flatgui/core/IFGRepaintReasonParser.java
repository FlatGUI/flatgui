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

import java.util.Collection;
import java.util.Map;

/**
 * @author Denys Lebediev
 *         Date: 8/11/13
 *         Time: 4:01 PM
 */
public interface IFGRepaintReasonParser<Reason>
{
    public Map<String, Object> initialize(IFGModule fgModule);

    public Map<String, Object> getTargetedPropertyValues(Reason reason);

    /**
     * Determines target cells that need to receive given update. Depending
     * of the nature of update, implementation my decide to direct it to
     * different cells. For example, mouse updates may be targeted to cells
     * that are under cusror at the moment; keyboard updates may be targeted
     * to focus owner; timer updates may be targeted to cells that are explicitly
     * subscribed to the times. All these criterias may be calculated with the
     * help of fgModule passed as an argument. Implementation may generate more
     * that one reasons and target cell groups if needed
     *
     * @param reason the update
     * @param fgModule FlatGUI module that may be used by implementation to
     *                 find out what cells should receive the update.
     * @param generalPropertyMap
     * @return a map of reason to target cell ids. Ids may be null in case all
     *         cells should receive the update.
     */
    public Map<Reason, Collection<Object>> getTargetCellIds(Reason reason, IFGModule fgModule, Map<String, Object> generalPropertyMap);

}
