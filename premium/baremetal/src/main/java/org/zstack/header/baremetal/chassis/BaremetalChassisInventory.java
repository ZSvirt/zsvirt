package org.zstack.header.baremetal.chassis;

import org.zstack.header.baremetal.instance.BaremetalInstanceInventory;
import org.zstack.header.baremetal.pxeserver.BaremetalPxeServerInventory;
import org.zstack.header.cluster.ClusterInventory;
import org.zstack.header.configuration.PythonClassInventory;
import org.zstack.header.log.NoLogging;
import org.zstack.header.query.ExpandedQueries;
import org.zstack.header.query.ExpandedQuery;
import org.zstack.header.rest.APINoSee;
import org.zstack.header.search.Inventory;
import org.zstack.header.zone.ZoneInventory;

import java.io.Serializable;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Created by GuoYi on 4/26/17.
 */
@Inventory(mappingVOClass = BaremetalChassisVO.class)
@PythonClassInventory
@ExpandedQueries({
        @ExpandedQuery(expandedField = "zone", inventoryClass = ZoneInventory.class,
                foreignKey = "zoneUuid", expandedInventoryKey = "uuid"),
        @ExpandedQuery(expandedField = "cluster", inventoryClass = ClusterInventory.class,
                foreignKey = "clusterUuid", expandedInventoryKey = "uuid"),
        @ExpandedQuery(expandedField = "pxeServer", inventoryClass = BaremetalPxeServerInventory.class,
                foreignKey = "pxeServerUuid", expandedInventoryKey = "uuid"),
        @ExpandedQuery(expandedField = "baremetalInstance", inventoryClass = BaremetalInstanceInventory.class,
                foreignKey = "uuid", expandedInventoryKey = "chassisUuid"),
        @ExpandedQuery(expandedField = "hardwareInfos", inventoryClass = BaremetalHardwareInfoInventory.class,
                foreignKey = "uuid", expandedInventoryKey = "chassisUuid"),
})
public class BaremetalChassisInventory implements Serializable {
    private String uuid;
    private String name;
    private String description;
    private String zoneUuid;
    private String clusterUuid;
    private String pxeServerUuid;
    private String ipmiAddress;
    private Integer ipmiPort;
    private String ipmiUsername;
    @APINoSee
    @NoLogging
    private String ipmiPassword;
    private String state;
    private String status;
    private Timestamp createDate;
    private Timestamp lastOpDate;

    private List<BaremetalHardwareInfoInventory> hardwareInfos;

    public BaremetalChassisInventory() {
    }

    public static BaremetalChassisInventory valueOf(BaremetalChassisVO vo) {
        BaremetalChassisInventory chassis = new BaremetalChassisInventory();
        chassis.setUuid(vo.getUuid());
        chassis.setName(vo.getName());
        chassis.setDescription(vo.getDescription());
        chassis.setZoneUuid(vo.getZoneUuid());
        chassis.setClusterUuid(vo.getClusterUuid());
        chassis.setPxeServerUuid(vo.getPxeServerUuid());
        chassis.setIpmiAddress(vo.getIpmiAddress());
        chassis.setIpmiPort(vo.getIpmiPort());
        chassis.setIpmiUsername(vo.getIpmiUsername());
        chassis.setIpmiPassword(vo.getIpmiPassword());
        chassis.setState(vo.getState().toString());
        chassis.setStatus(vo.getStatus().toString());
        chassis.setCreateDate(vo.getCreateDate());
        chassis.setLastOpDate(vo.getLastOpDate());
        chassis.setHardwareInfos(BaremetalHardwareInfoInventory.valueOf(vo.getHardwareInfos()));
        return chassis;
    }

    public static List<BaremetalChassisInventory> valueOf(Collection<BaremetalChassisVO> vos) {
        List<BaremetalChassisInventory> inventories = new ArrayList<>();
        for (BaremetalChassisVO vo: vos) {
            inventories.add(valueOf(vo));
        }
        return inventories;
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

    public String getPxeServerUuid() {
        return pxeServerUuid;
    }

    public void setPxeServerUuid(String pxeServerUuid) {
        this.pxeServerUuid = pxeServerUuid;
    }

    public String getIpmiAddress() {
        return ipmiAddress;
    }

    public void setIpmiAddress(String ipmiAddress) {
        this.ipmiAddress = ipmiAddress;
    }

    public Integer getIpmiPort() {
        return ipmiPort;
    }

    public void setIpmiPort(Integer ipmiPort) {
        this.ipmiPort = ipmiPort;
    }

    public String getIpmiUsername() {
        return ipmiUsername;
    }

    public void setIpmiUsername(String ipmiUsername) {
        this.ipmiUsername = ipmiUsername;
    }

    public String getIpmiPassword() {
        return ipmiPassword;
    }

    public void setIpmiPassword(String ipmiPassword) {
        this.ipmiPassword = ipmiPassword;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
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

    public List<BaremetalHardwareInfoInventory> getHardwareInfos() {
        return hardwareInfos;
    }

    public void setHardwareInfos(List<BaremetalHardwareInfoInventory> hardwareInfos) {
        this.hardwareInfos = hardwareInfos;
    }
}
