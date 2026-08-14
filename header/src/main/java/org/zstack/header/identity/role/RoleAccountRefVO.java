package org.zstack.header.identity.role;

import javax.persistence.*;
import java.sql.Timestamp;

/**
 * RoleAccountRefVO: if account HAS this role;
 * AccountResourceRefVO(resourceType='RoleVO'): if account OWN this role;
 */
@Table
@Entity
public class RoleAccountRefVO {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column
    private String roleUuid;
    @Column
    private String accountUuid;
    @Column
    private String accountPermissionFrom;
    @Column
    private Timestamp createDate;
    @Column
    private Timestamp lastOpDate;

    @PreUpdate
    private void preUpdate() {
        lastOpDate = null;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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
}
