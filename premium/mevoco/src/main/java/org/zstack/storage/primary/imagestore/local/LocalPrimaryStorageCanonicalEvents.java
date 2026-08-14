package org.zstack.storage.primary.imagestore.local;

import org.zstack.core.Platform;
import org.zstack.core.cloudbus.EventFacade;
import org.zstack.header.storage.snapshot.VolumeSnapshotInventory;

public class LocalPrimaryStorageCanonicalEvents {
    public static String IMAGE_INNER_SNAPSHOT_CREATED = "/local/primary-storage/image/inner-snapshot-created";

    public static class ImageInnerSnapshotCreated {
        public String imageUuid;
        public VolumeSnapshotInventory snapshot;
        public String primaryStorageUuid;

        public void setImageUuid(String imageUuid) {
            this.imageUuid = imageUuid;
        }

        public String getImageUuid() {
            return imageUuid;
        }

        public void setSnapshot(VolumeSnapshotInventory snapshot) {
            this.snapshot = snapshot;
        }

        public VolumeSnapshotInventory getSnapshot() {
            return snapshot;
        }

        public void setPrimaryStorageUuid(String primaryStorageUuid) {
            this.primaryStorageUuid = primaryStorageUuid;
        }

        public String getPrimaryStorageUuid() {
            return primaryStorageUuid;
        }

        public void fire() {
            EventFacade evtf = Platform.getComponentLoader().getComponent(EventFacade.class);
            evtf.fire(IMAGE_INNER_SNAPSHOT_CREATED, this);
        }
    }
}
