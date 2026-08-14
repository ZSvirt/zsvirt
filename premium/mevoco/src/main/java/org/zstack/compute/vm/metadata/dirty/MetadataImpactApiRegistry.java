package org.zstack.compute.vm.metadata.dirty;

import org.zstack.core.Platform;
import org.zstack.header.message.APIMessage;
import org.zstack.header.vm.metadata.MetadataImpact;
import org.zstack.header.vm.metadata.VmUuidFromApiResolver;
import org.zstack.utils.BeanUtils;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

class MetadataImpactApiRegistry {
    private static final CLogger logger = Utils.getLogger(MetadataImpactApiRegistry.class);

    private final Set<Class<? extends APIMessage>> trackedApiClasses = ConcurrentHashMap.newKeySet();
    private final Map<Class<? extends APIMessage>, ApiResolverMeta> apiClassToResolverMeta = new ConcurrentHashMap<>();

    private final Map<String, VmUuidFromApiResolver> resolversByBeanName = new ConcurrentHashMap<>();
    private final Set<String> failedResolverBeanNames = ConcurrentHashMap.newKeySet();

    private final Map<String, Method> fieldGettersByClassAndName = new ConcurrentHashMap<>();
    private final Set<String> failedGetterCacheKeys = ConcurrentHashMap.newKeySet();

    /**
     * Scan all {@link MetadataImpact}-annotated API classes, resolve their resolvers and
     * field getters, and register them for runtime tracking.
     * Called once at startup.
     */
    void scan() {
        BeanUtils.reflections.getTypesAnnotatedWith(MetadataImpact.class).forEach(clz -> {
            if (!APIMessage.class.isAssignableFrom(clz)) {
                return;
            }

            @SuppressWarnings("unchecked")
            Class<? extends APIMessage> apiClass = (Class<? extends APIMessage>) clz;
            MetadataImpact impact = clz.getAnnotation(MetadataImpact.class);

            if (impact.value() == MetadataImpact.Impact.NONE) {
                return;
            }

            String resolverBeanName = impact.resolver();
            if (resolverBeanName == null || resolverBeanName.isEmpty()) {
                logger.error(String.format("[MetadataRegistry] @MetadataImpact on %s has impact=%s " +
                        "but resolver is empty, API will be ignored", clz.getName(), impact.value()));
                return;
            }

            String fieldName = impact.field();
            if (fieldName == null || fieldName.isEmpty()) {
                logger.error(String.format("[MetadataRegistry] @MetadataImpact on %s has impact=%s " +
                        "but field is empty, API will be ignored", clz.getName(), impact.value()));
                return;
            }

            VmUuidFromApiResolver resolver = lookupResolver(resolverBeanName, clz.getName());
            if (resolver == null) {
                return;
            }

            Method getter = findFieldGetter(apiClass, fieldName);
            if (getter == null) {
                logger.error(String.format("[MetadataRegistry] cannot find getter for field [%s] on %s, " +
                        "API will be ignored", fieldName, clz.getName()));
                return;
            }

            trackedApiClasses.add(apiClass);
            apiClassToResolverMeta.put(apiClass, new ApiResolverMeta(resolver, fieldName, getter));
            logger.info(String.format("[MetadataRegistry] registered @MetadataImpact API: %s " +
                    "(impact=%s, resolver=%s, field=%s)", clz.getName(), impact.value(), resolverBeanName, fieldName));
        });

        logger.info(String.format("[MetadataRegistry] scan complete: %d API classes registered", trackedApiClasses.size()));
    }

    Set<Class<? extends APIMessage>> getTrackedApiClasses() {
        return Collections.unmodifiableSet(trackedApiClasses);
    }

    ApiResolverMeta getResolverMeta(Class<?> apiClass) {
        return apiClassToResolverMeta.get(apiClass);
    }

