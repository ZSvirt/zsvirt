package org.zstack.kvm;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import org.zstack.core.validation.ConditionalValidation;
import org.zstack.header.HasThreadContext;
import org.zstack.header.agent.CancelCommand;
import org.zstack.header.core.validation.Validation;
import org.zstack.header.host.BlockDevices;
import org.zstack.header.host.HostNUMANode;
import org.zstack.header.host.Sensor;
import org.zstack.header.host.VmNicRedirectConfig;
import org.zstack.header.log.NoLogging;
import org.zstack.header.message.CancelTaskResult;
import org.zstack.header.vm.*;
import org.zstack.header.vm.additions.VmHostFileBackupJob;
import org.zstack.header.vm.devices.DeviceAddress;
import org.zstack.header.vm.devices.VirtualDeviceInfo;
import org.zstack.kvm.tpm.TpmTO;
import org.zstack.network.securitygroup.RuleTO;
import org.zstack.network.securitygroup.SecurityGroupMembersTO;
import org.zstack.network.securitygroup.VmNicSecurityTO;
import org.zstack.utils.opaque.OpaqueScripts;

import java.io.Serializable;
import java.util.*;
import java.util.stream.Collectors;

import static org.zstack.utils.CollectionDSL.e;
import static org.zstack.utils.CollectionDSL.map;
import static org.zstack.utils.CollectionUtils.transform;
import static org.zstack.utils.opaque.OpaqueConstants.OPAQUE_KEY_RESPONSE_ERROR;

public class KVMAgentCommands {
    public enum BootDev {
        hd(VmBootDevice.HardDisk),
        cdrom(VmBootDevice.CdRom),
        network(VmBootDevice.Network);

        private VmBootDevice device;

        BootDev(VmBootDevice dev) {
            device = dev;
        }

        public VmBootDevice toVmBootDevice() {
            return device;
        }
    }

    public static class AgentResponse implements ConditionalValidation, OpaqueScripts {
        private boolean success = true;
        private String error;

        public boolean isSuccess() {
            return success;
        }

        public void setSuccess(boolean success) {
            this.success = success;
        }

        public String getError() {
            return error;
        }

        public void setError(String error) {
            this.error = error;
            this.success = false;
        }

        @Override
        public boolean needValidation() {
            return success;
        }

        @Override
        @SuppressWarnings("unchecked")
        public Map<String, Object> opaqueScripts() {
            if (!success) {
                return map(
                    e(OPAQUE_KEY_RESPONSE_ERROR, error)
                );
            }
            return Collections.emptyMap();
        }
    }

    public static class AgentCommand {
        public LinkedHashMap kvmHostAddons;
    }

    public static class CheckVmStateCmd extends AgentCommand {
        public List<String> vmUuids;
        public String hostUuid;
    }

    public static class CheckVmStateRsp extends AgentResponse {
        public Map<String, String> states;
    }

    public static class UpdateVmPriorityCmd extends AgentCommand {
        public List<PriorityConfigStruct> priorityConfigStructs;
    }

    public static class UpdateVmPriorityRsp extends AgentResponse {
    }

    public static class UpdateVmCpuQuotaCmd extends AgentCommand {
        private String vmUuid;
        private long vmCpuQuota;

        public String getVmUuid() {
            return vmUuid;
        }

        public void setVmUuid(String vmUuid) {
            this.vmUuid = vmUuid;
        }

        public long getVmCpuQuota() {
            return vmCpuQuota;
        }

        public void setVmCpuQuota(long vmCpuQuota) {
            this.vmCpuQuota = vmCpuQuota;
        }
    }

    public static class UpdateVmCpuQuotaRsp extends AgentResponse {
    }

    public static class ChangeVmNicStateCommand extends AgentCommand {
        private String vmUuid;
        private String state;
        private NicTO nic;

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

        public String getState() {
            return state;
        }

        public void setState(String state) {
            this.state = state;
        }
    }

    public static class ChangeVmNicStateRsp extends AgentResponse {

    }

    public static class DetachNicCommand extends AgentCommand {
        private String vmUuid;
        private NicTO nic;

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
    }

    public static class DetachNicRsp extends AgentResponse {

    }

    public static class AttachNicCommand extends AgentCommand {
        private String vmUuid;
        private NicTO nic;
        private String accountUuid;
        private Map addons = new HashMap();

        public Map getAddons() {
            return addons;
        }

        public void setAddons(Map addons) {
            this.addons = addons;
        }

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

        public String getAccountUuid() {
            return accountUuid;
        }

        public void setAccountUuid(String accountUuid) {
            this.accountUuid = accountUuid;
        }
    }

    public static class AttachNicResponse extends AgentResponse {
        List<VirtualDeviceInfo> virtualDeviceInfoList;

        public List<VirtualDeviceInfo> getVirtualDeviceInfoList() {
            return virtualDeviceInfoList;
        }

        public void setVirtualDeviceInfoList(List<VirtualDeviceInfo> virtualDeviceInfoList) {
            this.virtualDeviceInfoList = virtualDeviceInfoList;
        }
    }

    public static class UpdateNicCmd extends AgentCommand implements VmAddOnsCmd {
        private String vmInstanceUuid;
        private List<KVMAgentCommands.NicTO> nics;
        private Map<String, Object> addons = new HashMap<>();
        private String accountUuid;

        public List<NicTO> getNics() {
            return nics;
        }

        public void setNics(List<NicTO> nics) {
            this.nics = nics;
        }

        public Map<String, Object> getAddons() {
            if (addons == null) {
                addons = new HashMap<>();
            }
            return addons;
        }

        public void setAddons(Map<String, Object> addons) {
            this.addons = addons;
        }

        @Override
        public String getVmInstanceUuid() {
            return vmInstanceUuid;
        }

        public void setVmInstanceUuid(String vmInstanceUuid) {
            this.vmInstanceUuid = vmInstanceUuid;
        }

        public String getAccountUuid() {
            return accountUuid;
        }

        public void setAccountUuid(String accountUuid) {
            this.accountUuid = accountUuid;
        }
    }

    public static class UpdateNicRsp extends AgentResponse {

    }

    public static class ConnectCmd extends AgentCommand {
        private String hostUuid;
        private String sendCommandUrl;
        private List<String> iptablesRules;
        private boolean ignoreMsrs;
        private boolean pageTableExtensionDisabled;
        private int tcpServerPort;
        private String version;

        public boolean isIgnoreMsrs() {
            return ignoreMsrs;
        }

        public void setIgnoreMsrs(boolean ignoreMsrs) {
            this.ignoreMsrs = ignoreMsrs;
        }

        public List<String> getIptablesRules() {
            return iptablesRules;
        }

        public void setIptablesRules(List<String> iptablesRules) {
            this.iptablesRules = iptablesRules;
        }

        public String getSendCommandUrl() {
            return sendCommandUrl;
        }

        public void setSendCommandUrl(String sendCommandUrl) {
            this.sendCommandUrl = sendCommandUrl;
        }

        public String getHostUuid() {
            return hostUuid;
        }

        public void setHostUuid(String hostUuid) {
            this.hostUuid = hostUuid;
        }

        public boolean isPageTableExtensionDisabled() {
            return pageTableExtensionDisabled;
        }

        public void setPageTableExtensionDisabled(boolean pageTableExtensionDisabled) {
            this.pageTableExtensionDisabled = pageTableExtensionDisabled;
        }

        public int getTcpServerPort() {
            return tcpServerPort;
        }

        public void setTcpServerPort(int tcpServerPort) {
            this.tcpServerPort = tcpServerPort;
        }

        public String getVersion() {
            return version;
        }

        public void setVersion(String version) {
            this.version = version;
        }
    }

    public static class ConnectResponse extends AgentResponse {
        private String libvirtVersion;
        private String qemuVersion;

        public String getLibvirtVersion() {
            return libvirtVersion;
        }

        public void setLibvirtVersion(String libvirtVersion) {
            this.libvirtVersion = libvirtVersion;
        }

        public String getQemuVersion() {
            return qemuVersion;
        }

        public void setQemuVersion(String qemuVersion) {
            this.qemuVersion = qemuVersion;
        }
    }

    public static class CreatePublicKeyCmd extends AgentCommand {
    }

    public static class CreatePublicKeyResponse extends AgentResponse {
    }

    public static class GetPublicKeyCmd extends AgentCommand {
    }

    public static class GetPublicKeyResponse extends AgentResponse {
        private String publicKey;

        public String getPublicKey() {
            return publicKey;
        }

        public void setPublicKey(String publicKey) {
            this.publicKey = publicKey;
        }
    }

    public static class RotatePublicKeyCmd extends AgentCommand {
    }

    public static class RotatePublicKeyResponse extends AgentResponse {
    }

    public static class VerifyPublicKeyCmd extends AgentCommand {
    }

    public static class VerifyPublicKeyResponse extends AgentResponse {
    }

    public static class SecretHostDefineCmd extends AgentCommand {
        /** Base64 envelope of DEK; agent expects this field name (encryptedDek). */
        private String encryptedDek;
        private String vmUuid;
        private String purpose;
        private Integer keyVersion;
        private String usageInstance;
        private String secretUuid;
        private String description;

        public String getEncryptedDek() {
            return encryptedDek;
        }

        public void setEncryptedDek(String encryptedDek) {
            this.encryptedDek = encryptedDek;
        }

        public String getVmUuid() {
            return vmUuid;
        }

        public void setVmUuid(String vmUuid) {
            this.vmUuid = vmUuid;
        }

        public String getPurpose() {
            return purpose;
        }

        public void setPurpose(String purpose) {
            this.purpose = purpose;
        }

        public Integer getKeyVersion() {
            return keyVersion;
        }

        public void setKeyVersion(Integer keyVersion) {
            this.keyVersion = keyVersion;
        }

        public String getUsageInstance() {
            return usageInstance;
        }

        public void setUsageInstance(String usageInstance) {
            this.usageInstance = usageInstance;
        }

        public String getSecretUuid() {
            return secretUuid;
        }

        public void setSecretUuid(String secretUuid) {
            this.secretUuid = secretUuid;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }
    }

    public static class SecretHostDefineResponse extends AgentResponse {
        private String secretUuid;

        public String getSecretUuid() {
            return secretUuid;
        }

        public void setSecretUuid(String secretUuid) {
            this.secretUuid = secretUuid;
        }
    }

    public static class SecretHostGetCmd extends AgentCommand {
        private String vmUuid;
        private String purpose;
        private Integer keyVersion;
        private String usageInstance;

        public String getVmUuid() {
            return vmUuid;
        }

        public void setVmUuid(String vmUuid) {
            this.vmUuid = vmUuid;
        }

        public String getPurpose() {
            return purpose;
        }

        public void setPurpose(String purpose) {
            this.purpose = purpose;
        }

        public Integer getKeyVersion() {
            return keyVersion;
        }

        public void setKeyVersion(Integer keyVersion) {
            this.keyVersion = keyVersion;
        }

        public String getUsageInstance() {
            return usageInstance;
        }

        public void setUsageInstance(String usageInstance) {
            this.usageInstance = usageInstance;
        }
    }

    public static class SecretHostGetResponse extends AgentResponse {
        private String secretUuid;

        public String getSecretUuid() {
            return secretUuid;
        }

        public void setSecretUuid(String secretUuid) {
            this.secretUuid = secretUuid;
        }
    }

    public static class ResolveVtpmLibvirtSecretCmd extends AgentCommand {
        private String vmUuid;

        public String getVmUuid() {
            return vmUuid;
        }

        public void setVmUuid(String vmUuid) {
            this.vmUuid = vmUuid;
        }
    }

    public static class ResolveVtpmLibvirtSecretResponse extends AgentResponse {
        private String secretUuid;

        public String getSecretUuid() {
            return secretUuid;
        }

        public void setSecretUuid(String secretUuid) {
            this.secretUuid = secretUuid;
        }
    }

    public static class SecretHostDeleteCmd extends AgentCommand {
        private String vmUuid;
        private String purpose;
        private Integer keyVersion;
        private String usageInstance;

        public String getVmUuid() {
            return vmUuid;
        }

        public void setVmUuid(String vmUuid) {
            this.vmUuid = vmUuid;
        }

        public String getPurpose() {
            return purpose;
        }

        public void setPurpose(String purpose) {
            this.purpose = purpose;
        }

        public Integer getKeyVersion() {
            return keyVersion;
        }

        public void setKeyVersion(Integer keyVersion) {
            this.keyVersion = keyVersion;
        }

        public String getUsageInstance() {
            return usageInstance;
        }

        public void setUsageInstance(String usageInstance) {
            this.usageInstance = usageInstance;
        }
    }

    public static class SecretHostDeleteResponse extends AgentResponse {
    }

    public static class PingCmd extends AgentCommand {
        public String hostUuid;
        public Map<String, Object> configs;
    }

    public static class PingResponse extends AgentResponse {
        private String hostUuid;
        private String sendCommandUrl;
        private String version;

        public String getHostUuid() {
            return hostUuid;
        }

        public void setHostUuid(String hostUuid) {
            this.hostUuid = hostUuid;
        }

        public String getSendCommandUrl() {
            return sendCommandUrl;
        }

        public void setSendCommandUrl(String sendCommandUrl) {
            this.sendCommandUrl = sendCommandUrl;
        }

        public String getVersion() {
            return version;
        }

        public void setVersion(String version) {
            this.version = version;
        }
    }

    public static class UpdateHostConfigurationCmd extends AgentCommand {
        public String hostUuid;
        public String sendCommandUrl;

        public String getHostUuid() {
            return hostUuid;
        }

        public void setHostUuid(String hostUuid) {
            this.hostUuid = hostUuid;
        }

        public String getSendCommandUrl() {
            return sendCommandUrl;
        }

        public void setSendCommandUrl(String sendCommandUrl) {
            this.sendCommandUrl = sendCommandUrl;
        }
    }

    public static class UpdateHostConfigurationResponse extends AgentResponse {

    }

    public static class CheckPhysicalNetworkInterfaceCmd extends AgentCommand {
        private List<String> interfaceNames = new ArrayList<>(2);

        public CheckPhysicalNetworkInterfaceCmd addInterfaceName(String name) {
            interfaceNames.add(name);
            return this;
        }

        public List<String> getInterfaceNames() {
            return interfaceNames;
        }

        public void setInterfaceNames(List<String> interfaceNames) {
            this.interfaceNames = interfaceNames;
        }
    }

    public static class CheckPhysicalNetworkInterfaceResponse extends AgentResponse {
        private List<String> failedInterfaceNames;

