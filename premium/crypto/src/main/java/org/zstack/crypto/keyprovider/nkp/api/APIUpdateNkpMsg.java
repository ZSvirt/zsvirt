package org.zstack.crypto.keyprovider.nkp.api;

import org.springframework.http.HttpMethod;
import org.zstack.crypto.keyprovider.api.APIUpdateKeyProviderMsg;
import org.zstack.header.keyprovider.NkpVO;
import org.zstack.header.message.APIEvent;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.message.OverriddenApiParam;
import org.zstack.header.message.OverriddenApiParams;
import org.zstack.header.other.APIAuditor;
import org.zstack.header.rest.RestRequest;

@RestRequest(
        path = "/key-providers/nkp/{uuid}/actions",
        method = HttpMethod.PUT,
        responseClass = APIUpdateNkpEvent.class,
        isAction = true
)
@OverriddenApiParams({
        @OverriddenApiParam(field = "uuid", param = @APIParam(resourceType = NkpVO.class, emptyString = false)),
})
public class APIUpdateNkpMsg extends APIUpdateKeyProviderMsg implements APIAuditor {
    @Override
    public Result audit(APIMessage msg, APIEvent rsp) {
        return new APIAuditor.Result(((APIUpdateNkpMsg) msg).getUuid(), NkpVO.class);
    }

    public static APIUpdateNkpMsg __example__() {
        APIUpdateNkpMsg msg = new APIUpdateNkpMsg();
        msg.setUuid(uuid(NkpVO.class));
        msg.setDescription("example");
        return msg;
    }
}
