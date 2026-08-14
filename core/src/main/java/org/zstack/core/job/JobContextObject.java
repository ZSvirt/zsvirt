package org.zstack.core.job;

import org.zstack.header.exception.CloudRuntimeException;

import java.io.Serializable;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

final class JobContextObject implements Serializable {
    private final String className;
    private Map<String, Object> args = new HashMap<String, Object>();

    JobContextObject(Job job) {
        className = job.getClass().getName();
        save(job);
    }

    private void save(Job obj) {
        Class<?> currClass = obj.getClass();
        Field debugField = null;
        try {
            do {
                for (Field f : currClass.getDeclaredFields()) {
                    if (f.isAnnotationPresent(JobContext.class)) {
                        debugField = f;
                        f.setAccessible(true);
                        Object val = f.get(obj);
                        if (val != null) {
                            assert val instanceof Serializable : val.getClass().getName() + " doesn't implement Serializable";
                        }
                        args.put(f.getName(), val);
                    }
                }
                currClass = currClass.getSuperclass();
            } while (currClass != Object.class && currClass != null);
        } catch (Exception e) {
            String name = debugField == null ? "Unknown" : debugField.getName();
            throw new CloudRuntimeException("Unable to get value of " + obj.getClass().getCanonicalName() + "." + name, e);
        }
    }
    
    Job load() {
        try {
            Class<?> currClass = Class.forName(className);
            Constructor<?> cons = currClass.getDeclaredConstructor(null);
            cons.setAccessible(true);
            Object obj = cons.newInstance();
            do {
                for (Field f : currClass.getDeclaredFields()) {
                    if (f.isAnnotationPresent(JobContext.class)) {
                        f.setAccessible(true);
                        if (!args.containsKey(f.getName())) {
                            String err = String.format("%s.%s is marked as JobContext, however, we cannot find it in previous saved context. DB corrupted? %s binary changed?", currClass.getCanonicalName(), f.getName(), currClass.getCanonicalName());
                            throw new IllegalArgumentException(err);
                        }
                        Object val = args.get(f.getName());
                        f.set(obj, val);
                    }
                }
                currClass = currClass.getSuperclass();
            } while (currClass != Object.class && currClass != null);
            return (Job) obj;
        } catch (NoSuchMethodException e) { 
            throw new CloudRuntimeException(String.format("Job %s must have a constructor with zero-argument", className), e);
        } catch (Exception e) {
            throw new CloudRuntimeException("Unable to load " + className + " from previous saved context", e);
        }
    }
}
