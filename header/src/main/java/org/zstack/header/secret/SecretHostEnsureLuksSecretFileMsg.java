package org.zstack.header.secret;

import org.zstack.header.host.HostMessage;
import org.zstack.header.log.NoLogging;
import org.zstack.header.message.NeedReplyMessage;

public class SecretHostEnsureLuksSecretFileMsg extends NeedReplyMessage implements HostMessage {
    private String hostUuid;
    @NoLogging
    private String dekBase64;

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
}
