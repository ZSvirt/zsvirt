package org.zstack.iam1.api.accounts;

import org.zstack.header.message.APIReply;
import org.zstack.header.rest.RestResponse;
import org.zstack.iam1.entity.accounts.AccountGroupResourceView;

import java.util.List;

@RestResponse(fieldsTo = "all")
public class APIGetResourceInAccountGroupReply extends APIReply {
    private AccountGroupResourceView currentGroup;
    private List<AccountGroupResourceView> parentGroups;

    public AccountGroupResourceView getCurrentGroup() {
        return currentGroup;
    }

    public void setCurrentGroup(AccountGroupResourceView currentGroup) {
        this.currentGroup = currentGroup;
    }

    public List<AccountGroupResourceView> getParentGroups() {
        return parentGroups;
    }

    public void setParentGroups(List<AccountGroupResourceView> parentGroups) {
        this.parentGroups = parentGroups;
    }

    public static APIGetResourceInAccountGroupReply __example__() {
        APIGetResourceInAccountGroupReply reply = new APIGetResourceInAccountGroupReply();
        reply.setCurrentGroup(AccountGroupResourceView.__example__());
        return reply;
    }
}
