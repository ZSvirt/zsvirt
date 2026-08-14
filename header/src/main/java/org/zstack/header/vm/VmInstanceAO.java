package org.zstack.header.vm;

import org.zstack.header.cluster.ClusterEO;
import org.zstack.header.configuration.InstanceOfferingEO;
import org.zstack.header.host.HostEO;
import org.zstack.header.image.ImageEO;
import org.zstack.header.vo.ForeignKey;
import org.zstack.header.vo.ForeignKey.ReferenceOption;
import org.zstack.header.vo.Index;
import org.zstack.header.vo.ResourceVO;
import org.zstack.header.volume.Volume;
import org.zstack.header.volume.VolumeEO;
import org.zstack.header.volume.VolumeVO;
import org.zstack.header.zone.ZoneEO;

import javax.persistence.*;
import java.sql.Timestamp;

@MappedSuperclass
public class VmInstanceAO extends ResourceVO {
    @Column
    @Index(length = 128)
    private String name;

    @Column
    private String description;

    @Column
    @ForeignKey(parentEntityClass = ZoneEO.class, onDeleteAction = ReferenceOption.SET_NULL)
    private String zoneUuid;

    @Column
    @ForeignKey(parentEntityClass = ClusterEO.class, onDeleteAction = ReferenceOption.SET_NULL)
    private String clusterUuid;

    @Column
    @ForeignKey(parentEntityClass = ImageEO.class, onDeleteAction = ReferenceOption.RESTRICT)
    private String imageUuid;

    @Column
    @ForeignKey(parentEntityClass = HostEO.class, onDeleteAction = ReferenceOption.SET_NULL)
    private String hostUuid;

    @Column
    private Long internalId;

    @Column
    @ForeignKey(parentEntityClass = HostEO.class, onDeleteAction = ReferenceOption.SET_NULL)
    private String lastHostUuid;

    @Column
    @ForeignKey(parentEntityClass = InstanceOfferingEO.class, onDeleteAction = ReferenceOption.RESTRICT)
    private String instanceOfferingUuid;

    @Column
    @ForeignKey(parentEntityClass = VolumeEO.class, onDeleteAction = ReferenceOption.SET_NULL)
    private String rootVolumeUuid;

    @Column
    private String defaultL3NetworkUuid;

    @Column
    private String type;

    @Column
    private String hypervisorType;

    @Column
    private int cpuNum;

    @Column
    private long cpuSpeed;

    @Column
    private long memorySize;

    @Column
    private long reservedMemorySize;

    @Column
    private String platform;

    @Column
    private String architecture;

    @Column
    private String allocatorStrategy;

    @Column
    private String guestOsType;

    @Column
    private Timestamp createDate;

    @Column
    private Timestamp lastOpDate;

    @Column
    @Enumerated(EnumType.STRING)
    private VmInstanceState state;

    public VmInstanceAO() {
    }

    public VmInstanceAO(VmInstanceAO other) {
        this.uuid = other.uuid;
        this.name = other.name;
        this.description = other.description;
        this.zoneUuid = other.zoneUuid;
        this.clusterUuid = other.clusterUuid;
        this.imageUuid = other.imageUuid;
        this.hostUuid = other.hostUuid;
        this.internalId = other.internalId;
        this.lastHostUuid = other.lastHostUuid;
        this.instanceOfferingUuid = other.instanceOfferingUuid;
        this.rootVolumeUuid = other.rootVolumeUuid;
        this.defaultL3NetworkUuid = other.defaultL3NetworkUuid;
        this.type = other.type;
        this.hypervisorType = other.hypervisorType;
        this.cpuNum = other.cpuNum;
        this.cpuSpeed = other.cpuSpeed;
        this.memorySize = other.memorySize;
        this.reservedMemorySize = other.getReservedMemorySize();
        this.allocatorStrategy = other.allocatorStrategy;
        this.createDate = other.createDate;
        this.lastOpDate = other.lastOpDate;
        this.state = other.state;
        this.platform = other.platform;
        this.guestOsType = other.guestOsType;
        this.architecture = other.architecture;
    }

    @PreUpdate
    private void preUpdate() {
        lastOpDate = null;
    }

    public String getPlatform() {
        return platform;
    }

    public void setPlatform(String platform) {
        this.platform = platform;
    }

    public String getAllocatorStrategy() {
        return allocatorStrategy;
    }

    public void setAllocatorStrategy(String allocatorStrategy) {
        this.allocatorStrategy = allocatorStrategy;
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

    public long getMemorySize() {
        return memorySize;
    }

    public void setMemorySize(long memorySize) {
        this.memorySize = memorySize;
    }

    public long getReservedMemorySize() {
        return reservedMemorySize;
    }

    public void setReservedMemorySize(long reservedMemorySize) {
        this.reservedMemorySize = reservedMemorySize;
    }

    public String getDefaultL3NetworkUuid() {
        return defaultL3NetworkUuid;
    }

    public void setDefaultL3NetworkUuid(String defaultL3NetworkUuid) {
        this.defaultL3NetworkUuid = defaultL3NetworkUuid;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getZoneUuid() {
        return zoneUuid;
    }

    public void setZoneUuid(String zoneUuid) {
        this.zoneUuid = zoneUuid;
    }

    public String getClusterUuid() {
        return clusterUuid;
    }

    public void setClusterUuid(String clusterUuid) {
        this.clusterUuid = clusterUuid;
    }

    public String getImageUuid() {
        return imageUuid;
    }

    public void setImageUuid(String imageUuid) {
        this.imageUuid = imageUuid;
    }

    public String getHostUuid() {
        return hostUuid;
    }

    public void setHostUuid(String hostUuid) {
        this.hostUuid = hostUuid;
    }

    public String getLastHostUuid() {
        return lastHostUuid;
    }

    public void setLastHostUuid(String lastHostUuid) {
        this.lastHostUuid = lastHostUuid;
    }

    public String getInstanceOfferingUuid() {
        return instanceOfferingUuid;
    }

    public void setInstanceOfferingUuid(String instanceOfferingUuid) {
        this.instanceOfferingUuid = instanceOfferingUuid;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getHypervisorType() {
        return hypervisorType;
    }

    public void setHypervisorType(String hypervisorType) {
        this.hypervisorType = hypervisorType;
    }

    public VmInstanceState getState() {
        return state;
    }

    public void setState(VmInstanceState state) {
        this.state = state;
    }

    public String getRootVolumeUuid() {
        return rootVolumeUuid;
    }

    public void setRootVolumeUuid(String rootVolumeUuid) {
        this.rootVolumeUuid = rootVolumeUuid;
    }

    public long getInternalId() {
        return internalId;
    }

    public void setInternalId(long internalId) {
        this.internalId = internalId;
    }

    public Timestamp getCreateDate() {
        return createDate;
    }

    public void setCreateDate(Timestamp createDate) {
        this.createDate = createDate;
    }

    public Timestamp getLastOpDate() {
        return lastOpDate;
    }

    public void setLastOpDate(Timestamp lastOpDate) {
        this.lastOpDate = lastOpDate;
    }

    public void setInternalId(Long internalId) {
        this.internalId = internalId;
    }

    public String getArchitecture() {
        return architecture;
    }

    public void setArchitecture(String architecture) {
        this.architecture = architecture;
    }

    public String getGuestOsType() {
        return guestOsType;
    }

    public void setGuestOsType(String guestOsType) {
        this.guestOsType = guestOsType;
    }
}
