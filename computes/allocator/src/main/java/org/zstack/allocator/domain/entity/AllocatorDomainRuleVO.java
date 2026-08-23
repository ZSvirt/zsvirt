package org.zstack.allocator.domain.entity;

import org.zstack.allocator.domain.header.AllocatorDomainRuleEffect;
import org.zstack.header.vo.EntityGraph;
import org.zstack.header.vo.ForeignKey;
import org.zstack.header.vo.ToInventory;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import java.sql.Timestamp;

@Entity
@Table
@EntityGraph(
        parents = {
                @EntityGraph.Neighbour(type = AllocatorDomainVO.class, myField = "domainUuid", targetField = "uuid")
        }
)
public class AllocatorDomainRuleVO implements ToInventory {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column
    private Long id;

    @Column
    @ForeignKey(parentEntityClass = AllocatorDomainVO.class, parentKey = "uuid", onDeleteAction = ForeignKey.ReferenceOption.CASCADE)
    private String domainUuid;

    /**
     * Evaluation order within the domain; 0-based. Assigned from the API rules list index on create/rewrite.
     */
    @Column
    private int priority;

    @Column
    private String resourceUuid;

    @Column
    private String resourceType;

    @Column
    private String resourceQualifier;

    @Column
    private String resourcePolicy;

    @Column
    @Enumerated(EnumType.STRING)
    private AllocatorDomainRuleEffect effect;

    @Column
    private String policyFrom;

    @Column
    private String policyUuid;

    @Column
    private Timestamp createDate;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getDomainUuid() {
        return domainUuid;
    }

    public void setDomainUuid(String domainUuid) {
        this.domainUuid = domainUuid;
    }

    public int getPriority() {
        return priority;
    }

    public void setPriority(int priority) {
        this.priority = priority;
    }

    public String getResourceUuid() {
        return resourceUuid;
    }

    public void setResourceUuid(String resourceUuid) {
        this.resourceUuid = resourceUuid;
    }

    public String getResourceType() {
        return resourceType;
    }

    public void setResourceType(String resourceType) {
        this.resourceType = resourceType;
    }

    public String getResourceQualifier() {
        return resourceQualifier;
    }

    public void setResourceQualifier(String resourceQualifier) {
        this.resourceQualifier = resourceQualifier;
    }

    public String getResourcePolicy() {
        return resourcePolicy;
    }

    public void setResourcePolicy(String resourcePolicy) {
        this.resourcePolicy = resourcePolicy;
    }

    public AllocatorDomainRuleEffect getEffect() {
        return effect;
    }

    public void setEffect(AllocatorDomainRuleEffect effect) {
        this.effect = effect;
    }

    public String getPolicyFrom() {
        return policyFrom;
    }

    public void setPolicyFrom(String policyFrom) {
        this.policyFrom = policyFrom;
    }

    public String getPolicyUuid() {
        return policyUuid;
    }

    public void setPolicyUuid(String policyUuid) {
        this.policyUuid = policyUuid;
    }

    public Timestamp getCreateDate() {
        return createDate;
    }

    public void setCreateDate(Timestamp createDate) {
        this.createDate = createDate;
    }
}
