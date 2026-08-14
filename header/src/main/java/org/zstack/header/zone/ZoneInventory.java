package org.zstack.header.zone;

import org.zstack.header.cluster.ClusterInventory;
import org.zstack.header.configuration.PythonClassInventory;
import org.zstack.header.host.HostInventory;
import org.zstack.header.network.l2.L2NetworkInventory;
import org.zstack.header.network.l3.L3NetworkInventory;
import org.zstack.header.query.ExpandedQueries;
import org.zstack.header.query.ExpandedQuery;
import org.zstack.header.query.ExpandedQueryAlias;
import org.zstack.header.query.ExpandedQueryAliases;
import org.zstack.header.search.Inventory;
import org.zstack.header.storage.backup.BackupStorageZoneRefInventory;
import org.zstack.header.storage.primary.PrimaryStorageInventory;
import org.zstack.header.vm.VmInstanceInventory;

import java.io.Serializable;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * @inventory inventory for zone
 * @example {
 * "uuid": "b729da71b1c7412781d5de22229d5e17",
 * "name": "TestZone",
 * "description": "Test",
 * "state": "Enabled",
 * "type": "zstack",
 * "createDate": "Apr 25, 2014 6:04:52 PM",
 * "lastOpDate": "Apr 25, 2014 6:04:52 PM"
 * }
 * @since 0.1.0
 */

@Inventory(mappingVOClass = ZoneVO.class)
@PythonClassInventory
@ExpandedQueries({
        @ExpandedQuery(expandedField = "vmInstance", inventoryClass = VmInstanceInventory.class,
                foreignKey = "uuid", expandedInventoryKey = "zoneUuid"),
        @ExpandedQuery(expandedField = "cluster", inventoryClass = ClusterInventory.class,
                foreignKey = "uuid", expandedInventoryKey = "zoneUuid"),
        @ExpandedQuery(expandedField = "host", inventoryClass = HostInventory.class,
                foreignKey = "uuid", expandedInventoryKey = "zoneUuid"),
        @ExpandedQuery(expandedField = "primaryStorage", inventoryClass = PrimaryStorageInventory.class,
                foreignKey = "uuid", expandedInventoryKey = "zoneUuid"),
        @ExpandedQuery(expandedField = "l2Network", inventoryClass = L2NetworkInventory.class,
                foreignKey = "uuid", expandedInventoryKey = "zoneUuid"),
        @ExpandedQuery(expandedField = "l3Network", inventoryClass = L3NetworkInventory.class,
                foreignKey = "uuid", expandedInventoryKey = "zoneUuid"),
        @ExpandedQuery(expandedField = "backupStorageRef", inventoryClass = BackupStorageZoneRefInventory.class,
                foreignKey = "uuid", expandedInventoryKey = "zoneUuid", hidden = true),
})
@ExpandedQueryAliases({
        @ExpandedQueryAlias(alias = "backupStorage", expandedField = "backupStorageRef.backupStorage")
})
public class ZoneInventory implements Serializable {
    /**
     * @desc resource uuid
     */
    private String uuid;

    /**
     * @desc max length of 255 characters
     */
    private String name;

    /**
     * @desc max length of 2048 characters
     * @nullable
     */
    private String description;

    /**
     * @desc when state is Disabled, no vm can be created, unless descendant resource cluster/host has different state Enabled
     * @choices - Enabled
     * - Disabled
     */
    private String state;

    /**
     * @desc for now, the only zone type is 'zstack'
     */
    private String type;

    /**
     * @desc true if this zone is the default zone
     */
    private boolean isDefault;

    /**
     * @desc the time this resource gets created
     */
    private Timestamp createDate;

    /**
     * @desc last time this resource gets operated
     */
    private Timestamp lastOpDate;

    public static ZoneInventory valueOf(ZoneVO vo) {
        ZoneInventory inv = new ZoneInventory();
        inv.setName(vo.getName());
        inv.setDescription(vo.getDescription());
        inv.setUuid(vo.getUuid());
        inv.setState(vo.getState().toString());
        inv.setType(vo.getType());
        inv.setDefault(vo.isDefault());
        inv.setCreateDate(vo.getCreateDate());
        inv.setLastOpDate(vo.getLastOpDate());
        return inv;
    }

    public static List<ZoneInventory> valueOf(Collection<ZoneVO> vos) {
        List<ZoneInventory> invs = new ArrayList<ZoneInventory>(vos.size());
        for (ZoneVO vo : vos) {
            invs.add(ZoneInventory.valueOf(vo));
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

    public boolean isDefault() {
        return isDefault;
    }

    public void setDefault(boolean aDefault) {
        isDefault = aDefault;
    }
}
