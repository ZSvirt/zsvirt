package org.zstack.iam1.api.accounts;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIParam;
import org.zstack.header.message.APISyncCallMessage;
import org.zstack.header.rest.RestRequest;
import org.zstack.iam1.entity.accounts.AccountGroupVO;

@RestRequest(
        path = "/account-groups/tree",
        method = HttpMethod.GET,
        responseClass = APIGetAccountGroupTreeReply.class
)
public class APIGetAccountGroupTreeMsg extends APISyncCallMessage implements AccountGroupMessage {
    @APIParam(resourceType = AccountGroupVO.class, required = false)
    private String groupUuid;
    @APIParam(required = false, numberRange = {0, 127})
    private int level = 0;
    @APIParam(required = false)
    private boolean showGroup = true;
    @APIParam(required = false)
    private boolean showAccount = true;

    @Override
    public String getAccountGroupUuid() {
        return getGroupUuid();
    }

    public String getGroupUuid() {
        return groupUuid;
    }

    public void setGroupUuid(String groupUuid) {
        this.groupUuid = groupUuid;
    }

    public int getLevel() {
        return level;
    }

    public void setLevel(int level) {
        this.level = level;
    }

    public boolean isShowGroup() {
        return showGroup;
    }

    public void setShowGroup(boolean showGroup) {
        this.showGroup = showGroup;
    }

    public boolean isShowAccount() {
        return showAccount;
    }

    public void setShowAccount(boolean showAccount) {
        this.showAccount = showAccount;
    }

    public static APIGetAccountGroupTreeMsg __example__() {
        APIGetAccountGroupTreeMsg msg = new APIGetAccountGroupTreeMsg();
        msg.setGroupUuid(uuid());
        return msg;
    }
}
