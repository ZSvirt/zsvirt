package org.zstack.header.sshkeypair;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.*;
import org.zstack.header.rest.RestRequest;
import org.zstack.header.tag.TagResourceType;
import org.zstack.header.volume.VolumeVO;
import org.zstack.sshkeypair.SshKeyPairConstant;

@TagResourceType(VolumeVO.class)
@RestRequest(
        path = "/ssh-key-pair/generate",
        method = HttpMethod.POST,
        responseClass = APIGenerateSshKeyPairReply.class,
        parameterName = "params"
)
public class APIGenerateSshKeyPairMsg extends APISyncCallMessage {
    @APIParam(maxLength = 255, validRegexValues = SshKeyPairConstant.SSH_KEY_PAIR_NAME_REGEX)
    private String name;

    @APIParam(maxLength = 2048, required = false)
    private String description;

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

    public static APIGenerateSshKeyPairMsg __example__() {
        APIGenerateSshKeyPairMsg ret = new APIGenerateSshKeyPairMsg();
        ret.name = "ssh-key-pair";
        ret.description = "description";
        return ret;
    }
}
