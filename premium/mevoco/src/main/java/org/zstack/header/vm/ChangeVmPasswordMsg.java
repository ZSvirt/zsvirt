package org.zstack.header.vm;

import org.zstack.header.log.NoLogging;
import org.zstack.header.message.NeedReplyMessage;

public class ChangeVmPasswordMsg extends NeedReplyMessage implements VmInstanceMessage {
    private String uuid;
    @NoLogging
    private String password;
    private String account;

    @Override
    public String getVmInstanceUuid() {
        return uuid;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getAccount() {
        return account;
    }

    public void setAccount(String account) {
        this.account = account;
    }
}
