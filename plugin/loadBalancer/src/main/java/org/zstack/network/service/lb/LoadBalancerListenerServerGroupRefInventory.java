package org.zstack.network.service.lb;

import org.zstack.header.query.ExpandedQueries;
import org.zstack.header.query.ExpandedQuery;
import org.zstack.header.query.ExpandedQueryAlias;
import org.zstack.header.query.ExpandedQueryAliases;
import org.zstack.header.search.Inventory;

import java.io.Serializable;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@Inventory(mappingVOClass = LoadBalancerListenerServerGroupRefVO.class)
@ExpandedQueries({
        @ExpandedQuery(expandedField = "group", inventoryClass = LoadBalancerServerGroupInventory.class,
                foreignKey = "serverGroupUuid", expandedInventoryKey = "uuid"),
        @ExpandedQuery(expandedField = "listener", inventoryClass = LoadBalancerListenerInventory.class,
                foreignKey = "listenerUuid", expandedInventoryKey = "uuid"),
})
public class LoadBalancerListenerServerGroupRefInventory implements Serializable {
    private long id;
    private String listenerUuid;
    private String serverGroupUuid;
    private Timestamp createDate;
    private Timestamp lastOpDate;

    public static LoadBalancerListenerServerGroupRefInventory valueOf(LoadBalancerListenerServerGroupRefVO vo) {
        LoadBalancerListenerServerGroupRefInventory inv = new LoadBalancerListenerServerGroupRefInventory();
        inv.setId(vo.getId());
        inv.setListenerUuid(vo.getListenerUuid());
        inv.setServerGroupUuid(vo.getServerGroupUuid());
        inv.setCreateDate(vo.getCreateDate());
        inv.setLastOpDate(vo.getLastOpDate());
        return inv;
    }

    public static List<LoadBalancerListenerServerGroupRefInventory> valueOf(Collection<LoadBalancerListenerServerGroupRefVO> vos) {
        List<LoadBalancerListenerServerGroupRefInventory> invs = new ArrayList<LoadBalancerListenerServerGroupRefInventory>();
        for (LoadBalancerListenerServerGroupRefVO vo : vos) {
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

    public String getListenerUuid() {
        return listenerUuid;
    }

    public void setListenerUuid(String listenerUuid) {
        this.listenerUuid = listenerUuid;
    }

    public String getServerGroupUuid() {
        return serverGroupUuid;
    }

    public void setServerGroupUuid(String serverGroupUuid) {
        this.serverGroupUuid = serverGroupUuid;
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
