package org.zstack.crypto.keyprovider.nkp.api;

import org.springframework.http.HttpMethod;
import org.zstack.crypto.keyprovider.api.APIDeleteKeyProviderMsg;
import org.zstack.header.keyprovider.NkpVO;
import org.zstack.header.message.APIEvent;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.message.OverriddenApiParam;
import org.zstack.header.message.OverriddenApiParams;
import org.zstack.header.other.APIAuditor;
import org.zstack.header.rest.RestRequest;

@RestRequest(
        path = "/key-providers/nkp/{uuid}",
        method = HttpMethod.DELETE,
        responseClass = APIDeleteNkpEvent.class
)
@OverriddenApiParams({
        @OverriddenApiParam(field = "uuid", param = @APIParam(resourceType = NkpVO.class, successIfResourceNotExisting = true)),
})
public class APIDeleteNkpMsg extends APIDeleteKeyProviderMsg implements APIAuditor {
    @Override
    public Result audit(APIMessage msg, APIEvent rsp) {
        return new Result(((APIDeleteNkpMsg) msg).getUuid(), NkpVO.class);
    }

    public static APIDeleteNkpMsg __example__() {
        APIDeleteNkpMsg msg = new APIDeleteNkpMsg();
        msg.setUuid(uuid(NkpVO.class));
        return msg;
    }
}
