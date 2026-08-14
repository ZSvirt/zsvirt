package org.zstack.storage.snapshot;

import org.zstack.core.Platform;
import org.zstack.core.cloudbus.EventFacade;
import org.zstack.header.storage.snapshot.VolumeSnapshotInventory;

public class SnapshotCanonicalEvents {
    public static String INNER_SNAPSHOT_CREATED = "/primary-storage/inner-snapshot-created";

    public static class InnerVolumeSnapshotCreated {
        public String volumeUuid;
        public VolumeSnapshotInventory snapshot;
        public String primaryStorageUuid;

        public InnerVolumeSnapshotCreated(String volumeUuid, String primaryStorageUuid, VolumeSnapshotInventory snapshot) {
            this.volumeUuid = volumeUuid;
            this.primaryStorageUuid = primaryStorageUuid;
            this.snapshot = snapshot;
        }

        public void fire() {
            EventFacade evtf = Platform.getComponentLoader().getComponent(EventFacade.class);
            evtf.fire(INNER_SNAPSHOT_CREATED, this);
        }
    }
}