package org.zstack.header.vm;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.RestRequest;

import java.util.Arrays;
import java.util.List;

/**
 * @ Author : yh.w
 * @ Date   : Created in 21:33 2021/2/2
 */
@RestRequest(
        path = "/vm-instances/capabilities",
        method = HttpMethod.POST,
        responseClass = APIGetVmsCapabilitiesEvent.class,
        parameterName = "params"
)
public class APIGetVmsCapabilitiesMsg extends APIMessage {
    @APIParam(nonempty = true)
    private List<String> vmUuids;

    public List<String> getVmUuids() {
        return vmUuids;
    }

    public void setVmUuids(List<String> vmUuids) {
        this.vmUuids = vmUuids;
    }

    public static APIGetVmsCapabilitiesMsg __example__() {
        APIGetVmsCapabilitiesMsg msg = new APIGetVmsCapabilitiesMsg();
        msg.setVmUuids(Arrays.asList(uuid()));
        return msg;
    }
}
