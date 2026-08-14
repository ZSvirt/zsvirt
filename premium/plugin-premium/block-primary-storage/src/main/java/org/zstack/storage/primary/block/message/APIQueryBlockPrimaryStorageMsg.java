package org.zstack.storage.primary.block.message;

import org.springframework.http.HttpMethod;
import org.zstack.header.query.APIQueryMessage;
import org.zstack.header.query.AutoQuery;
import org.zstack.header.rest.RestRequest;
import org.zstack.storage.primary.block.BlockPrimaryStorageInventory;

import java.util.List;

import static java.util.Arrays.asList;

/**
 * @author Lei Liu lei.liu@zstack.io
 * @date 2022/12/15 17:39
 */

@RestRequest(
        path = "/primary-storage/block",
        optionalPaths = {"/primary-storage/block/{uuid}"},
        method = HttpMethod.GET,
        responseClass = APIQueryBlockPrimaryStorageReply.class
)
@AutoQuery(replyClass = APIQueryBlockPrimaryStorageReply.class, inventoryClass = BlockPrimaryStorageInventory.class)
public class APIQueryBlockPrimaryStorageMsg extends APIQueryMessage {
    public static List<String> __example__() {
        return asList();
    }
}
