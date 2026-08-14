package org.zstack.storage.backup.imagestore;

import org.zstack.header.log.NoLogging;
import org.zstack.header.message.MessageReply;

import java.io.Serializable;

public class GetImageStoreBackupStorageDownloadCredentialReply extends MessageReply implements Serializable {
    private String username;
    private String hostname;
    private int sshPort;
    @NoLogging
    private String password;

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getHostname() {
        return hostname;
    }

    public void setHostname(String hostname) {
        this.hostname = hostname;
    }

    public int getSshPort() {
        return sshPort;
    }

    public void setSshPort(int sshPort) {
        this.sshPort = sshPort;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
