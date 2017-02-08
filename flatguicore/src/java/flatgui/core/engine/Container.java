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

import clojure.lang.PersistentHashMap;
import clojure.lang.PersistentVector;
import flatgui.core.util.Tuple;

import java.lang.reflect.Array;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

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
    private final List<Node> nodesWithAmbiguousDependencies_;
    private final List<Object> values_;
    private final Map<List<Object>, Integer> pathToIndex_;

    private final IContainerAccessor containerAccessor_;
    private final IPropertyValueAccessor propertyValueAccessor_;
    private final IContainerMutator containerMutator_;

    private final IContainerParser containerParser_;

    private Node[] reusableNodeBuffer_;
    private Object[] reusableReasonBuffer_;
    private int indexBufferSize_;

    private Set<Integer> initializedNodes_;

    public static boolean debug_ = false;

    public Container(IContainerParser containerParser, IResultCollector resultCollector, Map<Object, Object> container)
    {
        containerParser_ = containerParser;
        resultCollector_ = resultCollector;
        components_ = new ArrayList<>();
        componentPathToIndex_ = new HashMap<>();
        vacantComponentIndices_ = new HashSet<>();
        vacantNodeIndices_ = new HashSet<>();
        nodes_ = new ArrayList<>();
        nodesWithAmbiguousDependencies_ = new ArrayList<>();
        values_ = new ArrayList<>();
        pathToIndex_ = new HashMap<>();

        reusableNodeBuffer_ = new Node[1048576];
        reusableReasonBuffer_ = new Object[1048576];
        indexBufferSize_ = 0;

        containerAccessor_ = components_::get;
        propertyValueAccessor_ = this::getPropertyValue;
        containerMutator_ = (nodeIndex, newValue) -> values_.set(nodeIndex, newValue);

        addContainer(null, new ArrayList<>(), container);
        finishContainerIndexing();

        initializeContainer();
    }

    public Integer addComponent(List<Object> componentPath, ComponentAccessor component)
    {
        return addComponentImpl(componentPath, component);
    }

    public Integer indexOfPath(List<Object> path)
    {
        return pathToIndex_.get(path);
    }

    public Integer indexOfPathStrict(List<Object> path)
    {
        Integer i = pathToIndex_.get(path);
//        if (i == null)
//        {
//            throw new IllegalArgumentException("No index for path: " + path);
//        }
        return i;
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
        long evolveStartTime = System.currentTimeMillis();
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

        Set<Integer> addedComponentIds = new HashSet<>();
        int currentIndex = 0;
        while (currentIndex < indexBufferSize_)
        {
            Node node = reusableNodeBuffer_[currentIndex];
            Object triggeringReason = reusableReasonBuffer_[currentIndex];
            int nodeIndex = node.getNodeIndex().intValue();
            Function<Map<Object, Object>, Object> evolver = node.getEvolver();

            if (triggeringReason != null || initializedNodes_ != null && !initializedNodes_.contains(nodeIndex))
            {
                Object oldValue;
                Object newValue;
                ComponentAccessor component = components_.get(node.getComponentUid());
                if (triggeringReason == null)
                {
                    if (evolver == null)
                    {
                        oldValue = null;
                        newValue = values_.get(nodeIndex);
                    }
                    else
                    {
                        component.setEvolveReason(null);
                        oldValue = values_.get(nodeIndex);
                        try
                        {
                            newValue = evolver.apply(component);
                        }
                        catch (Exception ex)
                        {
                            log(" Error evolving " + node.getNodePath() + " " + node.getPropertyId() + " while initializing ");
                            ex.printStackTrace();
                            throw ex;
                        }
                    }
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
                        log(" Error evolving " + node.getNodePath() + " " + node.getPropertyId() + " for reason: " + triggeringReason);
                        ex.printStackTrace();
                        throw ex;
                    }
                }

                boolean changeDetected = initializedNodes_ != null && !initializedNodes_.contains(nodeIndex);
                Set<Object> removedChildIds = null;
                Set<Object> changedChildIds = null;
                Set<Object> addedChildIds = null;
                if (node.isChildrenProperty())
                {
                    removedChildIds = new HashSet<>();
                    changedChildIds = new HashSet<>();
                    addedChildIds = new HashSet<>();

                    Map<Object, Map<Object, Object>> newValueMap = (Map<Object, Map<Object, Object>>)newValue;
                    if (newValueMap == null)
                    {
                        newValueMap = Collections.emptyMap();
                    }
                    newValue = new HashMap<>();
                    for (Object cid : newValueMap.keySet())
                    {
                        // TODO(f) backward compatibility. Non-children maps are there by keys:
                        // :_flexible-childset-added
                        // :_flex-target-id-paths-added
                        if (!cid.toString().contains("_flex"))
                        {
                            ((Map<Object, Map<Object, Object>>) newValue).put(cid, newValueMap.get(cid));
                        }
                    }
                    newValue = PersistentHashMap.create((Map)newValue);

                    Map<Object, Map<Object, Object>> children = (Map<Object, Map<Object, Object>>) oldValue;
                    Map<Object, Map<Object, Object>> oldChildren = children != null ? children : Collections.emptyMap();
                    Set<Object> allIds = new HashSet<>();
                    allIds.addAll(oldChildren.keySet());
                    allIds.addAll(((Map)newValue).keySet());
                    for (Object id : allIds)
                    {
                        if (!oldChildren.containsKey(id))
                        {
                            addedChildIds.add(id);
                        }
                        else if (!((Map)newValue).containsKey(id))
                        {
                            removedChildIds.add(id);
                        }
                        else if (!oldChildren.get(id).equals(((Map)newValue).get(id)))
                        {
                            changedChildIds.add(id);
                        }
                    }
                    if (!changeDetected)
                    {
                        changeDetected = addedChildIds.size() > 0 || removedChildIds.size() > 0 || changedChildIds.size() > 0;
                    }
                }
                else
                {
                    try
                    {
                        if (!changeDetected)
                        {
                            changeDetected = !Objects.equals(oldValue, newValue);
                        }
                    }
                    catch (Exception ex)
                    {
                        throw ex;
                    }
                }

                if (changeDetected)
                {
                    log(" Evolved: " + nodeIndex + " " + node.getNodePath() + " for reason: " + valueToString(triggeringReason) + ": " + valueToString(oldValue) + " -> " + valueToString(newValue));
                    containerMutator_.setValue(nodeIndex, newValue);

                    List<Object> componentPath = component.getComponentPath();
                    resultCollector_.appendResult(node.getParentComponentUid(), componentPath, node, newValue);

                    addNodeDependentsToEvolvebuffer(node);

                    if (triggeringReason != null && node.isChildrenProperty())
                    {
                        log(" Detected children change");
                        Map<Object, Map<Object, Object>> newChildren = (Map<Object, Map<Object, Object>>) newValue;

                        Set<Object> idsToRemove = new HashSet<>(removedChildIds);
                        idsToRemove.addAll(changedChildIds);
                        Set<Object> idsToAdd = new HashSet<>(addedChildIds);
                        idsToAdd.addAll(changedChildIds);

                        log(" Removing " + removedChildIds.size() + " removed and " + changedChildIds.size() + " changed children...");
                        for (Object id : idsToRemove)
                        {
                            List<Object> childPath = new ArrayList<>(componentPath.size()+1);
                            childPath.addAll(componentPath);
                            childPath.add(id);
                            Integer removedChildIndex = getComponentUid(childPath);
                            removeComponent(removedChildIndex);
                        }

                        log(" Adding " + changedChildIds.size() + " changed and " + addedChildIds.size() + " added children...");
                        Set<Integer> newChildIndices = new HashSet<>(idsToAdd.size());
                        Map<Object, Integer> newChildIdToIndex = new HashMap<>();

                        idsToAdd
                                .forEach(childId -> {
                                    Map<Object, Object> child = newChildren.get(childId);
                                    Integer index = addContainer(node.getComponentUid(), component.getComponentPath(), child);
                                    addedComponentIds.add(index);
                                    newChildIndices.add(index);
                                    newChildIdToIndex.put(childId, index);
                                });

                        component.addChildIndices(newChildIndices, newChildIdToIndex);
                    }
                    if (node.isChildOrderProperty() && newValue != null)
                    {
                        List<Object> newChildIdOrder = (List<Object>) newValue;

                        List<Integer> newChildIndices = new ArrayList<>(newChildIdOrder.size());
                        for (int i=0; i<newChildIdOrder.size(); i++)
                        {
                            List<Object> childPath = new ArrayList<>(componentPath.size()+1);
                            childPath.addAll(componentPath);
                            childPath.add(newChildIdOrder.get(i));
                            newChildIndices.add(getComponentUid(childPath));
                        }
                        component.changeChildIndicesOrder(newChildIndices);
                    }
                }
                else
                {
                    log(" Evolved: " + nodeIndex + " " + node.getNodePath() + " for reason: " + valueToString(triggeringReason) + ": no change (" + oldValue + ").");
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

        if (!addedComponentIds.isEmpty())
        {
            processAllNodesOfComponents(addedComponentIds, this::setupEvolversForNode);
            processAllNodesOfComponents(addedComponentIds, this::resolveDependencyIndicesForNode);

            processAllNodesOfComponents(addedComponentIds, this::markNodeAsDependent);

            for (Node n : nodesWithAmbiguousDependencies_)
            {
                Collection<Tuple> newDependencies = n.reevaluateAmbiguousDependencies(components_, containerParser_::isWildcardPathElement);
                markNodeAsDependent(n, newDependencies);
            }

            boolean newNodeSet = initializedNodes_ == null;
            if (newNodeSet)
            {
                initializedNodes_ = new HashSet<>();
            }
            addedComponentIds.forEach(this::initializeAddedComponent);
            if (newNodeSet)
            {
                initializedNodes_.clear();
                initializedNodes_ = null;
            }
        }

        long spentEvolving = System.currentTimeMillis() - evolveStartTime;
        //System.out.println("-DLTEMP- Container.evolve spent evolving " + spentEvolving);
    }

    public IContainerAccessor getContainerAccessor()
    {
        return containerAccessor_;
    }

    public IPropertyValueAccessor getPropertyValueAccessor()
    {
        return propertyValueAccessor_;
    }

    public final Integer getComponentUid(List<Object> componentPath)
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

    public Node getNode(Integer nodeId)
    {
        return nodes_.get(nodeId);
    }

    public int getNodeCount()
    {
        return nodes_.size();
    }

    public <V> V getPropertyValue(Integer index)
    {
        //TODO see "strict" return (V) values_.get(index.intValue());
        return index != null ? (V) values_.get(index.intValue()) : null;
    }

    public IResultCollector getResultCollector()
    {
        return resultCollector_;
    }

    public Collection<List<Object>> getAllIdPaths()
    {
        return Collections.unmodifiableSet(pathToIndex_.keySet());
    }

    // Private

    private Integer addComponentImpl(List<Object> componentPath, ComponentAccessor component)
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

        resultCollector_.componentAdded(index);

        return index;
    }

    private void removeComponent(Integer componentUid)
    {
        if (componentUid.intValue() >= components_.size())
        {
            throw new IllegalArgumentException("Component does not exist: " + componentUid);
        }
        ComponentAccessor c = components_.get(componentUid.intValue());
        if (c == null)
        {
            throw new IllegalArgumentException("Component already removed: " + componentUid);
        }

        Collection<Integer> propertyIndices = c.getPropertyIndices();
        propertyIndices.forEach(i -> {
            vacantNodeIndices_.add(i);
            Node node = nodes_.set(i.intValue(), null);
            nodesWithAmbiguousDependencies_.remove(node);//TODO there should be indices, not nodes
            values_.set(i.intValue(), null);
            pathToIndex_.remove(node.getNodePath());
            unMarkNodeAsDependent(node, node.getDependencyIndices());
            for (Integer dependentIndex : node.getDependentIndices().keySet())
            {
                Node dependentNode = nodes_.get(dependentIndex.intValue());
                if (dependentNode != null)
                {
                    dependentNode.forgetDependency(i);
                }
            }
            if (initializedNodes_ != null)
            {
                initializedNodes_.remove(i);
            }
        });

        components_.set(componentUid.intValue(), null);
        componentPathToIndex_.remove(c.getComponentPath());

        vacantComponentIndices_.add(componentUid);
        resultCollector_.componentRemoved(componentUid);

        List<Integer> children = c.getChildIndices();
        if (children != null)
        {
            children.forEach(this::removeComponent);
        }
    }

    private Integer addContainer(Integer parentComponentUid, List<Object> pathToContainer, Map<Object, Object> container)
    {
        // Add and thus index all components/properties

        List<Object> componentPath = new ArrayList<>(pathToContainer.size()+1);
        componentPath.addAll(pathToContainer);
        componentPath.add(containerParser_.getComponentId(container));

        ComponentAccessor component = new ComponentAccessor(
                componentPath, values_, path -> getPropertyValue(indexOfPathStrict(path)));
        Integer componentUid = addComponent(componentPath, component);
        log("Added and indexed component " + componentPath + ": " + componentUid);
        Collection<SourceNode> componentPropertyNodes = containerParser_.processComponent(
                componentPath, container, propertyValueAccessor_);
        for (SourceNode node : componentPropertyNodes)
        {
            Integer nodeIndex = addNode(parentComponentUid, componentUid, node, container.get(node.getPropertyId()));
            log("Indexing " + componentPath + " node " + node.getNodePath() + ": " + nodeIndex);
            component.putPropertyIndex(node.getPropertyId(), nodeIndex);
        }

        containerParser_.processComponentAfterIndexing(component);

        Map<Object, Map<Object, Object>> children = (Map<Object, Map<Object, Object>>) container.get(containerParser_.getChildrenPropertyName());
        if (children != null)
        {
            List<Integer> childIndices = new ArrayList<>(children.size());
            Map<Object, Integer> childIdToIndex = new HashMap<>();
            for (Map<Object, Object> child : children.values())
            {
                Object childId = containerParser_.getComponentId(child);
                Integer childIndex = addContainer(componentUid, componentPath, child);
                childIndices.add(childIndex);
                childIdToIndex.put(childId, childIndex);
            }
            component.setChildIndices(childIndices, childIdToIndex);
        }

        return componentUid;
    }

    private void processAllNodesOfComponents(Collection<Integer> addedComponentIds, Consumer<Node> nodeProcessor)
    {
        for (Integer uid : addedComponentIds)
        {
            ComponentAccessor addedComponentAccessor = components_.get(uid);
            Map<Object, Integer> addedComponentPropertyIdToNodeIndex = addedComponentAccessor.getPropertyIdToIndex();
            for (Object propertyId : addedComponentPropertyIdToNodeIndex.keySet())
            {
                Integer nodeIndex = addedComponentPropertyIdToNodeIndex.get(propertyId);
                Node node = nodes_.get(nodeIndex);
                nodeProcessor.accept(node);
            }
        }
    }

    private void initializeAddedComponent(Integer componentUid)
    {
        evolve(componentUid, null);
        //getResultCollector().componentInitialized(this, componentUid); TODO is looks like we are good without this
        ComponentAccessor component = components_.get(componentUid);
        Iterable<Integer> childIndices = component.getChildIndices();
        if (childIndices != null)
        {
            for (Integer childIndex : childIndices)
            {
                initializeAddedComponent(childIndex);
            }
        }
    }

    private static List<Object> dropLast(List<Object> path)
    {
        List<Object> list = new ArrayList<>(path);
        list.remove(list.size()-1);
        return list;
    }

    private void setupEvolversForNode(Container.Node n)
    {
        n.setEvolver(n.getEvolverCode() != null ? containerParser_.compileEvolverCode(
                n.getPropertyId(), n.getEvolverCode(), this::indexOfPath, dropLast(n.getNodePath()), propertyValueAccessor_) : null);
    }

    private void resolveDependencyIndicesForNode(Container.Node n)
    {
        n.resolveDependencyIndices(components_, containerParser_::isWildcardPathElement);
    }

    private void markNodeAsDependent(Container.Node n)
    {
        markNodeAsDependent(n, n.getDependencyIndices());
    }

    private void markNodeAsDependent(Container.Node n, Collection<Tuple> dependencies)
    {
        dependencies
            .forEach(dependencyTuple -> nodes_.get(dependencyTuple.getFirst()).addDependent(n.getNodeIndex(), n.getNodePath(), dependencyTuple.getSecond()));
    }

    private void unMarkNodeAsDependent(Container.Node n, Collection<Tuple> dependencies)
    {
        dependencies
                .forEach(dependencyTuple -> {
                    // May be null - if node belonged to the component being removed
                    Node dn = nodes_.get(dependencyTuple.getFirst());
                    if (dn != null)
                    {
                        dn.removeDependent(n.getNodeIndex());
                    }
                });
    }

    private void finishContainerIndexing()
    {
        nodes_.forEach(this::setupEvolversForNode);

        // and resolve dependency indices for each property

        nodes_.forEach(this::resolveDependencyIndicesForNode);

        // For each component N, take its dependencies and mark that components that they have N as a dependent

        nodes_.forEach(this::markNodeAsDependent);

        // TODO Optimize:
        // remove dependents covered by longer chains. Maybe not remove but just hide since longer chains may be provided
        // by components that may be removed
    }

    private Integer addNode(Integer parentComponentUid, Integer componentUid, SourceNode sourceNode, Object initialValue)
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
                parentComponentUid,
                sourceNode.isChildrenProperty(),
                sourceNode.isChildOrderProperty(),
                sourceNode.getNodePath(),
                index,
                sourceNode.getRelAndAbsDependencyPaths(),
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
            if (node.isHasAmbiguousDependencies())
            {
                nodesWithAmbiguousDependencies_.add(node);
            }
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
        Map<Integer, List<Object>> dependents = node.getDependentIndices();
        int dependentCollSize = dependents.size();
        ensureIndexBufferSize(indexBufferSize_ + dependentCollSize);
        for (Integer i : dependents.keySet())
        {
            Node dependent = nodes_.get(i.intValue());
            List<Object> invokerRefRelPath = new ArrayList<>(dependents.get(i));
            // By convention, do not include property into what (get-reason) returns
            invokerRefRelPath.remove(invokerRefRelPath.size()-1);

            // TODO delegate reference to Clojure to parser
            invokerRefRelPath = PersistentVector.create(invokerRefRelPath);

            reusableNodeBuffer_[indexBufferSize_] = dependent;
            reusableReasonBuffer_[indexBufferSize_] = invokerRefRelPath;

            log("    Triggered dependent: " + dependent.getNodePath() + " referenced as " + invokerRefRelPath);

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
            T[] newBuffer = (T[]) Array.newInstance(oldBuffer.getClass().getComponentType(), oldBuffer.length + 128);
            System.arraycopy(oldBuffer, 0, newBuffer, 0, oldBuffer.length);
            return newBuffer;
        }
        return oldBuffer;
    }

    private void initializeContainer()
    {
        initializedNodes_ = new HashSet<>();
        log("========================Started initialization cycle================================");
        for (int i=0; i<components_.size(); i++)
        {
            evolve(Integer.valueOf(i), null);
        }
        log("=====Ended initialization cycle");
        initializedNodes_.clear();
        initializedNodes_ = null;
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
            if (s.length() > 100)
            {
                return s.substring(0, 100) + "...";
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

    private boolean pathMatches(List<Object> path, List<Object> mapKey)
    {
        if (path.size() == mapKey.size())
        {
            for (int i=0; i<path.size(); i++)
            {
                Object e = path.get(i);
                if (!(e.equals(mapKey.get(i)) || containerParser_.isWildcardPathElement(e) /* TODO(IND) !(path.get(i) instanceof Keyword)*/))
                {
                    return false;
                }
            }
            return true;
        }
        else
        {
            return false;
        }
    }


    // // //

    public static class DependencyInfo
    {
        private final List<Object> relPath_;

        private final List<Object> absPath_;

        /**
         * true if contains one or more wildcards (:*)
         */
        private boolean isAmbiguous_;

        public DependencyInfo(List<Object> relPath, List<Object> absPath, boolean isAmbiguous)
        {
            relPath_ = relPath;
            absPath_ = absPath;
            isAmbiguous_ = isAmbiguous;
        }

        public List<Object> getRelPath()
        {
            return relPath_;
        }

        public List<Object> getAbsPath()
        {
            return absPath_;
        }

        public boolean isAmbiguous()
        {
            return isAmbiguous_;
        }
    }


    /**
     * Represents a property of a component (internal indexed)
     */
    public static class SourceNode
    {
        // Path means: last element is property id, all elements before last represent component path

        private final Object propertyId_;

        private final boolean childrenProperty_;

        private final boolean childOrderProperty_;

        private final List<Object> nodePath_;

        private final Collection<DependencyInfo> relAndAbsDependencyPaths_;

        private final Object evolverCode_;

        private final List<Object> inputDependencies_;

        public SourceNode(
                Object propertyId,
                boolean childrenProperty,
                boolean childOrderProperty,
                List<Object> nodePath,
                Collection<DependencyInfo> relAndAbsDependencyPaths,
                Object evolverCode,
                List<Object> inputDependencies)
        {
            propertyId_ = propertyId;
            childrenProperty_ = childrenProperty;
            childOrderProperty_ = childOrderProperty;
            nodePath_ = nodePath;
            relAndAbsDependencyPaths_ = relAndAbsDependencyPaths;
            evolverCode_ = evolverCode;
            inputDependencies_ = inputDependencies;
        }

        public Object getPropertyId()
        {
            return propertyId_;
        }

        public boolean isChildrenProperty()
        {
            return childrenProperty_;
        }

        public boolean isChildOrderProperty()
        {
            return childOrderProperty_;
        }

        public List<Object> getNodePath()
        {
            return nodePath_;
        }

        public Collection<DependencyInfo> getRelAndAbsDependencyPaths()
        {
            return relAndAbsDependencyPaths_;
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

        Object getChildrenPropertyName();

        Object getChildOrderPropertyName();

        List<Object> getChildOrder(Map<Object, Object> container);

        Collection<SourceNode> processComponent(
                List<Object> componentPath,
                Map<Object, Object> component,
                Container.IPropertyValueAccessor propertyValueAccessor);

        void processComponentAfterIndexing(IComponent component);

        Function<Map<Object, Object>, Object> compileEvolverCode(Object propertyId, Object evolverCode,
                                                                 Function<List<Object>, Integer> indexProvider,
                                                                 List<Object> componentPath,
                                                                 IPropertyValueAccessor propertyValueAccessor);

        /**
         * @param inputDependencies
         * @param evolveReason
         * @return true if given inputDependencies list explicitly declares dependency on given evolveReason,
         *         or if it is not known; false only if it is known that given inputDependencies does NOT
         *         depend on given evolveReason
         */
        boolean isInterestedIn(Collection<Object> inputDependencies, Object evolveReason);

        boolean isWildcardPathElement(Object e);
    }

    public interface IComponent extends Map<Object, Object>
    {
        Integer getPropertyIndex(Object key);

        Collection<Integer> getPropertyIndices();

        List<Integer> getChildIndices();

        Integer getChildIndex(Object childId);

        Object getCustomData();

        void setCustomData(Object data);
    }

    public interface IContainerAccessor
    {
        IComponent getComponent(int componentUid);
    }

    public interface IPropertyValueAccessor
    {
        Object getPropertyValue(Integer index);
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
        private List<Integer> childIndices_;
        private Map<Object, Integer> childIdToIndex_;

        private final Function<List<Object>, Object> globalIndexToValueProvider_;

        private Object currentEvolveReason_;

        private Object customData_;

        public ComponentAccessor(List<Object> componentPath, List<Object> values, Function<List<Object>, Object> globalIndexToValueProvider)
        {
            componentPath_ = Collections.unmodifiableList(componentPath);
            propertyIdToIndex_ = new HashMap<>();
            values_ = Collections.unmodifiableList(values);
            globalIndexToValueProvider_ = globalIndexToValueProvider;
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
        public Collection<Integer> getPropertyIndices()
        {
            return propertyIdToIndex_.values();
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
        public List<Integer> getChildIndices()
        {
            return childIndices_;
        }

        @Override
        public Integer getChildIndex(Object childId)
        {
            return childIdToIndex_.get(childId);
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

        public List<Object> getComponentPath()
        {
            return componentPath_;
        }

        void setChildIndices(List<Integer> childIndices, Map<Object, Integer> childIdToIndex)
        {
            childIndices_ = childIndices;
            childIdToIndex_ = childIdToIndex;
        }

        void changeChildIndicesOrder(Collection<Integer> childIndices)
        {
            if (debug_)
            {
                Set<Integer> oldSet = new HashSet<>(childIndices_);
                Set<Integer> newSet = new HashSet<>(childIndices);
                if (!oldSet.equals(newSet))
                {
                    throw new IllegalStateException("Old child indices: " + oldSet + " new: " + newSet);
                }
            }
            childIndices_ = new ArrayList<>(childIndices);
        }

        void addChildIndices(Collection<Integer> childIndices, Map<Object, Integer> childIdToIndex)
        {
            childIndices_.addAll(childIndices);
            childIdToIndex_.putAll(childIdToIndex);
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

        public Object getValueByAbsPath(List<Object> asbPath)
        {
            return globalIndexToValueProvider_.apply(asbPath);
        }
    }

    /**
     * Represents a property of a component (internal indexed)
     */
    public static class Node
    {
        private final Integer componentUid_;
        private final Object propertyId_;
        private final Integer parentComponentUid_;
        private final boolean childrenProperty_;
        private final boolean childOrderProperty_;
        private final List<Object> nodePath_;
        private final Integer nodeUid_;
        private final Collection<DependencyInfo> relAndAbsDependencyPaths_;
        private final boolean hasAmbiguousDependencies_;
        private final Collection<Object> inputDependencies_;
        private Map<Integer, Tuple> dependencyIndices_;
        private Object evolverCode_;
        private Function<Map<Object, Object>, Object> evolver_;

        // TODO Optimize:
        // remove dependents covered by longer chains. Maybe not remove but just hide since longer chains may be provided
        // by components that may be removed
        private final Map<Integer, List<Object>> dependentIndexToRelPath_;

        public Node(
                Integer componentUid,
                Object propertyId,
                Integer parentComponentUid,
                boolean childrenProperty,
                boolean childOrderProperty,
                List<Object> nodePath,
                Integer nodeUid,
                Collection<DependencyInfo> relAndAbsDependencyPaths,
                Collection<Object> inputDependencies,
                Object evolverCode)
        {
            componentUid_ = componentUid;
            propertyId_ = propertyId;
            parentComponentUid_ = parentComponentUid;
            childrenProperty_ = childrenProperty;
            childOrderProperty_ = childOrderProperty;
            nodePath_ = nodePath;
            nodeUid_ = nodeUid;
            relAndAbsDependencyPaths_ = relAndAbsDependencyPaths;
            boolean hasAmbiguousDependencies = false;
            for (DependencyInfo dependencyInfo : relAndAbsDependencyPaths_)
            {
                if (dependencyInfo.isAmbiguous())
                {
                    hasAmbiguousDependencies = true;
                    break;
                }
            }
            hasAmbiguousDependencies_ = hasAmbiguousDependencies;
            inputDependencies_ = inputDependencies;
            dependentIndexToRelPath_ = new HashMap<>();
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

        public Integer getParentComponentUid()
        {
            return parentComponentUid_;
        }

        public boolean isChildrenProperty()
        {
            return childrenProperty_;
        }

        public boolean isChildOrderProperty()
        {
            return childOrderProperty_;
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

        public boolean isHasAmbiguousDependencies()
        {
            return hasAmbiguousDependencies_;
        }

        private void findNodeIndices(ComponentAccessor c, int pathIndex, DependencyInfo d,
                                     List<ComponentAccessor> components, Predicate<Object> isWildcard,
                                     Consumer<Tuple> dependencyPostprocessor)
        {
            List<Object> absPath = d.getAbsPath();
            int absPathSize = absPath.size();
            if (absPathSize == 1)
            {
                return;
            }
            Object e = absPath.get(pathIndex);
            if (pathIndex < absPathSize-1)
            {
                if (isWildcard.test(e))
                {
                    List<Integer> allChildIndices = c.getChildIndices();
                    for (Integer childIndex : allChildIndices)
                    {
                        findNodeIndices(components.get(childIndex), pathIndex+1, d, components, isWildcard, dependencyPostprocessor);
                    }
                }
                else
                {
                    Integer childIndex = c.getChildIndex(e);
                    if (childIndex != null)
                    {
                        findNodeIndices(components.get(childIndex), pathIndex+1, d, components, isWildcard, dependencyPostprocessor);
                    }
                }
            }
            else
            {
                Integer propertyIndex = c.getPropertyIndex(e);
                if (propertyIndex != null)
                {
                    Tuple dependency = Tuple.triple(propertyIndex, d.getRelPath(), d.isAmbiguous());
                    dependencyIndices_.put(propertyIndex, dependency);
                    if (dependencyPostprocessor != null)
                    {
                        dependencyPostprocessor.accept(dependency);
                    }
                }
            }
        }

        public void resolveDependencyIndices(List<ComponentAccessor> components, Predicate<Object> isWildCard)
        {
            dependencyIndices_ = new HashMap<>();

            for (DependencyInfo d : relAndAbsDependencyPaths_)
            {
                findNodeIndices(components.get(0), 1, d, components, isWildCard, null);
            }
        }

        public Collection<Tuple> reevaluateAmbiguousDependencies(List<ComponentAccessor> components, Predicate<Object> isWildCard)
        {
            Collection<Tuple> newlyAddedDependencies = new ArrayList<>();
            for (DependencyInfo d : relAndAbsDependencyPaths_)
            {
                findNodeIndices(components.get(0), 1, d, components, isWildCard, newlyAddedDependencies::add);
            }
            return newlyAddedDependencies;
        }

        public Collection<Tuple> getDependencyIndices()
        {
            return dependencyIndices_.values();
        }

        public Map<Integer, List<Object>> getDependentIndices()
        {
            return dependentIndexToRelPath_;
        }

        public void addDependent(Integer nodeIndex, List<Object> nodeAbsPath, List<Object> relPath)
        {
            List<Object> actualRef = new ArrayList<>(relPath);
            if (nodeAbsPath.size() < nodePath_.size())
            {
                // Replace wildcards (:*) with actual child ids. Start from 1 not to replace *this
                for (int i=1; i<relPath.size(); i++)
                {
                    actualRef.set(i, nodePath_.get(nodePath_.size() - relPath.size() + i));
                }
            }

            log(nodeUid_ + " " + nodePath_ + " added dependent: " + nodeIndex + " " + nodeAbsPath + " referenced as " + relPath + " actual ref " + actualRef);
            dependentIndexToRelPath_.put(nodeIndex, actualRef);
        }

        public void removeDependent(Integer nodeIndex)
        {
            dependentIndexToRelPath_.remove(nodeIndex);
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

        void forgetDependency(Integer nodeIndex)
        {
            dependencyIndices_.remove(nodeIndex);
            ((ClojureContainerParser.EvolverWrapper)evolver_).unlinkAllDelegates();
        }
    }
}
