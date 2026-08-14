package org.zstack.header.storage.addon.backup;

import org.zstack.header.configuration.PythonClassInventory;
import org.zstack.header.search.Inventory;
import org.zstack.header.search.Parent;
import org.zstack.header.storage.backup.BackupStorageConstant;
import org.zstack.header.storage.backup.BackupStorageInventory;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

@Inventory(mappingVOClass = ExternalBackupStorageVO.class, collectionValueOfMethod="valueOf1",
        parent = {@Parent(inventoryClass = BackupStorageInventory.class, type = BackupStorageConstant.EXTERNAL_BACKUP_STORAGE_TYPE)})
@PythonClassInventory
public class ExternalBackupStorageInventory extends BackupStorageInventory {
    private String identity;

    public String getIdentity() {
        return identity;
    }

    public void setIdentity(String identity) {
        this.identity = identity;
    }

    protected ExternalBackupStorageInventory(ExternalBackupStorageVO vo) {
        super(vo);
        identity = vo.getIdentity();
    }

    public ExternalBackupStorageInventory() {
    }

    public static ExternalBackupStorageInventory valueOf(ExternalBackupStorageVO vo) {
        return new ExternalBackupStorageInventory(vo);
    }

    public static List<ExternalBackupStorageInventory> valueOf1(Collection<ExternalBackupStorageVO> vos) {
        return vos.stream()
                .map(ExternalBackupStorageInventory::valueOf)
                .collect(Collectors.toList());
    }
}
