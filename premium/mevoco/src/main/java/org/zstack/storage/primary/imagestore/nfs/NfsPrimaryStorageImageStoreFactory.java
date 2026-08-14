package org.zstack.storage.primary.imagestore.nfs;

import org.zstack.header.storage.primary.PrimaryStorage;
import org.zstack.header.storage.primary.PrimaryStorageVO;
import org.zstack.storage.primary.nfs.NfsPrimaryStorageFactory;

public class NfsPrimaryStorageImageStoreFactory extends NfsPrimaryStorageFactory {
    @Override
    public PrimaryStorage getPrimaryStorage(PrimaryStorageVO vo) {
        return new NfsPrimaryStorageImageStoreBackend(vo);
    }
}
