package org.zstack.header.tag;

import org.zstack.header.network.l2.L2NetworkInventory;
import org.zstack.header.network.l3.UsedIpInventory;
import org.zstack.header.network.service.NetworkServiceL3NetworkRefInventory;
import org.zstack.header.query.ExpandedQueries;
import org.zstack.header.query.ExpandedQuery;
import org.zstack.header.search.Inventory;
import org.zstack.header.vm.VmNicInventory;
import org.zstack.header.zone.ZoneInventory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 */
@Inventory(mappingVOClass = UserTagVO.class)
@ExpandedQueries({
        @ExpandedQuery(expandedField = "tagPattern", inventoryClass = TagPatternInventory.class,
                foreignKey = "tagPatternUuid", expandedInventoryKey = "uuid"),
})
public class UserTagInventory extends TagInventory {
    private String tagPatternUuid;

    private TagPatternInventory tagPattern;

    public static UserTagInventory valueOf(UserTagVO vo) {
        UserTagInventory inv = new UserTagInventory();
        inv.setUuid(vo.getUuid());
        inv.setType(vo.getType().toString());
        inv.setTag(vo.getTag());
        inv.setResourceUuid(vo.getResourceUuid());
        inv.setCreateDate(vo.getCreateDate());
        inv.setLastOpDate(vo.getLastOpDate());
        inv.setResourceType(vo.getResourceType());
        inv.setTagPatternUuid(vo.getTagPatternUuid());
        if (vo.getTagPattern() != null) {
            inv.setTagPattern(TagPatternInventory.valueOf(vo.getTagPattern()));
        }
        return inv;
    }

    public static List<UserTagInventory> valueOf(Collection<UserTagVO> vos) {
        List<UserTagInventory> invs = new ArrayList<UserTagInventory>();
        for (UserTagVO vo : vos) {
            invs.add(valueOf(vo));
        }
        return invs;
    }

    public String getTagPatternUuid() {
        return tagPatternUuid;
    }

    public void setTagPatternUuid(String tagPatternUuid) {
        this.tagPatternUuid = tagPatternUuid;
    }

    public TagPatternInventory getTagPattern() {
        return tagPattern;
    }

    public void setTagPattern(TagPatternInventory tagPattern) {
        this.tagPattern = tagPattern;
    }
}
