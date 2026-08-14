package org.zstack.header.host;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

/**
 * author:kaicai.hu
 * Date:2021/8/5
 */
@RestResponse
public class APILocateHostNetworkInterfaceEvent extends APIEvent {
    public APILocateHostNetworkInterfaceEvent() {
    }

    public APILocateHostNetworkInterfaceEvent(String apiId) {
        super(apiId);
    }

    public static APILocateHostNetworkInterfaceEvent __example__() {
        APILocateHostNetworkInterfaceEvent event = new APILocateHostNetworkInterfaceEvent();
        return event;
    }
}
