package org.zstack.header.vm;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.network.l3.L3NetworkVO;
import org.zstack.header.rest.APINoSee;
import org.zstack.header.rest.RestRequest;
import org.zstack.header.vm.metadata.MetadataImpact;
import org.zstack.utils.network.NicIpAddressInfo;

import java.util.List;
import java.util.Map;

/**
 * @api attach a nic to vm. If vm is running, user is responsible for running DHCP client software inside
 * vm in order to get ip address
 * @cli
 * @httpMsg {
 * "org.zstack.header.vm.APIAttachNicToVmMsg": {
 * "vmInstanceUuid": "94d991c631674b16be65bfdf28b9e84a",
 * "l3NetworkUuid": "e438b93332ba40dcbb5d553c749a43ca",
 * "session": {
 * "uuid": "f6bcff806cb84ae5b91589ae6716b200"
 * }
 * }
 * }
 * @msg {
 * "org.zstack.header.vm.APIAttachNicToVmMsg": {
 * "vmInstanceUuid": "94d991c631674b16be65bfdf28b9e84a",
 * "l3NetworkUuid": "e438b93332ba40dcbb5d553c749a43ca",
 * "session": {
 * "uuid": "f6bcff806cb84ae5b91589ae6716b200"
 * },
 * "timeout": 1800000,
 * "id": "5a441ef9697c4e7b98c9e86715e4987f",
 * "serviceId": "api.portal"
 * }
 * }
 * @result see :ref:`APIAttachNicToVmEvent`
 * @since 0.1.0
 */
@RestRequest(
        path = "/vm-instances/{vmInstanceUuid}/l3-networks/{l3NetworkUuid}",
        parameterName = "params",
        method = HttpMethod.POST,
        responseClass = APIAttachL3NetworkToVmEvent.class
)
@MetadataImpact(value = MetadataImpact.Impact.CONFIG, resolver = "VmUuidDirectResolver", field = "vmInstanceUuid")
public class APIAttachL3NetworkToVmMsg extends APIMessage implements VmInstanceMessage {
    /**
     * @desc vm uuid
     */
    @APIParam(resourceType = VmInstanceVO.class)
    private String vmInstanceUuid;
    /**
     * @desc uuid of L3Network where the nic will be created
     */
    @APIParam(resourceType = L3NetworkVO.class)
    private String l3NetworkUuid;

    private String staticIp;

    @APIParam(required = false)
    private String driverType;

    @APIParam(required = false)
    private String customMac;

    @APIParam(required = false)
    private String vmNicParams;

    @APINoSee
    private Map<String, List<String>> staticIpMap;

    @APINoSee
    private Map<String, NicIpAddressInfo> nicNetworkInfo;

    @APINoSee
    private boolean applyToBackend = true;

    public String getStaticIp() {
        return staticIp;
    }

    public void setStaticIp(String staticIp) {
        this.staticIp = staticIp;
    }

    public String getCustomMac() {
        return customMac;
    }

    public void setCustomMac(String customMac) {
        this.customMac = customMac;
    }

    public String getVmNicParams() {
        return vmNicParams;
    }

    public void setVmNicParams(String vmNicParams) {
        this.vmNicParams = vmNicParams;
    }

    @Override
    public String getVmInstanceUuid() {
        return vmInstanceUuid;
    }

    public void setVmInstanceUuid(String vmInstanceUuid) {
        this.vmInstanceUuid = vmInstanceUuid;
    }

    public String getL3NetworkUuid() {
        return l3NetworkUuid;
    }

    public void setL3NetworkUuid(String l3NetworkUuid) {
        this.l3NetworkUuid = l3NetworkUuid;
    }

    public Map<String, List<String>> getStaticIpMap() {
        return staticIpMap;
    }

    public void setStaticIpMap(Map<String, List<String>> staticIpMap) {
        this.staticIpMap = staticIpMap;
    }

    public boolean isApplyToBackend() {
        return applyToBackend;
    }

    public void setApplyToBackend(boolean applyToBackend) {
        this.applyToBackend = applyToBackend;
    }

    public String getDriverType() {
        return driverType;
    }

    public void setDriverType(String driverType) {
        this.driverType = driverType;
    }

    public Map<String, NicIpAddressInfo> getNicNetworkInfo() {
        return nicNetworkInfo;
    }

    public void setNicNetworkInfo(Map<String, NicIpAddressInfo> nicNetworkInfo) {
        this.nicNetworkInfo = nicNetworkInfo;
    }

    public static APIAttachL3NetworkToVmMsg __example__() {
        APIAttachL3NetworkToVmMsg msg = new APIAttachL3NetworkToVmMsg();
        msg.vmInstanceUuid = uuid();
        msg.l3NetworkUuid = uuid();
        msg.driverType = VmNicConstant.NIC_DRIVER_TYPE_E1000;
        return msg;
    }
}
