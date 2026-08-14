package org.zstack.imagereplicator;

import org.zstack.header.configuration.PythonClassInventory;
import org.zstack.header.query.*;
import org.zstack.header.search.Inventory;

import javax.persistence.JoinColumn;
import java.io.Serializable;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@Inventory(mappingVOClass = ImageReplicationGroupVO.class)
@PythonClassInventory
@ExpandedQueries({
        @ExpandedQuery(expandedField = "backupStorageRef", inventoryClass = ImageReplicationGroupBackupStorageRefInventory.class,
                foreignKey = "uuid", expandedInventoryKey = "replicationGroupUuid", hidden = true),
})
@ExpandedQueryAliases({
        @ExpandedQueryAlias(alias = "backupStorage", expandedField = "backupStorageRef.backupStorage")
})
public class ImageReplicationGroupInventory implements Serializable {
    private String uuid;
    private String name;
    private String description;
    private ReplicationGroupState state;
    private Timestamp createDate;
    private Timestamp lastOpDate;
    @Queryable(mappingClass = ImageReplicationGroupBackupStorageRefInventory.class,
            joinColumn = @JoinColumn(name = "replicationGroupUuid"))
    private List<ImageReplicationGroupBackupStorageRefInventory> backupStorageRefs;

    public static ImageReplicationGroupInventory valueOf(ImageReplicationGroupVO vo) {
        ImageReplicationGroupInventory inv = new ImageReplicationGroupInventory();
        inv.setUuid(vo.getUuid());
        inv.setName(vo.getName());
        inv.setDescription(vo.getDescription());
        inv.setState(vo.getState());
        inv.setCreateDate(vo.getCreateDate());
        inv.setLastOpDate(vo.getLastOpDate());
        inv.setBackupStorageRefs(ImageReplicationGroupBackupStorageRefInventory.valueOf(vo.getBackupStorageRefs()));
        return inv;
    }

    public static List<ImageReplicationGroupInventory> valueOf(Collection<ImageReplicationGroupVO> vos) {
        List<ImageReplicationGroupInventory> invs = new ArrayList<>(vos.size());
        for (ImageReplicationGroupVO vo : vos) {
            invs.add(ImageReplicationGroupInventory.valueOf(vo));
        }
        return invs;
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

    public ReplicationGroupState getState() {
        return state;
    }

    public void setState(ReplicationGroupState state) {
        this.state = state;
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

    public List<ImageReplicationGroupBackupStorageRefInventory> getBackupStorageRefs() {
        return backupStorageRefs;
    }

    public void setBackupStorageRefs(List<ImageReplicationGroupBackupStorageRefInventory> backupStorageRefs) {
        this.backupStorageRefs = backupStorageRefs;
    }
}
