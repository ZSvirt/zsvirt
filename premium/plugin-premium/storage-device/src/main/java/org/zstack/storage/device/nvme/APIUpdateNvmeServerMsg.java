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
        isAction = true,
        responseClass = APIUpdateNvmeServerEvent.class
)
public class APIUpdateNvmeServerMsg extends APIMessage implements APIAuditor {
    @APIParam(resourceType = NvmeServerVO.class)
    private String uuid;

    @APIParam(required = false, maxLength = 256, emptyString = false)
    private String name;

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public static APIUpdateNvmeServerMsg __example__() {
        APIUpdateNvmeServerMsg msg = new APIUpdateNvmeServerMsg();
        msg.setUuid(uuid());
        msg.setName("nvme-server-10.0.0.201");
        return msg;
    }

    @Override
    public Result audit(APIMessage msg, APIEvent rsp) {
        return new Result(
                rsp.isSuccess() ? ((APIUpdateNvmeServerEvent) rsp).getInventory().getUuid() : "",
                NvmeServerVO.class
        );
    }
}
