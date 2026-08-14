package org.zstack.core.db;

import org.zstack.header.core.StaticInit;
import org.zstack.header.core.encrypt.EncryptColumn;
import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.utils.BeanUtils;
import org.zstack.utils.FieldUtils;

import javax.persistence.Entity;
import javax.persistence.Id;
import java.lang.reflect.Field;
import java.util.*;

public class EntityMetadata {
    private static class Metadata {
        Class entityClass;
        List<Field> fieldPrimaryKeys = new ArrayList<>();
        Set<String> allFieldNames = new HashSet<>();
        List<String> encryptColumns = new ArrayList<>();
    }

    private static Map<Class, Metadata> metadata = new HashMap<>();

    @StaticInit
    static void staticInit() {
        BeanUtils.reflections.getTypesAnnotatedWith(Entity.class).forEach(clz -> {
            Metadata m = new Metadata();
            m.entityClass = clz;
            FieldUtils.getAllFields(clz).forEach(f -> {
                m.allFieldNames.add(f.getName());
                if (f.isAnnotationPresent(Id.class)) {
                    m.fieldPrimaryKeys.add(f);
                }

                if (f.isAnnotationPresent(EncryptColumn.class)) {
                    m.encryptColumns.add(f.getName());
                }
            });

            metadata.put(clz, m);
        });
    }


    private static Metadata getMetadata(Class clz) {
        Metadata m = metadata.get(clz);
        if (m == null) {
            throw new CloudRuntimeException(String.format("cannot find metadata for entity[%s]", clz));
        }
        return m;
    }

    public static Field getPrimaryKeyField(Class clz) {
        Metadata m = getMetadata(clz);
        if (m.fieldPrimaryKeys.size() > 1) {
            throw new CloudRuntimeException(String.format("%s has multiple primary keys", clz));
        }

        return m.fieldPrimaryKeys.get(0);
    }

    public static List<String> getEncryptColumn(Class clz) {
        Metadata m = getMetadata(clz);
        if (m.encryptColumns.size() == 0) {
            return new ArrayList<>();
        }
        return m.encryptColumns;
    }
}
