package org.zstack.header.vm;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIEvent;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.other.APIAuditor;
import org.zstack.header.rest.RestRequest;
import org.zstack.utils.data.SizeUnit;

@RestRequest(
        path = "/vm-instances/templatedVmInstance/{uuid}/actions",
        method = HttpMethod.PUT,
        isAction = true,
        responseClass = APIUpdateTemplatedVmInstanceEvent.class
)
public class APIUpdateTemplatedVmInstanceMsg extends APIMessage implements VmInstanceMessage, APIAuditor {
    @APIParam(resourceType = TemplatedVmInstanceVO.class)
    private String uuid;

    @APIParam(maxLength = 255, required = false)
    private String name;

    @APIParam(maxLength = 2048, required = false)
    private String description;

    @APIParam(required = false, numberRange = {1, 1024})
    private Integer cpuNum;

    @APIParam(required = false, numberRange = {1, Long.MAX_VALUE})
    private Long memorySize;

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

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

    @Override
    public String getVmInstanceUuid() {
        return uuid;
    }

    public Integer getCpuNum() {
        return cpuNum;
    }

    public void setCpuNum(Integer cpuNum) {
        this.cpuNum = cpuNum;
    }

    public Long getMemorySize() {
        return memorySize;
    }

    public void setMemorySize(Long memorySize) {
        this.memorySize = memorySize;
    }

    public static APIUpdateTemplatedVmInstanceMsg __example__() {
        APIUpdateTemplatedVmInstanceMsg msg = new APIUpdateTemplatedVmInstanceMsg();
        msg.setUuid(uuid());
        msg.setName("templated-vm");
        msg.setDescription("description");
        msg.setCpuNum(1);
        msg.setMemorySize(SizeUnit.GIGABYTE.toByte(1));
        return msg;
    }

    @Override
    public Result audit(APIMessage msg, APIEvent rsp) {
        return new Result(rsp.isSuccess() ? ((APIUpdateTemplatedVmInstanceEvent)rsp).getInventory().getUuid() : "", TemplatedVmInstanceVO.class);
    }
}
