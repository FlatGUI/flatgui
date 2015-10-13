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

/**
 * @author Denys Lebediev
 *         Date: 8/15/13
 *         Time: 9:24 PM
 */
public class FGTimerEventParser extends FGFocusTargetedEventParser<FGTimerEvent>
{
    // TODO
    // It is a FGFocusTargetedEventParser but it should send events to all interested component evolvers.
    // This is not implemented yet, and should work for other input parsers as well (for example, no need
    // to invoke an evolver fn in case of timer event if it does not depend on timer)
}