package org.zstack.header.vm;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.RestRequest;
import org.zstack.header.vm.metadata.MetadataImpact;


/**
 * Created by longtao.wu@zstack.io on 21/12/01
 */
@RestRequest(
        path = "/vm-instances/{uuid}/actions",
        isAction = true,
        method = HttpMethod.PUT,
        responseClass = APISetVmEmulatorPinningEvent.class
)
@MetadataImpact(value = MetadataImpact.Impact.CONFIG, resolver = "VmUuidDirectResolver", field = "uuid")
public class APISetVmEmulatorPinningMsg extends APIMessage implements VmInstanceMessage {
    @APIParam(resourceType = VmInstanceVO.class )
    private String uuid;
    @APIParam
    private String emulatorPinning;

    @Override
    public String getVmInstanceUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getUuid() {
        return uuid;
    }

    public String getEmulatorPinning() {
        return emulatorPinning;
    }

    public void setEmulatorPinning(String emulatorPinning) {
        this.emulatorPinning = emulatorPinning;
    }

    public static APISetVmEmulatorPinningMsg __example__() {
        APISetVmEmulatorPinningMsg msg = new APISetVmEmulatorPinningMsg();
        msg.uuid = uuid();
        msg.emulatorPinning = "";
        return msg;
    }
}