        public List<String> getFailedInterfaceNames() {
            if (failedInterfaceNames == null) {
                failedInterfaceNames = new ArrayList<String>(0);
            }
            return failedInterfaceNames;
        }

        public void setFailedInterfaceNames(List<String> failedInterfaceNames) {
            this.failedInterfaceNames = failedInterfaceNames;
        }
    }

    public static class HostFactCmd extends AgentCommand {
    }

    public static class HostFactResponse extends AgentResponse {
        private String osDistribution;
        private String osVersion;
        private String osRelease;
        private String qemuImgVersion;
        private String libvirtVersion;
        private String libvirtPackageVersion;
        private String hvmCpuFlag;
        private String eptFlag;
        private String cpuArchitecture;
        private String cpuModelName;
        private String cpuGHz;
        private Integer cpuProcessorNum;
        private Integer cpuSockets;
        private Integer cpuCoresPerSocket;
        private Integer cpuThreadsPerCore;
        private String powerSupplyModelName;
        private String powerSupplyManufacturer;
        private String ipmiAddress;
        private String powerSupplyMaxPowerCapacity;
        private String hostCpuModelName;
        private String systemProductName;
        private String systemSerialNumber;
        private String systemManufacturer;
        private String systemUUID;
        private String biosVendor;
        private String biosVersion;
        private String biosReleaseDate;
        private String bmcVersion;
        private String uptime;
        private String memorySlotsMaximum;
        private String cpuCache;
        private List<String> ipAddresses;
        private List<String> libvirtCapabilities;
        private VirtualizerInfoTO virtualizerInfo;
        private String iscsiInitiatorName;
        private String nqn;
        private String hostname;
        private String cpuFeatureMd5;

        public String getOsDistribution() {
            return osDistribution;
        }

        public void setOsDistribution(String osDistribution) {
            this.osDistribution = osDistribution;
        }

        public String getOsVersion() {
            return osVersion;
        }

        public void setOsVersion(String osVersion) {
            this.osVersion = osVersion;
        }

        public String getOsRelease() {
            return osRelease;
        }

        public void setOsRelease(String osRelease) {
            this.osRelease = osRelease;
        }

        public String getQemuImgVersion() {
            return qemuImgVersion;
        }

        public void setQemuImgVersion(String qemuImgVersion) {
            this.qemuImgVersion = qemuImgVersion;
        }

        public String getLibvirtVersion() {
            return libvirtVersion;
        }

        public void setLibvirtVersion(String libvirtVersion) {
            this.libvirtVersion = libvirtVersion;
        }

        public String getLibvirtPackageVersion() {
            return libvirtPackageVersion;
        }

        public void setLibvirtPackageVersion(String libvirtPackageVersion) {
            this.libvirtPackageVersion = libvirtPackageVersion;
        }

        public String getHvmCpuFlag() {
            return hvmCpuFlag;
        }

        public void setHvmCpuFlag(String hvmCpuFlag) {
            this.hvmCpuFlag = hvmCpuFlag;
        }

        public String getEptFlag() {
            return eptFlag;
        }

        public void setEptFlag(String eptFlag) {
            this.eptFlag = eptFlag;
        }

        public String getCpuArchitecture() {
            return cpuArchitecture;
        }

        public void setCpuArchitecture(String cpuArchitecture) {
            this.cpuArchitecture = cpuArchitecture;
        }

        public String getCpuModelName() {
            return cpuModelName;
        }

        public void setCpuModelName(String cpuModelName) {
            this.cpuModelName = cpuModelName;
        }

        public String getCpuGHz() {
            return cpuGHz;
        }

        public void setCpuGHz(String cpuGHz) {
            this.cpuGHz = cpuGHz;
        }

        public Integer getCpuProcessorNum() {
            return cpuProcessorNum;
        }

        public void setCpuProcessorNum(Integer cpuProcessorNum) {
            this.cpuProcessorNum = cpuProcessorNum;
        }

        public Integer getCpuSockets() {
            return cpuSockets;
        }

        public void setCpuSockets(Integer cpuSockets) {
            this.cpuSockets = cpuSockets;
        }

        public Integer getCpuCoresPerSocket() {
            return cpuCoresPerSocket;
        }

        public void setCpuCoresPerSocket(Integer cpuCoresPerSocket) {
            this.cpuCoresPerSocket = cpuCoresPerSocket;
        }

        public Integer getCpuThreadsPerCore() {
            return cpuThreadsPerCore;
        }

        public void setCpuThreadsPerCore(Integer cpuThreadsPerCore) {
            this.cpuThreadsPerCore = cpuThreadsPerCore;
        }

        public String getPowerSupplyModelName() {
            return powerSupplyModelName;
        }

        public void setPowerSupplyModelName(String powerSupplyModelName) {
            this.powerSupplyModelName = powerSupplyModelName;
        }

        public String getPowerSupplyManufacturer() {
            return powerSupplyManufacturer;
        }

        public void setPowerSupplyManufacturer(String powerSupplyManufacturer) {
            this.powerSupplyManufacturer = powerSupplyManufacturer;
        }

        public String getIpmiAddress() {
            return ipmiAddress;
        }

        public void setIpmiAddress(String ipmiAddress) {
            this.ipmiAddress = ipmiAddress;
        }

        public String getPowerSupplyMaxPowerCapacity() {
            return powerSupplyMaxPowerCapacity;
        }

        public void setPowerSupplyMaxPowerCapacity(String powerSupplyMaxPowerCapacity) {
            this.powerSupplyMaxPowerCapacity = powerSupplyMaxPowerCapacity;
        }

        public String getHostCpuModelName() {
            return hostCpuModelName;
        }

        public void setHostCpuModelName(String hostCpuModelName) {
            this.hostCpuModelName = hostCpuModelName;
        }

        public String getSystemProductName() {
            return systemProductName;
        }

        public void setSystemProductName(String systemProductName) {
            this.systemProductName = systemProductName;
        }

        public String getSystemSerialNumber() {
            return systemSerialNumber;
        }

        public void setSystemSerialNumber(String systemSerialNumber) {
            this.systemSerialNumber = systemSerialNumber;
        }

        public String getSystemManufacturer() {
            return systemManufacturer;
        }

        public void setSystemManufacturer(String systemManufacturer) {
            this.systemManufacturer = systemManufacturer;
        }

        public String getSystemUUID() {
            return systemUUID;
        }

        public void setSystemUUID(String systemUUID) {
            this.systemUUID = systemUUID;
        }

        public String getBiosVendor() {
            return biosVendor;
        }

        public void setBiosVendor(String biosVendor) {
            this.biosVendor = biosVendor;
        }

        public String getBiosVersion() {
            return biosVersion;
        }

        public void setBiosVersion(String biosVersion) {
            this.biosVersion = biosVersion;
        }

        public String getBiosReleaseDate() {
            return biosReleaseDate;
        }

        public void setBiosReleaseDate(String biosReleaseDate) {
            this.biosReleaseDate = biosReleaseDate;
        }

        public String getBmcVersion() {
            return bmcVersion;
        }

        public void setBmcVersion(String bmcVersion) {
            this.bmcVersion = bmcVersion;
        }

        public String getUptime() {
            return uptime;
        }

        public void setUptime(String uptime) {
            this.uptime = uptime;
        }

        public String getMemorySlotsMaximum() {
            return memorySlotsMaximum;
        }

        public void setMemorySlotsMaximum(String memorySlotsMaximum) {
            this.memorySlotsMaximum = memorySlotsMaximum;
        }

        public String getCpuCache() {
            return cpuCache;
        }

        public void setCpuCache(String cpuCache) {
            this.cpuCache = cpuCache;
        }

        public List<String> getIpAddresses() {
            return ipAddresses;
        }

        public void setIpAddresses(List<String> ipAddresses) {
            this.ipAddresses = ipAddresses;
        }

        public List<String> getLibvirtCapabilities() {
            return libvirtCapabilities;
        }

        public void setLibvirtCapabilities(List<String> libvirtCapabilities) {
            this.libvirtCapabilities = libvirtCapabilities;
        }

        public VirtualizerInfoTO getVirtualizerInfo() {
            return virtualizerInfo;
        }

        public void setVirtualizerInfo(VirtualizerInfoTO virtualizerInfo) {
            this.virtualizerInfo = virtualizerInfo;
        }

        public String getIscsiInitiatorName() {
            return iscsiInitiatorName;
        }

        public void setIscsiInitiatorName(String iscsiInitiatorName) {
            this.iscsiInitiatorName = iscsiInitiatorName;
        }

        public String getNqn() {
            return nqn;
        }

        public void setNqn(String nqn) {
            this.nqn = nqn;
        }

        public String getHostname() {
            return hostname;
        }

        public void setHostname(String hostname) {
            this.hostname = hostname;
        }

        public String getCpuFeatureMd5() {
            return cpuFeatureMd5;
        }

        public void setCpuFeatureMd5(String cpuFeatureMd5) {
            this.cpuFeatureMd5 = cpuFeatureMd5;
        }
    }

    public static class HostCapacityCmd extends AgentCommand {
    }

    public static class HostCapacityResponse extends AgentResponse {
        private long cpuNum;
        private long cpuSpeed;
        private long usedCpu;
        private long totalMemory;
        private long usedMemory;
        private int cpuSockets;
        private int cpuCoreNum;

        public int getCpuSockets() {
            return cpuSockets;
        }

        public void setCpuSockets(int cpuSockets) {
            this.cpuSockets = cpuSockets;
        }

        public int getCpuCoreNum() {
            return cpuCoreNum;
        }

        public void setCpuCoreNum(int cpuCoreNum) {
            this.cpuCoreNum = cpuCoreNum;
        }

        public long getCpuNum() {
            return cpuNum;
        }

        public void setCpuNum(long cpuNum) {
            this.cpuNum = cpuNum;
        }

        public long getCpuSpeed() {
            return cpuSpeed;
        }

        public void setCpuSpeed(long cpuSpeed) {
            this.cpuSpeed = cpuSpeed;
        }

        public long getUsedCpu() {
            return usedCpu;
        }

        public void setUsedCpu(long usedCpu) {
            this.usedCpu = usedCpu;
        }

        public long getTotalMemory() {
            return totalMemory;
        }

        public void setTotalMemory(long totalMemory) {
            this.totalMemory = totalMemory;
        }

        public long getUsedMemory() {
            return usedMemory;
        }

        public void setUsedMemory(long usedMemory) {
            this.usedMemory = usedMemory;
        }
    }

    public static class DeleteBridgeCmd extends AgentCommand {
        private String physicalInterfaceName;
        private String bridgeName;
        private String l2NetworkUuid;

        public String getPhysicalInterfaceName() {
            return physicalInterfaceName;
        }

        public void setPhysicalInterfaceName(String physicalInterfaceName) {
            this.physicalInterfaceName = physicalInterfaceName;
        }

        public String getBridgeName() {
            return bridgeName;
        }

        public void setBridgeName(String bridgeName) {
            this.bridgeName = bridgeName;
        }

        public String getL2NetworkUuid() {
            return l2NetworkUuid;
        }

        public void setL2NetworkUuid(String l2NetworkUuid) {
            this.l2NetworkUuid = l2NetworkUuid;
        }
    }




    public static class CreateBridgeCmd extends AgentCommand {
        private String physicalInterfaceName;
        private String bridgeName;
        private String l2NetworkUuid;
        private Boolean disableIptables;
        private Integer mtu;

        public String getL2NetworkUuid() {
            return l2NetworkUuid;
        }

        public void setL2NetworkUuid(String l2NetworkUuid) {
            this.l2NetworkUuid = l2NetworkUuid;
        }

        public String getPhysicalInterfaceName() {
            return physicalInterfaceName;
        }

        public void setPhysicalInterfaceName(String physicalInterfaceName) {
            this.physicalInterfaceName = physicalInterfaceName;
        }

        public String getBridgeName() {
            return bridgeName;
        }

        public void setBridgeName(String bridgeName) {
            this.bridgeName = bridgeName;
        }

        public Boolean getDisableIptables() {
            return disableIptables;
        }

        public void setDisableIptables(Boolean disableIptables) {
            this.disableIptables = disableIptables;
        }

        public Integer getMtu() {
            return mtu;
        }

        public void setMtu(Integer mtu) {
            this.mtu = mtu;
        }
    }


    public static class CreateBridgeResponse extends AgentResponse {
    }

    public static class UpdateBridgeCmd extends AgentCommand {
        private String physicalInterfaceName;
        private String bridgeName;
        private Integer oldVirtualNetworkId;
        private Integer newVirtualNetworkId;
        private String l2NetworkUuid;
        private List<String> peers;

        public String getPhysicalInterfaceName() {
            return physicalInterfaceName;
        }

        public void setPhysicalInterfaceName(String physicalInterfaceName) {
            this.physicalInterfaceName = physicalInterfaceName;
        }

        public String getBridgeName() {
            return bridgeName;
        }

        public void setBridgeName(String bridgeName) {
            this.bridgeName = bridgeName;
        }

        public Integer getOldVirtualNetworkId() {
            return oldVirtualNetworkId;
        }

        public void setOldVirtualNetworkId(Integer oldVirtualNetworkId) {
            this.oldVirtualNetworkId = oldVirtualNetworkId;
        }

        public Integer getNewVirtualNetworkId() {
            return newVirtualNetworkId;
        }

        public void setNewVirtualNetworkId(Integer newVirtualNetworkId) {
            this.newVirtualNetworkId = newVirtualNetworkId;
        }

        public String getL2NetworkUuid() {
            return l2NetworkUuid;
        }

        public void setL2NetworkUuid(String l2NetworkUuid) {
            this.l2NetworkUuid = l2NetworkUuid;
        }

        public List<String> getPeers() {
            return peers;
        }

        public void setPeers(List<String> peers) {
            this.peers = peers;
        }
    }

    public static class UpdateL2NetworkResponse extends AgentResponse {
    }

    public static class DeleteBridgeResponse extends AgentResponse {
    }
    public static class CheckBridgeCmd extends AgentCommand {
        private String physicalInterfaceName;
        private String bridgeName;

        public String getPhysicalInterfaceName() {
            return physicalInterfaceName;
        }

        public void setPhysicalInterfaceName(String physicalInterfaceName) {
            this.physicalInterfaceName = physicalInterfaceName;
        }

        public String getBridgeName() {
            return bridgeName;
        }

        public void setBridgeName(String bridgeName) {
            this.bridgeName = bridgeName;
        }
    }

    public static class CheckBridgeResponse extends AgentResponse {
    }

