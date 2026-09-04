package org.zstack.iam1.message.accounts;

import org.zstack.header.message.NeedReplyMessage;
import org.zstack.iam1.api.accounts.AccountGroupMessage;

import java.util.List;

/**
 * Created by Wenhao.Zhang on 2024/09/04
 */
public class DetachRoleFromAccountGroupMsg extends NeedReplyMessage implements AccountGroupMessage {
    private List<String> roleUuids;
    private String groupUuid;

    @Override
    public String getAccountGroupUuid() {
        return getGroupUuid();
    }

    public List<String> getRoleUuids() {
        return roleUuids;
    }

    public void setRoleUuids(List<String> roleUuids) {
        this.roleUuids = roleUuids;
    }

    public String getGroupUuid() {
        return groupUuid;
    }

    public void setGroupUuid(String groupUuid) {
        this.groupUuid = groupUuid;
    }
}
