package org.zstack.header.storage.primary;

import org.zstack.header.core.ReturnValueCompletion;

/**
 * Extension point for discovering unmanaged (strange) primary storage.
 * Each primary storage type can implement this to discover its own PS instances
 * that exist on hosts but are not managed by the platform.
 */
public interface PrimaryStorageDiscoverExtensionPoint {
    void discoverStrangePrimaryStorage(String clusterUuid, ReturnValueCompletion<PrimaryStorageDiscoveryResult> completion);
}
