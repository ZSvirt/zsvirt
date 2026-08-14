package org.zstack.header.storage.primary;

import org.zstack.header.configuration.PythonClassInventory;
import org.zstack.header.log.NoLogging;
import org.zstack.header.query.*;
import org.zstack.header.search.Inventory;
import org.zstack.header.search.TypeField;
import org.zstack.header.storage.snapshot.VolumeSnapshotInventory;
import org.zstack.header.volume.VolumeInventory;
import org.zstack.header.zone.ZoneInventory;

import javax.persistence.JoinColumn;
import java.io.Serializable;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * @inventory inventory for primary storage
 * @example {
 * "inventory": {
 * "uuid": "f4ac0a3119c94c6fae844c2298615d27",
 * "zoneUuid": "f04caf351c014aa890126fc78193d063",
 * "name": "SimulatorPrimaryStorage-0",
 * "url": "nfs://simulator/primary/-0",
 * "description": "Test Primary Storage",
 * "totalCapacity": 10995116277760,
 * "availableCapacity": 10995116277760,
 * "type": "SimulatorPrimaryStorage",
 * "state": "Enabled",
 * "mountPath": "/primarystoragesimulator/f4ac0a3119c94c6fae844c2298615d27",
 * "createDate": "May 1, 2014 2:42:51 PM",
 * "lastOpDate": "May 1, 2014 2:42:51 PM",
 * "attachedClusterUuids": [
 * "f23e402bc53b4b5abae87273b6004016",
 * "4a1789235a86409a9a6db83f97bc582f",
 * "fe755538d4e845d5b82073e4f80cb90b",
 * "1f45d6d6c02b43bfb6196dcacb5b8a25"
 * ]
 * }
 * }
 * @since 0.1.0
 */
@Inventory(mappingVOClass = PrimaryStorageVO.class)
@PythonClassInventory
@ExpandedQueries({
        @ExpandedQuery(expandedField = "zone", inventoryClass = ZoneInventory.class,
                foreignKey = "zoneUuid", expandedInventoryKey = "uuid"),
        @ExpandedQuery(expandedField = "volume", inventoryClass = VolumeInventory.class,
                foreignKey = "uuid", expandedInventoryKey = "primaryStorageUuid"),
        @ExpandedQuery(expandedField = "volumeSnapshot", inventoryClass = VolumeSnapshotInventory.class,
                foreignKey = "uuid", expandedInventoryKey = "primaryStorageUuid"),
        @ExpandedQuery(expandedField = "clusterRef", inventoryClass = PrimaryStorageClusterRefInventory.class,
                foreignKey = "uuid", expandedInventoryKey = "primaryStorageUuid", hidden = true),
})
@ExpandedQueryAliases({
        @ExpandedQueryAlias(alias = "cluster", expandedField = "clusterRef.cluster")
})
public class PrimaryStorageInventory implements Serializable {
    /**
     * @desc primary storage uuid
     */
    private String uuid;
    /**
     * @desc uuid of zone this primary storage is in
     */
    private String zoneUuid;
    /**
     * @desc max length of 255 characters
     */
    private String name;
    /**
     * @desc depending on primary storage type, url may have various formats. For example,
     * nfs primary storage uses url as *server_ip:/share_path*
     */
    @NoLogging(type = NoLogging.Type.Uri)
    private String url;
    /**
     * @desc max length of 2048 characters
     * @nullable
     */
    private String description;
    /**
     * @desc total capacity in bytes
     */
    @Queryable(mappingClass = PrimaryStorageCapacityInventory.class,
            joinColumn = @JoinColumn(name = "uuid", referencedColumnName = "totalCapacity"))
    private Long totalCapacity;
    /**
     * @desc available capacity in bytes
     */
    @Queryable(mappingClass = PrimaryStorageCapacityInventory.class,
            joinColumn = @JoinColumn(name = "uuid", referencedColumnName = "availableCapacity"))
    private Long availableCapacity;

    @Queryable(mappingClass = PrimaryStorageCapacityInventory.class,
            joinColumn = @JoinColumn(name = "uuid", referencedColumnName = "totalPhysicalCapacity"))
    private Long totalPhysicalCapacity;

    @Queryable(mappingClass = PrimaryStorageCapacityInventory.class,
            joinColumn = @JoinColumn(name = "uuid", referencedColumnName = "availablePhysicalCapacity"))
    private Long availablePhysicalCapacity;

    @Queryable(mappingClass = PrimaryStorageCapacityInventory.class,
            joinColumn = @JoinColumn(name = "uuid", referencedColumnName = "systemUsedCapacity"))
    private Long systemUsedCapacity;

