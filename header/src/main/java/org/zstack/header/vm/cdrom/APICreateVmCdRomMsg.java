package org.zstack.header.vm.cdrom;

import org.springframework.http.HttpMethod;
import org.zstack.header.image.ImageVO;
import org.zstack.header.message.APICreateMessage;
import org.zstack.header.message.APIEvent;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.other.APIAuditor;
import org.zstack.header.rest.RestRequest;
import org.zstack.header.tag.TagResourceType;
import org.zstack.header.vm.VmInstanceMessage;
import org.zstack.header.vm.VmInstanceVO;

/**
 * Create by lining at 2018/12/29
 */
@TagResourceType(VmCdRomVO.class)
@RestRequest(
        path = "/vm-instances/cdroms",
        method = HttpMethod.POST,
        responseClass = APICreateVmCdRomEvent.class,
        parameterName = "params"
)
public class APICreateVmCdRomMsg extends APICreateMessage implements APIAuditor, VmInstanceMessage {
    @APIParam(maxLength = 255)
    private String name;

    @APIParam(resourceType = VmInstanceVO.class)
    private String vmInstanceUuid;

    @APIParam(resourceType = ImageVO.class, required = false)
    private String isoUuid;

    @APIParam(required = false, maxLength = 2048)
    private String description;

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

    public static APICreateVmCdRomMsg __example__() {
        APICreateVmCdRomMsg msg = new APICreateVmCdRomMsg();
        msg.setName("cd-1");
        msg.setIsoUuid(uuid());
        msg.setVmInstanceUuid(uuid());
        msg.setDescription("this is a cd-rom");
        return msg;
    }

    @Override
    public Result audit(APIMessage msg, APIEvent rsp) {
        return new Result(rsp.isSuccess() ? ((APICreateVmCdRomEvent)rsp).getInventory().getVmInstanceUuid() : "", VmInstanceVO.class);
    }

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
}
