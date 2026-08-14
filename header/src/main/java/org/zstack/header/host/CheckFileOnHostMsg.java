package org.zstack.header.host;

import org.zstack.header.message.NeedReplyMessage;

import java.util.List;

/**
 * Check whether the specified file exists on the host,
 * and return MD5 if `md5Return` is true
 */
public class CheckFileOnHostMsg extends NeedReplyMessage implements HostMessage {
    private String hostUuid;
    private List<String> paths;
    private boolean md5Return = false;

    @Override
    public String getHostUuid() {
        return hostUuid;
    }

    public void setHostUuid(String hostUuid) {
        this.hostUuid = hostUuid;
    }

    public List<String> getPaths() {
        return paths;
    }

    public void setPaths(List<String> paths) {
        this.paths = paths;
    }

    public boolean isMd5Return() {
        return md5Return;
    }

    public void setMd5Return(boolean md5Return) {
        this.md5Return = md5Return;
    }
}
