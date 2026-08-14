package org.zstack.storage.device.nvme;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIEvent;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.other.APIAuditor;
import org.zstack.header.rest.RestRequest;

@RestRequest(
        path = "/storage-devices/nvme/servers/{uuid}/actions",
        method = HttpMethod.PUT,
        responseClass = APIRefreshNvmeServerEvent.class,
        isAction = true
)
public class APIRefreshNvmeServerMsg extends APIMessage implements APIAuditor {
    @APIParam(resourceType = NvmeServerVO.class)
    private String uuid;

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public static APIRefreshNvmeServerMsg __example__() {
        APIRefreshNvmeServerMsg msg = new APIRefreshNvmeServerMsg();
        msg.setUuid(uuid());
        return msg;
    }

    @Override
    public Result audit(APIMessage msg, APIEvent rsp) {
        return new Result(
                rsp.isSuccess() ? ((APIRefreshNvmeServerEvent) rsp).getInventory().getUuid() : "",
                NvmeServerVO.class
        );
    }
}
