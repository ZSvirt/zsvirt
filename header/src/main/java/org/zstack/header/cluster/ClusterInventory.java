package org.zstack.header.cluster;

import org.zstack.header.configuration.PythonClassInventory;
import org.zstack.header.host.HostInventory;
import org.zstack.header.network.l2.L2NetworkClusterRefInventory;
import org.zstack.header.query.ExpandedQueries;
import org.zstack.header.query.ExpandedQuery;
import org.zstack.header.query.ExpandedQueryAlias;
import org.zstack.header.query.ExpandedQueryAliases;
import org.zstack.header.search.Inventory;
import org.zstack.header.storage.primary.PrimaryStorageClusterRefInventory;
import org.zstack.header.vm.VmInstanceInventory;
import org.zstack.header.zone.ZoneInventory;

import java.io.Serializable;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * @inventory inventory for cluster
 * @example {
 * "inventory": {
 * "createDate": "Apr 28, 2014 5:54:09 PM",
 * "description": "test",
 * "hypervisorType": "KVM",
 * "lastOpDate": "Apr 28, 2014 5:54:09 PM",
 * "name": "cluster1",
 * "state": "Enabled",
 * "type": "zstack",
 * "uuid": "c1bd173d5cd84f0e9e7c47195ae27ec6",
 * "zoneUuid": "1b830f5bd1cb469b821b4b77babfdd6f"
 * }
 * }
 * @since 0.1.0
 */
@Inventory(mappingVOClass = ClusterVO.class)
@PythonClassInventory
@ExpandedQueries({
        @ExpandedQuery(expandedField = "zone", inventoryClass = ZoneInventory.class,
                foreignKey = "zoneUuid", expandedInventoryKey = "uuid"),
        @ExpandedQuery(expandedField = "host", inventoryClass = HostInventory.class,
                foreignKey = "uuid", expandedInventoryKey = "clusterUuid"),
        @ExpandedQuery(expandedField = "vmInstance", inventoryClass = VmInstanceInventory.class,
                foreignKey = "uuid", expandedInventoryKey = "clusterUuid"),
        @ExpandedQuery(expandedField = "l2NetworkRef", inventoryClass = L2NetworkClusterRefInventory.class,
                foreignKey = "uuid", expandedInventoryKey = "clusterUuid", hidden = true),
        @ExpandedQuery(expandedField = "l2VlanNetworkRef", inventoryClass = L2NetworkClusterRefInventory.class,
                foreignKey = "uuid", expandedInventoryKey = "clusterUuid", hidden = true),
        @ExpandedQuery(expandedField = "primaryStorageRef", inventoryClass = PrimaryStorageClusterRefInventory.class,
                foreignKey = "uuid", expandedInventoryKey = "clusterUuid", hidden = true),
})
@ExpandedQueryAliases({
        @ExpandedQueryAlias(alias = "l2Network", expandedField = "l2NetworkRef.l2Network"),
        @ExpandedQueryAlias(alias = "l2VlanNetwork", expandedField = "l2VlanNetworkRef.l2Network"),
        @ExpandedQueryAlias(alias = "primaryStorage", expandedField = "primaryStorageRef.primaryStorage"),
})
public class ClusterInventory implements Serializable {
    /**
     * @desc max length of 255 characters
     */
    private String name;
    /**
     * @desc cluster uuid
     */
    private String uuid;
    /**
     * @desc max length of 2048 characters
     * @nullable
     */
    private String description;
    /**
     * @desc when state is Disabled, no vm can be created, unless child resource host has different state Enabled
     * @choices - Enabled
     * - Disabled
     */
    private String state;
    /**
     * @desc hypervisor type of hosts in cluster. All hosts in the same cluster must have the same hypervisor type.
     * Now only kvm has been supported
     * @choices KVM
     * - Simulator
     * - baremetal
     * - baremetal2
     */
    private String hypervisorType;
    /**
     * @desc the time this resource gets created
     */
    private Timestamp createDate;
    /**
     * @desc last time this resource gets operated
     */
    private Timestamp lastOpDate;
    /**
     * @desc zone uuid this cluster belongs to. For zone (see :ref:`ZoneInventory`)
     */
    private String zoneUuid;
    /**
     * @desc for now, the only types are 'zstack' 'baremetal' and 'baremetal2'. This field is reserved for future extension
     */
    private String type;

    private String architecture;

    public static ClusterInventory valueOf(ClusterVO vo) {
        ClusterInventory inv = new ClusterInventory();
        inv.setName(vo.getName());
        inv.setDescription(vo.getDescription());
        inv.setUuid(vo.getUuid());
        inv.setState(vo.getState().toString());
        inv.setCreateDate(vo.getCreateDate());
        inv.setHypervisorType(vo.getHypervisorType());
        inv.setZoneUuid(vo.getZoneUuid());
        inv.setType(vo.getType());
        inv.setLastOpDate(vo.getLastOpDate());
        inv.setArchitecture(vo.getArchitecture());
        return inv;
    }

    public static List<ClusterInventory> valueOf(Collection<ClusterVO> vos) {
        List<ClusterInventory> invs = new ArrayList<ClusterInventory>(vos.size());
        for (ClusterVO vo : vos) {
            invs.add(ClusterInventory.valueOf(vo));
        }
        return invs;
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

    public String getHypervisorType() {
        return hypervisorType;
    }

    public void setHypervisorType(String hypervisorType) {
        this.hypervisorType = hypervisorType;
    }

    public String getZoneUuid() {
        return zoneUuid;
    }

    public void setZoneUuid(String zoneUuid) {
        this.zoneUuid = zoneUuid;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getArchitecture() {
        return architecture;
    }

    public void setArchitecture(String architecture) {
        this.architecture = architecture;
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
