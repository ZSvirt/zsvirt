package org.zstack.storage.primary.imagestore.local;

import org.zstack.header.storage.primary.PrimaryStorage;
import org.zstack.header.storage.primary.PrimaryStorageVO;
import org.zstack.storage.primary.local.LocalStorageFactory;

public class ImageStorePrimaryStorageFactory extends LocalStorageFactory {
    @Override
    public PrimaryStorage getPrimaryStorage(PrimaryStorageVO vo) {
        return new LocalStorageImageStoreBackend(vo);
    }
}
