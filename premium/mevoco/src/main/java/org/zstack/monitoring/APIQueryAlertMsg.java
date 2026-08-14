package org.zstack.monitoring;

import org.springframework.http.HttpMethod;
import org.zstack.header.query.APIQueryMessage;
import org.zstack.header.query.AutoQuery;
import org.zstack.header.rest.RestRequest;

import java.util.List;

import static java.util.Arrays.asList;

/**
 * Created by xing5 on 2017/6/18.
 */
@AutoQuery(replyClass = APIQueryAlertReply.class, inventoryClass = AlertInventory.class)
@RestRequest(
        path = "/monitoring/alerts",
        optionalPaths = {"/monitoring/alerts/{uuid}"},
        method = HttpMethod.GET,
        responseClass = APIQueryAlertReply.class
)
@Deprecated
public class APIQueryAlertMsg extends APIQueryMessage {
    public static List<String> __example__() {
        return asList("uuid="+uuid());
    }
}
