package org.zstack.header.baremetal.pxeserver;

import org.zstack.header.message.NeedReplyMessage;

/**
 * Created by GuoYi on 2018-10-15.
 */
public class PingBaremetalPxeServerMsg extends NeedReplyMessage implements BaremetalPxeServerMessage {
    private String uuid;

    // if enabled, then check and make sure dnsmasq/nginx etc is running
    private boolean enabled;

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    @Override
    public String getPxeServerUuid() {
        return uuid;
    }
}
