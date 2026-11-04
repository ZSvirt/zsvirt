package org.zstack.iam1.message.accounts;

import org.zstack.header.message.NeedReplyMessage;
import org.zstack.iam1.api.accounts.AccountGroupMessage;

/**
 * Created by Wenhao.Zhang on 2024/09/02
 */
public class DeleteAccountGroupMsg extends NeedReplyMessage implements AccountGroupMessage {
    private String uuid;

    @Override
    public String getAccountGroupUuid() {
        return getUuid();
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }
}
