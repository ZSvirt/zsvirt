package org.zstack.storage.ceph.primary;

import org.springframework.http.HttpMethod;
import org.zstack.header.query.APIQueryMessage;
import org.zstack.header.query.AutoQuery;
import org.zstack.header.rest.RestRequest;

import java.util.List;

import static java.util.Arrays.asList;

/**
 * Created by xing5 on 2017/2/28.
 */
@RestRequest(
        path = "/primary-storage/ceph/pools",
        optionalPaths = {"/primary-storage/ceph/pools/{uuid}"},
        method = HttpMethod.GET,
        responseClass = APIQueryCephPrimaryStoragePoolReply.class
)
@AutoQuery(replyClass = APIQueryCephPrimaryStoragePoolReply.class, inventoryClass = CephPrimaryStoragePoolInventory.class)
public class APIQueryCephPrimaryStoragePoolMsg extends APIQueryMessage {

    public static List<String> __example__() {
        return asList("name=highPerformance");
    }
}