    public static class CheckVlanBridgeCmd extends CheckBridgeCmd {
        private int vlan;

        public int getVlan() {
            return vlan;
        }

        public void setVlan(int vlan) {
            this.vlan = vlan;
        }
    }


    public static class CheckVlanBridgeResponse extends CheckBridgeResponse {
    }

    public static class AddInterfaceToBridgeCmd extends KVMAgentCommands.AgentCommand {
        private String physicalInterfaceName;
        private String bridgeName;

        public String getPhysicalInterfaceName() {
            return physicalInterfaceName;
        }

        public void setPhysicalInterfaceName(String physicalInterfaceName) {
            this.physicalInterfaceName = physicalInterfaceName;
        }

        public String getBridgeName() {
            return bridgeName;
        }

        public void setBridgeName(String bridgeName) {
            this.bridgeName = bridgeName;
        }
    }

    public static class AddInterfaceToBridgeResponse extends KVMAgentCommands.AgentResponse {
    }

    public static class DeleteVlanBridgeCmd extends DeleteBridgeCmd {
        private int vlan;

        public int getVlan() {
            return vlan;
        }

        public void setVlan(int vlan) {
            this.vlan = vlan;
        }
    }

    public static class DeleteVlanBridgeResponse extends DeleteBridgeResponse {
    }


    public static class CreateVlanBridgeCmd extends CreateBridgeCmd {
        private int vlan;

        public int getVlan() {
            return vlan;
        }

        public void setVlan(int vlan) {
            this.vlan = vlan;
        }
    }

    public static class CreateVlanBridgeResponse extends CreateBridgeResponse {
    }

    public static class VHostAddOn {
        private int queueNum;
        private String rxBufferSize;
        private String txBufferSize;

        public int getQueueNum() {
            return queueNum;
        }

        public void setQueueNum(int queueNum) {
            this.queueNum = queueNum;
        }

        public String getRxBufferSize() {
            return rxBufferSize;
        }

        public void setRxBufferSize(String rxBufferSize) {
            this.rxBufferSize = rxBufferSize;
        }

        public String getTxBufferSize() {
            return txBufferSize;
        }

        public void setTxBufferSize(String txBufferSize) {
            this.txBufferSize = txBufferSize;
        }
    }

    public static class NicTO extends BaseVirtualDeviceTO {
        private String mac;
        private List<String> ips;
        private String bridgeName;
        // run `bridge fdb add NicTO.mac dev NicTO.physicalInterface` on vnics to allow vf <-> vnic communication
        private String physicalInterface;
        private String uuid;
        private String nicInternalName;
        private int deviceId;
        private String metaData;
        private Boolean useVirtio;
        private int bootOrder;
        private Integer mtu;
        private String driverType;
        private VHostAddOn vHostAddOn;
        private DeviceAddress pci;
        private String type;
        private String state;

        // only for vf nic
        private String vlanId;
        private String pciDeviceAddress;
        // only for tf nic
        private String ipForTf;
        private String l2NetworkUuid;

        // for vDPA & dpdkvhostuserclient nic
        private String srcPath;
        
        private Boolean cleanTraffic;

        public List<String> getIps() {
            return ips;
        }

        public void setIps(List<String> ips) {
            this.ips = ips;
        }

        public String getUuid() {
            return uuid;
        }

        public void setUuid(String uuid) {
            this.uuid = uuid;
        }

        public Boolean getUseVirtio() {
            return useVirtio;
        }

        public void setUseVirtio(Boolean useVirtio) {
            this.useVirtio = useVirtio;
        }

        public String getMac() {
            return mac;
        }

        public void setMac(String mac) {
            this.mac = mac;
        }

        public String getBridgeName() {
            return bridgeName;
        }

        public void setBridgeName(String bridgeName) {
            this.bridgeName = bridgeName;
        }

        public String getPhysicalInterface() {
            return physicalInterface;
        }

        public void setPhysicalInterface(String physicalInterface) {
            this.physicalInterface = physicalInterface;
        }

        public int getDeviceId() {
            return deviceId;
        }

        public void setDeviceId(int deviceId) {
            this.deviceId = deviceId;
        }

        public String getMetaData() {
            return metaData;
        }

        public void setMetaData(String metaData) {
            this.metaData = metaData;
        }

        public String getNicInternalName() {
            return nicInternalName;
        }

        public void setNicInternalName(String nicInternalName) {
            this.nicInternalName = nicInternalName;
        }

        public int getBootOrder() {
            return bootOrder;
        }

        public void setBootOrder(int bootOrder) {
            this.bootOrder = bootOrder;
		}

        public String getVlanId() {
            return vlanId;
        }

        public void setVlanId(String vlanId) {
            this.vlanId = vlanId;
        }

        public String getPciDeviceAddress() {
            return pciDeviceAddress;
        }

        public void setPciDeviceAddress(String pciDeviceAddress) {
            this.pciDeviceAddress = pciDeviceAddress;
        }

        public Integer getMtu() {
            return mtu;
        }

        public void setMtu(Integer mtu) {
            this.mtu = mtu;
        }

        public String getDriverType() {
            return driverType;
        }

        public void setDriverType(String driverType) {
            this.driverType = driverType;
        }

        public VHostAddOn getvHostAddOn() {
            return vHostAddOn;
        }

        public void setvHostAddOn(VHostAddOn vHostAddOn) {
            this.vHostAddOn = vHostAddOn;
        }

        public DeviceAddress getPci() {
            return pci;
        }

        public void setPci(DeviceAddress pci) {
            this.pci = pci;
        }

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public String getState() {
            return state;
        }

        public void setState(String state) {
            this.state = state;
        }

        public String getSrcPath() {
            return srcPath;
        }

        public void setSrcPath(String srcPath) {
            this.srcPath = srcPath;
        }

        public Boolean getCleanTraffic() {
            return cleanTraffic;
        }

        public void setCleanTraffic(Boolean cleanTraffic) {
            this.cleanTraffic = cleanTraffic;
        }

        public String getIpForTf() {
            return ipForTf;
        }

        public void setIpForTf(String ipForTf) {
            this.ipForTf = ipForTf;
        }

        public String getL2NetworkUuid() {
            return l2NetworkUuid;
        }

        public void setL2NetworkUuid(String l2NetworkUuid) {
            this.l2NetworkUuid = l2NetworkUuid;
        }

        public static NicTO fromVmNicInventory(VmNicInventory nic) {
            KVMAgentCommands.NicTO to = new KVMAgentCommands.NicTO();
            to.setMac(nic.getMac());
            to.setUuid(nic.getUuid());
            to.setDeviceId(nic.getDeviceId());
            to.setNicInternalName(nic.getInternalName());
            to.setType(nic.getType());

            return to;
        }
    }

    public static class VolumeSnapshotJobTO {
        public String vmInstanceUuid;
        public String installPath;
        public String previousInstallPath;
        public String newVolumeInstallPath;
        public String live;
        public String full;

        public VolumeSnapshotJobTO() {
        }

        public String getVmInstanceUuid() {
            return vmInstanceUuid;
        }

        public void setVmInstanceUuid(String vmInstanceUuid) {
            this.vmInstanceUuid = vmInstanceUuid;
        }

        public String getInstallPath() {
            return installPath;
        }

        public void setInstallPath(String installPath) {
            this.installPath = installPath;
        }

        public String getPreviousInstallPath() {
            return previousInstallPath;
        }

        public void setPreviousInstallPath(String previousInstallPath) {
            this.previousInstallPath = previousInstallPath;
        }

        public String getNewVolumeInstallPath() {
            return newVolumeInstallPath;
        }

        public void setNewVolumeInstallPath(String newVolumeInstallPath) {
            this.newVolumeInstallPath = newVolumeInstallPath;
        }

        public String getLive() {
            return live;
        }

        public void setLive(String live) {
            this.live = live;
        }

        public String getFull() {
            return full;
        }

        public void setFull(String full) {
            this.full = full;
        }
    }

    public static class DetachDataVolumeCmd extends AgentCommand {
        private VolumeTO volume;
        private String vmInstanceUuid;

        public VolumeTO getVolume() {
            return volume;
        }

        public void setVolume(VolumeTO volume) {
            this.volume = volume;
        }

        public String getVmUuid() {
            return vmInstanceUuid;
        }

        public void setVmUuid(String vmInstanceUuid) {
            this.vmInstanceUuid = vmInstanceUuid;
        }
    }

    public static class DetachDataVolumeResponse extends AgentResponse {
    }

    public static class AttachDataVolumeCmd extends AgentCommand {
        private VolumeTO volume;
        private String vmInstanceUuid;
        private Map<String, Object> addons;

        public Map<String, Object> getAddons() {
            if (addons == null) {
                addons = new HashMap<>();
            }
            return addons;
        }

        public void setAddons(Map<String, Object> addons) {
            this.addons = addons;
        }

        public VolumeTO getVolume() {
            return volume;
        }

        public void setVolume(VolumeTO volume) {
            this.volume = volume;
        }

        public String getVmUuid() {
            return vmInstanceUuid;
        }

        public void setVmUuid(String vmInstanceUuid) {
            this.vmInstanceUuid = vmInstanceUuid;
        }
    }

    public static class AttachDataVolumeResponse extends AgentResponse {
        List<VirtualDeviceInfo> virtualDeviceInfoList;

        public List<VirtualDeviceInfo> getVirtualDeviceInfoList() {
            return virtualDeviceInfoList;
        }

        public void setVirtualDeviceInfoList(List<VirtualDeviceInfo> virtualDeviceInfoList) {
            this.virtualDeviceInfoList = virtualDeviceInfoList;
        }
    }

    public static class IsoTO extends BaseVirtualDeviceTO {
        protected String path;
        protected String imageUuid;
        protected String primaryStorageUuid;
        protected String protocol;
        protected int deviceId;

        public IsoTO() {
        }

        public IsoTO(IsoTO other) {
            this.resourceUuid = other.resourceUuid;
            this.path = other.path;
            this.imageUuid = other.imageUuid;
            this.deviceId = other.deviceId;
            this.primaryStorageUuid = other.primaryStorageUuid;
            this.protocol = other.protocol;
        }

        public String getImageUuid() {
            return imageUuid;
        }

        public void setImageUuid(String imageUuid) {
            this.imageUuid = imageUuid;
        }

        public String getPath() {
            return path;
        }

        public void setPath(String path) {
            this.path = path;
        }

        public int getDeviceId() {
            return deviceId;
        }

        public void setDeviceId(int deviceId) {
            this.deviceId = deviceId;
        }

        public void setPrimaryStorageUuid(String primaryStorageUuid) {
            this.primaryStorageUuid = primaryStorageUuid;
        }

        public String getPrimaryStorageUuid() {
            return primaryStorageUuid;
        }

        public void setProtocol(String protocol) {
            this.protocol = protocol;
        }

        public String getProtocol() {
            return protocol;
        }
    }

    public static class CdRomTO extends IsoTO {
        // unmounted iso
        private boolean isEmpty;
        private int bootOrder;

        public CdRomTO() {
        }

        public CdRomTO(CdRomTO other) {
            this.isEmpty = other.isEmpty;
            this.path = other.path;
            this.imageUuid = other.imageUuid;
            this.deviceId = other.deviceId;
            this.bootOrder = other.bootOrder;
        }

        public boolean isEmpty() {
            return isEmpty;
        }

        public void setEmpty(boolean empty) {
            this.isEmpty = empty;
        }

        public int getBootOrder() {
            return bootOrder;
        }

        public void setBootOrder(int bootOrder) {
            this.bootOrder = bootOrder;
        }
    }

    public static class GenerateVdpaCmd extends AgentCommand {
        public String vmUuid;
        public List<NicTO> nics;

        public String getVmUuid() {
            return vmUuid;
        }

        public void setVmUuid(String vmUuid) {
            this.vmUuid = vmUuid;
        }

        public List<NicTO> getNics() {
            return nics;
        }

        public void setNics(List<NicTO> nics) {
            this.nics = nics;
        }
    }

    public static class GenerateVdpaResponse extends AgentResponse {
        public List<String> vdpaPaths;

        public List<String> getVdpaPaths() {
            return vdpaPaths;
        }

        public void setVdpaPaths(List<String> vdpaPaths) {
            this.vdpaPaths = vdpaPaths;
        }
    }

    public static class DeleteVdpaCmd extends AgentCommand {
        public String vmUuid;
        public String nicInternalName;

        public String getVmUuid() {
            return vmUuid;
        }

        public void setVmUuid(String vmUuid) {
            this.vmUuid = vmUuid;
        }

        public String getNicInternalName() {
            return nicInternalName;
        }

        public void setNicInternalName(String nicInternalName) {
            this.nicInternalName = nicInternalName;
        }
    }

    public static class DeleteVdpaRsp extends AgentResponse {
    }

    public static class GenerateVHostUserClientCmd extends AgentCommand {
        public String vmUuid;
        public List<NicTO> nics;

        public String getVmUuid() {
            return vmUuid;
        }

        public void setVmUuid(String vmUuid) {
            this.vmUuid = vmUuid;
        }

        public List<NicTO> getNics() {
            return nics;
        }

        public void setNics(List<NicTO> nics) {
            this.nics = nics;
        }
    }

    public static class GenerateVHostUserClientResponse extends AgentResponse {

    }

    public static class DeleteVHostUserClientCmd extends AgentCommand {
        public String vmUuid;
        public String nicInternalName;

        public String getVmUuid() {
            return vmUuid;
        }

        public void setVmUuid(String vmUuid) {
            this.vmUuid = vmUuid;
        }

        public String getNicInternalName() {
            return nicInternalName;
        }

        public void setNicInternalName(String nicInternalName) {
            this.nicInternalName = nicInternalName;
        }
    }

    public static class DeleteVHostUserClientRsp extends AgentResponse {

    }
    public static class CleanVmFirmwareFlashCmd extends AgentCommand {
        public String vmUuid;
    }

    public static class HardenVmConsoleCmd extends AgentCommand {
        public String vmUuid;
        public Long vmInternalId;
        public String hostManagementIp;
    }

    public static class DeleteVmConsoleFirewallCmd extends AgentCommand {
        public String vmUuid;
        public Long vmInternalId;
        public String hostManagementIp;
    }

    public interface VmAddOnsCmd {
        Map<String, Object> getAddons();

        String getVmInstanceUuid();
    }

    public static class PriorityConfig {
        private String vmUuid;
        private int cpuShares;
        private int oomScoreAdj;

