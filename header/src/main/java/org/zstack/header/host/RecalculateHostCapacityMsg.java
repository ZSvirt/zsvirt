package org.zstack.header.host;

import org.zstack.header.message.NeedReplyMessage;

/**
 * Created by frank on 10/15/2015.
 */
public class RecalculateHostCapacityMsg extends NeedReplyMessage {
    private String zoneUuid;
    private String clusterUuid;
    private String hostUuid;

    public String getHostUuid() {
        return hostUuid;
    }

    public void setHostUuid(String hostUuid) {
        this.hostUuid = hostUuid;
    }

    public String getZoneUuid() {
        return zoneUuid;
    }

    public void setZoneUuid(String zoneUuid) {
        this.zoneUuid = zoneUuid;
    }

    public String getClusterUuid() {
        return clusterUuid;
    }

    public void setClusterUuid(String clusterUuid) {
        this.clusterUuid = clusterUuid;
    }
}
