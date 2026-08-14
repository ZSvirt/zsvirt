package org.zstack.network.l2.vxlan.vtep;

import org.zstack.header.message.NeedReplyMessage;
import org.zstack.header.network.l2.L2NetworkMessage;

/**
 * Created by weiwang on 21/04/2017.
 */
public class CreateVtepMsg extends NeedReplyMessage implements L2NetworkMessage {
    private String vtepIp;
    private String hostUuid;
    private String clusterUuid;
    private Integer port;
    private String type;
    private String poolUuid;
    private String physicalInterface;

    public String getPoolUuid() {
        return poolUuid;
    }

    public void setPoolUuid(String poolUuid) {
        this.poolUuid = poolUuid;
    }

    public String getVtepIp() {
        return vtepIp;
    }

    public void setVtepIp(String vtepIp) {
        this.vtepIp = vtepIp;
    }

    public String getHostUuid() {
        return hostUuid;
    }

    public void setHostUuid(String hostUuid) {
        this.hostUuid = hostUuid;
    }

    public String getClusterUuid() {
        return clusterUuid;
    }

    public void setClusterUuid(String clusterUuid) {
        this.clusterUuid = clusterUuid;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Integer getPort() {
        return port;
    }

    public void setPort(Integer port) {
        this.port = port;
    }

    public String getPhysicalInterface() {
        return physicalInterface;
    }

    public void setPhysicalInterface(String physicalInterface) {
        this.physicalInterface = physicalInterface;
    }

    @Override
    public String getL2NetworkUuid() {
        return getPoolUuid();
    }
}
