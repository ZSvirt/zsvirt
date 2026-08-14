package org.zstack.iam1.entity.accounts;

import org.zstack.header.identity.role.RoleInventory;

import java.util.ArrayList;
import java.util.List;

import static org.zstack.utils.CollectionDSL.list;

/**
 * Created by Wenhao.Zhang on 2024/08/28
 */
public class AccountGroupRoleView {
    private String groupUuid;
    private String groupName;
    private final List<RoleInventory> roles = new ArrayList<>();

    public String getGroupUuid() {
        return groupUuid;
    }

    public void setGroupUuid(String groupUuid) {
        this.groupUuid = groupUuid;
    }

    public String getGroupName() {
        return groupName;
    }

    public void setGroupName(String groupName) {
        this.groupName = groupName;
    }

    public List<RoleInventory> getRoles() {
        return roles;
    }

    public static AccountGroupRoleView __example__() {
        AccountGroupRoleView view = new AccountGroupRoleView();
        final AccountGroupInventory inventory = AccountGroupInventory.__example__();
        view.setGroupUuid(inventory.getUuid());
        view.setGroupName(inventory.getName());
        view.getRoles().addAll(list(RoleInventory.__example__()));
        return view;
    }
}
