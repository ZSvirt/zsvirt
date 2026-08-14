package org.zstack.kvm;

import org.zstack.header.host.AddHostMsg;
import org.zstack.header.log.NoLogging;
import org.zstack.utils.verify.Param;

import java.io.Serializable;

public class AddKVMHostMsg extends AddHostMsg implements AddKVMHostMessage, Serializable {
    @Param
    private String username;
    @Param
    @NoLogging
    private String password;
    @Param(numberRange = {1, 65535}, required = false)
    private int sshPort = 22;

    @Override
    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    @Override
    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    @Override
    public int getSshPort() {
        return sshPort;
    }

    public void setSshPort(int sshPort) {
        this.sshPort = sshPort;
    }
}
