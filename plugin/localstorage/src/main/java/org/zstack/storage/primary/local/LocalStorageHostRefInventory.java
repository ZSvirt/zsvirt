package org.zstack.storage.primary.local;

import org.zstack.header.host.HostInventory;
import org.zstack.header.query.ExpandedQueries;
import org.zstack.header.query.ExpandedQuery;
import org.zstack.header.search.Inventory;
import org.zstack.header.storage.primary.PrimaryStorageInventory;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by frank on 6/30/2015.
 */
@Inventory(mappingVOClass = LocalStorageHostRefVO.class)
@ExpandedQueries({
        @ExpandedQuery(expandedField = "host", inventoryClass = HostInventory.class,
                foreignKey = "hostUuid", expandedInventoryKey = "uuid"),
        @ExpandedQuery(expandedField = "primaryStorage", inventoryClass = PrimaryStorageInventory.class,
                foreignKey = "primaryStorageUuid", expandedInventoryKey = "uuid")
})
public class LocalStorageHostRefInventory {
    private String primaryStorageUuid;
    private String hostUuid;
    private Long totalCapacity;
    private Long availableCapacity;
    private Long totalPhysicalCapacity;
    private Long availablePhysicalCapacity;
    private Long systemUsedCapacity;
    private Timestamp createDate;
    private Timestamp lastOpDate;

    public static LocalStorageHostRefInventory valueOf(LocalStorageHostRefVO vo) {
        LocalStorageHostRefInventory inv = new LocalStorageHostRefInventory();
        inv.setPrimaryStorageUuid(vo.getPrimaryStorageUuid());
        inv.setHostUuid(vo.getHostUuid());
        inv.setAvailableCapacity(vo.getAvailableCapacity());
        inv.setAvailablePhysicalCapacity(vo.getAvailablePhysicalCapacity());
        inv.setTotalCapacity(vo.getTotalCapacity());
        inv.setTotalPhysicalCapacity(vo.getTotalPhysicalCapacity());
        inv.setSystemUsedCapacity(vo.getSystemUsedCapacity());
        inv.setCreateDate(vo.getCreateDate());
        inv.setLastOpDate(vo.getLastOpDate());
        return inv;
    }

    public static List<LocalStorageHostRefInventory> valueOf(List<LocalStorageHostRefVO> vos) {
        List<LocalStorageHostRefInventory> invs = new ArrayList<LocalStorageHostRefInventory>();
        for (LocalStorageHostRefVO vo : vos) {
            invs.add(valueOf(vo));
        }

        return invs;
    }

    public String getPrimaryStorageUuid() {
        return primaryStorageUuid;
    }

    public void setPrimaryStorageUuid(String primaryStorageUuid) {
        this.primaryStorageUuid = primaryStorageUuid;
    }

    public String getHostUuid() {
        return hostUuid;
    }

    public void setHostUuid(String hostUuid) {
        this.hostUuid = hostUuid;
    }

    public Long getTotalCapacity() {
        return totalCapacity;
    }

    public void setTotalCapacity(Long totalCapacity) {
        this.totalCapacity = totalCapacity;
    }

    public Long getAvailableCapacity() {
        return availableCapacity;
    }

    public void setAvailableCapacity(Long availableCapacity) {
        this.availableCapacity = availableCapacity;
    }

    public Long getTotalPhysicalCapacity() {
        return totalPhysicalCapacity;
    }

    public void setTotalPhysicalCapacity(Long totalPhysicalCapacity) {
        this.totalPhysicalCapacity = totalPhysicalCapacity;
    }

    public Long getAvailablePhysicalCapacity() {
        return availablePhysicalCapacity;
    }

    public void setAvailablePhysicalCapacity(Long availablePhysicalCapacity) {
        this.availablePhysicalCapacity = availablePhysicalCapacity;
    }

    public Long getSystemUsedCapacity() {
        return systemUsedCapacity;
    }

    public void setSystemUsedCapacity(Long systemUsedCapacity) {
        this.systemUsedCapacity = systemUsedCapacity;
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
