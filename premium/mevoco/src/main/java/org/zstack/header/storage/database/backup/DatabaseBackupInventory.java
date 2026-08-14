package org.zstack.header.storage.database.backup;

import org.zstack.header.configuration.PythonClassInventory;
import org.zstack.header.query.*;
import org.zstack.header.rest.SDK;
import org.zstack.header.search.Inventory;

import javax.persistence.JoinColumn;
import java.sql.Timestamp;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

@Inventory(mappingVOClass = DatabaseBackupVO.class)
@PythonClassInventory
@ExpandedQueries({
        @ExpandedQuery(expandedField = "backupStorageRef", inventoryClass = DatabaseBackupStorageRefInventory.class,
                foreignKey = "uuid", expandedInventoryKey = "databaseBackupUuid", hidden = true),
})
@ExpandedQueryAliases({
        @ExpandedQueryAlias(alias = "backupStorage", expandedField = "backupStorageRef.backupStorage")
})
@SDK
public class DatabaseBackupInventory {
    private String uuid;
    private String name;
    private String description;
    private String state;
    private String status;
    private Long size;
    private String metadata;
    private Timestamp createDate;
    private Timestamp lastOpDate;
    @Queryable(mappingClass = DatabaseBackupStorageRefInventory.class,
            joinColumn = @JoinColumn(name = "databaseBackupUuid"))
    private List<DatabaseBackupStorageRefInventory> backupStorageRefs;

    public static DatabaseBackupInventory valueOf(DatabaseBackupVO vo) {
        DatabaseBackupInventory inv = new DatabaseBackupInventory();
        inv.setName(vo.getName());
        inv.setCreateDate(vo.getCreateDate());
        inv.setDescription(vo.getDescription());
        inv.setLastOpDate(vo.getLastOpDate());
        inv.setUuid(vo.getUuid());
        inv.setSize(vo.getSize());
        inv.setState(vo.getState().toString());
        inv.setStatus(vo.getStatus().toString());
        inv.setMetadata(vo.getMetadata());
        inv.setBackupStorageRefs(DatabaseBackupStorageRefInventory.valueOf(vo.getBackupStorageRefs()));
        return inv;
    }

    public static List<DatabaseBackupInventory> valueOf(Collection<DatabaseBackupVO> vos) {
        return vos.stream().map(DatabaseBackupInventory::valueOf).collect(Collectors.toList());
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

    public Long getSize() {
        return size;
    }

    public void setSize(Long size) {
        this.size = size;
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

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getMetadata() {
        return metadata;
    }

    public void setMetadata(String metadata) {
        this.metadata = metadata;
    }

    public List<DatabaseBackupStorageRefInventory> getBackupStorageRefs() {
        return backupStorageRefs;
    }

    public void setBackupStorageRefs(List<DatabaseBackupStorageRefInventory> backupStorageRefs) {
        this.backupStorageRefs = backupStorageRefs;
    }
}