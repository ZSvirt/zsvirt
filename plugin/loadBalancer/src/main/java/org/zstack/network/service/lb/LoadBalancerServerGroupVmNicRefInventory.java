package org.zstack.network.service.lb;

import org.zstack.header.query.ExpandedQueries;
import org.zstack.header.query.ExpandedQuery;
import org.zstack.header.query.ExpandedQueryAlias;
import org.zstack.header.query.ExpandedQueryAliases;
import org.zstack.header.search.Inventory;
import org.zstack.header.vm.VmNicInventory;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
@Inventory(mappingVOClass = LoadBalancerServerGroupVmNicRefVO.class)
@ExpandedQueries({
        @ExpandedQuery(expandedField = "vmNic", inventoryClass = VmNicInventory.class,
                foreignKey = "vmNicUuid", expandedInventoryKey = "uuid"),
        @ExpandedQuery(expandedField = "serverGroup", inventoryClass = LoadBalancerServerGroupInventory.class,
                foreignKey = "serverGroupUuid", expandedInventoryKey = "uuid"),
        @ExpandedQuery(target = VmNicInventory.class, expandedField = "loadBalancerServerGroupRef",
                inventoryClass = LoadBalancerServerGroupVmNicRefInventory.class, foreignKey = "uuid", expandedInventoryKey = "vmNicUuid", hidden = true)
})
@ExpandedQueryAliases({
        @ExpandedQueryAlias(target = VmNicInventory.class, alias = "loadBalancerServerGroup", expandedField = "loadBalancerServerGroupRef.serverGroup")
})
public class LoadBalancerServerGroupVmNicRefInventory {
    private Long id;
    private String serverGroupUuid;
    private String vmNicUuid;
    private Long weight;
    private String status;
    private Timestamp createDate;
    private Timestamp lastOpDate;

    public static LoadBalancerServerGroupVmNicRefInventory valueOf(LoadBalancerServerGroupVmNicRefVO vo) {
        LoadBalancerServerGroupVmNicRefInventory inv = new LoadBalancerServerGroupVmNicRefInventory();
        inv.setId(vo.getId());
        inv.setListenerUuid(vo.getServerGroupUuid());
        inv.setVmNicUuid(vo.getVmNicUuid());
        inv.setWeight(vo.getWeight());
        inv.setCreateDate(vo.getCreateDate());
        inv.setLastOpDate(vo.getLastOpDate());
        inv.setStatus(vo.getStatus().toString());
        return inv;
    }

    public static List<LoadBalancerServerGroupVmNicRefInventory> valueOf(Collection<LoadBalancerServerGroupVmNicRefVO> vos) {
        List<LoadBalancerServerGroupVmNicRefInventory> invs = new ArrayList<LoadBalancerServerGroupVmNicRefInventory>();
        for (LoadBalancerServerGroupVmNicRefVO vo : vos) {
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
    
    public void setListenerUuid(String listenerUuid) {
        this.serverGroupUuid = listenerUuid;
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

    public void setId(Long id) {
        this.id = id;
    }

    public String getServerGroupUuid() {
        return serverGroupUuid;
    }

    public void setServerGroupUuid(String serverGroupUuid) {
        this.serverGroupUuid = serverGroupUuid;
    }

    public Long getWeight() {
        return weight;
    }

    public void setWeight(Long weight) {
        this.weight = weight;
    }
}
