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

import java.lang.reflect.Array;
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
    private final List<Integer> naturalComponentOrder_;

    private final List<Node> nodes_;
    private final List<Object> values_;
    private final Map<List<Object>, Integer> pathToIndex_;

    private final IContainerAccessor containerAccessor_;
    private final IContainerMutator containerMutator_;

    private final IContainerParser containerParser_;

    private Node[] reusableNodeBuffer_;
    private Object[] reusableReasonBuffer_;
    private int indexBufferSize_;

    private Set<Integer> initializedNodes_;

    private static boolean debug_ = true;

    public Container(IContainerParser containerParser, IResultCollector resultCollector, Map<Object, Object> container)
    {
        containerParser_ = containerParser;
        resultCollector_ = resultCollector;
        components_ = new ArrayList<>();
        naturalComponentOrder_ = new ArrayList<>();
        componentPathToIndex_ = new HashMap<>();
        vacantComponentIndices_ = new HashSet<>();
        vacantNodeIndices_ = new HashSet<>();
        nodes_ = new ArrayList<>();
        values_ = new ArrayList<>();
        pathToIndex_ = new HashMap<>();

        reusableNodeBuffer_ = new Node[128];
        reusableReasonBuffer_ = new Object[128];
        indexBufferSize_ = 0;

        containerAccessor_ = components_::get;
        containerMutator_ = (nodeIndex, newValue) -> values_.set(nodeIndex, newValue);

        addContainer(new ArrayList<>(), container);
        finishContainerIndexing();

        initializedNodes_ = new HashSet<>();
        log("========================Started initialization cycle================================");
        initializeContainer();
        log("=====Ended initialization cycle");
        initializedNodes_.clear();
        initializedNodes_ = null;
    }

    public Integer addComponent(List<Object> componentPath, ComponentAccessor component)
    {
        return addComponent(componentPath, component, false);
    }

    public int indexOfPath(List<Object> path)
    {
        return pathToIndex_.get(path);
    }

    public void evolve(List<Object> targetPath, Object evolveReason)
    {
        Integer componentUid = getComponentUid(targetPath);
        if (componentUid == null)
        {
            throw new IllegalArgumentException("Component path does not exist: " + targetPath);
        }
        evolve(componentUid, evolveReason);
    }

    public void evolve(Integer componentUid, Object evolveReason)
    {
        indexBufferSize_ = 0;

        log("----------------Started evolve cycle ---- for reason: " + valueToString(evolveReason));

        ComponentAccessor initialComponentAccessor = components_.get(componentUid);
        Map<Object, Integer> propertyIdToNodeIndex = initialComponentAccessor.getPropertyIdToIndex();
        for (Object propertyId : propertyIdToNodeIndex.keySet())
        {
            Integer nodeIndex = propertyIdToNodeIndex.get(propertyId);
            Node node = nodes_.get(nodeIndex);
            if (evolveReason == null ||
                    (node.getEvolver() != null && containerParser_.isInterestedIn(node.getInputDependencies(), evolveReason)))
            {
                addNodeToReusableBuffer(node, evolveReason);
            }
        }

        for (int i=0; i<indexBufferSize_; i++)
        {
            log(" Initial component: " + reusableNodeBuffer_[i].getNodePath());
        }

        int currentIndex = 0;
        while (currentIndex < indexBufferSize_)
        {
            Node node = reusableNodeBuffer_[currentIndex];
            Object triggeringReason = reusableReasonBuffer_[currentIndex];
            int nodeIndex = node.getNodeIndex().intValue();
            Function<Map<Object, Object>, Object> evolver = node.getEvolver();

            if (triggeringReason != null || !initializedNodes_.contains(nodeIndex))
            {
                Object oldValue;
                Object newValue;
                ComponentAccessor component = components_.get(node.getComponentUid());
                if (triggeringReason == null)
                {
                    oldValue = null;
                    newValue = values_.get(nodeIndex);
                }
                else
                {
                    component.setEvolveReason(triggeringReason);
                    oldValue = values_.get(nodeIndex);
                    try
                    {
                        newValue = evolver.apply(component);
                    }
                    catch (Exception ex)
                    {
                        log(" Error evolving " + node.getNodePath() + " " + node.getPropertyId() +
                                " for reason: " + triggeringReason);
                        throw ex;
                    }
                }

                if (!Objects.equals(oldValue, newValue))
                {
                    log(" Evolved: " + nodeIndex + " " + node.getNodePath() + " for reason: " + valueToString(triggeringReason) +
                            ": " + valueToString(oldValue) + " -> " + valueToString(newValue));

                    containerMutator_.setValue(nodeIndex, newValue);

                    resultCollector_.appendResult(component.getComponentPath(), componentUid, node.getPropertyId(), newValue);

                    addNodeDependentsToEvolvebuffer(node);
                }
                else
                {
                    log(" Evolved: " + nodeIndex + " " + node.getNodePath() + " for reason: " + valueToString(triggeringReason) +
                            ": no change (" + oldValue + ").");
                }

                if (initializedNodes_ != null)
                {
                    initializedNodes_.add(nodeIndex);
                }
            }

            currentIndex++;
        }

        resultCollector_.postProcessAfterEvolveCycle(containerAccessor_, containerMutator_);

        log("---Ended evolve cycle");
    }

    public IContainerAccessor getContainerAccessor()
    {
        return containerAccessor_;
    }

    public Iterable<Integer> getComponentNaturalOrder()
    {
        return naturalComponentOrder_;
    }

    public Integer getComponentUid(List<Object> componentPath)
    {
        return componentPathToIndex_.get(componentPath);
    }

    public IComponent getRootComponent()
    {
        return components_.get(0);
    }

    public IComponent getComponent(Integer componentUid)
    {
        return components_.get(componentUid.intValue());
    }

    public <V> V getPropertyValue(Integer index)
    {
        return (V) values_.get(index.intValue());
    }

    // Private

    private Integer addComponent(List<Object> componentPath, ComponentAccessor component, boolean needToResolveNaturalOrder)
    {
        Integer index;
        if (vacantComponentIndices_.isEmpty())
        {
            index = components_.size();
            components_.add(index, component);
        }
        else
        {
            index = vacantComponentIndices_.stream().findAny().get();
            vacantComponentIndices_.remove(index);
            components_.set(index, component);
        }
        componentPathToIndex_.put(componentPath, index);

        if (needToResolveNaturalOrder)
        {
            // TODO
        }
        else
        {
            naturalComponentOrder_.add(index);
        }

        return index;
    }

    private Integer addContainer(List<Object> pathToContainer, Map<Object, Object> container)
    {
        // Add and thus index all components/properties

        List<Object> componentPath = new ArrayList<>(pathToContainer.size()+1);
        componentPath.addAll(pathToContainer);
        componentPath.add(containerParser_.getComponentId(container));

        ComponentAccessor component = new ComponentAccessor(componentPath, values_);
        Integer componentUid = addComponent(componentPath, component);
        Collection<SourceNode> componentPropertyNodes = containerParser_.processComponent(
                componentPath, container);
        for (SourceNode node : componentPropertyNodes)
        {
            Integer nodeIndex = addNode(componentUid, node, container.get(node.getPropertyId()));
            log("Indexing " + componentPath + " node " + node.getNodePath() + ": " + nodeIndex);
            component.putPropertyIndex(node.getPropertyId(), nodeIndex);
        }

        containerParser_.processComponentAfterIndexing(component);

        Map<Object, Map<Object, Object>> children = containerParser_.getChildren(container);
        if (children != null)
        {
            Collection<Integer> childIndices = new HashSet<>(children.size());
            for (Map<Object, Object> child : children.values())
            {
                Integer childIndex = addContainer(componentPath, child);
                childIndices.add(childIndex);
            }
            component.setChildIndices(childIndices);
        }

        return componentUid;
    }

    private void finishContainerIndexing()
    {
        // Now that all components/properties are indexed, compile evolvers

        nodes_.forEach(n -> n.setEvolver(n.getEvolverCode() != null ? containerParser_.compileEvolverCode(
                n.getEvolverCode(), pathToIndex_::get) : null));

        // and resolve dependency indices for each property

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
        Node node = new Node(
                componentUid,
                sourceNode.getPropertyId(),
                sourceNode.getNodePath(),
                index,
                sourceNode.getDependencyPaths(),
                sourceNode.getInputDependencies(),
                sourceNode.getEvolverCode());
        int indexInt = index.intValue();
        if (index < nodes_.size())
        {
            nodes_.set(indexInt, node);
            values_.set(indexInt, initialValue);
        }
        else
        {
            nodes_.add(node);
            values_.add(initialValue);
        }

        pathToIndex_.put(sourceNode.getNodePath(), index);
        return index;
    }

    private void addNodeToReusableBuffer(Node node, Object evolveReason)
    {
        ensureIndexBufferSize(indexBufferSize_ + 1);
        reusableNodeBuffer_[indexBufferSize_] = node;
        reusableReasonBuffer_[indexBufferSize_] = evolveReason;
        indexBufferSize_ ++;
    }

    private void addNodeDependentsToEvolvebuffer(Node node)
    {
        Collection<Integer> dependents = node.getDependentIndices();
        int dependentCollSize = dependents.size();
        ensureIndexBufferSize(indexBufferSize_ + dependentCollSize);
        for (Integer i : dependents)
        {
            Node dependent = nodes_.get(i.intValue());
            reusableNodeBuffer_[indexBufferSize_] = dependent;
            reusableReasonBuffer_[indexBufferSize_] = node.getNodePath();

            log("    Triggered dependent: " + dependent.getNodePath());

            indexBufferSize_ ++;
        }
    }
