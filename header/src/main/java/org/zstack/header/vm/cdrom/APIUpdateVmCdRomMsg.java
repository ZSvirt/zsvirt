package org.zstack.header.vm.cdrom;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.APINoSee;
import org.zstack.header.rest.RestRequest;
import org.zstack.header.vm.VmInstanceMessage;

/**
 * Create by lining at 2018/12/30
 */
@RestRequest(
        path = "/vm-instances/cdroms/{uuid}/actions",
        method = HttpMethod.PUT,
        isAction = true,
        responseClass = APIUpdateVmCdRomEvent.class
)
public class APIUpdateVmCdRomMsg extends APIMessage implements VmInstanceMessage {
    @APIParam(resourceType = VmCdRomVO.class)
    private String uuid;

    @APINoSee
    private String vmInstanceUuid;

    @APIParam(required = false, maxLength = 2048)
    private String description;

    @APIParam(required = false, maxLength = 255)
    private String name;

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    @Override
    public String getVmInstanceUuid() {
        return vmInstanceUuid;
    }

    public void setVmInstanceUuid(String vmInstanceUuid) {
        this.vmInstanceUuid = vmInstanceUuid;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public static APIUpdateVmCdRomMsg __example__() {
        APIUpdateVmCdRomMsg msg = new APIUpdateVmCdRomMsg();
        msg.uuid = uuid();
        msg.setName("cd-2");
        return msg;
    }
}
