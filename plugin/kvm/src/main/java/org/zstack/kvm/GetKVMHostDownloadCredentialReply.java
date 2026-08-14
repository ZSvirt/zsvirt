package org.zstack.kvm;

import org.zstack.header.message.MessageReply;

/**
 * Created by GuoYi on 8/26/18.
 */
public class GetKVMHostDownloadCredentialReply extends MessageReply {
    private String hostname;
    private String username;
    private String sshKey;
    private int sshPort;

    public String getHostname() {
        return hostname;
    }

    public void setHostname(String hostname) {
        this.hostname = hostname;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getSshKey() {
        return sshKey;
    }

    public void setSshKey(String sshKey) {
        this.sshKey = sshKey;
    }

    public int getSshPort() {
        return sshPort;
    }

    public void setSshPort(int sshPort) {
        this.sshPort = sshPort;
    }
}
