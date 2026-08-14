package org.zstack.header.host;

import org.zstack.header.message.NeedReplyMessage;

/**
 * Created by mingjian.deng on 2018/4/19.
 */
public class CheckMountDomainMsg extends NeedReplyMessage implements HostMessage {
    private String hostUuid;
    private String mountDomain;
    private long wait = 180000;

    public void setHostUuid(String hostUuid) {
        this.hostUuid = hostUuid;
    }

    public String getMountDomain() {
        return mountDomain;
    }

    public void setMountDomain(String mountDomain) {
        this.mountDomain = mountDomain;
    }

    public long getWait() {
        return wait;
    }

    public void setWait(long wait) {
        this.wait = wait;
    }

    @Override
    public String getHostUuid() {
        return hostUuid;
    }
}
