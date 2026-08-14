package org.zstack.network.securitygroup;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.RestRequest;
import org.zstack.header.vm.VmNicVO;

@RestRequest(
        path = "/security-groups/nics/{vmNicUuid}/security-policy/actions",
        method = HttpMethod.PUT,
        responseClass = APIChangeVmNicSecurityPolicyEvent.class,
        isAction = true
)
public class APIChangeVmNicSecurityPolicyMsg extends APIMessage implements VmNicSecurityGroupMessage {
    @APIParam(resourceType = VmNicVO.class)
    private String vmNicUuid;

    @APIParam(required = false, validValues = {"DENY", "ALLOW"})
    private String ingressPolicy;

    @APIParam(required = false, validValues = {"DENY", "ALLOW"})
    private String egressPolicy;

    @Override
    public String getVmNicUuid() {
        return vmNicUuid;
    }

    public void setVmNicUuid(String vmNicUuid) {
        this.vmNicUuid = vmNicUuid;
    }

    public String getIngressPolicy() {
        return ingressPolicy;
    }

    public void setIngressPolicy(String ingressPolicy) {
        this.ingressPolicy = ingressPolicy;
    }

    public String getEgressPolicy() {
        return egressPolicy;
    }

    public void setEgressPolicy(String egressPolicy) {
        this.egressPolicy = egressPolicy;
    }

    public static APIChangeVmNicSecurityPolicyMsg __example__() {
        APIChangeVmNicSecurityPolicyMsg msg = new APIChangeVmNicSecurityPolicyMsg();
        msg.vmNicUuid = uuid();
        msg.ingressPolicy = "ALLOW";
        msg.egressPolicy = "DENY";
        return msg;
    }
}
