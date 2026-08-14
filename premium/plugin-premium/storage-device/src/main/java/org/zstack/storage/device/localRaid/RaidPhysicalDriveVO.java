package org.zstack.storage.device.localRaid;

import org.zstack.header.storageDevice.HasSmartInfo;
import org.zstack.header.tag.AutoDeleteTag;
import org.zstack.header.vo.EntityGraph;
import org.zstack.header.vo.ResourceVO;
import org.zstack.header.vo.ToInventory;

import javax.persistence.*;
import java.sql.Timestamp;

@Entity
@Table
@AutoDeleteTag
@EntityGraph(
        parents = {
                @EntityGraph.Neighbour(type = RaidControllerVO.class, myField = "raidControllerUuid", targetField = "uuid"),
        }
)
public class RaidPhysicalDriveVO extends ResourceVO implements ToInventory, HasSmartInfo {
    @Column
    private String name;

    @Column
    private String raidControllerUuid;

    @Column
    private String description;

    @Column
    private String raidLevel;

    @Column
    private Integer enclosureDeviceId;

    @Column
    private Integer slotNumber;

    @Column
    private Integer deviceId;

    @Column
    private Integer diskGroup;

    @Column
    private String wwn;

    @Column
    private String serialNumber;

    @Column
    private String deviceModel;

    @Column
    private Long size;

    @Column
    private String driveState;

    @Column
    @Enumerated(EnumType.STRING)
    private LocateStatus locateStatus;

    @Column
    private String driveType;

    @Column
    private String mediaType;

    @Column
    private Integer rotationRate;

    @Column
    private Timestamp createDate;

    @Column
    private Timestamp lastOpDate;

    public void updateFromStruct(RaidPhysicalDriveStruct s) {
        this.setRaidLevel(s.getRaidLevel());
        this.setEnclosureDeviceId(s.getEnclosureDeviceId());
        this.setSlotNumber(s.getSlotNumber());
        this.setDiskGroup(s.getDiskGroup());
        this.setDriveState(s.getDriveState());
    }

    @PreUpdate
    private void preUpdate() {
        lastOpDate = null;
    }

    public String getRaidLevel() {
        return raidLevel;
    }

    public void setRaidLevel(String raidLevel) {
        this.raidLevel = raidLevel;
    }

    public String getRaidControllerUuid() {
        return raidControllerUuid;
    }

    public void setRaidControllerUuid(String raidControllerUuid) {
        this.raidControllerUuid = raidControllerUuid;
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

    public Integer getEnclosureDeviceId() {
        return enclosureDeviceId;
    }

    public void setEnclosureDeviceId(Integer enclosureDeviceId) {
        this.enclosureDeviceId = enclosureDeviceId;
    }

    public Integer getSlotNumber() {
        return slotNumber;
    }

    public void setSlotNumber(Integer slotNumber) {
        this.slotNumber = slotNumber;
    }

    public Integer getDiskGroup() {
        return diskGroup;
    }

    public void setDiskGroup(Integer diskGroup) {
        this.diskGroup = diskGroup;
    }

    public String getWwn() {
        return wwn;
    }

    public void setWwn(String wwn) {
        this.wwn = wwn;
    }

    public String getSerialNumber() {
        return serialNumber;
    }

    public void setSerialNumber(String serialNumber) {
        this.serialNumber = serialNumber;
    }

    public String getDeviceModel() {
        return deviceModel;
    }

    public void setDeviceModel(String deviceModel) {
        this.deviceModel = deviceModel;
    }

    public Long getSize() {
        return size;
    }

    public void setSize(Long size) {
        this.size = size;
    }

    public String getDriveState() {
        return driveState;
    }

    public void setDriveState(String driveState) {
        this.driveState = driveState;
    }

    public LocateStatus getLocateStatus() {
        return locateStatus;
    }

    public void setLocateStatus(LocateStatus locateStatus) {
        this.locateStatus = locateStatus;
    }

    public String getDriveType() {
        return driveType;
    }

    public void setDriveType(String driveType) {
        this.driveType = driveType;
    }

    public String getMediaType() {
        return mediaType;
    }

    public void setMediaType(String mediaType) {
        this.mediaType = mediaType;
    }

    public Integer getRotationRate() {
        return rotationRate;
    }

    public void setRotationRate(Integer rotationRate) {
        this.rotationRate = rotationRate;
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

    public Integer getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(Integer deviceId) {
        this.deviceId = deviceId;
    }
}
