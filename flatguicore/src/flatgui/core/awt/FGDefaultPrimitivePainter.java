/*
 * Copyright (c) 2015 Denys Lebediev and contributors. All rights reserved.
 * The use and distribution terms for this software are covered by the
 * Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
 * which can be found in the file LICENSE at the root of this distribution.
 * By using this software in any fashion, you are agreeing to be bound by
 * the terms of this license.
 * You must not remove this notice, or any other, from this software.
 */
package flatgui.core.awt;


import java.awt.*;
import java.lang.reflect.Method;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * @author Denys Lebediev
 *         Date: 9/3/13
 *         Time: 6:55 PM
 */
public class FGDefaultPrimitivePainter implements IFGPrimitivePainter
{
    private static Map<Class, Class> NUMBER_CLASS_TO_PRIMITIVE = new HashMap<>();
    static
    {
        NUMBER_CLASS_TO_PRIMITIVE.put(Integer.class, Integer.TYPE);
        NUMBER_CLASS_TO_PRIMITIVE.put(Long.class, Integer.TYPE);
    }

    private static final String PUSH_CURRENT_CLIP = "pushCurrentClip";
    private static final String POP_CURRENT_CLIP = "popCurrentClip";

    private double unitSizePx_;

    private Deque<Shape> clipRectStack_;
    private Map<String, Consumer<Graphics2D>> customMethods_;
    private Map<String, Method> methodByNameCache_;

    public FGDefaultPrimitivePainter(double unitSizePx)
    {
        clipRectStack_ = new LinkedList<>();
        customMethods_ = new HashMap<>();
        customMethods_.put(PUSH_CURRENT_CLIP, g -> clipRectStack_.addLast(g.getClip()));
        customMethods_.put(POP_CURRENT_CLIP, g -> g.setClip(clipRectStack_.removeLast()));

        methodByNameCache_ = new HashMap<>();

        unitSizePx_ = unitSizePx;
    }

    /*
     * @todo cache found methods
     */
    @Override
    public void paintPrimitive(Graphics g, java.util.List<Object> primitive)
    {
        //System.out.println("paintPrimitive starts at " + System.currentTimeMillis());

//        System.out.println("-DLTEMP- FGDefaultPrimitivePainter.paintPrimitive invoking for " +
//                primitive);

        String methodName = (String)primitive.get(0);

        Object[] argValues = primitive.stream().skip(1).map(e -> e instanceof Double
                ? (int) (((Double) e).doubleValue()*unitSizePx_)
                : (e instanceof Number ? (int)(((Number)e).doubleValue()*unitSizePx_) : e)).
            collect(Collectors.toList()).toArray();
        Class<?>[] argTypes = Arrays.stream(argValues).map(e -> (e != null ? e.getClass() : null)).
                map(e -> NUMBER_CLASS_TO_PRIMITIVE.containsKey(e)
                        ? NUMBER_CLASS_TO_PRIMITIVE.get(e)
                        : e).
            collect(Collectors.toList()).toArray(new Class<?>[argValues.length]);

        if (customMethods_.containsKey(methodName))
        {
            customMethods_.get(methodName).accept((Graphics2D)g);
        }
        else
        {
            try
            {
                Method m = methodByNameCache_.get(methodName);

                if (m == null)
                {
                    if (Arrays.stream(argTypes).anyMatch(e -> e == null))
                    {
                        Method[] allMethods = g.getClass().getMethods();
                        Optional<Method> found = Arrays.stream(allMethods).filter(e -> (e.getName().equals(methodName) && e.getParameterCount() == argTypes.length)).findAny();
                        m = found.get();
                    }
                    else
                    {
                        m = g.getClass().getMethod(methodName, argTypes);
                    }
                    methodByNameCache_.put(methodName, m);
                }

//            System.out.println("-DLTEMP- FGDefaultPrimitivePainter.paintPrimitive invoking " + m.getName() +
//                " : " + Arrays.toString(argValues));

                m.invoke(g, argValues);
            }
            catch (Exception e)
            {
                System.out.println("Error painting primitive: " + primitive);
                e.printStackTrace();
            }
        }

        //System.out.println("paintPrimitive finishes at " + System.currentTimeMillis());
    }

//    private static class CacheKey
//    {
//        private String methodName_;
//        private Class<?> argTypes_;
//
//        private CacheKey(String methodName, Class<?> argTypes)
//        {
//        }
//
//    }
}
