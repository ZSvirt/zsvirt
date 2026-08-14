package org.zstack.storage.device.nvme;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIDeleteMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.RestRequest;

@RestRequest(
        path = "/storage-devices/nvme/servers/{uuid}",
        method = HttpMethod.DELETE,
        responseClass = APIDeleteNvmeServerEvent.class
)
public class APIDeleteNvmeServerMsg extends APIDeleteMessage {
    @APIParam(resourceType = NvmeServerVO.class)
    private String uuid;

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public static APIDeleteNvmeServerMsg __example__() {
        APIDeleteNvmeServerMsg msg = new APIDeleteNvmeServerMsg();
        msg.setUuid(uuid());
        return msg;
    }
}

