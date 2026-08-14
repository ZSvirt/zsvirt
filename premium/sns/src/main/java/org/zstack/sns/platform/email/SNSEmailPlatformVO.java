package org.zstack.sns.platform.email;

import org.zstack.sns.SNSApplicationPlatformVO;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.PrimaryKeyJoinColumn;
import javax.persistence.Table;

@Entity
@Table
@PrimaryKeyJoinColumn(name="uuid", referencedColumnName="uuid")
public class SNSEmailPlatformVO extends SNSApplicationPlatformVO {
    @Column
    private String smtpServer;
    @Column
    private int smtpPort;
    @Column
    private String username;
    @Column
    private String password;

    public SNSEmailPlatformVO() {
    }

    public SNSEmailPlatformVO(SNSApplicationPlatformVO other) {
        super(other);
    }

    public String getSmtpServer() {
        return smtpServer;
    }

    public void setSmtpServer(String smtpServer) {
        this.smtpServer = smtpServer;
    }

    public int getSmtpPort() {
        return smtpPort;
    }

    public void setSmtpPort(int smtpPort) {
        this.smtpPort = smtpPort;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
