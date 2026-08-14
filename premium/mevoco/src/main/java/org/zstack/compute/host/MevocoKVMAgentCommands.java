package org.zstack.compute.host;

import org.zstack.header.HasThreadContext;
import org.zstack.header.host.HostPhysicalCpuInventory;
import org.zstack.header.host.HostPhysicalMemoryInventory;
import org.zstack.header.log.NoLogging;
import org.zstack.header.storage.snapshot.TakeSnapshotsOnKvmJobStruct;
import org.zstack.header.storage.snapshot.TakeSnapshotsOnKvmResultStruct;
import org.zstack.header.vm.VmAccountPreference;
import org.zstack.header.vm.VmConfigSyncStruct;
import org.zstack.header.vm.additions.VmHostFileBackupJob;
import org.zstack.header.volume.IoThreadPinStruct;
import org.zstack.kvm.KVMAgentCommands;
import org.zstack.kvm.KVMAgentCommands.NicTO;
import org.zstack.kvm.VolumeTO;
import org.zstack.network.hostNetworkInterface.HostNetworkBondingInventory;
import org.zstack.network.hostNetworkInterface.HostNetworkInterfaceInventory;
import org.zstack.network.hostNetworkInterface.HostKernelInterfaceTO;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by mingjian.deng on 16/12/1.
 */
public class MevocoKVMAgentCommands {
    public static class ChangeVmPasswordResponse extends KVMAgentCommands.AgentResponse implements Serializable {

        private VmAccountPreference accountPerference;

        public VmAccountPreference getVmAccountPerference() { return accountPerference; }
        public void setVmAccountPerference(VmAccountPreference accountPerference) { this.accountPerference = accountPerference; }

    }

    public static class ChangeEmulatorPinnningCmd extends KVMAgentCommands.AgentCommand {
        public String emulatorPinning;
        public String uuid;

        public String getEmulatorPinning() {
            return emulatorPinning;
        }

        public void setEmulatorPinning(String emulatorPinning) {
            this.emulatorPinning = emulatorPinning;
        }

        public String getUuid() {
            return uuid;
        }

        public void setUuid(String uuid) {
            this.uuid = uuid;
        }
    }

    public static class ChangeEmulatorPinnningResponse extends KVMAgentCommands.AgentResponse {
    }

    public static class CheckMountDomainCmd extends KVMAgentCommands.AgentCommand {
        private long timeout;
        private String url;

        public String getUrl() {
            return url;
        }

        public void setUrl(String url) {
            this.url = url;
        }

        public long getTimeout() {
            return timeout;
        }

        public void setTimeout(long timeout) {
            this.timeout = timeout;
        }
    }

    public static class ChangeVmPasswordCmd extends KVMAgentCommands.AgentCommand implements Serializable {
        private VmAccountPreference accountPerference;
        private long timeout;

        public VmAccountPreference getAccountPerference() { return accountPerference; }

        public void setAccountPerference(VmAccountPreference accountPerference) { this.accountPerference = accountPerference; }

        public long getTimeout() {
            return timeout;
        }

        public void setTimeout(long timeout) {
            this.timeout = timeout;
        }
    }

    public static class CheckVmVolumesCmd extends KVMAgentCommands.AgentCommand {
        private String uuid;
        private List<VolumeTO> volumes;

        public String getUuid() {
            return uuid;
        }

        public void setUuid(String uuid) {
            this.uuid = uuid;
        }

        public List<VolumeTO> getVolumes() {
            return volumes;
        }

        public void setVolumes(List<VolumeTO> volumes) {
            this.volumes = volumes;
        }
    }

    public static class VolumeQosCmd extends KVMAgentCommands.AgentCommand{
        public long totalBandwidth = 0;
        public long readBandwidth = 0;
        public long writeBandwidth = 0;
        public long totalIOPS = 0;
        public long readIOPS = 0;
        public long writeIOPS = 0;
        public VolumeTO volume;
        public String vmUuid;
        // total, read, write, all, overwrite
        public String mode;

        public long getTotalBandwidth() {
            return totalBandwidth;
        }

        public void setTotalBandwidth(long totalBandwidth) {
            this.totalBandwidth = totalBandwidth;
        }

        public String getVmUuid() {
            return vmUuid;
        }

        public void setVmUuid(String vmUuid) {
            this.vmUuid = vmUuid;
        }

        public String getMode() {
            return mode;
        }

        public void setMode(String mode) {
            this.mode = mode;
        }

        public long getReadBandwidth() {
            return readBandwidth;
        }

        public void setReadBandwidth(long readBandwidth) {
            this.readBandwidth = readBandwidth;
        }

        public long getWriteBandwidth() {
            return writeBandwidth;
        }

        public void setWriteBandwidth(long writeBandwidth) {
            this.writeBandwidth = writeBandwidth;
        }

        public long getTotalIOPS() {
            return totalIOPS;
        }

        public void setTotalIOPS(long totalIOPS) {
            this.totalIOPS = totalIOPS;
        }

        public long getReadIOPS() {
            return readIOPS;
        }

        public void setReadIOPS(long readIOPS) {
            this.readIOPS = readIOPS;
        }

        public long getWriteIOPS() {
            return writeIOPS;
        }

