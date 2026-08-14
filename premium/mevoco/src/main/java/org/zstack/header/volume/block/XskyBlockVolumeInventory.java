package org.zstack.header.volume.block;

import org.zstack.header.configuration.PythonClassInventory;
import org.zstack.header.search.Inventory;
import org.zstack.header.search.Parent;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * @author shenjin
 * @date 2023/6/21 15:03
 */
@PythonClassInventory
@Inventory(mappingVOClass = XskyBlockVolumeVO.class, collectionValueOfMethod="valueOf2",
        parent = {@Parent(inventoryClass = BlockVolumeInventory.class, type = BlockVolumeConstants.XSKY_BLOCK_VOLUME_TYPE)})
public class XskyBlockVolumeInventory extends BlockVolumeInventory {
    private Integer accessPathId;
    private String accessPathIqn;
    private String xskyStatus;
    private Integer xskyBlockVolumeId;
    private Long burstTotalBw;
    private Long burstTotalIops;
    private Long maxTotalBw;
    private Long maxTotalIops;

    public XskyBlockVolumeInventory() {
    }

    public XskyBlockVolumeInventory(XskyBlockVolumeVO vo) {
        super(vo);
        this.setAccessPathId(vo.getAccessPathId());
        this.setAccessPathIqn(vo.getAccessPathIqn());
        this.setXskyStatus(vo.getXskyStatus());
        this.setXskyBlockVolumeId(vo.getXskyBlockVolumeId());
        this.setBurstTotalBw(vo.getBurstTotalBw());
        this.setBurstTotalIops(vo.getBurstTotalIops());
        this.setMaxTotalBw(vo.getMaxTotalBw());
        this.setMaxTotalIops(vo.getMaxTotalIops());
    }

    public static XskyBlockVolumeInventory valueOf(XskyBlockVolumeVO vo) {
        return new XskyBlockVolumeInventory(vo);
    }

    public static List<XskyBlockVolumeInventory> valueOf2(Collection<XskyBlockVolumeVO> vos) {
        List<XskyBlockVolumeInventory> invs = new ArrayList<XskyBlockVolumeInventory>(vos.size());
        for (XskyBlockVolumeVO vo : vos) {
            invs.add(new XskyBlockVolumeInventory(vo));
        }
        return invs;
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

