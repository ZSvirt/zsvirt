package org.zstack.header.network.service;

import org.zstack.header.message.APIEvent;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.network.l2.L2NetworkVO;

public class APIDetachNetworkServiceProviderFromL2NetworkMsg extends APIMessage {
    @APIParam
    private String networkServiceProviderUuid;
    @APIParam
    private String l2NetworkUuid;

    public String getNetworkServiceProviderUuid() {
        return networkServiceProviderUuid;
    }

    public String getL2NetworkUuid() {
        return l2NetworkUuid;
    }

    public void setL2NetworkUuid(String l2NetworkUuid) {
        this.l2NetworkUuid = l2NetworkUuid;
    }

    public void setNetworkServiceProviderUuid(String networkServiceProviderUuid) {
        this.networkServiceProviderUuid = networkServiceProviderUuid;
    }
 
    public static APIDetachNetworkServiceProviderFromL2NetworkMsg __example__() {
        APIDetachNetworkServiceProviderFromL2NetworkMsg msg = new APIDetachNetworkServiceProviderFromL2NetworkMsg();
        return msg;
    }
}
