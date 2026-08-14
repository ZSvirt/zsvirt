package org.zstack.header.resourceattribute.entity;

import org.zstack.header.vo.BaseResource;
import org.zstack.header.vo.NoView;
import org.zstack.header.vo.ResourceVO;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.OneToMany;
import javax.persistence.PreUpdate;
import javax.persistence.Table;
import java.sql.Timestamp;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table
@BaseResource
public class ResourceAttributeKeyVO extends ResourceVO {
    @Column
    private String name;

    @Column
    private String description;

    @Column
    private Timestamp createDate;

    @Column
    private Timestamp lastOpDate;

    @OneToMany(fetch = FetchType.EAGER)
    @JoinColumn(name = "keyUuid", insertable = false, updatable = false)
    @NoView
    private Set<ResourceAttributeKeyResourceTypeVO> types = new HashSet<>();

    @OneToMany(fetch = FetchType.EAGER)
    @JoinColumn(name = "keyUuid", insertable = false, updatable = false)
    @NoView
    private Set<ResourceAttributeConstraintVO> constraints = new HashSet<>();

    @PreUpdate
    private void preUpdate() {
        lastOpDate = null;
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

    public Set<ResourceAttributeKeyResourceTypeVO> getTypes() {
        return types;
    }

    public void setTypes(Set<ResourceAttributeKeyResourceTypeVO> types) {
        this.types = types;
    }

    public Set<ResourceAttributeConstraintVO> getConstraints() {
        return constraints;
    }

    public void setConstraints(Set<ResourceAttributeConstraintVO> constraints) {
        this.constraints = constraints;
    }
}
