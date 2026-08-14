package org.zstack.header.storage.backup;

import org.zstack.header.query.ExpandedQueries;
import org.zstack.header.query.ExpandedQuery;
import org.zstack.header.search.Inventory;
import org.zstack.header.zone.ZoneInventory;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@Inventory(mappingVOClass = BackupStorageZoneRefVO.class)
@ExpandedQueries({
        @ExpandedQuery(expandedField = "zone", inventoryClass = ZoneInventory.class,
                foreignKey = "zoneUuid", expandedInventoryKey = "uuid"),
        @ExpandedQuery(expandedField = "backupStorage", inventoryClass = BackupStorageInventory.class,
                foreignKey = "backupStorageUuid", expandedInventoryKey = "uuid"),
})
public class BackupStorageZoneRefInventory {
    private Long id;
    private String backupStorageUuid;
    private String zoneUuid;
    private Timestamp createDate;
    private Timestamp lastOpDate;

    public static BackupStorageZoneRefInventory valueOf(BackupStorageZoneRefVO vo) {
        BackupStorageZoneRefInventory inv = new BackupStorageZoneRefInventory();
        inv.setBackupStorageUuid(vo.getBackupStorageUuid());
        inv.setCreateDate(vo.getCreateDate());
        inv.setId(vo.getId());
        inv.setLastOpDate(vo.getLastOpDate());
        inv.setZoneUuid(vo.getZoneUuid());
        return inv;
    }

    public static List<BackupStorageZoneRefInventory> valueOf(Collection<BackupStorageZoneRefVO> vos) {
        List<BackupStorageZoneRefInventory> invs = new ArrayList<BackupStorageZoneRefInventory>();
        for (BackupStorageZoneRefVO vo : vos) {
            invs.add(valueOf(vo));
        }
        return invs;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getBackupStorageUuid() {
        return backupStorageUuid;
    }

    public void setBackupStorageUuid(String backupStorageUuid) {
        this.backupStorageUuid = backupStorageUuid;
    }

    public String getZoneUuid() {
        return zoneUuid;
    }

    public void setZoneUuid(String zoneUuid) {
        this.zoneUuid = zoneUuid;
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
