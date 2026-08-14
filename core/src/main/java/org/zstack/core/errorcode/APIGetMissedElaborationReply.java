package org.zstack.core.errorcode;

import org.zstack.header.message.APIReply;
import org.zstack.header.rest.RestResponse;

/**
 * Created by mingjian.deng on 2018/12/3.
 */
@RestResponse
@Deprecated
public class APIGetMissedElaborationReply extends APIReply {
    public static APIGetMissedElaborationReply __example__() {
        APIGetMissedElaborationReply reply = new APIGetMissedElaborationReply();

        return reply;
    }
}
