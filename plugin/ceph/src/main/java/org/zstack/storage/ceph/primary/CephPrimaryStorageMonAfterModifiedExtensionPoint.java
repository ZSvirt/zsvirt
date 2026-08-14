package org.zstack.storage.ceph.primary;

public interface CephPrimaryStorageMonAfterModifiedExtensionPoint {
    void afterModified(CephPrimaryStorageVO vo);
}
