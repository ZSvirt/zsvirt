package org.zstack.core.thread;

import org.zstack.header.PassMaskWords;
import org.zstack.header.core.AbstractCompletion;
import org.zstack.header.core.AsyncBackup;

import java.util.Map;

public abstract class AbstractChainTask extends AbstractCompletion implements PassMaskWords {
    protected AbstractChainTask(AsyncBackup one, AsyncBackup... others) {
        super(one, others);
    }

    public abstract String getSyncSignature();

    public abstract String getName();
}
