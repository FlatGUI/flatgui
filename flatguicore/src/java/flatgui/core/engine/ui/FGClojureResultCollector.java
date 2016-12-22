/*
 * Copyright Denys Lebediev
 */
package flatgui.core.engine.ui;

import clojure.lang.Keyword;
import clojure.lang.PersistentHashMap;
import clojure.lang.Var;
import flatgui.core.engine.Container;
import flatgui.core.engine.IResultCollector;

import java.awt.geom.AffineTransform;
import java.awt.geom.NoninvertibleTransformException;
import java.util.*;
import java.util.function.Consumer;

/**
 * @author Denis Lebedev
 */
public class FGClojureResultCollector implements IResultCollector
{
    private static final Keyword VISIBLE_POPUP_KW = Keyword.intern("_visible-popup");

    //private static final String FG_NS = "flatgui.core";
    private static final Var rebuildLook_ = clojure.lang.RT.var("flatgui.paint", "rebuild-look");

    private final int unitSizePx_;

    private final Map<Integer, Set<Integer>> parentToVisiblePopupChildCount_;

    private final Set<Integer> changedComponents_;

    private final List<List<Object>> lookVectors_;

    public FGClojureResultCollector(int unitSizePx)
    {
        changedComponents_ = new HashSet<>();
        parentToVisiblePopupChildCount_ = new HashMap<>();
        lookVectors_ = new ArrayList<>();
        unitSizePx_ = unitSizePx;
    }

    @Override
    public void componentAdded(Integer componentUid)
    {
        if (componentUid.intValue() >= lookVectors_.size())
        {
            int add = componentUid.intValue() + 1 - lookVectors_.size();
            for (int i=0; i<add; i++)
            {
                lookVectors_.add(null);
            }
        }
    }

    @Override
    public void componentRemoved(Integer componentUid)
    {
        lookVectors_.set(componentUid.intValue(), null);
    }

    @Override
    public void appendResult(Integer parentComponentUid, List<Object> path, Integer componentUid, Object property, Object newValue)
    {
        changedComponents_.add(componentUid);

        // TODO
        // Evolved property node can already contain boolean flags so that there is no need to compare.
        // But it has to be a UI subclass
        if (property.equals(VISIBLE_POPUP_KW))
        {
            Set<Integer> visiblePopupChildIndices = parentToVisiblePopupChildCount_.get(parentComponentUid);
            if (visiblePopupChildIndices == null)
            {
                visiblePopupChildIndices = new HashSet<>();
                parentToVisiblePopupChildCount_.put(parentComponentUid, visiblePopupChildIndices);
            }
            if (newValue != null && !(newValue instanceof Boolean && !((Boolean) newValue).booleanValue()))
            {
                visiblePopupChildIndices.add(componentUid);
            }
            else
            {
                visiblePopupChildIndices.remove(componentUid);
            }
        }
    }

    @Override
    public void onEvolveCycleStarted()
    {
    }

    @Override
    public void postProcessAfterEvolveCycle(Container.IContainerAccessor containerAccessor, Container.IContainerMutator containerMutator)
    {
        // TODO (*1) maybe rework deflookfn to utilize indexed access to properties

        //System.out.println("-DLTEMP- (1)FGClojureResultCollector.postProcessAfterEvolveCycle " + changedComponents_);
        for (Integer changedComponentUid : changedComponents_)
        {
            Container.IComponent componentAccessor = containerAccessor.getComponent(changedComponentUid.intValue());
            FGClojureContainerParser.FGComponentDataCache componentDataCache =
                    (FGClojureContainerParser.FGComponentDataCache) componentAccessor.getCustomData();

            // TODO creating Clojure map should not be needed after (*1)
            Object componentClj = PersistentHashMap.create(componentAccessor);
            Object lookResult = rebuildLook_.invoke(componentClj);
            //System.out.println("-DLTEMP- (2)FGClojureResultCollector.postProcessAfterEvolveCycle " + changedComponentUid + " " + lookResult);
            List<Object> lookVec = (List<Object>) rebuildLook_.invoke(componentClj);

            containerMutator.setValue(componentDataCache.getLookVecIndex(), lookVec);

            lookVectors_.set(changedComponentUid.intValue(), lookVec);
        }

        changedComponents_.clear();
    }

    boolean hasVisiblePopupChildren(Integer componentUid)
    {
        Set<Integer> visiblePopupChildCount = parentToVisiblePopupChildCount_.get(componentUid);
        return visiblePopupChildCount != null && !visiblePopupChildCount.isEmpty();
    }

    // Paint in Java

    private static Double mxGet(List<List<Number>> matrix, int r, int c)
    {
        return matrix.get(r).get(c).doubleValue();
    }

    private static Double mxX(List<List<Number>> matrix)
    {
        return mxGet(matrix, 0, 0);
    }

    private static Double mxY(List<List<Number>> matrix)
    {
        return mxGet(matrix, 1, 0);
    }

