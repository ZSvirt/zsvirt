package org.zstack.crypto.keyprovider.kms.api;

import org.springframework.http.HttpMethod;
import org.zstack.crypto.keyprovider.api.APIDeleteKeyProviderMsg;
import org.zstack.header.keyprovider.KmsVO;
import org.zstack.header.message.APIEvent;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.message.OverriddenApiParam;
import org.zstack.header.message.OverriddenApiParams;
import org.zstack.header.other.APIAuditor;
import org.zstack.header.rest.RestRequest;

@RestRequest(
        path = "/key-providers/kms/{uuid}",
        method = HttpMethod.DELETE,
        responseClass = APIDeleteKmsEvent.class
)
@OverriddenApiParams({
        @OverriddenApiParam(field = "uuid", param = @APIParam(resourceType = KmsVO.class, successIfResourceNotExisting = true)),
})
public class APIDeleteKmsMsg extends APIDeleteKeyProviderMsg implements APIAuditor {
    @Override
    public Result audit(APIMessage msg, APIEvent rsp) {
        return new Result(((APIDeleteKmsMsg) msg).getUuid(), KmsVO.class);
    }

    public static APIDeleteKmsMsg __example__() {
        APIDeleteKmsMsg msg = new APIDeleteKmsMsg();
        msg.setUuid(uuid(KmsVO.class));
        return msg;
    }
}
