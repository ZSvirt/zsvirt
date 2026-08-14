package org.zstack.billing.table;

import org.springframework.http.HttpMethod;
import org.zstack.billing.APIQueryResourcePriceReply;
import org.zstack.billing.PriceInventory;
import org.zstack.header.query.APIQueryMessage;
import org.zstack.header.query.AutoQuery;
import org.zstack.header.rest.RestRequest;

import java.util.List;

import static java.util.Arrays.asList;

/**
 * Created by lining on 2019/9/10.
 */
@AutoQuery(inventoryClass = PriceTableInventory.class, replyClass = APIQueryPriceTableRely.class)
@RestRequest(
        path = "/billings/price-tables",
        method = HttpMethod.GET,
        responseClass = APIQueryPriceTableRely.class
)
public class APIQueryPriceTableMsg extends APIQueryMessage {

    public static List<String> __example__() {
        return asList("uuid=" + uuid());
    }

}
