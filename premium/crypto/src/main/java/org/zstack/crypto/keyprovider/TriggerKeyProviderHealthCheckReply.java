package org.zstack.crypto.keyprovider;

import org.zstack.header.message.MessageReply;

public class TriggerKeyProviderHealthCheckReply extends MessageReply {
    private boolean available;
    private boolean connected;
    private String trustState;
    private String activeIdentityUuid;
    private Integer statusCode;
    private String statusMessage;

    public boolean isAvailable() {
        return available;
    }

    public void setAvailable(boolean available) {
        this.available = available;
    }

    public boolean isConnected() {
        return connected;
    }

    public void setConnected(boolean connected) {
        this.connected = connected;
    }

    public String getTrustState() {
        return trustState;
    }

    public void setTrustState(String trustState) {
        this.trustState = trustState;
    }

    public String getActiveIdentityUuid() {
        return activeIdentityUuid;
    }

    public void setActiveIdentityUuid(String activeIdentityUuid) {
        this.activeIdentityUuid = activeIdentityUuid;
    }

    public Integer getStatusCode() {
        return statusCode;
    }

    public void setStatusCode(Integer statusCode) {
        this.statusCode = statusCode;
    }

    public String getStatusMessage() {
        return statusMessage;
    }

    public void setStatusMessage(String statusMessage) {
        this.statusMessage = statusMessage;
    }
}
