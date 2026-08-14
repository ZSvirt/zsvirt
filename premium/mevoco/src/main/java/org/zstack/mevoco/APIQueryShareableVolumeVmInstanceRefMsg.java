package org.zstack.mevoco;

import org.springframework.http.HttpMethod;
import org.zstack.header.query.APIQueryMessage;
import org.zstack.header.query.AutoQuery;
import org.zstack.header.rest.RestRequest;
import java.util.List;

import static java.util.Arrays.asList;

@AutoQuery(replyClass = APIQueryShareableVolumeVmInstanceRefReply.class,
        inventoryClass = ShareableVolumeVmInstanceRefInventory.class)
@RestRequest(
        path = "/volumes/vm-instances/refs",
        method = HttpMethod.GET,
        responseClass = APIQueryShareableVolumeVmInstanceRefReply.class
)
public class APIQueryShareableVolumeVmInstanceRefMsg extends APIQueryMessage {

    public static List<String> __example__() {
        return asList("0c917562d5954544b37bf5e0ed0735a6","cddc1c17e94a43b5b50e1df53e076d11","2","Jan 22, 2017 2:41:37 PM","Jan 22, 2017 2:43:18 PM" );
    }

}
