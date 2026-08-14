package org.zstack.header.cbt;

import org.zstack.header.configuration.PythonClassInventory;
import org.zstack.header.query.ExpandedQueries;
import org.zstack.header.query.ExpandedQuery;
import org.zstack.header.search.Inventory;

import java.io.Serializable;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@PythonClassInventory
@Inventory(mappingVOClass = VolumeCbtBackupRecordVO.class, collectionValueOfMethod = "valueOf1")
@ExpandedQueries({
        @ExpandedQuery(expandedField = "task", inventoryClass = CbtTaskInventory.class,
                foreignKey = "taskUuid", expandedInventoryKey = "uuid"),
})
public class VolumeCbtBackupRecordInventory implements Serializable {
    private long id;
    private String taskUuid;
    private String volumeUuid;
    private String mode;
    private String target;
    private String scratchNodeName;
    private String bitmapName;
    private String lastBitmapName;
    private Timestamp createDate;
    private Timestamp lastOpDate;

    protected VolumeCbtBackupRecordInventory(VolumeCbtBackupRecordVO vo) {
        this.setId(vo.getId());
        this.setTaskUuid(vo.getTaskUuid());
        this.setVolumeUuid(vo.getVolumeUuid());
        this.setMode(vo.getMode());
        this.setTarget(vo.getTarget());
        this.setScratchNodeName(vo.getScratchNodeName());
        this.setBitmapName(vo.getBitmapName());
        this.setLastBitmapName(vo.getLastBitmapName());
        this.setCreateDate(vo.getCreateDate());
        this.setLastOpDate(vo.getLastOpDate());
    }

    public static VolumeCbtBackupRecordInventory valueOf(VolumeCbtBackupRecordVO vo) {
        return new VolumeCbtBackupRecordInventory(vo);
    }

    public static List<VolumeCbtBackupRecordInventory> valueOf1(Collection<VolumeCbtBackupRecordVO> vos) {
        List<VolumeCbtBackupRecordInventory> invs = new ArrayList<VolumeCbtBackupRecordInventory>(vos.size());
        for (VolumeCbtBackupRecordVO vo : vos) {
            invs.add(VolumeCbtBackupRecordInventory.valueOf(vo));
        }
        return invs;
    }

    public VolumeCbtBackupRecordInventory() {
    }

    public String getTaskUuid() {
        return taskUuid;
    }

    public void setTaskUuid(String taskUuid) {
        this.taskUuid = taskUuid;
    }

    public String getVolumeUuid() {
        return volumeUuid;
    }

    public void setVolumeUuid(String volumeUuid) {
        this.volumeUuid = volumeUuid;
    }

    public String getMode() {
        return mode;
    }

    public void setMode(String mode) {
        this.mode = mode;
    }

    public String getTarget() {
        return target;
    }

    public void setTarget(String target) {
        this.target = target;
    }

    public String getScratchNodeName() {
        return scratchNodeName;
    }

    public void setScratchNodeName(String scratchNodeName) {
        this.scratchNodeName = scratchNodeName;
    }

    public String getBitmapName() {
        return bitmapName;
    }

    public void setBitmapName(String bitmapName) {
        this.bitmapName = bitmapName;
    }

    public String getLastBitmapName() {
        return lastBitmapName;
    }

    public void setLastBitmapName(String lastBitmapName) {
        this.lastBitmapName = lastBitmapName;
    }

    public Timestamp getCreateDate() {
        return createDate;
    }

    public void setCreateDate(Timestamp createDate) {
        this.createDate = createDate;
    }

    public Timestamp getLastOpDate() {
        return lastOpDate;
    }

    public void setLastOpDate(Timestamp lastOpDate) {
        this.lastOpDate = lastOpDate;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }
}