        PriorityConfig(VmPriorityConfigVO vo, String vmUuid) {
            this.vmUuid = vmUuid;
            this.cpuShares = vo.getCpuShares();
            this.oomScoreAdj = vo.getOomScoreAdj();
        }

        public int getCpuShares() {
            return cpuShares;
        }

        public void setCpuShares(int cpuShares) {
            this.cpuShares = cpuShares;
        }

        public int getOomScoreAdj() {
            return oomScoreAdj;
        }

        public void setOomScoreAdj(int oomScoreAdj) {
            this.oomScoreAdj = oomScoreAdj;
        }

        public String getVmUuid() {
            return vmUuid;
        }

        public void setVmUuid(String vmUuid) {
            this.vmUuid = vmUuid;
        }
    }

    public static class vdiCmd extends AgentCommand implements Serializable {
        private String consoleMode;
        private String videoType;
        private String soundType;
        private String spiceStreamingMode;
        private Integer VDIMonitorNumber;
        @NoLogging
        private String consolePassword;
        private Map<String, String> qxlMemory;
        private List<String> spiceChannels;

        public String getConsoleMode() {
            return consoleMode;
        }

        public void setConsoleMode(String consoleMode) {
            this.consoleMode = consoleMode;
        }

        public String getVideoType() {
            return videoType;
        }

        public void setVideoType(String videoType) {
            this.videoType = videoType;
        }

        public String getSoundType() {
            return soundType;
        }

        public void setSoundType(String soundType) {
            this.soundType = soundType;
        }

        public String getSpiceStreamingMode() {
            return spiceStreamingMode;
        }

        public void setSpiceStreamingMode(String spiceStreamingMode) {
            this.spiceStreamingMode = spiceStreamingMode;
        }

        public Integer getVDIMonitorNumber() {
            return VDIMonitorNumber;
        }

        public void setVDIMonitorNumber(Integer VDIMonitorNumber) {
            this.VDIMonitorNumber = VDIMonitorNumber;
        }

        public String getConsolePassword() {
            return consolePassword;
        }

        public void setConsolePassword(String consolePassword) {
            this.consolePassword = consolePassword;
        }

        public Map<String, String> getQxlMemory() {
            return qxlMemory;
        }

        public void setQxlMemory(Map<String, String> qxlMemory) {
            this.qxlMemory = qxlMemory;
        }

        public List<String> getSpiceChannels() {
            return spiceChannels;
        }

        public void setSpiceChannels(List<String> spiceChannels) {
            this.spiceChannels = spiceChannels;
        }
    }

    public static class ConfigPrimaryVmCmd extends AgentCommand {
        private List<VmNicRedirectConfig> configs;
        private String vmInstanceUuid;
        private String hostIp;

        public String getVmInstanceUuid() {
            return vmInstanceUuid;
        }

        public void setVmInstanceUuid(String vmInstanceUuid) {
            this.vmInstanceUuid = vmInstanceUuid;
        }

        public List<VmNicRedirectConfig> getConfigs() {
            return configs;
        }

        public void setConfigs(List<VmNicRedirectConfig> configs) {
            this.configs = configs;
        }

        public String getHostIp() {
            return hostIp;
        }

        public void setHostIp(String hostIp) {
            this.hostIp = hostIp;
        }
    }

    public static class RegisterPrimaryVmHeartbeatCmd extends AgentCommand {
        private String hostUuid;
        private String vmInstanceUuid;
        private Integer heartbeatPort;
        private String targetHostIp;
        private boolean coloPrimary;
        private Integer redirectNum;
        private List<VolumeTO> volumes;

        public Integer getRedirectNum() {
            return redirectNum;
        }

        public void setRedirectNum(Integer redirectNum) {
            this.redirectNum = redirectNum;
        }

        public String getHostUuid() {
            return hostUuid;
        }

        public void setHostUuid(String hostUuid) {
            this.hostUuid = hostUuid;
        }

        public String getVmInstanceUuid() {
            return vmInstanceUuid;
        }

        public void setVmInstanceUuid(String vmInstanceUuid) {
            this.vmInstanceUuid = vmInstanceUuid;
        }

        public Integer getHeartbeatPort() {
            return heartbeatPort;
        }

        public void setHeartbeatPort(Integer heartbeatPort) {
            this.heartbeatPort = heartbeatPort;
        }

        public String getTargetHostIp() {
            return targetHostIp;
        }

        public void setTargetHostIp(String targetHostIp) {
            this.targetHostIp = targetHostIp;
        }

        public boolean isColoPrimary() {
            return coloPrimary;
        }

        public void setColoPrimary(boolean coloPrimary) {
            this.coloPrimary = coloPrimary;
        }

        public List<VolumeTO> getVolumes() {
            return volumes;
        }

        public void setVolumes(List<VolumeTO> volumes) {
            this.volumes = volumes;
        }
    }

    public static class StartColoSyncCmd extends AgentCommand {
        private String vmInstanceUuid;
        private Integer blockReplicationPort;
        private Integer nbdServerPort;
        private String secondaryVmHostIp;
        private Long checkpointDelay;
        private boolean fullSync;
        private List<VolumeTO> volumes = new ArrayList<>();
        private List<NicTO> nics = new ArrayList<>();

        public String getVmInstanceUuid() {
            return vmInstanceUuid;
        }

        public void setVmInstanceUuid(String vmInstanceUuid) {
            this.vmInstanceUuid = vmInstanceUuid;
        }

        public Integer getBlockReplicationPort() {
            return blockReplicationPort;
        }

        public void setBlockReplicationPort(Integer blockReplicationPort) {
            this.blockReplicationPort = blockReplicationPort;
        }

        public Integer getNbdServerPort() {
            return nbdServerPort;
        }

        public void setNbdServerPort(Integer nbdServerPort) {
            this.nbdServerPort = nbdServerPort;
        }

        public String getSecondaryVmHostIp() {
            return secondaryVmHostIp;
        }

        public void setSecondaryVmHostIp(String secondaryVmHostIp) {
            this.secondaryVmHostIp = secondaryVmHostIp;
        }

        public Long getCheckpointDelay() {
            return checkpointDelay;
        }

        public void setCheckpointDelay(Long checkpointDelay) {
            this.checkpointDelay = checkpointDelay;
        }

        public boolean isFullSync() {
            return fullSync;
        }

        public void setFullSync(boolean fullSync) {
            this.fullSync = fullSync;
        }

        public List<VolumeTO> getVolumes() {
            return volumes;
        }

        public void setVolumes(List<VolumeTO> volumes) {
            this.volumes = volumes;
        }

        public List<NicTO> getNics() {
            return nics;
        }

        public void setNics(List<NicTO> nics) {
            this.nics = nics;
        }
    }

    public static class ConfigSecondaryVmCmd extends AgentCommand {
        private String vmInstanceUuid;
        private Integer mirrorPort;
        private Integer secondaryInPort;
        private Integer nbdServerPort;
        private String primaryVmHostIp;

        public Integer getMirrorPort() {
            return mirrorPort;
        }

        public void setMirrorPort(Integer mirrorPort) {
            this.mirrorPort = mirrorPort;
        }

        public Integer getSecondaryInPort() {
            return secondaryInPort;
        }

        public void setSecondaryInPort(Integer secondaryInPort) {
            this.secondaryInPort = secondaryInPort;
        }

        public String getVmInstanceUuid() {
            return vmInstanceUuid;
        }

        public void setVmInstanceUuid(String vmInstanceUuid) {
            this.vmInstanceUuid = vmInstanceUuid;
        }

        public String getPrimaryVmHostIp() {
            return primaryVmHostIp;
        }

        public void setPrimaryVmHostIp(String primaryVmHostIp) {
            this.primaryVmHostIp = primaryVmHostIp;
        }

        public Integer getNbdServerPort() {
            return nbdServerPort;
        }

        public void setNbdServerPort(Integer nbdServerPort) {
            this.nbdServerPort = nbdServerPort;
        }
    }

    public static class StartVmCmd extends vdiCmd implements VmAddOnsCmd {
        private String accountUuid;
        private String vmInstanceUuid;
        private long vmInternalId;
        private String vmName;
        private String imagePlatform;
        private String imageArchitecture;
        private long memory;
        private long maxMemory;
        private long reservedMemory;
        private int cpuNum;
        private int maxVcpuNum;
        private long cpuSpeed;
        // cpu topology
        private Integer socketNum;
        private Integer cpuOnSocket;
        // set thread per core default 1 to keep backward compatibility
        private Integer threadsPerCore = 1;

        private List<String> bootDev;
        private VolumeTO rootVolume;
        private VirtualDeviceInfo memBalloon;
        private List<IsoTO> bootIso = new ArrayList<>();
        private List<CdRomTO> cdRoms = new ArrayList<>();
        private List<VolumeTO> dataVolumes;
        private List<VolumeTO> cacheVolumes;
        private VolumeTO nvRam;
        private List<NicTO> nics;
        private TpmTO tpm;
        private long timeout;
        private Map<String, Object> addons;
        private boolean instanceOfferingOnlineChange;
        private String nestedVirtualization;
        private String hostManagementIp;
        private String clock;
        private String clockTrack;
        private boolean useNuma;
        private String MemAccess;
        private boolean usbRedirect;
        private boolean enableSecurityElement;
        private boolean useBootMenu;
        private Integer bootMenuSplashTimeout;
        private boolean createPaused;
        private boolean kvmHiddenState;
        private boolean vmPortOff;
        private String vmCpuModel;
        private boolean emulateHyperV;
        private long vmCpuQuota;

        // hyperv features
        private boolean hypervClock;
        private String vendorId;

        // suspend features
        private boolean suspendToRam;
        private boolean suspendToDisk;

        private boolean additionalQmp;
        private boolean isApplianceVm;
        private String systemSerialNumber;
        private String bootMode;
        // used when bootMode == 'UEFI'
        private boolean secureBoot;
        private String edkVersion;

        private boolean fromForeignHypervisor;
        private String machineType;
        private Integer pciePortNums;
        private Integer predefinedPciBridgeNum;
        private boolean useHugePage;
        private String chassisAssetTag;
        private PriorityConfigStruct priorityConfigStruct;
        private String memorySnapshotPath;
        @NoLogging
        private String vmXml;
        private boolean coloPrimary;
        private boolean coloSecondary;
        private boolean consoleLogToFile;
        private boolean acpi;
        private boolean x2apic = true;
        // cpuid hypervisor feature
        private boolean cpuHypervisorFeature = true;
        private List<String> oemStrings = new ArrayList<>();

        // TODO: only for test
        private boolean useColoBinary;
        private String vmCpuVendorId;
        private boolean qemu64BitPciMmioSetup;

        public String getVmXml() {
            return vmXml;
        }

        public void setVmXml(String vmXml) {
            this.vmXml = vmXml;
        }

        public boolean isQemu64BitPciMmioSetup() {
            return qemu64BitPciMmioSetup;
        }

        public void setQemu64BitPciMmioSetup(boolean qemu64BitPciMmioSetup) {
            this.qemu64BitPciMmioSetup = qemu64BitPciMmioSetup;
        }

        public long getVmCpuQuota() {
            return vmCpuQuota;
        }

        public void setVmCpuQuota(long vmCpuQuota) {
            this.vmCpuQuota = vmCpuQuota;
        }

        public void setSocketNum(Integer socketNum) {
            this.socketNum = socketNum;
        }

        public void setCpuOnSocket(Integer cpuOnSocket) {
            this.cpuOnSocket = cpuOnSocket;
        }

        public void setThreadsPerCore(Integer threadsPerCore) {
            this.threadsPerCore = threadsPerCore;
        }

        public int getMaxVcpuNum() {
            return maxVcpuNum;
        }

        public void setMaxVcpuNum(int maxVcpuNum) {
            this.maxVcpuNum = maxVcpuNum;
        }

        public String getAccountUuid() {
            return accountUuid;
        }

        public void setAccountUuid(String accountUuid) {
            this.accountUuid = accountUuid;
        }

        public List<String> getOemStrings() {
            return oemStrings;
        }

        public void setOemStrings(List<String> oemStrings) {
            this.oemStrings = oemStrings.stream().distinct().collect(Collectors.toList());;
        }

        public String getChassisAssetTag() {
            return chassisAssetTag;
        }

        public void setChassisAssetTag(String chassisAssetTag) {
            this.chassisAssetTag = chassisAssetTag;
        }

        public PriorityConfigStruct getPriorityConfigStruct() {
            return priorityConfigStruct;
        }

        public void setPriorityConfigStruct(PriorityConfigStruct priorityConfigStruct) {
            this.priorityConfigStruct = priorityConfigStruct;
        }

        public boolean isUseHugePage() {
            return useHugePage;
        }

        public void setUseHugePage(boolean useHugePage) {
            this.useHugePage = useHugePage;
        }

        public boolean isFromForeignHypervisor() {
            return fromForeignHypervisor;
        }

        public void setFromForeignHypervisor(boolean fromForeignHypervisor) {
            this.fromForeignHypervisor = fromForeignHypervisor;
        }

        public String getMachineType() {
            return machineType;
        }

        public void setMachineType(String machineType) {
            this.machineType = machineType;
        }

        public void setPciePortNums(Integer pciePortNums) {
            this.pciePortNums = pciePortNums;
        }

        public Integer getPciePortNums() {
            return pciePortNums;
        }

        public boolean isAdditionalQmp() {
            return additionalQmp;
        }

        public void setAdditionalQmp(boolean additionalQmp) {
            this.additionalQmp = additionalQmp;
        }

        public String getBootMode() {
            return bootMode;
        }

        public void setBootMode(String bootMode) {
            this.bootMode = bootMode;
        }

        public boolean isSecureBoot() {
            return secureBoot;
        }

        public void setSecureBoot(boolean secureBoot) {
            this.secureBoot = secureBoot;
        }

        public String getEdkVersion() {
            return edkVersion;
        }

        public void setEdkVersion(String edkVersion) {
            this.edkVersion = edkVersion;
        }

        public boolean isEmulateHyperV() {
            return emulateHyperV;
        }

        public void setEmulateHyperV(boolean emulateHyperV) {
            this.emulateHyperV = emulateHyperV;
        }

        public boolean isApplianceVm() {
            return isApplianceVm;
        }

        public void setApplianceVm(boolean applianceVm) {
            isApplianceVm = applianceVm;
        }

        public String getSystemSerialNumber() {
            return systemSerialNumber;
        }

        public void setSystemSerialNumber(String systemSerialNumber) {
            this.systemSerialNumber = systemSerialNumber;
        }

