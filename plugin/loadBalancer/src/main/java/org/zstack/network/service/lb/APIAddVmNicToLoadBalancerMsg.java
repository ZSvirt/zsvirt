package org.zstack.network.service.lb;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIEvent;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.other.APIAuditor;
import org.zstack.header.rest.APINoSee;
import org.zstack.header.rest.RestRequest;
import org.zstack.header.vm.VmNicVO;

import java.util.Arrays;
import java.util.List;

/**
 * Created by frank on 8/8/2015.
 */
@RestRequest(
        path = "/load-balancers/listeners/{listenerUuid}/vm-instances/nics",
        method = HttpMethod.POST,
        parameterName = "params",
        responseClass = APIAddVmNicToLoadBalancerEvent.class
)
public class APIAddVmNicToLoadBalancerMsg extends APIMessage implements LoadBalancerMessage, APIAuditor {
    @APIParam(resourceType = VmNicVO.class, nonempty = true)
    private List<String> vmNicUuids;
    @APIParam(resourceType = LoadBalancerListenerVO.class)
    private String listenerUuid;
    @APINoSee
    private String loadBalancerUuid;

    public List<String> getVmNicUuids() {
        return vmNicUuids;
    }

    public void setVmNicUuids(List<String> vmNicUuids) {
        this.vmNicUuids = vmNicUuids;
    }

    public String getListenerUuid() {
        return listenerUuid;
    }

    public void setListenerUuid(String listenerUuid) {
        this.listenerUuid = listenerUuid;
    }

    @Override
    public String getLoadBalancerUuid() {
        return loadBalancerUuid;
    }

    public void setLoadBalancerUuid(String loadBalancerUuid) {
        this.loadBalancerUuid = loadBalancerUuid;
    }
 
    public static APIAddVmNicToLoadBalancerMsg __example__() {
        APIAddVmNicToLoadBalancerMsg msg = new APIAddVmNicToLoadBalancerMsg();

        msg.setVmNicUuids(Arrays.asList(uuid()));
        msg.setListenerUuid(uuid());
        msg.setLoadBalancerUuid(uuid());

        return msg;
    }

    @Override
    public Result audit(APIMessage msg, APIEvent rsp) {
        return new Result(((APIAddVmNicToLoadBalancerMsg)msg).getLoadBalancerUuid(), LoadBalancerVO.class);
    }
}
