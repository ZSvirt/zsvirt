package org.zstack.network.service.lb;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIEvent;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.other.APIAuditor;
import org.zstack.header.rest.APINoSee;
import org.zstack.header.rest.RestRequest;
import org.zstack.header.acl.AccessControlListVO;

import java.util.Arrays;
import java.util.List;

/**
 * @author: zhanyong.miao
 * @date: 2020-03-11
 **/
@RestRequest(
        path = "/load-balancers/listeners/{listenerUuid}/access-control-lists",
        method = HttpMethod.DELETE,
        responseClass = APIRemoveAccessControlListFromLoadBalancerEvent.class
)
public class APIRemoveAccessControlListFromLoadBalancerMsg extends APIMessage implements LoadBalancerMessage, APIAuditor {
    @APIParam(resourceType = AccessControlListVO.class, nonempty = true)
    private List<String> aclUuids;
    @APINoSee
    private String loadBalancerUuid;
    @APIParam(resourceType = LoadBalancerListenerVO.class)
    private String listenerUuid;

    @APIParam(required = false, resourceType = LoadBalancerServerGroupVO.class)
    private List<String> serverGroupUuids;

    public List<String> getAclUuids() {
        return aclUuids;
    }

    public void setAclUuids(List<String> aclUuids) {
        this.aclUuids = aclUuids;
    }

    public String getListenerUuid() {
        return listenerUuid;
    }

    public void setListenerUuid(String listenerUuid) {
        this.listenerUuid = listenerUuid;
    }

    public List<String> getServerGroupUuids() {
        return serverGroupUuids;
    }

    public void setServerGroupUuids(List<String> serverGroupUuids) {
        this.serverGroupUuids = serverGroupUuids;
    }

    @Override
    public String getLoadBalancerUuid() {
        return loadBalancerUuid;
    }

    public void setLoadBalancerUuid(String loadBalancerUuid) {
        this.loadBalancerUuid = loadBalancerUuid;
    }

    public static APIRemoveAccessControlListFromLoadBalancerMsg __example__() {
        APIRemoveAccessControlListFromLoadBalancerMsg msg = new APIRemoveAccessControlListFromLoadBalancerMsg();

        msg.setListenerUuid(uuid());
        msg.setAclUuids(Arrays.asList(uuid()));

        return msg;
    }

    @Override
    public Result audit(APIMessage msg, APIEvent rsp) {
        return new Result(((APIRemoveAccessControlListFromLoadBalancerMsg)msg).loadBalancerUuid, LoadBalancerVO.class);
    }
}
