package org.zstack.imagereplicator;

import org.zstack.header.query.ExpandedQueries;
import org.zstack.header.query.ExpandedQuery;
import org.zstack.header.search.Inventory;
import org.zstack.header.storage.backup.BackupStorageInventory;

import java.io.Serializable;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@Inventory(mappingVOClass = ImageReplicationGroupBackupStorageRefVO.class)
@ExpandedQueries({
        @ExpandedQuery(expandedField = "replicationGroup", inventoryClass = ImageReplicationGroupInventory.class,
                foreignKey = "replicationGroupUuid", expandedInventoryKey = "uuid"),
        @ExpandedQuery(expandedField = "backupStorage", inventoryClass = BackupStorageInventory.class,
                foreignKey = "backupStorageUuid", expandedInventoryKey = "uuid"),
})
public class ImageReplicationGroupBackupStorageRefInventory implements Serializable {
    private String replicationGroupUuid;
    private String backupStorageUuid;
    private Timestamp createDate;
    private Timestamp lastOpDate;

    public static ImageReplicationGroupBackupStorageRefInventory valueOf(ImageReplicationGroupBackupStorageRefVO vo) {
        ImageReplicationGroupBackupStorageRefInventory inv = new ImageReplicationGroupBackupStorageRefInventory();
        inv.setBackupStorageUuid(vo.getBackupStorageUuid());
        inv.setReplicationGroupUuid(vo.getReplicationGroupUuid());
        inv.setCreateDate(vo.getCreateDate());
        inv.setLastOpDate(vo.getLastOpDate());
        return inv;
    }

    public static List<ImageReplicationGroupBackupStorageRefInventory> valueOf(Collection<ImageReplicationGroupBackupStorageRefVO> vos) {
        List<ImageReplicationGroupBackupStorageRefInventory> invs = new ArrayList<>(vos.size());
        for (ImageReplicationGroupBackupStorageRefVO vo : vos) {
            invs.add(ImageReplicationGroupBackupStorageRefInventory.valueOf(vo));
        }
        return invs;
    }

    public String getReplicationGroupUuid() {
        return replicationGroupUuid;
    }

    public void setReplicationGroupUuid(String replicationGroupUuid) {
        this.replicationGroupUuid = replicationGroupUuid;
    }

    public String getBackupStorageUuid() {
        return backupStorageUuid;
    }

    public void setBackupStorageUuid(String backupStorageUuid) {
        this.backupStorageUuid = backupStorageUuid;
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
