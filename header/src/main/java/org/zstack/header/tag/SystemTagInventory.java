package org.zstack.header.tag;

import org.zstack.header.search.Inventory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 */
@Inventory(mappingVOClass = SystemTagVO.class)
public class SystemTagInventory extends TagInventory {
    private Boolean inherent;

    public static SystemTagInventory valueOf(SystemTagVO vo) {
        SystemTagInventory inv = new SystemTagInventory();
        inv.setCreateDate(vo.getCreateDate());
        inv.setLastOpDate(vo.getCreateDate());
        inv.setResourceType(vo.getResourceType());
        inv.setResourceUuid(vo.getResourceUuid());
        inv.setTag(vo.getTag());
        inv.setType(vo.getType().toString());
        inv.setUuid(vo.getUuid());
        inv.setInherent(vo.isInherent());
        return inv;
    }

    public static List<SystemTagInventory> valueOf(Collection<SystemTagVO> vos) {
        List<SystemTagInventory> invs = new ArrayList<SystemTagInventory>();
        for (SystemTagVO vo : vos) {
            invs.add(valueOf(vo));
        }
        return invs;
    }

    public boolean isInherent() {
        return inherent;
    }

    public void setInherent(boolean inherent) {
        this.inherent = inherent;
    }
}
