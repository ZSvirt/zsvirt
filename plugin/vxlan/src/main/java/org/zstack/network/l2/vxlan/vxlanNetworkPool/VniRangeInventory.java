package org.zstack.network.l2.vxlan.vxlanNetworkPool;

import org.zstack.header.configuration.PythonClassInventory;
import org.zstack.header.query.ExpandedQueries;
import org.zstack.header.query.ExpandedQuery;
import org.zstack.header.query.Queryable;
import org.zstack.header.search.Inventory;
import org.zstack.network.l2.vxlan.vxlanNetwork.VxlanNetworkVO;

import javax.persistence.JoinColumn;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Created by weiwang on 08/03/2017.
 */
@PythonClassInventory
@Inventory(mappingVOClass = VniRangeVO.class)
@ExpandedQueries({
        @ExpandedQuery(expandedField = "vxlanPool", inventoryClass = L2VxlanNetworkPoolInventory.class,
                foreignKey = "l2NetworkUuid", expandedInventoryKey = "uuid")
})
public class VniRangeInventory {
    private String uuid;

    private String name;

    private String description;

    private Integer startVni;

    private Integer endVni;

    private Timestamp createDate;

    private Timestamp lastOpDate;

    private String l2NetworkUuid;

    public VniRangeInventory() {
    }

    public VniRangeInventory(VniRangeVO vo) {
        this.uuid = vo.getUuid();
        this.name = vo.getName();
        this.description = vo.getDescription();
        this.startVni = vo.getStartVni();
        this.endVni = vo.getEndVni();
        this.l2NetworkUuid = vo.getL2NetworkUuid();
        this.createDate = vo.getCreateDate();
        this.lastOpDate = vo.getLastOpDate();
    }

    public static VniRangeInventory valueOf(VniRangeVO vo) {
        return new VniRangeInventory(vo);
    }

    public static List<VniRangeInventory> valueOf(Collection<VniRangeVO> vos) {
        List<VniRangeInventory> invs = new ArrayList<>();
        for (VniRangeVO vo : vos) {
            invs.add(new VniRangeInventory(vo));
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

    public Integer getStartVni() {
        return startVni;
    }

    public void setStartVni(Integer startVni) {
        this.startVni = startVni;
    }

    public Integer getEndVni() {
        return endVni;
    }

    public void setEndVni(Integer endVni) {
        this.endVni = endVni;
    }

    public String getL2NetworkUuid() {
        return l2NetworkUuid;
    }

    public void setL2NetworkUuid(String l2NetworkUuid) {
        this.l2NetworkUuid = l2NetworkUuid;
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
