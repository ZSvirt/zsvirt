package org.zstack.core.trash;

import org.zstack.header.core.trash.InstallPathRecycleVO;

/**
 * Created by mingjian.deng on 2019/10/29.
 */
public interface CreateRecycleExtensionPoint {
    void setHostUuid(InstallPathRecycleVO vo, String primaryStorageUuid);
}
