package org.zstack.drs.api;

import org.springframework.http.HttpMethod;
import org.zstack.drs.entity.ClusterDRSInventory;
import org.zstack.drs.entity.DRSAdviceInventory;
import org.zstack.header.query.APIQueryMessage;
import org.zstack.header.query.AutoQuery;
import org.zstack.header.rest.RestRequest;

import java.util.List;

import static java.util.Arrays.asList;

/**
 * Created by lining on 2019/12/12.
 */
@AutoQuery(replyClass = APIQueryDRSAdviceReply.class, inventoryClass = DRSAdviceInventory.class)
@RestRequest(
        path = "/clusters/drs/advice",
        optionalPaths = {"/clusters/drs/advice/{uuid}"},
        method = HttpMethod.GET,
        responseClass = APIQueryDRSAdviceReply.class
)
public class APIQueryDRSAdviceMsg extends APIQueryMessage {
    public static List<String> __example__() {
        return asList();
    }
}
