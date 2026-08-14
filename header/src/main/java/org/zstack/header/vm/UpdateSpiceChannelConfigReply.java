package org.zstack.header.vm;

import org.zstack.header.message.MessageReply;

/**
 * author:kaicai.hu
 * Date:2019/9/17
 */
public class UpdateSpiceChannelConfigReply extends MessageReply {
    private boolean restartLibvirt;

    public boolean isRestartLibvirt() {
        return restartLibvirt;
    }

    public void setRestartLibvirt(boolean restartLibvirt) {
        this.restartLibvirt = restartLibvirt;
    }
}
