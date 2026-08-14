package org.zstack.network.service.lb;

import org.zstack.header.query.ExpandedQueries;
import org.zstack.header.query.ExpandedQuery;
import org.zstack.header.query.ExpandedQueryAlias;
import org.zstack.header.query.ExpandedQueryAliases;
import org.zstack.header.search.Inventory;
import org.zstack.network.service.vip.VipInventory;

import java.io.Serializable;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Created by frank on 8/8/2015.
 */
@Inventory(mappingVOClass = LoadBalancerVO.class)
@ExpandedQueries({
        @ExpandedQuery(expandedField = "listeners", inventoryClass = LoadBalancerListenerInventory.class,
                foreignKey = "uuid", expandedInventoryKey = "loadBalancerUuid"),
        @ExpandedQuery(expandedField = "vip", inventoryClass = VipInventory.class,
                foreignKey = "vipUuid", expandedInventoryKey = "uuid"),
        @ExpandedQuery(target = VipInventory.class, expandedField = "loadBalancer",
                inventoryClass = LoadBalancerInventory.class, foreignKey = "uuid", expandedInventoryKey = "vipUuid")
})
public class LoadBalancerInventory implements Serializable {
    private String name;
    private String uuid;
    private String description;
    private String serverGroupUuid;
    private String state;
    private String type;
    private String vipUuid;
    private Timestamp createDate;
    private Timestamp lastOpDate;
    private List<LoadBalancerListenerInventory> listeners;

    public LoadBalancerInventory(LoadBalancerVO vo) {
        this.setUuid(vo.getUuid());
        this.setName(vo.getName());
        this.setDescription(vo.getDescription());
        this.setState(vo.getState().toString());
        this.setVipUuid(vo.getVipUuid());
        this.setServerGroupUuid(vo.getServerGroupUuid());
        this.setType(vo.getType().toString());
        this.setCreateDate(vo.getCreateDate());
        this.setLastOpDate(vo.getLastOpDate());
        this.setListeners(LoadBalancerListenerInventory.valueOf(vo.getListeners()));
    }

    public LoadBalancerInventory() {
    }

    public static LoadBalancerInventory valueOf(LoadBalancerVO vo) {
        LoadBalancerInventory inv = new LoadBalancerInventory(vo);
        return inv;
    }

    public static List<LoadBalancerInventory> valueOf(Collection<LoadBalancerVO> vos) {
        List<LoadBalancerInventory> invs = new ArrayList<LoadBalancerInventory>();
        for (LoadBalancerVO vo : vos) {
            invs.add(valueOf(vo));
        }
        return invs;
    }

    public String getVipUuid() {
        return vipUuid;
    }

    public void setVipUuid(String vipUuid) {
        this.vipUuid = vipUuid;
    }

    public List<LoadBalancerListenerInventory> getListeners() {
        return listeners;
    }

    public void setListeners(List<LoadBalancerListenerInventory> listeners) {
        this.listeners = listeners;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
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

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getServerGroupUuid() {
        return serverGroupUuid;
    }

    public void setServerGroupUuid(String serverGroupUuid) {
        this.serverGroupUuid = serverGroupUuid;
    }
}
