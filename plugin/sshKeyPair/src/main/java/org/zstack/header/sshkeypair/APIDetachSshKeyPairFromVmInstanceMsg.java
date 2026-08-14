package org.zstack.header.sshkeypair;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.RestRequest;
import org.zstack.header.vm.VmInstanceVO;

@RestRequest(
        path = "/ssh-key-pair/{sshKeyPairUuid}/vm-instance/{vmInstanceUuid}",
        method = HttpMethod.DELETE,
        responseClass = APIDetachSshKeyPairFromVmInstanceEvent.class
)
public class APIDetachSshKeyPairFromVmInstanceMsg extends APIMessage implements SshKeyPairMessage {
    @APIParam(resourceType = VmInstanceVO.class)
    private String vmInstanceUuid;

    @APIParam(resourceType = SshKeyPairVO.class)
    private String sshKeyPairUuid;

    public String getVmInstanceUuid() {
        return vmInstanceUuid;
    }

    public void setVmInstanceUuid(String vmInstanceUuid) {
        this.vmInstanceUuid = vmInstanceUuid;
    }

    @Override
    public String getSshKeyPairUuid() {
        return sshKeyPairUuid;
    }

    public void setSshKeyPairUuid(String sshKeyPairUuid) {
        this.sshKeyPairUuid = sshKeyPairUuid;
    }

    public static APIDetachSshKeyPairFromVmInstanceMsg __example__() {
        APIDetachSshKeyPairFromVmInstanceMsg msg = new APIDetachSshKeyPairFromVmInstanceMsg();
        msg.setVmInstanceUuid(uuid());
        msg.setSshKeyPairUuid(uuid());

        return msg;
    }
}
