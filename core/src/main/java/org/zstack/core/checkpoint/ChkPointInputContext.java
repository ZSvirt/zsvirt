package org.zstack.core.checkpoint;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

class ChkPointInputContext implements Serializable {
    private Map<String, Object> inputs = new HashMap<String, Object>();

    ChkPointInputContext(CheckPoint cp) throws IllegalArgumentException, IllegalAccessException {
        save(cp);
    }

    CheckPoint load(CheckPoint cp) {
        try {
            Class<?> currClass = cp.getClass();
            do {
                for (Field f : cp.getClass().getDeclaredFields()) {
                    if (f.isAnnotationPresent(ChkPointInput.class)) {
                        f.setAccessible(true);
                        if (!inputs.containsKey(f.getName())) {
                            String err = "Cannot find input field: " + f.getName() + " in context for CheckPoint " + cp.getClass().getCanonicalName()
                                    + ". This is probably source code changed after last time saving check point context";
                            throw new IllegalArgumentException(err);
                        }
                        Object val = inputs.get(f.getName());
                        f.set(cp, val);
                    }
                }
                currClass = currClass.getSuperclass();
            } while (currClass != Object.class && currClass != null);
            return cp;
        } catch (Exception e) {
            String err = "Unable to reload inputs for CheckPoint " + cp.getClass().getCanonicalName() + ", uuid: ";
            throw new CloudCheckPointException(err, e);
        }
    }

    void save(CheckPoint cp) throws IllegalArgumentException, IllegalAccessException {
        Class<?> currClass = cp.getClass();
        byte[] bcxt = null;
        do {
            for (Field f : cp.getClass().getDeclaredFields()) {
                if (f.isAnnotationPresent(ChkPointInput.class)) {
                    f.setAccessible(true);
                    inputs.put(f.getName(), f.get(cp));
                }
            }
            currClass = currClass.getSuperclass();
        } while (currClass != Object.class && currClass != null);
    }
}
