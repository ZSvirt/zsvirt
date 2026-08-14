package org.zstack.header.baremetal.chassis;

import org.zstack.header.query.ExpandedQueries;
import org.zstack.header.query.ExpandedQuery;
import org.zstack.header.search.Inventory;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Created by GuoYi on 17/4/20.
 */
@Inventory(mappingVOClass = BaremetalHardwareInfoVO.class)
@ExpandedQueries({
        @ExpandedQuery(expandedField = "baremetalChassis", inventoryClass = BaremetalChassisInventory.class,
                foreignKey = "chassisUuid", expandedInventoryKey = "uuid"),
})
public class BaremetalHardwareInfoInventory {
    private String uuid;
    private String chassisUuid;
    private String type;
    private String content;
    private Timestamp createDate;
    private Timestamp lastOpDate;

    public BaremetalHardwareInfoInventory(){

    }

    public static BaremetalHardwareInfoInventory valueOf(BaremetalHardwareInfoVO vo) {
        BaremetalHardwareInfoInventory inv = new BaremetalHardwareInfoInventory();
        inv.setUuid(vo.getUuid());
        inv.setChassisUuid(vo.getChassisUuid());
        inv.setType(vo.getType());
        inv.setContent(vo.getContent());
        inv.setCreateDate(vo.getCreateDate());
        inv.setLastOpDate(vo.getLastOpDate());
        return inv;
    }

    public static List<BaremetalHardwareInfoInventory> valueOf(Collection<BaremetalHardwareInfoVO> vos) {
        List<BaremetalHardwareInfoInventory> inventories = new ArrayList<>();
        for (BaremetalHardwareInfoVO vo: vos) {
            inventories.add(valueOf(vo));
        }
        return inventories;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getChassisUuid() {
        return chassisUuid;
    }

    public void setChassisUuid(String chassisUuid) {
        this.chassisUuid = chassisUuid;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
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
