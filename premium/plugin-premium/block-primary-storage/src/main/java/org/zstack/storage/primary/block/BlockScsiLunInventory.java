package org.zstack.storage.primary.block;

import org.zstack.header.configuration.PythonClassInventory;
import org.zstack.header.search.Inventory;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * @author Lei Liu lei.liu@zstack.io
 * @date 2023/4/25 10:36
 */

@Inventory(mappingVOClass = BlockScsiLunVO.class)
@PythonClassInventory
public class BlockScsiLunInventory {
    private String uuid;
    private String name;
    private String wwn;
    private Long size;
    private Integer id;
    private String volumeUuid;
    private String target;
    private Integer lunMapId;
    private String lunType;
    private Integer lunInitSnapshotID;
    private long usedSize;
    private Timestamp createDate;
    private Timestamp lastOpDate;

    public static BlockScsiLunInventory valueOf(BlockScsiLunVO vo) {
        BlockScsiLunInventory inv = new BlockScsiLunInventory();
        inv.setUuid(vo.getUuid());
        inv.setUuid(vo.getUuid());
        inv.setCreateDate(vo.getCreateDate());
        inv.setLastOpDate(vo.getLastOpDate());
        inv.setLunInitSnapshotID(vo.getLunInitSnapshotID());
        inv.setLunMapId(vo.getLunMapId());
        inv.setId(vo.getId());
        inv.setLunType(vo.getLunType());
        inv.setName(vo.getName());
        inv.setWwn(vo.getWwn());
        inv.setUsedSize(vo.getUsedSize());
        inv.setVolumeUuid(vo.getVolumeUuid());
        inv.setSize(vo.getSize());
        return inv;
    }

    public static List<BlockScsiLunInventory> valueOf(Collection<BlockScsiLunVO> vos) {
        List<BlockScsiLunInventory> inventoryList = new ArrayList<>(vos.size());
        vos.forEach(blockScsiLunVO -> {
            inventoryList.add(BlockScsiLunInventory.valueOf(blockScsiLunVO));
        });
        return inventoryList;

    }

    public void setName(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public void setCreateDate(Timestamp createDate) {
        this.createDate = createDate;
    }

    public Timestamp getCreateDate() {
        return createDate;
    }

    public void setWwn(String wwn) {
        this.wwn = wwn;
    }

    public String getWwn() {
        return wwn;
    }

    public void setLastOpDate(Timestamp lastOpDate) {
        this.lastOpDate = lastOpDate;
    }

    public Timestamp getLastOpDate() {
        return lastOpDate;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getUuid() {
        return uuid;
    }

    public Long getSize() {
        return size;
    }

    public void setSize(Long size) {
        this.size = size;
    }

    public void setLunType(String lunType) {
        this.lunType = lunType;
    }

    public String getLunType() {
        return lunType;
    }

    public Integer getLunInitSnapshotID() {
        return lunInitSnapshotID;
    }

    public void setLunInitSnapshotID(Integer lunInitSnapshotID) {
        this.lunInitSnapshotID = lunInitSnapshotID;
    }

    public void setUsedSize(long usedSize) {
        this.usedSize = usedSize;
    }

    public long getUsedSize() {
        return usedSize;
    }

    public Integer getLunMapId() {
        return lunMapId;
    }

    public void setLunMapId(Integer lunMapId) {
        this.lunMapId = lunMapId;
    }

    public String getTarget() {
        return target;
    }

    public void setTarget(String target) {
        this.target = target;
    }

    public String getVolumeUuid() {
        return volumeUuid;
    }

    public void setVolumeUuid(String volumeUuid) {
        this.volumeUuid = volumeUuid;
    }
}
