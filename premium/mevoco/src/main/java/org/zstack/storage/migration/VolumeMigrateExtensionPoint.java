package org.zstack.storage.migration;

import org.zstack.header.core.Completion;
import org.zstack.header.volume.VolumeInventory;

import java.util.List;
import java.util.Map;

public interface VolumeMigrateExtensionPoint {
    void preMigrateVolumes(List<VolumeInventory> oldVolumes, String srcHostUuid, String dstHostUuid, Completion completion);
    void afterMigrateVolumes(List<VolumeInventory> oldVolumes, String srcHostUuid, String dstHostUuid, Completion completion);
    void failedToMigrateVolumes(List<VolumeInventory> oldVolumes, String srcHostUuid, String dstHostUuid, Completion completion);

    void preCopyVolumes(List<VolumeInventory> oldVolumes, List<VolumeInventory> newVolumes, Map<String, String> old2NewVolumeUuids, String dstHostUuid, Completion completion);
    void afterCopyVolumes(List<VolumeInventory> oldVolumes, String srcHostUuid);
}
