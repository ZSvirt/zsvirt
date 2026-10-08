package org.zstack.resourceconfig;

import org.zstack.core.config.GlobalConfig;
import org.zstack.core.config.ResourceConfigGetter;

import java.util.List;
import java.util.Map;

public interface ResourceConfigFacade extends ResourceConfigGetter {
    String SERVICE_ID = ResourceConfigConstant.SERVICE_ID;

    ResourceConfig getResourceConfig(String identity);

    /**
     * <p>This method will get the resource configuration from the specified type.
     *
     * <p>If the type does not have a resource configuration set,
     * it will return <code>null</code> and will NOT fetch
     * the configuration of the upper level resource or the global configuration.
     * </p>
     * @since zsv 4.1.0
     */
    <T> T getResourceConfigValueByResourceType(GlobalConfig gc, String resourceUuid, String type, Class<T> clz);

    <T> Map<String, T> getResourceConfigValues(GlobalConfig gc, List<String> resourceUuids, Class<T> clz);

    <T> Map<String, T> getResourceConfigValueByResourceUuids(GlobalConfig gc, List<String> resourceUuids, Class<T> clz);
}
