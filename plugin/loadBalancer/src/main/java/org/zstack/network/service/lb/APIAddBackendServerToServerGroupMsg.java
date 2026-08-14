package org.zstack.network.service.lb;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIEvent;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.other.APIAuditor;
import org.zstack.header.rest.APINoSee;
import org.zstack.header.rest.RestRequest;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestRequest(
        path = "/load-balancers/servergroups/{serverGroupUuid}/backendservers",
        method = HttpMethod.POST,
        parameterName = "params",
        responseClass = APIAddBackendServerToServerGroupEvent.class
)
public class APIAddBackendServerToServerGroupMsg extends APIMessage implements LoadBalancerMessage, APIAuditor {
    @APIParam(resourceType = LoadBalancerServerGroupVO.class, nonempty = true)
    private String serverGroupUuid;
    @APIParam(required = false)
    private List<Map<String,String>> vmNics;
    @APIParam(required = false)
    private List<Map<String,String>> servers;
    @APINoSee
    private String loadBalancerUuid;

    public String getServerGroupUuid() {
        return serverGroupUuid;
    }

    public void setServerGroupUuid(String serverGroupUuid) {
        this.serverGroupUuid = serverGroupUuid;
    }

    public List<Map<String, String>> getVmNics() {
        return vmNics;
    }

    public void setVmNics(List<Map<String, String>> vmNics) {
        this.vmNics = vmNics;
    }

    public List<Map<String, String>> getServers() {
        return servers;
    }

    public void setServers(List<Map<String, String>> servers) {
        this.servers = servers;
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
        return new Result(((APIAddBackendServerToServerGroupMsg)msg).getLoadBalancerUuid(), LoadBalancerVO.class);
    }

    public static APIAddBackendServerToServerGroupMsg __example__() {
        APIAddBackendServerToServerGroupMsg msg = new APIAddBackendServerToServerGroupMsg();
        Map<String,String> vmnicMap = new HashMap<String, String>() {
            {
                put("uuid", uuid());
                put("weight","20");
            }
        };

        List<Map<String, String>> vmnicList = new ArrayList<>();
        vmnicList.add(vmnicMap);

        Map<String,String> serverMap = new HashMap<String, String>() {
            {
                put("ipAddress", "192.168.2.1");
                put("weight","20");
            }
        };

        List<Map<String, String>> serverList = new ArrayList<>();
        serverList.add(serverMap);

        msg.setVmNics(vmnicList);
        msg.setServers(serverList);
        msg.setServerGroupUuid(uuid());

        return msg;
    }

}

