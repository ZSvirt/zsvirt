package org.zstack.header.vm;

import org.zstack.header.message.NeedReplyMessage;

/**
 * Created by MaJin on 2017-07-05.
 */
public class ReimageVmInstanceMsg extends NeedReplyMessage implements VmInstanceMessage{
    private String vmInstanceUuid;
    private String accountUuid;
    @Override
    public String getVmInstanceUuid() {
        return vmInstanceUuid;
    }

    public void setVmInstanceUuid(String vmInstanceUuid) {
        this.vmInstanceUuid = vmInstanceUuid;
    }

    public void setAccountUuid(String accountUuid) {
        this.accountUuid = accountUuid;
    }

    public String getAccountUuid() {
        return accountUuid;
    }
}
