package org.zstack.network.l2.virtualSwitch.header;

import org.springframework.http.HttpMethod;
import org.zstack.header.host.HostVO;
import org.zstack.header.message.APIGetMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.RestRequest;

import java.util.Collections;
import java.util.List;

@RestRequest(
        path = "/hosts/kernel-interfaces",
        method = HttpMethod.GET,
        responseClass = APIGetCandidateHostKernelInterfacesReply.class
)
public class APIGetCandidateHostKernelInterfacesMsg extends APIGetMessage {
    @APIParam(resourceType = HostVO.class, emptyString = false, nonempty = true)
    private List<String> hostUuids;

    @APIParam(required = false, emptyString = false)
    private String cidr;

    @APIParam(required = false, validEnums = HostKernelInterfaceTrafficType.class, nonempty = true)
    private List<String> trafficTypes;

    @APIParam(required = false)
    private boolean containsRejected = true;

    public List<String> getHostUuids() {
        return hostUuids;
    }

    public void setHostUuids(List<String> hostUuids) {
        this.hostUuids = hostUuids;
    }

    public String getCidr() {
        return cidr;
    }

    public void setCidr(String cidr) {
        this.cidr = cidr;
    }

    public List<String> getTrafficTypes() {
        return trafficTypes;
    }

    public void setTrafficTypes(List<String> trafficTypes) {
        this.trafficTypes = trafficTypes;
    }

    public boolean isContainsRejected() {
        return containsRejected;
    }

    public void setContainsRejected(boolean containsRejected) {
        this.containsRejected = containsRejected;
    }

    public static APIGetCandidateHostKernelInterfacesMsg __example__() {
        APIGetCandidateHostKernelInterfacesMsg msg = new APIGetCandidateHostKernelInterfacesMsg();
        msg.setHostUuids(Collections.singletonList(uuid(HostVO.class)));
        msg.setCidr("192.168.1.0/24");
        msg.setTrafficTypes(Collections.singletonList(HostKernelInterfaceTrafficType.Management.toString()));
        msg.setContainsRejected(true);
        return msg;
    }
}