        public void setWriteIOPS(long writeIOPS) {
            this.writeIOPS = writeIOPS;
        }

        public VolumeTO getVolume() {
            return volume;
        }

        public void setVolume(VolumeTO volume) {
            this.volume = volume;
        }
    }

    public static class NicAgentResponse extends KVMAgentCommands.AgentResponse {
        public String inbound;
        public String outbound;
    }

    public static class VolumeAgentResponse extends KVMAgentCommands.AgentResponse {
        public String bandWidth;
        public String bandWidthWrite;
        public String bandWidthRead;
        public String iopsTotal;
        public String iopsRead;
        public String iopsWrite;
    }

    public static class CheckMountDomainResponse extends KVMAgentCommands.AgentResponse {
        boolean active = false;
    }

    public static class NicQosCmd extends KVMAgentCommands.AgentCommand{
        String vmUuid;
        String internalName;
        // set -1 for default, it means change nothing
        long inboundBandwidth = -1;
        long outboundBandwidth = -1;

        public String getVmUuid() {
            return vmUuid;
        }

        public void setVmUuid(String vmUuid) {
            this.vmUuid = vmUuid;
        }

        public String getInternalName() {
            return internalName;
        }

        public void setInternalName(String internalName) {
            this.internalName = internalName;
        }

        public long getInboundBandwidth() {
            return inboundBandwidth;
        }

        public void setInboundBandwidth(long inboundBandwidth) {
            this.inboundBandwidth = inboundBandwidth;
        }

        public long getOutboundBandwidth() {
            return outboundBandwidth;
        }

        public void setOutboundBandwidth(long outboundBandwidth) {
            this.outboundBandwidth = outboundBandwidth;
        }
    }

    public static class ResizeVolumeCmd extends KVMAgentCommands.AgentCommand {
        String vmUuid;
        String deviceType;
        String installPath;
        VolumeTO volume;
        long size;

        public String getDeviceType() {
            return deviceType;
        }

        public void setDeviceType(String deviceType) {
            this.deviceType = deviceType;
        }

        public long getSize() {
            return size;
        }

        public void setSize(long size) {
            this.size = size;
        }

        public String getVmUuid() {
            return vmUuid;
        }

        public void setVmUuid(String vmUuid) {
            this.vmUuid = vmUuid;
        }

        public String getInstallPath() {
            return installPath;
        }

        public void setInstallPath(String installPath) {
            this.installPath = installPath;
        }

        public void setVolume(VolumeTO volume) {
            this.volume = volume;
        }

        public VolumeTO getVolume() {
            return volume;
        }
    }

    public static class ResizeVolumeResponse extends KVMAgentCommands.AgentResponse {
    }

    public static class TakeSnapshotsCmd extends KVMAgentCommands.AgentCommand implements HasThreadContext {
        List<TakeSnapshotsOnKvmJobStruct> snapshotJobs;
        List<VmHostFileBackupJob> vmHostFileBackupJobs;
        long timeout;

        public List<TakeSnapshotsOnKvmJobStruct> getSnapshotJobs() {return snapshotJobs;}

        public void setSnapshotJobs(List<TakeSnapshotsOnKvmJobStruct> snapshotJobs) {this.snapshotJobs = snapshotJobs;}

        public List<VmHostFileBackupJob> getVmHostFileBackupJobs() {return vmHostFileBackupJobs;}

        public void setVmHostFileBackupJobs(List<VmHostFileBackupJob> vmHostFileBackupJobs) {this.vmHostFileBackupJobs = vmHostFileBackupJobs;}

        public long getTimeout() {return timeout;}

        public void setTimeout(long timeout) {this.timeout = timeout;}
    }

    public static class TakeSnapshotsResponse extends KVMAgentCommands.AgentResponse {
        List<TakeSnapshotsOnKvmResultStruct> snapshots;

        public List<TakeSnapshotsOnKvmResultStruct> getSnapshots() { return snapshots;}

        public void setSnapshots(List<TakeSnapshotsOnKvmResultStruct> snapshots) { this.snapshots = snapshots; }
    }

    public static class BlockStreamVolumeCmd extends KVMAgentCommands.AgentCommand implements HasThreadContext {
        public String vmUuid;
        public VolumeTO volume;
    }

    public static class EnableHugePageCmd extends  KVMAgentCommands.AgentCommand {
        public long reserveSize;
        public long pageSize;
    }

    public static class EnableHugePageResponse extends KVMAgentCommands.AgentResponse {
    }

    public static class DisableHugePageCmd extends  KVMAgentCommands.AgentCommand {
    }

    public static class DisableHugePageResponse extends KVMAgentCommands.AgentResponse {

    }
    public static class IdentifyHostCmd extends KVMAgentCommands.AgentCommand {
        public long interval;
    }

    public static class LocateHostNetworkInterfaceCmd extends KVMAgentCommands.AgentCommand {
        public String networkInterface;
        public long interval;
    }

    public static class GetHostPhysicalCpuFactsCmd extends KVMAgentCommands.AgentCommand {
    }

    public static class GetHostPhysicalCpuFactsResponse extends KVMAgentCommands.AgentResponse {
        public List<HostPhysicalCpuInventory> physicalCpuFacts;
    }

