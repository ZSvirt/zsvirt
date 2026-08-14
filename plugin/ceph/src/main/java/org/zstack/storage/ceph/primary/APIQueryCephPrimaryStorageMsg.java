package org.zstack.storage.ceph.primary;

import org.springframework.http.HttpMethod;
import org.zstack.header.query.APIQueryMessage;
import org.zstack.header.query.AutoQuery;
import org.zstack.header.rest.RestRequest;
import org.zstack.header.storage.primary.APIQueryPrimaryStorageReply;

import java.util.Collections;
import java.util.List;

/**
 * Created by frank on 8/6/2015.
 */
@AutoQuery(replyClass = APIQueryPrimaryStorageReply.class, inventoryClass = CephPrimaryStorageInventory.class)
@RestRequest(
        path = "/primary-storage/ceph",
        optionalPaths = {"/primary-storage/ceph/{uuid}"},
        method = HttpMethod.GET,
        responseClass = APIQueryPrimaryStorageReply.class
)
public class APIQueryCephPrimaryStorageMsg extends APIQueryMessage {

    public static List<String> __example__() {
        return Collections.singletonList("uuid=" + uuid());
    }

}
