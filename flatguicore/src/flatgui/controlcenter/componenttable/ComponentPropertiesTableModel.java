/*
 * Copyright (c) 2015 Denys Lebediev and contributors. All rights reserved.
 * The use and distribution terms for this software are covered by the
 * Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
 * which can be found in the file LICENSE at the root of this distribution.
 * By using this software in any fashion, you are agreeing to be bound by
 * the terms of this license.
 * You must not remove this notice, or any other, from this software.
 */

package flatgui.controlcenter.componenttable;

import javax.swing.table.AbstractTableModel;
import java.util.Collections;
import java.util.Map;

/**
 * @author Denis Lebedev
 */
public class ComponentPropertiesTableModel extends AbstractTableModel
{
    private String[] ids_;
    private Object[] values_;

    public ComponentPropertiesTableModel()
    {
        this(Collections.EMPTY_MAP);
    }

    public ComponentPropertiesTableModel(Map<String, Object> properties)
    {
        int size = properties.size();
        ids_ = new String[size];
        values_ = new Object[size];
        int i=0;
        for (String key : properties.keySet())
        {
            ids_[i] = key;
            values_[i] = properties.get(key);
            i++;
        }
    }

    @Override
    public int getRowCount()
    {
        return ids_.length;
    }

    @Override
    public int getColumnCount()
    {
        return 2;
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex)
    {
        return columnIndex == 0 ? ids_[rowIndex] : values_[rowIndex];
    }
}
