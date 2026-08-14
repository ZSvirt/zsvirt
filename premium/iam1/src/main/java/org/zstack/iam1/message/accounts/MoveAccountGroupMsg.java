package org.zstack.iam1.message.accounts;

import org.zstack.header.message.NeedReplyMessage;
import org.zstack.iam1.api.accounts.AccountGroupMessage;

/**
 * Created by Wenhao.Zhang on 2024/08/30
 */
public class MoveAccountGroupMsg extends NeedReplyMessage implements AccountGroupMessage {
    private String uuid;
    private String parentUuid;

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

    public String getParentUuid() {
        return parentUuid;
    }

    public void setParentUuid(String parentUuid) {
        this.parentUuid = parentUuid;
    }
}
