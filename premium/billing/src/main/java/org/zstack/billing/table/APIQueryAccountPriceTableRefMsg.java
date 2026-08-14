package org.zstack.billing.table;

import org.springframework.http.HttpMethod;
import org.zstack.header.query.APIQueryMessage;
import org.zstack.header.query.AutoQuery;
import org.zstack.header.rest.RestRequest;

import java.util.List;

import static java.util.Arrays.asList;

/**
 * Created by lining on 2019/9/10.
 */
@AutoQuery(inventoryClass = AccountPriceTableRefInventory.class, replyClass = APIQueryPriceTableRely.class)
@RestRequest(
        path = "/accounts/price-tables/refs",
        method = HttpMethod.GET,
        responseClass = APIQueryAccountPriceTableRefReply.class
)
public class APIQueryAccountPriceTableRefMsg extends APIQueryMessage {

    public static List<String> __example__() {
        return asList("uuid=" + uuid());
    }

}
