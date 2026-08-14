package org.zstack.header.baremetal.instance;

import org.zstack.header.baremetal.chassis.BaremetalChassisInventory;
import org.zstack.header.baremetal.network.BaremetalNicInventory;
import org.zstack.header.baremetal.pxeserver.BaremetalPxeServerInventory;
import org.zstack.header.cluster.ClusterInventory;
import org.zstack.header.configuration.PythonClassInventory;
import org.zstack.header.image.ImageInventory;
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
 * Created by GuoYi on 7/4/18.
 */
@Inventory(mappingVOClass = BaremetalInstanceVO.class)
@PythonClassInventory
@ExpandedQueries({
        @ExpandedQuery(expandedField = "zone", inventoryClass = ZoneInventory.class, foreignKey = "zoneUuid", expandedInventoryKey = "uuid"),
        @ExpandedQuery(expandedField = "cluster", inventoryClass = ClusterInventory.class, foreignKey = "clusterUuid", expandedInventoryKey = "uuid"),
        @ExpandedQuery(expandedField = "pxeServer", inventoryClass = BaremetalPxeServerInventory.class, foreignKey = "pxeServerUuid", expandedInventoryKey = "uuid"),
        @ExpandedQuery(expandedField = "chassis", inventoryClass = BaremetalChassisInventory.class, foreignKey = "chassisUuid", expandedInventoryKey = "uuid"),
        @ExpandedQuery(expandedField = "image", inventoryClass = ImageInventory.class, foreignKey = "imageUuid", expandedInventoryKey = "uuid"),
        @ExpandedQuery(expandedField = "bmNics", inventoryClass = BaremetalNicInventory.class, foreignKey = "uuid", expandedInventoryKey = "baremetalInstanceUuid"),
})
public class BaremetalInstanceInventory implements Serializable, Cloneable {
    private String uuid;
    private String name;
    private String description;
    @APINoSee
    private Long internalId;
    private String zoneUuid;
    private String clusterUuid;
    private String pxeServerUuid;
    private String chassisUuid;
    private String imageUuid;
    private String templateUuid;
    private String platform;
    private String managementIp;
    private String username;
    @APINoSee
    @NoLogging
    private String password;
    private Integer port;
    private String state;
    private String status;
    private Timestamp createDate;
    private Timestamp lastOpDate;

    private List<BaremetalNicInventory> bmNics;

    public BaremetalInstanceInventory() {
    }

    protected BaremetalInstanceInventory(BaremetalInstanceVO vo) {
        this.setUuid(vo.getUuid());
        this.setName(vo.getName());
        this.setDescription(vo.getDescription());
        this.setInternalId(vo.getInternalId());
        this.setZoneUuid(vo.getZoneUuid());
        this.setClusterUuid(vo.getClusterUuid());
        this.setPxeServerUuid(vo.getPxeServerUuid());
        this.setChassisUuid(vo.getChassisUuid());
        this.setImageUuid(vo.getImageUuid());
        this.setTemplateUuid(vo.getTemplateUuid());
        this.setPlatform(vo.getPlatform());
        this.setManagementIp(vo.getManagementIp());
        this.setUsername(vo.getUsername());
        this.setPassword(vo.getPassword());
        this.setPort(vo.getPort());
        this.setState(vo.getState().toString());
        this.setStatus(vo.getStatus().toString());
        this.setCreateDate(vo.getCreateDate());
        this.setLastOpDate(vo.getLastOpDate());

        this.setBmNics(BaremetalNicInventory.valueOf(vo.getBmNics()));
    }

    public static BaremetalInstanceInventory valueOf(BaremetalInstanceVO vo) {
        return new BaremetalInstanceInventory(vo);
    }

    public static List<BaremetalInstanceInventory> valueOf(Collection<BaremetalInstanceVO> vos) {
        List<BaremetalInstanceInventory> inventories = new ArrayList<>();
        for (BaremetalInstanceVO vo : vos) {
            inventories.add(BaremetalInstanceInventory.valueOf(vo));
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

    public Long getInternalId() {
        return internalId;
    }

    public void setInternalId(Long internalId) {
        this.internalId = internalId;
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

    public String getChassisUuid() {
        return chassisUuid;
    }

    public void setChassisUuid(String chassisUuid) {
        this.chassisUuid = chassisUuid;
    }

    public String getImageUuid() {
        return imageUuid;
    }

    public void setImageUuid(String imageUuid) {
        this.imageUuid = imageUuid;
    }

    public String getTemplateUuid() {
        return templateUuid;
    }

    public void setTemplateUuid(String templateUuid) {
        this.templateUuid = templateUuid;
    }

    public String getPlatform() {
        return platform;
    }

    public void setPlatform(String platform) {
        this.platform = platform;
    }

    public String getManagementIp() {
        return managementIp;
    }

    public void setManagementIp(String managementIp) {
        this.managementIp = managementIp;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public Integer getPort() {
        return port;
    }

    public void setPort(Integer port) {
        this.port = port;
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

    public List<BaremetalNicInventory> getBmNics() {
        return bmNics;
    }

    public void setBmNics(List<BaremetalNicInventory> bmNics) {
        this.bmNics = bmNics;
    }
}
