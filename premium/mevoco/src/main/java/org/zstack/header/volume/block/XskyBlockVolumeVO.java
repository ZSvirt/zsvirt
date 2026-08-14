package org.zstack.header.volume.block;

import org.zstack.header.vo.ToInventory;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.PrimaryKeyJoinColumn;
import javax.persistence.Table;

/**
 * @author shenjin
 * @date 2023/6/13 16:04
 */
@Entity
@Table
@PrimaryKeyJoinColumn(name="uuid", referencedColumnName="uuid")
public class XskyBlockVolumeVO extends BlockVolumeVO implements ToInventory {
    @Column
    private Integer accessPathId;

    @Column
    private String accessPathIqn;

    @Column
    private String xskyStatus;

    @Column
    private Integer xskyBlockVolumeId;

    @Column
    private Long burstTotalBw;

    @Column
    private Long burstTotalIops;

    @Column
    private Long maxTotalBw;

    @Column
    private Long maxTotalIops;

    public XskyBlockVolumeVO() {
    }

    public XskyBlockVolumeVO(BlockVolumeVO other) {
        super(other);
        this.setIscsiPath(other.getIscsiPath());
        this.setVendor(other.getVendor());
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

    public String getXskyStatus() {
        return xskyStatus;
    }

    public void setXskyStatus(String xskyStatus) {
        this.xskyStatus = xskyStatus;
    }

    public Integer getXskyBlockVolumeId() {
        return xskyBlockVolumeId;
    }

    public void setXskyBlockVolumeId(Integer xskyBlockVolumeId) {
        this.xskyBlockVolumeId = xskyBlockVolumeId;
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
