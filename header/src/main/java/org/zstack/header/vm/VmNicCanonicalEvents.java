package org.zstack.header.vm;

import org.zstack.header.message.NeedJsonSchema;

import java.util.Date;

/**
 * Created by lining on 2018/11/27.
 */
public class VmNicCanonicalEvents {
    public static final String VM_NIC_CREATED_PATH = "/vmNic/vmNicCreated";
    public static final String VM_NIC_DELETED_PATH = "/vmNic/vmNicDeleted";

    @NeedJsonSchema
    public static class VmNicEventData {
        private VmNicInventory inventory;
        private Date date = new Date();
        private String currentStatus;
        private String accountUuid;

        public VmNicInventory getInventory() {
            return inventory;
        }

        public void setInventory(VmNicInventory inventory) {
            this.inventory = inventory;
        }

        public Date getDate() {
            return date;
        }

        public void setDate(Date date) {
            this.date = date;
        }

        public String getCurrentStatus() {
            return currentStatus;
        }

        public void setCurrentStatus(String currentStatus) {
            this.currentStatus = currentStatus;
        }

        public String getAccountUuid() {
            return accountUuid;
        }

        public void setAccountUuid(String accountUuid) {
            this.accountUuid = accountUuid;
        }
    }
}