    /**
     * Extract the field value from an API message and resolve it to VM UUIDs
     * via the configured resolver.
     */
    @SuppressWarnings("unchecked")
    List<String> extractVmUuidsFromApi(APIMessage msg, ApiResolverMeta meta) {
        Object fieldValue;
        try {
            fieldValue = meta.getter.invoke(msg);
        } catch (Exception e) {
            logger.warn(String.format("[MetadataRegistry] failed to invoke %s() on %s: %s",
                    meta.getter.getName(), msg.getClass().getSimpleName(), e.getMessage()));
            return Collections.emptyList();
        }

        if (fieldValue == null) {
            return Collections.emptyList();
        }

        if (fieldValue instanceof List) {
            List<String> vmUuids;
            try {
                vmUuids = meta.resolver.batchResolveVmUuids((List<String>) fieldValue);
            } catch (Exception e) {
                logger.warn(String.format("[MetadataRegistry] resolver %s threw exception for %s.%s: %s",
                        meta.resolver.getClass().getSimpleName(), msg.getClass().getSimpleName(),
                        meta.fieldName, e.getMessage()));
                return Collections.emptyList();
            }
            if (vmUuids == null || vmUuids.isEmpty()) {
                logger.debug(String.format("[MetadataRegistry] resolver %s returned empty vmUuids for %s.%s=%s",
                        meta.resolver.getClass().getSimpleName(), msg.getClass().getSimpleName(),
                        meta.fieldName, fieldValue));
                return Collections.emptyList();
            }
            return vmUuids;
        } else if (fieldValue instanceof String) {
            String vmUuid;
            try {
                vmUuid = meta.resolver.resolveVmUuid((String) fieldValue);
            } catch (Exception e) {
                logger.warn(String.format("[MetadataRegistry] resolver %s threw exception for %s.%s: %s",
                        meta.resolver.getClass().getSimpleName(), msg.getClass().getSimpleName(),
                        meta.fieldName, e.getMessage()));
                return Collections.emptyList();
            }
            if (vmUuid == null) {
                logger.debug(String.format("[MetadataRegistry] resolver %s returned null vmUuid for %s.%s=%s",
                        meta.resolver.getClass().getSimpleName(), msg.getClass().getSimpleName(),
                        meta.fieldName, fieldValue));
                return Collections.emptyList();
            }
            return Collections.singletonList(vmUuid);
        } else {
            logger.warn(String.format("[MetadataRegistry] field [%s] on %s returned unexpected type %s",
                    meta.fieldName, msg.getClass().getSimpleName(), fieldValue.getClass().getName()));
            return Collections.emptyList();
        }
    }

    private VmUuidFromApiResolver lookupResolver(String beanName, String apiClassName) {
        VmUuidFromApiResolver resolver = resolversByBeanName.get(beanName);
        if (resolver != null) {
            return resolver;
        }
        if (failedResolverBeanNames.contains(beanName)) {
            logger.error(String.format("[MetadataRegistry] resolver bean [%s] previously failed for %s, " +
                    "API will be ignored", beanName, apiClassName));
            return null;
        }
        try {
            resolver = Platform.getComponentLoader().getComponentByBeanName(beanName);
            resolversByBeanName.put(beanName, resolver);
            return resolver;
        } catch (Exception e) {
            logger.error(String.format("[MetadataRegistry] resolver bean [%s] not found: %s",
                    beanName, e.getMessage()));
            failedResolverBeanNames.add(beanName);
            return null;
        }
    }

    private Method findFieldGetter(Class<?> clz, String fieldName) {
        String getterName = "get" + Character.toUpperCase(fieldName.charAt(0)) + fieldName.substring(1);
        String cacheKey = clz.getName() + "#" + getterName;

        Method cached = fieldGettersByClassAndName.get(cacheKey);
        if (cached != null) {
            return cached;
        }
        if (failedGetterCacheKeys.contains(cacheKey)) {
            return null;
        }

        try {
            Method method = clz.getMethod(getterName);
            fieldGettersByClassAndName.put(cacheKey, method);
            return method;
        } catch (NoSuchMethodException e) {
            logger.warn(String.format("[MetadataRegistry] no method %s() on %s", getterName, clz.getName()));
            failedGetterCacheKeys.add(cacheKey);
            return null;
        }
    }

    static class ApiResolverMeta {
        final VmUuidFromApiResolver resolver;
        final String fieldName;
        final Method getter;

        ApiResolverMeta(VmUuidFromApiResolver resolver, String fieldName, Method getter) {
            this.resolver = resolver;
            this.fieldName = fieldName;
            this.getter = getter;
        }
    }
}