    private final AffineTransform affineTransform(List<List<Number>> matrix)
    {
        double m00 = matrix.get(0).get(0).doubleValue();
        double m10 = matrix.get(1).get(0).doubleValue();
        double m01 = matrix.get(0).get(1).doubleValue();
        double m11 = matrix.get(1).get(1).doubleValue();
        double m02 = unitSizePx_ * matrix.get(0).get(3).doubleValue();
        double m12 = unitSizePx_ * matrix.get(1).get(3).doubleValue();
        return new AffineTransform(m00, m10, m01, m11, m02, m12);
    }

    private final List<Object> PUSH_CURRENT_CLIP = Collections.unmodifiableList(
            Arrays.asList("pushCurrentClip"));
    private final List<Object> POP_CURRENT_CLIP = Collections.unmodifiableList(
            Arrays.asList("popCurrentClip"));
    private final List<Object> SET_CLIP = Arrays.asList("setClip", 0, 0, null, null);
    private final List<Object> CLIP_RECT = Arrays.asList("clipRect", 0, 0, null, null);
    private final List<Object> TRANSFORM = Arrays.asList("transform", null);

    private final List<Object> setClip(Double clipW, Double clipH)
    {
        SET_CLIP.set(3, clipW);
        SET_CLIP.set(4, clipH);
        return SET_CLIP;
    }

    private final List<Object> clipRect(Double clipW, Double clipH)
    {
        CLIP_RECT.set(3, clipW);
        CLIP_RECT.set(4, clipH);
        return CLIP_RECT;
    }

    private final List<Object> transform(AffineTransform matrix)
    {
        TRANSFORM.set(1, matrix);
        return TRANSFORM;
    }

