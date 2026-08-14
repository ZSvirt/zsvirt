package org.zstack.storage.snapshot;

public interface SnapshotDeletionExtensionPoint {
    String getHostUuidByResourceUuid(String primaryStorageUuid, String resUuid);
}
