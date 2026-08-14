package org.zstack.header.identity.role;

import org.zstack.header.identity.OwnedByAccount;
import org.zstack.header.vo.BaseResource;
import org.zstack.header.vo.ResourceVO;

import javax.persistence.*;
import java.sql.Timestamp;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table
@BaseResource
public class RoleVO extends ResourceVO implements OwnedByAccount {
    @Column
    private String name;
    @Column
    private String description;
    @Column
    private Timestamp createDate;
    @Column
    private Timestamp lastOpDate;
    @Column
    @Enumerated(EnumType.STRING)
    private RoleType type;
    @OneToMany(fetch = FetchType.EAGER)
    @JoinColumn(name = "roleUuid", insertable = false, updatable = false)
    private Set<RolePolicyVO> policies = new HashSet<>();

    @Transient
    private String accountUuid;

    public RoleVO() {
    }

    @PreUpdate
    private void preUpdate() {
        lastOpDate = null;
    }

    @Override
    public String getAccountUuid() {
        return accountUuid;
    }

    @Override
    public void setAccountUuid(String accountUuid) {
        this.accountUuid = accountUuid;
    }

    public Set<RolePolicyVO> getPolicies() {
        return policies;
    }

    public void setPolicies(Set<RolePolicyVO> policies) {
        this.policies = policies;
    }

    public RoleType getType() {
        return type;
    }

    public void setType(RoleType type) {
        this.type = type;
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
