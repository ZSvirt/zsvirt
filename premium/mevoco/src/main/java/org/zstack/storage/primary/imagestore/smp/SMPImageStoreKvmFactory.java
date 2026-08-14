package org.zstack.storage.primary.imagestore.smp;

import org.zstack.header.storage.primary.PrimaryStorageVO;
import org.zstack.storage.backup.imagestore.ImageStoreBackupStorageConstant;
import org.zstack.storage.primary.smp.KvmFactory;

/**
 * Created by david on 7/27/16.
 */
public class SMPImageStoreKvmFactory extends KvmFactory {
    @Override
    public SMPImageStoreKvmBackend getHypervisorBackend(PrimaryStorageVO vo) {
        return new SMPImageStoreKvmBackend(vo);
    }

    @Override
    public String getExtensionType() {
        return ImageStoreBackupStorageConstant.IMAGE_STORE_BACKUP_STORAGE_TYPE;
    }
}
