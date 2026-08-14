package org.zstack.storage.primary.imagestore.ceph;

import org.zstack.header.storage.primary.BackupStorageMediator;
import org.zstack.header.storage.primary.PrimaryStorageToBackupStorageMediatorExtensionPoint;
import org.zstack.storage.backup.imagestore.ImageStoreBackupStorageConstant;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by mingjian.deng on 2017/11/3.
 */
public class PrimaryStorageToBackupStorageMediatorExtension implements PrimaryStorageToBackupStorageMediatorExtensionPoint {
    @Override
    public Map<String, BackupStorageMediator> getBackupStorageMediators() {
        Map<String, BackupStorageMediator> map = new HashMap<>();
        map.put(ImageStoreBackupStorageConstant.IMAGE_STORE_BACKUP_STORAGE_TYPE, new CephPrimaryToImageStoreBackupStorageMediatorImpl());
        return map;
    }
}
