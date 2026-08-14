package org.zstack.header.volume.block;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.*;
import org.zstack.header.other.APIAuditor;
import org.zstack.header.rest.RestRequest;
import org.zstack.header.storage.primary.PrimaryStorageVO;
import org.zstack.header.tag.TagResourceType;

import java.util.concurrent.TimeUnit;

@RestRequest(
        path = "/block-volumes",
        method = HttpMethod.POST,
        responseClass = APICreateBlockVolumeEvent.class,
        parameterName = "params"
)
@TagResourceType(BlockVolumeVO.class)
@DefaultTimeout(timeunit = TimeUnit.HOURS, value = 12)
public class APICreateBlockVolumeMsg extends APICreateMessage implements APIAuditor {
    @APIParam
    private String name;
    
    @APIParam(required = false)
    private String description;
    
    @APIParam
    private Long size;
    
    @APIParam(resourceType = PrimaryStorageVO.class)
    private String primaryStorageUuid;
    
    @APIParam(required = false)
    private Integer accessPathId;
    
    @APIParam(required = false)
    private String accessPathIqn;
    
    @APIParam(required = false)
    private Long burstTotalBw;
    
    @APIParam(required = false)
    private Long burstTotalIops;
    
    @APIParam(required = false)
    private Long maxTotalBw;
    
    @APIParam(required = false)
    private Long maxTotalIops;

    @APIParam(required = false)
    private String protocol;

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

    public Long getSize() {
        return size;
    }

    public void setSize(Long size) {
        this.size = size;
    }

    public String getPrimaryStorageUuid() {
        return primaryStorageUuid;
    }

    public void setPrimaryStorageUuid(String primaryStorageUuid) {
        this.primaryStorageUuid = primaryStorageUuid;
    }

    public Integer getAccessPathId() {
        return accessPathId;
    }

    public void setAccessPathId(Integer accessPathId) {
        this.accessPathId = accessPathId;
    }
    public String getAccessPathIqn() {
        return accessPathIqn;
    }

    public void setAccessPathIqn(String accessPathIqn) {
        this.accessPathIqn = accessPathIqn;
    }

    public Long getBurstTotalBw() {
        return burstTotalBw;
    }

    public void setBurstTotalBw(Long burstTotalBw) {
        this.burstTotalBw = burstTotalBw;
    }

    public Long getBurstTotalIops() {
        return burstTotalIops;
    }

    public void setBurstTotalIops(Long burstTotalIops) {
        this.burstTotalIops = burstTotalIops;
    }

    public Long getMaxTotalBw() {
        return maxTotalBw;
    }

    public void setMaxTotalBw(Long maxTotalBw) {
        this.maxTotalBw = maxTotalBw;
    }

    public Long getMaxTotalIops() {
        return maxTotalIops;
    }

    public void setMaxTotalIops(Long maxTotalIops) {
        this.maxTotalIops = maxTotalIops;
    }

    public String getProtocol() {
        return protocol;
    }

    public void setProtocol(String protocol) {
        this.protocol = protocol;
    }

    public static APICreateBlockVolumeMsg __example__() {
        APICreateBlockVolumeMsg msg = new APICreateBlockVolumeMsg();
        msg.setName("example");
        msg.setDescription("block volume test");
        return msg;
    }

    @Override
    public Result audit(APIMessage msg, APIEvent rsp) {
        return new Result(((APICreateBlockVolumeMsg) msg).getName(), BlockVolumeVO.class);
    }
}
