/*
; Copyright (c) 2016 Denys Lebediev and contributors. All rights reserved.
; The use and distribution terms for this software are covered by the
; Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
; which can be found in the file LICENSE at the root of this distribution.
; By using this software in any fashion, you are agreeing to be bound by
; the terms of this license.
; You must not remove this notice, or any other, from this software.
 */
package flatgui.core.engine;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @author Denis Lebedev
 */
public class Container
{
    private final IResultCollector resultCollector_;

    private final Set<Integer> vacantComponentIndices_;
    private final Set<Integer> vacantNodeIndices_;

    private final List<ComponentAccessor> components_;
    private final Map<List<Object>, Integer> componentPathToIndex_;

    private final List<Node> nodes_;
    private final List<Object> values_;
    private final Map<List<Object>, Integer> pathToIndex_;

    private final IContainerParser containerParser_;

    private Node[] reusableNodeBuffer_;
    private Object[] reusableReasonBuffer_;
    private int indexBufferSize_;

    private boolean initializationCycle_;

    public Container(IContainerParser containerParser, IResultCollector resultCollector, Map<Object, Object> container)
    {
        containerParser_ = containerParser;
        resultCollector_ = resultCollector;
        components_ = new ArrayList<>();
        componentPathToIndex_ = new HashMap<>();
        vacantComponentIndices_ = new HashSet<>();
        vacantNodeIndices_ = new HashSet<>();
        nodes_ = new ArrayList<>();
        values_ = new ArrayList<>();
        pathToIndex_ = new HashMap<>();

        reusableNodeBuffer_ = new Node[128];
        reusableReasonBuffer_ = new Integer[128];
        indexBufferSize_ = 0;

        addContainer(new ArrayList<>(), container);

        initializationCycle_ = true;

        // TODO initialize;

        initializationCycle_ = false;
    }

    public Integer addComponent(List<Object> componentPath, ComponentAccessor component)
    {
        Integer index;
        if (vacantComponentIndices_.isEmpty())
        {
            index = components_.size();
        }
        else
        {
            index = vacantComponentIndices_.stream().findAny().get();
            vacantComponentIndices_.remove(index);
        }
        components_.set(index, component);
        componentPathToIndex_.put(componentPath, index);
        return index;
    }

    public int indexOfPath(List<Object> path)
    {
        return pathToIndex_.get(path);
    }

    public void evolve(List<Object> targetPath, Object evolveReason)
    {
        Integer componentUid = componentPathToIndex_.get(targetPath);
        if (componentUid == null)
        {
            throw new IllegalArgumentException("Component path does not exist: " + targetPath);
        }

        indexBufferSize_ = 0;

        ComponentAccessor componentAccessor = components_.get(componentUid);
        Map<Object, Integer> propertyIdToNodeIndex = componentAccessor.getPropertyIdToIndex();
        for (Object propertyId : propertyIdToNodeIndex.keySet())
        {
            Integer nodeIndex = propertyIdToNodeIndex.get(propertyId);
            Node node = nodes_.get(nodeIndex);
            if (containerParser_.isInterestedIn(node.getNodePath(), evolveReason))
            {
                addNodeToReusableBuffer(node, evolveReason);
            }
        }

        int currentNode = 0;
        while (currentNode < indexBufferSize_)
        {
            Node node = reusableNodeBuffer_[currentNode];
            int nodeIndex = node.getNodeIndex().intValue();
            Function<Map<Object, Object>, Object> evolver = node.getEvolver();

            Object oldValue;
            Object newValue;
            ComponentAccessor component = components_.get(node.getComponentUid());
            if (initializationCycle_)
            {
                oldValue = null;
                newValue = values_.get(nodeIndex);
            }
            else
            {
                oldValue = values_.get(nodeIndex);
                newValue = evolver.apply(component);
            }

            if (!Objects.equals(oldValue, newValue))
            {
                values_.set(nodeIndex, newValue);
                resultCollector_.appendResult(component.getComponentPath(), node.getPropertyId(), newValue);

                addNodeDependentsToEvolvebuffer(node);
            }

            currentNode++;
        }
    }

    // Private

