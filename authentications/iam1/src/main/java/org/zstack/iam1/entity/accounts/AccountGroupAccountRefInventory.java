package org.zstack.iam1.entity.accounts;

import org.zstack.header.configuration.PythonClassInventory;
import org.zstack.header.search.Inventory;
import org.zstack.utils.CollectionUtils;

import java.sql.Timestamp;
import java.util.Collection;
import java.util.List;

@Inventory(mappingVOClass = AccountGroupAccountRefVO.class)
@PythonClassInventory
public class AccountGroupAccountRefInventory {
    private String groupUuid;
    private String accountUuid;
    private Timestamp createDate;
    private Timestamp lastOpDate;

    public static AccountGroupAccountRefInventory valueOf(AccountGroupAccountRefVO vo) {
        AccountGroupAccountRefInventory inv = new AccountGroupAccountRefInventory();
        inv.setGroupUuid(vo.getGroupUuid());
        inv.setAccountUuid(vo.getAccountUuid());
        inv.setCreateDate(vo.getCreateDate());
        inv.setLastOpDate(vo.getLastOpDate());
        return inv;
    }

    public static List<AccountGroupAccountRefInventory> valueOf(Collection<AccountGroupAccountRefVO> vos) {
        return CollectionUtils.transform(vos, AccountGroupAccountRefInventory::valueOf);
    }

    public String getGroupUuid() {
        return groupUuid;
    }

    public void setGroupUuid(String groupUuid) {
        this.groupUuid = groupUuid;
    }

    public String getAccountUuid() {
        return accountUuid;
    }

    public void setAccountUuid(String accountUuid) {
        this.accountUuid = accountUuid;
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
        return "AccountGroupAccountRefInventory{" +
        "groupUuid='" + groupUuid + '\'' +
        ", accountUuid='" + accountUuid + '\'' +
        '}';
    }

    public static AccountGroupAccountRefInventory __example__() {
        AccountGroupAccountRefInventory inventory = new AccountGroupAccountRefInventory();
        inventory.setGroupUuid("50fa69b8ec7c41d98dd88631fba89c9f");
        inventory.setAccountUuid("8b84ebf7b2784485b329688c36002567");
        inventory.setCreateDate(new Timestamp(org.zstack.header.message.DocUtils.date));
        inventory.setLastOpDate(new Timestamp(org.zstack.header.message.DocUtils.date));
        return inventory;
    }
}
