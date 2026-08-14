package org.zstack.header.vm;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.RestRequest;
import org.zstack.header.vm.metadata.MetadataImpact;

import java.util.List;

import static java.util.Arrays.asList;

/**
 * Created by frank on 11/22/2015.
 */
@RestRequest(
        path = "/vm-instances/{uuid}/actions",
        isAction = true,
        method = HttpMethod.PUT,
        responseClass = APISetVmBootOrderEvent.class
)
@MetadataImpact(value = MetadataImpact.Impact.CONFIG, resolver = "VmUuidDirectResolver", field = "uuid")
public class APISetVmBootOrderMsg extends APIMessage implements VmInstanceMessage {
    @APIParam(resourceType = VmInstanceVO.class)
    private String uuid;
    private List<String> bootOrder;

    @Override
    public String getVmInstanceUuid() {
        return uuid;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public List<String> getBootOrder() {
        return bootOrder;
    }

    public void setBootOrder(List<String> bootOrder) {
        this.bootOrder = bootOrder;
    }
 
    public static APISetVmBootOrderMsg __example__() {
        APISetVmBootOrderMsg msg = new APISetVmBootOrderMsg();
        msg.uuid = uuid();
        msg.bootOrder = asList(VmBootDevice.CdRom.toString(), VmBootDevice.HardDisk.toString());
        return msg;
    }
}
