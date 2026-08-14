package org.zstack.header.secret;

import org.zstack.header.host.HostMessage;
import org.zstack.header.log.NoLogging;
import org.zstack.header.message.NeedReplyMessage;

/**
 * Request to ensure secret on KVM host (for VM e.g. vTPM).
 * Caller provides plaintext DEK (dekBase64), then host seals it with host public key
 * and forwards the envelope to key-agent.
 */
public class SecretHostDefineMsg extends NeedReplyMessage implements HostMessage {
    private String hostUuid;
    @NoLogging
    private String dekBase64;
    private String vmUuid;
    private String purpose;
    private Integer keyVersion;
    private String usageInstance;
    private String secretUuid;
    private String description;

    @Override
    public String getHostUuid() {
        return hostUuid;
    }

    public void setHostUuid(String hostUuid) {
        this.hostUuid = hostUuid;
    }

    public String getDekBase64() {
        return dekBase64;
    }

    public void setDekBase64(String dekBase64) {
        this.dekBase64 = dekBase64;
    }

    public String getVmUuid() {
        return vmUuid;
    }

    public void setVmUuid(String vmUuid) {
        this.vmUuid = vmUuid;
    }

    public String getPurpose() {
        return purpose;
    }

    public void setPurpose(String purpose) {
        this.purpose = purpose;
    }

    public Integer getKeyVersion() {
        return keyVersion;
    }

    public void setKeyVersion(Integer keyVersion) {
        this.keyVersion = keyVersion;
    }

    public String getUsageInstance() {
        return usageInstance;
    }

    public void setUsageInstance(String usageInstance) {
        this.usageInstance = usageInstance;
    }

    public String getSecretUuid() {
        return secretUuid;
    }

    public void setSecretUuid(String secretUuid) {
        this.secretUuid = secretUuid;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
