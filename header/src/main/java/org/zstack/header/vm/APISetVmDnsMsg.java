package org.zstack.header.vm;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.RestRequest;
import org.zstack.utils.network.IPv6Constants;

import java.util.Collections;
import java.util.List;

@RestRequest(
        path = "/vm-instances/{vmInstanceUuid}/actions",
        isAction = true,
        method = HttpMethod.PUT,
        responseClass = APISetVmDnsEvent.class
)
public class APISetVmDnsMsg extends APIMessage implements VmInstanceMessage {
    @APIParam(resourceType = VmInstanceVO.class)
    private String vmInstanceUuid;

    @APIParam(resourceType = VmNicVO.class, required = false)
    private String vmNicUuid;

    @APIParam
    private List<String> dnsList;

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

    public List<String> getDnsList() {
        return dnsList;
    }

    public void setDnsList(List<String> dnsList) {
        this.dnsList = dnsList;
    }

    public Integer getIpVersion() {
        return ipVersion;
    }

    public void setIpVersion(Integer ipVersion) {
        this.ipVersion = ipVersion;
    }

    public static APISetVmDnsMsg __example__() {
        APISetVmDnsMsg msg = new APISetVmDnsMsg();
        msg.setVmInstanceUuid(uuid(VmInstanceVO.class));
        msg.setVmNicUuid(uuid(VmNicVO.class));
        msg.setDnsList(Collections.singletonList("223.5.5.5"));
        msg.setIpVersion(IPv6Constants.IPv4);
        return msg;
    }
}
