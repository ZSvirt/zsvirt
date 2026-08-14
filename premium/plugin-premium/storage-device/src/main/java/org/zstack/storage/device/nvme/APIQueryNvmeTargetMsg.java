package org.zstack.storage.device.nvme;

import org.springframework.http.HttpMethod;
import org.zstack.header.query.APIQueryMessage;
import org.zstack.header.query.AutoQuery;
import org.zstack.header.rest.RestRequest;

import java.util.List;

import static java.util.Arrays.asList;

@RestRequest(
        path = "/storage-devices/nvme/controllers",
        optionalPaths = {"/storage-devices/nvme",
                         "/storage-devices/nvme/controllers/{uuid}"},
        method = HttpMethod.GET,
        responseClass = APIQueryNvmeTargetReply.class
)
@AutoQuery(replyClass = APIQueryNvmeTargetReply.class, inventoryClass = NvmeTargetInventory.class)
public class APIQueryNvmeTargetMsg extends APIQueryMessage {

    public static List<String> __example__() {
        return asList();
    }
}
