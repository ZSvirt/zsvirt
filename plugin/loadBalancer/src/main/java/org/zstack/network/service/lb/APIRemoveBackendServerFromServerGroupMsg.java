package org.zstack.network.service.lb;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIEvent;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.other.APIAuditor;
import org.zstack.header.rest.APINoSee;
import org.zstack.header.rest.RestRequest;
import org.zstack.header.vm.VmNicVO;

import java.util.List;

@RestRequest(
        path = "/load-balancers/servergroups/{serverGroupUuid}/backendservers/actions",
        isAction = true,
        method = HttpMethod.PUT,
        responseClass = APIRemoveBackendServerFromServerGroupEvent.class
)
public class APIRemoveBackendServerFromServerGroupMsg extends APIMessage implements LoadBalancerMessage, APIAuditor {
    @APIParam(resourceType = LoadBalancerServerGroupVO.class, nonempty = true)
    private String serverGroupUuid;
    @APIParam(resourceType = VmNicVO.class, required = false)
    private List<String> vmNicUuids;
    @APIParam(required = false)
    private List<String> serverIps;
    @APINoSee
    private String loadBalancerUuid;

    public String getServerGroupUuid() {
        return serverGroupUuid;
    }

    public void setServerGroupUuid(String serverGroupUuid) {
        this.serverGroupUuid = serverGroupUuid;
    }

    public List<String> getVmNicUuids() {
        return vmNicUuids;
    }

    public void setVmNicUuids(List<String> vmNicUuids) {
        this.vmNicUuids = vmNicUuids;
    }

    public List<String> getServerIps() {
        return serverIps;
    }

    public void setServerIps(List<String> serverIps) {
        this.serverIps = serverIps;
    }

    @Override
    public String getLoadBalancerUuid() {
        return loadBalancerUuid;
    }

    public void setLoadBalancerUuid(String loadBalancerUuid) {
        this.loadBalancerUuid = loadBalancerUuid;
    }

    public static APIRemoveBackendServerFromServerGroupMsg __example__() {
        APIRemoveBackendServerFromServerGroupMsg msg = new APIRemoveBackendServerFromServerGroupMsg();
        msg.setLoadBalancerUuid(uuid());
        msg.setServerGroupUuid(uuid());
        return msg;
    }

    @Override
    public Result audit(APIMessage msg, APIEvent rsp) {
        return new Result(((APIRemoveBackendServerFromServerGroupMsg)msg).getLoadBalancerUuid(), LoadBalancerVO.class);
    }

}
