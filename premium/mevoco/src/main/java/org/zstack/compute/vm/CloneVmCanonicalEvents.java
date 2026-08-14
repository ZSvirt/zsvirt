package org.zstack.compute.vm;

import org.zstack.core.Platform;
import org.zstack.core.cloudbus.EventFacade;
import org.zstack.header.storage.snapshot.VolumeSnapshotInventory;
import org.zstack.header.storage.snapshot.group.VolumeSnapshotGroupRefInventory;

import java.util.List;

public class CloneVmCanonicalEvents {
    public static String VM_INNER_SNAPSHOT_CREATED = "/vm/inner-snapshot-created";

    public static class VmInnerSnapshotCreated {
        public String vmInstanceUuid;
        public List<VolumeSnapshotGroupRefInventory> snapshotInventoryList;
        public VolumeSnapshotInventory volumeSnapshot;
        public String primaryStorageUuid;

        public void setVmInstanceUuid(String vmInstanceUuid) {
            this.vmInstanceUuid = vmInstanceUuid;
        }

        public String getVmInstanceUuid() {
            return vmInstanceUuid;
        }

        public void setSnapshotInventoryList(List<VolumeSnapshotGroupRefInventory> snapshotInventoryList) {
            this.snapshotInventoryList = snapshotInventoryList;
        }

        public List<VolumeSnapshotGroupRefInventory> getSnapshotInventoryList() {
            return snapshotInventoryList;
        }

        public void setVolumeSnapshot(VolumeSnapshotInventory volumeSnapshot) {
            this.volumeSnapshot = volumeSnapshot;
        }

        public VolumeSnapshotInventory getVolumeSnapshot() {
            return volumeSnapshot;
        }

        public void setPrimaryStorageUuid(String primaryStorageUuid) {
            this.primaryStorageUuid = primaryStorageUuid;
        }

        public String getPrimaryStorageUuid() {
            return primaryStorageUuid;
        }

        public void fire() {
            EventFacade evtf = Platform.getComponentLoader().getComponent(EventFacade.class);
            evtf.fire(VM_INNER_SNAPSHOT_CREATED, this);
        }
    }
}
