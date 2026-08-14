package org.zstack.storage.snapshot.group;

import org.zstack.header.storage.snapshot.group.VolumeSnapshotGroupInventory;

import java.util.List;

/**
 * Created by LiangHanYu on 2022/7/8 16:44
 */
public interface MemorySnapshotGroupReferenceFactory {
    String getReferenceResourceType();

    List<VolumeSnapshotGroupInventory> getVolumeSnapshotGroupReferenceList(String ResourceUuid);
}