        public String getVmCpuModel() {
            return vmCpuModel;
        }

        public void setVmCpuModel(String vmCpuModel) {
            this.vmCpuModel = vmCpuModel;
        }

        public boolean isKvmHiddenState() {
            return kvmHiddenState;
        }

        public void setKvmHiddenState(boolean kvmHiddenState) {
            this.kvmHiddenState = kvmHiddenState;
        }

        public void setVmPortOff(boolean vmPortOff) {
            this.vmPortOff = vmPortOff;
        }

        public boolean isVmPortOff() {
            return vmPortOff;
        }

        public void setUseBootMenu(boolean useBootMenu) {
            this.useBootMenu = useBootMenu;
        }

        public boolean isUseBootMenu() {
            return useBootMenu;
        }

        public Integer getBootMenuSplashTimeout() {
            return bootMenuSplashTimeout;
        }

        public void setBootMenuSplashTimeout(Integer bootMenuSplashTimeout) {
            this.bootMenuSplashTimeout = bootMenuSplashTimeout;
        }

        public void setCreatePaused(boolean createPaused) {
            this.createPaused = createPaused;
        }

        public boolean isCreatePaused() {
            return createPaused;
        }

        public boolean isUsbRedirect() {
            return usbRedirect;
        }

        public void setUsbRedirect(boolean usbRedirect) {
            this.usbRedirect = usbRedirect;
        }

        public boolean isEnableSecurityElement() {
            return enableSecurityElement;
        }

        public void setEnableSecurityElement(boolean enableSecurityElement) {
            this.enableSecurityElement = enableSecurityElement;
        }
        public boolean isUseNuma() {
            return useNuma;
        }

        public void setUseNuma(boolean useNuma) {
            this.useNuma = useNuma;
        }

        public String getMemAccess() {
            return MemAccess;
        }

        public void setMemAccess(String memAccess) {
            MemAccess = memAccess;
        }

        public long getMaxMemory() {
            return maxMemory;
        }

        public void setMaxMemory(long maxMemory) {
            this.maxMemory = maxMemory;
        }

        public long getReservedMemory() {
            return reservedMemory;
        }

        public void setReservedMemory(long reservedMemory) {
            this.reservedMemory = reservedMemory;
        }

        public String getClock() {
            return clock;
        }

        public void setClock(String clock) {
            this.clock = clock;
        }

        public String getClockTrack() {
            return clockTrack;
        }

        public void setClockTrack(String clockTrack) {
            this.clockTrack = clockTrack;
        }

        public Integer getSocketNum() {
            return socketNum;
        }

        public void setSocketNum(int socketNum) {
            this.socketNum = socketNum;
        }

        public Integer getCpuOnSocket() {
            return cpuOnSocket;
        }

        public void setCpuOnSocket(int cpuOnSocket) {
            this.cpuOnSocket = cpuOnSocket;
        }

        public Integer getThreadsPerCore() {
            return threadsPerCore;
        }

        public void setThreadsPerCore(int threadsPerCore) {
            this.threadsPerCore = threadsPerCore;
        }

        public String getVmName() {
            return vmName;
        }

        public void setVmName(String vmName) {
            this.vmName = vmName;
        }

        public String getImagePlatform() {
            return imagePlatform;
        }

        public void setImagePlatform(String imagePlatform) {
            this.imagePlatform = imagePlatform;
        }

        public String getImageArchitecture() {
            return imageArchitecture;
        }

        public void setImageArchitecture(String imageArchitecture) {
            this.imageArchitecture = imageArchitecture;
        }

        public long getMemory() {
            return memory;
        }

        public void setMemory(long memory) {
            this.memory = memory;
        }

        public int getCpuNum() {
            return cpuNum;
        }

        public void setCpuNum(int cpuNum) {
            this.cpuNum = cpuNum;
        }

        public long getCpuSpeed() {
            return cpuSpeed;
        }

        public void setCpuSpeed(long cpuSpeed) {
            this.cpuSpeed = cpuSpeed;
        }

        public List<String> getBootDev() {
            return bootDev;
        }

        public void setBootDev(List<String> bootDev) {
            this.bootDev = bootDev;
        }

        public boolean getInstanceOfferingOnlineChange() {
            return instanceOfferingOnlineChange;
        }

        public void setInstanceOfferingOnlineChange(boolean instanceOfferingOnlineChange) {
            this.instanceOfferingOnlineChange = instanceOfferingOnlineChange;
        }

        public List<CdRomTO> getCdRoms() {
            return cdRoms;
        }

        public void setCdRoms(List<CdRomTO> cdRoms) {
            this.cdRoms = cdRoms;
        }

        public String getNestedVirtualization() {
            return nestedVirtualization;
        }

        public void setNestedVirtualization(String nestedVirtualization) {
            this.nestedVirtualization = nestedVirtualization;
        }

        public String getHostManagementIp() {
            return hostManagementIp;
        }

        public void setHostManagementIp(String hostManagementIp) {
            this.hostManagementIp = hostManagementIp;
        }

        public VolumeTO getRootVolume() {
            return rootVolume;
        }

        public void setRootVolume(VolumeTO rootVolume) {
            this.rootVolume = rootVolume;
        }

        public VirtualDeviceInfo getMemBalloon() {
            return memBalloon;
        }

        public void setMemBalloon(VirtualDeviceInfo memBalloon) {
            this.memBalloon = memBalloon;
        }

        public List<VolumeTO> getDataVolumes() {
            return dataVolumes;
        }

        public void setDataVolumes(List<VolumeTO> dataVolumes) {
            this.dataVolumes = dataVolumes;
        }

        public List<VolumeTO> getCacheVolumes() {
            return cacheVolumes;
        }

        public void setCacheVolumes(List<VolumeTO> cacheVolumes) {
            this.cacheVolumes = cacheVolumes;
        }

        public VolumeTO getNvRam() {
            return nvRam;
        }

        public void setNvRam(VolumeTO nvRam) {
            this.nvRam = nvRam;
        }

        public List<NicTO> getNics() {
            return nics;
        }

        public void setNics(List<NicTO> nics) {
            this.nics = nics;
        }

        public TpmTO getTpm() {
            return tpm;
        }

        public void setTpm(TpmTO tpm) {
            this.tpm = tpm;
        }

        public long getTimeout() {
            return timeout;
        }

        public void setTimeout(long timeout) {
            this.timeout = timeout;
        }

        @Override
        public String getVmInstanceUuid() {
            return vmInstanceUuid;
        }

        public void setVmInstanceUuid(String vmInstanceUuid) {
            this.vmInstanceUuid = vmInstanceUuid;
        }

        public boolean isUseColoBinary() {
            return useColoBinary;
        }

        public void setUseColoBinary(boolean useColoBinary) {
            this.useColoBinary = useColoBinary;
        }

        public long getVmInternalId() {
            return vmInternalId;
        }

        public void setVmInternalId(long vmInternalId) {
            this.vmInternalId = vmInternalId;
        }

        public Integer getPredefinedPciBridgeNum() {
            return predefinedPciBridgeNum;
        }

        public void setPredefinedPciBridgeNum(Integer predefinedPciBridgeNum) {
            this.predefinedPciBridgeNum = predefinedPciBridgeNum;
        }

        public String getMemorySnapshotPath() {
            return memorySnapshotPath;
        }

        public void setMemorySnapshotPath(String memorySnapshotPath) {
            this.memorySnapshotPath = memorySnapshotPath;
        }

        public boolean isColoPrimary() {
            return coloPrimary;
        }

        public void setColoPrimary(boolean coloPrimary) {
            this.coloPrimary = coloPrimary;
        }

        public boolean isColoSecondary() {
            return coloSecondary;
        }

        public void setColoSecondary(boolean coloSecondary) {
            this.coloSecondary = coloSecondary;
        }

        public boolean isConsoleLogToFile() {
            return consoleLogToFile;
        }

        public void setConsoleLogToFile(boolean consoleLogToFile) {
            this.consoleLogToFile = consoleLogToFile;
        }

        public boolean getAcpi() {
            return acpi;
        }

        public void setAcpi(boolean acpi) {
            this.acpi = acpi;
        }

        public boolean getX2apic() {
            return x2apic;
        }

        public void setX2apic(boolean x2apic) {
            this.x2apic = x2apic;
        }

        public boolean isCpuHypervisorFeature() {
            return cpuHypervisorFeature;
        }

        public void setCpuHypervisorFeature(boolean cpuHypervisorFeature) {
            this.cpuHypervisorFeature = cpuHypervisorFeature;
        }

        public boolean isHypervClock() {
            return hypervClock;
        }

        public void setHypervClock(boolean hypervClock) {
            this.hypervClock = hypervClock;
        }

        public String getVendorId() {
            return vendorId;
        }

        public void setVendorId(String vendorId) {
            this.vendorId = vendorId;
        }

        public boolean isSuspendToRam() {
            return suspendToRam;
        }

        public void setSuspendToRam(boolean suspendToRam) {
            this.suspendToRam = suspendToRam;
        }

        public boolean isSuspendToDisk() {
            return suspendToDisk;
        }

        public void setSuspendToDisk(boolean suspendToDisk) {
            this.suspendToDisk = suspendToDisk;
        }

        @Override
        public Map<String, Object> getAddons() {
            if (addons == null) {
                addons = new HashMap<>();
            }
            return addons;
        }

        public void setAddons(Map<String, Object> addons) {
            this.addons = addons;
        }

        public String getVmCpuVendorId() {
            return vmCpuVendorId;
        }

        public void setVmCpuVendorId(String vmCpuVendorId) {
            this.vmCpuVendorId = vmCpuVendorId;
        }
    }

    public static class StartVmResponse extends VmDevicesInfoResponse {
    }

    public static class VmHostFileTO {
        private String path;
        /**
         * maybe "NvRam" or "TpmState" ...
         * @see org.zstack.header.vm.additions.VmHostFileType
         */
        private String type;
        /**
         * file format: Raw or TarballGzip
         * @see org.zstack.header.vm.additions.VmHostFileContentFormat
         */
        private String fileFormat;
        /**
         * operation: Write, Prepare, or Delete
         * only use in WriteVmHostFileContentCmd
         * @see org.zstack.header.vm.additions.VmHostFileOperation
         */
        private String operation;
        /**
         * null if operation is Prepare or Delete
         */
        @NoLogging(type = NoLogging.Type.LongText)
        private String contentBase64;
        private String error;

        public String getPath() {
            return path;
        }

        public void setPath(String path) {
            this.path = path;
        }

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public String getFileFormat() {
            return fileFormat;
        }

        public void setFileFormat(String fileFormat) {
            this.fileFormat = fileFormat;
        }

        public String getOperation() {
            return operation;
        }

        public void setOperation(String operation) {
            this.operation = operation;
        }

        public String getContentBase64() {
            return contentBase64;
        }

        public void setContentBase64(String contentBase64) {
            this.contentBase64 = contentBase64;
        }

        public String getError() {
            return error;
        }

        public void setError(String error) {
            this.error = error;
        }
    }

    public static class VmDevicesInfoResponse extends AgentResponse {
        private List<VmNicInfo> nicInfos;
        private List<VirtualDeviceInfo> virtualDeviceInfoList;
        private VirtualDeviceInfo memBalloonInfo;
        private VirtualizerInfoTO virtualizerInfo;
        private String edkRpm;
        @NoLogging
        private String vmXml;

        public VirtualDeviceInfo getMemBalloonInfo() {
            return memBalloonInfo;
        }

        public void setMemBalloonInfo(VirtualDeviceInfo memBalloonInfo) {
            this.memBalloonInfo = memBalloonInfo;
        }

        public List<VmNicInfo> getNicInfos() {
            return nicInfos;
        }

        public void setNicInfos(List<VmNicInfo> nicInfos) {
            this.nicInfos = nicInfos;
        }

        public List<VirtualDeviceInfo> getVirtualDeviceInfoList() {
            return virtualDeviceInfoList;
        }

        public void setVirtualDeviceInfoList(List<VirtualDeviceInfo> virtualDeviceInfoList) {
            this.virtualDeviceInfoList = virtualDeviceInfoList;
        }

        public VirtualizerInfoTO getVirtualizerInfo() {
            return virtualizerInfo;
        }

        public void setVirtualizerInfo(VirtualizerInfoTO virtualizerInfo) {
            this.virtualizerInfo = virtualizerInfo;
        }

        public String getEdkRpm() {
            return edkRpm;
        }

        public void setEdkRpm(String edkRpm) {
            this.edkRpm = edkRpm;
        }

        public String getVmXml() {
            return vmXml;
        }

        public void setVmXml(String vmXml) {
            this.vmXml = vmXml;
        }
    }

    public static class SyncVmDeviceInfoCmd extends AgentCommand {
        private String vmInstanceUuid;

        public String getVmInstanceUuid() {
            return vmInstanceUuid;
        }

        public void setVmInstanceUuid(String vmInstanceUuid) {
            this.vmInstanceUuid = vmInstanceUuid;
        }
    }

    public static class SyncVmDeviceInfoResponse extends VmDevicesInfoResponse {
    }

    public static class ReadVmHostFileContentCmd extends AgentCommand {
        /**
         * without contentBase64, fileFormat
         */
        private List<VmHostFileTO> hostFiles = new ArrayList<>();

        public List<String> getPaths() {
            return transform(hostFiles, VmHostFileTO::getPath);
        }

        public List<VmHostFileTO> getHostFiles() {
            return hostFiles;
        }

        public void setHostFiles(List<VmHostFileTO> hostFiles) {
            this.hostFiles = hostFiles;
        }
    }

    public static class ReadVmHostFileContentResponse extends AgentResponse {
        private List<VmHostFileTO> hostFiles = new ArrayList<>();

        public List<VmHostFileTO> getHostFiles() {
            return hostFiles;
        }

        public void setHostFiles(List<VmHostFileTO> hostFiles) {
            this.hostFiles = hostFiles;
        }
    }

    public static class WriteVmHostFileContentCmd extends AgentCommand {
        private List<VmHostFileTO> hostFiles = new ArrayList<>();

        public List<VmHostFileTO> getHostFiles() {
            return hostFiles;
        }

        public void setHostFiles(List<VmHostFileTO> hostFiles) {
            this.hostFiles = hostFiles;
        }
    }

    public static class WriteVmHostFileContentResponse extends AgentResponse {
    }

    public static class BackupVmHostFileCmd extends AgentCommand {
        private List<VmHostFileBackupJob> vmHostFileBackupJobs;

