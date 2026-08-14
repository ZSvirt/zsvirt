package org.zstack.header.storage.primary;

import org.zstack.header.cluster.ClusterInventory;
import org.zstack.header.query.ExpandedQueries;
import org.zstack.header.query.ExpandedQuery;
import org.zstack.header.search.Inventory;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@Inventory(mappingVOClass = PrimaryStorageClusterRefVO.class)
@ExpandedQueries({
        @ExpandedQuery(expandedField = "cluster", inventoryClass = ClusterInventory.class,
                foreignKey = "clusterUuid", expandedInventoryKey = "uuid"),
        @ExpandedQuery(expandedField = "primaryStorage", inventoryClass = PrimaryStorageInventory.class,
                foreignKey = "primaryStorageUuid", expandedInventoryKey = "uuid"),
})
public class PrimaryStorageClusterRefInventory {
    private Long id;
    private String clusterUuid;
    private String primaryStorageUuid;
    private Timestamp createDate;
    private Timestamp lastOpDate;

    public static PrimaryStorageClusterRefInventory valueOf(PrimaryStorageClusterRefVO vo) {
        PrimaryStorageClusterRefInventory inv = new PrimaryStorageClusterRefInventory();
        inv.setClusterUuid(vo.getClusterUuid());
        inv.setId(vo.getId());
        inv.setLastOpDate(vo.getLastOpDate());
        inv.setCreateDate(vo.getCreateDate());
        return inv;
    }

    public static List<PrimaryStorageClusterRefInventory> valueOf(Collection<PrimaryStorageClusterRefVO> vos) {
        List<PrimaryStorageClusterRefInventory> invs = new ArrayList<PrimaryStorageClusterRefInventory>();
        for (PrimaryStorageClusterRefVO vo : vos) {
            invs.add(valueOf(vo));
        }
        return invs;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getClusterUuid() {
        return clusterUuid;
    }

    public void setClusterUuid(String clusterUuid) {
        this.clusterUuid = clusterUuid;
    }

    public String getPrimaryStorageUuid() {
        return primaryStorageUuid;
    }

    public void setPrimaryStorageUuid(String primaryStorageUuid) {
        this.primaryStorageUuid = primaryStorageUuid;
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
