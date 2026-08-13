package org.zstack.header.tpm.message;

import org.zstack.header.message.NeedReplyMessage;
import org.zstack.header.tpm.api.APIAddTpmMsg;

public class AddTpmMsg extends NeedReplyMessage {
    /**
     * Create new key with the provider uuid.
     * "keyProviderUuid" and "resourceUuidKeyFrom" should not be set at the same time.
     */
    private String keyProviderUuid;
    /**
     * Copy key from the resource uuid.
     * "keyProviderUuid" and "resourceUuidKeyFrom" should not be set at the same time.
     */
    private String resourceUuidKeyFrom;

    private String vmInstanceUuid;
    /**
     * for creating TpmVO
     */
    private String tpmUuid;

    public String getKeyProviderUuid() {
        return keyProviderUuid;
    }

    public void setKeyProviderUuid(String keyProviderUuid) {
        this.keyProviderUuid = keyProviderUuid;
    }

    public String getResourceUuidKeyFrom() {
        return resourceUuidKeyFrom;
    }

    public void setResourceUuidKeyFrom(String resourceUuidKeyFrom) {
        this.resourceUuidKeyFrom = resourceUuidKeyFrom;
    }

    public String getVmInstanceUuid() {
        return vmInstanceUuid;
    }

    public void setVmInstanceUuid(String vmInstanceUuid) {
        this.vmInstanceUuid = vmInstanceUuid;
    }

    public String getTpmUuid() {
        return tpmUuid;
    }

    public void setTpmUuid(String tpmUuid) {
        this.tpmUuid = tpmUuid;
    }

    public static AddTpmMsg valueOf(APIAddTpmMsg api) {
        AddTpmMsg msg = new AddTpmMsg();
        msg.setKeyProviderUuid(api.getKeyProviderUuid());
        msg.setVmInstanceUuid(api.getVmInstanceUuid());
        msg.setTpmUuid(api.getResourceUuid());
        return msg;
    }
}
