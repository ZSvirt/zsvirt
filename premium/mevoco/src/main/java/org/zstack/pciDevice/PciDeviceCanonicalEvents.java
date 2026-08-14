package org.zstack.pciDevice;

import org.zstack.header.message.NeedJsonSchema;

import java.util.Date;

public class PciDeviceCanonicalEvents {
    public static final String PCIDEVICE_FULL_STATE_CHANGED_PATH = "/pcidevice/state/change";

    @NeedJsonSchema
    public static class PciDeviceStateChangedData {
        private String pciDeviceUuid;
        private String description;
        private String vmUuid;
        private String hostUuid;
        private String status;
        private PciDeviceInventory inventory;
        private Date date = new Date();

        public String getPciDeviceUuid() {
            return pciDeviceUuid;
        }

        public void setPciDeviceUuid(String pciDeviceUuid) {
            this.pciDeviceUuid = pciDeviceUuid;
        }

        public String getHostUuid() {
            return hostUuid;
        }

        public void setHostUuid(String hostUuid) {
            this.hostUuid = hostUuid;
        }

        public PciDeviceInventory getInventory() {
            return inventory;
        }

        public void setInventory(PciDeviceInventory inventory) {
            this.inventory = inventory;
        }

        public Date getDate() {
            return date;
        }

        public void setDate(Date date) {
            this.date = date;
        }

        public String getVmUuid() {
            return vmUuid;
        }

        public void setVmUuid(String vmUuid) {
            this.vmUuid = vmUuid;
        }

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }
    }
}
