package org.zstack.crypto.keyprovider.api;

import org.zstack.header.message.DocUtils;
import org.zstack.header.tpm.entity.TpmVO;

public class RekeyFailedResource {
    private Long keyRefId;
    private String resourceType;
    private String resourceUuid;
    private String reason;

    public static RekeyFailedResource __example__() {
        RekeyFailedResource resource = new RekeyFailedResource();
        resource.setKeyRefId(12L);
        resource.setResourceType(TpmVO.class.getSimpleName());
        resource.setResourceUuid(DocUtils.createFixedUuid(TpmVO.class));
        resource.setReason("key-tool rekey failed: failed to decrypt wrapper DEK with KEK");
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
