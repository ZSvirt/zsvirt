package org.zstack.iam1.message.accounts;

import org.zstack.header.message.NeedReplyMessage;
import org.zstack.iam1.api.accounts.AccountGroupMessage;

import java.util.List;

/**
 * Created by Wenhao.Zhang on 2024/09/03
 */
public class RemoveAccountFromGroupMsg extends NeedReplyMessage implements AccountGroupMessage {
    private List<String> accountUuids;
    private String groupUuid;

    @Override
    public String getAccountGroupUuid() {
        return getGroupUuid();
    }

    public List<String> getAccountUuids() {
        return accountUuids;
    }

    public void setAccountUuids(List<String> accountUuids) {
        this.accountUuids = accountUuids;
    }

    public String getGroupUuid() {
        return groupUuid;
    }

    public void setGroupUuid(String groupUuid) {
        this.groupUuid = groupUuid;
    }
}
