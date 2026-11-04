package org.zstack.identity.imports.message;

import org.zstack.header.message.MessageReply;
import org.zstack.identity.imports.header.SyncTaskResult;

/**
 * Created by Wenhao.Zhang on 2024/06/05
 */
public class SyncThirdPartyAccountReply extends MessageReply {
    private SyncTaskResult result;

    public SyncTaskResult getResult() {
        return result;
    }

    public void setResult(SyncTaskResult result) {
        this.result = result;
    }
}
