package org.zstack.header.vo;

import org.apache.commons.lang.StringUtils;
import org.zstack.header.core.StaticInit;
import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.header.search.Inventory;
import org.zstack.utils.BeanUtils;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.*;

public interface ToInventory {
    CLogger logger = Utils.getLogger(ToInventory.class);

    class InventoryMetadata {
        Class inventoryClass;
        Method valueOf;
        Method valueOfCollection;

        @StaticInit
        static void staticInit() {
            List<String> errors = new ArrayList<>();

            BeanUtils.reflections.getTypesAnnotatedWith(Inventory.class).stream().filter(i->i.isAnnotationPresent(Inventory.class)).forEach(clz-> {
                String collectionMethodName = "valueOf";
                Inventory at = clz.getAnnotation(Inventory.class);
                if (!at.collectionValueOfMethod().equals("")) {
                    collectionMethodName = at.collectionValueOfMethod();
                }

                InventoryMetadata m = new InventoryMetadata();
                m.inventoryClass = clz;
                Class tmp = clz;
                while (tmp != Object.class) {
                    for (Method method : tmp.getDeclaredMethods()) {
                        if (m.valueOf == null && method.getName().equals("valueOf")
                                && !Collection.class.isAssignableFrom(method.getReturnType())
                                && Modifier.isStatic(method.getModifiers())
                                && method.getParameterTypes().length == 1
                                && method.getParameterTypes()[0].isAssignableFrom(at.mappingVOClass())) {
                            m.valueOf = method;
                        } else if (m.valueOfCollection == null && method.getName().equals(collectionMethodName)
                                && Collection.class.isAssignableFrom(method.getReturnType())
                                && method.getParameterTypes().length == 1
                                && Collection.class.isAssignableFrom(method.getParameterTypes()[0])
                                && Modifier.isStatic(method.getModifiers())) {
                            m.valueOfCollection = method;
                        }
                    }

                    if (m.valueOf != null && m.valueOfCollection != null) {
                        break;
                    }

                    tmp = tmp.getSuperclass();
                }

                if (m.valueOf == null)  {
                    errors.add(String.format("class[%s] annotated by @Inventory but not having a static method valueOf", clz));
                } else {
                    m.valueOf.setAccessible(true);
                }

                if (m.valueOfCollection == null) {
                    errors.add(String.format("class[%s] annotated by @Inventory but not having a static collection method %s", clz, collectionMethodName));
                } else {
                    m.valueOfCollection.setAccessible(true);
                }

                inventoryMetadata.put(at.mappingVOClass(), m);
            });

            if (!errors.isEmpty()) {
                throw new CloudRuntimeException(StringUtils.join(errors, "\n"));
            }
        }
    }

    Map<Class, InventoryMetadata> inventoryMetadata = new HashMap<>();

    static Object toInventory(Object vo) {
        InventoryMetadata m = null;
        Class clz = vo.getClass();
        while (clz != Object.class) {
            m = inventoryMetadata.get(clz);
            if (m != null) {
                break;
            }

            clz = clz.getSuperclass();
        }

        if (m == null) {
            throw new CloudRuntimeException(String.format("no Inventory class for %s", vo.getClass()));
        }

        try {
            return m.valueOf.invoke(null, vo);
        } catch (Exception e) {
            logger.warn(String.format("unable to convert class[%s] to inventory[%s]", vo.getClass(), m.inventoryClass));
            throw new CloudRuntimeException(e);
        }
    }

    default <T> T toInventory() {
        return null;
    }
}
