package org.zstack.header.vm;

import org.zstack.header.host.HostMessage;
import org.zstack.header.message.NeedReplyMessage;

/**
 * Created by Wenhao.Zhang on 23/10/16
 */
public class UpdateVmOnHypervisorMsg extends NeedReplyMessage implements HostMessage {
    private UpdateVmInstanceSpec spec;

    @Override
    public String getHostUuid() {
        return spec.getHostUuid();
    }

    public UpdateVmInstanceSpec getSpec() {
        return spec;
    }

    public void setSpec(UpdateVmInstanceSpec spec) {
        this.spec = spec;
    }
}
