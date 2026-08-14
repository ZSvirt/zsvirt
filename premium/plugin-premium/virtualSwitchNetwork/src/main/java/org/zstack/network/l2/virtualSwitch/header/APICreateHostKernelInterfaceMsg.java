package org.zstack.network.l2.virtualSwitch.header;

import java.util.Collections;
import java.util.List;

import org.springframework.http.HttpMethod;
import org.zstack.header.host.HostVO;
import org.zstack.header.message.APICreateMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.APINoSee;
import org.zstack.header.rest.RestRequest;

@RestRequest(
        path = "/l3-networks/kernel-interfaces",
        method = HttpMethod.POST,
        responseClass = APICreateHostKernelInterfaceEvent.class,
        parameterName = "params"
)
public class APICreateHostKernelInterfaceMsg extends APICreateMessage {
    @APIParam(maxLength = 255, emptyString = false)
    private String name;

    @APIParam(maxLength = 255, required = false)
    private String description;

    @APIParam(resourceType = HostVO.class, emptyString = false)
    private String hostUuid;

    @APIParam(resourceType = PortGroupVO.class, emptyString = false)
    private String l3NetworkUuid;

    @APIParam(required = false)
    private String requiredIp;

    @APIParam(required = false)
    private String netmask;

    @APIParam(validEnums = {HostKernelInterfaceTrafficType.class}, required = false)
    private List<String> trafficTypes;

    @APINoSee
    private String l2NetworkUuid;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getHostUuid() {
        return hostUuid;
    }

    public void setHostUuid(String hostUuid) {
        this.hostUuid = hostUuid;
    }

    public String getL2NetworkUuid() {
        return l2NetworkUuid;
    }

    public void setL2NetworkUuid(String l2NetworkUuid) {
        this.l2NetworkUuid = l2NetworkUuid;
    }

    public String getL3NetworkUuid() {
        return l3NetworkUuid;
    }

    public void setL3NetworkUuid(String l3NetworkUuid) {
        this.l3NetworkUuid = l3NetworkUuid;
    }

    public String getRequiredIp() {
        return requiredIp;
    }

    public void setRequiredIp(String requiredIp) {
        this.requiredIp = requiredIp;
    }

    public String getNetmask() {
        return netmask;
    }

    public void setNetmask(String netmask) {
        this.netmask = netmask;
    }

    public List<String> getTrafficTypes() {
        return trafficTypes;
    }

    public void setTrafficTypes(List<String> trafficTypes) {
        this.trafficTypes = trafficTypes;
    }

    public static APICreateHostKernelInterfaceMsg __example__() {
        APICreateHostKernelInterfaceMsg msg = new APICreateHostKernelInterfaceMsg();
        msg.setName("zs-kernel");
        msg.setDescription("test-description");
        msg.setHostUuid(uuid(HostVO.class));
        msg.setL3NetworkUuid(uuid(PortGroupVO.class));
        msg.setRequiredIp("172.16.1.1");
        msg.setNetmask("255.255.255.0");
        msg.setTrafficTypes(Collections.singletonList(HostKernelInterfaceTrafficType.Management.toString()));

        return msg;
    }

}
