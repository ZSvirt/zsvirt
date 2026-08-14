package org.zstack.header.affinitygroup;

import org.zstack.header.identity.OwnedByAccount;
import org.zstack.header.vo.*;
import org.zstack.header.vo.EntityGraph;
import org.zstack.header.vo.ForeignKey;
import org.zstack.header.zone.ZoneEO;

import javax.persistence.*;
import java.sql.Timestamp;
import java.util.HashSet;
import java.util.Set;

/**
 * Created by shixin on 10/24/2017.
 */
@BaseResource
@Entity
@Table
public class AffinityGroupVO extends ResourceVO implements OwnedByAccount {
    @Column
    private String name;

    @Column
    private String description;

    @Column
    @Enumerated(EnumType.STRING)
    private AffinityGroupPolicy policy;

    @Column
    @Enumerated(EnumType.STRING)
    private AffinityGroupType type;

    @Column
    private String version;

    @Column
    private String appliance;

    @Column
    @Enumerated(EnumType.STRING)
    private AffinityGroupState state;

    @Column
    @ForeignKey(parentEntityClass = ZoneEO.class, onDeleteAction = ForeignKey.ReferenceOption.RESTRICT)
    private String zoneUuid;

    @Column
    private Timestamp createDate;

    @Column
    private Timestamp lastOpDate;

    @PreUpdate
    private void preUpdate() {
        lastOpDate = null;
    }

    @OneToMany(fetch=FetchType.EAGER)
    @JoinColumn(name="affinityGroupUuid", insertable=false, updatable=false)
    @NoView
    private Set<AffinityGroupUsageVO> usages = new HashSet<AffinityGroupUsageVO>();

    @Transient
    private String accountUuid;

    @Override
    public String getAccountUuid() {
        return accountUuid;
    }

    @Override
    public void setAccountUuid(String accountUuid) {
        this.accountUuid = accountUuid;
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

    public AffinityGroupPolicy getPolicy() {
        return policy;
    }

    public void setPolicy(AffinityGroupPolicy policy) {
        this.policy = policy;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public AffinityGroupType getType() {
        return type;
    }

    public void setType(AffinityGroupType type) {
        this.type = type;
    }

    public Set<AffinityGroupUsageVO> getUsages() {
        return usages;
    }

    public void setUsages(Set<AffinityGroupUsageVO> usages) {
        this.usages = usages;
    }

    public String getAppliance() {
        return appliance;
    }

    public void setAppliance(String appliance) {
        this.appliance = appliance;
    }

    public AffinityGroupState getState() {
        return state;
    }

    public void setState(AffinityGroupState state) {
        this.state = state;
    }

    public String getZoneUuid() {
        return zoneUuid;
    }

    public void setZoneUuid(String zoneUuid) {
        this.zoneUuid = zoneUuid;
    }

    public AffinityGroupVO() {

    }

    protected AffinityGroupVO(AffinityGroupVO vo) {
        this.uuid = vo.getUuid();
        this.name = vo.getName();
        this.description = vo.getDescription();
        this.policy = vo.getPolicy();
        this.type = vo.getType();
        this.version = vo.getVersion();
        this.appliance = vo.getAppliance();
        this.state = vo.getState();
        this.zoneUuid = vo.getZoneUuid();
        this.createDate = vo.getCreateDate();
        this.lastOpDate = vo.getLastOpDate();
        this.accountUuid = vo.getAccountUuid();
        this.usages = vo.getUsages();
    }
}
