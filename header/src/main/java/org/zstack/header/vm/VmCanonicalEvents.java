package org.zstack.header.vm;

import org.zstack.header.message.NeedJsonSchema;
import org.zstack.header.errorcode.ErrorCode;
import java.util.Date;
import java.util.List;
import java.time.LocalDateTime;

/**
 * Created by frank on 3/4/2016.
 */
public class VmCanonicalEvents {
    public static final String VM_LIBVIRT_REPORT_SHUTDOWN = "/vm/libvirtreportshutdown";
    public static final String VM_FULL_STATE_CHANGED_PATH = "/vm/state/change";
    public static final String VM_INSTANCE_OFFERING_CHANGED_PATH = "/vm/instanceoffering/change";
    public static final String VM_CONFIG_CHANGED_PATH = "/vm/config/change";
    public static final String VM_LIBVIRT_REPORT_REBOOT = "/vm/libvirtReportReboot";
    public static final String VM_LIBVIRT_REPORT_START = "/vm/libvirtReportStart";
    public static final String VM_LIBVIRT_REPORT_CRASH = "/vm/libvirtReportCrash";
    public static final String VM_NIC_INFO_CHANGED_PATH = "/vm/nicinfo/change";
    public static final String VM_NIC_INFO_DUPLICATE_PATH = "/vm/nicinfo/duplicate";
    public static final String VM_NIC_INFO_IPRANGE_CONFLICT_PATH = "/vm/nicinfo/iprangeConflict";
    public static final String VM_HOST_FILE_CHANGED_PATH = "/vm/hostfile/changed";
    public static final String VM_USERDATA_ENCRYPTION_DEGRADED_PATH = "/vm/userdata/encryption/degraded";

    @NeedJsonSchema
    public static class VmCrashReportData {
        private String vmUuid;
        private LocalDateTime time = LocalDateTime.now();
        private ErrorCode reason;

        public ErrorCode getReason() {
            return reason;
        }

        public void setReason(ErrorCode reason) {
            this.reason = reason;
        }

        public String getVmUuid() {
            return vmUuid;
        }

        public void setVmUuid(String vmUuid) {
            this.vmUuid = vmUuid;
        }

        public LocalDateTime getTime() {
            return time;
        }

        public void setTime(LocalDateTime time) {
            this.time = time;
        }
    }

    @NeedJsonSchema
    public static class VmConfigChangedData {
        private String vmUuid;
        private VmInstanceInventory inv;
        private Date date = new Date();
        private String accountUuid;

        public String getVmUuid() {
            return vmUuid;
        }

        public void setVmUuid(String vmUuid) {
            this.vmUuid = vmUuid;
        }

        public VmInstanceInventory getInv() {
            return inv;
        }

        public void setInv(VmInstanceInventory inv) {
            this.inv = inv;
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

        public void setAccoundUuid(String accountUuid) {
            this.accountUuid = accountUuid;
        }
    }

    @NeedJsonSchema
    public static class InstanceOfferingChangedData {
        private String vmUuid;
        private String oldInstanceOfferingUuid;
        private String oldInstanceOfferingInventory;
        private String newInstanceOfferingUuid;
        private String newInstanceOfferingInventory;
        private Date date = new Date();

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

        public String getOldInstanceOfferingUuid() {
            return oldInstanceOfferingUuid;
        }

        public void setOldInstanceOfferingUuid(String oldInstanceOfferingUuid) {
            this.oldInstanceOfferingUuid = oldInstanceOfferingUuid;
        }

        public String getOldInstanceOfferingInventory() {
            return oldInstanceOfferingInventory;
        }

        public void setOldInstanceOfferingInventory(String oldInstanceOfferingInventory) {
            this.oldInstanceOfferingInventory = oldInstanceOfferingInventory;
        }

        public String getNewInstanceOfferingUuid() {
            return newInstanceOfferingUuid;
        }

        public void setNewInstanceOfferingUuid(String newInstanceOfferingUuid) {
            this.newInstanceOfferingUuid = newInstanceOfferingUuid;
        }

        public String getNewInstanceOfferingInventory() {
            return newInstanceOfferingInventory;
        }

        public void setNewInstanceOfferingInventory(String newInstanceOfferingInventory) {
            this.newInstanceOfferingInventory = newInstanceOfferingInventory;
        }
    }

    @NeedJsonSchema
    public static class VmStateChangedData {
        private String vmUuid;
        private String oldState;
        private String newState;
        private VmInstanceInventory inventory;
        private Date date = new Date();

        public VmInstanceInventory getInventory() {
            return inventory;
        }

        public void setInventory(VmInstanceInventory inventory) {
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

        public String getOldState() {
            return oldState;
        }

        public void setOldState(String oldState) {
            this.oldState = oldState;
        }

        public String getNewState() {
            return newState;
        }

        public void setNewState(String newState) {
            this.newState = newState;
        }
    }

