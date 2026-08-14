package org.zstack.header.baremetal.preconfiguration;

import org.zstack.header.identity.OwnedByAccount;
import org.zstack.header.vo.BaseResource;
import org.zstack.header.vo.NoView;
import org.zstack.header.vo.ResourceVO;
import org.zstack.header.vo.ToInventory;

import javax.persistence.*;
import java.sql.Timestamp;
import java.util.HashSet;
import java.util.Set;

/**
 * Created by GuoYi on 2018-12-28.
 */
@Entity
@Table
@BaseResource
public class PreconfigurationTemplateVO extends ResourceVO implements ToInventory, OwnedByAccount {
    @Column
    private String name;

    @Column
    private String description;

    @Column
    private String distribution;

    @Column
    private String type;

    @Column
    private String content;

    @Column
    private String md5sum;

    @Column
    private Boolean isPredefined = false;

    @Column
    @Enumerated(EnumType.STRING)
    private PreconfigurationTemplateState state;

    @OneToMany(fetch = FetchType.EAGER)
    @JoinColumn(name = "templateUuid", insertable = false, updatable = false)
    @NoView
    private Set<TemplateCustomParamVO> customParams = new HashSet<>();

    @Transient
    private String accountUuid;

    @Column
    private Timestamp createDate;

    @Column
    private Timestamp lastOpDate;

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

    public String getDistribution() {
        return distribution;
    }

    public void setDistribution(String distribution) {
        this.distribution = distribution;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getMd5sum() {
        return md5sum;
    }

    public void setMd5sum(String md5sum) {
        this.md5sum = md5sum;
    }

    public Boolean getPredefined() {
        return isPredefined;
    }

    public void setPredefined(Boolean predefined) {
        isPredefined = predefined;
    }

    public PreconfigurationTemplateState getState() {
        return state;
    }

    public void setState(PreconfigurationTemplateState state) {
        this.state = state;
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

    public Set<TemplateCustomParamVO> getCustomParams() {
        return customParams;
    }

    public void setCustomParams(Set<TemplateCustomParamVO> customParams) {
        this.customParams = customParams;
    }

    @Override
    public String getAccountUuid() {
        return accountUuid;
    }

    @Override
    public void setAccountUuid(String accountUuid) {
        this.accountUuid = accountUuid;
    }
}
