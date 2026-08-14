package org.zstack.network.l2.virtualSwitch.header;

import java.util.Collections;
import java.util.List;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.RestRequest;

@RestRequest(
        path = "/l3-networks/kernel-interfaces/{uuid}/actions",
        method = HttpMethod.PUT,
        isAction = true,
        responseClass = APIUpdateHostKernelInterfaceEvent.class
)
public class APIUpdateHostKernelInterfaceMsg extends APIMessage implements HostKernelInterfaceMessage {
    @APIParam(resourceType = HostKernelInterfaceVO.class, emptyString = false)
    private String uuid;

    @APIParam(maxLength = 255, required = false, emptyString = false)
    private String name;

    @APIParam(maxLength = 255, required = false)
    private String description;

    @APIParam(required = false, emptyString = false)
    private String requiredIp;

    @APIParam(required = false, emptyString = false)
    private String netmask;

    @APIParam(validEnums = {HostKernelInterfaceTrafficType.class}, required = false)
    private List<String> trafficTypes;

    @Override
    public String getHostKernelInterfaceUuid() {
        return uuid;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

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

    public String getRequiredIp() {
        return requiredIp;
    }

    public void setRequiredIp(String requiredIp) {
        this.requiredIp = requiredIp;
    }

    public String getNetmask() {
        return netmask;
    }

    public void setNetmask(String requiredIp) {
        this.netmask = requiredIp;
    }

    public List<String> getTrafficTypes() {
        return trafficTypes;
    }

    public void setTrafficTypes(List<String> trafficTypes) {
        this.trafficTypes = trafficTypes;
    }

    public static APIUpdateHostKernelInterfaceMsg __example__() {
        APIUpdateHostKernelInterfaceMsg msg = new APIUpdateHostKernelInterfaceMsg();
        msg.setUuid(uuid(HostKernelInterfaceVO.class));
        msg.setName("test2");
        msg.setDescription("test2-descrption");
        msg.setRequiredIp("172.16.100.100");
        msg.setNetmask("255.255.255.0");
        msg.setTrafficTypes(Collections.singletonList("Management"));
        return msg;
    }

}
