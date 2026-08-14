package org.zstack.header.volume;

import org.zstack.header.message.NeedJsonSchema;
import org.zstack.header.vm.VmInstanceInventory;

import java.util.Date;

/**
 * Created by xing5 on 2016/3/12.
 */
public class VolumeCanonicalEvents {
    public static final String VOLUME_STATUS_CHANGED_PATH = "/volume/status/change";
    public static final String VOLUME_CONFIG_CHANGED_PATH = "/volume/config/change";
    public static final String VOLUME_ATTACHED_VM_PATH = "/volume/attached/vm";

    @NeedJsonSchema
    public static class VolumeConfigChangedData {
        private VolumeInventory inventory;
        private String accoutUuid;
        private Date date = new Date();

        public VolumeInventory getInventory() {
            return inventory;
        }

        public void setInventory(VolumeInventory inventory) {
            this.inventory = inventory;
        }

        public String getAccoutUuid() {
            return accoutUuid;
        }

        public void setAccoutUuid(String accoutUuid) {
            this.accoutUuid = accoutUuid;
        }
        public Date getDate() {
            return date;
        }

        public void setDate(Date date) {
            this.date = date;
        }
    }

    @NeedJsonSchema
    public static class VolumeStatusChangedData {
        private String volumeUuid;
        private String oldStatus;
        private String newStatus;
        private VolumeInventory inventory;
        private Date date = new Date();
        private String accountUuid;

        public String getVolumeUuid() {
            return volumeUuid;
        }

        public void setVolumeUuid(String volumeUuid) {
            this.volumeUuid = volumeUuid;
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

        public VolumeInventory getInventory() {
            return inventory;
        }

        public void setInventory(VolumeInventory inventory) {
            this.inventory = inventory;
        }

        public Date getDate() {
            return date;
        }

        public void setDate(Date date) {
            this.date = date;
        }

        public String getAccountUuid() {
            return accountUuid;
        }

        public void setAccountUuid(String accountUuid) {
            this.accountUuid = accountUuid;
        }
    }

    @NeedJsonSchema
    public static class VolumeAttachedData {
        private String volumeUuid;
        private VmInstanceInventory vmInventory;
        private VolumeInventory inventory;

        public String getVolumeUuid() {
            return volumeUuid;
        }

        public void setVolumeUuid(String volumeUuid) {
            this.volumeUuid = volumeUuid;
        }

        public VolumeInventory getInventory() {
            return inventory;
        }

        public void setInventory(VolumeInventory inventory) {
            this.inventory = inventory;
        }

        public VmInstanceInventory getVmInventory() {
            return vmInventory;
        }

        public void setVmInventory(VmInstanceInventory vmInventory) {
            this.vmInventory = vmInventory;
        }
    }
}
