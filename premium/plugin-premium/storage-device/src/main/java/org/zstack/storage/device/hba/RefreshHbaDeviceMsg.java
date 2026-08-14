package org.zstack.storage.device.hba;

import org.zstack.header.message.NeedReplyMessage;

/**
 * @Author: qiuyu.zhang
 * @Date: 2024/9/18 16:39
 */
public class RefreshHbaDeviceMsg extends NeedReplyMessage {
    private String hostUuid;

    public String getHostUuid() {
        return hostUuid;
    }

    public void setHostUuid(String hostUuid) {
        this.hostUuid = hostUuid;
    }
}
