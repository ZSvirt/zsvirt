package org.zstack.header.network.l2;

import org.zstack.header.host.HostInventory;
import org.zstack.header.query.ExpandedQueries;
import org.zstack.header.query.ExpandedQuery;
import org.zstack.header.search.Inventory;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@Inventory(mappingVOClass = L2NetworkHostRefVO.class)
@ExpandedQueries({
        @ExpandedQuery(expandedField = "host", inventoryClass = HostInventory.class,
                foreignKey = "hostUuid", expandedInventoryKey = "uuid"),
        @ExpandedQuery(expandedField = "l2Network", inventoryClass = L2NetworkInventory.class,
                foreignKey = "l2NetworkUuid", expandedInventoryKey = "uuid")
})
public class L2NetworkHostRefInventory {
    private String hostUuid;
    private String l2NetworkUuid;
    private String l2ProviderType;
    private String bridgeName;
    private boolean skipDeletion;
    private Timestamp createDate;
    private Timestamp lastOpDate;

    public L2NetworkHostRefInventory() {
    }

    public L2NetworkHostRefInventory(L2NetworkHostRefVO vo) {
        this.setHostUuid(vo.getHostUuid());
        this.setL2NetworkUuid(vo.getL2NetworkUuid());
        this.setL2ProviderType(vo.getL2ProviderType());
        this.setBridgeName(vo.getBridgeName());
        this.setCreateDate(vo.getCreateDate());
        this.setLastOpDate(vo.getLastOpDate());
    }

    public static L2NetworkHostRefInventory valueOf(L2NetworkHostRefVO vo) {
        return new L2NetworkHostRefInventory(vo);
    }

    public static List<L2NetworkHostRefInventory> valueOf(Collection<L2NetworkHostRefVO> vos) {
        List<L2NetworkHostRefInventory> invs = new ArrayList<L2NetworkHostRefInventory>();
        for (L2NetworkHostRefVO vo : vos) {
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

    public String getHostUuid() {
        return hostUuid;
    }

    public void setHostUuid(String hostUuid) {
        this.hostUuid = hostUuid;
    }

    public String getL2NetworkUuid() {
        return l2NetworkUuid;
    }

    public void setL2NetworkUuid(String l2NetworkUuid) {
        this.l2NetworkUuid = l2NetworkUuid;
    }

    public String getBridgeName() {
        return bridgeName;
    }

    public void setBridgeName(String bridgeName) {
        this.bridgeName = bridgeName;
    }

    public boolean getSkipDeletion() {
        return skipDeletion;
    }

    public void setSkipDeletion(boolean skipDeletion) {
        this.skipDeletion = skipDeletion;
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
