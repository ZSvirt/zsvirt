package org.zstack.sns;

import org.zstack.header.message.APICreateMessage;
import org.zstack.header.message.APIEvent;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.other.APIAuditor;

import java.util.List;

/**
 * Created by Qi Le on 2019-07-10
 */
public abstract class APICreateSNSSmsEndpointMsg extends APICreateSNSApplicationEndpointMsg implements SNSApplicationPlatformMessage {
    @APIParam(nonempty = true, required = false)
    private List<String> receivers;

    public List<String> getReceivers() {
        return receivers;
    }

    public void setReceivers(List<String> receivers) {
        this.receivers = receivers;
    }

    @Override
    public String getApplicationPlatformUuid() {
        return super.getPlatformUuid();
    }

    public String getApplicationEndpointType() {
        return null;
    }

}
