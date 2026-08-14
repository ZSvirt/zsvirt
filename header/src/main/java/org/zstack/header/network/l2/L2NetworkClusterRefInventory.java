package org.zstack.header.network.l2;

import org.zstack.header.cluster.ClusterInventory;
import org.zstack.header.query.ExpandedQueries;
import org.zstack.header.query.ExpandedQuery;
import org.zstack.header.search.Inventory;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@Inventory(mappingVOClass = L2NetworkClusterRefVO.class)
@ExpandedQueries({
        @ExpandedQuery(expandedField = "cluster", inventoryClass = ClusterInventory.class,
                foreignKey = "clusterUuid", expandedInventoryKey = "uuid"),
        @ExpandedQuery(expandedField = "l2Network", inventoryClass = L2NetworkInventory.class,
                foreignKey = "l2NetworkUuid", expandedInventoryKey = "uuid"),
})
public class L2NetworkClusterRefInventory {
    private String clusterUuid;
    private String l2NetworkUuid;

    private String l2ProviderType;

    private Timestamp createDate;
    private Timestamp lastOpDate;


    public static L2NetworkClusterRefInventory valueOf(L2NetworkClusterRefVO vo) {
        L2NetworkClusterRefInventory inv = new L2NetworkClusterRefInventory();
        inv.setClusterUuid(vo.getClusterUuid());
        inv.setL2NetworkUuid(vo.getL2NetworkUuid());
        inv.setL2ProviderType(vo.getL2ProviderType());
        inv.setCreateDate(vo.getCreateDate());
        inv.setLastOpDate(vo.getLastOpDate());
        return inv;
    }

    public static List<L2NetworkClusterRefInventory> valueOf(Collection<L2NetworkClusterRefVO> vos) {
        List<L2NetworkClusterRefInventory> invs = new ArrayList<L2NetworkClusterRefInventory>();
        for (L2NetworkClusterRefVO vo : vos) {
            invs.add(valueOf(vo));
        }
        return invs;
    }

    public String getL2ProviderType() {
        return l2ProviderType;
    }

    public void setL2ProviderType(String l2ProviderType) {
        this.l2ProviderType = l2ProviderType;
    }

    public String getClusterUuid() {
        return clusterUuid;
    }

    public void setClusterUuid(String clusterUuid) {
        this.clusterUuid = clusterUuid;
    }

    public String getL2NetworkUuid() {
        return l2NetworkUuid;
    }

    public void setL2NetworkUuid(String l2NetworkUuid) {
        this.l2NetworkUuid = l2NetworkUuid;
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
