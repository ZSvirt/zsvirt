
package org.zstack.header.volume.block;

import org.springframework.http.HttpMethod;
import org.zstack.header.query.APIQueryMessage;
import org.zstack.header.query.AutoQuery;
import org.zstack.header.rest.RestRequest;

import java.util.List;

import static java.util.Arrays.asList;

@AutoQuery(replyClass = APIQueryBlockVolumeReply.class, inventoryClass = BlockVolumeInventory.class)
@RestRequest(
        path = "/block-volumes",
        optionalPaths = {"/block-volumes/{uuid}"},
        responseClass = APIQueryBlockVolumeReply.class,
        method = HttpMethod.GET
)
public class APIQueryBlockVolumeMsg extends APIQueryMessage {
    public static List<String> __example__() {
        return asList("uuid=xxx", "name=xxx");
    }
}
