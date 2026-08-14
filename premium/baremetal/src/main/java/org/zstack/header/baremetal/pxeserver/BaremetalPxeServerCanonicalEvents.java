package org.zstack.header.baremetal.pxeserver;

/**
 * Created by GuoYi on 2018-10-26.
 */
public class BaremetalPxeServerCanonicalEvents {
    public static final String CREATE_BAREMETAL_PXE_SERVER = "/baremetal/pxeserver/create";
    public static final String DELETE_BAREMETAL_PXE_SERVER = "/baremetal/pxeserver/delete";

    public static final String BAREMETAL_PXE_SERVER_STATUS_CHANGE = "/baremetal/pxeserver/status/change";

    public static class BaremetalPxeServerStatusChangeData {
        private String oldStatus;
        private String newStatus;
        private String pxeServerUuid;
        private String pxeServerHostName;

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

        public String getPxeServerUuid() {
            return pxeServerUuid;
        }

        public void setPxeServerUuid(String pxeServerUuid) {
            this.pxeServerUuid = pxeServerUuid;
        }

        public String getPxeServerHostName() {
            return pxeServerHostName;
        }

        public void setPxeServerHostName(String pxeServerHostName) {
            this.pxeServerHostName = pxeServerHostName;
        }
    }
}
