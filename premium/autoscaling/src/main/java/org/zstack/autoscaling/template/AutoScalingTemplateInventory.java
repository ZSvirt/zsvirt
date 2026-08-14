package org.zstack.autoscaling.template;

import org.zstack.header.configuration.PythonClassInventory;
import org.zstack.header.network.l2.L2NetworkClusterRefInventory;
import org.zstack.header.network.l3.L3NetworkInventory;
import org.zstack.header.query.ExpandedQueries;
import org.zstack.header.query.ExpandedQuery;
import org.zstack.header.query.ExpandedQueryAlias;
import org.zstack.header.query.ExpandedQueryAliases;
import org.zstack.header.search.Inventory;
import org.zstack.header.tag.SystemTagVO;
import org.zstack.header.zone.ZoneInventory;

import java.io.Serializable;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Create by weiwang at 2018/8/19
 */
@PythonClassInventory
@Inventory(mappingVOClass = AutoScalingTemplateVO.class, collectionValueOfMethod = "valueOf1")
@ExpandedQueries({
        @ExpandedQuery(expandedField = "autoScalingGroupRef", inventoryClass = AutoScalingTemplateGroupRefInventory.class,
                foreignKey = "uuid", expandedInventoryKey = "templateUuid", hidden = true),
})
@ExpandedQueryAliases({
        @ExpandedQueryAlias(alias = "autoScalingGroup", expandedField = "autoScalingGroupRef.autoScalingGroup")
})
public class AutoScalingTemplateInventory implements Serializable {
    private String uuid;

    private String name;

    private String description;

    private String type;

    private String state;

    private List<String> systemTags;

    private Timestamp createDate;

    private Timestamp lastOpDate;

    public AutoScalingTemplateInventory() {
    }

    public AutoScalingTemplateInventory(AutoScalingTemplateVO vo) {
        this.setUuid(vo.getUuid());
        this.setName(vo.getResourceName());
        this.setDescription(vo.getDescription());
        this.setState(vo.getState().toString());
        this.setCreateDate(vo.getCreateDate());
        this.setLastOpDate(vo.getLastOpDate());
        this.setType(vo.getType());
    }

    public static AutoScalingTemplateInventory valueOf(AutoScalingTemplateVO vo) {
        return new AutoScalingTemplateInventory(vo);
    }

    public static List<AutoScalingTemplateInventory> valueOf1(Collection<AutoScalingTemplateVO> vos) {
        List<AutoScalingTemplateInventory> invs = new ArrayList<AutoScalingTemplateInventory>();
        for (AutoScalingTemplateVO vo : vos) {
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

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public List<String> getSystemTags() {
        return systemTags;
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

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }
}
