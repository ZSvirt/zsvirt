package org.zstack.storage.primary.sharedblock;

import org.zstack.header.query.ExpandedQueries;
import org.zstack.header.query.ExpandedQuery;
import org.zstack.header.search.Inventory;

import java.sql.Timestamp;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

@Inventory(mappingVOClass = SharedBlockCapacityVO.class)
@ExpandedQueries({
        @ExpandedQuery(expandedField = "sharedBlock", inventoryClass = SharedBlockInventory.class, foreignKey = "uuid", expandedInventoryKey = "uuid")
})
public class SharedBlockCapacityInventory {
    private String uuid;
    private long totalCapacity;
    private long availableCapacity;
    private Timestamp createDate;
    private Timestamp lastOpDate;

    public static SharedBlockCapacityInventory valueOf(SharedBlockCapacityVO vo) {
        SharedBlockCapacityInventory inv = new SharedBlockCapacityInventory();
        inv.setUuid(vo.getUuid());
        inv.setTotalCapacity(vo.getTotalCapacity());
        inv.setAvailableCapacity(vo.getAvailableCapacity());
        inv.setCreateDate(vo.getCreateDate());
        inv.setLastOpDate(vo.getLastOpDate());
        return inv;
    }

    public static List<SharedBlockCapacityInventory> valueOf(Collection<SharedBlockCapacityVO> vos) {
        return vos.stream().map(SharedBlockCapacityInventory::valueOf).collect(Collectors.toList());
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public long getTotalCapacity() {
        return totalCapacity;
    }

    public void setTotalCapacity(long totalCapacity) {
        this.totalCapacity = totalCapacity;
    }

    public long getAvailableCapacity() {
        return availableCapacity;
    }

    public void setAvailableCapacity(long availableCapacity) {
        this.availableCapacity = availableCapacity;
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
}
