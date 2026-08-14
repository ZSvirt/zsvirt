package org.zstack.header.sshkeypair;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIEvent;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.other.APIAuditor;
import org.zstack.header.rest.RestRequest;
import org.zstack.sshkeypair.SshKeyPairConstant;

@RestRequest(
        path = "/ssh-key-pair/{uuid}/actions",
        method = HttpMethod.PUT,
        responseClass = APIUpdateSshKeyPairEvent.class,
        isAction = true
)
public class APIUpdateSshKeyPairMsg extends APIMessage implements SshKeyPairMessage, APIAuditor {
    @APIParam(resourceType = SshKeyPairVO.class, maxLength = 32)
    private String uuid;

    @APIParam(maxLength = 255, required = false, validRegexValues = SshKeyPairConstant.SSH_KEY_PAIR_NAME_REGEX)
    private String name;

    @APIParam(maxLength = 2048, required = false)
    private String description;

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }


    public static APIUpdateSshKeyPairMsg __example__() {
        APIUpdateSshKeyPairMsg ret = new APIUpdateSshKeyPairMsg();
        ret.uuid = uuid(SshKeyPairVO.class);
        ret.name = "ssh-key-pair";
        ret.description = "description";
        return ret;
    }

    @Override
    public Result audit(APIMessage msg, APIEvent rsp) {
        return new APIAuditor.Result(((APIUpdateSshKeyPairMsg)msg).getUuid(), SshKeyPairVO.class);
    }

    @Override
    public String getSshKeyPairUuid() {
        return uuid;
    }
}
