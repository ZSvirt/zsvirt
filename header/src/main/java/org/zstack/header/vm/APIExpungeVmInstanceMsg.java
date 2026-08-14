package org.zstack.header.vm;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.message.DefaultTimeout;
import org.zstack.header.rest.RestRequest;

import java.util.concurrent.TimeUnit;

/**
 * Created by frank on 11/12/2015.
 */
@RestRequest(
        path = "/vm-instances/{uuid}/actions",
        isAction = true,
        responseClass = APIExpungeVmInstanceEvent.class,
        method = HttpMethod.PUT
)
@DefaultTimeout(timeunit = TimeUnit.HOURS, value = 3)
public class APIExpungeVmInstanceMsg extends APIMessage implements VmInstanceMessage {
    @APIParam(resourceType = VmInstanceVO.class)
    private String uuid;

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    @Override
    public String getVmInstanceUuid() {
        return uuid;
    }
 
    public static APIExpungeVmInstanceMsg __example__() {
        APIExpungeVmInstanceMsg msg = new APIExpungeVmInstanceMsg();
        msg.setUuid(uuid());
        return msg;
    }
}
