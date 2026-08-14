package org.zstack.autoscaling.template;

import org.zstack.autoscaling.group.AutoScalingGroupInventory;
import org.zstack.header.cluster.ClusterInventory;
import org.zstack.header.network.l2.L2NetworkClusterRefVO;
import org.zstack.header.network.l2.L2NetworkInventory;
import org.zstack.header.query.ExpandedQueries;
import org.zstack.header.query.ExpandedQuery;
import org.zstack.header.search.Inventory;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@Inventory(mappingVOClass = AutoScalingTemplateGroupRefVO.class)
@ExpandedQueries({
        @ExpandedQuery(expandedField = "autoScalingTemplate", inventoryClass = AutoScalingTemplateInventory.class,
                foreignKey = "templateUuid", expandedInventoryKey = "uuid"),
        @ExpandedQuery(expandedField = "autoScalingGroup", inventoryClass = AutoScalingGroupInventory.class,
                foreignKey = "groupUuid", expandedInventoryKey = "uuid"),
})
public class AutoScalingTemplateGroupRefInventory {
    private String templateUuid;
    private String groupUuid;
    private Timestamp createDate;
    private Timestamp lastOpDate;

    public static AutoScalingTemplateGroupRefInventory valueOf(AutoScalingTemplateGroupRefVO vo) {
        AutoScalingTemplateGroupRefInventory inv = new AutoScalingTemplateGroupRefInventory();
        inv.setGroupUuid(vo.getGroupUuid());
        inv.setTemplateUuid(vo.getTemplateUuid());
        inv.setCreateDate(vo.getCreateDate());
        inv.setLastOpDate(vo.getLastOpDate());
        return inv;
    }

    public static List<AutoScalingTemplateGroupRefInventory> valueOf(Collection<AutoScalingTemplateGroupRefVO> vos) {
        List<AutoScalingTemplateGroupRefInventory> invs = new ArrayList<AutoScalingTemplateGroupRefInventory>();
        for (AutoScalingTemplateGroupRefVO vo : vos) {
            invs.add(valueOf(vo));
        }
        return invs;
    }

    public String getTemplateUuid() {
        return templateUuid;
    }

    public void setTemplateUuid(String templateUuid) {
        this.templateUuid = templateUuid;
    }

    public String getGroupUuid() {
        return groupUuid;
    }

    public void setGroupUuid(String groupUuid) {
        this.groupUuid = groupUuid;
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
