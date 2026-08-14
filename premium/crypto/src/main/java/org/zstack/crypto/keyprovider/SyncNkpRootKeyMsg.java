package org.zstack.crypto.keyprovider;

import org.zstack.header.message.NeedReplyMessage;

public class SyncNkpRootKeyMsg extends NeedReplyMessage {
    private String providerUuid;
    private String contentBase64;
    private String requestId;

    public String getProviderUuid() {
        return providerUuid;
    }

    public void setProviderUuid(String providerUuid) {
        this.providerUuid = providerUuid;
    }

    public String getContentBase64() {
        return contentBase64;
    }

    public void setContentBase64(String contentBase64) {
        this.contentBase64 = contentBase64;
    }

    public String getRequestId() {
        return requestId;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }
}
