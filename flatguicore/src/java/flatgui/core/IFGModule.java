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

import clojure.lang.Keyword;
import flatgui.core.awt.FGMouseTargetComponentInfo;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author Denys Lebediev
 *         Date: 10/1/13
 *         Time: 9:51 PM
 */
public interface IFGModule
{
    String FG_CORE_NAMESPACE = "flatgui.appcontainer";
    String RESPONSE_FEED_NS = "flatgui.responsefeed";

    public void evolve(List<Keyword> targetCellIds, Object inputEvent);

    public FGMouseTargetComponentInfo getMouseTargetInfoAt(double x, double y, FGComponentPath knownPath);

    public Object getContainer();

    public List<Keyword> getFocusedPath();

    // Painting

    // old approach

    public List<Object> getPaintAllSequence();

    public List<Object> getPaintAllSequence(double clipX, double clipY, double clipW, double clipH);

    public Object getDirtyRectFromContainer();

    public List<Object> getPaintChangesSequence(Collection dirtyRects);

    // new approach

    public Set<List<Keyword>> getChangedComponentIdPaths();

    public Map<List<Keyword>, Map<Keyword, Object>> getComponentIdPathToComponent(Collection<List<Keyword>> paths);

    public Map<Object, Object> getStringPoolDiffs(Map<List<Keyword>, List<String>> idPathToStrings);

    public Integer getStringPoolId(String s, Object componentId);

    public List<Object> getPaintAllSequence2();
}
