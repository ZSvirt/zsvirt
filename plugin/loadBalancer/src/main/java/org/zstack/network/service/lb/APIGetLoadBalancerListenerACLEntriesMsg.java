package org.zstack.network.service.lb;

import org.springframework.http.HttpMethod;
import org.zstack.header.acl.AccessControlListEntryInventory;
import org.zstack.header.message.APIParam;
import org.zstack.header.message.APISyncCallMessage;
import org.zstack.header.rest.RestRequest;

import java.util.List;

@RestRequest(
        path = "/load-balancers/listeners/access-control-lists/entries",
        method = HttpMethod.GET,
        responseClass = APIGetLoadBalancerListenerACLEntriesReply.class
)
public class APIGetLoadBalancerListenerACLEntriesMsg extends APISyncCallMessage {
    @APIParam(resourceType = LoadBalancerListenerVO.class, required = false)
    private List<String> listenerUuids;

    @APIParam(required = false, validValues = {"RedirectRule", "IpEntry"})
    private String type;

    public List<String> getListenerUuids() {
        return listenerUuids;
    }

    public void setListenerUuids(List<String> listenerUuids) {
        this.listenerUuids = listenerUuids;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public static APIGetLoadBalancerListenerACLEntriesReply __example__() {
        APIGetLoadBalancerListenerACLEntriesReply reply = new APIGetLoadBalancerListenerACLEntriesReply();
        AccessControlListEntryInventory inv = new AccessControlListEntryInventory();
        inv.setUuid(uuid());
        inv.setUrl("/test");
        inv.setDomain("zstack.io");
        inv.setName("test");
        inv.setAclUuid(uuid());
        inv.setType("redirect");
        return reply;
    }
}
