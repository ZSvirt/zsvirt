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
        method = HttpMethod.POST,
        parameterName = "params",
        responseClass = APIAddAccessControlListToLoadBalancerEvent.class
)
public class APIAddAccessControlListToLoadBalancerMsg extends APIMessage implements LoadBalancerMessage, APIAuditor {
    @APIParam(resourceType = AccessControlListVO.class, nonempty = true)
    private List<String> aclUuids;
    @APIParam(validValues = {"white","black","redirect"})
    private String aclType;
    @APIParam(resourceType = LoadBalancerListenerVO.class)
    private String listenerUuid;
    @APIParam(resourceType = LoadBalancerServerGroupVO.class, required = false)
    private List<String> serverGroupUuids;
    @APINoSee
    private String loadBalancerUuid;


    public List<String> getAclUuids() {
        return aclUuids;
    }

    public void setAclUuids(List<String> aclUuids) {
        this.aclUuids = aclUuids;
    }

    public String getAclType() {
        return aclType;
    }

    public void setAclType(String aclType) {
        this.aclType = aclType;
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

    public static APIAddAccessControlListToLoadBalancerMsg __example__() {
        APIAddAccessControlListToLoadBalancerMsg msg = new APIAddAccessControlListToLoadBalancerMsg();

        msg.setAclUuids(Arrays.asList(uuid()));
        msg.setListenerUuid(uuid());
        msg.setLoadBalancerUuid(uuid());
        msg.setAclType(LoadBalancerAclType.black.toString());

        return msg;
    }

    @Override
    public APIAuditor.Result audit(APIMessage msg, APIEvent rsp) {
        return new Result(((APIAddAccessControlListToLoadBalancerMsg)msg).getLoadBalancerUuid(), LoadBalancerVO.class);
    }
}