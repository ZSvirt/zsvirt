package org.zstack.drs.message;

import org.zstack.header.message.NeedReplyMessage;

import java.util.List;

/**
 * Created by lining on 2019/12/13.
 */
public class GetClusterHostsLoadMsg extends NeedReplyMessage {
    private String clusterUuid;
    //empty not allow
    private List<String> hostUuids;

    // allow empty
    private List<String> vmUuids;

    public String getClusterUuid() {
        return clusterUuid;
    }

    public void setClusterUuid(String clusterUuid) {
        this.clusterUuid = clusterUuid;
    }

    public List<String> getHostUuids() {
        return hostUuids;
    }

    public void setHostUuids(List<String> hostUuids) {
        this.hostUuids = hostUuids;
    }

    public List<String> getVmUuids() {
        return vmUuids;
    }

    public void setVmUuids(List<String> vmUuids) {
        this.vmUuids = vmUuids;
    }
}
