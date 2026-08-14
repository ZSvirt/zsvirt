package org.zstack.header.volume;

import org.zstack.header.message.NeedJsonSchema;
import org.zstack.header.storage.snapshot.VolumeSnapshotInventory;

import java.util.Date;

/**
 * Created by camile on 2017/5/23.
 */
public class SnapShotCanonicalEvents {
    public static final String SNAPSHOT_STATUS_CHANGED_PATH = "/snap/status/change";

    @NeedJsonSchema
    public static class SnapShotStatusChangedData {
        private String snapShotUuid;
        private String oldStatus;
        private String newStatus;
        private VolumeSnapshotInventory inventory;
        private Date date = new Date();

        public String getSnapShotUuid() {
            return snapShotUuid;
        }

        public void setSnapShotUuid(String snapShotUuid) {
            this.snapShotUuid = snapShotUuid;
        }

        public String getOldStatus() {
            return oldStatus;
        }

        public void setOldStatus(String oldStatus) {
            this.oldStatus = oldStatus;
        }

        public String getNewStatus() {
            return newStatus;
        }

        public void setNewStatus(String newStatus) {
            this.newStatus = newStatus;
        }

        public VolumeSnapshotInventory getInventory() {
            return inventory;
        }

        public void setInventory(VolumeSnapshotInventory inventory) {
            this.inventory = inventory;
        }

        public Date getDate() {
            return date;
        }

        public void setDate(Date date) {
            this.date = date;
        }
    }
}
