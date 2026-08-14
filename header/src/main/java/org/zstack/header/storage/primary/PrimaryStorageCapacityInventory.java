package org.zstack.header.storage.primary;

import org.zstack.header.search.Inventory;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 */
@Inventory(mappingVOClass = PrimaryStorageCapacityVO.class)
public class PrimaryStorageCapacityInventory {
    private String uuid;
    private Long totalCapacity;
    private Long availableCapacity;
    private Long totalPhysicalCapacity;
    private Long availablePhysicalCapacity;
    private Long systemUsedCapacity;
    private Timestamp createDate;
    private Timestamp lastOpDate;

    public static PrimaryStorageCapacityInventory valueOf(PrimaryStorageCapacityVO vo) {
        PrimaryStorageCapacityInventory inv = new PrimaryStorageCapacityInventory();
        inv.setUuid(vo.getUuid());
        inv.setTotalCapacity(vo.getTotalCapacity());
        inv.setAvailableCapacity(vo.getAvailableCapacity());
        inv.setCreateDate(vo.getCreateDate());
        inv.setLastOpDate(vo.getLastOpDate());
        inv.setTotalPhysicalCapacity(vo.getTotalPhysicalCapacity());
        inv.setAvailablePhysicalCapacity(vo.getAvailablePhysicalCapacity());
        inv.setSystemUsedCapacity(vo.getSystemUsedCapacity());
        return inv;
    }

    public static List<PrimaryStorageCapacityInventory> valueOf(Collection<PrimaryStorageCapacityVO> vos) {
        List<PrimaryStorageCapacityInventory> invs = new ArrayList<PrimaryStorageCapacityInventory>(vos.size());
        for (PrimaryStorageCapacityVO vo : vos) {
            invs.add(valueOf(vo));
        }
        return invs;
    }

    public Long getSystemUsedCapacity() {
        return systemUsedCapacity;
    }

    public void setSystemUsedCapacity(Long systemUsedCapacity) {
        this.systemUsedCapacity = systemUsedCapacity;
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
