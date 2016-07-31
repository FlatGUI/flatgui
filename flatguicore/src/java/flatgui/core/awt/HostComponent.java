/*
 * Copyright (c) 2015 Denys Lebediev and contributors. All rights reserved.
 * The use and distribution terms for this software are covered by the
 * Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
 * which can be found in the file LICENSE at the root of this distribution.
 * By using this software in any fashion, you are agreeing to be bound by
 * the terms of this license.
 * You must not remove this notice, or any other, from this software.
 */

package flatgui.core.awt;

import clojure.lang.Keyword;
import clojure.lang.Var;

import flatgui.core.*;
import flatgui.core.util.Tuple;

import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.List;
import java.util.concurrent.Future;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
* @author Denis Lebedev
*/
public class HostComponent extends AbstractHostComponent
{
    private static final Var extractCursor_ = clojure.lang.RT.var(IFGModule.RESPONSE_FEED_NS, "extract-cursor");
    private static final Var getDataForClipboard_ = clojure.lang.RT.var(IFGModule.RESPONSE_FEED_NS, "get-data-for-clipboard");

    private static final Map<Keyword, Integer> FG_TO_AWT_CUSROR_MAP;
    static
    {
        Map<Keyword, Integer> m = new HashMap<>();

        m.put(Keyword.intern("wait"), Cursor.WAIT_CURSOR);
        m.put(Keyword.intern("text"), Cursor.TEXT_CURSOR);
        m.put(Keyword.intern("ns-resize"), Cursor.N_RESIZE_CURSOR);
        m.put(Keyword.intern("ew-resize"), Cursor.W_RESIZE_CURSOR);
        m.put(Keyword.intern("nesw-resize"), Cursor.NE_RESIZE_CURSOR);
        m.put(Keyword.intern("nwse-resize"), Cursor.NW_RESIZE_CURSOR);

        FG_TO_AWT_CUSROR_MAP = Collections.unmodifiableMap(m);
    }


    private IFGContainer fgContainer_;

    private Function<FGEvolveInputData, Future<FGEvolveResultData>> feedFn_;

    Future<FGEvolveResultData> changedPathsFuture_;

    public void initialize(IFGContainer fgContainer)
    {
        fgContainer_ = fgContainer;
    }

    public ActionListener getEventFedCallback()
    {
        return e -> repaint();
    }

    public void setInputEventConsumer(Function<FGEvolveInputData, Future<FGEvolveResultData>> feedFn)
    {
        feedFn_ = feedFn;
    }

    @Override
    protected void changeCursorIfNeeded() throws Exception
    {
        FGEvolveResultData evolveResultData = changedPathsFuture_.get();
        Set<Object> reasons = evolveResultData.getEvolveReasonToTargetPath().keySet();
        if (!reasons.isEmpty() && reasons.stream().anyMatch(r -> r instanceof MouseEvent))
        {
            Collection<List<Keyword>> targetComponentPaths = evolveResultData.getEvolveReasonToTargetPath().values();
            Map<List<Keyword>, Map<Keyword, Object>> targetIdPathToComponent = fgContainer_.getFGModule().getComponentIdPathToComponent(targetComponentPaths);
            Keyword c = resolveCursor(targetIdPathToComponent, fgContainer_);
            Integer cursor = c != null ? FG_TO_AWT_CUSROR_MAP.get(c) : null;
            setCursor(cursor != null ? Cursor.getPredefinedCursor(cursor.intValue()) : Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
        }
    }

    @Override
    protected Iterable<Object> getPaintList(double clipX, double clipY, double clipW, double clipH) throws Exception
    {
        Future<List<Object>> paintListFuture =
                fgContainer_.submitTask(() -> fgContainer_.getFGModule().getPaintAllSequence(clipX, clipY, clipW, clipH));
        return paintListFuture.get();
    }

    public static Keyword resolveCursor(Map<java.util.List<Keyword>, Map<Keyword, Object>> idPathToComponent,
                                        IFGContainer fgContainer)
    {
        Map<java.util.List<Keyword>, Keyword> componentToCursor =
            idPathToComponent.entrySet().stream()
                .map(e -> Tuple.pair(e.getKey(), extractCursor_.invoke(e.getValue())))
                .filter(t -> t.getSecond() != null)
                .collect(Collectors.toMap(t -> t.getFirst(), t -> t.getSecond()));

        if (componentToCursor.isEmpty())
        {
            // Default cursor
            return null;
        }
        else
        {
            Keyword c = componentToCursor.get(fgContainer.getLastMouseTargetIdPath());
            if (c == null)
            {
                c = componentToCursor.values().stream().findAny().orElse(null);
            }
            return c;
        }
    }

    // TODO add content type info; support other content types
    public static String getTextForClipboard(IFGContainer fgContainer)
    {
        Object data = getDataForClipboard_.invoke(fgContainer.getFGModule().getContainer());
        return data != null ? data.toString() : null;
    }

    @Override
    protected void acceptEvolveReason(Object evolveReason)
    {
        changedPathsFuture_ = feedFn_.apply(new FGEvolveInputData(evolveReason, false));
    }
}
