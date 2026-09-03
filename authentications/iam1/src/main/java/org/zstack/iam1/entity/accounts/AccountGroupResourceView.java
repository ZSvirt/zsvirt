package org.zstack.iam1.entity.accounts;

import org.zstack.header.vo.ResourceInventory;

import java.util.ArrayList;
import java.util.List;

import static org.zstack.utils.CollectionDSL.list;

/**
 * Created by Wenhao.Zhang on 2024/08/28
 */
public class AccountGroupResourceView {
    private String groupUuid;
    private String groupName;
    private final List<ResourceInventory> resources = new ArrayList<>();

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

    public List<ResourceInventory> getResources() {
        return resources;
    }

    public static AccountGroupResourceView __example__() {
        AccountGroupResourceView view = new AccountGroupResourceView();
        final AccountGroupInventory inventory = AccountGroupInventory.__example__();
        view.setGroupUuid(inventory.getUuid());
        view.setGroupName(inventory.getName());
        view.getResources().addAll(list(ResourceInventory.__example__()));
        return view;
    }
}
