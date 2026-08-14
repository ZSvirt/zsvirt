package org.zstack.header.storage.primary;

import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.message.NeedJsonSchema;

/**
 * Created by xing5 on 2016/3/23.
 */
public class PrimaryStorageCanonicalEvent {
    public static final String PRIMARY_STORAGE_DELETED_PATH = "/primaryStorage/delete";
    public static final String PRIMARY_STORAGE_STATUS_CHANGED_PATH = "/primaryStorage/status/change";
    public static final String PRIMARY_STORAGE_STATE_CHANGED_PATH = "/primaryStorage/state/change";
    public static final String PRIMARY_STORAGE_HOST_STATUS_CHANGED_PATH = "/primaryStorage/host/status/change";
    public static final String PRIMARY_STORAGE_DISCONNECTED = "/primaryStorage/disconnected";

    public static class DisconnectedData {
        private String primaryStorageUuid;
        private ErrorCode reason;

        public String getPrimaryStorageUuid() {
            return primaryStorageUuid;
        }

        public void setPrimaryStorageUuid(String primaryStorageUuid) {
            this.primaryStorageUuid = primaryStorageUuid;
        }

        public ErrorCode getReason() {
            return reason;
        }

        public void setReason(ErrorCode reason) {
            this.reason = reason;
        }
    }

    @NeedJsonSchema
    public static class PrimaryStorageDeletedData {
        private String primaryStorageUuid;
        private PrimaryStorageInventory inventory;

        public String getPrimaryStorageUuid() {
            return primaryStorageUuid;
        }

        public void setPrimaryStorageUuid(String primaryStorageUuid) {
            this.primaryStorageUuid = primaryStorageUuid;
        }

        public PrimaryStorageInventory getInventory() {
            return inventory;
        }

        public void setInventory(PrimaryStorageInventory inventory) {
            this.inventory = inventory;
        }
    }

    @NeedJsonSchema
    public static class PrimaryStorageStatusChangedData {
        private String primaryStorageUuid;
        private String oldStatus;
        private String newStatus;
        private PrimaryStorageInventory inventory;

        public String getPrimaryStorageUuid() {
            return primaryStorageUuid;
        }

        public void setPrimaryStorageUuid(String primaryStorageUuid) {
            this.primaryStorageUuid = primaryStorageUuid;
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

        public PrimaryStorageInventory getInventory() {
            return inventory;
        }

        public void setInventory(PrimaryStorageInventory inventory) {
            this.inventory = inventory;
        }
    }

    @NeedJsonSchema
    public static class PrimaryStorageStateChangedData{
        private String primaryStorageUuid;
        private PrimaryStorageState oldState;
        private PrimaryStorageState newState;
        private PrimaryStorageInventory inventory;

        public String getPrimaryStorageUuid() {
            return primaryStorageUuid;
        }

        public void setPrimaryStorageUuid(String primaryStorageUuid) {
            this.primaryStorageUuid = primaryStorageUuid;
        }

        public PrimaryStorageState getOldState() {
            return oldState;
        }

        public void setOldState(PrimaryStorageState oldState) {
            this.oldState = oldState;
        }

        public PrimaryStorageState getNewState() {
            return newState;
        }

        public void setNewState(PrimaryStorageState newState) {
            this.newState = newState;
        }

        public PrimaryStorageInventory getInventory() {
            return inventory;
        }

        public void setInventory(PrimaryStorageInventory inventory) {
            this.inventory = inventory;
        }
    }

    @NeedJsonSchema
    public static class PrimaryStorageHostStatusChangeData{
        private String primaryStorageUuid;
        private String hostUuid;
        private PrimaryStorageHostStatus oldStatus;
        private PrimaryStorageHostStatus newStatus;
        private ErrorCode reason;

        public void setHostUuid(String hostUuid) {
            this.hostUuid = hostUuid;
        }

        public String getHostUuid() {
            return hostUuid;
        }

        public void setNewStatus(PrimaryStorageHostStatus newStatus) {
            this.newStatus = newStatus;
        }

        public PrimaryStorageHostStatus getNewStatus() {
            return newStatus;
        }

        public void setOldStatus(PrimaryStorageHostStatus oldStatus) {
            this.oldStatus = oldStatus;
        }

        public PrimaryStorageHostStatus getOldStatus() {
            return oldStatus;
        }

        public void setPrimaryStorageUuid(String primaryStorageUuid) {
            this.primaryStorageUuid = primaryStorageUuid;
        }

        public String getPrimaryStorageUuid() {
            return primaryStorageUuid;
        }

        public ErrorCode getReason() {
            return reason;
        }

        public void setReason(ErrorCode reason) {
            this.reason = reason;
        }
    }
}
