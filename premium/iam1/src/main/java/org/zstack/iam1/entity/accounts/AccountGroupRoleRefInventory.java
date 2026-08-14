package org.zstack.iam1.entity.accounts;

import org.zstack.header.configuration.PythonClassInventory;
import org.zstack.header.search.Inventory;
import org.zstack.utils.CollectionUtils;

import java.sql.Timestamp;
import java.util.Collection;
import java.util.List;

@Inventory(mappingVOClass = AccountGroupRoleRefVO.class)
@PythonClassInventory
public class AccountGroupRoleRefInventory {
    private String groupUuid;
    private String roleUuid;
    private Timestamp createDate;
    private Timestamp lastOpDate;

    public static AccountGroupRoleRefInventory valueOf(AccountGroupRoleRefVO vo) {
        AccountGroupRoleRefInventory inv = new AccountGroupRoleRefInventory();
        inv.setGroupUuid(vo.getGroupUuid());
        inv.setRoleUuid(vo.getRoleUuid());
        inv.setCreateDate(vo.getCreateDate());
        inv.setLastOpDate(vo.getLastOpDate());
        return inv;
    }

    public static List<AccountGroupRoleRefInventory> valueOf(Collection<AccountGroupRoleRefVO> vos) {
        return CollectionUtils.transform(vos, AccountGroupRoleRefInventory::valueOf);
    }

    public String getGroupUuid() {
        return groupUuid;
    }

    public void setGroupUuid(String groupUuid) {
        this.groupUuid = groupUuid;
    }

    public String getRoleUuid() {
        return roleUuid;
    }

    public void setRoleUuid(String roleUuid) {
        this.roleUuid = roleUuid;
    }

    public Timestamp getCreateDate() {
        return createDate;
    }

    public void setCreateDate(Timestamp createDate) {
        this.createDate = createDate;
    }

    public Timestamp getLastOpDate() {
        return lastOpDate;
    }

    public void setLastOpDate(Timestamp lastOpDate) {
        this.lastOpDate = lastOpDate;
    }

    @Override
    public String toString() {
        return "AccountGroupRoleRefInventory{" +
        "groupUuid='" + groupUuid + '\'' +
        ", roleUuid='" + roleUuid + '\'' +
        '}';
    }

    public static AccountGroupRoleRefInventory __example__() {
        AccountGroupRoleRefInventory inventory = new AccountGroupRoleRefInventory();
        inventory.setGroupUuid("50fa69b8ec7c41d98dd88631fba89c9f");
        inventory.setRoleUuid("26a6e21bc28b49cbaaff9a91a01d072a");
        inventory.setCreateDate(new Timestamp(org.zstack.header.message.DocUtils.date));
        inventory.setLastOpDate(new Timestamp(org.zstack.header.message.DocUtils.date));
        return inventory;
    }
}
