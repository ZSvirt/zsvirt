package org.zstack.network.securitygroup;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.RestRequest;
import org.zstack.header.vm.VmNicVO;

import java.util.List;
import static java.util.Arrays.asList;

@RestRequest(
        path = "/security-groups/nics/{vmNicUuid}/actions",
        method = HttpMethod.PUT,
        responseClass = APISetVmNicSecurityGroupEvent.class,
        isAction = true
)
public class APISetVmNicSecurityGroupMsg extends APIMessage implements VmNicSecurityGroupMessage {
    @APIParam(resourceType = VmNicVO.class, nonempty = true, required = true)
    private String vmNicUuid;

    @APIParam(required = true)
    private List<VmNicSecurityGroupRefAO> refs;

    public static class VmNicSecurityGroupRefAO {
        @APIParam(resourceType = SecurityGroupVO.class, required = true, nonempty = true)
        private String securityGroupUuid;

        @APIParam(required = true, nonempty = true)
        private Integer priority;

        public String getSecurityGroupUuid() {
            return securityGroupUuid;
        }

        public void setSecurityGroupUuid(String securityGroupUuid) {
            this.securityGroupUuid = securityGroupUuid;
        }

        public Integer getPriority() {
            return priority;
        }

        public void setPriority(Integer priority) {
            this.priority = priority;
        }
    }

    @Override
    public String getVmNicUuid() {
        return vmNicUuid;
    }

    public void setVmNicUuid(String vmNicUuid) {
        this.vmNicUuid = vmNicUuid;
    }

    public List<VmNicSecurityGroupRefAO> getRefs() {
        return refs;
    }

    public void setRefs(List<VmNicSecurityGroupRefAO> refs) {
        this.refs = refs;
    }

    public static APISetVmNicSecurityGroupMsg __example__() {
        APISetVmNicSecurityGroupMsg msg = new APISetVmNicSecurityGroupMsg();
        VmNicSecurityGroupRefAO ref = new VmNicSecurityGroupRefAO();
        ref.setSecurityGroupUuid(uuid());
        ref.setPriority(1);
        msg.setVmNicUuid(uuid());
        msg.setRefs(asList(ref));
        return msg;
    }
}
