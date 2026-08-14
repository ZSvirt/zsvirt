package org.zstack.storage.device.iscsi;

import org.springframework.http.HttpMethod;
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
        method = HttpMethod.POST,
        responseClass = APIRefreshIscsiServerEvent.class,
        parameterName = "params"
)
public class APIRefreshIscsiServerMsg extends APIMessage implements APIAuditor {
    @APIParam(resourceType = IscsiServerVO.class)
    private String uuid;

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public static APIRefreshIscsiServerMsg __example__() {
        APIRefreshIscsiServerMsg msg = new APIRefreshIscsiServerMsg();
        msg.setUuid(uuid());
        return msg;
    }

    @Override
    public Result audit(APIMessage msg, APIEvent rsp) {
        return new Result(
                rsp.isSuccess() ? ((APIRefreshIscsiServerEvent) rsp).getInventory().getUuid() : "",
                IscsiServerVO.class
        );
    }
}