    public static class UpdateHostIscsiInitiatorNameCmd extends KVMAgentCommands.AgentCommand {
        public String iscsiInitiatorName;
    }

    public static class UpdateHostIscsiInitiatorNameRsp extends KVMAgentCommands.AgentResponse {
    }

    public static class GetHostPhysicalMemoryFactsCmd extends KVMAgentCommands.AgentCommand {
    }

    public static class GetHostPhysicalMemoryFactsResponse extends KVMAgentCommands.AgentResponse {
        public List<HostPhysicalMemoryInventory> physicalMemoryFacts;
    }

    public static class ChangeHostPasswordCmd extends KVMAgentCommands.AgentCommand implements Serializable {
        @NoLogging
        public String password;
    }

    public static class UpdateOvsCpuPinningCmd extends KVMAgentCommands.AgentCommand {
        public String ovsCpuPinning;

        public String getOvsCpuPinning() {
            return ovsCpuPinning;
        }

        public void setOvsCpuPinning(String ovsCpuPinning) {
            this.ovsCpuPinning = ovsCpuPinning;
        }
    }

    public static class UpdateOvsCpuPinningResponse extends KVMAgentCommands.AgentResponse {

    }

    public static class GetHostNetworkBondingCmd extends KVMAgentCommands.AgentCommand {
        public String managementServerIp;

        public String getManagementServerIp() {
            return managementServerIp;
        }

        public void setManagementServerIp(String managementServerIp) {
            this.managementServerIp = managementServerIp;
        }
    }

    public static class GetHostNetworkBondingResponse extends KVMAgentCommands.AgentResponse {
        public List<HostNetworkBondingInventory> bondings;
        public List<HostNetworkInterfaceInventory> nics;
    }

    public static class GetHostBondingFactsCmd extends KVMAgentCommands.AgentCommand {
        private String managementServerIp;
        private String bondName;

        public String getManagementServerIp() {
            return managementServerIp;
        }

        public void setManagementServerIp(String managementServerIp) {
            this.managementServerIp = managementServerIp;
        }

        public String getBondName() {
            return bondName;
        }

        public void setBondName(String bondName) {
            this.bondName = bondName;
        }
    }

    public static class GetHostBondingFactsResponse extends KVMAgentCommands.AgentResponse {
        private HostNetworkBondingInventory bonding;

        public HostNetworkBondingInventory getBonding() {
            return bonding;
        }

        public void setBonding(HostNetworkBondingInventory bonding) {
            this.bonding = bonding;
        }
    }

    public static class BridgeRouterPortCmd extends KVMAgentCommands.AgentCommand {
        List<String> nicNames;
        Boolean enable;

        public List<String> getNicNames() {
            return nicNames;
        }

        public void setNicNames(List<String> nicNames) {
            this.nicNames = nicNames;
        }

        public Boolean getEnable() {
            return enable;
        }

        public void setEnable(Boolean enable) {
            this.enable = enable;
        }
    }

    public static class BridgeRouterPortResponse extends KVMAgentCommands.AgentResponse {

    }

    public static class EnableZeroCopyCmd extends  KVMAgentCommands.AgentCommand {
    }

    public static class EnableZeroCopyResponse extends KVMAgentCommands.AgentResponse {
    }

    public static class DisableZeroCopyCmd extends  KVMAgentCommands.AgentCommand {
    }

    public static class DisableZeroCopyResponse extends KVMAgentCommands.AgentResponse {
    }

    public static class AddBridgeFdbEntryCmd extends KVMAgentCommands.AgentCommand {
        private String physicalInterface;
        private List<String> macs;

        public String getPhysicalInterface() {
            return physicalInterface;
        }

        public void setPhysicalInterface(String physicalInterface) {
            this.physicalInterface = physicalInterface;
        }

        public List<String> getMacs() {
            return macs;
        }

        public void setMacs(List<String> macs) {
            this.macs = macs;
        }
    }

    public static class AddBridgeFdbEntryRsp extends KVMAgentCommands.AgentResponse {
    }

    public static class BatchUpdateBridgeCmd extends KVMAgentCommands.AgentCommand {
        private String oldPhysicalInterface;
        private String oldBondingName;
        private String newPhysicalInterface;
        private CreateBondingCmd newBonding;
        private List<BridgeParam> bridgeParams;

        public String getOldPhysicalInterface() {
            return oldPhysicalInterface;
        }

        public void setOldPhysicalInterface(String oldPhysicalInterface) {
            this.oldPhysicalInterface = oldPhysicalInterface;
        }

        public String getOldBondingName() {
            return oldBondingName;
        }

        public void setOldBondingName(String oldBondingName) {
            this.oldBondingName = oldBondingName;
        }

        public String getNewPhysicalInterface() {
            return newPhysicalInterface;
        }

        public void setNewPhysicalInterface(String newPhysicalInterface) {
            this.newPhysicalInterface = newPhysicalInterface;
        }

        public CreateBondingCmd getNewBonding() {
            return newBonding;
        }

        public void setNewBonding(CreateBondingCmd newBonding) {
            this.newBonding = newBonding;
        }

        public List<BridgeParam> getBridgeParams() {
            return bridgeParams;
        }

