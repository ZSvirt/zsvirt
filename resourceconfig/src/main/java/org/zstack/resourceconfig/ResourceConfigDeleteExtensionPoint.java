package org.zstack.resourceconfig;

/**
 * Created by MaJin on 2019/2/26.
 */
public interface ResourceConfigDeleteExtensionPoint {
    void deleteResourceConfig(ResourceConfig config, String resourceUuid, String resourceType, String originValue);
}
