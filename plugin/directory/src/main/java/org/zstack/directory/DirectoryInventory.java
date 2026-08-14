package org.zstack.directory;

import org.zstack.header.query.ExpandedQueries;
import org.zstack.header.query.ExpandedQuery;
import org.zstack.header.search.Inventory;
import org.zstack.header.zone.ZoneInventory;

import java.io.Serializable;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * @author shenjin
 * @date 2022/11/29 13:21
 */
@Inventory(mappingVOClass = DirectoryVO.class)
@ExpandedQueries({
        @ExpandedQuery(expandedField = "zone", inventoryClass = ZoneInventory.class,
                foreignKey = "zoneUuid", expandedInventoryKey = "uuid")
})
public class DirectoryInventory implements Serializable {
    private String uuid;
    private String name;
    private String groupName;
    private String parentUuid;
    private String rootDirectoryUuid;
    private String zoneUuid;
    private String type;
    private Timestamp createDate;
    private Timestamp lastOpDate;

    public static DirectoryInventory valueOf(DirectoryVO vo) {
        DirectoryInventory inv = new DirectoryInventory();
        inv.setName(vo.getName());
        inv.setUuid(vo.getUuid());
        inv.setGroupName(vo.getGroupName());
        inv.setParentUuid(vo.getParentUuid());
        inv.setRootDirectoryUuid(vo.getRootDirectoryUuid());
        inv.setZoneUuid(vo.getZoneUuid());
        inv.setType(vo.getType());
        inv.setCreateDate(vo.getCreateDate());
        inv.setLastOpDate(vo.getLastOpDate());
        return inv;
    }

    public static List<DirectoryInventory> valueOf(Collection<DirectoryVO> vos) {
        List<DirectoryInventory> invs = new ArrayList<>();
        for (DirectoryVO vo : vos) {
            invs.add(valueOf(vo));
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

    public String getGroupName() {
        return groupName;
    }

    public void setGroupName(String groupName) {
        this.groupName = groupName;
    }

    public String getParentUuid() {
        return parentUuid;
    }

    public void setParentUuid(String parentUuid) {
        this.parentUuid = parentUuid;
    }

    public String getRootDirectoryUuid() {
        return rootDirectoryUuid;
    }

    public void setRootDirectoryUuid(String rootDirectoryUuid) {
        this.rootDirectoryUuid = rootDirectoryUuid;
    }

    public String getZoneUuid() {
        return zoneUuid;
    }

    public void setZoneUuid(String zoneUuid) {
        this.zoneUuid = zoneUuid;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
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
