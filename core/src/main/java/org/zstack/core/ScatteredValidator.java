package org.zstack.core;

import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.utils.DebugUtils;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Created by xing5 on 2017/5/2.
 */
public abstract class ScatteredValidator {
    protected static List<Method> collectValidatorMethods(Class annotationClass, Class...argTypes) {
        if (argTypes == null) {
            argTypes = new Class[]{};
        }

        List<Method> methods = new ArrayList<>();
        Set<Method> ms = Platform.getReflections().getMethodsAnnotatedWith(annotationClass);
        for (Method m : ms) {
            if (!Modifier.isStatic(m.getModifiers())) {
                throw new CloudRuntimeException(String.format("@%s %s.%s must be defined as static method", annotationClass, m.getDeclaringClass(), m.getName()));
            }

            if (m.getParameterCount() != argTypes.length) {
                throw new CloudRuntimeException(String.format("wrong argument list of the @%s %s.%s, %s arguments required" +
                        " but the method has %s arguments", annotationClass, m.getDeclaringClass(), m.getName(), argTypes.length,
                        m.getParameterCount()));
            }

            for (int i=0; i<argTypes.length; i++) {
                Class expectedType = argTypes[i];
                Class actualType = m.getParameterTypes()[i];

                if (expectedType != actualType) {
                    throw new CloudRuntimeException(String.format("wrong argument list of the @%s %s.%s. The argument[%s] is expected of type %s" +
                                    " but got type %s", annotationClass, m.getDeclaringClass(), m.getName(), i, expectedType, actualType));
                }
            }

            m.setAccessible(true);
            methods.add(m);
        }
        return methods;
    }

    protected void invokeValidatorMethods(List<Method> methods, Object...args) {
        DebugUtils.Assert(methods != null, "call collectValidatorMethods in static block before calling any methods");

        for (Method m : methods) {
            try {
                m.invoke(null, args);
            } catch (IllegalAccessException e) {
                throw new CloudRuntimeException(e);
            } catch (InvocationTargetException e) {
                if (e.getCause() instanceof OperationFailureException) {
                    throw (OperationFailureException)e.getCause();
                } else {
                    throw new CloudRuntimeException(e);
                }
            }
        }
    }
}
