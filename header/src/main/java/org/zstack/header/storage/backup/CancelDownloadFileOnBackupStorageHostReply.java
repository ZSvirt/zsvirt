package org.zstack.header.storage.backup;

import org.zstack.header.message.MessageReply;
import org.zstack.header.message.CancelTaskResult;

public class CancelDownloadFileOnBackupStorageHostReply extends MessageReply {
    private CancelTaskResult cancelResult;

    public CancelTaskResult getCancelResult() {
        return cancelResult == null ? CancelTaskResult.CANCEL_SIGNALLED : cancelResult;
    }

    public void setCancelResult(CancelTaskResult cancelResult) {
        this.cancelResult = cancelResult;
    }
}
