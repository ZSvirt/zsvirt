package org.zstack.header.sshkeypair;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIEvent;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.other.APIAuditor;
import org.zstack.header.rest.RestRequest;

@RestRequest(
        path = "/ssh-key-pair/{uuid}",
        method = HttpMethod.DELETE,
        responseClass = APIDeleteSshKeyPairEvent.class
)
public class APIDeleteSshKeyPairMsg extends APIMessage implements SshKeyPairMessage, APIAuditor {
    @APIParam(resourceType = SshKeyPairVO.class)
    private String uuid;

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    @Override
    public String getSshKeyPairUuid() { return uuid; }

    public static APIDeleteSshKeyPairMsg __example__() {
        APIDeleteSshKeyPairMsg msg = new APIDeleteSshKeyPairMsg();

        msg.setUuid(uuid());
        return msg;
    }

    @Override
    public Result audit(APIMessage msg, APIEvent rsp) {
        return new APIAuditor.Result(((APIDeleteSshKeyPairMsg)msg).getUuid(), SshKeyPairVO.class);
    }
}
