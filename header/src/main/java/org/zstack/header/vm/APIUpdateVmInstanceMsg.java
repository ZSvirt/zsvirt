package org.zstack.header.vm;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.network.l3.L3NetworkVO;
import org.zstack.header.rest.RestRequest;
import org.zstack.header.vm.metadata.MetadataImpact;

/**
 * Created by frank on 6/14/2015.
 */
@RestRequest(
        path = "/vm-instances/{uuid}/actions",
        method = HttpMethod.PUT,
        isAction = true,
        responseClass = APIUpdateVmInstanceEvent.class
)
@MetadataImpact(value = MetadataImpact.Impact.CONFIG, resolver = "VmUuidDirectResolver", field = "uuid")
public class APIUpdateVmInstanceMsg extends APIMessage implements VmInstanceMessage {
    @APIParam(resourceType = VmInstanceVO.class)
    private String uuid;
    @APIParam(maxLength = 255, required = false)
    private String name;
    @APIParam(maxLength = 2048, required = false)
    private String description;
    @APIParam(validValues = {"Stopped", "Running"}, required = false)
    private String state;
    @APIParam(resourceType = L3NetworkVO.class, required = false)
    private String defaultL3NetworkUuid;
    @APIParam(required = false, validValues = {"Linux", "Windows", "Other", "Paravirtualization", "WindowsVirtio"})
    private String platform;
    @APIParam(required = false, numberRange = {1, 1024})
    private Integer cpuNum;
    @APIParam(required = false, numberRange = {1, Long.MAX_VALUE})
    private Long memorySize;
    @APIParam(required = false, numberRange = {0, Long.MAX_VALUE})
    private Long reservedMemorySize;
    @APIParam(required = false, maxLength = 255)
    private String guestOsType;
    @APIParam(required = false)
    private String allocatorStrategy;
    @APIParam(required = false)
    private Boolean vmEncryption;

    public String getDefaultL3NetworkUuid() {
        return defaultL3NetworkUuid;
    }

    public void setDefaultL3NetworkUuid(String defaultL3NetworkUuid) {
        this.defaultL3NetworkUuid = defaultL3NetworkUuid;
    }

    public String getPlatform() {
        return platform;
    }

    public void setPlatform(String platform) {
        this.platform = platform;
    }

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

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
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

    public String getGuestOsType() {
        return guestOsType;
    }

    public void setGuestOsType(String guestOsType) {
        this.guestOsType = guestOsType;
    }

    public Long getReservedMemorySize() {
        return reservedMemorySize;
    }

    public void setReservedMemorySize(Long reservedMemorySize) {
        this.reservedMemorySize = reservedMemorySize;
    }

    public String getAllocatorStrategy() {
        return allocatorStrategy;
    }

    public void setAllocatorStrategy(String allocatorStrategy) {
        this.allocatorStrategy = allocatorStrategy;
    }

    public Boolean getVmEncryption() {
        return vmEncryption;
    }

    public void setVmEncryption(Boolean vmEncryption) {
        this.vmEncryption = vmEncryption;
    }

    public static APIUpdateVmInstanceMsg __example__() {
        APIUpdateVmInstanceMsg msg = new APIUpdateVmInstanceMsg();
        msg.uuid = uuid();
        msg.name = "new vm name";
        return msg;
    }
}
