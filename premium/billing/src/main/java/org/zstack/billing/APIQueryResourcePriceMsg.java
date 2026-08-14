package org.zstack.billing;

import org.springframework.http.HttpMethod;
import org.zstack.header.query.APIQueryMessage;
import org.zstack.header.query.AutoQuery;
import org.zstack.header.rest.RestRequest;

import java.util.List;

import static java.util.Arrays.asList;

/**
 * Created by xing5 on 2016/5/14.
 */
@AutoQuery(inventoryClass = PriceInventory.class, replyClass = APIQueryResourcePriceReply.class)
@RestRequest(
        path = "/billings/prices",
        optionalPaths = {"/billing/prices/{uuid}"},
        method = HttpMethod.GET,
        responseClass = APIQueryResourcePriceReply.class
)
public class APIQueryResourcePriceMsg extends APIQueryMessage {

    public static List<String> __example__() {
        return asList("uuid=" + uuid());
    }

}
