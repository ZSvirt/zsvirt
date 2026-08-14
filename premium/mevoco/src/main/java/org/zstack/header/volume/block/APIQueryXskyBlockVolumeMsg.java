package org.zstack.header.volume.block;

import org.springframework.http.HttpMethod;
import org.zstack.header.query.APIQueryMessage;
import org.zstack.header.query.AutoQuery;
import org.zstack.header.rest.RestRequest;

import java.util.List;

import static java.util.Arrays.asList;

/**
 * @author shenjin
 * @date 2023/6/24 15:35
 */
@AutoQuery(replyClass = APIQueryXskyBlockVolumeReply.class, inventoryClass = XskyBlockVolumeInventory.class)
@RestRequest(
        path = "/xksy/block-volumes",
        optionalPaths = {"/xsky/block-volumes/{uuid}"},
        responseClass = APIQueryXskyBlockVolumeReply.class,
        method = HttpMethod.GET
)
public class APIQueryXskyBlockVolumeMsg extends APIQueryMessage {
    public static List<String> __example__() {
        return asList("uuid=xxx", "name=xxx");
    }
}
