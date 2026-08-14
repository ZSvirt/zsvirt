package org.zstack.header.tpm.api;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIDeleteMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.message.DocUtils;
import org.zstack.header.rest.RestRequest;
import org.zstack.header.tpm.entity.TpmVO;
import org.zstack.header.vm.VmInstanceMessage;
import org.zstack.header.vm.VmInstanceVO;

@RestRequest(
        path = "/tpms",
        method = HttpMethod.DELETE,
        responseClass = APIRemoveTpmEvent.class
)
public class APIRemoveTpmMsg extends APIDeleteMessage implements VmInstanceMessage, TpmMessage {
    @APIParam(required = false, resourceType = TpmVO.class, successIfResourceNotExisting = true)
    private String tpmUuid;

    @APIParam(required = false, resourceType = VmInstanceVO.class)
    private String vmInstanceUuid;

    @Override
    public String getTpmUuid() {
        return tpmUuid;
    }

    @Override
    public void setTpmUuid(String tpmUuid) {
        this.tpmUuid = tpmUuid;
    }

    @Override
    public String getVmInstanceUuid() {
        return vmInstanceUuid;
    }

    @Override
    public void setVmInstanceUuid(String vmInstanceUuid) {
        this.vmInstanceUuid = vmInstanceUuid;
    }

    public static APIRemoveTpmMsg __example__() {
        APIRemoveTpmMsg msg = new APIRemoveTpmMsg();
        msg.setVmInstanceUuid(DocUtils.createFixedUuid(VmInstanceVO.class));
        return msg;
    }
}
