package org.zstack.storage.primary.sharedblock;

import org.zstack.core.Platform;
import org.zstack.core.cloudbus.EventFacade;
import org.zstack.header.storage.snapshot.VolumeSnapshotInventory;

public class SharedBlockPrimaryStorageCanonicalEvents {
    public static String IMAGE_INNER_SNAPSHOT_CREATED = "/shared-block/primary-storage/image/inner-snapshot-created";

    public static class ImageInnerSnapshotCreated {
        public String imageUuid;
        public VolumeSnapshotInventory snapshot;
        public String primaryStorageUuid;

        public void fire() {
            EventFacade evtf = Platform.getComponentLoader().getComponent(EventFacade.class);
            evtf.fire(IMAGE_INNER_SNAPSHOT_CREATED, this);
        }
    }
}
