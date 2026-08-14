package org.zstack.storage.device.localRaid;

import org.zstack.header.configuration.PythonClassInventory;
import org.zstack.header.query.ExpandedQueries;
import org.zstack.header.query.ExpandedQuery;
import org.zstack.header.search.Inventory;

import javax.persistence.PreUpdate;
import java.io.Serializable;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@PythonClassInventory
@Inventory(mappingVOClass = RaidPhysicalDriveVO.class)
@ExpandedQueries({
        @ExpandedQuery(expandedField = "raidController", inventoryClass = RaidControllerInventory.class,
                foreignKey = "raidControllerUuid", expandedInventoryKey = "uuid"),
})
public class RaidPhysicalDriveInventory implements Serializable {
    private String uuid;

    private String name;

    private String raidLevel;

    private String raidControllerUuid;

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

    private LocateStatus locateStatus;

    private String driveType;

    private String mediaType;

    private Integer rotationRate;

    private Timestamp createDate;

    private Timestamp lastOpDate;

    public RaidPhysicalDriveInventory() {
    }

    public RaidPhysicalDriveInventory(RaidPhysicalDriveVO vo) {
        this.setUuid(vo.getUuid());
        this.setName(vo.getName());
        this.setRaidLevel(vo.getRaidLevel());
        this.setRaidControllerUuid(vo.getRaidControllerUuid());
        this.setDescription(vo.getDescription());
        this.setEnclosureDeviceId(vo.getEnclosureDeviceId());
        this.setSlotNumber(vo.getSlotNumber());
        this.setDeviceId(vo.getDeviceId());
        this.setDiskGroup(vo.getDiskGroup());
        this.setWwn(vo.getWwn());
        this.setSerialNumber(vo.getSerialNumber());
        this.setDeviceModel(vo.getDeviceModel());
        this.setSize(vo.getSize());
        this.setDriveState(vo.getDriveState());
        this.setLocateStatus(vo.getLocateStatus());
        this.setDriveType(vo.getDriveType());
        this.setMediaType(vo.getMediaType());
        this.setRotationRate(vo.getRotationRate());
        this.setCreateDate(vo.getCreateDate());
        this.setLastOpDate(vo.getLastOpDate());
    }

    public static RaidPhysicalDriveInventory valueOf(RaidPhysicalDriveVO vo) {
        return new RaidPhysicalDriveInventory(vo);
    }

    public static List<RaidPhysicalDriveInventory> valueOf(Collection<RaidPhysicalDriveVO> vos) {
        List<RaidPhysicalDriveInventory> invs = new ArrayList<>(vos.size());
        for (RaidPhysicalDriveVO vo : vos) {
            invs.add(vo.toInventory());
        }
        return invs;
    }

    @PreUpdate
    private void preUpdate() {
        lastOpDate = null;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getRaidControllerUuid() {
        return raidControllerUuid;
    }

    public void setRaidControllerUuid(String raidControllerUuid) {
        this.raidControllerUuid = raidControllerUuid;
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

    public String getRaidLevel() {
        return raidLevel;
    }

    public void setRaidLevel(String raidLevel) {
        this.raidLevel = raidLevel;
    }

    public Integer getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(Integer deviceId) {
        this.deviceId = deviceId;
    }
}
