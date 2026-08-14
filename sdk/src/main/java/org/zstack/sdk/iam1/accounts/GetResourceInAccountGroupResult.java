package org.zstack.sdk.iam1.accounts;

import org.zstack.sdk.iam1.accounts.AccountGroupResourceView;

public class GetResourceInAccountGroupResult {
    public AccountGroupResourceView currentGroup;
    public void setCurrentGroup(AccountGroupResourceView currentGroup) {
        this.currentGroup = currentGroup;
    }
    public AccountGroupResourceView getCurrentGroup() {
        return this.currentGroup;
    }

    public java.util.List parentGroups;
    public void setParentGroups(java.util.List parentGroups) {
        this.parentGroups = parentGroups;
    }
    public java.util.List getParentGroups() {
        return this.parentGroups;
    }

}
