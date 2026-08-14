package org.zstack.header.host;

import org.zstack.header.vo.EntityGraph;
import org.zstack.header.vo.ForeignKey;

import javax.persistence.*;

/**
 * @Author : jingwang
 * @create 2023/4/13 10:32 AM
 */
@Entity
@Table
@EntityGraph(
        parents = {
                @EntityGraph.Neighbour(type = HostVO.class, myField = "uuid", targetField = "uuid")
        }
)
public class HostIpmiVO {
    @Id
    @Column
    @ForeignKey(parentEntityClass = HostEO.class, onDeleteAction = ForeignKey.ReferenceOption.CASCADE)
    private String uuid;

    @Column
    private String ipmiAddress;

    @Column
    private String ipmiUsername;

    @Column
    private int ipmiPort;

    @Column
    private String ipmiPassword;

    @Column
    @Enumerated(EnumType.STRING)
    private HostPowerStatus ipmiPowerStatus;

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getIpmiAddress() {
        return ipmiAddress;
    }

    public void setIpmiAddress(String ipmiAddress) {
        this.ipmiAddress = ipmiAddress;
    }

    public String getIpmiUsername() {
        return ipmiUsername;
    }

    public void setIpmiUsername(String ipmiUsername) {
        this.ipmiUsername = ipmiUsername;
    }

    public int getIpmiPort() {
        return ipmiPort;
    }

    public void setIpmiPort(int ipmiPort) {
        this.ipmiPort = ipmiPort;
    }

    public String getIpmiPassword() {
        return ipmiPassword;
    }

    public void setIpmiPassword(String ipmiPassword) {
        this.ipmiPassword = ipmiPassword;
    }

    public HostPowerStatus getIpmiPowerStatus() {
        return ipmiPowerStatus;
    }

    public void setIpmiPowerStatus(HostPowerStatus ipmiPowerStatus) {
        this.ipmiPowerStatus = ipmiPowerStatus;
    }
}
