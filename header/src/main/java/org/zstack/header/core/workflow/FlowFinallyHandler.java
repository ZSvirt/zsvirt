package org.zstack.header.core.workflow;

import org.zstack.header.core.AbstractCompletion;
import org.zstack.header.core.AsyncBackup;

/**
 * Created by xing5 on 2016/3/29.
 */
public abstract class FlowFinallyHandler extends AbstractCompletion {
    public FlowFinallyHandler(AsyncBackup one, AsyncBackup... others) {
        super(one, others);
    }

    public abstract void Finally();
}
