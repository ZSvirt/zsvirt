package org.zstack.ha;

import org.zstack.header.core.HaCompletion;

/**
 * Created by xing5 on 2016/3/28.
 */
public interface HaHypervisorWorker {
    void start(HaCompletion completion);
}
