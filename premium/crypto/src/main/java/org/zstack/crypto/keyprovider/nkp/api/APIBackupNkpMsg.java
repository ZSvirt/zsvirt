package org.zstack.crypto.keyprovider.nkp.api;

import org.springframework.http.HttpMethod;
import org.zstack.crypto.keyprovider.KeyProviderMessage;
import org.zstack.header.keyprovider.NkpVO;
import org.zstack.header.log.NoLogging;
import org.zstack.header.message.APIEvent;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.other.APIAuditor;
import org.zstack.header.rest.RestRequest;

@RestRequest(
        path = "/key-providers/nkp/{uuid}/actions",
        method = HttpMethod.PUT,
        responseClass = APIBackupNkpEvent.class,
        isAction = true
)
public class APIBackupNkpMsg extends APIMessage implements KeyProviderMessage, APIAuditor {
    @APIParam(resourceType = NkpVO.class, emptyString = false)
    private String uuid;

    @APIParam(required = false, maxLength = 255, emptyString = false)
    @NoLogging
    private String password;

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    @Override
    public String getKeyProviderUuid() {
        return uuid;
    }

    @Override
    public Result audit(APIMessage msg, APIEvent rsp) {
        return new APIAuditor.Result(((APIBackupNkpMsg) msg).getUuid(), NkpVO.class);
    }

    public static APIBackupNkpMsg __example__() {
        APIBackupNkpMsg msg = new APIBackupNkpMsg();
        msg.setUuid(uuid(NkpVO.class));
        msg.setPassword("password");
        return msg;
    }
}
