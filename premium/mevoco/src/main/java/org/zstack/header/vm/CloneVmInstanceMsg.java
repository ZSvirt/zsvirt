package org.zstack.header.vm;

import org.zstack.header.configuration.VmCustomSpecificationStruct;
import org.zstack.header.identity.SessionInventory;
import org.zstack.header.message.NeedReplyMessage;
import org.zstack.header.storage.snapshot.group.VolumeSnapshotGroupInventory;
import org.zstack.utils.CollectionUtils;

import java.util.*;

public class CloneVmInstanceMsg extends NeedReplyMessage implements VmInstanceMessage {
    private String vmInstanceUuid;
    private List<String> names;
    private String strategy;
    private boolean full = false;
    private SessionInventory session;
    private String clusterUuid;
    private String hostUuid;
    private String description;

    private Map<String, String> resourceUuidByName = new HashMap<>();
    private Integer cpuNum;
    private Long memorySize;
    private Long reservedMemorySize;
    private String platform;
    private String guestOsType;
    private String architecture;
    private List<String> l3NetworkUuids;
    private String defaultL3NetworkUuid;
    private List<VmNicParam> vmNicParams;
    private List<DiskAO> diskAOs;
    private VolumeSnapshotGroupInventory volumeSnapshotGroup;
    private VmCustomSpecificationStruct vmCustomSpecification;
    private Boolean resetTpm;

    @Override
    public String getVmInstanceUuid() {
        return vmInstanceUuid;
    }

    public void setVmInstanceUuid(String vmInstanceUuid){
        this.vmInstanceUuid = vmInstanceUuid;
    }

    public void setNames(List<String> names){
        this.names = names;
    }

    public List<String> getNames(){
        return names;
    }

    public void setStrategy(String strategy){
        this.strategy = strategy;
    }

    public String getStrategy(){
        return strategy;
    }

    public boolean isFull() {
        return full;
    }

    public void setFull(boolean full) {
        this.full = full;
    }

    public SessionInventory getSession() {
        return session;
    }

    public void setSession(SessionInventory session) {
        this.session = session;
    }

    public String getClusterUuid() {
        return clusterUuid;
    }

    public void setClusterUuid(String clusterUuid) {
        this.clusterUuid = clusterUuid;
    }

    public String getHostUuid() {
        return hostUuid;
    }

    public void setHostUuid(String hostUuid) {
        this.hostUuid = hostUuid;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Map<String, String> getResourceUuidByName() {
        return resourceUuidByName;
    }

    public void setResourceUuidByName(Map<String, String> resourceUuidByName) {
        this.resourceUuidByName = resourceUuidByName;
    }

    public Integer getCpuNum() {
        return cpuNum;
    }

    public void setCpuNum(Integer cpuNum) {
        this.cpuNum = cpuNum;
    }

    public Long getMemorySize() {
        return memorySize;
    }

    public void setMemorySize(Long memorySize) {
        this.memorySize = memorySize;
    }

    public String getPlatform() {
        return platform;
    }

    public void setPlatform(String platform) {
        this.platform = platform;
    }

    public String getGuestOsType() {
        return guestOsType;
    }

    public void setGuestOsType(String guestOsType) {
        this.guestOsType = guestOsType;
    }

    public String getArchitecture() {
        return architecture;
    }

    public void setArchitecture(String architecture) {
        this.architecture = architecture;
    }

    public Long getReservedMemorySize() {
        return reservedMemorySize;
    }

    public void setReservedMemorySize(Long reservedMemorySize) {
        this.reservedMemorySize = reservedMemorySize;
    }

    public List<String> getL3NetworkUuids() {
        return l3NetworkUuids;
    }

    public void setL3NetworkUuids(List<String> l3NetworkUuids) {
        this.l3NetworkUuids = l3NetworkUuids;
    }

    public String getDefaultL3NetworkUuid() {
        return defaultL3NetworkUuid;
    }

    public void setDefaultL3NetworkUuid(String defaultL3NetworkUuid) {
        this.defaultL3NetworkUuid = defaultL3NetworkUuid;
    }

    public List<VmNicParam> getVmNicParams() {
        return vmNicParams;
    }

    public void setVmNicParams(List<VmNicParam> vmNicParams) {
        this.vmNicParams = vmNicParams;
    }

    public List<DiskAO> getDiskAOs() {
        return diskAOs;
    }

    public void setDiskAOs(List<DiskAO> diskAOs) {
        this.diskAOs = diskAOs;
    }

    public VolumeSnapshotGroupInventory getVolumeSnapshotGroup() {
        return volumeSnapshotGroup;
    }

    public void setVolumeSnapshotGroup(VolumeSnapshotGroupInventory volumeSnapshotGroup) {
        this.volumeSnapshotGroup = volumeSnapshotGroup;
    }

    public VmCustomSpecificationStruct getVmCustomSpecification() {
        return vmCustomSpecification;
    }

    public void setVmCustomSpecification(VmCustomSpecificationStruct vmCustomSpecification) {
        this.vmCustomSpecification = vmCustomSpecification;
    }

    public Boolean getResetTpm() {
        return resetTpm;
    }

    public void setResetTpm(Boolean resetTpm) {
        this.resetTpm = resetTpm;
    }

    public Map<String, DiskAO> getDiskAOsByVolumeUuid() {
        if (CollectionUtils.isEmpty(diskAOs)) {
            return Collections.emptyMap();
        }

        Map<String, DiskAO> diskAOsByVolumeUuid = new HashMap<>();
        diskAOs.forEach(diskAO -> diskAOsByVolumeUuid.put(diskAO.getSourceUuid(), diskAO));
        return diskAOsByVolumeUuid;
    }
}
