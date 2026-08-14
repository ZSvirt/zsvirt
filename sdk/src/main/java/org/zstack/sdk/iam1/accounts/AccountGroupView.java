package org.zstack.sdk.iam1.accounts;

import org.zstack.sdk.iam1.accounts.AccountGroupInventory;

public class AccountGroupView  {

    public java.lang.String groupUuid;
    public void setGroupUuid(java.lang.String groupUuid) {
        this.groupUuid = groupUuid;
    }
    public java.lang.String getGroupUuid() {
        return this.groupUuid;
    }

    public java.lang.String groupName;
    public void setGroupName(java.lang.String groupName) {
        this.groupName = groupName;
    }
    public java.lang.String getGroupName() {
        return this.groupName;
    }

    public AccountGroupInventory inventory;
    public void setInventory(AccountGroupInventory inventory) {
        this.inventory = inventory;
    }
    public AccountGroupInventory getInventory() {
        return this.inventory;
    }

    public java.util.List accounts;
    public void setAccounts(java.util.List accounts) {
        this.accounts = accounts;
    }
    public java.util.List getAccounts() {
        return this.accounts;
    }

    public java.util.List groups;
    public void setGroups(java.util.List groups) {
        this.groups = groups;
    }
    public java.util.List getGroups() {
        return this.groups;
    }

}
