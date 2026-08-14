package org.zstack.core.validator;

import org.zstack.core.GlobalProperty;
import org.zstack.core.GlobalPropertyDefinition;
import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.utils.logging.CLogger;
import org.zstack.utils.logging.CLoggerImpl;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.*;

import static org.zstack.utils.BeanUtils.reflections;

public class ValidatorTool {
    private static final CLogger logger = CLoggerImpl.getLogger(org.zstack.core.validator.ValidatorTool.class);
    static Map<Class<?>, Class<?>> validateMap = new HashMap<>();
    static Map<GlobalProperty, Field> annotationsAndFieldMap = new HashMap<>();
    static Map<Field, Annotation[]> fieldAndAnnotationsMap = new HashMap<>();

    static {
        reflections.getTypesAnnotatedWith(ValidateTarget.class).forEach(clz -> {
            ValidateTarget at = clz.getAnnotation(ValidateTarget.class);
            if (at != null) {
                validateMap.put(at.target(), clz);
            }
        });

        reflections.getTypesAnnotatedWith(GlobalPropertyDefinition.class).forEach(clz -> {
            Arrays.stream(clz.getDeclaredFields()).filter(f->!f.isSynthetic()).forEach(f -> {
                GlobalProperty at = f.getAnnotation(GlobalProperty.class);
                Annotation[] annotations = f.getAnnotations();
                fieldAndAnnotationsMap.put(f, annotations);
                annotationsAndFieldMap.put(at, f);
            });
        });
    }

    public boolean checkProperty(String propertyName, String propertyValue) {

        Set<GlobalProperty> annotationsSet = annotationsAndFieldMap.keySet();

        for (GlobalProperty at : annotationsSet) {
            Field f = annotationsAndFieldMap.get(at);
            if (at.name().isEmpty() || !at.name().trim().equals(propertyName)) {
                continue;
            }
            return matchAnnotationMethod(f, propertyName, propertyValue);
        }

        return false;
    }

    private boolean matchAnnotationMethod(Field f, String propertyName, String propertyValue) {

        Annotation[] annotations = fieldAndAnnotationsMap.get(f);
        if (annotations == null) {
            return false;
        }
        for (Annotation annotation : annotations) {
            Class<?> annotationType = annotation.annotationType();
            Class<?> validatorType = validateMap.get(annotationType);

            if (validatorType == null) {
                continue;
            }
            try {
                Constructor<? extends PropertyValidator> constructor = (Constructor<? extends PropertyValidator>) validatorType.getConstructor();
                PropertyValidator validator = constructor.newInstance();
                return validator.validate(propertyName, propertyValue, annotation);
            } catch (PropertyValidatorException t) {
                logger.warn("error happened :", t);
                throw new CloudRuntimeException(t);
            } catch (NoSuchMethodException | InstantiationException | InvocationTargetException | IllegalAccessException | RuntimeException e) {
                logger.warn("exception happened :", e);
            }
        }
        return false;
    }
}

