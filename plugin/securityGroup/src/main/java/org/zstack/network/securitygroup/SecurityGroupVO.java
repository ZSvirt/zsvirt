package org.zstack.network.securitygroup;

import org.zstack.header.identity.OwnedByAccount;
import org.zstack.header.vo.BaseResource;
import org.zstack.header.vo.EntityGraph;
import org.zstack.header.vo.Index;
import org.zstack.header.vo.ResourceVO;

import javax.persistence.*;
import java.sql.Timestamp;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table
@BaseResource
public class SecurityGroupVO extends ResourceVO implements OwnedByAccount {
    @Column
    @Index
    private String name;
    
    @Column
    private String description;
    
    @Column
    private long internalId;

    @Column
    @Enumerated(EnumType.STRING)
    private SecurityGroupState state;

    @Column
    @Deprecated
    private Integer ipVersion;

    @Column
    private Timestamp createDate;
    
    @Column
    private Timestamp lastOpDate;
    
    @OneToMany(fetch=FetchType.EAGER)
    @JoinColumn(name="securityGroupUuid", insertable=false, updatable=false)
    private Set<SecurityGroupRuleVO> rules = new HashSet<SecurityGroupRuleVO>();
    
    @OneToMany(fetch=FetchType.EAGER)
    @JoinColumn(name="securityGroupUuid", insertable=false, updatable=false)
    private Set<SecurityGroupL3NetworkRefVO> attachedL3NetworkRefs = new HashSet<SecurityGroupL3NetworkRefVO>();

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

    public Set<SecurityGroupRuleVO> getRules() {
        return rules;
    }

    public void setRules(Set<SecurityGroupRuleVO> rules) {
        this.rules = rules;
    }

    public long getInternalId() {
        return internalId;
    }

    public void setInternalId(long internalId) {
        this.internalId = internalId;
    }

    public Set<SecurityGroupL3NetworkRefVO> getAttachedL3NetworkRefs() {
        return attachedL3NetworkRefs;
    }

    public void setAttachedL3NetworkRefs(Set<SecurityGroupL3NetworkRefVO> attachedL3NetworkRefs) {
        this.attachedL3NetworkRefs = attachedL3NetworkRefs;
    }

    public SecurityGroupState getState() {
        return state;
    }

    public void setState(SecurityGroupState state) {
        this.state = state;
    }

    @Deprecated
    public Integer getIpVersion() {
        return ipVersion;
    }

    @Deprecated
    public void setIpVersion(Integer ipVersion) {
        this.ipVersion = ipVersion;
    }
}
