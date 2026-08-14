package org.zstack.sdk.iam1.accounts;

import org.zstack.sdk.iam1.accounts.AccountGroupRoleView;

public class GetRolesForAccountGroupResult {
    public AccountGroupRoleView currentGroup;
    public void setCurrentGroup(AccountGroupRoleView currentGroup) {
        this.currentGroup = currentGroup;
    }
    public AccountGroupRoleView getCurrentGroup() {
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
