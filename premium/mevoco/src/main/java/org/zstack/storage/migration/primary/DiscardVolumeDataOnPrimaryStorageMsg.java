package org.zstack.storage.migration.primary;

import org.zstack.header.message.NeedReplyMessage;
import org.zstack.header.storage.snapshot.VolumeSnapshotInventory;
import org.zstack.header.volume.VolumeInventory;
import org.zstack.storage.migration.StorageMigrationMessage;

import java.util.List;
import java.util.Set;

public class DiscardVolumeDataOnPrimaryStorageMsg extends NeedReplyMessage implements StorageMigrationMessage {
    private VolumeInventory srcVolume;
    private List<VolumeSnapshotInventory> srcVolumeSnapshots;
    private Set<String> srcVolumeInstallPaths;
    private String type;
    private boolean migrateWithSnapshots = true;

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public boolean isMigrateWithSnapshots() {
        return migrateWithSnapshots;
    }

    public void setMigrateWithSnapshots(boolean migrateWithSnapshots) {
        this.migrateWithSnapshots = migrateWithSnapshots;
    }

    public VolumeInventory getSrcVolume() {
        return srcVolume;
    }

    public void setSrcVolume(VolumeInventory srcVolume) {
        this.srcVolume = srcVolume;
    }

    public List<VolumeSnapshotInventory> getSrcVolumeSnapshots() {
        return srcVolumeSnapshots;
    }

    public void setSrcVolumeSnapshots(List<VolumeSnapshotInventory> srcVolumeSnapshots) {
        this.srcVolumeSnapshots = srcVolumeSnapshots;
    }

    public Set<String> getSrcVolumeInstallPaths() {
        return srcVolumeInstallPaths;
    }

    public void setSrcVolumeInstallPaths(Set<String> srcVolumeInstallPaths) {
        this.srcVolumeInstallPaths = srcVolumeInstallPaths;
    }
}
