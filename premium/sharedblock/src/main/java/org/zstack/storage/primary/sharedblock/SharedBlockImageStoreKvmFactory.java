package org.zstack.storage.primary.sharedblock;

import org.zstack.header.storage.primary.PrimaryStorageVO;

public class SharedBlockImageStoreKvmFactory extends SharedBlockKvmFactory {
    @Override
    public SharedBlockImageStoreKvmBackend getHypervisorBackend(PrimaryStorageVO vo) {
        return new SharedBlockImageStoreKvmBackend(vo);
    }
}
