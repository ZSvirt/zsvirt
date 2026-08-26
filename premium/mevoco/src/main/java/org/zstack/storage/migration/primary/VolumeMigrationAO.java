package org.zstack.storage.migration.primary;

import org.zstack.header.configuration.PythonClassInventory;
import org.zstack.header.rest.SDK;
import org.zstack.header.vm.VmInstanceVO;
import org.zstack.header.volume.VolumeType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@SDK(sdkClassName = "VolumeMigrationAO")
@PythonClassInventory
public class VolumeMigrationAO {
    private String volumeUuid;
    private String dstPrimaryStorageUuid;
    private boolean withSnapshots = true;

    public static Map<String, String> toVolToDstMap(List<VolumeMigrationAO> specs) {
        Map<String, String> map = new HashMap<>();
        if (specs == null) {
            return map;
        }
        specs.forEach(s -> map.put(s.getVolumeUuid(), s.getDstPrimaryStorageUuid()));
        return map;
    }

    public static Map<String, Boolean> toVolToWithSnapshotsMap(List<VolumeMigrationAO> specs) {
        Map<String, Boolean> map = new HashMap<>();
        if (specs == null) {
            return map;
        }
        specs.forEach(s -> map.put(s.getVolumeUuid(), s.isWithSnapshots()));
        return map;
    }

    public static List<VolumeMigrationAO> fromLegacyDstPs(VmInstanceVO vm, List<VolumeMigrationAO> specs,
                                                          String dstPrimaryStorageUuid, boolean withDataVolumes,
                                                          boolean withSnapshots) {
        if (specs != null && !specs.isEmpty()) {
            return specs;
        }

        List<VolumeMigrationAO> legacy = new ArrayList<>();
        legacy.add(new VolumeMigrationAO(vm.getRootVolumeUuid(), dstPrimaryStorageUuid, withSnapshots));
        if (withDataVolumes) {
            vm.getAllDiskVolumes().stream()
                    .filter(v -> v.getType() == VolumeType.Data)
                    .forEach(v -> legacy.add(new VolumeMigrationAO(v.getUuid(), dstPrimaryStorageUuid, withSnapshots)));
        }
        return legacy;
    }

    public VolumeMigrationAO() {
    }

    public VolumeMigrationAO(String volumeUuid, String dstPrimaryStorageUuid) {
        this.volumeUuid = volumeUuid;
        this.dstPrimaryStorageUuid = dstPrimaryStorageUuid;
    }

    public VolumeMigrationAO(String volumeUuid, String dstPrimaryStorageUuid, boolean withSnapshots) {
        this.volumeUuid = volumeUuid;
        this.dstPrimaryStorageUuid = dstPrimaryStorageUuid;
        this.withSnapshots = withSnapshots;
    }

    public String getVolumeUuid() {
        return volumeUuid;
    }

    public void setVolumeUuid(String volumeUuid) {
        this.volumeUuid = volumeUuid;
    }

    public String getDstPrimaryStorageUuid() {
        return dstPrimaryStorageUuid;
    }

    public void setDstPrimaryStorageUuid(String dstPrimaryStorageUuid) {
        this.dstPrimaryStorageUuid = dstPrimaryStorageUuid;
    }

    public boolean isWithSnapshots() {
        return withSnapshots;
    }

    public void setWithSnapshots(boolean withSnapshots) {
        this.withSnapshots = withSnapshots;
    }
}
