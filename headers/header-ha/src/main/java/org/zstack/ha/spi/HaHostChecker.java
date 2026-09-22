package org.zstack.ha.spi;

import org.zstack.ha.message.CheckerStruct;
import org.zstack.header.core.HaCheckerCompletion;

/**
 * Created by xing5 on 2016/3/28.
 */
public interface HaHostChecker {
    void check(CheckerStruct struct, HaCheckerCompletion completion);

    int getWeight();
}
