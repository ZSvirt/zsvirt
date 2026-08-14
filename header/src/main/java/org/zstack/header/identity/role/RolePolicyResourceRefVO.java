package org.zstack.header.identity.role;

import org.zstack.header.vo.ForeignKey;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import java.util.Objects;

@Entity
@Table
public class RolePolicyResourceRefVO {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column
    private Long id;
    @Column
    @ForeignKey(parentEntityClass = RolePolicyVO.class, parentKey = "id")
    private Long rolePolicyId;
    @Column
    @Enumerated(EnumType.STRING)
    private RolePolicyResourceEffect effect;
    @Column
    private String resourceUuid;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getRolePolicyId() {
        return rolePolicyId;
    }

    public void setRolePolicyId(Long rolePolicyId) {
        this.rolePolicyId = rolePolicyId;
    }

    public RolePolicyResourceEffect getEffect() {
        return effect;
    }

    public void setEffect(RolePolicyResourceEffect effect) {
        this.effect = effect;
    }

    public String getResourceUuid() {
        return resourceUuid;
    }

    public void setResourceUuid(String resourceUuid) {
        this.resourceUuid = resourceUuid;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RolePolicyResourceRefVO that = (RolePolicyResourceRefVO) o;
        return effect == that.effect && Objects.equals(resourceUuid, that.resourceUuid);
    }

    @Override
    public int hashCode() {
        return Objects.hash(effect, resourceUuid);
    }
}