        public List<VmHostFileBackupJob> getVmHostFileBackupJobs() {
            return vmHostFileBackupJobs;
        }

        public void setVmHostFileBackupJobs(List<VmHostFileBackupJob> vmHostFileBackupJobs) {
            this.vmHostFileBackupJobs = vmHostFileBackupJobs;
        }
    }

    public static class BackupVmHostFileResponse extends AgentResponse {
    }

    public static class VmNicInfo {
        private String macAddress;
        private DeviceAddress deviceAddress;

        public String getMacAddress() {
            return macAddress;
        }

        public void setMacAddress(String macAddress) {
            this.macAddress = macAddress;
        }

        public DeviceAddress getDeviceAddress() {
            return deviceAddress;
        }

        public void setDeviceAddress(DeviceAddress deviceAddress) {
            this.deviceAddress = deviceAddress;
        }
    }

    public static class ChangeCpuMemoryCmd extends AgentCommand {
        private String vmUuid;
        private int cpuNum;
        private long memorySize;

        public void setVmUuid(String vmUuid) {
            this.vmUuid = vmUuid;
        }

        public String getVmUuid() {
            return vmUuid;
        }

        public void setCpuNum(int cpuNum) {
            this.cpuNum = cpuNum;
        }

        public int getCpuNum() {
            return cpuNum;
        }

        public void setMemorySize(long memorySize) {
            this.memorySize = memorySize;
        }

        public long getMemorySize() {
            return memorySize;
        }
    }

    public static class ChangeCpuMemoryResponse extends AgentResponse {
        private int cpuNum;
        private long memorySize;

        public void setCpuNum(int cpuNum) {
            this.cpuNum = cpuNum;
        }

        public int getCpuNum() {
            return cpuNum;
        }

        public void setMemorySize(long memorySize) {
            this.memorySize = memorySize;
        }

        public long getMemorySize() {
            return memorySize;
        }
    }

    public static class IncreaseCpuCmd extends AgentCommand {
        private String vmUuid;
        private int cpuNum;

        public void setVmUuid(String vmUuid) {
            this.vmUuid = vmUuid;
        }

        public String getVmUuid() {
            return vmUuid;
        }

        public void setCpuNum(int cpuNum) {
            this.cpuNum = cpuNum;
        }

        public int getCpuNum() {
            return cpuNum;
        }
    }

    public static class IncreaseCpuResponse extends AgentResponse {
        private int cpuNum;

        public void setCpuNum(int cpuNum) {
            this.cpuNum = cpuNum;
        }

        public int getCpuNum() {
            return cpuNum;
        }
    }

    public static class IncreaseMemoryCmd extends AgentCommand {
        private String vmUuid;
        private long memorySize;

        public void setVmUuid(String vmUuid) {
            this.vmUuid = vmUuid;
        }

        public String getVmUuid() {
            return vmUuid;
        }

        public void setMemorySize(long memorySize) {
            this.memorySize = memorySize;
        }

        public long getMemorySize() {
            return memorySize;
        }
    }

    public static class IncreaseMemoryResponse extends AgentResponse {
        private long memorySize;

        public void setMemorySize(long memorySize) {
            this.memorySize = memorySize;
        }

        public long getMemorySize() {
            return memorySize;
        }
    }

    public static class ScanVmPortCmd extends AgentCommand {
        private String ip;
        private String brname;
        private int port;

        public String getIp() {
            return ip;
        }

        public void setIp(String ip) {
            this.ip = ip;
        }

        public int getPort() {
            return port;
        }

        public void setPort(int port) {
            this.port = port;
        }

        public String getBrname() {
            return brname;
        }

        public void setBrname(String brname) {
            this.brname = brname;
        }
    }

    public static class ScanVmPortResponse extends AgentResponse {
        private Map<String, String> portStatus;

        public Map<String, String> getPortStatus() {
            return portStatus;
        }

        public void setPortStatus(Map<String, String> portStatus) {
            this.portStatus = portStatus;
        }
    }

    public static class GetVncPortCmd extends AgentCommand {
        private String vmUuid;

        public String getVmUuid() {
            return vmUuid;
        }

        public void setVmUuid(String vmUuid) {
            this.vmUuid = vmUuid;
        }
    }

    public static class GetVncPortResponse extends AgentResponse {
        private int port;
        private String protocol;
        private Integer vncPort;
        private Integer spicePort;
        private Integer spiceTlsPort;

        public int getPort() {
            return port;
        }

        public void setPort(int port) {
            this.port = port;
        }

        public String getProtocol() {
            return protocol;
        }

        public void setProtocol(String protocol) {
            this.protocol = protocol;
        }

        public Integer getVncPort() {
            return vncPort;
        }

        public void setVncPort(Integer vncPort) {
            this.vncPort = vncPort;
        }

        public Integer getSpicePort() {
            return spicePort;
        }

        public void setSpicePort(Integer spicePort) {
            this.spicePort = spicePort;
        }

        public Integer getSpiceTlsPort() {
            return spiceTlsPort;
        }

        public void setSpiceTlsPort(Integer spiceTlsPort) {
            this.spiceTlsPort = spiceTlsPort;
        }
    }

    public static class StopVmCmd extends AgentCommand {
        private String uuid;
        private String type;
        private long timeout;
        private boolean forceStopIfNoOperatingSystemDetected;
        private List<VmNicInventory> vmNics;

        public String getUuid() {
            return uuid;
        }

        public void setUuid(String uuid) {
            this.uuid = uuid;
        }

        public long getTimeout() {
            return timeout;
        }

        public void setTimeout(long timeout) {
            this.timeout = timeout;
        }

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public boolean isForceStopIfNoOperatingSystemDetected() {
            return forceStopIfNoOperatingSystemDetected;
        }

        public void setForceStopIfNoOperatingSystemDetected(boolean forceStopIfNoOperatingSystemDetected) {
            this.forceStopIfNoOperatingSystemDetected = forceStopIfNoOperatingSystemDetected;
        }

        public List<VmNicInventory> getVmNics() {
            return vmNics;
        }

        public void setVmNics(List<VmNicInventory> vmNics) {
            this.vmNics = vmNics;
        }
    }

    public static class StopVmResponse extends AgentResponse {
    }

    public static class PauseVmCmd extends AgentCommand {
        private String uuid;
        private long timeout;

        public String getUuid() {
            return uuid;
        }

        public void setUuid(String uuid) {
            this.uuid = uuid;
        }

        public long getTimeout() {
            return timeout;
        }

        public void setTimeout(long timeout) {
            this.timeout = timeout;
        }
    }

    public static class PauseVmResponse extends AgentResponse {

    }

    public static class ResumeVmCmd extends AgentCommand {
        private String uuid;
        private long timeout;

        public String getUuid() {
            return uuid;
        }

        public void setUuid(String uuid) {
            this.uuid = uuid;
        }

        public long getTimeout() {
            return timeout;
        }

        public void setTimeout(long timeout) {
            this.timeout = timeout;
        }
    }

    public static class ResumeVmResponse extends AgentResponse {

    }

    public static class RebootVmCmd extends AgentCommand {
        private String uuid;
        private long timeout;
        private List<String> bootDev;

        public List<String> getBootDev() {
            return bootDev;
        }

        public void setBootDev(List<String> bootDev) {
            this.bootDev = bootDev;
        }

        public String getUuid() {
            return uuid;
        }

        public void setUuid(String uuid) {
            this.uuid = uuid;
        }

        public long getTimeout() {
            return timeout;
        }

        public void setTimeout(long timeout) {
            this.timeout = timeout;
        }
    }

    public static class RebootVmResponse extends AgentResponse {
        private VirtualizerInfoTO virtualizerInfo;

        public VirtualizerInfoTO getVirtualizerInfo() {
            return virtualizerInfo;
        }

        public void setVirtualizerInfo(VirtualizerInfoTO virtualizerInfo) {
            this.virtualizerInfo = virtualizerInfo;
        }
    }

    public static class DestroyVmCmd extends AgentCommand {
        private String uuid;
        private List<VmNicInventory> vmNics;

        public String getUuid() {
            return uuid;
        }

        public void setUuid(String uuid) {
            this.uuid = uuid;
        }

        public List<VmNicInventory> getVmNics() {
            return vmNics;
        }

        public void setVmNics(List<VmNicInventory> vmNics) {
            this.vmNics = vmNics;
        }

    }

    public static class DestroyVmResponse extends AgentResponse {
    }

    public static class GetVmFirstBootDeviceCmd extends AgentCommand {
        private String uuid;

        public String getUuid() {
            return uuid;
        }

        public void setUuid(String uuid) {
            this.uuid = uuid;
        }
    }

    public static class GetVmFirstBootDeviceResponse extends AgentResponse {
        private String firstBootDevice;

        public String getFirstBootDevice() {
            return firstBootDevice;
        }

        public void setFirstBootDevice(String firstBootDevice) {
            this.firstBootDevice = firstBootDevice;
        }
    }

    public static class GetVmDeviceAddressCmd extends AgentCommand {
        private String uuid;
        private Map<String, List> deviceTOs = new HashMap<>();

        public void setUuid(String uuid) {
            this.uuid = uuid;
        }

        public String getUuid() {
            return uuid;
        }

        public void putDevice(String type, List deviceTOs) {
            this.deviceTOs.put(type, deviceTOs);
        }

        public Map<String, List> getDeviceTOs() {
            return deviceTOs;
        }

        public void setDeviceTOs(Map<String, List> deviceTOs) {
            this.deviceTOs = deviceTOs;
        }
    }

    public static class GetVmDeviceAddressRsp extends AgentResponse {
        private Map<String, List<VmDeviceAddressTO>> addresses;

        public Map<String, List<VmDeviceAddressTO>> getAddresses() {
            return addresses;
        }

        public List<VmDeviceAddressTO> getAddresses(String resourceType) {
            return addresses.get(resourceType);
        }

        public void setAddresses(Map<String, List<VmDeviceAddressTO>> addresses) {
            this.addresses = addresses;
        }
    }

    public static class GetVirtualizerInfoCmd extends AgentCommand {
        private List<String> vmUuids;

        public List<String> getVmUuids() {
            return vmUuids;
        }

        public void setVmUuids(List<String> vmUuids) {
            this.vmUuids = vmUuids;
        }
    }

    public static class GetVirtualizerInfoRsp extends AgentResponse {
        private VirtualizerInfoTO hostInfo;
        private List<VirtualizerInfoTO> vmInfoList;

        public VirtualizerInfoTO getHostInfo() {
            return hostInfo;
        }

        public void setHostInfo(VirtualizerInfoTO hostInfo) {
            this.hostInfo = hostInfo;
        }

        public List<VirtualizerInfoTO> getVmInfoList() {
            return vmInfoList;
        }

        public void setVmInfoList(List<VirtualizerInfoTO> vmInfoList) {
            this.vmInfoList = vmInfoList;
        }
    }

    public static class VirtualizerInfoTO {
        private String uuid;
        private String virtualizer;
        private String version;

        public String getUuid() {
            return uuid;
        }

        public void setUuid(String uuid) {
            this.uuid = uuid;
        }

        public void setVirtualizer(String virtualizer) {
            this.virtualizer = virtualizer;
        }

        public String getVirtualizer() {
            return virtualizer;
        }

        public void setVersion(String version) {
            this.version = version;
        }

        public String getVersion() {
            return version;
        }
    }

    public static class VmDeviceAddressTO {
        private String addressType;
        private String address;
        private String deviceType;
        private String uuid;

        public String getAddress() {
            return address;
        }

        public void setAddress(String address) {
            this.address = address;
        }

        public String getAddressType() {
            return addressType;
        }

        public void setAddressType(String addressType) {
            this.addressType = addressType;
        }

        public String getDeviceType() {
            return deviceType;
        }

        public void setDeviceType(String deviceType) {
            this.deviceType = deviceType;
        }

        public String getUuid() {
            return uuid;
        }

        public void setUuid(String uuid) {
            this.uuid = uuid;
        }
    }

    public static class VmSyncCmd extends AgentCommand {
    }

    public static class VmSyncResponse extends AgentResponse {
        private HashMap<String, String> states;

        private List<String> vmInShutdowns;

        public HashMap<String, String> getStates() {
            return states;
        }

        public void setStates(HashMap<String, String> states) {
            this.states = states;
        }

        public List<String> getVmInShutdowns() {
            return vmInShutdowns == null ? Collections.emptyList() : vmInShutdowns;
        }

        public void setVmInShutdowns(List<String> vmInShutdowns) {
            this.vmInShutdowns = vmInShutdowns;
        }
    }

    public static class VolumeSyncCmd extends AgentCommand {
        private List<String> storagePaths;

        public void setStoragePaths(List<String> storagePaths) {
            this.storagePaths = storagePaths;
        }

        public List<String> getStoragePaths() {
            return storagePaths;
        }
    }

    public static class VolumeSyncRsp extends AgentResponse {
        private Map<String, List<String>> inactiveVolumePaths;

        public void setInactiveVolumePaths(Map<String, List<String>> inactiveVolumePaths) {
            this.inactiveVolumePaths = inactiveVolumePaths;
        }

        public Map<String, List<String>> getInactiveVolumePaths() {
            return inactiveVolumePaths;
        }
    }

    public static class RefreshAllRulesOnHostCmd extends AgentCommand {
        private List<VmNicSecurityTO> vmNicTOs;
        private Map<String, List<RuleTO>> ruleTOs;
        private Map<String, List<RuleTO>> ip6RuleTOs;

        public RefreshAllRulesOnHostCmd() {
            vmNicTOs = new ArrayList<VmNicSecurityTO>();
            ruleTOs = new HashMap<String, List<RuleTO>>();
            ip6RuleTOs = new HashMap<String, List<RuleTO>>();
        }

        public List<VmNicSecurityTO> getVmNicTOs() {
            return vmNicTOs;
        }

        public void setVmNicTOs(List<VmNicSecurityTO> vmNicTOs) {
            this.vmNicTOs = vmNicTOs;
        }

        public Map<String, List<RuleTO>> getRuleTOs() {
            return ruleTOs;
        }

        public void setRuleTOs(Map<String, List<RuleTO>> ruleTOs) {
            this.ruleTOs = ruleTOs;
        }

        public Map<String, List<RuleTO>> getIp6RuleTOs() {
            return ip6RuleTOs;
        }

        public void setIp6RuleTOs(Map<String, List<RuleTO>> ip6RuleTOs) {
            this.ip6RuleTOs = ip6RuleTOs;
        }
    }

    public static class RefreshAllRulesOnHostResponse extends AgentResponse {
    }

