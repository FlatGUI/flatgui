/*
 * Copyright (c) 2015 Denys Lebediev and contributors. All rights reserved.
 * The use and distribution terms for this software are covered by the
 * Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
 * which can be found in the file LICENSE at the root of this distribution.
 * By using this software in any fashion, you are agreeing to be bound by
 * the terms of this license.
 * You must not remove this notice, or any other, from this software.
 */

package flatgui.core.util;

import java.util.Arrays;

/**
 * @author Denis Lebedev
 */
public class Tuple
{
    private final Object[] data_;

    public Tuple(Object[] data)
    {
        data_ = data;
    }

    public static <F,S> Tuple pair(F f, S s)
    {
        return new Tuple(new Object[]{f, s});
    }

    public static <F,S,T> Tuple triple(F f, S s, T t)
    {
        return new Tuple(new Object[]{f, s, t});
    }

    // Ways to access

    public <T> T get(int index)
    {
        return (T) data_[index];
    }

    public <F> F getFirst()
    {
        return get(0);
    }

    public <S> S getSecond()
    {
        return get(1);
    }

    // General

    @Override
    public String toString()
    {
        return Arrays.toString(data_);
    }
}
