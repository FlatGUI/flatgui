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

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * @author Denis Lebedev
 */
public class FGLogger
{
    private static final DateFormat DATE_FORMAT = new SimpleDateFormat("MMM d HH:mm:ss.SSS Z");

    private static final String DEBUG_PREFIX = "[debug]";
    private static final String INFO_PREFIX = "[info]";
    private static final String ERROR_PREFIX = "[error]";

    public void debug(String message)
    {
        msg(DEBUG_PREFIX, message);
    }

    public void info(String message)
    {
        msg(INFO_PREFIX, message);
    }

    public void error(String message)
    {
        msg(ERROR_PREFIX, message);
    }

    private static void msg(String prefix, String message)
    {
        System.out.println(DATE_FORMAT.format(new Date(System.currentTimeMillis())) +
                " " + prefix + " " +
                message);
    }

}