    void paintComponentWithChildren(Consumer<List<Object>> primitivePainter, Container.IContainerAccessor containerAccessor, Container.IPropertyValueAccessor propertyValueAccessor, Integer componentUid) throws NoninvertibleTransformException
    {
        List<Object> lookVector = lookVectors_.get(componentUid);
        if (lookVector == null)
        {
            // TODO lookVector seems to be null for newly added table cells, but not always??
            return;
        }

        Container.IComponent componentAccessor = containerAccessor.getComponent(componentUid.intValue());
        FGClojureContainerParser.FGComponentDataCache componentDataCache =
                (FGClojureContainerParser.FGComponentDataCache) componentAccessor.getCustomData();

        Object visible = propertyValueAccessor.getPropertyValue(componentDataCache.getVisibleIndex());
        if (visible == null || visible instanceof Boolean && !(((Boolean) visible).booleanValue()))
        {
            return;
        }

        Object positionMatrixObj = propertyValueAccessor.getPropertyValue(componentDataCache.getPositionMatrixIndex());

        // TODO Cache both AWT format matrix object and and its inverse in the original matrix (in meta)?

        AffineTransform positionMatrix = affineTransform((List<List<Number>>) positionMatrixObj);
        AffineTransform positionMatrixInv = positionMatrix.createInverse();

        Object viewportMatrixObj = propertyValueAccessor.getPropertyValue(componentDataCache.getViewportMatrixIndex());
        AffineTransform viewportMatrix = affineTransform((List<List<Number>>) viewportMatrixObj);
        AffineTransform viewportMatrixInv = viewportMatrix.createInverse();

        List<List<Number>> clipSizeObj = (List<List<Number>>)propertyValueAccessor.getPropertyValue(componentDataCache.getClipSizeIndex());
        Double clipW = mxX(clipSizeObj);
        Double clipH = mxY(clipSizeObj);

        Boolean popup = (Boolean)propertyValueAccessor.getPropertyValue(componentDataCache.getPopupIndex());

        primitivePainter.accept(PUSH_CURRENT_CLIP);
        primitivePainter.accept(transform(positionMatrix));
        primitivePainter.accept(popup.booleanValue() ? setClip(clipW, clipH) : clipRect(clipW, clipH));
        primitivePainter.accept(transform(viewportMatrix));
        primitivePainter.accept(lookVector);
        Integer childrenIndex = componentDataCache.getChildrenIndex();
        if (childrenIndex != null)
        {
            //Map<Keyword, Map<Keyword, Object>> children = (Map<Keyword, Map<Keyword, Object>>) propertyValueAccessor.getPropertyValue(childrenIndex);
            for (Integer childIndex : componentAccessor.getChildIndices())
            {
                paintComponentWithChildren(primitivePainter, containerAccessor, propertyValueAccessor, childIndex);
            }
        }
        primitivePainter.accept(transform(viewportMatrixInv));
        primitivePainter.accept(transform(positionMatrixInv));
        primitivePainter.accept(POP_CURRENT_CLIP);
    }


//    public List<Object> getLookVector(Integer componentUid)
//    {
//        return lookVectors_.get(componentUid.intValue());
//    }

//    private static class PaintListIterator implements Iterator<Object>
//    {
//        private static Function<FGClojureContainerParser.FGComponentDataCache, List<Object>>[] ComponentDrawSeq_;
//
//        //private Integer currentComponentId_;
//        private int currentComponentInternalIndex_;
//        //private List<TraversedComponent> traversedComponents_;
//        private List<Integer> traversedComponentIds_;
//        private List<Iterator<Integer>> traversedComponentChildIndexIterators_;
//        private List<FGClojureContainerParser.FGComponentDataCache> traversedComponentCustomDatas_;
//
//        private Container.IContainerAccessor containerAccessor_;
//        private Container.IPropertyValueAccessor propertyValueAccessor_;
//
//        public PaintListIterator()
//        {
//            //traversedComponents_ = new ArrayList<>();
//            traversedComponentIds_ = new ArrayList<>();
//            traversedComponentChildIndexIterators_ = new ArrayList<>();
//            traversedComponentCustomDatas_ = new ArrayList<>();
//
//            //currentComponentId_ = 0;
//            currentComponentInternalIndex_ = 0;
//            //updateCurrentComponentData();
//        }
//
//        @Override
//        public boolean hasNext()
//        {
//            ////return currentComponentId_ < lookVectors_.size() && isWalkingThroughComponent();
//            //return !traversedComponents_.isEmpty();
//            return !traversedComponentIds_.isEmpty();
//        }
//
//        @Override
//        public Object next()
//        {
//            //TraversedComponent traversedComponent = traversedComponents_.get(traversedComponents_.size()-1);
//            int currentStackIndex = traversedComponentIds_.size()-1;
//            //currentComponentId_ = traversedComponentIds_.get(currentStackIndex);
//            Iterator<Integer> childIndexIterator = traversedComponentChildIndexIterators_.get(currentStackIndex);
//            FGClojureContainerParser.FGComponentDataCache customData = traversedComponentCustomDatas_.get(currentStackIndex);
//
//            List<Object> cmdToReturn;
//
//            if (isWalkingThroughComponent())
//            {
//                cmdToReturn = ComponentDrawSeq_[currentComponentInternalIndex_].apply(customData);
//                currentComponentInternalIndex_++;
//
//                if (cmdToReturn == null)
//                {
//                    // Begin painting children of current component
//                }
//            }
//            else
//            {
//                currentComponentInternalIndex_ = 0;
//
//                if (childIndexIterator.hasNext())
//                {
//                    Integer nextChildId = childIndexIterator.next();
//
//                    traversedComponentIds_.add(nextChildId);
//                    Container.IComponent componentAccessor = containerAccessor_.getComponent(nextChildId);
//                    traversedComponentCustomDatas_.add((FGClojureContainerParser.FGComponentDataCache) componentAccessor.getCustomData());
//                    traversedComponentChildIndexIterators_.add(componentAccessor.getChildIndices().iterator());
//                }
//                else
//                {
//                    traversedComponentIds_.remove(currentStackIndex);
//                    traversedComponentChildIndexIterators_.remove(currentStackIndex);
//                    traversedComponentCustomDatas_.remove(currentStackIndex);
//                }
//            }
//
//
//
//            return cmdToReturn;
//
//            if (isWalkingThroughComponent())
//            {
//                List<Object> cmd = ComponentDrawSeq_[currentComponentInternalIndex_].apply(customData);
//                if (cmd != null)
//                {
//                    return cmd;
//                }
//                else
//                {
//                    // Paint children
//                }
//
//                currentComponentInternalIndex_++;
//            }
//            else
//            {
//                currentComponentInternalIndex_ = 0;
//
//                if (childIndexIterator.hasNext())
//                {
//                    currentComponentId_ = childIndexIterator.next();
//                    updateCurrentComponentData();
//                }
//                else
//                {
//                    traversedComponentIds_.remove(currentStackIndex);
//                    traversedComponentChildIndexIterators_.remove(currentStackIndex);
//                    traversedComponentCustomDatas_.remove(currentStackIndex);
//                }
//            }
//
////            if (isWalkingThroughComponent())
////            {
////                currentComponentInternalIndex_++;
////            }
////            else
////            {
////                currentComponentInternalIndex_ = 0;
////                currentComponentId_++;
////            }
////            return null;
//        }
//
//        private boolean isWalkingThroughComponent()
//        {
//            return currentComponentInternalIndex_ < ComponentDrawSeq_.length;
//        }
//
////        private final void updateCurrentComponentData()
////        {
////            Container.IComponent componentAccessor = containerAccessor_.getComponent(currentComponentId_);
////            currentComponentData_ = (FGClojureContainerParser.FGComponentDataCache) componentAccessor.getCustomData();
////            traversedComponents_.add(new TraversedComponent(currentComponentId_, componentAccessor.getChildIndices().iterator()));
////        }
//
////        private class TraversedComponent
////        {
////            private int componentId_;
////            private Iterator<Integer> childIndexIterator_;
////            private FGClojureContainerParser.FGComponentDataCache currentComponentData_;
////
////            public TraversedComponent(int componentId, Iterator<Integer> childIndexIterator, FGClojureContainerParser.FGComponentDataCache currentComponentData)
////            {
////                componentId_ = componentId;
////                childIndexIterator_ = childIndexIterator;
////                currentComponentData_ = currentComponentData
////            }
////
////            public int getComponentId()
////            {
////                return componentId_;
////            }
////
////            public Iterator<Integer> getChildIndexIterator()
////            {
////                return childIndexIterator_;
////            }
////        }
//    }
}
