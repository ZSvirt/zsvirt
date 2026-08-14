package org.zstack.storage.backup.imagestore;

import org.zstack.header.configuration.PythonClassInventory;
import org.zstack.header.image.ImageBackupStorageRefInventory;
import org.zstack.header.query.ExpandedQueries;
import org.zstack.header.query.ExpandedQuery;
import org.zstack.header.query.ExpandedQueryAlias;
import org.zstack.header.query.ExpandedQueryAliases;
import org.zstack.header.search.Inventory;
import org.zstack.header.search.Parent;
import org.zstack.header.storage.backup.BackupStorageInventory;
import org.zstack.header.storage.backup.BackupStorageZoneRefInventory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@Inventory(mappingVOClass = ImageStoreBackupStorageVO.class, collectionValueOfMethod="valueOf1",
        parent = {@Parent(inventoryClass = BackupStorageInventory.class, type = ImageStoreBackupStorageConstant.IMAGE_STORE_BACKUP_STORAGE_TYPE)})
@PythonClassInventory
@ExpandedQueries({
        @ExpandedQuery(expandedField = "zoneRef", inventoryClass = BackupStorageZoneRefInventory.class,
                foreignKey = "uuid", expandedInventoryKey = "backupStorageUuid", hidden = true),
        @ExpandedQuery(expandedField = "imageRef", inventoryClass = ImageBackupStorageRefInventory.class,
                foreignKey = "uuid", expandedInventoryKey = "backupStorageUuid", hidden = true),
})
@ExpandedQueryAliases({
        @ExpandedQueryAlias(alias = "zone", expandedField = "zoneRef.zone"),
        @ExpandedQueryAlias(alias = "image", expandedField = "imageRef.image"),
})
public class ImageStoreBackupStorageInventory extends BackupStorageInventory {
    public String hostname;
    public String username;
    public Integer sshPort;

    protected ImageStoreBackupStorageInventory(ImageStoreBackupStorageVO vo) {
        super(vo);
        hostname = vo.getHostname();
        username = vo.getUsername();
        sshPort = vo.getSshPort();
    }

    public ImageStoreBackupStorageInventory() {
    }

    public static ImageStoreBackupStorageInventory valueOf(ImageStoreBackupStorageVO vo) {
        ImageStoreBackupStorageInventory inv = new ImageStoreBackupStorageInventory(vo);
        return inv;
    }

    public static List<ImageStoreBackupStorageInventory> valueOf1(Collection<ImageStoreBackupStorageVO> vos) {
        List<ImageStoreBackupStorageInventory> invs = new ArrayList<ImageStoreBackupStorageInventory>(vos.size());
        for (ImageStoreBackupStorageVO vo : vos) {
            ImageStoreBackupStorageInventory inv = ImageStoreBackupStorageInventory.valueOf(vo);
            invs.add(inv);
        }
        return invs;
    }

    public String getHostname() {
        return hostname;
    }

    public void setHostname(String hostname) {
        this.hostname = hostname;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {

        this.username = username;
    }

    public Integer getSshPort() {
        return sshPort;
    }

    public void setSshPort(Integer sshPort) {
        this.sshPort = sshPort;
    }

}
