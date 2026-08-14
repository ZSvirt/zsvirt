package org.zstack.storage.device.localRaid;

import org.zstack.header.configuration.PythonClassInventory;
import org.zstack.header.query.ExpandedQueries;
import org.zstack.header.query.ExpandedQuery;
import org.zstack.header.search.Inventory;
import java.io.Serializable;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@PythonClassInventory
@Inventory(mappingVOClass = RaidControllerVO.class)
@ExpandedQueries({
        @ExpandedQuery(expandedField = "raidPhysicalDrive", inventoryClass = RaidPhysicalDriveInventory.class,
                foreignKey = "raidControllerUuid", expandedInventoryKey = "uuid"),
})
public class RaidControllerInventory implements Serializable {
    private String name;

    private String uuid;

    private String description;

    private String productName;

    private String sasAddress;

    private String hostUuid;

    private Timestamp createDate;

    private Timestamp lastOpDate;

    private Integer adapterNumber;

    private List<RaidPhysicalDriveInventory> raidPhysicalDrives;

    public RaidControllerInventory() {
    }

    public RaidControllerInventory(RaidControllerVO vo) {
        this.setName(vo.getName());
        this.setUuid(vo.getUuid());
        this.setHostUuid(vo.getHostUuid());
        this.setSasAddress(vo.getSasAddress());
        this.setDescription(vo.getDescription());
        this.setProductName(vo.getProductName());
        this.setAdapterNumber(vo.getAdapterNumber());
        this.setCreateDate(vo.getCreateDate());
        this.setLastOpDate(vo.getLastOpDate());
        this.setRaidPhysicalDrives(RaidPhysicalDriveInventory.valueOf(vo.getRaidPhysicalDrives()));
    }

    public static RaidControllerInventory valueOf(RaidControllerVO vo) {
        return new RaidControllerInventory(vo);
    }

    public static List<RaidControllerInventory> valueOf(Collection<RaidControllerVO> vos) {
        List<RaidControllerInventory> invs = new ArrayList<>(vos.size());
        for (RaidControllerVO vo : vos) {
            invs.add(vo.toInventory());
        }
        return invs;
    }

    public String getSasAddress() {
        return sasAddress;
    }

    public void setSasAddress(String sasAddress) {
        this.sasAddress = sasAddress;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getProductName() {
        return productName;
    }

    public void setProductName(String productName) {
        this.productName = productName;
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

    public List<RaidPhysicalDriveInventory> getRaidPhysicalDrives() {
        return raidPhysicalDrives;
    }

    public void setRaidPhysicalDrives(List<RaidPhysicalDriveInventory> raidPhysicalDrives) {
        this.raidPhysicalDrives = raidPhysicalDrives;
    }

    public String getHostUuid() {
        return hostUuid;
    }

    public void setHostUuid(String hostUuid) {
        this.hostUuid = hostUuid;
    }

    public Integer getAdapterNumber() {
        return adapterNumber;
    }

    public void setAdapterNumber(Integer adapterNumber) {
        this.adapterNumber = adapterNumber;
    }
}
