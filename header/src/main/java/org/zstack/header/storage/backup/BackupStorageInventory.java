package org.zstack.header.storage.backup;

import org.zstack.header.configuration.PythonClassInventory;
import org.zstack.header.image.ImageBackupStorageRefInventory;
import org.zstack.header.query.*;
import org.zstack.header.search.Inventory;
import org.zstack.header.search.TypeField;
import org.zstack.header.storage.snapshot.VolumeSnapshotBackupStorageRefInventory;

import javax.persistence.JoinColumn;
import java.io.Serializable;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * @inventory inventory for backup storage
 * @example {
 * "inventory": {
 * "uuid": "18421b64c18c458a8f362203c73593e1",
 * "name": "SimulatoryBackupStorage-0",
 * "url": "nfs://simulator/backupstorage/-0",
 * "totalCapacity": 107374182400,
 * "availableCapacity": 107374182400,
 * "type": "SimulatorBackupStorage",
 * "state": "Enabled",
 * "status": "Connected",
 * "createDate": "May 2, 2014 12:23:20 AM",
 * "lastOpDate": "May 2, 2014 12:23:20 AM",
 * "attachedZoneUuids": []
 * }
 * }
 * @since 0.1.0
 */
@Inventory(mappingVOClass = BackupStorageVO.class)
@PythonClassInventory
@ExpandedQueries({
        @ExpandedQuery(expandedField = "zoneRef", inventoryClass = BackupStorageZoneRefInventory.class,
                foreignKey = "uuid", expandedInventoryKey = "backupStorageUuid", hidden = true),
        @ExpandedQuery(expandedField = "imageRef", inventoryClass = ImageBackupStorageRefInventory.class,
                foreignKey = "uuid", expandedInventoryKey = "backupStorageUuid", hidden = true),
        @ExpandedQuery(expandedField = "volumeSnapshotRef", inventoryClass = VolumeSnapshotBackupStorageRefInventory.class,
                foreignKey = "uuid", expandedInventoryKey = "backupStorageUuid", hidden = true),
})
@ExpandedQueryAliases({
        @ExpandedQueryAlias(alias = "zone", expandedField = "zoneRef.zone"),
        @ExpandedQueryAlias(alias = "image", expandedField = "imageRef.image"),
        @ExpandedQueryAlias(alias = "volumeSnapshot", expandedField = "volumeSnapshotRef.volumeSnapshot"),
})
public class BackupStorageInventory implements Serializable {
    /**
     * @desc backup storage uuid
     */
    private String uuid;
    /**
     * @desc max length of 255 characters
     */
    private String name;
    /**
     * @desc depending on backup storage type, ulr may have various meanings. For example,
     * for Sftp backup storage it means path to folder on filesystem that is used to
     * store images/volumes/snapshots
     */
    private String url;
    /**
     * @desc max length of 2048 characters
     * @nullable
     */
    private String description;
    /**
     * @desc total capacity in bytes
     */
    private Long totalCapacity;
    /**
     * @desc available capacity in bytes
     */
    private Long availableCapacity;
    /**
     * @desc backup storage type
     */
    @TypeField
    private String type;
    /**
     * @desc - Enabled: images/volumes/snapshots can be downloaded/uploaded
     * - Disabled: NO images/volumes/snapshots can be downloaded/uploaded
     * @choices - Enabled
     * - Disabled
     */
    private String state;
    /**
     * @desc - Connecting: connection is being established between backup storage and zstack. NO images/volumes/snapshots can be downloaded/uploaded
     * - Connected: connection is established. images/volumes/snapshots can be downloaded/uploaded
     * - Disconnected: connection is broken. NO images/volumes/snapshots can be downloaded/uploaded
     * <p>
     * depending on backup storage type, the connection may have various meanings. For example, for Sftp backup storage connection is the http
     * channel between zstack and sftp backup storage agent
     */
	private String status;


    /**
     * @desc the time this resource gets created
     */
    private Timestamp createDate;
    /**
     * @desc last time this resource gets operated
     */
    private Timestamp lastOpDate;
    /**
     * @desc a list of zone uuid this backup storage has attached to. See :ref:`ZoneInventory`
     */
    @Queryable(mappingClass = BackupStorageZoneRefInventory.class,
            joinColumn = @JoinColumn(name = "backupStorageUuid", referencedColumnName = "zoneUuid"))
    private List<String> attachedZoneUuids;

    protected BackupStorageInventory(BackupStorageVO vo) {
        this.setCreateDate(vo.getCreateDate());
        this.setDescription(vo.getDescription());
        this.setLastOpDate(vo.getLastOpDate());
        this.setName(vo.getName());
        this.setState(vo.getState().toString());
        this.setStatus(vo.getStatus().toString());
        this.setTotalCapacity(vo.getTotalCapacity());
        this.setAvailableCapacity(vo.getAvailableCapacity());
        this.setType(vo.getType());
        this.setUrl(vo.getUrl());
        this.setUuid(vo.getUuid());
        this.attachedZoneUuids = new ArrayList<String>(vo.getAttachedZoneRefs().size());
        for (BackupStorageZoneRefVO ref : vo.getAttachedZoneRefs()) {
            if (!this.attachedZoneUuids.contains(ref.getZoneUuid())) {
                this.attachedZoneUuids.add(ref.getZoneUuid());
            }
        }
    }

    public BackupStorageInventory() {
    }

    public static BackupStorageInventory valueOf(BackupStorageVO vo) {
        BackupStorageInventory inv = new BackupStorageInventory(vo);
        return inv;
    }

    public static List<BackupStorageInventory> valueOf(Collection<BackupStorageVO> vos) {
        List<BackupStorageInventory> invs = new ArrayList<BackupStorageInventory>(vos.size());
        for (BackupStorageVO vo : vos) {
            invs.add(BackupStorageInventory.valueOf(vo));
        }
        return invs;
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

    public List<String> getAttachedZoneUuids() {
        return attachedZoneUuids;
    }

    public void setAttachedZoneUuids(List<String> attachedZoneUuids) {
        this.attachedZoneUuids = attachedZoneUuids;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

}
