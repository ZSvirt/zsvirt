package org.zstack.network.l2.virtualSwitch.header;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIDeleteMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.RestRequest;


@RestRequest(
        path = "/l3-networks/kernel-interfaces/{uuid}",
        method = HttpMethod.DELETE,
        responseClass = APIDeleteHostKernelInterfaceEvent.class
)
public class APIDeleteHostKernelInterfaceMsg extends APIDeleteMessage implements HostKernelInterfaceMessage {
    @APIParam(resourceType = HostKernelInterfaceVO.class, emptyString = false)
    private String uuid;

    @Override
    public String getHostKernelInterfaceUuid() {
        return uuid;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public static APIDeleteHostKernelInterfaceMsg __example__() {
        APIDeleteHostKernelInterfaceMsg msg = new APIDeleteHostKernelInterfaceMsg();
        msg.setUuid(uuid(HostKernelInterfaceVO.class));
        return msg;
    }

}
