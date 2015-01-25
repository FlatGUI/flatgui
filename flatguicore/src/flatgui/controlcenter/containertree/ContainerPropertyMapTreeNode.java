/*
 * Copyright (c) 2015 Denys Lebediev and contributors. All rights reserved.
 * The use and distribution terms for this software are covered by the
 * Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
 * which can be found in the file LICENSE at the root of this distribution.
 * By using this software in any fashion, you are agreeing to be bound by
 * the terms of this license.
 * You must not remove this notice, or any other, from this software.
 */
package flatgui.controlcenter.containertree;

import javax.swing.tree.TreeNode;
import java.util.*;

/**
 * @author Denis Lebedev
 */
public class ContainerPropertyMapTreeNode implements TreeNode
{
    private static final String ID = ":id";
    private static final String CHILDREN = ":children";
    private static final String LOOK = ":look";
    private static final String EVOLVERS = ":evolvers";
    private static final String PARENTS = ":parents";
    private static final String ROOT_CONTAINER = ":root-container";
    private static final String AUX_CONTAINER = ":aux-container";
    private static final String DEPENDENTS = ":dependent-stage-list";

    // As come from outside
    private Map<Object, Object> containerPropertyMap_;

    private TreeNode parent_;

    private String id_;
    private List<TreeNode> childNodes_;
    private TreeMap<String, Object> sortedProperties_;

    public ContainerPropertyMapTreeNode(TreeNode parent, Map<Object, Object> containerPropertyMap)
    {
        parent_ = parent;

        sortedProperties_ = new TreeMap<>();
        containerPropertyMap_ = containerPropertyMap;
        childNodes_ = new ArrayList<>();
    }

    public void initialize()
    {
        for (Object key : containerPropertyMap_.keySet())
        {
            String keyStr = key != null ? key.toString() : "null";

            if (keyStr.equals(ID))
            {
                id_ = containerPropertyMap_.get(key).toString();
            }
            else if (keyStr.equals(CHILDREN))
            {
                Map<Object, Object> childrenMap = (Map<Object, Object>) containerPropertyMap_.get(key);
                if (childrenMap != null)
                {
                    for (Object childId : childrenMap.keySet())
                    {
                        Map<Object, Object> child = (Map<Object, Object>) childrenMap.get(childId);
                        ContainerPropertyMapTreeNode childNode = new ContainerPropertyMapTreeNode(this, child);
                        childNode.initialize();
                        childNodes_.add(childNode);
                    }
                }
            }
            else
            {
                if (!keyStr.equals(LOOK) && !keyStr.equals(EVOLVERS) && !keyStr.equals(PARENTS) && !keyStr.equals(ROOT_CONTAINER) && !keyStr.equals(AUX_CONTAINER))
                {
                    Object value = containerPropertyMap_.get(key);
                    if (value instanceof List)
                    {
                        value = Arrays.toString( ((List) value).toArray() );
                    }
                    sortedProperties_.put(keyStr, value);
                }
                if (keyStr.equals(LOOK))
                {
                    Object value = containerPropertyMap_.get(key);
                    if (value != null)
                    {
                        sortedProperties_.put(keyStr, value.getClass().getName());
                    }
                }
            }
        }
    }

    @Override
    public TreeNode getChildAt(int childIndex)
    {
        return childNodes_.get(childIndex);
    }

    @Override
    public int getChildCount()
    {
        return childNodes_.size();
    }

    @Override
    public TreeNode getParent()
    {
        return parent_;
    }

    @Override
    public int getIndex(TreeNode node)
    {
        return childNodes_.indexOf(node);
    }

    @Override
    public boolean getAllowsChildren()
    {
        return true;
    }

    @Override
    public boolean isLeaf()
    {
        return childNodes_.size() == 0;
    }

    @Override
    public Enumeration children()
    {
        Vector v = new Vector();
        for (TreeNode node : childNodes_)
        {
            v.addElement(node);
        }
        return v.elements();
    }

    @Override
    public String toString()
    {
        return id_;
    }

    @Override
    public boolean equals(Object other)
    {
        if (other instanceof ContainerPropertyMapTreeNode)
        {
            return id_.equals(((ContainerPropertyMapTreeNode) other).id_);
        }
        return false;
    }

    public Map<String, Object> getNodeProperties()
    {
        return new TreeMap<>(sortedProperties_);
    }
}
