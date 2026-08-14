package org.zstack.header.vm.metadata;

public interface VmMetadataPathBuildExtensionPoint {
    String getPrimaryStorageType();
    String buildVmMetadataPath(String primaryStorageUuid, String vmInstanceUuid);
    String buildMetadataDir(String primaryStorageUuid);
    String validateMetadataPath(String primaryStorageUuid, String path);
    default boolean requireHostForCleanup() {
        return false;
    }
}
