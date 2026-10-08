package org.zstack.ha.spi;

import org.zstack.header.core.Completion;

/**
 * Created by xing5 on 2016/3/30.
 */
public interface SelfFencerHypervisorBackend {
    void setup(Completion completion);

    void cancel(Completion completion);

    void sync(Completion completion);
}
