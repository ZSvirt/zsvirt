package org.zstack.testlib.util

import net.sf.cglib.proxy.Enhancer
import net.sf.cglib.proxy.MethodInterceptor
import net.sf.cglib.proxy.MethodProxy
import org.zstack.utils.FieldUtils
import org.zstack.utils.Utils
import org.zstack.utils.logging.CLogger

import java.lang.reflect.Method
import java.lang.reflect.Modifier

class TProxy {
    CLogger logger = Utils.getLogger(TProxy.class)

    private Map self = [:]
    private Object proxyedObject

    class ProxyHandler implements MethodInterceptor {
        @Override
        Object intercept(Object o, Method method, Object[] objects, MethodProxy methodProxy) throws Throwable {
            Closure call = self[method.name] as Closure

            if (call != null) {
                if (call.maximumNumberOfParameters == objects.length) {
                    return call(*objects)
                } else {
                    return call({
                        return methodProxy.invokeSuper(o, objects)
                    }, *objects)
                }
            } else {
                return methodProxy.invokeSuper(o, objects)
            }
        }
    }

    TProxy(Class adapteeClass) {
        Enhancer enhancer = new Enhancer()
        enhancer.setSuperclass(adapteeClass)
        enhancer.setCallback(new ProxyHandler())
        proxyedObject = enhancer.create()
    }

    TProxy(Object adaptee) {
        assert adaptee != null : "call TProxy(Class adapteeClass) instead"

        Enhancer enhancer = new Enhancer()
        enhancer.setSuperclass(adaptee.getClass())
        enhancer.setCallback(new ProxyHandler())
        try {
            proxyedObject = enhancer.create()
            if (adaptee != null) {
                FieldUtils.getAllFields(adaptee.getClass()).each { f ->
                    if (Modifier.isStatic(f.modifiers) || Modifier.isFinal(f.modifiers)) {
                        return
                    }

                    f.setAccessible(true)
                    f.set(proxyedObject, f.get(adaptee))
                }
            }
        } catch (IllegalArgumentException e) {
            if (e.getMessage().contains("Superclass has no null constructors" )) {
                throw new Exception("the class ${adaptee.getClass()} has no non-argument constructor", e)
            }

            throw e
        }
    }

    /**
     *
     * @param name: 要替换函数的名称
     * @param c：函数体，其参数列表有两种形式：
     *
     * 1. 跟要替换的函数相同
     *
     * 例如要替换的write函数原型为：
     *
     * void write(Point p)
     *
     * 则传入的@c的应该是：
     *
     * mockMethod("write") { Point p -> }
     *
     * 2. 包含一个额外的invokeSuper参数
     *
     * 当要在@c中调用被替代的函数本身的实现时，@c的参数列表第一个参数必须是invokeSuper参数，
     * 后面才是原函数的参数列表，例如：
     *
     *
     * mockMethod("write") { invokeSuper, Point p ->
     *     invokeSuper()
     * }
     *
     * invokeSuper参数是一个函数，原型为void invokeSuper()
     *
     * 注意：这种方式只对用TProxy(Object adaptee)构造的TProxy object有意义
     *
     *
     * @return TProxy object
     */
    TProxy mockMethod(String name, Closure c) {
        self[name] = c
        return this
    }

    Object asType(Class clz) {
        return proxyedObject
    }

    /**
     *
     * @param bean: 要保护的bean对象
     * @param fnames：要保护的字段名称数组
     * @return FieldProtector 对象，调用其recover函数可以恢复@bean上被保护字段原来的值
     */
    FieldProtector protect(bean, String...fnames) {
        return new FieldProtector(bean, fnames)
    }

    class FieldProtector {
        def bean
        Map<String, Object> fields = [:]

        FieldProtector(bean, String...fnames) {
            this.bean = bean
            fnames.each {
                fields[it] = bean[it]
            }
        }

        void recover() {
            fields.each {k ,v ->
                bean[k] = v
            }
        }
    }
}
