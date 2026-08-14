package org.zstack.header.host;

import org.zstack.header.message.MessageReply;
import org.zstack.header.vm.VmAccountPreference;

import java.io.Serializable;

/**
 * Created by mingjian.deng on 16/10/19.
 */
public class ChangeVmPasswordReply extends MessageReply implements Serializable {
    private VmAccountPreference vmAccountPreference;

    public VmAccountPreference getVmAccountPreference() {
        return vmAccountPreference;
    }

    public void setVmAccountPreference(VmAccountPreference vmAccountPreference) {
        this.vmAccountPreference = vmAccountPreference;
    }
}
