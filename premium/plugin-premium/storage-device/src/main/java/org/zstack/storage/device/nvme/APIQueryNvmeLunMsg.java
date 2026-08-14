package org.zstack.storage.device.nvme;

import org.springframework.http.HttpMethod;
import org.zstack.header.query.APIQueryMessage;
import org.zstack.header.query.AutoQuery;
import org.zstack.header.rest.RestRequest;

import java.util.List;

import static java.util.Arrays.asList;

@RestRequest(
        path = "/storage-devices/nvme/luns",
        optionalPaths = {"/storage-devices/nvme/luns/{uuid}"},
        method = HttpMethod.GET,
        responseClass = APIQueryNvmeLunReply.class
)
@AutoQuery(replyClass = APIQueryNvmeLunReply.class, inventoryClass = NvmeLunInventory.class)
public class APIQueryNvmeLunMsg extends APIQueryMessage {

    public static List<String> __example__() {
        return asList();
    }
}