        public void setBridgeParams(List<BridgeParam> bridgeParams) {
            this.bridgeParams = bridgeParams;
        }
    }

    public static class BridgeParam {
        private String bridgeName;
        private Integer vlanId;
        private String l2NetworkUuid;

        public String getBridgeName() {
            return bridgeName;
        }

        public void setBridgeName(String bridgeName) {
            this.bridgeName = bridgeName;
        }

        public Integer getVlanId() {
            return vlanId;
        }

        public void setVlanId(Integer vlanId) {
            this.vlanId = vlanId;
        }

        public String getL2NetworkUuid() {
            return l2NetworkUuid;
        }

        public void setL2NetworkUuid(String l2NetworkUuid) {
            this.l2NetworkUuid = l2NetworkUuid;
        }
    }

    public static class BatchUpdateBridgeRsp extends KVMAgentCommands.AgentResponse {
    }

    public static class ChangeVfNicHaStateCmd extends KVMAgentCommands.AgentCommand {
        private String vmUuid;
        private NicTO nic;
        private String haState;

        public String getVmUuid() {
            return vmUuid;
        }

        public void setVmUuid(String vmUuid) {
            this.vmUuid = vmUuid;
        }

        public NicTO getNic() {
            return nic;
        }

        public void setNic(NicTO nic) {
            this.nic = nic;
        }

        public String getHaState() {
            return haState;
        }

        public void setHaState(String haState) {
            this.haState = haState;
        }
    }

    public static class ChangeVfNicHaStateRsp extends KVMAgentCommands.AgentResponse {
    }

    public static class CreateBondingCmd extends KVMAgentCommands.AgentCommand {
        private String bondName;
        private List<HostNetworkInterfaceInventory> slaves;
        private String type;
        private String mode;
        private String xmitHashPolicy;

        public CreateBondingCmd() {
        }

        public String getBondName() {
            return bondName;
        }

        public void setBondName(String bondName) {
            this.bondName = bondName;
        }

        public List<HostNetworkInterfaceInventory> getSlaves() {
            return slaves;
        }

        public void setSlaves(List<HostNetworkInterfaceInventory> slaves) {
            this.slaves = slaves;
        }

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public String getMode() {
            return mode;
        }

        public void setMode(String mode) {
            this.mode = mode;
        }

        public String getXmitHashPolicy() {
            return xmitHashPolicy;
        }

        public void setXmitHashPolicy(String xmitHashPolicy) {
            this.xmitHashPolicy = xmitHashPolicy;
        }

    }

    public static class CreateBondingRsp extends KVMAgentCommands.AgentResponse {
    }

    public static class UpdateBondingCmd extends KVMAgentCommands.AgentCommand {
        private String bondName;
        private List<HostNetworkInterfaceInventory> oldSlaves;
        private List<HostNetworkInterfaceInventory> slaves;
        private String mode;
        private String xmitHashPolicy;

        public String getBondName() {
            return bondName;
        }

        public void setBondName(String bondName) {
            this.bondName = bondName;
        }

        public List<HostNetworkInterfaceInventory> getOldSlaves() {
            return oldSlaves;
        }

        public void setOldSlaves(List<HostNetworkInterfaceInventory> oldSlaves) {
            this.oldSlaves = oldSlaves;
        }

        public List<HostNetworkInterfaceInventory> getSlaves() {
            return slaves;
        }

        public void setSlaves(List<HostNetworkInterfaceInventory> slaves) {
            this.slaves = slaves;
        }

        public String getMode() {
            return mode;
        }

        public void setMode(String mode) {
            this.mode = mode;
        }

        public String getXmitHashPolicy() {
            return xmitHashPolicy;
        }

        public void setXmitHashPolicy(String xmitHashPolicy) {
            this.xmitHashPolicy = xmitHashPolicy;
        }

    }

    public static class UpdateBondingRsp extends KVMAgentCommands.AgentResponse {
    }

    public static class DeleteBondingCmd extends KVMAgentCommands.AgentCommand {
        private String bondName;

        public String getBondName() {
            return bondName;
        }

        public void setBondName(String bondName) {
            this.bondName = bondName;
        }
    }

    public static class DeleteBondingRsp extends KVMAgentCommands.AgentResponse {
    }

    public static class AttachNicToBondingCmd extends KVMAgentCommands.AgentCommand {
        private String bondName;
        private List<HostNetworkInterfaceInventory> slaves;

        public String getBondName() {
            return bondName;
        }

        public void setBondName(String bondName) {
            this.bondName = bondName;
        }

        public List<HostNetworkInterfaceInventory> getSlaves() {
            return slaves;
        }

        public void setSlaves(List<HostNetworkInterfaceInventory> slaves) {
            this.slaves = slaves;
        }
    }

    public static class AttachNicToBondingRsp extends KVMAgentCommands.AgentResponse {
    }

    public static class DetachNicFromBondingCmd extends KVMAgentCommands.AgentCommand {
        private String bondName;
        private List<HostNetworkInterfaceInventory> slaves;

        public String getBondName() {
            return bondName;
        }

        public void setBondName(String bondName) {
            this.bondName = bondName;
        }

        public List<HostNetworkInterfaceInventory> getSlaves() {
            return slaves;
        }

