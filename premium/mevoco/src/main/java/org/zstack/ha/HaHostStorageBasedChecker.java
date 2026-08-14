package org.zstack.ha;

import org.zstack.header.core.HaCheckerCompletion;
import org.zstack.header.storage.primary.PrimaryStorageType;

/**
 * Created by xing5 on 2016/3/28.
 */
public interface HaHostStorageBasedChecker extends HaHostChecker {
    /**
    * The storage type checker needs to add the corresponding storage type,
    * other types of checks return null by default
    */
    default PrimaryStorageType getPrimaryStorageType() {
        return null;
    }
}
