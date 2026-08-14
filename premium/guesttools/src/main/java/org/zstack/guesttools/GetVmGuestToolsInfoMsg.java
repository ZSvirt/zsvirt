package org.zstack.guesttools;

import org.zstack.header.message.NeedReplyMessage;
import org.zstack.header.vm.VmInstanceMessage;

import java.util.Set;

public class GetVmGuestToolsInfoMsg extends NeedReplyMessage implements VmInstanceMessage {

    private String uuid;

    private Set<String> debug;

    private boolean readMetricOnly = false;

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    @Override
    public String getVmInstanceUuid() {
        return uuid;
    }

    public Set<String> getDebug() {
        return debug;
    }

    public void setDebug(Set<String> debug) {
        this.debug = debug;
    }

    public boolean isReadMetricOnly() {
        return readMetricOnly;
    }

    public void setReadMetricOnly(boolean readMetricOnly) {
        this.readMetricOnly = readMetricOnly;
    }
}
