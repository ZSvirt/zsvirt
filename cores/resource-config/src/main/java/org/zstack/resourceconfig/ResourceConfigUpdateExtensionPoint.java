package org.zstack.resourceconfig;

/**
 * Created by MaJin on 2019/2/23.
 */
public interface ResourceConfigUpdateExtensionPoint {
    void updateResourceConfig(ResourceConfig config, String resourceUuid, String resourceType, String oldValue, String newValue);
}