        public void setSlaves(List<HostNetworkInterfaceInventory> slaves) {
            this.slaves = slaves;
        }
    }

    public static class DetachNicFromBondingRsp extends KVMAgentCommands.AgentResponse {
    }

    public static class SetIpOnHostNetworkInterfaceCmd extends KVMAgentCommands.AgentCommand {
        private String interfaceName;
        private String oldIpAddress;
        private String oldNetmask;
        private String oldGateway;
        private String ipAddress;
        private String netmask;
        private String gateway;

        public String getInterfaceName() {
            return interfaceName;
        }

        public void setInterfaceName(String interfaceName) {
            this.interfaceName = interfaceName;
        }

        public String getOldIpAddress() {
            return oldIpAddress;
        }

        public void setOldIpAddress(String oldIpAddress) {
            this.oldIpAddress = oldIpAddress;
        }

        public String getOldNetmask() {
            return oldNetmask;
        }

        public void setOldNetmask(String oldNetmask) {
            this.oldNetmask = oldNetmask;
        }

        public String getOldGateway() {
            return oldGateway;
        }

        public void setOldGateway(String oldGateway) {
            this.oldGateway = oldGateway;
        }

        public String getIpAddress() {
            return ipAddress;
        }

        public void setIpAddress(String ipAddress) {
            this.ipAddress = ipAddress;
        }

        public String getNetmask() {
            return netmask;
        }

        public void setNetmask(String netmask) {
            this.netmask = netmask;
        }

        public String getGateway() {
            return gateway;
        }

        public void setGateway(String gateway) {
            this.gateway = gateway;
        }
    }

    public static class SetIpOnHostNetworkInterfaceRsp extends KVMAgentCommands.AgentResponse {
    }

    public static class CheckInterfaceVlanCmd extends KVMAgentCommands.AgentCommand {
        private String interfaceName;

        private Integer vlanId;

        public String getInterfaceName() {
            return interfaceName;
        }

        public void setInterfaceName(String interfaceName) {
            this.interfaceName = interfaceName;
        }

        public Integer getVlanId() {
            return vlanId;
        }

        public void setVlanId(Integer vlanId) {
            this.vlanId = vlanId;
        }
    }

    public static class CheckInterfaceVlanRsp extends KVMAgentCommands.AgentResponse {
    }

    public static class GetInterfaceVlanCmd extends KVMAgentCommands.AgentCommand {
        private List<String> interfaceNames;

        public void setInterfaceNames(List<String> interfaceNames) {
            this.interfaceNames = interfaceNames;
        }

        public List<String> getInterfaceNames() {
            return interfaceNames;
        }
    }

    public static class GetInterfaceVlanRsp extends KVMAgentCommands.AgentResponse {
        private List<Integer> vlanIds;

        public List<Integer> getVlanIds() {
            return vlanIds;
        }

        public void setVlanIds(List<Integer> vlanIds) {
            this.vlanIds = vlanIds;
        }
    }

    public static class GetInterfaceNameCmd extends KVMAgentCommands.AgentCommand {
        private List<String> ipAddresses;

        public List<String> getIpAddresses() {
            return ipAddresses;
        }

        public void setIpAddresses(List<String> ipAddresses) {
            this.ipAddresses = ipAddresses;
        }
    }

    public static class GetInterfaceNameRsp extends KVMAgentCommands.AgentResponse {
        private List<String> interfaceNames;

        public List<String> getInterfaceNames() {
            return interfaceNames;
        }

        public void setInterfaceNames(List<String> interfaceNames) {
            this.interfaceNames = interfaceNames;
        }
    }

    public static class SetServiceTypeOnHostNetworkInterfaceCmd extends KVMAgentCommands.AgentCommand {
        private String interfaceName;

        private Integer vlanId;

        private List<String> serviceType;;

        public String getInterfaceName() {
            return interfaceName;
        }

        public void setInterfaceName(String interfaceName) {
            this.interfaceName = interfaceName;
        }

        public Integer getVlanId() {
            return vlanId;
        }

        public void setVlanId(Integer vlanId) {
            this.vlanId = vlanId;
        }

        public List<String> getServiceType() {
            return serviceType;
        }

        public void setServiceType(List<String> serviceType) {
            this.serviceType = serviceType;
        }
    }

    public static class SetServiceTypeOnHostNetworkInterfaceRsp extends KVMAgentCommands.AgentResponse {
    }

    public static class SetHostPhysicalNicMonitorCmd extends KVMAgentCommands.AgentResponse {
        Boolean enable;
        public List<HostNetworkInterfaceInventory> nics;

        public Boolean getEnable() {
            return enable;
        }

        public void setEnable(Boolean enable) {
            this.enable = enable;
        }

        public List<HostNetworkInterfaceInventory> getNics() {
            return nics;
        }

        public void setNics(List<HostNetworkInterfaceInventory> nics) {
            this.nics = nics;
        }
    }

    public static class GetHostKernelInterfaceCmd extends KVMAgentCommands.AgentCommand {
        private String hostUuid;
        private String targetIp;

        public String getHostUuid() {
            return hostUuid;
        }

        public void setHostUuid(String hostUuid) {
            this.hostUuid = hostUuid;
        }

        public String getTargetIp() {
            return targetIp;
        }

