package org.zstack.header.tpm.api;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APICreateMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.message.DocUtils;
import org.zstack.header.rest.RestRequest;
import org.zstack.header.vm.VmInstanceMessage;
import org.zstack.header.vm.VmInstanceVO;
import org.zstack.utils.StringDSL;

@RestRequest(
        path = "/tpms",
        method = HttpMethod.POST,
        responseClass = APIAddTpmEvent.class,
        parameterName = "params"
)
public class APIAddTpmMsg extends APICreateMessage implements VmInstanceMessage {
    /**
     * If null, use the default key provider from global config (if set).
     */
    @APIParam(required = false, minLength = 32, maxLength = 32)
    private String keyProviderUuid;

    @APIParam(resourceType = VmInstanceVO.class)
    private String vmInstanceUuid;

    public String getKeyProviderUuid() {
        return keyProviderUuid;
    }

    public void setKeyProviderUuid(String keyProviderUuid) {
        this.keyProviderUuid = keyProviderUuid;
    }

    @Override
    public String getVmInstanceUuid() {
        return vmInstanceUuid;
    }

    public void setVmInstanceUuid(String vmInstanceUuid) {
        this.vmInstanceUuid = vmInstanceUuid;
    }

    public static APIAddTpmMsg __example__() {
        APIAddTpmMsg msg = new APIAddTpmMsg();
        msg.setKeyProviderUuid(StringDSL.createFixedUuid("keyProviderUuid"));
        msg.setVmInstanceUuid(DocUtils.createFixedUuid(VmInstanceVO.class));
        return msg;
    }
}
