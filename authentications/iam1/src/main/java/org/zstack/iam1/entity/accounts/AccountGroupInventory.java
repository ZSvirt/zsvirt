package org.zstack.iam1.entity.accounts;

import org.zstack.header.configuration.PythonClassInventory;
import org.zstack.header.message.DocUtils;
import org.zstack.header.search.Inventory;
import org.zstack.utils.CollectionUtils;

import java.sql.Timestamp;
import java.util.Collection;
import java.util.List;

@Inventory(mappingVOClass = AccountGroupVO.class)
@PythonClassInventory
public class AccountGroupInventory {
    private String uuid;
    private String name;
    private String description;
    private String parentUuid;
    private String rootGroupUuid;
    private Timestamp createDate;
    private Timestamp lastOpDate;

    public static AccountGroupInventory valueOf(AccountGroupVO vo) {
        AccountGroupInventory inv = new AccountGroupInventory();
        inv.setUuid(vo.getUuid());
        inv.setName(vo.getName());
        inv.setDescription(vo.getDescription());
        inv.setParentUuid(vo.getParentUuid());
        inv.setRootGroupUuid(vo.getRootGroupUuid());
        inv.setCreateDate(vo.getCreateDate());
        inv.setLastOpDate(vo.getLastOpDate());
        return inv;
    }

    public static List<AccountGroupInventory> valueOf(Collection<AccountGroupVO> vos) {
        return CollectionUtils.transform(vos, AccountGroupInventory::valueOf);
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getParentUuid() {
        return parentUuid;
    }

    public void setParentUuid(String parentUuid) {
        this.parentUuid = parentUuid;
    }

    public String getRootGroupUuid() {
        return rootGroupUuid;
    }

    public void setRootGroupUuid(String rootGroupUuid) {
        this.rootGroupUuid = rootGroupUuid;
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
        return "AccountGroupInventory{" +
        "uuid='" + uuid + '\'' +
        ", name='" + name + '\'' +
        ", description='" + description + '\'' +
        ", parentUuid='" + parentUuid + '\'' +
        ", rootGroupUuid='" + rootGroupUuid + '\'' +
        ", createDate=" + createDate +
        ", lastOpDate=" + lastOpDate +
        '}';
    }

    public static AccountGroupInventory __example__() {
        AccountGroupInventory inventory = new AccountGroupInventory();
        inventory.setUuid(DocUtils.createFixedUuid(AccountGroupVO.class));
        inventory.setName("my-group");
        inventory.setDescription("my-group-description");
        inventory.setRootGroupUuid(inventory.getUuid());
        inventory.setCreateDate(DocUtils.timestamp());
        inventory.setLastOpDate(DocUtils.timestamp());
        return inventory;
    }
}
