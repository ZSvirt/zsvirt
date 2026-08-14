package org.zstack.search;

import org.zstack.header.message.APIReply;
import org.zstack.header.rest.RestResponse;

/**
 * @ Author : yh.w
 * @ Date   : Created in 19:26 2020/10/29
 */
@RestResponse
public class APIRefreshSearchIndexesReply extends APIReply {
    public APIRefreshSearchIndexesReply() {
    }

    public static APIRefreshSearchIndexesReply __example__() {
        APIRefreshSearchIndexesReply reply = new APIRefreshSearchIndexesReply();
        return reply;
    }
}
