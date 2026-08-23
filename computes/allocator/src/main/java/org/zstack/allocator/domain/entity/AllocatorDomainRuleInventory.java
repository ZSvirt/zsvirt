package org.zstack.allocator.domain.entity;

import org.zstack.allocator.domain.header.AllocatorDomainRuleEffect;
import org.zstack.header.configuration.PythonClassInventory;
import org.zstack.header.host.HostVO;
import org.zstack.header.message.DocUtils;
import org.zstack.header.search.Inventory;

import java.io.Serializable;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;

@PythonClassInventory
@Inventory(mappingVOClass = AllocatorDomainRuleVO.class)
public class AllocatorDomainRuleInventory implements Serializable {
    private Long id;
    private String domainUuid;
    /**
     * Evaluation order within the domain; 0-based.
     */
    private Integer priority;
    private String resourceUuid;
    private String resourceType;
    private String resourceQualifier;
    private String resourcePolicy;
    private String effect;
    private String policyFrom;
    private String policyUuid;
    private Timestamp createDate;

    public static AllocatorDomainRuleInventory valueOf(AllocatorDomainRuleVO vo) {
        AllocatorDomainRuleInventory inv = new AllocatorDomainRuleInventory();
        inv.setId(vo.getId());
        inv.setDomainUuid(vo.getDomainUuid());
        inv.setPriority(vo.getPriority());
        inv.setResourceUuid(vo.getResourceUuid());
        inv.setResourceType(vo.getResourceType());
        inv.setResourceQualifier(vo.getResourceQualifier());
        inv.setResourcePolicy(vo.getResourcePolicy());
        inv.setEffect(vo.getEffect() == null ? null : vo.getEffect().toString());
        inv.setPolicyFrom(vo.getPolicyFrom());
        inv.setPolicyUuid(vo.getPolicyUuid());
        inv.setCreateDate(vo.getCreateDate());
        return inv;
    }

    public static List<AllocatorDomainRuleInventory> valueOf(Collection<AllocatorDomainRuleVO> vos) {
        List<AllocatorDomainRuleInventory> invs = new ArrayList<>();
        if (vos == null) {
            return invs;
        }
        for (AllocatorDomainRuleVO vo : vos) {
            invs.add(valueOf(vo));
        }
        invs.sort(Comparator.comparing(AllocatorDomainRuleInventory::getPriority, Comparator.nullsLast(Integer::compareTo)));
        return invs;
    }

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

    public Integer getPriority() {
        return priority;
    }

    public void setPriority(Integer priority) {
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

    public String getEffect() {
        return effect;
    }

    public void setEffect(String effect) {
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

    public static AllocatorDomainRuleInventory __example__() {
        AllocatorDomainRuleInventory inv = new AllocatorDomainRuleInventory();
        inv.setId(1L);
        inv.setDomainUuid(DocUtils.createFixedUuid(AllocatorDomainVO.class));
        inv.setPriority(0);
        inv.setResourceType("HostVO");
        inv.setResourceUuid(DocUtils.createFixedUuid(HostVO.class));
        inv.setEffect(AllocatorDomainRuleEffect.Include.toString());
        return inv;
    }
}
