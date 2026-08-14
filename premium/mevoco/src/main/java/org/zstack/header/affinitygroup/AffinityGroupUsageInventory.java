package org.zstack.header.affinitygroup;

import org.zstack.header.search.Inventory;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 *  * Created by shixin on 10/24/2015.
 *   */
@Inventory(mappingVOClass = AffinityGroupUsageVO.class)
public class AffinityGroupUsageInventory {
    private String uuid;
    private String affinityGroupUuid;
    private String resourceUuid;
    private String resourceType;
    private Timestamp createDate;
    private Timestamp lastOpDate;

    public static AffinityGroupUsageInventory valueOf(AffinityGroupUsageVO vo) {
        AffinityGroupUsageInventory inv = new AffinityGroupUsageInventory();
        inv.setUuid(vo.getUuid());
        inv.setAffinityGroupUuid(vo.getAffinityGroupUuid());
        inv.setResourceType(vo.getResourceType());
        inv.setResourceUuid(vo.getResourceUuid());
        inv.setCreateDate(vo.getCreateDate());
        inv.setLastOpDate(vo.getLastOpDate());
        return inv;
    }

    public static List<AffinityGroupUsageInventory> valueOf(Collection<AffinityGroupUsageVO> vos) {
        List<AffinityGroupUsageInventory> invs = new ArrayList<AffinityGroupUsageInventory>();
        for (AffinityGroupUsageVO vo : vos) {
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

    public String getResourceUuid() {
        return resourceUuid;
    }

    public void setResourceUuid(String resourceUuid) {
        this.resourceUuid = resourceUuid;
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

    public String getAffinityGroupUuid() {
        return affinityGroupUuid;
    }

    public void setAffinityGroupUuid(String affinityGroupUuid) {
        this.affinityGroupUuid = affinityGroupUuid;
    }

    public Timestamp getLastOpDate() {
        return lastOpDate;
    }

    public void setLastOpDate(Timestamp lastOpDate) {
        this.lastOpDate = lastOpDate;
    }

}
