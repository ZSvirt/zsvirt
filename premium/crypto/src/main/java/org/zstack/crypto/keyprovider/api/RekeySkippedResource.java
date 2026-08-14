package org.zstack.crypto.keyprovider.api;

import org.zstack.header.message.DocUtils;
import org.zstack.header.tpm.entity.TpmVO;

public class RekeySkippedResource {
    private Long keyRefId;
    private String resourceType;
    private String resourceUuid;
    private String reason;

    public static RekeySkippedResource __example__() {
        RekeySkippedResource resource = new RekeySkippedResource();
        resource.setKeyRefId(11L);
        resource.setResourceType(TpmVO.class.getSimpleName());
        resource.setResourceUuid(DocUtils.createFixedUuid(TpmVO.class));
        resource.setReason(String.format("encrypted resource key ref[id:%s, resourceType:%s, resourceUuid:%s] has empty secret ref",
                resource.getKeyRefId(), resource.getResourceType(), resource.getResourceUuid()));
        return resource;
    }

    public Long getKeyRefId() {
        return keyRefId;
    }

    public void setKeyRefId(Long keyRefId) {
        this.keyRefId = keyRefId;
    }

    public String getResourceType() {
        return resourceType;
    }

    public void setResourceType(String resourceType) {
        this.resourceType = resourceType;
    }

    public String getResourceUuid() {
        return resourceUuid;
    }

    public void setResourceUuid(String resourceUuid) {
        this.resourceUuid = resourceUuid;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}
