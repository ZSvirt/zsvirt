package org.zstack.iam1.api.accounts;

import org.zstack.header.message.APIReply;
import org.zstack.header.rest.RestResponse;
import org.zstack.iam1.entity.accounts.AccountGroupRoleView;

import java.util.List;

@RestResponse(fieldsTo = "all")
public class APIGetRolesForAccountGroupReply extends APIReply {
    private AccountGroupRoleView currentGroup;
    private List<AccountGroupRoleView> parentGroups;

    public AccountGroupRoleView getCurrentGroup() {
        return currentGroup;
    }

    public void setCurrentGroup(AccountGroupRoleView currentGroup) {
        this.currentGroup = currentGroup;
    }

    public List<AccountGroupRoleView> getParentGroups() {
        return parentGroups;
    }

    public void setParentGroups(List<AccountGroupRoleView> parentGroups) {
        this.parentGroups = parentGroups;
    }

    public static APIGetRolesForAccountGroupReply __example__() {
        APIGetRolesForAccountGroupReply reply = new APIGetRolesForAccountGroupReply();
        reply.setCurrentGroup(AccountGroupRoleView.__example__());
        return reply;
    }
}
