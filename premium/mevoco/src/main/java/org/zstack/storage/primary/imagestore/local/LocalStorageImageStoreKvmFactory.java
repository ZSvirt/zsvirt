package org.zstack.storage.primary.imagestore.local;

import org.zstack.header.storage.primary.PrimaryStorageVO;
import org.zstack.storage.primary.local.LocalStorageHypervisorBackend;
import org.zstack.storage.primary.local.LocalStorageKvmFactory;

public class LocalStorageImageStoreKvmFactory extends LocalStorageKvmFactory {
    @Override
    public LocalStorageHypervisorBackend getHypervisorBackend(PrimaryStorageVO vo) {
        return new LocalStorageImageStoreKvmBackend(vo);
    }
}
