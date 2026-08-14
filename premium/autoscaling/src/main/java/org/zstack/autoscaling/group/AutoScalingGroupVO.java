package org.zstack.autoscaling.group;

import org.zstack.autoscaling.group.rule.RemovalPolicy;
import org.zstack.autoscaling.template.AutoScalingTemplateGroupRefVO;
import org.zstack.header.identity.OwnedByAccount;
import org.zstack.header.tag.AutoDeleteTag;
import org.zstack.header.tag.SystemTagVO;
import org.zstack.header.vo.*;
import javax.persistence.*;
import java.sql.Timestamp;
import java.util.Set;

/**
 * 云监控 DefaultCooldown
 *
 * RemovalPolicy.N
 * LoadBalancerIds
 *
 */

/**
 * Created by lining on 2018/9/4.
 */
@Entity
@Table
@AutoDeleteTag
@BaseResource
public class AutoScalingGroupVO extends ResourceVO implements ToInventory, OwnedByAccount {

    @Column
    private String name;

    @Column
    private String description;

    @Column
    private Long defaultCooldown;

    @Column
    @Enumerated(EnumType.STRING)
    private ScalingResourceType scalingResourceType;

    // range：[0, 1000]
    @Column
    private Integer maxResourceSize;

    // range：[0, 1000]
    @Column
    private Integer minResourceSize;

    @Column
    @Enumerated(EnumType.STRING)
    private AutoScalingGroupState state;

    @Column
    @Enumerated(EnumType.STRING)
    private RemovalPolicy removalPolicy;

    @Column
    private Timestamp createDate;

    @Column
    private Timestamp lastOpDate;

    @OneToMany(fetch = FetchType.EAGER)
    @JoinColumn(name = "groupUuid", insertable = false, updatable = false)
    @NoView
    private Set<AutoScalingTemplateGroupRefVO> attachedTemplates;

    @OneToMany(fetch = FetchType.EAGER)
    @JoinColumn(name = "resourceUuid", insertable = false, updatable = false)
    @NoView
    private Set<SystemTagVO> systemTags;

    @Transient
    private String accountUuid;

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

    public Set<SystemTagVO> getSystemTags() {
        return systemTags;
    }

    public void setSystemTags(Set<SystemTagVO> systemTags) {
        this.systemTags = systemTags;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Integer getMaxResourceSize() {
        return maxResourceSize;
    }

    public void setMaxResourceSize(Integer maxResourceSize) {
        this.maxResourceSize = maxResourceSize;
    }

    public Integer getMinResourceSize() {
        return minResourceSize;
    }

    public void setMinResourceSize(Integer minResourceSize) {
        this.minResourceSize = minResourceSize;
    }

    public AutoScalingGroupState getState() {
        return state;
    }

    public void setState(AutoScalingGroupState state) {
        this.state = state;
    }

    public ScalingResourceType getScalingResourceType() {
        return scalingResourceType;
    }

    public void setScalingResourceType(ScalingResourceType scalingResourceType) {
        this.scalingResourceType = scalingResourceType;
    }

    public RemovalPolicy getRemovalPolicy() {
        return removalPolicy;
    }

    public void setRemovalPolicy(RemovalPolicy removalPolicy) {
        this.removalPolicy = removalPolicy;
    }

    public Set<AutoScalingTemplateGroupRefVO> getAttachedTemplates() {
        return attachedTemplates;
    }

    public void setAttachedTemplates(Set<AutoScalingTemplateGroupRefVO> attachedTemplates) {
        this.attachedTemplates = attachedTemplates;
    }

    public Long getDefaultCooldown() {
        return defaultCooldown;
    }

    public void setDefaultCooldown(Long defaultCooldown) {
        this.defaultCooldown = defaultCooldown;
    }
}
