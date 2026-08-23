package org.zstack.allocator.domain.entity;

import org.zstack.allocator.domain.header.AllocatorDomainState;
import org.zstack.header.identity.OwnedByAccount;
import org.zstack.header.tag.AutoDeleteTag;
import org.zstack.header.vo.BaseResource;
import org.zstack.header.vo.EntityGraph;
import org.zstack.header.vo.ForeignKey;
import org.zstack.header.vo.NoView;
import org.zstack.header.vo.ResourceVO;
import org.zstack.header.vo.ToInventory;
import org.zstack.header.zone.ZoneEO;
import org.zstack.header.zone.ZoneVO;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.OneToMany;
import javax.persistence.OrderBy;
import javax.persistence.PreUpdate;
import javax.persistence.Table;
import javax.persistence.Transient;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Entity
@Table
@BaseResource
@AutoDeleteTag
@EntityGraph(
        parents = {
                @EntityGraph.Neighbour(type = ZoneVO.class, myField = "zoneUuid", targetField = "uuid")
        },
        friends = {
                @EntityGraph.Neighbour(type = AllocatorDomainRuleVO.class, myField = "uuid", targetField = "domainUuid"),
                @EntityGraph.Neighbour(type = AllocatorDomainSceneVO.class, myField = "uuid", targetField = "domainUuid")
        }
)
public class AllocatorDomainVO extends ResourceVO implements OwnedByAccount, ToInventory {
    @Column
    private String name;

    @Column
    private String description;

    @Column
    @ForeignKey(parentEntityClass = ZoneEO.class, onDeleteAction = ForeignKey.ReferenceOption.RESTRICT)
    private String zoneUuid;

    @Column
    @Enumerated(EnumType.STRING)
    private AllocatorDomainState state;

    @Column
    private Timestamp createDate;

    @Column
    private Timestamp lastOpDate;

    @OneToMany(fetch = FetchType.EAGER)
    @JoinColumn(name = "domainUuid", insertable = false, updatable = false)
    @OrderBy("priority ASC")
    @NoView
    private List<AllocatorDomainRuleVO> rules = new ArrayList<>();

    @OneToMany(fetch = FetchType.EAGER)
    @JoinColumn(name = "domainUuid", insertable = false, updatable = false)
    @NoView
    private Set<AllocatorDomainSceneVO> scenes = new HashSet<>();

    @Transient
    private String accountUuid;

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

    public String getZoneUuid() {
        return zoneUuid;
    }

    public void setZoneUuid(String zoneUuid) {
        this.zoneUuid = zoneUuid;
    }

    public AllocatorDomainState getState() {
        return state;
    }

    public void setState(AllocatorDomainState state) {
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

    public List<AllocatorDomainRuleVO> getRules() {
        return rules;
    }

    public void setRules(List<AllocatorDomainRuleVO> rules) {
        this.rules = rules;
    }

    public Set<AllocatorDomainSceneVO> getScenes() {
        return scenes;
    }

    public void setScenes(Set<AllocatorDomainSceneVO> scenes) {
        this.scenes = scenes;
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
