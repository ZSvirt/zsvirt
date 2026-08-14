package org.zstack.header.identity.role;

import org.zstack.header.search.Inventory;

import java.sql.Timestamp;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

@Inventory(mappingVOClass = RoleAccountRefVO.class)
public class RoleAccountRefInventory {
    private String roleUuid;
    private String accountUuid;
    private String accountPermissionFrom;
    private Timestamp createDate;
    private Timestamp lastOpDate;

    public static RoleAccountRefInventory valueOf(RoleAccountRefVO vo) {
        RoleAccountRefInventory inv = new RoleAccountRefInventory();
        inv.setRoleUuid(vo.getRoleUuid());
        inv.setAccountUuid(vo.getAccountUuid());
        inv.setAccountPermissionFrom(vo.getAccountPermissionFrom());
        inv.setCreateDate(vo.getCreateDate());
        inv.setLastOpDate(vo.getLastOpDate());
        return inv;
    }

    public static List<RoleAccountRefInventory> valueOf(Collection<RoleAccountRefVO> vos) {
        return vos.stream().map(RoleAccountRefInventory::valueOf).collect(Collectors.toList());
    }

    public String getRoleUuid() {
        return roleUuid;
    }

    public void setRoleUuid(String roleUuid) {
        this.roleUuid = roleUuid;
    }

    public String getAccountUuid() {
        return accountUuid;
    }

    public void setAccountUuid(String accountUuid) {
        this.accountUuid = accountUuid;
    }

    public String getAccountPermissionFrom() {
        return accountPermissionFrom;
    }

    public void setAccountPermissionFrom(String accountPermissionFrom) {
        this.accountPermissionFrom = accountPermissionFrom;
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

    public static RoleAccountRefInventory __example__() {
        RoleAccountRefInventory ref = new RoleAccountRefInventory();
        ref.setRoleUuid("686cb963323e491e955a0fd0b49dd743");
        ref.setAccountUuid("5360250ef145409e862b4e99d2b2efc4");
        ref.setAccountPermissionFrom(null);
        ref.setCreateDate(new Timestamp(org.zstack.header.message.DocUtils.date));
        ref.setLastOpDate(new Timestamp(org.zstack.header.message.DocUtils.date));
        return ref;
    }
}
