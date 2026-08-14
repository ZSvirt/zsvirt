package org.zstack.ovf.datatype;

import org.zstack.header.query.ExpandedQueries;
import org.zstack.header.query.ExpandedQuery;
import org.zstack.header.search.Inventory;
import org.zstack.header.storage.backup.BackupStorageInventory;
import org.zstack.header.vm.VmInstanceInventory;

import java.sql.Timestamp;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Created by Qi Le on 2022/4/26
 */
@Inventory(mappingVOClass = ImagePackageVO.class)
@ExpandedQueries({
        @ExpandedQuery(expandedField = "vmInstance", inventoryClass = VmInstanceInventory.class,
                foreignKey = "vmUuid", expandedInventoryKey = "uuid"),
        @ExpandedQuery(expandedField = "backupStorage", inventoryClass = BackupStorageInventory.class,
                foreignKey = "backupStorageUuid", expandedInventoryKey = "uuid"),
})
public class ImagePackageInventory {
    private String uuid;
    private String name;
    private String description;
    private String vmUuid;
    private String backupStorageUuid;

    private ImagePackageState state;
    private String exportUrl;
    private String md5Sum;

    private String format;

    private Long size;
    private Timestamp createDate;
    private Timestamp lastOpDate;

    public static ImagePackageInventory valueOf(ImagePackageVO vo) {
        ImagePackageInventory inventory = new ImagePackageInventory();
        inventory.setUuid(vo.getUuid());
        inventory.setName(vo.getName());
        inventory.setDescription(vo.getDescription());
        inventory.setVmUuid(vo.getVmUuid());
        inventory.setBackupStorageUuid(vo.getBackupStorageUuid());
        inventory.setState(vo.getState());
        inventory.setExportUrl(vo.getExportUrl());
        inventory.setMd5Sum(vo.getMd5Sum());
        inventory.setFormat(vo.getFormat());
        inventory.setSize(vo.getSize());
        inventory.setCreateDate(vo.getCreateDate());
        inventory.setLastOpDate(vo.getLastOpDate());
        return inventory;
    }

    public static List<ImagePackageInventory> valueOf(List<ImagePackageVO> vos) {
        return vos.stream().map(ImagePackageInventory::valueOf).collect(Collectors.toList());
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

    public String getVmUuid() {
        return vmUuid;
    }

    public void setVmUuid(String vmUuid) {
        this.vmUuid = vmUuid;
    }

    public String getBackupStorageUuid() {
        return backupStorageUuid;
    }

    public void setBackupStorageUuid(String backupStorageUuid) {
        this.backupStorageUuid = backupStorageUuid;
    }

    public ImagePackageState getState() {
        return state;
    }

    public void setState(ImagePackageState state) {
        this.state = state;
    }

    public String getExportUrl() {
        return exportUrl;
    }

    public void setExportUrl(String exportUrl) {
        this.exportUrl = exportUrl;
    }

    public String getMd5Sum() {
        return md5Sum;
    }

    public void setMd5Sum(String md5Sum) {
        this.md5Sum = md5Sum;
    }

    public String getFormat() {
        return format;
    }

    public void setFormat(String format) {
        this.format = format;
    }

    public Long getSize() {
        return size;
    }

    public void setSize(Long size) {
        this.size = size;
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
}
