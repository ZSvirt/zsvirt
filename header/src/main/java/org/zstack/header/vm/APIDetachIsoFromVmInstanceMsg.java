package org.zstack.header.vm;

import org.springframework.http.HttpMethod;
import org.zstack.header.image.ImageVO;
import org.zstack.header.message.APIEvent;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.other.APIAuditor;
import org.zstack.header.rest.RestRequest;
import org.zstack.header.vm.metadata.MetadataImpact;

/**
 * Created by frank on 10/17/2015.
 */
@RestRequest(
        path = "/vm-instances/{vmInstanceUuid}/iso",
        method = HttpMethod.DELETE,
        responseClass = APIDetachIsoFromVmInstanceEvent.class
)
@MetadataImpact(value = MetadataImpact.Impact.CONFIG, resolver = "VmUuidDirectResolver", field = "vmInstanceUuid")
public class APIDetachIsoFromVmInstanceMsg extends APIMessage implements VmInstanceMessage, APIAuditor {
    @APIParam(resourceType = VmInstanceVO.class)
    private String vmInstanceUuid;

    // resourceType can not be set to ImageVO.class, because the image may have been deleted
    // required can not be set to true, Because of the need to be compatible with the old API
    @APIParam(required = false)
    private String isoUuid;

    @Override
    public String getVmInstanceUuid() {
        return vmInstanceUuid;
    }

    public void setVmInstanceUuid(String vmInstanceUuid) {
        this.vmInstanceUuid = vmInstanceUuid;
    }

    public String getIsoUuid() {
        return isoUuid;
    }

    public void setIsoUuid(String isoUuid) {
        this.isoUuid = isoUuid;
    }

    public static APIDetachIsoFromVmInstanceMsg __example__() {
        APIDetachIsoFromVmInstanceMsg msg = new APIDetachIsoFromVmInstanceMsg();
        msg.vmInstanceUuid = uuid();
        return msg;
    }

    @Override
    public Result audit(APIMessage msg, APIEvent rsp) {
        return new Result(((APIDetachIsoFromVmInstanceMsg)msg).isoUuid, ImageVO.class);
    }
}
