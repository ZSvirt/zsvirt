package org.zstack.header.host;

import org.zstack.header.message.NeedReplyMessage;

public class UpdateHostNqnMsg extends NeedReplyMessage implements HostMessage  {
    private String uuid;
    private String nqn;

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getNqn() {
        return nqn;
    }

    public void setNqn(String nqn) {
        this.nqn = nqn;
    }

    @Override
    public String getHostUuid() {
        return uuid;
    }
}
