package org.zstack.storage.primary.sharedblock;

import org.zstack.header.storage.primary.PrimaryStorageVO;

public interface SharedBlockHypervisorFactory {
    String getHypervisorType();

    SharedBlockHypervisorBackend getHypervisorBackend(PrimaryStorageVO vo);
}
