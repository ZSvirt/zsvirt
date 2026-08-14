package org.zstack.header.core;

import org.zstack.header.core.AsyncBackup;
import org.zstack.header.core.Completion;

/**
 * Created by xing5 on 2016/3/29.
 */
public abstract class HaCompletion extends Completion {
    private String vmUuid;

    public HaCompletion(AsyncBackup one, AsyncBackup... others) {
        super(one, others);
    }

    public abstract void noNeed(String details);

    public String getVmUuid() {
        return vmUuid;
    }

    public void setVmUuid(String vmUuid) {
        this.vmUuid = vmUuid;
    }
}
