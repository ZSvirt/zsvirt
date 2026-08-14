package org.zstack.storage.device.localRaid;

import org.zstack.core.Platform;
import org.zstack.header.configuration.PythonClass;

import java.sql.Timestamp;

@PythonClass
public class RaidPhysicalDriveStruct {
    private String name;

    private String raidLevel;

    private String description;

    private Integer enclosureDeviceId;

    private Integer slotNumber;

    private Integer deviceId;

    private Integer diskGroup;

    private String wwn;

    private String serialNumber;

    private String deviceModel;

    private Long size;

    private String driveState;

    private String locateStatus;

    private String driveType;

    private String mediaType;

    private Integer rotationRate;

    private String raidControllerSasAddreess;

    private String raidControllerProductName;

    private Integer raidControllerNumber;

    public RaidPhysicalDriveVO toVO(String raidControllerUuid) {
        RaidPhysicalDriveVO vo = new RaidPhysicalDriveVO();
        vo.setRaidLevel(this.getRaidLevel());
        vo.setName(this.getName() == null ? String.format("raid-pd-%s", this.getWwn()) : this.getName());
        vo.setDescription(this.getDescription());
        vo.setEnclosureDeviceId(this.getEnclosureDeviceId());
        vo.setSlotNumber(this.getSlotNumber());
        vo.setDiskGroup(this.getDiskGroup());
        vo.setDeviceId(this.getDeviceId());
        vo.setWwn(this.getWwn());
        vo.setSerialNumber(this.getSerialNumber());
        vo.setDeviceModel(this.getDeviceModel());
        vo.setSize(this.getSize());
        vo.setDriveState(this.getDriveState());
        vo.setDriveType(this.getDriveType());
        vo.setMediaType(this.getMediaType());
        vo.setRotationRate(this.getRotationRate());
        vo.setCreateDate(new Timestamp(System.currentTimeMillis()));
        vo.setLastOpDate(new Timestamp(System.currentTimeMillis()));
        vo.setUuid(Platform.getUuid());
        vo.setRaidControllerUuid(raidControllerUuid);
        vo.setLocateStatus(LocateStatus.Disabled);

        return vo;
    }

    public Boolean compareToVO(RaidPhysicalDriveVO that) {

        if (getRaidLevel() != null ? !getRaidLevel().equals(that.getRaidLevel()) : that.getRaidLevel() != null)
            return false;
        if (getEnclosureDeviceId() != null ? !getEnclosureDeviceId().equals(that.getEnclosureDeviceId()) : that.getEnclosureDeviceId() != null)
            return false;
        if (getSlotNumber() != null ? !getSlotNumber().equals(that.getSlotNumber()) : that.getSlotNumber() != null)
            return false;
        if (getDiskGroup() != null ? !getDiskGroup().equals(that.getDiskGroup()) : that.getDiskGroup() != null)
            return false;
        if (getWwn() != null ? !getWwn().equals(that.getWwn()) : that.getWwn() != null) return false;
        if (getSerialNumber() != null ? !getSerialNumber().equals(that.getSerialNumber()) : that.getSerialNumber() != null)
            return false;
        if (getDeviceModel() != null ? !getDeviceModel().equals(that.getDeviceModel()) : that.getDeviceModel() != null)
            return false;
        if (getSize() != null ? !getSize().equals(that.getSize()) : that.getSize() != null) return false;
        if (getDriveState() != null ? !getDriveState().equals(that.getDriveState()) : that.getDriveState() != null)
            return false;
        if (getDriveType() != null ? !getDriveType().equals(that.getDriveType()) : that.getDriveType() != null)
            return false;
        if (getMediaType() != null ? !getMediaType().equals(that.getMediaType()) : that.getMediaType() != null)
            return false;
        if (getRotationRate() != null ? !getRotationRate().equals(that.getRotationRate()) : that.getRotationRate() != null)
            return false;
        if (getDeviceId() != null ? !getDeviceId().equals(that.getDeviceId()) : that.getDeviceId() != null)
            return false;
        return true;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getRaidLevel() {
        return raidLevel;
    }

    public void setRaidLevel(String raidLevel) {
        this.raidLevel = raidLevel;
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

    public String getLocateStatus() {
        return locateStatus;
    }

    public void setLocateStatus(String locateStatus) {
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

    public String getRaidControllerSasAddreess() {
        return raidControllerSasAddreess;
    }

    public void setRaidControllerSasAddreess(String raidControllerSasAddreess) {
        this.raidControllerSasAddreess = raidControllerSasAddreess;
    }

    public String getRaidControllerProductName() {
        return raidControllerProductName;
    }

    public void setRaidControllerProductName(String raidControllerProductName) {
        this.raidControllerProductName = raidControllerProductName;
    }

    public Integer getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(Integer deviceId) {
        this.deviceId = deviceId;
    }

    public Integer getRaidControllerNumber() {
        return raidControllerNumber;
    }

    public void setRaidControllerNumber(Integer raidControllerNumber) {
        this.raidControllerNumber = raidControllerNumber;
    }
}
