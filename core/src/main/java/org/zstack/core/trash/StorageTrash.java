package org.zstack.core.trash;

import org.zstack.header.core.trash.InstallPathRecycleInventory;

import java.util.List;

/**
 * Created by mingjian.deng on 2018/12/13.
 */
public interface StorageTrash {
    InstallPathRecycleInventory createTrash(TrashType type, boolean isFolder, Object o);

    List<InstallPathRecycleInventory> getTrashList(String storageUuid);
    List<InstallPathRecycleInventory> getTrashList(String storageUuid, List<TrashType> types);
    InstallPathRecycleInventory getTrash(Long trashId);

    String makeSureInstallPathNotUsed(InstallPathRecycleInventory inventory);
    String makeSureInstallPathNotUsed(String installPath, String resourceType);
    String makeSurePrimaryStorageInstallPathNotUsed(String installPath);
    List<String> findTrashInstallPath(String installPath, String storageUuid);
    Long getTrashId(String storageUuid, String installPath);

    void removeFromDb(Long trashId);
}
