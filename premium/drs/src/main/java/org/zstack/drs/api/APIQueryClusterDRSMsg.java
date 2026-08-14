package org.zstack.drs.api;

import org.springframework.http.HttpMethod;
import org.zstack.drs.entity.ClusterDRSInventory;
import org.zstack.header.query.APIQueryMessage;
import org.zstack.header.query.AutoQuery;
import org.zstack.header.rest.RestRequest;

import java.util.List;

import static java.util.Arrays.asList;

/**
 * Created by lining on 2019/12/12.
 */
@AutoQuery(replyClass = APIQueryClusterDRSReply.class, inventoryClass = ClusterDRSInventory.class)
@RestRequest(
        path = "/clusters/drs",
        optionalPaths = {"/clusters/drs/{uuid}"},
        method = HttpMethod.GET,
        responseClass = APIQueryClusterDRSReply.class
)
public class APIQueryClusterDRSMsg extends APIQueryMessage {
    public static List<String> __example__() {
        return asList();
    }
}
