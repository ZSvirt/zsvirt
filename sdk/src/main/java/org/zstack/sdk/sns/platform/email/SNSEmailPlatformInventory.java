package org.zstack.sdk.sns.platform.email;



public class SNSEmailPlatformInventory extends org.zstack.sdk.sns.SNSApplicationPlatformInventory {

    public java.lang.String smtpServer;
    public void setSmtpServer(java.lang.String smtpServer) {
        this.smtpServer = smtpServer;
    }
    public java.lang.String getSmtpServer() {
        return this.smtpServer;
    }

    public int smtpPort;
    public void setSmtpPort(int smtpPort) {
        this.smtpPort = smtpPort;
    }
    public int getSmtpPort() {
        return this.smtpPort;
    }

    public java.lang.String username;
    public void setUsername(java.lang.String username) {
        this.username = username;
    }
    public java.lang.String getUsername() {
        return this.username;
    }

}