    public static class CheckDefaultSecurityGroupCmd extends AgentCommand {
        Boolean skipIpv6;
    }

    public static class CheckDefaultSecurityGroupResponse extends AgentResponse {

    }

    public static class UpdateGroupMemberCmd extends AgentCommand {
        private List<SecurityGroupMembersTO> updateGroupTOs;

        public void setUpdateGroupTOs(List<SecurityGroupMembersTO> updateGroupTOs) {
            this.updateGroupTOs = updateGroupTOs;
        }

        public List<SecurityGroupMembersTO> getUpdateGroupTOs() {
            return updateGroupTOs;
        }
    }

    public static class UpdateGroupMemberResponse extends AgentResponse {
    }

    public static class CleanupUnusedRulesOnHostCmd extends AgentCommand {
        Boolean skipIpv6;
    }

    public static class CleanupUnusedRulesOnHostResponse extends AgentResponse {
    }


    public static class ApplySecurityGroupRuleCmd extends AgentCommand {
        private List<VmNicSecurityTO> vmNicTOs;
        private Map<String, List<RuleTO>> ruleTOs;
        private Map<String, List<RuleTO>> ip6RuleTOs;

        public ApplySecurityGroupRuleCmd() {
            vmNicTOs = new ArrayList<VmNicSecurityTO>();
            ruleTOs = new HashMap<String, List<RuleTO>>();
            ip6RuleTOs = new HashMap<String, List<RuleTO>>();
        }

        public List<VmNicSecurityTO> getVmNicTOs() {
            return vmNicTOs;
        }

        public void setVmNicTOs(List<VmNicSecurityTO> vmNicTOs) {
            this.vmNicTOs = vmNicTOs;
        }

        public Map<String, List<RuleTO>> getRuleTOs() {
            return ruleTOs;
        }

        public void setRuleTOs(Map<String, List<RuleTO>> ruleTOs) {
            this.ruleTOs = ruleTOs;
        }

        public Map<String, List<RuleTO>> getIp6RuleTOs() {
            return ip6RuleTOs;
        }

        public void setIp6RuleTOs(Map<String, List<RuleTO>> ip6RuleTOs) {
            this.ip6RuleTOs = ip6RuleTOs;
        }
    }

    public static class ApplySecurityGroupRuleResponse extends AgentResponse {
    }

    public static class MigrateVmCmd extends AgentCommand implements HasThreadContext {
        private String vmUuid;
        private String destHostIp;
        private String destHostManagementIp;
        private String storageMigrationPolicy;
        private String srcHostIp;
        private boolean useNuma;
        private boolean migrateFromDestination;
        private boolean autoConverge;
        private Integer downTime;
        private boolean xbzrle;
        private List<String> vdpaPaths;
        private List<NicTO> nics;
        private Map<String, VolumeTO> disks;  // A map from old install path to new volume
        private boolean reload;
        private long bandwidth;

        public Integer getDownTime() {
            return downTime;
        }

        public void setDownTime(Integer downTime) {
            this.downTime = downTime;
        }

        public boolean isReload() {
            return reload;
        }

        public void setReload(boolean reload) {
            this.reload = reload;
        }

        public boolean isUseNuma() {
            return useNuma;
        }

        public void setUseNuma(boolean useNuma) {
            this.useNuma = useNuma;
        }

        public String getSrcHostIp() {
            return srcHostIp;
        }

        public void setSrcHostIp(String srcHostIp) {
            this.srcHostIp = srcHostIp;
        }

        public String getStorageMigrationPolicy() {
            return storageMigrationPolicy;
        }

        public void setStorageMigrationPolicy(String storageMigrationPolicy) {
            this.storageMigrationPolicy = storageMigrationPolicy;
        }

        public String getVmUuid() {
            return vmUuid;
        }

        public void setVmUuid(String vmUuid) {
            this.vmUuid = vmUuid;
        }

        public String getDestHostIp() {
            return destHostIp;
        }

        public void setDestHostIp(String destHostIp) {
            this.destHostIp = destHostIp;
        }

        public boolean isAutoConverge() {
            return autoConverge;
        }

        public void setAutoConverge(boolean autoConverge) {
            this.autoConverge = autoConverge;
        }

        public boolean isXbzrle() {
            return xbzrle;
        }

        public void setXbzrle(boolean xbzrle) {
            this.xbzrle = xbzrle;
        }

        public boolean isMigrateFromDestination() {
            return migrateFromDestination;
        }

        public void setMigrateFromDestination(boolean migrateFromDestination) {
            this.migrateFromDestination = migrateFromDestination;
        }

        public List<String> getVdpaPaths() {
            return vdpaPaths;
        }

        public void setVdpaPaths(List<String> vdpaPaths) {
            this.vdpaPaths = vdpaPaths;
        }

        public Map<String, VolumeTO> getDisks() {
            return disks;
        }

        public void setDisks(Map<String, VolumeTO> disks) {
            this.disks = disks;
        }

        public String getDestHostManagementIp() {
            return destHostManagementIp;
        }

        public void setDestHostManagementIp(String destHostManagementIp) {
            this.destHostManagementIp = destHostManagementIp;
        }

        public long getBandwidth() {
            return bandwidth;
        }

        public void setBandwidth(long bandwidth) {
            this.bandwidth = bandwidth;
        }

        public List<NicTO> getNics() {
            return nics;
        }

        public void setNics(List<NicTO> nics) {
            this.nics = nics;
        }
    }

    public static class MigrateVmResponse extends AgentResponse {
    }

    public static class VmGetCpuXmlCmd extends AgentCommand{
    }

    public static class VmGetCpuXmlResponse extends AgentResponse {
        private String cpuXml;
        private String cpuModelName;

        public String getCpuModelName() {
            return cpuModelName;
        }

        public void setCpuModelName(String cpuModelName) {
            this.cpuModelName = cpuModelName;
        }

        public String getCpuXml() {
            return cpuXml;
        }

        public void setCpuXml(String cpuXml) {
            this.cpuXml = cpuXml;
        }
    }

    public static class VmCompareCpuFunctionCmd extends AgentCommand{
        private String cpuXml;

        public String getCpuXml() {
            return cpuXml;
        }

        public void setCpuXml(String cpuXml) {
            this.cpuXml = cpuXml;
        }
    }

    public static class VmCompareCpuFunctionResponse extends AgentResponse {
    }

    public static class MergeSnapshotRsp extends AgentResponse {
        public long size;
    }

    public static class MergeSnapshotCmd extends AgentCommand implements HasThreadContext {
        private String vmUuid;
        private VolumeTO volume;
        private String srcPath;
        private String destPath;
        private boolean fullRebase;

        public boolean isFullRebase() {
            return fullRebase;
        }

        public void setFullRebase(boolean fullRebase) {
            this.fullRebase = fullRebase;
        }

        public String getVmUuid() {
            return vmUuid;
        }

        public void setVmUuid(String vmUuid) {
            this.vmUuid = vmUuid;
        }

        public String getSrcPath() {
            return srcPath;
        }

        public void setSrcPath(String srcPath) {
            this.srcPath = srcPath;
        }

        public String getDestPath() {
            return destPath;
        }

        public void setDestPath(String destPath) {
            this.destPath = destPath;
        }

        public VolumeTO getVolume() {
            return volume;
        }

        public void setVolume(VolumeTO volume) {
            this.volume = volume;
        }
    }

    public static class CheckSnapshotCmd extends AgentCommand {
        private String vmUuid;
        private String volumeUuid;
        private String currentInstallPath;
        private List<String> excludeInstallPaths;
        private Map<String, Integer> volumeChainToCheck;

        public String getVmUuid() {
            return vmUuid;
        }

        public void setVmUuid(String vmUuid) {
            this.vmUuid = vmUuid;
        }

        public String getVolumeUuid() {
            return volumeUuid;
        }

        public void setVolumeUuid(String volumeUuid) {
            this.volumeUuid = volumeUuid;
        }

        public Map<String, Integer> getVolumeChainToCheck() {
            return volumeChainToCheck;
        }

        public void setVolumeChainToCheck(Map<String, Integer> volumeChainToCheck) {
            this.volumeChainToCheck = volumeChainToCheck;
        }

        public String getCurrentInstallPath() {
            return currentInstallPath;
        }

        public void setCurrentInstallPath(String currentInstallPath) {
            this.currentInstallPath = currentInstallPath;
        }

        public List<String> getExcludeInstallPaths() {
            return excludeInstallPaths;
        }

        public void setExcludeInstallPaths(List<String> excludeInstallPaths) {
            this.excludeInstallPaths = excludeInstallPaths;
        }
    }

    public static class BlockCommitCmd extends AgentCommand implements HasThreadContext {
        private String vmUuid;
        private VolumeTO volume;
        private String top;
        private String base;
        private List<String> topChildrenInstallPathInDb = new ArrayList<>();

        public String getVmUuid() {
            return vmUuid;
        }

        public void setVmUuid(String vmUuid) {
            this.vmUuid = vmUuid;
        }

        public VolumeTO getVolume() {
            return volume;
        }

        public void setVolume(VolumeTO volume) {
            this.volume = volume;
        }

        public String getTop() {
            return top;
        }

        public void setTop(String top) {
            this.top = top;
        }

        public String getBase() {
            return base;
        }

        public void setBase(String base) {
            this.base = base;
        }

        public List<String> getTopChildrenInstallPathInDb() {
            return topChildrenInstallPathInDb;
        }

        public void setTopChildrenInstallPathInDb(List<String> topChildrenInstallPathInDb) {
            this.topChildrenInstallPathInDb = topChildrenInstallPathInDb;
        }
    }

    public static class BlockCommitResponse extends AgentResponse {
        @Validation(notZero = true)
        private long size;

        public long getSize() {
            return size;
        }

        public void setSize(long size) {
            this.size = size;
        }
    }

    public static class BlockPullCmd extends AgentCommand implements HasThreadContext {
        private String vmUuid;
        private VolumeTO volume;
        private String base;

        public String getVmUuid() {
            return vmUuid;
        }

        public void setVmUuid(String vmUuid) {
            this.vmUuid = vmUuid;
        }

        public VolumeTO getVolume() {
            return volume;
        }

        public void setVolume(VolumeTO volume) {
            this.volume = volume;
        }

        public String getBase() {
            return base;
        }

        public void setBase(String base) {
            this.base = base;
        }
    }

    public static class BlockPullResponse extends AgentResponse {
        private long size;

        public long getSize() {
            return size;
        }

        public void setSize(long size) {
            this.size = size;
        }
    }

    public static class TakeSnapshotCmd extends AgentCommand implements HasThreadContext {
        private String vmUuid;
        private String volumeUuid;
        private VolumeTO volume;
        private String installPath;
        private boolean fullSnapshot;
        private String volumeInstallPath;
        private String newVolumeUuid;
        private String newVolumeInstallPath;
        private boolean online;
        private long timeout;

        // for baremetal2 instance
        private boolean isBaremetal2InstanceOnlineSnapshot;

        public boolean isOnline() {
            return online;
        }

        public void setOnline(boolean online) {
            this.online = online;
        }

        public String getVolumeUuid() {
            return volumeUuid;
        }

        public void setVolumeUuid(String volumeUuid) {
            this.volumeUuid = volumeUuid;
        }

        public String getVolumeInstallPath() {
            return volumeInstallPath;
        }

        public void setVolumeInstallPath(String volumeInstallPath) {
            this.volumeInstallPath = volumeInstallPath;
        }

        public boolean isFullSnapshot() {
            return fullSnapshot;
        }

        public void setFullSnapshot(boolean fullSnapshot) {
            this.fullSnapshot = fullSnapshot;
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

        public String getNewVolumeInstallPath() {
            return newVolumeInstallPath;
        }

        public void setNewVolumeInstallPath(String newVolumeInstallPath) {
            this.newVolumeInstallPath = newVolumeInstallPath;
        }

        public String getNewVolumeUuid() {
            return newVolumeUuid;
        }

        public void setNewVolumeUuid(String newVolumeUuid) {
            this.newVolumeUuid = newVolumeUuid;
        }

        public VolumeTO getVolume() {
            return volume;
        }

        public void setVolume(VolumeTO volume) {
            this.volume = volume;
        }

        public boolean isBaremetal2InstanceOnlineSnapshot() {
            return isBaremetal2InstanceOnlineSnapshot;
        }

        public void setBaremetal2InstanceOnlineSnapshot(boolean baremetal2InstanceOnlineSnapshot) {
            isBaremetal2InstanceOnlineSnapshot = baremetal2InstanceOnlineSnapshot;
        }

        public long getTimeout() {
            return timeout;
        }

        public void setTimeout(long timeout) {
            this.timeout = timeout;
        }
    }

    public static class CheckSnapshotResponse extends AgentResponse {

    }

    public static class TakeSnapshotResponse extends AgentResponse {
        @Validation
        private String newVolumeInstallPath;
        @Validation
        private String snapshotInstallPath;
        @Validation(notZero = true)
        private long size;

        public long getSize() {
            return size;
        }

        public void setSize(long size) {
            this.size = size;
        }

        public String getSnapshotInstallPath() {
            return snapshotInstallPath;
        }

        public void setSnapshotInstallPath(String snapshotInstallPath) {
            this.snapshotInstallPath = snapshotInstallPath;
        }

        public String getNewVolumeInstallPath() {
            return newVolumeInstallPath;
        }

        public void setNewVolumeInstallPath(String newVolumeInstallPath) {
            this.newVolumeInstallPath = newVolumeInstallPath;
        }
    }

    public static class LogoutIscsiTargetCmd extends AgentCommand {
        private String hostname;
        private int port;
        private String target;

        public String getHostname() {
            return hostname;
        }

        public void setHostname(String hostname) {
            this.hostname = hostname;
        }

        public int getPort() {
            return port;
        }

        public void setPort(int port) {
            this.port = port;
        }

        public String getTarget() {
            return target;
        }

        public void setTarget(String target) {
            this.target = target;
        }
    }

    public static class LogoutIscsiTargetRsp extends AgentResponse {
    }

    public static class LoginIscsiTargetCmd extends AgentCommand implements Serializable {
        private String url;
        private String hostname;
        private int port;
        private String target;
        private String chapUsername;
        @NoLogging
        private String chapPassword;

        public String getChapUsername() {
            return chapUsername;
        }

        public void setChapUsername(String chapUsername) {
            this.chapUsername = chapUsername;
        }

        public String getChapPassword() {
            return chapPassword;
        }

        public void setChapPassword(String chapPassword) {
            this.chapPassword = chapPassword;
        }