        public void setTargetIp(String targetIp) {
            this.targetIp = targetIp;
        }
    }

    public static class GetHostKernelInterfaceRsp extends KVMAgentCommands.AgentResponse {
        List<HostKernelInterfaceTO> interfaces;

        public List<HostKernelInterfaceTO> getInterfaces() {
            return interfaces;
        }

        public void setInterfaces(List<HostKernelInterfaceTO> interfaces) {
            this.interfaces = interfaces;
        }
    }

    public static class SetHostKernelInterfaceCmd extends KVMAgentCommands.AgentCommand {
        private List<HostKernelInterfaceTO> interfaces;

        public SetHostKernelInterfaceCmd() {
            this.interfaces = new ArrayList<>();
        }

        public List<HostKernelInterfaceTO> getInterfaces() {
            return interfaces;
        }

        public void setInterfaces(List<HostKernelInterfaceTO> interfaces) {
            this.interfaces = interfaces;
        }
    }

    public static class SetHostKernelInterfaceRsp extends KVMAgentCommands.AgentResponse {
    }

    public static class SetHostPhysicalNicMonitorRsp extends KVMAgentCommands.AgentResponse {
    }

    public static class SyncVmClockCmd extends KVMAgentCommands.AgentCommand {
        private String vmUuid;

        public String getVmUuid() {
            return vmUuid;
        }

        public void setVmUuid(String vmUuid) {
            this.vmUuid = vmUuid;
        }
    }

    public static class SyncVmClockRsp extends KVMAgentCommands.AgentResponse {
    }

    public static class SetSyncVmClockTaskCmd extends KVMAgentCommands.AgentCommand {
        private Map<String, Integer> intervalMap;

        public Map<String, Integer> getIntervalMap() {
            return intervalMap;
        }

        public void setIntervalMap(Map<String, Integer> intervalMap) {
            this.intervalMap = intervalMap;
        }
    }

    public static class SetSyncVmClockTaskRsp extends KVMAgentCommands.AgentResponse {
    }

    public static class SyncVmPortsCommand extends KVMAgentCommands.AgentCommand {
        private String vmUuid;
        private List<VmConfigSyncStruct.VmPortConfig> ports;
        private String hostname;
        private String defaultIP;

        public String getVmUuid() {return vmUuid;}

        public void setVmUuid(String vmUuid) {this.vmUuid = vmUuid;}

        public List<VmConfigSyncStruct.VmPortConfig> getPorts() {return ports;}

        public void setPorts(List<VmConfigSyncStruct.VmPortConfig> ports) {this.ports = ports;}

        public String getHostname() {
            return hostname;
        }

        public void setHostname(String hostname) {
            this.hostname = hostname;
        }

        public String getDefaultIP() {
            return defaultIP;
        }

        public void setDefaultIP(String defaultIP) {
            this.defaultIP = defaultIP;
        }
    }

    public static class VmConfigSyncResponse extends KVMAgentCommands.AgentResponse {
    }

    public static class SyncVmPortsResponse extends VmConfigSyncResponse {
    }

    public static class SyncVmSpecificationCommand extends KVMAgentCommands.AgentCommand implements Serializable {
        private String vmUuid;
        private VmConfigSyncStruct.VmSpecificationConfig spec;

        public String getVmUuid() {
            return vmUuid;
        }

        public void setVmUuid(String vmUuid) {
            this.vmUuid = vmUuid;
        }

        public VmConfigSyncStruct.VmSpecificationConfig getSpec() {
            return spec;
        }

        public void setSpec(VmConfigSyncStruct.VmSpecificationConfig spec) {
            this.spec = spec;
        }
    }

    public static class SyncVmSpecificationResponse extends VmConfigSyncResponse {
    }

    public static class SetVmHostnameCmd extends KVMAgentCommands.AgentCommand {
        private String vmUuid;
        private String hostname;
        private String defaultIP;

        public String getVmUuid() {return vmUuid;}

        public void setVmUuid(String vmUuid) {this.vmUuid = vmUuid;}

        public String getHostname() {
            return hostname;
        }

        public void setHostname(String hostName) {
            this.hostname = hostName;
        }

        public String getDefaultIP() {
            return defaultIP;
        }

        public void setDefaultIP(String defaultIP) {
            this.defaultIP = defaultIP;
        }
    }

    public static class SetVmHostnameResponse extends VmConfigSyncResponse {
    }

    public static class SetVmDnsCmd extends KVMAgentCommands.AgentCommand {
        private String vmUuid;
        private List<String> dns;

        public String getVmUuid() {
            return vmUuid;
        }

        public void setVmUuid(String vmUuid) {
            this.vmUuid = vmUuid;
        }

        public List<String> getDns() {
            return dns;
        }

        public void setDns(List<String> dns) {
            this.dns = dns;
        }
    }

    public static class SetVmDnsResponse extends VmConfigSyncResponse {
    }

    public static class ApplyMemoryBalloonCmd extends KVMAgentCommands.AgentCommand {
        private List<String> vmUuids;
        private int adjustPercent;
        private String direction;
        private Map<String, Long> vmReservedMemory;

        public List<String> getVmUuids() {
            return vmUuids;
        }

        public void setVmUuids(List<String> vmUuids) {
            this.vmUuids = vmUuids;
        }