//
//    private void computeChildIndices(ComponentAccessor container)
//    {
//        Collection<Integer> childIndices = new HashSet<>();
//
//        List<Object> containerPath = container.getComponentPath();
//        Map<Object, Map<Object, Object>> children = containerParser_.getChildren(container);
//        for (Object childId : children.keySet())
//        {
//            List<Object> childPath = new ArrayList<>(containerPath.size()+1);
//            childPath.addAll(containerPath);
//            childPath.add(childId);
//
//            Integer childComponentUid = componentPathToIndex_.get(childPath);
//            if (childComponentUid == null)
//            {
//                throw new IllegalStateException("Child component path does not exist: " + childPath);
//            }
//
//            childIndices.add(childComponentUid);
//        }
//    }

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
            T[] newBuffer = (T[]) Array.newInstance(oldBuffer.getClass().getComponentType(), oldBuffer.length + 128);
            System.arraycopy(oldBuffer, 0, newBuffer, 0, oldBuffer.length);
            return newBuffer;
        }
        return oldBuffer;
    }

    private void initializeContainer()
    {
        for (int i=0; i<components_.size(); i++)
        {
            evolve(Integer.valueOf(i), null);
        }
    }

    static void log(String message)
    {
        if (debug_)
        {
            System.out.println("[FG Eng]: " + message);
        }
    }

    static String valueToString(Object v)
    {
        if (v != null)
        {
            String s = v.toString();
            if (s.length() > 30)
            {
                return s.substring(0, 30) + "...";
            }
            else
            {
                return s;
            }
        }
        else
        {
            return null;
        }
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

        private final Object evolverCode_;

        private final List<Object> inputDependencies_;

        public SourceNode(
                Object propertyId,
                List<Object> nodePath,
                Collection<List<Object>> dependencyPaths,
                Object evolverCode,
                List<Object> inputDependencies)
        {
            propertyId_ = propertyId;
            nodePath_ = nodePath;
            dependencyPaths_ = dependencyPaths;
            evolverCode_ = evolverCode;
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

        public Object getEvolverCode()
        {
            return evolverCode_;
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
                Map<Object, Object> component);

        void processComponentAfterIndexing(IComponent component);

        Function<Map<Object, Object>, Object> compileEvolverCode(Object evolverCode,
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

    public interface IComponent extends Map<Object, Object>
    {
        Integer getPropertyIndex(Object key);

        Iterable<Integer> getChildIndices();

        Object getCustomData();

        void setCustomData(Object data);
    }

    public interface IContainerAccessor
    {
        IComponent getComponent(int componentUid);
    }

    public interface IContainerMutator
    {
        void setValue(int nodeIndex, Object newValue);
    }

    public static class ComponentAccessor implements IComponent
    {
        private final List<Object> componentPath_;
        private Map<Object, Integer> propertyIdToIndex_;
        private final List<Object> values_;
        private Collection<Integer> childIndices_;

        private Object currentEvolveReason_;

        private Object customData_;

        public ComponentAccessor(List<Object> componentPath, List<Object> values)
        {
            componentPath_ = Collections.unmodifiableList(componentPath);
            propertyIdToIndex_ = new HashMap<>();
            values_ = Collections.unmodifiableList(values);
        }

        @Override
        public Object get(Object key)
        {
            Integer index = getPropertyIndex(key);
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
        public Set<Entry<Object, Object>> entrySet()
        {
            Map<Object, Object> propertyIdToVal = new HashMap<>();
            for (Object propId : propertyIdToIndex_.keySet())
            {
                propertyIdToVal.put(propId, get(propId));
            }
            return propertyIdToVal.entrySet();
        }

        @Override
        public Integer getPropertyIndex(Object key)
        {
            return propertyIdToIndex_.get(key);
        }

        @Override
        public Object getCustomData()
        {
            return customData_;
        }

        @Override
        public void setCustomData(Object customData)
        {
            customData_ = customData;
        }

        @Override
        public Iterable<Integer> getChildIndices()
        {
            return childIndices_;
        }

        public Map<Object, Integer> getPropertyIdToIndex()
        {
            return propertyIdToIndex_;
        }

        void putPropertyIndex(Object key, Integer index)
        {
            propertyIdToIndex_.put(key, index);
        }

        void finishInitialization()
        {
            propertyIdToIndex_ = Collections.unmodifiableMap(propertyIdToIndex_);
        }

        List<Object> getComponentPath()
        {
            return componentPath_;
        }

//        Collection<Integer> getChildIndices()
//        {
//            return childIndices_;
//        }

        void setChildIndices(Collection<Integer> childIndices)
        {
            childIndices_ = childIndices;
        }

        void setEvolveReason(Object reason)
        {
            currentEvolveReason_ = reason;
        }

        // Methods immediately available for evolvers to implement get-property and get-reason

        public Object getNodeValueByIndex(Integer index)
        {
            return values_.get(index);
        }

        public Object getEvolveReason()
        {
            return currentEvolveReason_;
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
        private Object evolverCode_;
        private Function<Map<Object, Object>, Object> evolver_;

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
                Object evolverCode)
        {
            componentUid_ = componentUid;
            propertyId_ = propertyId;
            nodePath_ = nodePath;
            nodeUid_ = nodeUid;
            dependencyPaths_ = dependencyPaths;
            inputDependencies_ = inputDependencies;
            dependentIndices_ = new HashSet<>();
            evolverCode_ = evolverCode;
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

        public Collection<Object> getInputDependencies()
        {
            return inputDependencies_;
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

        public Object getEvolverCode()
        {
            return evolverCode_;
        }

        public Function<Map<Object, Object>, Object> getEvolver()
        {
            return evolver_;
        }

        public void setEvolver(Function<Map<Object, Object>, Object> evolver)
        {
            evolver_ = evolver;
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
