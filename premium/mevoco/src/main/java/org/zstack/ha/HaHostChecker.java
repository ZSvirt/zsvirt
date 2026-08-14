package org.zstack.ha;

import org.zstack.header.core.HaCheckerCompletion;
import org.zstack.header.storage.primary.PrimaryStorageType;

/**
 * Created by xing5 on 2016/3/28.
 */
public interface HaHostChecker {
    void check(CheckerStruct struct, HaCheckerCompletion completion);

    int getWeight();
}
