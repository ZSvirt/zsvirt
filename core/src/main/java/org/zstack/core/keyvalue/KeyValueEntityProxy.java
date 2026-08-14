package org.zstack.core.keyvalue;

import net.sf.cglib.proxy.*;
import org.apache.commons.lang.StringUtils;
import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.utils.DebugUtils;
import org.zstack.utils.FieldUtils;
import org.zstack.utils.FieldUtils.CollectionGenericType;
import org.zstack.utils.FieldUtils.MapGenericType;
import org.zstack.utils.StringDSL;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.*;

/**
 */
public class KeyValueEntityProxy<T> {
    private static Map<Class, Enhancer> enhancers = new HashMap<Class, Enhancer>();

    public class KeyValueMapProxy {
        private Object proxyMap;

        private void makePath() {
            paths.add(StringUtils.join(trace, "."));
            trace.clear();
        }

        KeyValueMapProxy(final Class valueType, final String fieldName, final List<String> paths, final Stack<String> trace) {
            Enhancer e = getEnhancer(Map.class);
            proxyMap = e.create();
            Factory f = (Factory) proxyMap;
            f.setCallback(0, new MethodInterceptor() {
                @Override
                public Object intercept(Object o, Method method, Object[] objects, MethodProxy methodProxy) throws Throwable {
                    if (!"get".equals(method.getName())) {
                        throw new CloudRuntimeException(String.format("only get() can be called on KeyValueMapProxy, current path is %s", StringUtils.join(paths, ".")));
                    }

                    String key = (String) objects[0];
                    if (key == null) {
                        trace.push(String.format("%s[\"%%\"]", fieldName));
                    } else {
                        trace.push(String.format("%s[\"%s\"]", fieldName, key));
                    }

                    if (KeyValueUtils.isPrimitiveTypeForKeyValue(valueType)) {
                        makePath();
                        return null;
                    }

                    return new KeyValueEntityProxy(valueType, paths, trace).getProxyEntity();
                }
            });
        }

        public Object getProxyMap() {
            return proxyMap;
        }
    }

    public class KeyValueListProxy {
        private Object proxyList;

        private void makePath() {
            paths.add(StringUtils.join(trace, "."));
            trace.clear();
        }

        KeyValueListProxy(final Class valueType, final String fieldName, final List<String> paths, final Stack<String> trace) {
            Enhancer e = getEnhancer(List.class);
            proxyList = e.create();
            Factory f = (Factory) proxyList;
            f.setCallback(0 ,new MethodInterceptor() {
                @Override
                public Object intercept(Object o, Method method, Object[] objects, MethodProxy methodProxy) throws Throwable {
                    if (!"get".equals(method.getName())) {
                        throw new CloudRuntimeException(String.format("only get() can be called on KeyValueListProxy, current path is %s", StringUtils.join(paths, ".")));
                    }

                    Integer index = (Integer) objects[0];
                    if (index == -1) {
                        trace.push(String.format("%s[%%]", fieldName));
                    } else {
                        trace.push(String.format("%s[%s]", fieldName, index));
                    }

                    if (KeyValueUtils.isPrimitiveTypeForKeyValue(valueType)) {
                        makePath();
                        return null;
                    }

                    return new KeyValueEntityProxy(valueType, paths, trace).getProxyEntity();
                }
            });
        }

        public Object getProxyList() {
            return proxyList;
        }
    }

    private T proxyEntity;
    private List<String> paths;
    private Stack<String> trace;

    private static Enhancer getEnhancer(Class clz) {
        if (Enhancer.isEnhanced(clz)) {
            clz = clz.getSuperclass();
        }

        Enhancer e = enhancers.get(clz);

        if (e == null) {
            e = new Enhancer();
            e.setSuperclass(clz);
            e.setCallback(new MethodInterceptor() {
                @Override
                public Object intercept(Object o, Method method, Object[] objects, MethodProxy methodProxy) throws Throwable {
                    throw new CloudRuntimeException("should not be here");
                }
            });
            enhancers.put(clz, e);
        }

        return e;
    }

    private Field getFieldFromMethod(Object o, Method method) {
        DebugUtils.Assert(method.getName().startsWith("get"), String.format("only getter can be called on KeyValueEntityProxy, but %s is called", method.getName()));
        String fieldName = StringDSL.stripStart(method.getName(), "get");
        fieldName = StringUtils.uncapitalize(fieldName);
        Field f = FieldUtils.getField(fieldName, o.getClass());
        DebugUtils.Assert(f != null, String.format("cannot find field[%s] on class[%s], %s is a wrong getter", fieldName, o.getClass().getName(), method.getName()));
        return f;
    }

    private void makePath() {
        paths.add(StringUtils.join(trace, "."));
        trace.clear();
    }

    private Object doInvoke(Object o, Method method, Object[] objects, MethodProxy methodProxy) throws Throwable {
        Field f = getFieldFromMethod(o, method);
        if (KeyValueUtils.isPrimitiveTypeForKeyValue(f.getType())) {
            trace.push(f.getName());
            makePath();
            return methodProxy.invokeSuper(o, objects);
        }

        if (Map.class.isAssignableFrom(f.getType())) {
            return handleMap(f);
        }

        if (List.class.isAssignableFrom(f.getType())) {
            return handleList(f);
        }

        DebugUtils.Assert(!Collection.class.isAssignableFrom(f.getType()), String.format("collection can only be List, but get %s", f.getType().getName()));

        return handleObject(f, o);
    }

    private Object handleObject(Field f, Object o) {
        trace.push(f.getName());
        return new KeyValueEntityProxy(o.getClass(), paths, trace).getProxyEntity();
    }

    private Object handleList(Field f) {
        CollectionGenericType type  = (CollectionGenericType) FieldUtils.inferGenericTypeOnMapOrCollectionField(f);
        return new KeyValueListProxy(type.getValueType(), f.getName(), paths, trace).getProxyList();
    }


    private Object handleMap(Field f) {
        MapGenericType type = (MapGenericType) FieldUtils.inferGenericTypeOnMapOrCollectionField(f);
        return new KeyValueMapProxy(type.getValueType(), f.getName(), paths, trace).getProxyMap();
    }

    private void createEnhancer(Class<T> entityClass) {
        Enhancer enhancer = getEnhancer(entityClass);
        proxyEntity = (T) enhancer.create();
        Factory f = (Factory) proxyEntity;
        f.setCallback(0, new MethodInterceptor() {
            @Override
            public Object intercept(Object o, Method method, Object[] objects, MethodProxy methodProxy) throws Throwable {
                return doInvoke(o, method, objects, methodProxy);
            }
        });
    }

    public KeyValueEntityProxy(Class<T> entityClass) {
        paths = new ArrayList<String>();
        trace = new Stack<String>();
        createEnhancer(entityClass);
    }

    private KeyValueEntityProxy(Class<T> entityClass, List<String> paths, Stack<String> trace) {
        this.paths = paths;
        this.trace = trace;
        createEnhancer(entityClass);
    }

    public T getProxyEntity() {
        return proxyEntity;
    }

    public List<String> getPaths() {
        return paths;
    }
}

