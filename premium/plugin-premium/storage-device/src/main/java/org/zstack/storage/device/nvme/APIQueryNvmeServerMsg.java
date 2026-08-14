package org.zstack.storage.device.nvme;

import org.springframework.http.HttpMethod;
import org.zstack.header.query.APIQueryMessage;
import org.zstack.header.query.AutoQuery;
import org.zstack.header.rest.RestRequest;

import java.util.List;

import static java.util.Arrays.asList;

@RestRequest(
        path = "/storage-devices/nvme/servers",
        optionalPaths = {"/storage-devices/nvme/servers/{uuid}"},
        method = HttpMethod.GET,
        responseClass = APIQueryNvmeServerReply.class
)
@AutoQuery(replyClass = APIQueryNvmeServerReply.class, inventoryClass = NvmeServerInventory.class)
public class APIQueryNvmeServerMsg extends APIQueryMessage {

    public static List<String> __example__() {
        return asList();
    }
}
