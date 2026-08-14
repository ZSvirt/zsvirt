package org.zstack.header.managementnode;

import javax.persistence.*;
import java.sql.Timestamp;

@Entity
@Table
public class ManagementNodeVO {
    @Id
    @Column
    private String uuid;

    @Column
    private String hostName;

    @Column
    private Timestamp joinDate;

    @Column
    private Timestamp heartBeat;

    @Column
    @Enumerated(EnumType.STRING)
    private ManagementNodeState state = ManagementNodeState.JOINING;

    @Column
    private int port;

    public String getHostName() {
        return hostName;
    }

    public void setHostName(String hostName) {
        this.hostName = hostName;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public Timestamp getJoinDate() {
        return joinDate;
    }

    public Timestamp getHeartBeat() {
        return heartBeat;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public ManagementNodeState getState() {
        return state;
    }

    public void setState(ManagementNodeState state) {
        this.state = state;
    }

    public void setJoinDate(Timestamp joinDate) {
        this.joinDate = joinDate;
    }

    public void setHeartBeat(Timestamp heartBeat) {
        this.heartBeat = heartBeat;
    }
}
