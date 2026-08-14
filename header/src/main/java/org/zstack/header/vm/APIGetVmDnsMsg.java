package org.zstack.header.vm;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIParam;
import org.zstack.header.message.APISyncCallMessage;
import org.zstack.header.rest.RestRequest;
import org.zstack.utils.network.IPv6Constants;

@RestRequest(
        path = "/vm-instances/{vmInstanceUuid}/dns",
        method = HttpMethod.GET,
        responseClass = APIGetVmDnsReply.class
)
public class APIGetVmDnsMsg extends APISyncCallMessage implements VmInstanceMessage {
    @APIParam(resourceType = VmInstanceVO.class)
    private String vmInstanceUuid;

    @APIParam(resourceType = VmNicVO.class, required = false)
    private String vmNicUuid;

    @APIParam(required = false, validValues = {"4", "6"})
    private Integer ipVersion;

    @Override
    public String getVmInstanceUuid() {
        return vmInstanceUuid;
    }

    public void setVmInstanceUuid(String vmInstanceUuid) {
        this.vmInstanceUuid = vmInstanceUuid;
    }

    public String getVmNicUuid() {
        return vmNicUuid;
    }

    public void setVmNicUuid(String vmNicUuid) {
        this.vmNicUuid = vmNicUuid;
    }

    public Integer getIpVersion() {
        return ipVersion;
    }

    public void setIpVersion(Integer ipVersion) {
        this.ipVersion = ipVersion;
    }

    public static APIGetVmDnsMsg __example__() {
        APIGetVmDnsMsg msg = new APIGetVmDnsMsg();
        msg.setVmInstanceUuid(uuid(VmInstanceVO.class));
        msg.setVmNicUuid(uuid(VmNicVO.class));
        msg.setIpVersion(IPv6Constants.IPv4);
        return msg;
    }
}
