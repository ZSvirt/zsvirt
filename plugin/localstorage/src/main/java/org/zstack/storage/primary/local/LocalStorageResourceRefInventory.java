package org.zstack.storage.primary.local;

import org.zstack.header.image.ImageInventory;
import org.zstack.header.query.ExpandedQueries;
import org.zstack.header.query.ExpandedQuery;
import org.zstack.header.search.Inventory;
import org.zstack.header.storage.snapshot.VolumeSnapshotInventory;
import org.zstack.header.volume.VolumeInventory;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Created by frank on 11/14/2015.
 */
@Inventory(mappingVOClass = LocalStorageResourceRefVO.class)
@ExpandedQueries({
        @ExpandedQuery(expandedField = "volume", inventoryClass = VolumeInventory.class,
                foreignKey = "resourceUuid", expandedInventoryKey = "uuid"),
        @ExpandedQuery(expandedField = "image", inventoryClass = ImageInventory.class,
                foreignKey = "resourceUuid", expandedInventoryKey = "uuid"),
        @ExpandedQuery(expandedField = "snapshot", inventoryClass = VolumeSnapshotInventory.class,
                foreignKey = "resourceUuid", expandedInventoryKey = "uuid"),
        @ExpandedQuery(target = VolumeInventory.class, expandedField = "localStorageHostRef", inventoryClass = LocalStorageResourceRefInventory.class,
                foreignKey = "uuid", expandedInventoryKey = "resourceUuid"),
        @ExpandedQuery(target = VolumeSnapshotInventory.class, expandedField = "localStorageHostRef", inventoryClass = LocalStorageResourceRefInventory.class,
                foreignKey = "uuid", expandedInventoryKey = "resourceUuid")
})
public class LocalStorageResourceRefInventory {
    private String resourceUuid;
    private String primaryStorageUuid;
    private String hostUuid;
    private Long size;
    private String resourceType;
    private Timestamp createDate;
    private Timestamp lastOpDate;

    public static LocalStorageResourceRefInventory valueOf(LocalStorageResourceRefVO vo) {
        LocalStorageResourceRefInventory inv = new LocalStorageResourceRefInventory();
        inv.setResourceType(vo.getResourceType());
        inv.setResourceUuid(vo.getResourceUuid());
        inv.setHostUuid(vo.getHostUuid());
        inv.setSize(vo.getSize());
        inv.setCreateDate(vo.getCreateDate());
        inv.setLastOpDate(vo.getLastOpDate());
        inv.setPrimaryStorageUuid(vo.getPrimaryStorageUuid());
        return inv;
    }

    public static List<LocalStorageResourceRefInventory> valueOf(Collection<LocalStorageResourceRefVO> vos) {
        List<LocalStorageResourceRefInventory> invs = new ArrayList<LocalStorageResourceRefInventory>();
        for (LocalStorageResourceRefVO vo : vos) {
            invs.add(valueOf(vo));
        }
        return invs;
    }

    public String getResourceUuid() {
        return resourceUuid;
    }

    public void setResourceUuid(String resourceUuid) {
        this.resourceUuid = resourceUuid;
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

    public Long getSize() {
        return size;
    }

    public void setSize(Long size) {
        this.size = size;
    }

    public String getResourceType() {
        return resourceType;
    }

    public void setResourceType(String resourceType) {
        this.resourceType = resourceType;
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
