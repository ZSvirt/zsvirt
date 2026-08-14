package org.zstack.header.cbt;

import org.zstack.header.message.APICreateMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.RestRequest;
import org.springframework.http.HttpMethod;
import org.zstack.header.vm.VmInstanceVO;

import static org.zstack.header.message.APIParam.SCOPE_ALLOWED_SHARING;

@RestRequest(
        path = "/cbt-task/create",
        method = HttpMethod.POST,
        responseClass = APICreateCbtTaskEvent.class,
        parameterName = "params"
)
public class APICreateCbtTaskMsg extends APICreateMessage {
    @APIParam(maxLength = 255)
    private String name;

    @APIParam(maxLength = 2048, required = false)
    private String description;

    @APIParam(resourceType = VmInstanceVO.class, nonempty = true, scope = SCOPE_ALLOWED_SHARING)
    private String vmInstanceUuid;

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

    public String getVmInstanceUuid() {
        return vmInstanceUuid;
    }

    public void setVmInstanceUuid(String vmInstanceUuid) {
        this.vmInstanceUuid = vmInstanceUuid;
    }

    public static APICreateCbtTaskMsg __example__() {
        APICreateCbtTaskMsg msg = new APICreateCbtTaskMsg();

        msg.setName("My CDP Task");
        msg.setVmInstanceUuid(uuid());
        return msg;
    }
}
