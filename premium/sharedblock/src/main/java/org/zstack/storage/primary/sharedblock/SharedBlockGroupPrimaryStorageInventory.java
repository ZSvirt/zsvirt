package org.zstack.storage.primary.sharedblock;

import org.zstack.header.configuration.PythonClassInventory;
import org.zstack.header.query.ExpandedQueries;
import org.zstack.header.query.ExpandedQuery;
import org.zstack.header.query.Queryable;
import org.zstack.header.search.Inventory;
import org.zstack.header.search.Parent;
import org.zstack.header.storage.primary.PrimaryStorageInventory;

import javax.persistence.JoinColumn;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@PythonClassInventory
@Inventory(mappingVOClass = SharedBlockGroupVO.class, collectionValueOfMethod = "valueOf1",
        parent = {@Parent(inventoryClass = PrimaryStorageInventory.class, type = SharedBlockConstants.SHARED_BLOCK_PRIMARY_STORAGE_TYPE)})
@ExpandedQueries({
        @ExpandedQuery(expandedField = "sharedBlocks", inventoryClass = SharedBlockInventory.class,
                foreignKey = "uuid", expandedInventoryKey = "sharedBlockGroupUuid")
})
public class SharedBlockGroupPrimaryStorageInventory extends PrimaryStorageInventory {
    @Queryable(mappingClass = SharedBlockInventory.class,
            joinColumn = @JoinColumn(name = "sharedBlockGroupUuid"))
    private List<SharedBlockInventory> sharedBlocks;

    private SharedBlockGroupType sharedBlockGroupType;

    public SharedBlockGroupPrimaryStorageInventory() {
    }

    public SharedBlockGroupPrimaryStorageInventory(SharedBlockGroupVO vo) {
        super(vo);
        this.setSharedBlocks(SharedBlockInventory.valueOf1(vo.getSharedBlocks()));
        this.setSharedBlockGroupType(vo.getSharedBlockGroupType());
    }

    public static SharedBlockGroupPrimaryStorageInventory valueOf(SharedBlockGroupVO vo) {
        return new SharedBlockGroupPrimaryStorageInventory(vo);
    }

    public static List<SharedBlockGroupPrimaryStorageInventory> valueOf1(Collection<SharedBlockGroupVO> vos) {
        List<SharedBlockGroupPrimaryStorageInventory> invs = new ArrayList<>();
        for (SharedBlockGroupVO vo : vos) {
            invs.add(valueOf(vo));
        }

        return invs;
    }

    public List<SharedBlockInventory> getSharedBlocks() {
        return sharedBlocks;
    }

    public void setSharedBlocks(List<SharedBlockInventory> sharedBlocks) {
        this.sharedBlocks = sharedBlocks;
    }

    public SharedBlockGroupType getSharedBlockGroupType() {
        return sharedBlockGroupType;
    }

    public void setSharedBlockGroupType(SharedBlockGroupType sharedBlockGroupType) {
        this.sharedBlockGroupType = sharedBlockGroupType;
    }
}
