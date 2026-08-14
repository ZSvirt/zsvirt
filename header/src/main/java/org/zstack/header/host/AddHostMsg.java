package org.zstack.header.host;

import org.zstack.header.cluster.ClusterVO;
import org.zstack.header.message.NeedReplyMessage;
import org.zstack.utils.verify.Param;
import org.zstack.utils.verify.Verifiable;

/**
 * Created by david on 9/12/16.
 */
public class AddHostMsg extends NeedReplyMessage implements AddHostMessage, Verifiable {
    private String name;
    private String description;
    @Param
    private String managementIp;
    @Param(resourceType = ClusterVO.class)
    private String clusterUuid;
    private String accountUuid;
    private String resourceUuid;

    public AddHostMsg() {
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getManagementIp() {
        return managementIp;
    }

    public void setManagementIp(String managementIp) {
        this.managementIp = managementIp;
    }

    public String getClusterUuid() {
        return clusterUuid;
    }

    public void setClusterUuid(String clusterUuid) {
        this.clusterUuid = clusterUuid;
    }

    public String getAccountUuid() {
        return accountUuid;
    }

    public void setAccountUuid(String accountUuid) {
        this.accountUuid = accountUuid;
    }

    public String getResourceUuid() {
        return resourceUuid;
    }

    public void setResourceUuid(String resourceUuid) {
        this.resourceUuid = resourceUuid;
    }

    public static AddHostMsg valueOf(final APIAddHostMsg msg) {
        AddHostMsg amsg = new AddHostMsg();

        amsg.setAccountUuid(msg.getSession().getAccountUuid());
        amsg.setName(msg.getName());
        amsg.setClusterUuid(msg.getClusterUuid());
        amsg.setDescription(msg.getDescription());
        amsg.setManagementIp(msg.getManagementIp());
        amsg.setResourceUuid(msg.getResourceUuid());
        return amsg;
    }
}
