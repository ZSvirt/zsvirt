package org.zstack.snmp.agent;

import org.zstack.header.vo.Index;
import org.zstack.snmp.agent.mib.SnmpAgentStatus;

import javax.persistence.*;
import java.sql.Timestamp;

/**
 * @Author : jingwang
 * @create 2023/7/20 1:51 PM
 */
@Entity
@Table
public class SnmpAgentVO {
    @Id
    @Column
    @Index
    private String uuid;
    @Column
    @Enumerated(EnumType.STRING)
    private SnmpAgentVersion version;
    @Column
    private String readCommunity;
    @Column
    private String userName;
    @Column
    @Enumerated(EnumType.STRING)
    private SnmpAgentAuthAlgorithm authAlgorithm;
    @Column
    private String authPassword;
    @Column
    @Enumerated(EnumType.STRING)
    private SnmpAgentPrivacyAlgorithm privacyAlgorithm;
    @Column
    private String privacyPassword;
    @Column
    private int port;
    @Column
    @Enumerated(EnumType.STRING)
    private SnmpAgentStatus status;
    @Column
    @Enumerated(EnumType.STRING)
    private SecurityLevel securityLevel = SecurityLevel.undefined;
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

    public SnmpAgentVersion getVersion() {
        return version;
    }

    public void setVersion(SnmpAgentVersion version) {
        this.version = version;
    }

    public String getReadCommunity() {
        return readCommunity;
    }

    public void setReadCommunity(String readCommunity) {
        this.readCommunity = readCommunity;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getAuthPassword() {
        return authPassword;
    }

    public void setAuthPassword(String authenticationPassphrase) {
        this.authPassword = authenticationPassphrase;
    }

    public SnmpAgentAuthAlgorithm getAuthAlgorithm() {
        return authAlgorithm;
    }

    public void setAuthAlgorithm(SnmpAgentAuthAlgorithm authAlgorithm) {
        this.authAlgorithm = authAlgorithm;
    }

    public SnmpAgentPrivacyAlgorithm getPrivacyAlgorithm() {
        return privacyAlgorithm;
    }

    public void setPrivacyAlgorithm(SnmpAgentPrivacyAlgorithm privacyAlgorithm) {
        this.privacyAlgorithm = privacyAlgorithm;
    }

    public String getPrivacyPassword() {
        return privacyPassword;
    }

    public void setPrivacyPassword(String privacyPassphrase) {
        this.privacyPassword = privacyPassphrase;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public SnmpAgentStatus getStatus() {
        return status;
    }

    public void setStatus(SnmpAgentStatus status) {
        this.status = status;
    }

    public SecurityLevel getSecurityLevel() {
        return securityLevel;
    }

    public void setSecurityLevel(SecurityLevel securityLevel) {
        this.securityLevel = securityLevel;
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
