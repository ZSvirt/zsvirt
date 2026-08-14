package org.zstack.directory;

import org.zstack.header.query.ExpandedQueries;
import org.zstack.header.query.ExpandedQuery;
import org.zstack.header.search.Inventory;
import org.zstack.header.vo.ResourceInventory;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * @author shenjin
 * @date 2022/11/29 11:53
 */
@Inventory(mappingVOClass = ResourceDirectoryRefVO.class)
@ExpandedQueries({
        @ExpandedQuery(expandedField = "resource", inventoryClass = ResourceInventory.class,
                foreignKey = "resourceUuid", expandedInventoryKey = "uuid"),
        @ExpandedQuery(expandedField = "directory", inventoryClass = DirectoryInventory.class,
                foreignKey = "directoryUuid", expandedInventoryKey = "uuid")
})
public class ResourceDirectoryRefInventory {
    private Long id;
    private String resourceUuid;
    private String directoryUuid;
    private String resourceType;
    private Timestamp createDate;
    private Timestamp lastOpDate;

    public static ResourceDirectoryRefInventory valueOf(ResourceDirectoryRefVO vo) {
        ResourceDirectoryRefInventory inv = new ResourceDirectoryRefInventory();
        inv.setId(vo.getId());
        inv.setResourceUuid(vo.getResourceUuid());
        inv.setDirectoryUuid(vo.getDirectoryUuid());
        inv.setResourceType(vo.getResourceType());
        inv.setCreateDate(vo.getCreateDate());
        inv.setLastOpDate(vo.getLastOpDate());
        return inv;
    }

    public static List<ResourceDirectoryRefInventory> valueOf(Collection<ResourceDirectoryRefVO> vos) {
        List<ResourceDirectoryRefInventory> invs = new ArrayList<>();
        for (ResourceDirectoryRefVO vo : vos) {
            invs.add(valueOf(vo));
        }
        return invs;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getResourceUuid() {
        return resourceUuid;
    }

    public void setResourceUuid(String resourceUuid) {
        this.resourceUuid = resourceUuid;
    }

    public String getDirectoryUuid() {
        return directoryUuid;
    }

    public void setDirectoryUuid(String directoryUuid) {
        this.directoryUuid = directoryUuid;
    }

    public String getResourceType() {
        return resourceType;
    }

    public void setResourceType(String resourceType) {
        this.resourceType = resourceType;
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