        public int getAdjustPercent() {
            return adjustPercent;
        }

        public void setAdjustPercent(int adjustPercent) {
            this.adjustPercent = adjustPercent;
        }

        public String getDirection() {
            return direction;
        }

        public void setDirection(String direction) {
            this.direction = direction;
        }

        public Map<String, Long> getVmReservedMemory() {
            return vmReservedMemory;
        }

        public void setVmReservedMemory(Map<String, Long> vmReservedMemory) {
            this.vmReservedMemory = vmReservedMemory;
        }
    }

    public static class ApplyMemoryBalloonResponse extends KVMAgentCommands.AgentResponse {
    }

    public static class GetGuestToolsStateCmd extends KVMAgentCommands.AgentCommand {
        List<String> vmInstanceUuids;

        public List<String> getVmInstanceUuids() {
            return vmInstanceUuids;
        }

        public void setVmInstanceUuids(List<String> vmInstanceUuids) {
            this.vmInstanceUuids = vmInstanceUuids;
        }
    }

    public static class GuestToolsState {
        Boolean qgaRunning;
        Boolean zsToolsFound;
        String version;
        String platForm;
        String osType;

        public String getPlatForm() {return platForm;}

        public void setPlatForm(String platForm) { this.platForm = platForm;}

        public String getOsType() { return osType;}

        public void setOsType(String osType) { this.osType = osType;}

        public Boolean getQgaRunning() {
            return qgaRunning;
        }

        public void setQgaRunning(Boolean qgaRunning) {
            this.qgaRunning = qgaRunning;
        }

        public Boolean getZsToolsFound() {
            return zsToolsFound;
        }

        public void setZsToolsFound(Boolean zsToolsFound) {
            this.zsToolsFound = zsToolsFound;
        }

        public String getVersion() {
            return version;
        }

        public void setVersion(String version) {
            this.version = version;
        }
    }

    public static class GuestToolsStateResp extends KVMAgentCommands.AgentResponse {
        private HashMap<String, GuestToolsState> states;

        public HashMap<String, GuestToolsState> getStates() {return states;}

        public void setStates(HashMap<String, GuestToolsState> states) {this.states = states;}
    }

    public static class SetVmIoThreadPinCmd extends KVMAgentCommands.AgentCommand {
        public String vmUuid;
        public Integer ioThreadId;
        public String pin;

        public String getVmUuid() {
            return vmUuid;
        }

        public void setVmUuid(String vmUuid) {
            this.vmUuid = vmUuid;
        }

        public Integer getIoThreadId() {
            return ioThreadId;
        }

        public void setIoThreadId(Integer ioThreadId) {
            this.ioThreadId = ioThreadId;
        }

        public String getPin() {
            return pin;
        }

        public void setPin(String pin) {
            this.pin = pin;
        }
    }

    public static class SetVmIoThreadPinRsp extends KVMAgentCommands.AgentResponse {
        public Integer ioThreadId;

        public Integer getIoThreadId() {
            return ioThreadId;
        }

        public void setIoThreadId(Integer ioThreadId) {
            this.ioThreadId = ioThreadId;
        }
    }

    public static class DelVmIoThreadPinCmd extends KVMAgentCommands.AgentCommand {
        public String vmUuid;
        public Integer ioThreadId;

        public String getVmUuid() {
            return vmUuid;
        }

        public void setVmUuid(String vmUuid) {
            this.vmUuid = vmUuid;
        }

        public Integer getIoThreadId() {
            return ioThreadId;
        }

        public void setIoThreadId(Integer ioThreadId) {
            this.ioThreadId = ioThreadId;
        }
    }

    public static class DelVmIoThreadPinRsp extends KVMAgentCommands.AgentResponse {
        public Integer ioThreadId;

        public Integer getIoThreadId() {
            return ioThreadId;
        }

        public void setIoThreadId(Integer ioThreadId) {
            this.ioThreadId = ioThreadId;
        }
    }

    public static class GetVmIoThreadPinCmd extends KVMAgentCommands.AgentCommand {
        public String vmUuid;

        public String getVmUuid() {
            return vmUuid;
        }

        public void setVmUuid(String vmUuid) {
            this.vmUuid = vmUuid;
        }
    }

    public static class GetVmIoThreadPinRsp extends KVMAgentCommands.AgentResponse {
        public List<IoThreadPinStruct> ioThreadInfo;

        public List<IoThreadPinStruct> getIoThreadInfo() {
            return ioThreadInfo;
        }

        public void setIoThreadInfo(List<IoThreadPinStruct> ioThreadInfo) {
            this.ioThreadInfo = ioThreadInfo;
        }
    }

    public static class SetScsiControllerCmd extends KVMAgentCommands.AgentCommand {
        public String vmUuid;
        public int ioThreadId;

        public String getVmUuid() {
            return vmUuid;
        }

        public void setVmUuid(String vmUuid) {
            this.vmUuid = vmUuid;
        }

        public int getIoThreadId() {
            return ioThreadId;
        }

        public void setIoThreadId(int ioThreadId) {
            this.ioThreadId = ioThreadId;
        }
    }

    public static class SetScsiControllerRsp extends KVMAgentCommands.AgentResponse {
        public String vmUuid;
        public int ioThreadId;
        public String controllerIndex;

