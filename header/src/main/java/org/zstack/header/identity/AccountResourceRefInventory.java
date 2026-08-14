package org.zstack.header.identity;

import org.zstack.header.rest.APINoSee;
import org.zstack.header.search.Inventory;
import org.zstack.utils.CollectionUtils;

import java.sql.Timestamp;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

/**
 */
@Inventory(mappingVOClass = AccountResourceRefVO.class)
public class AccountResourceRefInventory {
    @APINoSee
    private Long id;
    private String accountUuid;
    private String resourceUuid;
    private String resourceType;
    private String accountPermissionFrom;
    private String resourcePermissionFrom;
    private String type;
    private Timestamp createDate;
    private Timestamp lastOpDate;

    public static AccountResourceRefInventory valueOf(AccountResourceRefVO vo) {
        AccountResourceRefInventory inv = new AccountResourceRefInventory();
        inv.setAccountUuid(vo.getAccountUuid());
        inv.setCreateDate(vo.getCreateDate());
        inv.setId(vo.getId());
        inv.setLastOpDate(vo.getLastOpDate());
        inv.setResourceType(vo.getResourceType());
        inv.setResourceUuid(vo.getResourceUuid());
        inv.setAccountPermissionFrom(vo.getAccountPermissionFrom());
        inv.setResourcePermissionFrom(vo.getResourcePermissionFrom());
        inv.setType(Objects.toString(vo.getType()));
        return inv;
    }

    public static List<AccountResourceRefInventory> valueOf(Collection<AccountResourceRefVO> vos) {
        return CollectionUtils.transform(vos, AccountResourceRefInventory::valueOf);
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getAccountUuid() {
        return accountUuid;
    }

    public void setAccountUuid(String accountUuid) {
        this.accountUuid = accountUuid;
    }

    public String getResourceUuid() {
        return resourceUuid;
    }

    public void setResourceUuid(String resourceUuid) {
        this.resourceUuid = resourceUuid;
    }

    public String getResourceType() {
        return resourceType;
    }

    public void setResourceType(String resourceType) {
        this.resourceType = resourceType;
    }

    public String getAccountPermissionFrom() {
        return accountPermissionFrom;
    }

    public void setAccountPermissionFrom(String accountPermissionFrom) {
        this.accountPermissionFrom = accountPermissionFrom;
    }

    public String getResourcePermissionFrom() {
        return resourcePermissionFrom;
    }

    public void setResourcePermissionFrom(String resourcePermissionFrom) {
        this.resourcePermissionFrom = resourcePermissionFrom;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
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
}
