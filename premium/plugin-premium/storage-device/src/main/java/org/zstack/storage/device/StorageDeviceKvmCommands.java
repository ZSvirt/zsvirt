package org.zstack.storage.device;

import org.zstack.header.log.NoLogging;
import org.zstack.kvm.KVMAgentCommands;
import org.zstack.kvm.VolumeTO;
import org.zstack.storage.device.fibreChannel.FiberChannelLunStruct;
import org.zstack.storage.device.hba.HbaDeviceStruct;
import org.zstack.storage.device.iscsi.IscsiTargetStruct;
import org.zstack.storage.device.localRaid.RaidPhysicalDriveStruct;
import org.zstack.storage.device.localRaid.SmartDataStruct;
import org.zstack.storage.device.multipath.DeviceTO;
import org.zstack.storage.device.nvme.NvmeLunStruct;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Create by weiwang at 2018/8/5
 */
public class StorageDeviceKvmCommands {
    public static final String ISCSI_LOGIN_PATH = "/storagedevice/iscsi/login";
    public static final String ISCSI_LOGOUT_PATH = "/storagedevice/iscsi/logout";
    public static final String FC_SCAN_PATH = "/storagedevice/fc/scan";
    public static final String NVME_SCAN_PATH = "/storagedevice/nvme/scan";
    public static final String NVME_CONNECT_PATH = "/storagedevice/nvme/connect";
    public static final String NVME_DISCONNECT_PATH = "/storagedevice/nvme/disconnect";
    public static final String RAID_SCAN_PATH = "/storagedevice/raid/scan";
    public static final String RAID_SMART_PATH = "/storagedevice/raid/smart";
    public static final String RAID_LOCATE_PATH = "/storagedevice/raid/locate";
    public static final String RAID_SELF_TEST_PATH = "/storagedevice/raid/selftest";
    public static final String MULTIPATH_ENABLE_PATH = "/storagedevice/multipath/enable";
    public static final String MULTIPATH_DISABLE_PATH = "/storagedevice/multipath/disable";
    public static final String ATTACH_SCSI_LUN_PATH = "/storagedevice/scsilun/attach";
    public static final String DETACH_SCSI_LUN_PATH = "/storagedevice/scsilun/detach";
    public static final String DETACH_SCSI_DEV_PATH = "/storagedevice/scsilun/detachdev";
    public static final String GET_MULTIPATH_TOPOLOGY_PATH = "/storagedevice/multipath/topology";
    public static final String HBA_SCAN_PATH = "/storagedevice/hba/scan";

    public static class IscsiLoginCmd extends KVMAgentCommands.AgentCommand implements Serializable {
        public String iscsiServerIp;
        public String iscsiServerPort;
        public String iscsiChapUserName;
        @NoLogging
        public String iscsiChapUserPassword;
        public List<String> iscsiTargets;

        public String getIscsiServerIp() {
            return iscsiServerIp;
        }

        public void setIscsiServerIp(String iscsiServerIp) {
            this.iscsiServerIp = iscsiServerIp;
        }

        public String getIscsiServerPort() {
            return iscsiServerPort;
        }

        public void setIscsiServerPort(String iscsiServerPort) {
            this.iscsiServerPort = iscsiServerPort;
        }

        public String getIscsiChapUserName() {
            return iscsiChapUserName;
        }

        public void setIscsiChapUserName(String iscsiChapUserName) {
            this.iscsiChapUserName = iscsiChapUserName;
        }

        public String getIscsiChapUserPassword() {
            return iscsiChapUserPassword;
        }

        public void setIscsiChapUserPassword(String iscsiChapUserPassword) {
            this.iscsiChapUserPassword = iscsiChapUserPassword;
        }

        public List<String> getIscsiTargets() {
            return iscsiTargets;
        }

        public void setIscsiTargets(List<String> iscsiTargets) {
            this.iscsiTargets = iscsiTargets;
        }
    }

    public static class IscsiLoginRsp extends KVMAgentCommands.AgentResponse {
        public List<IscsiTargetStruct> iscsiTargetStructList = new ArrayList<>();
    }

    public static class IscsiLogoutCmd extends IscsiLoginCmd {
    }

