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

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @author Denis Lebedev
 */
public class FGStringPool
{
    private final HashMap<String, Integer> stringToIndex_;
    private final String[] strings_;
    private int indexToAddNew_ = 0;

    public FGStringPool(int capacity)
    {
        stringToIndex_ = new HashMap<>(capacity);
        strings_ = new String[capacity];
    }

    public String getStringAt(Integer index)
    {
        return strings_[index.intValue()];
    }

    public Integer getIndexOfString(String s)
    {
        return stringToIndex_.get(s);
    }

    public Integer addString(String s)
    {
        Integer index = stringToIndex_.get(s);
        if (index != null)
        {
            // No changes
            return null;
        }
        else
        {
            int i = indexToAddNew_;
            stringToIndex_.remove(strings_[i]);
            strings_[i] = s;
            Integer ib = Integer.valueOf(i);
            stringToIndex_.put(s, ib);

            indexToAddNew_++;
            if (indexToAddNew_ == strings_.length)
            {
                indexToAddNew_ = 0;
            }

            return ib;
        }
    }

    public Map<Integer, String> addStrings(Collection<String> strings)
    {
        return strings.stream()
                .map(s -> addString(s))
                .filter(i -> i != null)
                .collect(Collectors.toMap(Function.identity(), i -> strings_[i.intValue()]));
    }
}
