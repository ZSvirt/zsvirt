package org.zstack.header.baremetal.chassis;

import org.zstack.header.cluster.ClusterVO;
import org.zstack.header.log.NoLogging;
import org.zstack.header.message.NeedReplyMessage;
import org.zstack.utils.verify.Param;
import org.zstack.utils.verify.Verifiable;

/**
 * Created by GuoYi on 2018-10-08.
 */
public class CreateBaremetalChassisMsg extends NeedReplyMessage implements Verifiable {
    @Param(maxLength = 255)
    private String name;
    @Param(required = false, maxLength = 2048)
    private String description;
    @Param(resourceType = ClusterVO.class)
    private String clusterUuid;
    @Param
    private String ipmiAddress;
    @Param(required = false, numberRange = {1, 65535})
    private Integer ipmiPort = 623;
    @Param(maxLength = 255)
    private String ipmiUsername;
    @Param(maxLength = 255)
    @NoLogging
    private String ipmiPassword;

    // default not to power reset baremetal chassis automatically
    @Param(required = false)
    private Boolean reboot = false;

    public Boolean getReboot() {
        return reboot;
    }

    public void setReboot(Boolean reboot) {
        this.reboot = reboot;
    }

    public void setReboot(String reboot) {
        if (reboot != null && reboot.trim().toLowerCase().startsWith("y")) {
            this.reboot = true;
        }
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

    public String getClusterUuid() {
        return clusterUuid;
    }

    public void setClusterUuid(String clusterUuid) {
        this.clusterUuid = clusterUuid;
    }

    public String getIpmiAddress() {
        return ipmiAddress;
    }

    public void setIpmiAddress(String ipmiAddress) {
        this.ipmiAddress = ipmiAddress;
    }

    public Integer getIpmiPort() {
        return ipmiPort;
    }

    public void setIpmiPort(Integer ipmiPort) {
        this.ipmiPort = ipmiPort;
    }

    public String getIpmiUsername() {
        return ipmiUsername;
    }

    public void setIpmiUsername(String ipmiUsername) {
        this.ipmiUsername = ipmiUsername;
    }

    public String getIpmiPassword() {
        return ipmiPassword;
    }

    public void setIpmiPassword(String ipmiPassword) {
        this.ipmiPassword = ipmiPassword;
    }

    public static CreateBaremetalChassisMsg valueOf(APICreateBaremetalChassisMsg msg) {
        CreateBaremetalChassisMsg cmsg = new CreateBaremetalChassisMsg();
        cmsg.setName(msg.getName());
        cmsg.setDescription(msg.getDescription());
        cmsg.setClusterUuid(msg.getClusterUuid());
        cmsg.setIpmiAddress(msg.getIpmiAddress());
        cmsg.setIpmiPort(msg.getIpmiPort());
        cmsg.setIpmiUsername(msg.getIpmiUsername());
        cmsg.setIpmiPassword(msg.getIpmiPassword());
        return cmsg;
    }
}