    private void addContainer(List<Object> pathToContainer, Map<Object, Object> container)
    {
        // Add and thus index all components/properties

        List<Object> componentPath = new ArrayList<>(pathToContainer.size()+1);
        componentPath.add(containerParser_.getComponentId(container));

        ComponentAccessor component = new ComponentAccessor(componentPath, values_);
        Integer componentUid = addComponent(componentPath, component);
        Collection<SourceNode> componentPropertyNodes = containerParser_.processComponent(
                componentPath, container, values_, pathToIndex_::get);
        for (SourceNode node : componentPropertyNodes)
        {
            Integer nodeIndex = addNode(componentUid, node, container.get(node.getPropertyId()));
            component.putPropertyIndex(node.getPropertyId(), nodeIndex);
        }

        Map<Object, Map<Object, Object>> children = containerParser_.getChildren(container);
        for (Map<Object, Object> child : children.values())
        {
            addContainer(componentPath, child);
        }

        // Now that all components/properties are indexed, resolve dependency indices for each property

        nodes_.forEach(n -> n.resolveDependencyIndices(path -> pathToIndex_.get(path)));

        // For each component N, take its dependencies and mark that components that they have N as a dependent

        nodes_.forEach(n -> n.getDependencyIndices()
                .forEach(dependencyIndex -> nodes_.get(dependencyIndex).addDependent(n.getNodeIndex())));

        // TODO Optimize:
        // remove dependents covered by longer chains. Maybe not remove but just hide since longer chains may be provided
        // by components that may be removed
    }

    private Integer addNode(Integer componentUid, SourceNode sourceNode, Object initialValue)
    {
        Integer index;
        if (vacantNodeIndices_.isEmpty())
        {
             index = nodes_.size();
        }
        else
        {
            index = vacantNodeIndices_.stream().findAny().get();
            vacantNodeIndices_.remove(index);
        }
        nodes_.set(index.intValue(), new Node(
                componentUid,
                sourceNode.getPropertyId(),
                sourceNode.getNodePath(),
                index,
                sourceNode.getDependencyPaths(),
                sourceNode.getInputDependencies(),
                sourceNode.getEvolver()));
        pathToIndex_.put(sourceNode.getNodePath(), index);
        values_.set(index.intValue(), initialValue);
        return index;
    }

    private void addNodeToReusableBuffer(Node node, Object evolveReason)
    {
        ensureIndexBufferSize(reusableNodeBuffer_.length + 1);
        reusableNodeBuffer_[indexBufferSize_] = node;
        reusableReasonBuffer_[indexBufferSize_] = evolveReason;
        indexBufferSize_ ++;
    }

    private void addNodeDependentsToEvolvebuffer(Node node)
    {
        Collection<Integer> dependents = node.getDependentIndices();
        int dependentCollSize = dependents.size();
        ensureIndexBufferSize(reusableNodeBuffer_.length + dependentCollSize);
        for (Integer i : dependents)
        {
            reusableNodeBuffer_[indexBufferSize_] = nodes_.get(i.intValue());
            reusableReasonBuffer_[indexBufferSize_] = node.getNodePath();
            indexBufferSize_ ++;
        }
    }

    private void ensureIndexBufferSize(int requestedSize)
    {
        if (requestedSize >= reusableNodeBuffer_.length)
        {
            reusableNodeBuffer_ = ensureBufferSize(reusableNodeBuffer_, requestedSize);
            reusableReasonBuffer_ = ensureBufferSize(reusableReasonBuffer_, requestedSize);
        }
    }

    private static <T> T[] ensureBufferSize(T[] oldBuffer, int requestedSize)
    {
        if (requestedSize >= oldBuffer.length)
        {
            T[] newBuffer = (T[]) new Object[oldBuffer.length + 128];
            System.arraycopy(oldBuffer, 0, newBuffer, 0, oldBuffer.length);
            return newBuffer;
        }
        return oldBuffer;
    }


    // // //

    /**
     * Represents a property of a component (internal indexed)
     */
    public static class SourceNode
    {
        // Path means: last element is property id, all elements before last represent component path

        private final Object propertyId_;

        private final List<Object> nodePath_;

        private final Collection<List<Object>> dependencyPaths_;

        private final Function<Map<Object, Object>, Object> evolver_;

        private final List<Object> inputDependencies_;

        public SourceNode(
                Object propertyId,
                List<Object> nodePath,
                Collection<List<Object>> dependencyPaths,
                Function<Map<Object, Object>, Object> evolver,
                List<Object> inputDependencies)
        {
            propertyId_ = propertyId;
            nodePath_ = nodePath;
            dependencyPaths_ = dependencyPaths;
            evolver_ = evolver;
            inputDependencies_ = inputDependencies;
        }

        public Object getPropertyId()
        {
            return propertyId_;
        }

        public List<Object> getNodePath()
        {
            return nodePath_;
        }

        public Collection<List<Object>> getDependencyPaths()
        {
            return dependencyPaths_;
        }

        public Function<Map<Object, Object>, Object> getEvolver()
        {
            return evolver_;
        }

        public List<Object> getInputDependencies()
        {
            return inputDependencies_;
        }
    }

    public interface IContainerParser
    {
        Object getComponentId(Map<Object, Object> container);

        Map<Object, Map<Object, Object>> getChildren(Map<Object, Object> container);

        Collection<SourceNode> processComponent(
                List<Object> componentPath,
                Map<Object, Object> component,
                List<Object> propertyValueVec,
                Function<List<Object>, Integer> indexProvider);

