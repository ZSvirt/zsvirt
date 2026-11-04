package org.zstack.iam1.entity.accounts;

import org.zstack.header.configuration.PythonClassInventory;
import org.zstack.header.search.Inventory;
import org.zstack.utils.CollectionUtils;

import java.sql.Timestamp;
import java.util.Collection;
import java.util.List;

@Inventory(mappingVOClass = AccountGroupResourceRefVO.class)
@PythonClassInventory
public class AccountGroupResourceRefInventory {
    private String groupUuid;
    private String resourceUuid;
    private Timestamp createDate;
    private Timestamp lastOpDate;

    public static AccountGroupResourceRefInventory valueOf(AccountGroupResourceRefVO vo) {
        AccountGroupResourceRefInventory inv = new AccountGroupResourceRefInventory();
        inv.setGroupUuid(vo.getGroupUuid());
        inv.setResourceUuid(vo.getResourceUuid());
        inv.setCreateDate(vo.getCreateDate());
        inv.setLastOpDate(vo.getLastOpDate());
        return inv;
    }

    public static List<AccountGroupResourceRefInventory> valueOf(Collection<AccountGroupResourceRefVO> vos) {
        return CollectionUtils.transform(vos, AccountGroupResourceRefInventory::valueOf);
    }

    public String getGroupUuid() {
        return groupUuid;
    }

    public void setGroupUuid(String groupUuid) {
        this.groupUuid = groupUuid;
    }

    public String getResourceUuid() {
        return resourceUuid;
    }

    public void setResourceUuid(String resourceUuid) {
        this.resourceUuid = resourceUuid;
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
        return "AccountGroupResourceRefInventory{" +
        "groupUuid='" + groupUuid + '\'' +
        ", resourceUuid='" + resourceUuid + '\'' +
        '}';
    }

    public static AccountGroupResourceRefInventory __example__() {
        AccountGroupResourceRefInventory inventory = new AccountGroupResourceRefInventory();
        inventory.setGroupUuid("50fa69b8ec7c41d98dd88631fba89c9f");
        inventory.setResourceUuid("6f72c3d0cb4a4bf1a71682d5fc16e4f8");
        inventory.setCreateDate(new Timestamp(org.zstack.header.message.DocUtils.date));
        inventory.setLastOpDate(new Timestamp(org.zstack.header.message.DocUtils.date));
        return inventory;
    }
}
