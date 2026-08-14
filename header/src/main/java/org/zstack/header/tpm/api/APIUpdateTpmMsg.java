package org.zstack.header.tpm.api;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.message.DocUtils;
import org.zstack.header.rest.RestRequest;
import org.zstack.header.tpm.entity.TpmVO;
import org.zstack.header.vm.VmInstanceVO;
import org.zstack.utils.StringDSL;

@RestRequest(
        path = "/tpms",
        method = HttpMethod.PUT,
        isAction = true,
        responseClass = APIUpdateTpmEvent.class
)
public class APIUpdateTpmMsg extends APIMessage implements TpmMessage {
    @APIParam(required = false, resourceType = VmInstanceVO.class)
    private String vmInstanceUuid;

    @APIParam(required = false, resourceType = TpmVO.class)
    private String tpmUuid;

    @APIParam(required = false, minLength = 32, maxLength = 32)
    private String keyProviderUuid;

    @Override
    public String getVmInstanceUuid() {
        return vmInstanceUuid;
    }

    @Override
    public void setVmInstanceUuid(String vmInstanceUuid) {
        this.vmInstanceUuid = vmInstanceUuid;
    }

    @Override
    public String getTpmUuid() {
        return tpmUuid;
    }

    @Override
    public void setTpmUuid(String tpmUuid) {
        this.tpmUuid = tpmUuid;
    }

    public String getKeyProviderUuid() {
        return keyProviderUuid;
    }

    public void setKeyProviderUuid(String keyProviderUuid) {
        this.keyProviderUuid = keyProviderUuid;
    }

    public static APIUpdateTpmMsg __example__() {
        APIUpdateTpmMsg msg = new APIUpdateTpmMsg();
        msg.setKeyProviderUuid(StringDSL.createFixedUuid("keyProviderUuid"));
        msg.setVmInstanceUuid(DocUtils.createFixedUuid(VmInstanceVO.class));
        return msg;
    }
}
