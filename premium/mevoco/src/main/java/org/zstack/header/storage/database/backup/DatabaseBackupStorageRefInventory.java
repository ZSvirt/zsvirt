package org.zstack.header.storage.database.backup;

import org.zstack.header.query.ExpandedQueries;
import org.zstack.header.query.ExpandedQuery;
import org.zstack.header.rest.APINoSee;
import org.zstack.header.rest.SDK;
import org.zstack.header.search.Inventory;
import org.zstack.header.storage.backup.BackupStorageInventory;

import java.io.Serializable;
import java.sql.Timestamp;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

@Inventory(mappingVOClass = DatabaseBackupStorageRefVO.class)
@ExpandedQueries({
        @ExpandedQuery(expandedField = "databaseBackup", inventoryClass = DatabaseBackupInventory.class,
                foreignKey = "databaseBackupUuid", expandedInventoryKey = "uuid"),
        @ExpandedQuery(expandedField = "backupStorage", inventoryClass = BackupStorageInventory.class,
                foreignKey = "backupStorageUuid", expandedInventoryKey = "uuid"),
})
@SDK
public class DatabaseBackupStorageRefInventory implements Serializable {
    @APINoSee
    private long id;
    private String databaseBackupUuid;
    private String backupStorageUuid;
    private String installPath;
    private String exportUrl;
    private String status;
    private Timestamp createDate;
    private Timestamp lastOpDate;

    public static DatabaseBackupStorageRefInventory valueOf(DatabaseBackupStorageRefVO vo) {
        DatabaseBackupStorageRefInventory inv = new DatabaseBackupStorageRefInventory();
        inv.setId(vo.getId());
        inv.setDatabaseBackupUuid(vo.getDatabaseBackupUuid());
        inv.setBackupStorageUuid(vo.getBackupStorageUuid());
        inv.setStatus(vo.getStatus().toString());
        inv.setInstallPath(vo.getInstallPath());
        inv.setExportUrl(vo.getExportUrl());
        inv.setCreateDate(vo.getCreateDate());
        inv.setLastOpDate(vo.getLastOpDate());
        return inv;
    }

    public static List<DatabaseBackupStorageRefInventory> valueOf(Collection<DatabaseBackupStorageRefVO> vos) {
        return vos.stream().map(DatabaseBackupStorageRefInventory::valueOf)
                .collect(Collectors.toList());
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getDatabaseBackupUuid() {
        return databaseBackupUuid;
    }

    public void setDatabaseBackupUuid(String databaseBackupUuid) {
        this.databaseBackupUuid = databaseBackupUuid;
    }

    public String getBackupStorageUuid() {
        return backupStorageUuid;
    }

    public void setBackupStorageUuid(String backupStorageUuid) {
        this.backupStorageUuid = backupStorageUuid;
    }

    public String getInstallPath() {
        return installPath;
    }

    public void setInstallPath(String installPath) {
        this.installPath = installPath;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
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

    public String getExportUrl() {
        return exportUrl;
    }

    public void setExportUrl(String exportUrl) {
        this.exportUrl = exportUrl;
    }
}