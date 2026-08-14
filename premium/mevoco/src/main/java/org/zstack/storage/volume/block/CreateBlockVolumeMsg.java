package org.zstack.storage.volume.block;

/**
 * @author shenjin
 * @date 2023/6/20 16:44
 */
public class CreateBlockVolumeMsg {
    private String name;

    private String description;

    private Long size;

    private String primaryStorageUuid;

    private Integer accessPathId;

    private String accessPathIqn;

    private Long burstTotalBw;
    
    private Long burstTotalIops;

    private Long maxTotalBw;

    private Long maxTotalIops;

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
}