    @NeedJsonSchema
    public static class VmInternalIpChangedData {
        private String vmUuid;
        private String l3NetworkUuid;
        private String oldInternalIp;
        private String newInternalIp;
        private String relateResourceUuid;

        public String getVmUuid() {
            return vmUuid;
        }

        public void setVmUuid(String vmUuid) {
            this.vmUuid = vmUuid;
        }

        public String getL3NetworkUuid() {
            return l3NetworkUuid;
        }

        public void setL3NetworkUuid(String l3NetworkUuid) {
            this.l3NetworkUuid = l3NetworkUuid;
        }

        public String getOldInternalIp() {
            return oldInternalIp;
        }

        public void setOldInternalIp(String oldInternalIp) {
            this.oldInternalIp = oldInternalIp;
        }

        public String getNewInternalIp() {
            return newInternalIp;
        }

        public void setNewInternalIp(String newInternalIp) {
            this.newInternalIp = newInternalIp;
        }

        public String getRelateResourceUuid() {
            return relateResourceUuid;
        }

        public void setRelateResourceUuid(String relateResourceUuid) {
            this.relateResourceUuid = relateResourceUuid;
        }
    }

    @NeedJsonSchema
    public static class VmInternalIpDuplicateData {
        private String vmUuid;
        private String l3NetworkUuid;
        private String internalIp;
        private String relateResourceUuid;

        public String getVmUuid() {
            return vmUuid;
        }

        public void setVmUuid(String vmUuid) {
            this.vmUuid = vmUuid;
        }

        public String getL3NetworkUuid() {
            return l3NetworkUuid;
        }

        public void setL3NetworkUuid(String l3NetworkUuid) {
            this.l3NetworkUuid = l3NetworkUuid;
        }

        public String getInternalIp() {
            return internalIp;
        }

        public void setInternalIp(String internalIp) {
            this.internalIp = internalIp;
        }

        public String getRelateResourceUuid() {
            return relateResourceUuid;
        }

        public void setRelateResourceUuid(String relateResourceUuid) {
            this.relateResourceUuid = relateResourceUuid;
        }
    }

    @NeedJsonSchema
    public static class VmInternalIpRangeConflictData {
        private String vmUuid;
        private String l3NetworkUuid;
        private String internalIp;

        public String getVmUuid() {
            return vmUuid;
        }

        public void setVmUuid(String vmUuid) {
            this.vmUuid = vmUuid;
        }

        public String getL3NetworkUuid() {
            return l3NetworkUuid;
        }

        public void setL3NetworkUuid(String l3NetworkUuid) {
            this.l3NetworkUuid = l3NetworkUuid;
        }

        public String getInternalIp() {
            return internalIp;
        }

        public void setInternalIp(String internalIp) {
            this.internalIp = internalIp;
        }
    }

    @NeedJsonSchema
    public static class VmHostFileChangedData {
        private String hostUuid;
        private String vmUuid;
        private List<String> types;

        public String getHostUuid() {
            return hostUuid;
        }

        public void setHostUuid(String hostUuid) {
            this.hostUuid = hostUuid;
        }

        public String getVmUuid() {
            return vmUuid;
        }

        public void setVmUuid(String vmUuid) {
            this.vmUuid = vmUuid;
        }

        public List<String> getTypes() {
            return types;
        }

        public void setTypes(List<String> types) {
            this.types = types;
        }
    }

    @NeedJsonSchema
    public static class VmUserdataEncryptionDegradedData {
        private String vmUuid;
        private String tagType;
        private int userdataBase64Length;
        private int attemptedTagLength;
        private int threshold;
        private boolean degradedToPlaintext = true;
        private Date date = new Date();

        public String getVmUuid() {
            return vmUuid;
        }

        public void setVmUuid(String vmUuid) {
            this.vmUuid = vmUuid;
        }

        public String getTagType() {
            return tagType;
        }

        public void setTagType(String tagType) {
            this.tagType = tagType;
        }

        public int getUserdataBase64Length() {
            return userdataBase64Length;
        }

        public void setUserdataBase64Length(int userdataBase64Length) {
            this.userdataBase64Length = userdataBase64Length;
        }

        public int getAttemptedTagLength() {
            return attemptedTagLength;
        }

        public void setAttemptedTagLength(int attemptedTagLength) {
            this.attemptedTagLength = attemptedTagLength;
        }

        public int getThreshold() {
            return threshold;
        }

        public void setThreshold(int threshold) {
            this.threshold = threshold;
        }

        public boolean isDegradedToPlaintext() {
            return degradedToPlaintext;
        }

        public void setDegradedToPlaintext(boolean degradedToPlaintext) {
            this.degradedToPlaintext = degradedToPlaintext;
        }

        public Date getDate() {
            return date;
        }

        public void setDate(Date date) {
            this.date = date;
        }
    }
}
