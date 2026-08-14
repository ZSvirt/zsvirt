package org.zstack.network.l2.virtualSwitch.header;

import org.springframework.http.HttpMethod;
import org.zstack.header.host.HostInventory;
import org.zstack.header.host.HostVO;
import org.zstack.header.message.*;
import org.zstack.header.other.APIAuditor;
import org.zstack.header.other.APIMultiAuditor;
import org.zstack.header.rest.APINoSee;
import org.zstack.header.rest.RestRequest;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static java.util.Arrays.asList;

@DefaultTimeout(timeunit = TimeUnit.MINUTES, value = 30)
@RestRequest(
        path = "/l3-networks/{l3NetworkUuid}/kernel-interfaces",
        method = HttpMethod.POST,
        responseClass = APIBatchCreateHostKernelInterfaceEvent.class,
        parameterName = "params"
)
public class APIBatchCreateHostKernelInterfaceMsg extends APIMessage implements APIMultiAuditor, APIBatchRequest {
    @APIParam(nonempty = true)
    private List<HostKernelInterfaceStruct> structs;

    @APIParam(resourceType = PortGroupVO.class, emptyString = false)
    private String l3NetworkUuid;

    @APIParam(validEnums = {HostKernelInterfaceTrafficType.class}, required = false)
    private List<String> trafficTypes;

    @APINoSee
    private String l2NetworkUuid;

    public List<HostKernelInterfaceStruct> getStructs() {
        return structs;
    }

    public void setStructs(List<HostKernelInterfaceStruct> structs) {
        this.structs = structs;
    }

    public String getL3NetworkUuid() {
        return l3NetworkUuid;
    }

    public void setL3NetworkUuid(String l3NetworkUuid) {
        this.l3NetworkUuid = l3NetworkUuid;
    }

    public List<String> getTrafficTypes() {
        return trafficTypes;
    }

    public void setTrafficTypes(List<String> trafficTypes) {
        this.trafficTypes = trafficTypes;
    }

    public String getL2NetworkUuid() {
        return l2NetworkUuid;
    }

    public void setL2NetworkUuid(String l2NetworkUuid) {
        this.l2NetworkUuid = l2NetworkUuid;
    }

    @Override
    public Result collectResult(APIMessage message, APIEvent rsp) {
        APIBatchCreateHostKernelInterfaceEvent evt = (APIBatchCreateHostKernelInterfaceEvent) rsp;
        return new APIBatchRequest.Result(
                evt.getResults().size(),
                evt.getResultsWithoutError().size()
        );
    }

    @Override
    public List<APIAuditor.Result> multiAudit(APIMessage msg, APIEvent rsp) {
        if (!rsp.isSuccess()) {
            return null;
        }

        List<APIAuditor.Result> res = new ArrayList<>();
        APIBatchCreateHostKernelInterfaceEvent evt = (APIBatchCreateHostKernelInterfaceEvent) rsp;
        evt.getResults().stream().filter(r -> r.getInventory() != null)
                .forEach(r -> res.add(new APIAuditor.Result(r.getInventory().getUuid(), HostKernelInterfaceVO.class)));
        return res;
    }

    public static APIBatchCreateHostKernelInterfaceMsg __example__() {
        APIBatchCreateHostKernelInterfaceMsg msg = new APIBatchCreateHostKernelInterfaceMsg();

        HostKernelInterfaceStruct struct = new HostKernelInterfaceStruct();
        struct.setName("host-kernel-interface-1");
        struct.setDescription("example");
        struct.setHostUuid(uuid(HostVO.class));
        struct.setIp("192.168.0.1");
        struct.setNetmask("255.255.255.0");
        HostKernelInterfaceStruct struct2 = new HostKernelInterfaceStruct();
        struct2.setName("host-kernel-interface-2");
        struct2.setDescription("example");
        struct2.setHostUuid(uuid(HostInventory.class));
        struct2.setIp("192.168.0.2");
        struct2.setNetmask("255.255.255.0");

        msg.setStructs(asList(struct, struct2));
        msg.setL3NetworkUuid(uuid(PortGroupVO.class));
        msg.setTrafficTypes(Collections.singletonList(HostKernelInterfaceTrafficType.Management.toString()));

        return msg;
    }
}
