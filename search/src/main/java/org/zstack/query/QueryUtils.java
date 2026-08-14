package org.zstack.query;

import org.zstack.header.search.Inventory;
import org.zstack.utils.FieldUtils;

import javax.persistence.Id;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

/**
 */
public class QueryUtils {
    private static Map<Class, String> primaryKeys = new HashMap<Class, String>();

    public static Class getEntityClassFromInventoryClass(Class invClz) {
        Inventory at = (Inventory) invClz.getAnnotation(Inventory.class);
        return at.mappingVOClass();
    }

    public static String getPrimaryKeyNameFromEntityClass(Class entityClass) {
        String priKey = primaryKeys.get(entityClass);
        if (priKey == null) {
            Field f = FieldUtils.getAnnotatedField(Id.class, entityClass);
            priKey = f.getName();
            primaryKeys.put(entityClass, priKey);
        }
        return priKey;
    }

    public static String getPrimaryKeyNameFromInventoryClass(Class invClz) {
        Class entityClass = getEntityClassFromInventoryClass(invClz);
        return getPrimaryKeyNameFromEntityClass(entityClass);
    }
}
