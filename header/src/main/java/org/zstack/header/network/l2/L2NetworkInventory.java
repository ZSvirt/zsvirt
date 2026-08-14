package org.zstack.header.network.l2;

import org.zstack.header.configuration.PythonClassInventory;
import org.zstack.header.network.l3.L3NetworkInventory;
import org.zstack.header.query.*;
import org.zstack.header.search.Inventory;
import org.zstack.header.search.TypeField;
import org.zstack.header.zone.ZoneInventory;

import javax.persistence.JoinColumn;
import java.io.Serializable;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * @inventory inventory for l2Network
 * @category l2network
 * @example {
 * "inventory": {
 * "uuid": "f685ff94513542bbb8e814027f8deb13",
 * "name": "l2-basic",
 * "description": "Basic L2 Test",
 * "zoneUuid": "45a2864b6ddf4d2fb9b4c3736a923dcb",
 * "physicalInterface": "eth0",
 * "type": "L2NoVlanNetwork",
 * "createDate": "May 5, 2014 12:58:35 PM",
 * "lastOpDate": "May 5, 2014 12:58:35 PM",
 * "attachedClusterUuids": []
 * }
 * }
 * @since 0.1.0
 */
@Inventory(mappingVOClass = L2NetworkVO.class)
@PythonClassInventory
@ExpandedQueries({
        @ExpandedQuery(expandedField = "zone", inventoryClass = ZoneInventory.class,
                foreignKey = "zoneUuid", expandedInventoryKey = "uuid"),
        @ExpandedQuery(expandedField = "l3Network", inventoryClass = L3NetworkInventory.class,
                foreignKey = "uuid", expandedInventoryKey = "l2NetworkUuid"),
        @ExpandedQuery(expandedField = "clusterRef", inventoryClass = L2NetworkClusterRefInventory.class,
                foreignKey = "uuid", expandedInventoryKey = "l2NetworkUuid", hidden = true),
        @ExpandedQuery(expandedField = "hostRef", inventoryClass = L2NetworkHostRefInventory.class,
                foreignKey = "uuid", expandedInventoryKey = "l2NetworkUuid", hidden = true),
})
@ExpandedQueryAliases({
        @ExpandedQueryAlias(alias = "cluster", expandedField = "clusterRef.cluster"),
        @ExpandedQueryAlias(alias = "host", expandedField = "hostRef.host")
})
public class L2NetworkInventory implements Serializable {
    /**
     * @desc l2Network uuid
     */
    private String uuid;
    /**
     * @desc max length of 255 characters
     */
    private String name;
    /**
     * @desc max length of 2048 characters
     */
    private String description;
    /**
     * @desc uuid of zone this l2Network belongs to. See :ref:`ZoneInventory`
     */
    private String zoneUuid;
    /**
     * @desc physical interface name. Depending on l2Network type, this field may have various meanings. For example,
     * for L2NoVlanNetwork/L2VlanNetwork, it's ethernet device name on hypervisor(e.g. eth0)
     */
    private String physicalInterface;
    /**
     * @desc l2Network type
     */
    @TypeField
    private String type;
    /**
     * @desc l2Network vSwitch Type
     */
    private String vSwitchType;
    /**
     * @desc virtual id for vlan/vni
     */
    private Integer virtualNetworkId;
    /**
     * @desc the time this resource gets created
     */
    private Timestamp createDate;
    /**
     * @desc last time this resource gets operated
     */
    private Timestamp lastOpDate;
    /**
     * @desc a list of cluster uuid that the l2Network has attached to
     */
    @Queryable(mappingClass = L2NetworkClusterRefInventory.class,
            joinColumn = @JoinColumn(name = "l2NetworkUuid", referencedColumnName = "clusterUuid"))
    private List<String> attachedClusterUuids;

    private List<L2NetworkHostRefInventory> attachedHostRefs;

    public L2NetworkInventory() {
    }

    protected L2NetworkInventory(L2NetworkVO vo) {
        this.setUuid(vo.getUuid());
        this.setZoneUuid(vo.getZoneUuid());
        this.setType(vo.getType());
        this.setvSwitchType(vo.getvSwitchType());
        this.setVirtualNetworkId(vo.getVirtualNetworkId());
        this.setDescription(vo.getDescription());
        this.setName(vo.getName());
        this.setPhysicalInterface(vo.getPhysicalInterface());
        this.setCreateDate(vo.getCreateDate());
        this.setLastOpDate(vo.getLastOpDate());
        this.attachedClusterUuids = new ArrayList<String>(vo.getAttachedClusterRefs().size());
        for (L2NetworkClusterRefVO ref : vo.getAttachedClusterRefs()) {
            this.attachedClusterUuids.add(ref.getClusterUuid());
        }
        this.setAttachedHostRefs(L2NetworkHostRefInventory.valueOf(vo.getAttachedHostRefs()));
    }

    public static L2NetworkInventory valueOf(L2NetworkVO vo) {
        return new L2NetworkInventory(vo);
    }

    public static List<L2NetworkInventory> valueOf(Collection<L2NetworkVO> vos) {
        List<L2NetworkInventory> invs = new ArrayList<L2NetworkInventory>(vos.size());
        for (L2NetworkVO vo : vos) {
            invs.add(L2NetworkInventory.valueOf(vo));
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

    public String getZoneUuid() {
        return zoneUuid;
    }

    public void setZoneUuid(String zoneUuid) {
        this.zoneUuid = zoneUuid;
    }

    public String getPhysicalInterface() {
        return physicalInterface;
    }

    public void setPhysicalInterface(String physicalInterface) {
        this.physicalInterface = physicalInterface;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getvSwitchType() {
        return vSwitchType;
    }

    public void setvSwitchType(String vSwitchType) {
        this.vSwitchType = vSwitchType;
    }

    public Integer getVirtualNetworkId() {
        return virtualNetworkId;
    }

    public void setVirtualNetworkId(Integer virtualNetworkId) {
        this.virtualNetworkId = virtualNetworkId;
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

    public List<String> getAttachedClusterUuids() {
        return attachedClusterUuids;
    }

    public void setAttachedClusterUuids(List<String> attachedClusterUuids) {
        this.attachedClusterUuids = attachedClusterUuids;
    }

    public List<L2NetworkHostRefInventory> getAttachedHostRefs() {
        return attachedHostRefs;
    }

    public void setAttachedHostRefs(List<L2NetworkHostRefInventory> attachedHostRefs) {
        this.attachedHostRefs = attachedHostRefs;
    }
}
