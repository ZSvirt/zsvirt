package org.zstack.monitoring.media;

import org.springframework.http.HttpMethod;
import org.zstack.header.query.APIQueryMessage;
import org.zstack.header.query.AutoQuery;
import org.zstack.header.rest.RestRequest;

import java.util.List;

import static java.util.Arrays.asList;

/**
 * Created by xing5 on 2017/6/18.
 */
@RestRequest(
        path = "/media",
        optionalPaths = {"/media/{uuid}"},
        method = HttpMethod.GET,
        responseClass = APIQueryMediaReply.class
)
@AutoQuery(replyClass = APIQueryMediaReply.class, inventoryClass = MediaInventory.class)
@Deprecated
public class APIQueryMediaMsg extends APIQueryMessage {
    public static List<String> __example__() {
        return asList("name=notification");
    }
}
