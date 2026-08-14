package org.zstack.header.vo;

import org.zstack.header.core.StaticInit;
import org.zstack.header.identity.OwnedByAccount;
import org.zstack.utils.BeanUtils;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class ResourceTypeMetadata {
    public static Map<Class, Class> concreteBaseTypeMapping = new HashMap<>();
    public static Set<Class> allBaseResourceTypes = new HashSet<>();
    public static Map<String, Class<?>> nameResourceMap = new HashMap<>();

    @StaticInit
    static void staticInit() {
        BeanUtils.reflections.getSubTypesOf(ResourceVO.class).forEach(resourceType -> {
            nameResourceMap.put(resourceType.getSimpleName(), resourceType);

            BaseResource at = resourceType.getAnnotation(BaseResource.class);
            if (at != null) {
                BeanUtils.reflections.getSubTypesOf(resourceType)
                        .forEach(subType -> concreteBaseTypeMapping.put(subType, resourceType));
            }

            if (OwnedByAccount.class.isAssignableFrom(resourceType)) {
                allBaseResourceTypes.add(getBaseResourceTypeFromConcreteType(resourceType));
            }
        });
    }

    public static Set<Class> getAllBaseTypes() {
        return allBaseResourceTypes;
    }

    public static Class getBaseResourceTypeFromConcreteType(Class clz) {
        Class bclz = concreteBaseTypeMapping.get(clz);
        return bclz == null ? clz : bclz;
    }

    public static Class<?> resourceTypeForName(String resourceType) {
        return nameResourceMap.get(resourceType);
    }
}