    /**
     * @desc primary storage type
     */
    @TypeField
    private String type;
    /**
     * @desc - Enabled: volume can be created on this primary storage
     * - Disabled: volume can NOT be created on this primary storage
     * @choices - Enabled
     * - Disabled
     */
    private String state;
    /**
     * @desc - Connecting: connection is being established between zstack and primary storage, no volume can be created
     * - Connected: primary storage is functional
     * - Disconnected: primary storage is out of order, no volume can be created
     * @choices - Connecting
     * - Connected
     * - Disconnected
     */
    private String status;

    /**
     * @desc depending on primary storage type, mountPath can have various meanings.
     * For example, for nfs primary storage mountPath is hypervisor filesystem path where remote share
     * was mounted
     */
    private String mountPath;
    /**
     * @desc the time this resource gets created
     */
    private Timestamp createDate;
    /**
     * @desc last time this resource gets operated
     */
    private Timestamp lastOpDate;
    /**
     * @desc a list of cluster uuid this primary storage has attached to
     */
    @Queryable(mappingClass = PrimaryStorageClusterRefInventory.class,
            joinColumn = @JoinColumn(name = "primaryStorageUuid", referencedColumnName = "clusterUuid"))
    private List<String> attachedClusterUuids;

    public PrimaryStorageInventory() {
    }

    protected PrimaryStorageInventory(PrimaryStorageVO vo) {
        setZoneUuid(vo.getZoneUuid());
        setCreateDate(vo.getCreateDate());
        setDescription(vo.getDescription());
        setLastOpDate(vo.getLastOpDate());
        setName(vo.getName());
        setState(vo.getState().toString());
        setType(vo.getType());
        setUrl(vo.getUrl());
        setUuid(vo.getUuid());
        setMountPath(vo.getMountPath());
        setStatus(vo.getStatus().toString());
        attachedClusterUuids = new ArrayList<String>(vo.getAttachedClusterRefs().size());
        for (PrimaryStorageClusterRefVO ref : vo.getAttachedClusterRefs()) {
            attachedClusterUuids.add(ref.getClusterUuid());
        }

        if (vo.getCapacity() != null) {
            setTotalCapacity(vo.getCapacity().getTotalCapacity());
            setAvailableCapacity(vo.getCapacity().getAvailableCapacity());
            setTotalPhysicalCapacity(vo.getCapacity().getTotalPhysicalCapacity());
            setAvailablePhysicalCapacity(vo.getCapacity().getAvailablePhysicalCapacity());
            setSystemUsedCapacity(vo.getCapacity().getSystemUsedCapacity());
        }
    }

    public Long getSystemUsedCapacity() {
        return systemUsedCapacity;
    }

    public void setSystemUsedCapacity(Long systemUsedCapacity) {
        this.systemUsedCapacity = systemUsedCapacity;
    }

    public static PrimaryStorageInventory valueOf(PrimaryStorageVO vo) {
        return new PrimaryStorageInventory(vo);
    }

    public static List<PrimaryStorageInventory> valueOf(Collection<PrimaryStorageVO> vos) {
        List<PrimaryStorageInventory> invs = new ArrayList<PrimaryStorageInventory>(vos.size());
        for (PrimaryStorageVO vo : vos) {
            invs.add(PrimaryStorageInventory.valueOf(vo));
        }
        return invs;
    }

    public Long getTotalPhysicalCapacity() {
        return totalPhysicalCapacity;
    }

    public void setTotalPhysicalCapacity(Long totalPhysicalCapacity) {
        this.totalPhysicalCapacity = totalPhysicalCapacity;
    }

    public Long getAvailablePhysicalCapacity() {
        return availablePhysicalCapacity;
    }

    public void setAvailablePhysicalCapacity(Long availablePhysicalCapacity) {
        this.availablePhysicalCapacity = availablePhysicalCapacity;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public long getAvailableCapacity() {
        return availableCapacity;
    }

    public void setAvailableCapacity(long availableCapacity) {
        this.availableCapacity = availableCapacity;
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

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public long getTotalCapacity() {
        return totalCapacity;
    }

    public void setTotalCapacity(long totalCapacity) {
        this.totalCapacity = totalCapacity;
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

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public List<String> getAttachedClusterUuids() {
        return attachedClusterUuids;
    }

    public void setAttachedClusterUuids(List<String> attachedClusterUuids) {
        this.attachedClusterUuids = attachedClusterUuids;
    }

    public String getMountPath() {
        return mountPath;
    }

    public void setMountPath(String mountPath) {
        this.mountPath = mountPath;
    }

    public String getZoneUuid() {
        return zoneUuid;
    }

    public void setZoneUuid(String zoneUuid) {
        this.zoneUuid = zoneUuid;
    }
}
