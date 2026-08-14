package org.zstack.iam1.entity.accounts;

import org.zstack.header.identity.AccountInventory;
import org.zstack.header.message.DocUtils;
import org.zstack.iam1.header.accounts.GroupNode;
import org.zstack.utils.CollectionUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.zstack.utils.CollectionDSL.list;

/**
 * Created by Wenhao.Zhang on 2024/08/28
 */
public class AccountGroupView {
    private String groupUuid;
    private String groupName;

    private AccountGroupInventory inventory;
    private List<AccountInventory> accounts = new ArrayList<>();
    private List<AccountGroupView> groups = new ArrayList<>();

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

    public AccountGroupInventory getInventory() {
        return inventory;
    }

    public void setInventory(AccountGroupVO vo) {
        setInventory(vo == null ? null : AccountGroupInventory.valueOf(vo));
    }

    public void setInventory(AccountGroupInventory inventory) {
        this.inventory = inventory;

        if (inventory != null) {
            setGroupUuid(inventory.getUuid());
            setGroupName(inventory.getName());
        }
    }

    public List<AccountInventory> getAccounts() {
        return accounts;
    }

    public void setAccounts(List<AccountInventory> accounts) {
        this.accounts = accounts;
    }

    public List<AccountGroupView> getGroups() {
        return groups;
    }

    public void setGroups(List<AccountGroupView> groups) {
        this.groups = groups;
    }

    public static AccountGroupView valueOf(GroupNode groupNode) {
        AccountGroupView view = new AccountGroupView();
        view.setGroupUuid(groupNode.uuid);

        if (groupNode.children != null) {
            for (GroupNode child : groupNode.children) {
                view.getGroups().add(valueOf(child));
            }
        }
        return view;
    }

    public List<AccountGroupView> flatten() {
        if (CollectionUtils.isEmpty(groups)) {
            return Arrays.asList(this);
        }

        List<AccountGroupView> list = new ArrayList<>();
        list.add(this);
        for (AccountGroupView group : groups) {
            list.addAll(group.flatten());
        }
        return list;
    }

    public static AccountGroupView __example__() {
        AccountGroupView view = new AccountGroupView();
        view.setInventory(AccountGroupInventory.__example__());
        view.setAccounts(list(AccountInventory.__example__()));

        AccountGroupView child = new AccountGroupView();
        child.setGroupUuid(DocUtils.createFixedUuid(AccountGroupVO.class));
        child.setGroupName("child-group");
        view.setGroups(list(child));

        return view;
    }
}
