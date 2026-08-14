package org.zstack.header.vm;

import org.zstack.header.message.NeedJsonSchema;

import java.util.Date;

/**
 * Created by lining on 2018/11/27.
 */
public class VmNicQosCanonicalEvents {
    public static final String VM_NIC_QOS_CHANGE_PATH = "/vmNic/qos/change";

    @NeedJsonSchema
    public static class VmNicQosEventData {
        private VmNicInventory inventory;
        private Date date = new Date();
        private String currentStatus;
        private String accountUuid;
        private Long bandwidthIn;
        private Long bandwidthOut;

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

        public Long getBandwidthIn() {
            return bandwidthIn;
        }

        public void setBandwidthIn(Long bandwidthIn) {
            this.bandwidthIn = bandwidthIn;
        }

        public Long getBandwidthOut() {
            return bandwidthOut;
        }

        public void setBandwidthOut(Long bandwidthOut) {
            this.bandwidthOut = bandwidthOut;
        }
    }
}