    public static class IscsiLogoutRsp extends KVMAgentCommands.AgentResponse {
    }

    public static class GetMultipathTopologyCmd extends KVMAgentCommands.AgentCommand {
        public List<String> wwids;
    }

    public static class GetMultipathTopologyRsp extends KVMAgentCommands.AgentResponse {
        public Map<String, List<DeviceTO>> devices;
    }

    public static class FcScanCmd extends KVMAgentCommands.AgentCommand {
        public boolean rescan;
        public List<String> identifiers;
    }

    public static class FcScanRsp extends KVMAgentCommands.AgentResponse {
        public List<FiberChannelLunStruct> fiberChannelLunStructs = new ArrayList<>();
    }

    public static class NvmeScanCmd extends KVMAgentCommands.AgentCommand {
        public boolean rescan;
        public List<String> identifiers;
    }

    public static class NvmeServerConnectCmd extends KVMAgentCommands.AgentCommand {
        public String ip;
        public Integer port;
        public String transport;
    }

    public static class NvmeServerDisconnectCmd extends NvmeServerConnectCmd {
    }

    public static class HbaScanCmd extends KVMAgentCommands.AgentCommand {
    }

    public static class HbaScanRsp extends KVMAgentCommands.AgentResponse {
        public List<HbaDeviceStruct> hbaDeviceStructs = new ArrayList<>();
    }


    public static class NvmeScanRsp extends KVMAgentCommands.AgentResponse {
        public List<NvmeLunStruct> nvmeLunStructs = new ArrayList<>();
    }

    public static class NvmeServerConnectRsp extends KVMAgentCommands.AgentResponse {
        public List<NvmeLunStruct> nvmeLunStructs = new ArrayList<>();
    }

    public static class NvmeServerDisconnectRsp extends KVMAgentCommands.AgentResponse {
    }

    public static class RaidScanCmd extends KVMAgentCommands.AgentCommand {
    }

    public static class RaidScanRsp extends KVMAgentCommands.AgentResponse {
        public List<RaidPhysicalDriveStruct> raidPhysicalDriveStructs = new ArrayList<>();
    }

    public static class RaidPhysicalDriveSmartCmd extends KVMAgentCommands.AgentCommand {
        public Integer busNumber;
        public Integer deviceNumber;
        public String wwn;
        public Integer slotNumber;
    }

    public static class RaidPhysicalDriveSmartRsp extends KVMAgentCommands.AgentResponse {
        public List<SmartDataStruct> smartDataStructs = new ArrayList<>();
    }

    public static class RaidPhysicalDriveLocateCmd extends KVMAgentCommands.AgentCommand {
        public Integer enclosureDeviceID;
        public Integer slotNumber;
        public Integer raidControllerNumber;
        public Integer deviceNumber;
        public Integer busNumber;
        public String wwn;
        public Boolean locate;
    }

    public static class RaidPhysicalDriveSmartTestCmd extends KVMAgentCommands.AgentCommand {
        public Integer busNumber;
        public Integer deviceNumber;
        public String wwn;
        public Integer slotNumber;
    }

    public static class RaidPhysicalDriveSmartTestRsp extends KVMAgentCommands.AgentResponse {
        public String result;
    }

    public static class MultipathEnableCmd extends KVMAgentCommands.AgentCommand {
        public List blacklist = null;
    }

    public static class MultipathEnableRsp extends KVMAgentCommands.AgentResponse {
    }

    public static class AttachScsiLunToVmCmd extends KVMAgentCommands.AgentCommand {
        public String vmInstanceUuid;
        public String wwid;
        public boolean multipath = true;
        public VolumeTO volume;
    }

    public static class AttachScsiLunToVmRsp extends KVMAgentCommands.AgentResponse {
    }


    public static class DetachScsiLunFromVmCmd extends KVMAgentCommands.AgentCommand {
        public String vmInstanceUuid;
        public String wwid;
        public VolumeTO volume;
    }

    public static class DetachScsiLunFromVmRsp extends KVMAgentCommands.AgentResponse {
    }

    public static class DetachScsiLunFromHostCmd extends KVMAgentCommands.AgentCommand {
        public String wwid;
    }

}
