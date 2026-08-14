package org.zstack.core.gc;

import org.zstack.header.core.AsyncBackup;
import org.zstack.header.core.Completion;

/**
 * Created by frank on 8/5/2015.
 */
public abstract class GCCompletion extends Completion {
    public GCCompletion(AsyncBackup one, AsyncBackup... others) {
        super(one, others);
    }

    public abstract void cancel();
}
