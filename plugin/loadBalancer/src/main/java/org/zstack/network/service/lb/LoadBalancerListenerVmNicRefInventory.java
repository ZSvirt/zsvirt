package org.zstack.network.service.lb;

import org.zstack.header.query.ExpandedQueries;
import org.zstack.header.query.ExpandedQuery;
import org.zstack.header.query.ExpandedQueryAlias;
import org.zstack.header.query.ExpandedQueryAliases;
import org.zstack.header.search.Inventory;
import org.zstack.header.vm.VmNicInventory;
import org.zstack.network.service.vip.VipInventory;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Created by frank on 8/8/2015.
 */
@Inventory(mappingVOClass = LoadBalancerListenerVmNicRefVO.class)
@ExpandedQueries({
        @ExpandedQuery(expandedField = "vmNic", inventoryClass = VmNicInventory.class,
                foreignKey = "vmNicUuid", expandedInventoryKey = "uuid"),
        @ExpandedQuery(expandedField = "listener", inventoryClass = LoadBalancerListenerInventory.class,
                foreignKey = "listenerUuid", expandedInventoryKey = "uuid"),
        @ExpandedQuery(target = VmNicInventory.class, expandedField = "loadBalancerListenerRef",
                inventoryClass = LoadBalancerListenerVmNicRefInventory.class, foreignKey = "uuid", expandedInventoryKey = "vmNicUuid", hidden = true)
})
@ExpandedQueryAliases({
        @ExpandedQueryAlias(target = VmNicInventory.class, alias = "loadBalancerListener", expandedField = "loadBalancerListenerRef.listener")
})
public class LoadBalancerListenerVmNicRefInventory {
    private Long id;
    private String listenerUuid;
    private String vmNicUuid;
    private String status;
    private Timestamp createDate;
    private Timestamp lastOpDate;

    public static LoadBalancerListenerVmNicRefInventory valueOf(LoadBalancerListenerVmNicRefVO vo) {
        LoadBalancerListenerVmNicRefInventory inv = new LoadBalancerListenerVmNicRefInventory();
        inv.setId(vo.getId());
        inv.setListenerUuid(vo.getListenerUuid());
        inv.setVmNicUuid(vo.getVmNicUuid());
        inv.setCreateDate(vo.getCreateDate());
        inv.setLastOpDate(vo.getLastOpDate());
        inv.setStatus(vo.getStatus().toString());
        return inv;
    }

    public static List<LoadBalancerListenerVmNicRefInventory> valueOf(Collection<LoadBalancerListenerVmNicRefVO> vos) {
        List<LoadBalancerListenerVmNicRefInventory> invs = new ArrayList<LoadBalancerListenerVmNicRefInventory>();
        for (LoadBalancerListenerVmNicRefVO vo : vos) {
            invs.add(valueOf(vo));
        }
        return invs;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
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

    public String getVmNicUuid() {
        return vmNicUuid;
    }

    public void setVmNicUuid(String vmNicUuid) {
        this.vmNicUuid = vmNicUuid;
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
