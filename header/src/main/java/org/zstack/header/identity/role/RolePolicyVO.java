package org.zstack.header.identity.role;

import org.zstack.header.vo.ForeignKey;

import javax.persistence.*;
import java.sql.Timestamp;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

@Entity
@Table
public class RolePolicyVO {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column
    private Long id;
    @Column
    @ForeignKey(parentEntityClass = RoleVO.class, parentKey = "uuid")
    private String roleUuid;
    @Column
    private String actions;
    @Column
    @Enumerated(EnumType.STRING)
    private RolePolicyEffect effect;
    @Column
    private String resourceType;
    @Column
    private Timestamp createDate;
    // no lastOpDate

    @OneToMany(fetch = FetchType.EAGER)
    @JoinColumn(name = "rolePolicyId", insertable = false, updatable = false)
    private Set<RolePolicyResourceRefVO> resourceRefs = new HashSet<>();

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

    public String getActions() {
        return actions;
    }

    public void setActions(String actions) {
        this.actions = actions;
    }

    public RolePolicyEffect getEffect() {
        return effect;
    }

    public void setEffect(RolePolicyEffect effect) {
        this.effect = effect;
    }

    public String getResourceType() {
        return resourceType;
    }

    public void setResourceType(String resourceType) {
        this.resourceType = resourceType;
    }

    public Timestamp getCreateDate() {
        return createDate;
    }

    public void setCreateDate(Timestamp createDate) {
        this.createDate = createDate;
    }

    public Set<RolePolicyResourceRefVO> getResourceRefs() {
        return resourceRefs;
    }

    public void setResourceRefs(Set<RolePolicyResourceRefVO> resourceRefs) {
        this.resourceRefs = resourceRefs;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RolePolicyVO policyVO = (RolePolicyVO) o;
        return Objects.equals(actions, policyVO.actions)
                && effect == policyVO.effect
                && Objects.equals(resourceType, policyVO.resourceType);
    }

    @Override
    public int hashCode() {
        return Objects.hash(actions, effect, resourceType);
    }
}
