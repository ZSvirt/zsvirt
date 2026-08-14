package org.zstack.kvm;

import org.zstack.header.host.HostMessage;
import org.zstack.header.message.NeedReplyMessage;

/**
 * Created by GuoYi on 8/26/18.
 */
public class GetKVMHostDownloadCredentialMsg extends NeedReplyMessage implements HostMessage {
    private String hostUuid;
    private String dataNetworkCidr;

    public void setHostUuid(String hostUuid) {
        this.hostUuid = hostUuid;
    }

    @Override
    public String getHostUuid() {
        return hostUuid;
    }

    public String getDataNetworkCidr() {
        return dataNetworkCidr;
    }

    public void setDataNetworkCidr(String dataNetworkCidr) {
        this.dataNetworkCidr = dataNetworkCidr;
    }
}
