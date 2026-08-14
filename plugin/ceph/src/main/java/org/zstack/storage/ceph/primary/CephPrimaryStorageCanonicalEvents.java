package org.zstack.storage.ceph.primary;

import org.zstack.core.Platform;
import org.zstack.core.cloudbus.EventFacade;
import org.zstack.header.storage.snapshot.VolumeSnapshotInventory;

public class CephPrimaryStorageCanonicalEvents {
    public static String IMAGE_INNER_SNAPSHOT_CREATED = "/ceph/primary-storage/image/inner-snapshot-created";

    public static class ImageInnerSnapshotCreated {
        public String imageUuid;
        public VolumeSnapshotInventory snapshot;
        public String primaryStorageUuid;

        public void setSnapshot(VolumeSnapshotInventory snapshot) {
            this.snapshot = snapshot;
        }

        public VolumeSnapshotInventory getSnapshot() {
            return this.snapshot;
        }

        public void fire() {
            EventFacade evtf = Platform.getComponentLoader().getComponent(EventFacade.class);
            evtf.fire(IMAGE_INNER_SNAPSHOT_CREATED, this);
        }
    }
}
