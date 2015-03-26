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

import java.util.Collection;
import java.util.List;

/**
 * @author Denis Lebedev
 */
public interface IFGEvolveConsumer
{
    /**
     * @return Id Paths of all components in which this consumer is interested.
     */
    public Collection<List<Keyword>> getTargetPaths();

    /**
     * This method is called by FlatGUI Core when Container is evolved for
     * Evolve Reason object targeted to any (or all) components identified by
     * {@link flatgui.core.IFGEvolveConsumer#getTargetPaths()} method
     *
     * @param sessionId Session Id of the session in which Evolve Reason has been
     *                  processed by Container, or null for local desktop
     *                  applications
     * @param containerObject reference to freshly evolved Container
     */
    public void acceptEvolveResult(Object sessionId, Object containerObject);
}
