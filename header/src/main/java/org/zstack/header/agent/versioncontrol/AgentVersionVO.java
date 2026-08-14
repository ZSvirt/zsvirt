package org.zstack.header.agent.versioncontrol;

import javax.persistence.*;
import java.sql.Timestamp;


@Table
@Entity
public class AgentVersionVO {
    @Id
    @Column
    private String uuid;

    @Column
    private String agentType;

    @Column
    private String currentVersion;

    @Column
    private String expectVersion;

    @Column
    private Timestamp createDate;

    @Column
    private Timestamp lastOpDate;

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getAgentType() {
        return agentType;
    }

    public void setAgentType(String agentType) {
        this.agentType = agentType;
    }

    public String getCurrentVersion() {
        return currentVersion;
    }

    public void setCurrentVersion(String currentVersion) {
        this.currentVersion = currentVersion;
    }

    public String getExpectVersion() {
        return expectVersion;
    }

    public void setExpectVersion(String expectVersion) {
        this.expectVersion = expectVersion;
    }

    public Timestamp getCreateDate() {
        return createDate;
    }

    public void setCreateDate(Timestamp createDate) {
        this.createDate = createDate;
    }

    public Timestamp getLastOpDate() {
        return lastOpDate;
    }

    public void setLastOpDate(Timestamp lastOpDate) {
        this.lastOpDate = lastOpDate;
    }
}
