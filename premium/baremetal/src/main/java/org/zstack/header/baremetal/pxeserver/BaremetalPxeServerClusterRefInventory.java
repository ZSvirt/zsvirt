package org.zstack.header.baremetal.pxeserver;

import org.zstack.header.cluster.ClusterInventory;
import org.zstack.header.query.ExpandedQueries;
import org.zstack.header.query.ExpandedQuery;
import org.zstack.header.search.Inventory;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Created by GuoYi on 2018-10-10.
 */
@Inventory(mappingVOClass = BaremetalPxeServerClusterRefVO.class)
@ExpandedQueries({
        @ExpandedQuery(expandedField = "cluster", inventoryClass = ClusterInventory.class,
                foreignKey = "clusterUuid", expandedInventoryKey = "uuid"),
        @ExpandedQuery(expandedField = "pxeServerUuid", inventoryClass = BaremetalPxeServerInventory.class,
                foreignKey = "pxeServerUuid", expandedInventoryKey = "uuid"),
})
public class BaremetalPxeServerClusterRefInventory {
    private Long id;
    private String clusterUuid;
    private String pxeServerUuid;
    private Timestamp createDate;
    private Timestamp lastOpDate;

    public static BaremetalPxeServerClusterRefInventory valueOf(BaremetalPxeServerClusterRefVO vo) {
        BaremetalPxeServerClusterRefInventory inv = new BaremetalPxeServerClusterRefInventory();
        inv.setId(vo.getId());
        inv.setClusterUuid(vo.getClusterUuid());
        inv.setPxeServerUuid(vo.getPxeServerUuid());
        inv.setCreateDate(vo.getCreateDate());
        inv.setLastOpDate(vo.getLastOpDate());
        return inv;
    }

    public static List<BaremetalPxeServerClusterRefInventory> valueOf(Collection<BaremetalPxeServerClusterRefVO> vos) {
        List<BaremetalPxeServerClusterRefInventory> invs = new ArrayList<>();
        for (BaremetalPxeServerClusterRefVO vo : vos) {
            invs.add(valueOf(vo));
        }
        return invs;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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
