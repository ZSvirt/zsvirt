package org.zstack.header.baremetal.instance;

import org.zstack.header.message.NeedReplyMessage;

/**
 * Created by GuoYi on 7/6/18.
 */
public class StartBaremetalInstanceMsg extends NeedReplyMessage implements BaremetalInstanceMessage {
    private String uuid;
    private Boolean reboot = false;
    private Boolean pxeBoot = false;

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public Boolean getReboot() {
        return reboot;
    }

    public void setReboot(Boolean reboot) {
        this.reboot = reboot;
    }

    public Boolean getPxeBoot() {
        return pxeBoot;
    }

    public void setPxeBoot(Boolean pxeBoot) {
        this.pxeBoot = pxeBoot;
    }

    @Override
    public String getBaremetalInstanceUuid() {
        return getUuid();
    }
}
