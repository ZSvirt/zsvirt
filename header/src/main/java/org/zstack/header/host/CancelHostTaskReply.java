package org.zstack.header.host;

import org.zstack.header.message.MessageReply;
import org.zstack.header.message.CancelTaskResult;

/**
 * Created by MaJin on 2019/7/23.
 */
public class CancelHostTaskReply extends MessageReply {
    private CancelTaskResult cancelResult;

    public CancelTaskResult getCancelResult() {
        return cancelResult == null ? CancelTaskResult.CANCEL_SIGNALLED : cancelResult;
    }

    public void setCancelResult(CancelTaskResult cancelResult) {
        this.cancelResult = cancelResult;
    }
}
