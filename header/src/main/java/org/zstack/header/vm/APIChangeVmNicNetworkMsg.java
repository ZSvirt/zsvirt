package org.zstack.header.vm;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.network.l3.L3NetworkVO;
import org.zstack.header.rest.APINoSee;
import org.zstack.header.rest.RestRequest;
import org.zstack.header.vm.metadata.MetadataImpact;

import java.util.List;
import java.util.Map;

@RestRequest(
        path = "/vm-instances/nics/{vmNicUuid}/l3-networks/{destL3NetworkUuid}",
        parameterName = "params",
        method = HttpMethod.POST,
        responseClass = APIChangeVmNicNetworkEvent.class
)
@MetadataImpact(value = MetadataImpact.Impact.CONFIG, resolver = "NicUuidToVmUuidResolver", field = "vmNicUuid")
public class APIChangeVmNicNetworkMsg extends APIMessage implements VmInstanceMessage{
    @APIParam(resourceType = VmNicVO.class)
    private String vmNicUuid;

    @APIParam(resourceType = L3NetworkVO.class)
    private String destL3NetworkUuid;

    @APIParam(required = false)
    private String vmNicParams;

    @APINoSee
    private String vmInstanceUuid;

    @APINoSee
    private Map<String, List<String>> requiredIpMap;

    private String staticIp;

    public String getVmNicUuid() {
        return vmNicUuid;
    }

    public void setVmNicUuid(String vmNicUuid) {
        this.vmNicUuid = vmNicUuid;
    }

    public String getDestL3NetworkUuid() {
        return destL3NetworkUuid;
    }

    public void setDestL3NetworkUuid(String destL3NetworkUuid) {
        this.destL3NetworkUuid = destL3NetworkUuid;
    }

    public String getVmNicParams() {
        return vmNicParams;
    }

    public void setVmNicParams(String vmNicParams) {
        this.vmNicParams = vmNicParams;
    }

    public Map<String, List<String>> getRequiredIpMap() {
        return requiredIpMap;
    }

    public void setRequiredIpMap(Map<String, List<String>> requiredIpMap) {
        this.requiredIpMap = requiredIpMap;
    }

    public static APIChangeVmNicNetworkMsg __example__() {
        APIChangeVmNicNetworkMsg msg = new APIChangeVmNicNetworkMsg();
        msg.vmNicUuid = uuid();
        msg.destL3NetworkUuid = uuid();
        return msg;
    }

    @Override
    public String getVmInstanceUuid() {
        return vmInstanceUuid;
    }

    public void setVmInstanceUuid(String vmInstanceUuid) {
        this.vmInstanceUuid = vmInstanceUuid;
    }

    public String getStaticIp() {
        return staticIp;
    }

    public void setStaticIp(String staticIp) {
        this.staticIp = staticIp;
    }
}
