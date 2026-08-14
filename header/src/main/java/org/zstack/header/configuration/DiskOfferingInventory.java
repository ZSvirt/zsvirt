package org.zstack.header.configuration;

import org.zstack.header.query.ExpandedQueries;
import org.zstack.header.query.ExpandedQuery;
import org.zstack.header.search.Inventory;
import org.zstack.header.volume.VolumeInventory;

import java.io.Serializable;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@Inventory(mappingVOClass = DiskOfferingVO.class)
@PythonClassInventory
@ExpandedQueries({
        @ExpandedQuery(expandedField = "volume", inventoryClass = VolumeInventory.class,
                foreignKey = "uuid", expandedInventoryKey = "diskOfferingUuid"),
})
public class DiskOfferingInventory implements Serializable {
    private String uuid;
    private String name;
    private String description;
    private Long diskSize;
    private Integer sortKey;
    private String state;
    private String type;
    private Timestamp createDate;
    private Timestamp lastOpDate;
    private String allocatorStrategy;

    public static DiskOfferingInventory valueOf(DiskOfferingVO vo) {
        DiskOfferingInventory inv = new DiskOfferingInventory();
        inv.setCreateDate(vo.getCreateDate());
        inv.setDescription(vo.getDescription());
        inv.setDiskSize(vo.getDiskSize());
        inv.setName(vo.getName());
        inv.setSortKey(vo.getSortKey());
        inv.setUuid(vo.getUuid());
        inv.setAllocatorStrategy(vo.getAllocatorStrategy());
        inv.setLastOpDate(vo.getLastOpDate());
        inv.setType(vo.getType());
        inv.setState(vo.getState().toString());
        return inv;
    }

    public static List<DiskOfferingInventory> valueOf(Collection<DiskOfferingVO> vos) {
        List<DiskOfferingInventory> invs = new ArrayList<DiskOfferingInventory>(vos.size());
        for (DiskOfferingVO vo : vos) {
            invs.add(DiskOfferingInventory.valueOf(vo));
        }
        return invs;
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

    public long getDiskSize() {
        return diskSize;
    }

    public void setDiskSize(long diskSize) {
        this.diskSize = diskSize;
    }

    public int getSortKey() {
        return sortKey;
    }

    public void setSortKey(int sortKey) {
        this.sortKey = sortKey;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getAllocatorStrategy() {
        return allocatorStrategy;
    }

    public void setAllocatorStrategy(String allocatorStrategy) {
        this.allocatorStrategy = allocatorStrategy;
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

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }
}
