package org.zstack.network.service.vip;

import org.zstack.header.message.NeedJsonSchema;
import org.zstack.header.vm.VmInstanceInventory;

import java.util.Date;

/**
 * Created by lining on 2018/11/20.
 */
public class VipCanonicalEvents {
    public static final String VIP_CREATED_PATH = "/vip/vipCreated";
    public static final String VIP_DELETED_PATH = "/vip/vipDeleted";

    public static final String VIP_STATUS_DELETED = "deleted";
    public static final String VIP_STATUS_CREATED = "created";

    @NeedJsonSchema
    public static class VipEventData {
        private String vipUuid;
        private VipInventory inventory;
        private Date date = new Date();
        private String currentStatus;
        private String accountUuid;

        public VipInventory getInventory() {
            return inventory;
        }

        public void setInventory(VipInventory inventory) {
            this.inventory = inventory;
        }

        public Date getDate() {
            return date;
        }

        public void setDate(Date date) {
            this.date = date;
        }

        public String getVipUuid() {
            return vipUuid;
        }

        public void setVipUuid(String vipUuid) {
            this.vipUuid = vipUuid;
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
