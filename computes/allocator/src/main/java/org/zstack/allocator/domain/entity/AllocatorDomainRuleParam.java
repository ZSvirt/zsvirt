package org.zstack.allocator.domain.entity;

import org.zstack.allocator.domain.header.AllocatorDomainResourceType;
import org.zstack.allocator.domain.header.AllocatorDomainRuleEffect;
import org.zstack.header.configuration.PythonClassInventory;
import org.zstack.header.host.HostVO;
import org.zstack.header.message.APIParam;
import org.zstack.header.message.DocUtils;
import org.zstack.header.rest.SDK;

import java.io.Serializable;

@SDK(sdkClassName = "AllocatorDomainRuleParam")
@PythonClassInventory
public class AllocatorDomainRuleParam implements Serializable {
    @APIParam(maxLength = 32)
    private String resourceUuid;

    @APIParam(validEnums = AllocatorDomainResourceType.class)
    private String resourceType;

    @APIParam(required = false, validValues = {"Root", "Data"})
    private String resourceQualifier;

    @APIParam(required = false)
    private String resourcePolicy;

    @APIParam(validEnums = AllocatorDomainRuleEffect.class)
    private String effect;

    @APIParam(required = false)
    private String policyFrom;

    @APIParam(required = false, maxLength = 32)
    private String policyUuid;

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

    public static AllocatorDomainRuleParam __example__() {
        AllocatorDomainRuleParam param = new AllocatorDomainRuleParam();
        param.setResourceType("HostVO");
        param.setResourceUuid(DocUtils.createFixedUuid(HostVO.class));
        param.setEffect("Include");
        return param;
    }
}