        public String getControllerIndex() {
            return controllerIndex;
        }

        public void setControllerIndex(String controllerIndex) {
            this.controllerIndex = controllerIndex;
        }

        public String getVmUuid() {
            return vmUuid;
        }

        public void setVmUuid(String vmUuid) {
            this.vmUuid = vmUuid;
        }

        public int getIoThreadId() {
            return ioThreadId;
        }

        public void setIoThreadId(int ioThreadId) {
            this.ioThreadId = ioThreadId;
        }
    }

    public static class DelScsiControllerCmd extends KVMAgentCommands.AgentCommand {
        public String vmUuid;
        public int ioThreadId;

        public String getVmUuid() {
            return vmUuid;
        }

        public void setVmUuid(String vmUuid) {
            this.vmUuid = vmUuid;
        }

        public int getIoThreadId() {
            return ioThreadId;
        }

        public void setIoThreadId(int ioThreadId) {
            this.ioThreadId = ioThreadId;
        }
    }

    public static class DelScsiControllerRsp extends KVMAgentCommands.AgentResponse {
        public String vmUuid;
        public int ioThreadId;

        public String getVmUuid() {
            return vmUuid;
        }

        public void setVmUuid(String vmUuid) {
            this.vmUuid = vmUuid;
        }

        public int getIoThreadId() {
            return ioThreadId;
        }

        public void setIoThreadId(int ioThreadId) {
            this.ioThreadId = ioThreadId;
        }
    }

    public static class PreconfigureOvsDpdkCmd extends KVMAgentCommands.AgentCommand{
        public long reserveSize;
        public long pageSize;
        public int socketMem;

        public long getReserveSize() {
            return reserveSize;
        }

        public void setReserveSize(long reserveSize) {
            this.reserveSize = reserveSize;
        }

        public long getPageSize() {
            return pageSize;
        }

        public void setPageSize(long pageSize) {
            this.pageSize = pageSize;
        }

        public int getSocketMem() {
            return socketMem;
        }

        public void setSocketMem(int socketMem) {
            this.socketMem = socketMem;
        }
    }

    public static class PreconfigureOvsDpdkCmdRsp extends KVMAgentCommands.AgentResponse {
    }

    public static class SyncVdpaCmd extends KVMAgentCommands.AgentCommand {
        public List<OvsDpdkBridgeTO> ovsBridgeInfo;

        public List<OvsDpdkBridgeTO> getOvsBridgeInfo() {
            return ovsBridgeInfo;
        }

        public void setOvsBridgeInfo(List<OvsDpdkBridgeTO> ovsBridgeInfo) {
            this.ovsBridgeInfo = ovsBridgeInfo;
        }
    }

    public static class SyncVdpaRsp extends KVMAgentCommands.AgentResponse {
    }

    public static class OvsDpdkBridgeTO {
        public String name;
        public String physicalInterface;
        public String phyType;
        public List<OvsDpdkPortTO> ports;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getPhysicalInterface() {
            return physicalInterface;
        }

        public void setPhysicalInterface(String physicalInterface) {
            this.physicalInterface = physicalInterface;
        }

        public String getPhyType() {
            return phyType;
        }

        public void setPhyType(String phyType) {
            this.phyType = phyType;
        }

        public List<OvsDpdkPortTO> getPorts() {
            return ports;
        }

        public void setPorts(List<OvsDpdkPortTO> ports) {
            this.ports = ports;
        }
    }
    public static class OvsDpdkPortTO {
        public String type;
        public String nicInternalName;
        public String physicalInterface;
        public String vmUuid;
        public Integer vlanId;
        public String pciDeviceAddress;
        public String bridgeName;
        public VHostAddOn vHostAddOn;

        public String getBridgeName() {
            return bridgeName;
        }

        public void setBridgeName(String bridgeName) {
            this.bridgeName = bridgeName;
        }

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public String getNicInternalName() {
            return nicInternalName;
        }

        public void setNicInternalName(String nicInternalName) {
            this.nicInternalName = nicInternalName;
        }

        public String getPhysicalInterface() {
            return physicalInterface;
        }

        public void setPhysicalInterface(String physicalInterface) {
            this.physicalInterface = physicalInterface;
        }

        public String getVmUuid() {
            return vmUuid;
        }

        public void setVmUuid(String vmUuid) {
            this.vmUuid = vmUuid;
        }

        public Integer getVlanId() {
            return vlanId;
        }

        public void setVlanId(Integer vlanId) {
            this.vlanId = vlanId;
        }

        public String getPciDeviceAddress() {
            return pciDeviceAddress;
        }

        public void setPciDeviceAddress(String pciDeviceAddress) {
            this.pciDeviceAddress = pciDeviceAddress;
        }

        public VHostAddOn getvHostAddOn() {
            return vHostAddOn;
        }

        public void setvHostAddOn(VHostAddOn vHostAddOn) {
            this.vHostAddOn = vHostAddOn;
        }

        public static class VHostAddOn {
            private int queueNum;

            public int getQueueNum() {
                return queueNum;
            }

            public void setQueueNum(int queueNum) {
                this.queueNum = queueNum;
            }
        }
    }
}
