package org.zstack.crypto.keyprovider.nkp.api;

import org.springframework.http.HttpMethod;
import org.zstack.header.keyprovider.NkpVO;
import org.zstack.header.message.APIEvent;
import org.zstack.header.message.APIMessage;
import org.zstack.header.other.APIAuditor;
import org.zstack.header.rest.RestRequest;

@RestRequest(
        path = "/key-providers/nkp/actions",
        method = HttpMethod.PUT,
        responseClass = APIRestoreNkpEvent.class,
        isAction = true
)
public class APIRestoreNkpMsg extends APINkpRestoreMsg implements APIAuditor {
    @Override
    public Result audit(APIMessage msg, APIEvent rsp) {
        String uuid = "";
        if (rsp.isSuccess()) {
            APIRestoreNkpEvent evt = (APIRestoreNkpEvent) rsp;
            if (evt.getInventory() != null) {
                uuid = evt.getInventory().getUuid();
            }
        }
        return new APIAuditor.Result(uuid, NkpVO.class);
    }

    public static APIRestoreNkpMsg __example__() {
        APIRestoreNkpMsg msg = new APIRestoreNkpMsg();
        msg.setContentBase64("BASE64_ENCODED_NKP_BACKUP");
        msg.setPassword("password");
        return msg;
    }
}
