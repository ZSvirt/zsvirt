package org.zstack.network.service.vip;

import org.zstack.header.core.Completion;
import org.zstack.header.message.Message;
/**
 * Created by xing5 on 2016/11/20.
 */
public abstract class VipBaseBackend extends VipBase {
    public VipBaseBackend(VipVO self) {
        super(self);
    }

    protected abstract void releaseVipOnBackend(Completion completion);
    protected abstract void acquireVipOnBackend(Completion completion);
    protected abstract void handleBackendSpecificMessage(Message msg);
}
