package org.zstack.header.host;


import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.RestRequest;

@RestRequest(
        path = "/hosts/{uuid}/resource-allocation",
        responseClass = APIGetHostResourceAllocationEvent.class,
        method = HttpMethod.POST,
        parameterName = "params"
)
@Deprecated
public class APIGetHostResourceAllocationMsg extends APIMessage implements HostMessage {
    @APIParam(resourceType = HostVO.class)
    private String uuid;

    @APIParam(validValues = {"continuous"})
    private String strategy;

    @APIParam(validValues = {"normal", "performance"})
    private String scene;

    @APIParam(numberRange = {1, 512})
    private int vcpu;

    @APIParam(required = false, numberRange = {1, Long.MAX_VALUE})
    private Long memSize;

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public Integer getVcpu() {
        return vcpu;
    }

    public void setVcpu(int vcpu) {
        this.vcpu = vcpu;
    }

    public void setScene(String scene) {
        this.scene = scene;
    }

    public void setStrategy(String strategy) {
        this.strategy = strategy;
    }

    public String getScene() {
        return scene;
    }

    public String getStrategy() {
        return strategy;
    }

    public void setMemSize(Long memSize) {
        this.memSize = memSize;
    }

    public Long getMemSize() {
        if (memSize == null) {
            return 0L;
        } else {
            return memSize;
        }
    }

    public static APIGetHostResourceAllocationMsg __example__() {
        APIGetHostResourceAllocationMsg msg = new APIGetHostResourceAllocationMsg();
        msg.setVcpu(2);
        msg.setScene("normal");
        msg.setStrategy("continuous");
        msg.setUuid(uuid());
        msg.setMemSize(1234567L);
        return msg;
    }

    @Override
    public String getHostUuid() {
        return getUuid();
    }
}
