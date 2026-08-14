package org.zstack.zops;

import java.util.List;

public class CheckMultiHostsNetworkReachableManagementNodeMsg extends ManagementNodeMessage {
    List<String> targetHostname;

    public List<String> getTargetHostname() {
        return targetHostname;
    }

    public void setTargetHostname(List<String> targetHostname) {
        this.targetHostname = targetHostname;
    }
}
