package org.zstack.ipsec;

import org.springframework.http.HttpMethod;
import org.zstack.header.query.APIQueryMessage;
import org.zstack.header.query.AutoQuery;
import org.zstack.header.rest.RestRequest;

import java.util.List;

import static java.util.Arrays.asList;

/**
 * Created by xing5 on 2016/11/11.
 */
@AutoQuery(replyClass = APIQueryIPSecConnectionReply.class, inventoryClass = IPsecConnectionInventory.class)
@RestRequest(
        path = "/ipsec",
        optionalPaths = {"/ipsec/{uuid}"},
        method = HttpMethod.GET,
        responseClass = APIQueryIPSecConnectionReply.class
)
public class APIQueryIPSecConnectionMsg extends APIQueryMessage {

    public static List<String> __example__() {
        return asList();
    }

}