        public String getHostname() {
            return hostname;
        }

        public void setHostname(String hostname) {
            this.hostname = hostname;
        }

        public int getPort() {
            return port;
        }

        public void setPort(int port) {
            this.port = port;
        }

        public String getTarget() {
            return target;
        }

        public void setTarget(String target) {
            this.target = target;
        }

        public String getUrl() {
            return url;
        }

        public void setUrl(String url) {
            this.url = url;
        }
    }

    public static class LoginIscsiTargetRsp extends AgentResponse {
    }

    public static class AttachIsoCmd extends AgentCommand {
        public IsoTO iso;
        public String vmUuid;
    }

    public static class AttachIsoRsp extends AgentResponse {
    }

    public static class DetachIsoCmd extends AgentCommand {
        public String vmUuid;
        public String isoUuid;
        public int deviceId;
    }

    public static class DetachIsoRsp extends AgentResponse {

    }

    public static class UpdateHostOSCmd extends AgentCommand {
        public String hostUuid;
        public String excludePackages;
        public String updatePackages;
        public String releaseVersion;
        public boolean enableExpRepo;
    }

    public static class UpdateHostOSRsp extends AgentResponse {
        public String libvirtVersion;

        public String getLibvirtVersion() {
            return libvirtVersion;
        }

        public void setLibvirtVersion(String libvirtVersion) {
            this.libvirtVersion = libvirtVersion;
        }
    }

    public static class UpdateDependencyCmd extends AgentCommand {
        public String hostUuid;
        public boolean enableExpRepo;
        public String excludePackages;
        public String updatePackages;
        public String zstackRepo;
    }

    public static class UpdateDependencyRsp extends AgentResponse {
    }

    public static class ReportVmStateCmd {
        public String hostUuid;
        public String vmUuid;
        public String vmState;
    }

    public static class ReconnectMeCmd {
        public String hostUuid;
        public String reason;
    }

    public static class ReportPsStatusCmd {
        public String hostUuid;
        public List<String> psUuids;
        public String psStatus;
        public String reason;
    }

    public static class ReportSelfFencerCmd {
        public String hostUuid;
        public List<String> psUuids;
        public String reason;
        public String vmUuidsString;
        public boolean fencerFailure = true;
    }

    public static class ReportHostMaintainCmd {
        public String hostUuid;
    }

    public static class ReportFailoverCmd {
        public String vmInstanceUuid;
        public String hostUuid;
        public String reason;
        public boolean primaryVmFailure;
    }

    public static class ReportHostDeviceEventCmd {
        public String hostUuid;
    }

    public static class ReportVmShutdownEventCmd {
        public String vmUuid;
        public String detail;
        public String opaque;
    }

    public static class ReportVmRebootEventCmd {
        public String vmUuid;
    }

    public static class ReportVmStartEventCmd {
        public String vmUuid;
    }

    public static class ReportVmCrashEventCmd {
        public String vmUuid;
    }

    public static class ReportHostStopEventCmd {
        public String hostIp;
    }

    public static class ReportVmHostFileChangedCmd {
        public String hostUuid;
        public String vmUuid;
        public List<String> types;
    }

    public static class ShutdownHostCmd extends AgentCommand {
    }

    public static class RebootHostCmd extends AgentCommand {
    }

    public static class ShutdownHostResponse extends AgentResponse {
    }

    public static class RebootHostResponse extends AgentResponse {
    }

    public static class UpdateSpiceChannelConfigCmd extends AgentCommand {
    }

    public static class UpdateSpiceChannelConfigResponse extends AgentResponse {
        public boolean restartLibvirt = false;
    }

    public static class PrimaryStorageCommand extends AgentCommand {
        public String primaryStorageUuid;
    }

    public static class CancelCmd extends AgentCommand implements CancelCommand {
        private String cancellationApiId;
        private boolean allowTaskNotFound;
        private Integer times;
        private Integer interval;

        public Integer getTimes() {
            return times;
        }

        public void setTimes(Integer times) {
            this.times = times;
        }

        public Integer getInterval() {
            return interval;
        }

        public void setInterval(Integer interval) {
            this.interval = interval;
        }

        @Override
        public void setCancellationApiId(String cancellationApiId) {
            this.cancellationApiId = cancellationApiId;
        }

        public boolean isAllowTaskNotFound() {
            return allowTaskNotFound;
        }

        public void setAllowTaskNotFound(boolean allowTaskNotFound) {
            this.allowTaskNotFound = allowTaskNotFound;
        }
    }

    public static class CancelRsp extends AgentResponse {
        private CancelTaskResult cancelResult;

        public CancelTaskResult getCancelResult() {
            return cancelResult == null ? CancelTaskResult.CANCEL_SIGNALLED : cancelResult;
        }

        public void setCancelResult(CancelTaskResult cancelResult) {
            this.cancelResult = cancelResult;
        }
    }

    public static class TransmitVmOperationToMnCmd {
        public String uuid;
        public String operation;
    }

    public static class GetDevCapacityCmd extends AgentCommand {
        public String dirPath;
    }

    public static class GetDevCapacityResponse extends AgentResponse {
        public long totalSize;
        public long availableSize;
        public long dirSize;
    }

    public static class CheckFileOnHostCmd extends AgentCommand {
        public Set<String> paths;
        public boolean md5Return;
    }

    public static class CheckFileOnHostResponse extends AgentResponse {
        public Map<String, String> existPaths;
    }

    public static class GetHostNUMATopologyCmd extends AgentCommand {
        public String HostUuid;

        public void setHostUuid(String hostUuid) {
            HostUuid = hostUuid;
        }

        public String getHostUuid() {
            return HostUuid;
        }
    }

    public static class GetHostNUMATopologyResponse extends AgentResponse {
        public Map<String, HostNUMANode> topology;


        public void setTopology(Map<String, HostNUMANode> topology) {
            this.topology = topology;
        }

        public Map<String, HostNUMANode> getTopology() {
            return topology;
        }
    }

    public static class PhysicalNicAlarmEventCmd {
        public String host;
        public String nic;
        public String ip;
        public String bond;
        public String status;

        public String getHost() {
            return host;
        }

        public void setHost(String host) {
            this.host = host;
        }

        public String getNic() {
            return nic;
        }

        public void setNic(String nic) {
            this.nic = nic;
        }

        public String getIp() {
            return ip;
        }

        public void setIp(String ip) {
            this.ip = ip;
        }

        public String getBond() {
            return bond;
        }

        public void setBond(String bond) {
            this.bond = bond;
        }

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }
    }

    public static class AttachVolumeCmd extends AgentCommand {
        public String volumePrimaryStorageUuid;
        public String volumeInstallPath;
        public String mountPath;
        public String device;
    }

    public static class AttachVolumeRsp extends AgentResponse {
        public String device;
    }

    public static class DetachVolumeCmd extends AgentCommand {
        public String volumeInstallPath;
        public String mountPath;
        public String device;
    }

    public static class DetachVolumeRsp extends AgentResponse {
    }

    public static class VmFstrimCmd extends AgentCommand {
        public String vmUuid;
    }

    public static class VmFstrimRsp extends AgentResponse {
    }

    public static class GetBlockDevicesCmd extends AgentCommand {
    }

    public static class GetBlockDevicesResponse extends AgentResponse {
        private List<BlockDevices.BlockDevice> blockDevices;

        public List<BlockDevices.BlockDevice> getBlockDevices() {
            return blockDevices;
        }

        public void setBlockDevices(List<BlockDevices.BlockDevice> blockDevices) {
            this.blockDevices = blockDevices;
        }
    }

    public static class GetSensorsCmd extends AgentCommand implements HasThreadContext {
    }

    public static class GetSensorsResponse extends AgentResponse {
        private List<Sensor> sensors;

        public List<Sensor> getSensors() {
            return sensors;
        }

        public void setSensors(List<Sensor> sensors) {
            this.sensors = sensors;
        }
    }

    public static class DownloadFileCmd extends AgentCommand implements HasThreadContext, Serializable {
        public String taskUuid;
        public String installPath;
        @NoLogging(type = NoLogging.Type.Uri)
        public String url;
        @NoLogging(type = NoLogging.Type.Uri)
        public String urlScheme;
        public long timeout;
        @NoLogging(type = NoLogging.Type.Uri)
        public String sendCommandUrl;
    }

    public static class DownloadFileResponse extends AgentResponse {
        public String md5sum;
        public long size;
    }

    public static class UploadFileCmd extends AgentCommand implements HasThreadContext, Serializable {
        public String taskUuid;
        public String installPath;
        @NoLogging(type = NoLogging.Type.Uri)
        public String url;
        public long timeout;
    }

    public static class UploadFileResponse extends AgentResponse {
        public String directUploadUrl;
    }

    public static class GetDownloadFileProgressCmd extends AgentCommand {
        public String taskUuid;
    }

    public static class GetDownloadFileProgressResponse extends AgentResponse {
        public boolean completed;
        public int progress;
        public long size;
        public long actualSize;
        public String installPath;
        public String format;
        public long lastOpTime;
        public long downloadSize;
        public String md5sum;
        public boolean supportSuspend;
    }

    public static class UploadFileToVmCmd extends AgentCommand implements HasThreadContext, Serializable {
        public String taskUuid;
        public String sourcePath;
        public String targetIp;
        public String targetPath;
        public String username;
        public int sshPort;
        public long timeout;
        @NoLogging
        public String password;
    }

    public static class UploadFileToVmResponse extends AgentResponse {
    }

    public static class CleanupUploadFileToVmCmd extends AgentCommand {
        public String taskUuid;
    }

    public static class CleanupUploadFileToVmResponse extends AgentResponse {
    }

    public static class TakeVmConsoleScreenshotCmd extends AgentCommand {
        private String vmUuid;

        public String getVmUuid() {
            return vmUuid;
        }

        public void setVmUuid(String vmUuid) {
            this.vmUuid = vmUuid;
        }
    }

    public static class TakeVmConsoleScreenshotRsp extends AgentResponse {
        @NoLogging(type = NoLogging.Type.LongText)
        private String imageData;

        public String getImageData() {
            return imageData;
        }

        public void setImageData(String imageData) {
            this.imageData = imageData;
        }
    }

    public static class GetVmUptimeCmd extends AgentCommand {
        private String vmUuid;

        public String getVmUuid() {
            return vmUuid;
        }

        public void setVmUuid(String vmUuid) {
            this.vmUuid = vmUuid;
        }
    }

    public static class GetVmUptimeRsp extends AgentResponse {
        private String createTime;

        public String getCreateTime() {
            return createTime;
        }

        public void setCreateTime(String createTime) {
            this.createTime = createTime;
        }
    }

    public static class HardwareMonitorCmd extends KVMAgentCommands.AgentCommand {
    }

    public static class HostPhysicalCpuStatusAlarmEventCmd {
        public String host;
        public String cpuName;
        public String status;
    }

    public static class HostPhysicalMemoryStatusAlarmEventCmd {
        public String host;
        public String locator;
        public String status;
    }

    public static class HostPhysicalFanStatusAlarmEventCmd {
        public String host;
        public String fan_name;
        public String status;
    }

    public static class HostPhysicalPowerSupplyStatusAlarmEventCmd {
        public String host;
        public String name;
        public String status;
    }

    public static class HostPhysicalDiskStatusAlarmEventCmd {
        public String host;
        public String slot_number;
        public String enclosure_device_id;
        public String drive_state;
        public String serial_number;
    }

    public static class HostPhysicalDiskInsertAlarmEventCmd {
        public String host;
        public Map<String, Object> additionalProperties = new HashMap<>();
    }

    public static class HostPhysicalDiskRemoveAlarmEventCmd {
        public String host;
        public Map<String, Object> additionalProperties = new HashMap<>();
    }

    public static class PhysicalMemoryEccErrorAlarmEventCmd {
        public String host;
        public String detail;
    }

    public static class PhysicalGpuStatusAlarmEventCmd {
        public String host;
        public String pcideviceAddress;
        public String status;
    }

    public static class PhysicalGpuRemoveAlarmEventCmd {
        public String host;
        public String pcideviceAddress;
    }

    public static class HostStorageDeviceHbaStateEventCmd {
        public String host;
        public String portName;
        public String portState;
        public String name;
    }

    public static class HostPhysicalDeviceStatusAlarmEventCmd {
        private String host;
        private String type;
        private Map<String, Object> additionalProperties = new HashMap<>();

        public String getHost() {
            return host;
        }

        public void setHost(String host) {
            this.host = host;
        }

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public Map<String, Object> getAdditionalProperties() {
            return additionalProperties;
        }

        @JsonAnySetter
        public void setAdditionalProperties(Map<String, Object> additionalProperties) {
            this.additionalProperties = additionalProperties;
        }
    }

    public static class UpdateHostNqnCmd extends AgentCommand {
        public String nqn;
    }

    public static class UpdateHostNqnRsp extends AgentResponse {
    }

    public static class UpdateHostnameCmd extends AgentCommand {
        public String hostname;
    }

    public static class UpdateHostnameRsp extends AgentResponse {
    }

    public static class HostProcessPhysicalMemoryUsageAlarmCmd {
        private String hostUuid;
        private String pid;
        private String processName;
        private long memoryUsage;
        private Map<String, Object> additionalProperties = new HashMap<>();

        public String getHostUuid() {
            return hostUuid;
        }

        public void setHostUuid(String hostUuid) {
            this.hostUuid = hostUuid;
        }

        public String getPid() {
            return pid;
        }

        public void setPid(String pid) {
            this.pid = pid;
        }

        public String getProcessName() {
            return processName;
        }

        public void setProcessName(String processName) {
            this.processName = processName;
        }

        public long getMemoryUsage() {
            return memoryUsage;
        }

        public void setMemoryUsage(long memoryUsage) {
            this.memoryUsage = memoryUsage;
        }

        public Map<String, Object> getAdditionalProperties() {
            return additionalProperties;
        }

        public void setAdditionalProperties(Map<String, Object> additionalProperties) {
            this.additionalProperties = additionalProperties;
        }
    }

    public static class HostKvmAgentStatusCmd {
        private String status;
        private String hostUuid;
        private long memoryUsage;

        public String getHostUuid() {
            return hostUuid;
        }

        public void setHostUuid(String hostUuid) {
            this.hostUuid = hostUuid;
        }

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }

        public long getMemoryUsage() {
            return memoryUsage;
        }

        public void setMemoryUsage(long memoryUsage) {
            this.memoryUsage = memoryUsage;
        }
    }
}
