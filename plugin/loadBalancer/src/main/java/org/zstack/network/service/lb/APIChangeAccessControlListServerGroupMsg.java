package org.zstack.network.service.lb;

import org.springframework.http.HttpMethod;
import org.zstack.header.acl.AccessControlListVO;
import org.zstack.header.message.APIEvent;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.other.APIAuditor;
import org.zstack.header.rest.APINoSee;
import org.zstack.header.rest.RestRequest;

import java.util.Arrays;
import java.util.List;

@RestRequest(
        path = "/load-balancers/listener/acl/{aclUuid}/servergroup/actions",
        method = HttpMethod.PUT,
        responseClass = APIChangeAccessControlListServerGroupEvent.class,
        isAction = true
)
public class APIChangeAccessControlListServerGroupMsg extends APIMessage implements LoadBalancerMessage, APIAuditor {
    @APIParam(resourceType = LoadBalancerServerGroupVO.class, nonempty = true)
    private List<String> serverGroupUuids;
    @APIParam(resourceType = LoadBalancerListenerVO.class)
    private String listenerUuid;
    @APIParam(resourceType = AccessControlListVO.class)
    private String aclUuid;

    @APINoSee
    private String loadBalancerUuid;

    public List<String> getServerGroupUuids() {
        return serverGroupUuids;
    }

    public void setServerGroupUuids(List<String> serverGroupUuid) {
        this.serverGroupUuids = serverGroupUuid;
    }

    public String getListenerUuid() {
        return listenerUuid;
    }

    public void setListenerUuid(String listenerUuid) {
        this.listenerUuid = listenerUuid;
    }

    public String getAclUuid() {
        return aclUuid;
    }

    public void setAclUuid(String aclUuid) {
        this.aclUuid = aclUuid;
    }

    @Override
    public String getLoadBalancerUuid() {
        return loadBalancerUuid;
    }

    public void setLoadBalancerUuid(String loadBalancerUuid) {
        this.loadBalancerUuid = loadBalancerUuid;
    }

    @Override
    public Result audit(APIMessage msg, APIEvent rsp) {
        return new APIAuditor.Result(((APIChangeAccessControlListServerGroupMsg)msg).getLoadBalancerUuid(), LoadBalancerVO.class);
    }

    public static APIChangeAccessControlListServerGroupMsg __example__() {
        APIChangeAccessControlListServerGroupMsg msg = new APIChangeAccessControlListServerGroupMsg();
        msg.setAclUuid(uuid());
        msg.setListenerUuid(uuid());
        msg.setServerGroupUuids(Arrays.asList(uuid()));

        return msg;
    }
}
