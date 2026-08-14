package org.zstack.storage.primary.local;

import org.zstack.header.storage.primary.PrimaryStorageVO;

/**
 * Created by frank on 6/30/2015.
 */
public interface LocalStorageHypervisorFactory {
    String getHypervisorType();

    LocalStorageHypervisorBackend getHypervisorBackend(PrimaryStorageVO vo);
}
