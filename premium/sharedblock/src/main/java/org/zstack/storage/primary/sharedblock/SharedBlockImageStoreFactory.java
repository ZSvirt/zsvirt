package org.zstack.storage.primary.sharedblock;

import org.zstack.header.storage.primary.PrimaryStorage;
import org.zstack.header.storage.primary.PrimaryStorageVO;

public class SharedBlockImageStoreFactory extends SharedBlockGroupPrimaryStorageFactory {
    @Override
    public PrimaryStorage getPrimaryStorage(PrimaryStorageVO vo) {
        return new SharedBlockImageStoreBackend(vo);
    }
}
