package org.zstack.header.image;

import org.zstack.header.query.ExpandedQueries;
import org.zstack.header.query.ExpandedQuery;
import org.zstack.header.rest.APINoSee;
import org.zstack.header.search.Inventory;
import org.zstack.header.storage.backup.BackupStorageInventory;

import java.io.Serializable;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 */
@Inventory(mappingVOClass = ImageBackupStorageRefVO.class)
@ExpandedQueries({
        @ExpandedQuery(expandedField = "image", inventoryClass = ImageInventory.class,
                foreignKey = "imageUuid", expandedInventoryKey = "uuid"),
        @ExpandedQuery(expandedField = "backupStorage", inventoryClass = BackupStorageInventory.class,
                foreignKey = "backupStorageUuid", expandedInventoryKey = "uuid"),
})
public class ImageBackupStorageRefInventory implements Serializable {
    @APINoSee
    private long id;
    private String imageUuid;
    private String backupStorageUuid;
    private String installPath;
    private String status;
    private String exportMd5Sum;
    private String exportUrl;
    private Timestamp createDate;
    private Timestamp lastOpDate;

    public static ImageBackupStorageRefInventory valueOf(ImageBackupStorageRefVO vo) {
        ImageBackupStorageRefInventory inv = new ImageBackupStorageRefInventory();
        inv.setBackupStorageUuid(vo.getBackupStorageUuid());
        inv.setImageUuid(vo.getImageUuid());
        inv.setInstallPath(vo.getInstallPath());
        inv.setCreateDate(vo.getCreateDate());
        inv.setLastOpDate(vo.getLastOpDate());
        inv.setId(vo.getId());
        inv.setStatus(vo.getStatus().toString());
        inv.setExportMd5Sum(vo.getExportMd5Sum());
        inv.setExportUrl(vo.getExportUrl());
        return inv;
    }

    public static List<ImageBackupStorageRefInventory> valueOf(Collection<ImageBackupStorageRefVO> vos) {
        List<ImageBackupStorageRefInventory> invs = new ArrayList<ImageBackupStorageRefInventory>();
        for (ImageBackupStorageRefVO vo : vos) {
            invs.add(valueOf(vo));
        }
        return invs;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getImageUuid() {
        return imageUuid;
    }

    public void setImageUuid(String imageUuid) {
        this.imageUuid = imageUuid;
    }

    public String getBackupStorageUuid() {
        return backupStorageUuid;
    }

    public void setBackupStorageUuid(String backupStorageUuid) {
        this.backupStorageUuid = backupStorageUuid;
    }

    public String getInstallPath() {
        return installPath;
    }

    public void setInstallPath(String installPath) {
        this.installPath = installPath;
    }

    public String getExportMd5Sum() {
        return exportMd5Sum;
    }

    public void setExportMd5Sum(String exportMd5Sum) {
        this.exportMd5Sum = exportMd5Sum;
    }

    public String getExportUrl() {
        return exportUrl;
    }

    public void setExportUrl(String exportUrl) {
        this.exportUrl = exportUrl;
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
