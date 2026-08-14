package org.zstack.header.vm;

import org.springframework.http.HttpMethod;
import org.zstack.header.image.ImageVO;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.APINoSee;
import org.zstack.header.rest.RestRequest;
import org.zstack.header.vm.metadata.MetadataImpact;

/**
 * Created by frank on 10/17/2015.
 */
@RestRequest(
        path = "/vm-instances/{vmInstanceUuid}/iso/{isoUuid}",
        method = HttpMethod.POST,
        responseClass = APIAttachIsoToVmInstanceEvent.class,
        parameterName = "null"
)
@MetadataImpact(value = MetadataImpact.Impact.CONFIG, resolver = "VmUuidDirectResolver", field = "vmInstanceUuid")
public class APIAttachIsoToVmInstanceMsg extends APIMessage implements VmInstanceMessage {
    @APIParam(resourceType = VmInstanceVO.class)
    private String vmInstanceUuid;
    @APIParam(resourceType = ImageVO.class)
    private String isoUuid;

    @APINoSee
    private String cdRomUuid;

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
 
    public static APIAttachIsoToVmInstanceMsg __example__() {
        APIAttachIsoToVmInstanceMsg msg = new APIAttachIsoToVmInstanceMsg();
        msg.vmInstanceUuid = uuid();
        msg.isoUuid = uuid();
        return msg;
    }

    public String getCdRomUuid() {
        return cdRomUuid;
    }

    public void setCdRomUuid(String cdRomUuid) {
        this.cdRomUuid = cdRomUuid;
    }
}
