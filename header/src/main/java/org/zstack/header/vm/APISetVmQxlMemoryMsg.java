package org.zstack.header.vm;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.RestRequest;
import org.zstack.header.vm.metadata.MetadataImpact;

@RestRequest(
        path = "/vm-instances/{uuid}/actions",
        isAction = true,
        method = HttpMethod.PUT,
        responseClass = APISetVmQxlMemoryEvent.class
)
@MetadataImpact(value = MetadataImpact.Impact.CONFIG, resolver = "VmUuidDirectResolver", field = "uuid")
public class APISetVmQxlMemoryMsg extends APIMessage implements VmInstanceMessage {
    @APIParam(resourceType = VmInstanceVO.class)
    private String uuid;
    @APIParam(numberRange = {1024, 524288}, required = false)
    private Integer ram = 65536;
    @APIParam(numberRange = {1024, 524288}, required = false)
    private Integer vram = 32768;
    @APIParam(numberRange = {1024, 524288}, required = false)
    private Integer vgamem = 16384;

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

    public Integer getRam() {
        return ram;
    }

    public void setRam(Integer ram) {
        this.ram = ram;
    }

    public Integer getVram() {
        return vram;
    }

    public void setVram(Integer vram) {
        this.vram = vram;
    }

    public Integer getVgamem() {
        return vgamem;
    }

    public void setVgamem(Integer vgamem) {
        this.vgamem = vgamem;
    }

    public static APISetVmQxlMemoryMsg __example__() {
        APISetVmQxlMemoryMsg msg = new APISetVmQxlMemoryMsg();
        msg.uuid = uuid();
        msg.ram = 65536;
        msg.vram = 32768;
        msg.vgamem = 16384;
        return msg;
    }
}
