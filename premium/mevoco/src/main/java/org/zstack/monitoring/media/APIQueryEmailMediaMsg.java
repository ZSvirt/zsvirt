package org.zstack.monitoring.media;

import org.springframework.http.HttpMethod;
import org.zstack.header.query.APIQueryMessage;
import org.zstack.header.query.AutoQuery;
import org.zstack.header.rest.RestRequest;

import java.util.Arrays;
import java.util.List;

/**
 * Created by xing5 on 2017/7/9.
 */
@AutoQuery(replyClass = APIQueryMediaReply.class, inventoryClass = EmailMediaInventory.class)
@RestRequest(
        path = "/media/emails",
        optionalPaths = { "/media/emails/{uuid}"},
        method = HttpMethod.GET,
        responseClass = APIQueryMediaReply.class
)
@Deprecated
public class APIQueryEmailMediaMsg extends APIQueryMessage {
    public static List<String> __example__(){
        return Arrays.asList("name=test");
    }
}
