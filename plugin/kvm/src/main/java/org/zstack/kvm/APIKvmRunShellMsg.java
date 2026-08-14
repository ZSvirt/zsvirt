package org.zstack.kvm;

import org.springframework.http.HttpMethod;
import org.zstack.header.host.HostVO;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.RestRequest;

import java.util.HashSet;
import java.util.Set;



/**
 * Created by xing5 on 2016/3/14.
 */
@RestRequest(
        path = "/hosts/kvm/actions",
        isAction = true,
        method = HttpMethod.PUT,
        responseClass = APIKvmRunShellEvent.class
)
public class APIKvmRunShellMsg extends APIMessage {
    @APIParam(resourceType = HostVO.class, nonempty = true)
    private Set<String> hostUuids;
    @APIParam
    private String script;

    public Set<String> getHostUuids() {
        return hostUuids;
    }

    public void setHostUuids(Set<String> hostUuids) {
        this.hostUuids = hostUuids;
    }

    public String getScript() {
        return script;
    }

    public void setScript(String script) {
        this.script = script;
    }
 
    public static APIKvmRunShellMsg __example__() {
        APIKvmRunShellMsg msg = new APIKvmRunShellMsg();
        Set <String >set = new HashSet();
        set.add(uuid());
        set.add(uuid());
        msg.setHostUuids(set);
        msg.setScript("ls");
        return msg;
    }

}