        /**
         * @param inputDependencies
         * @param evolveReason
         * @return true if given inputDependencies list explicitly declares dependency on given evolveReason,
         *         or if it is not known; false only if it is known that given inputDependencies does NOT
         *         depend on given evolveReason
         */
        boolean isInterestedIn(Collection<Object> inputDependencies, Object evolveReason);
    }

    private static class ComponentAccessor implements Map
    {
        private final List<Object> componentPath_;
        private final Map<Object, Integer> propertyIdToIndex_;
        private final List<Object> values_;

        public ComponentAccessor(List<Object> componentPath, List<Object> values)
        {
            componentPath_ = componentPath;
            propertyIdToIndex_ = new HashMap<>();
            values_ = values;
        }

        @Override
        public Object get(Object key)
        {
            Integer index = propertyIdToIndex_.get(key);
            return index != null ? values_.get(index.intValue()) : null;
        }

        @Override
        public int size()
        {
            return propertyIdToIndex_.size();
        }

        @Override
        public boolean isEmpty()
        {
            return propertyIdToIndex_.isEmpty();
        }

        @Override
        public boolean containsKey(Object key)
        {
            return propertyIdToIndex_.containsKey(key);
        }

        @Override
        public boolean containsValue(Object value)
        {
            for (Object key : propertyIdToIndex_.keySet())
            {
                if (Objects.equals(get(key), value))
                {
                    return true;
                }
            }
            return false;
        }

        @Override
        public Object put(Object key, Object value)
        {
            throw new UnsupportedOperationException();
        }

        @Override
        public Object remove(Object key)
        {
            throw new UnsupportedOperationException();
        }

        @Override
        public void putAll(Map m)
        {
            throw new UnsupportedOperationException();
        }

        @Override
        public void clear()
        {
            throw new UnsupportedOperationException();
        }

        @Override
        public Set keySet()
        {
            return propertyIdToIndex_.keySet();
        }

        @Override
        public Collection values()
        {
            throw new UnsupportedOperationException();
        }

        @Override
        public Set<Entry> entrySet()
        {
            throw new UnsupportedOperationException();
        }

        void putPropertyIndex(Object key, Integer index)
        {
            propertyIdToIndex_.put(key, index);
        }

        List<Object> getComponentPath()
        {
            return componentPath_;
        }

        public Map<Object, Integer> getPropertyIdToIndex()
        {
            return propertyIdToIndex_;
        }
    }

    /**
     * Represents a property of a component (internal indexed)
     */
    private static class Node
    {
        private final Integer componentUid_;
        private final Object propertyId_;
        private final List<Object> nodePath_;
        private final Integer nodeUid_;
        private final Collection<List<Object>> dependencyPaths_;
        private final Collection<Object> inputDependencies_;
        private Collection<Integer> dependencyIndices_;
        private final Function<Map<Object, Object>, Object> evolver_;

        // TODO Optimize:
        // remove dependents covered by longer chains. Maybe not remove but just hide since longer chains may be provided
        // by components that may be removed
        private final Collection<Integer> dependentIndices_;

        public Node(
                Integer componentUid,
                Object propertyId,
                List<Object> nodePath,
                Integer nodeUid,
                Collection<List<Object>> dependencyPaths,
                Collection<Object> inputDependencies,
                Function<Map<Object, Object>, Object> evolver)
        {
            componentUid_ = componentUid;
            propertyId_ = propertyId;
            nodePath_ = nodePath;
            nodeUid_ = nodeUid;
            dependencyPaths_ = dependencyPaths;
            inputDependencies_ = inputDependencies;
            dependentIndices_ = new HashSet<>();
            evolver_ = evolver;
        }

        public Integer getComponentUid()
        {
            return componentUid_;
        }

        public Object getPropertyId()
        {
            return propertyId_;
        }

        public Integer getNodeIndex()
        {
            return nodeUid_;
        }

        public List<Object> getNodePath()
        {
            return nodePath_;
        }

        public void resolveDependencyIndices(Function<List<Object>, Integer> pathIndexProvider)
        {
            dependencyIndices_ = Collections.unmodifiableSet(dependencyPaths_.stream().map(pathIndexProvider).collect(Collectors.toSet()));
        }

        public Collection<Integer> getDependencyIndices()
        {
            return dependencyIndices_;
        }

        public Collection<Integer> getDependentIndices()
        {
            return dependentIndices_;
        }

        public void addDependent(Integer nodeIndex)
        {
            dependentIndices_.add(nodeIndex);
        }

        public Function<Map<Object, Object>, Object> getEvolver()
        {
            return evolver_;
        }

//        public Object getValue()
//        {
//            return value_;
//        }
//
//        @Override
//        public String toString()
//        {
//            return nodePath_ + "=" + value_;
//        }
    }
}
