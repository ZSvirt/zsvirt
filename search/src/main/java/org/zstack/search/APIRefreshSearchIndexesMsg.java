package org.zstack.search;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APISyncCallMessage;
import org.zstack.header.rest.RestRequest;

/**
 * @ Author : yh.w
 * @ Date   : Created in 19:23 2020/10/29
 */
@RestRequest(path = "/search/indexes/refresh",
        method = HttpMethod.GET,
        responseClass = APIRefreshSearchIndexesReply.class
)
public class APIRefreshSearchIndexesMsg extends APISyncCallMessage {

    public static APIRefreshSearchIndexesMsg __example__() {
        APIRefreshSearchIndexesMsg msg = new APIRefreshSearchIndexesMsg();
        return msg;
    }
}
