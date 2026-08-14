package org.zstack.header.volume.block;

import org.springframework.http.HttpMethod;
import org.zstack.header.query.APIQueryMessage;
import org.zstack.header.query.AutoQuery;
import org.zstack.header.rest.RestRequest;

import java.util.List;

import static java.util.Arrays.asList;

@AutoQuery(replyClass = APIQueryExponBlockVolumeReply.class, inventoryClass = ExponBlockVolumeInventory.class)
@RestRequest(
        path = "/expon/block-volumes",
        optionalPaths = {"/expon/block-volumes/{uuid}"},
        responseClass = APIQueryExponBlockVolumeReply.class,
        method = HttpMethod.GET
)
public class APIQueryExponBlockVolumeMsg extends APIQueryMessage {
    public static List<String> __example__() {
        return asList("uuid=xxx", "name=xxx");
    }
}
