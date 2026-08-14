package org.zstack.header.vm;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIDeleteMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.RestRequest;
import org.zstack.header.vm.metadata.MetadataImpact;

/**
 * Created by frank on 2/26/2016.
 */
@RestRequest(
        path = "/vm-instances/{vmInstanceUuid}/static-ips",
        method = HttpMethod.DELETE,
        responseClass = APIDeleteVmStaticIpEvent.class
)
@MetadataImpact(value = MetadataImpact.Impact.CONFIG, resolver = "VmUuidDirectResolver", field = "vmInstanceUuid")
public class APIDeleteVmStaticIpMsg extends APIDeleteMessage implements VmInstanceMessage {
    @APIParam(resourceType = VmInstanceVO.class)
    private String vmInstanceUuid;
    @APIParam
    private String l3NetworkUuid;
    @APIParam(required = false)
    private String staticIp;   /* staticIp is not set, delete all static ip of this nic */

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

    public String getStaticIp() {
        return staticIp;
    }

    public void setStaticIp(String staticIp) {
        this.staticIp = staticIp;
    }

    public static APIDeleteVmStaticIpMsg __example__() {
        APIDeleteVmStaticIpMsg msg = new APIDeleteVmStaticIpMsg();
        msg.vmInstanceUuid = uuid();
        msg.l3NetworkUuid = uuid();
        return msg;
    }

}
