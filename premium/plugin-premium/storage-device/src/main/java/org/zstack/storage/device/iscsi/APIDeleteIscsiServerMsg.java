package org.zstack.storage.device.iscsi;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIDeleteMessage;
import org.zstack.header.message.APIEvent;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.other.APIAuditor;
import org.zstack.header.rest.RestRequest;

/**
 * Create by weiwang at 2018/8/2
 */
@RestRequest(
        path = "/storage-devices/iscsi/servers/{uuid}",
        method = HttpMethod.DELETE,
        responseClass = APIDeleteIscsiServerEvent.class
)
public class APIDeleteIscsiServerMsg extends APIDeleteMessage {
    @APIParam(resourceType = IscsiServerVO.class)
    private String uuid;

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public static APIDeleteIscsiServerMsg __example__() {
        APIDeleteIscsiServerMsg msg = new APIDeleteIscsiServerMsg();
        msg.setUuid(uuid());
        return msg;
    }
}
