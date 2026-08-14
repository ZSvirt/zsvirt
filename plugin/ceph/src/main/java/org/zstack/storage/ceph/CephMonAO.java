package org.zstack.storage.ceph;

import org.zstack.core.convert.PasswordConverter;
import org.zstack.header.vo.ResourceVO;

import javax.persistence.*;
import java.sql.Timestamp;

/**
 * Created by frank on 7/27/2015.
 */
@MappedSuperclass
public class CephMonAO extends ResourceVO {
    @Column
    private String sshUsername;

    @Column
    @Convert(converter = PasswordConverter.class)
    private String sshPassword;

    @Column
    private String hostname;

    @Column
    private String monAddr;

    @Column
    private int sshPort = 22;

    @Column
    private int monPort = 6789;

    @Column
    @Enumerated(EnumType.STRING)
    private MonStatus status;

    @Column
    private Timestamp createDate;

    @Column
    private Timestamp lastOpDate;

    @PreUpdate
    private void preUpdate() {
        lastOpDate = null;
    }

    public String getMonAddr() {
        return monAddr;
    }

    public void setMonAddr(String monAddr) {
        this.monAddr = monAddr;
    }

    public int getSshPort() {
        return sshPort;
    }

    public void setSshPort(int sshPort) {
        this.sshPort = sshPort;
    }

    public int getMonPort() {
        return monPort;
    }

    public void setMonPort(int monPort) {
        this.monPort = monPort;
    }

    public String getSshUsername() {
        return sshUsername;
    }

    public void setSshUsername(String sshUsername) {
        this.sshUsername = sshUsername;
    }

    public String getSshPassword() {
        return sshPassword;
    }

    public void setSshPassword(String sshPassword) {
        this.sshPassword = sshPassword;
    }

    public String getHostname() {
        return hostname;
    }

    public void setHostname(String hostname) {
        this.hostname = hostname;
    }

    public MonStatus getStatus() {
        return status;
    }

    public void setStatus(MonStatus status) {
        this.status = status;
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
