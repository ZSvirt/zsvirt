package org.zstack.network.service.lb;

import org.zstack.header.query.ExpandedQueries;
import org.zstack.header.query.ExpandedQuery;
import org.zstack.header.query.ExpandedQueryAlias;
import org.zstack.header.query.ExpandedQueryAliases;
import org.zstack.header.search.Inventory;
import org.zstack.header.acl.AccessControlListInventory;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * @author: zhanyong.miao
 * @date: 2020-03-11
 **/
@Inventory(mappingVOClass = LoadBalancerListenerACLRefVO.class)
@ExpandedQueries({
        @ExpandedQuery(expandedField = "acl", inventoryClass = AccessControlListInventory.class,
                foreignKey = "aclUuid", expandedInventoryKey = "uuid"),
        @ExpandedQuery(expandedField = "listener", inventoryClass = LoadBalancerListenerInventory.class,
                foreignKey = "listenerUuid", expandedInventoryKey = "uuid"),
        @ExpandedQuery(target = AccessControlListInventory.class, expandedField = "loadBalancerListenerRef",
                inventoryClass = LoadBalancerListenerACLRefInventory.class, foreignKey = "uuid", expandedInventoryKey = "aclUuid", hidden = true)
})
@ExpandedQueryAliases({
        @ExpandedQueryAlias(target = AccessControlListInventory.class, alias = "loadBalancerListener", expandedField = "loadBalancerListenerRef.listener")
})
public class LoadBalancerListenerACLRefInventory {
    private Long id;
    private String listenerUuid;
    private String serverGroupUuid;
    private String aclUuid;
    private String type;
    private Timestamp createDate;
    private Timestamp lastOpDate;

    public static List<LoadBalancerListenerACLRefInventory> valueOf(Collection<LoadBalancerListenerACLRefVO> vos) {
        List<LoadBalancerListenerACLRefInventory> invs = new ArrayList<>();
        for (LoadBalancerListenerACLRefVO vo : vos ) {
            invs.add(valueOf(vo));
        }
        return invs;
    }

    public static LoadBalancerListenerACLRefInventory valueOf(LoadBalancerListenerACLRefVO vo) {
        LoadBalancerListenerACLRefInventory inv = new LoadBalancerListenerACLRefInventory();
        inv.setType(vo.getType().toString());
        inv.setAclUuid(vo.getAclUuid());
        inv.setId(vo.getId());
        inv.setListenerUuid(vo.getListenerUuid());
        inv.setServerGroupUuid(vo.getServerGroupUuid());
        inv.setCreateDate(vo.getCreateDate());
        inv.setLastOpDate(vo.getLastOpDate());

        return inv;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
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

    public String getAclUuid() {
        return aclUuid;
    }

    public void setAclUuid(String aclUuid) {
        this.aclUuid = aclUuid;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
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
