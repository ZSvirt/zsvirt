package org.zstack.network.service.lb;

import org.zstack.core.db.Q;
import org.zstack.header.query.ExpandedQueries;
import org.zstack.header.query.ExpandedQuery;
import org.zstack.header.query.ExpandedQueryAlias;
import org.zstack.header.query.ExpandedQueryAliases;
import org.zstack.header.search.Inventory;
import org.zstack.header.vm.VmNicVO;
import org.zstack.header.vm.VmNicVO_;
import org.zstack.header.vo.NoView;

import javax.persistence.*;
import java.io.Serializable;
import java.sql.Timestamp;
import java.util.*;
import java.util.stream.Collectors;

@Inventory(mappingVOClass = LoadBalancerServerGroupVO.class)
@ExpandedQueries({
        @ExpandedQuery(expandedField = "serverGroupRef", inventoryClass = LoadBalancerListenerServerGroupRefInventory.class,
                foreignKey = "uuid", expandedInventoryKey = "serverGroupUuid", hidden = true),
        @ExpandedQuery(expandedField = "vmNicRef", inventoryClass = LoadBalancerServerGroupVmNicRefInventory.class,
                foreignKey = "uuid", expandedInventoryKey = "serverGroupUuid", hidden = true),
})
@ExpandedQueryAliases({
        @ExpandedQueryAlias(alias = "vmNic", expandedField = "vmNicRef.vmNic"),
        @ExpandedQueryAlias(alias = "listener", expandedField = "serverGroupRef.listener"),
})

public class LoadBalancerServerGroupInventory implements Serializable {
    private String uuid;
    private String name;
    private String description;

    private String loadBalancerUuid;
    private Timestamp createDate;
    private Timestamp lastOpDate;
    private List<LoadBalancerListenerServerGroupRefInventory> listenerServerGroupRefs;
    private List<LoadBalancerServerGroupServerIpInventory> serverIps;
    private List<LoadBalancerServerGroupVmNicRefInventory> vmNicRefs;

    public LoadBalancerServerGroupInventory(LoadBalancerServerGroupVO vo) {
        this.uuid = vo.getUuid();
        this.setName(vo.getName());
        this.setUuid(vo.getUuid());
        this.setDescription(vo.getDescription());
        this.setLoadBalancerUuid(vo.getLoadBalancerUuid());
        this.setCreateDate(vo.getCreateDate());
        this.setLastOpDate(vo.getLastOpDate());
        this.setListenerServerGroupRefs(LoadBalancerListenerServerGroupRefInventory.valueOf(vo.getLoadBalancerListenerServerGroupRefs()));
        this.setVmNicRefs(LoadBalancerServerGroupVmNicRefInventory.valueOf(vo.getLoadBalancerServerGroupVmNicRefs()));
        this.setServerIps(LoadBalancerServerGroupServerIpInventory.valueOf(vo.getLoadBalancerServerGroupServerIps()));
    }

    public LoadBalancerServerGroupInventory() {

    }

    public static LoadBalancerServerGroupInventory valueOf(LoadBalancerServerGroupVO vo) {
        LoadBalancerServerGroupInventory inv = new LoadBalancerServerGroupInventory();
        inv.setName(vo.getName());
        inv.setUuid(vo.getUuid());
        inv.setDescription(vo.getDescription());
        inv.setLoadBalancerUuid(vo.getLoadBalancerUuid());
        inv.setCreateDate(vo.getCreateDate());
        inv.setLastOpDate(vo.getLastOpDate());
        inv.setListenerServerGroupRefs(LoadBalancerListenerServerGroupRefInventory.valueOf(vo.getLoadBalancerListenerServerGroupRefs()));
        inv.setVmNicRefs(LoadBalancerServerGroupVmNicRefInventory.valueOf(vo.getLoadBalancerServerGroupVmNicRefs()));
        inv.setServerIps(LoadBalancerServerGroupServerIpInventory.valueOf(vo.getLoadBalancerServerGroupServerIps()));
        return inv;
    }

    public static List<LoadBalancerServerGroupInventory> valueOf(Collection<LoadBalancerServerGroupVO> vos) {
        List<LoadBalancerServerGroupInventory> invs = new ArrayList<LoadBalancerServerGroupInventory>();
        for (LoadBalancerServerGroupVO vo : vos) {
            invs.add(valueOf(vo));
        }
        return invs;
    }


    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
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


    public List<LoadBalancerListenerServerGroupRefInventory> getListenerServerGroupRefs() {
        return listenerServerGroupRefs;
    }

    public void setListenerServerGroupRefs(List<LoadBalancerListenerServerGroupRefInventory> listenerServerGroupRefs) {
        this.listenerServerGroupRefs = listenerServerGroupRefs;
    }

    public List<LoadBalancerServerGroupServerIpInventory> getServerIps() {
        return serverIps;
    }

    public void setServerIps(List<LoadBalancerServerGroupServerIpInventory> serverIps) {
        this.serverIps = serverIps;
    }

    public List<LoadBalancerServerGroupVmNicRefInventory> getVmNicRefs() {
        return vmNicRefs;
    }

    public void setVmNicRefs(List<LoadBalancerServerGroupVmNicRefInventory> vmNicRefs) {
        this.vmNicRefs = vmNicRefs;
    }


    public String getLoadBalancerUuid() {
        return loadBalancerUuid;
    }

    public void setLoadBalancerUuid(String loadBalancerUuid) {
        this.loadBalancerUuid = loadBalancerUuid;
    }

    public List<String> getAttachedL3Uuids() {
        List<String> attachedVmNics = this.vmNicRefs.stream()
                .map(LoadBalancerServerGroupVmNicRefInventory::getVmNicUuid).collect(Collectors.toList());
        if (attachedVmNics.isEmpty()) {
            return new ArrayList<>();
        }

        List<String> l3Uuids = Q.New(VmNicVO.class).select(VmNicVO_.l3NetworkUuid)
                .in(VmNicVO_.uuid, attachedVmNics).listValues();
        return l3Uuids.stream().distinct().collect(Collectors.toList());
    }
}
