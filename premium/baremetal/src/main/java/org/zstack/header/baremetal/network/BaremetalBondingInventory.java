package org.zstack.header.baremetal.network;

import org.zstack.header.baremetal.chassis.BaremetalChassisInventory;
import org.zstack.header.baremetal.chassis.BaremetalChassisVO;
import org.zstack.header.message.DocUtils;
import org.zstack.header.query.ExpandedQueries;
import org.zstack.header.query.ExpandedQuery;
import org.zstack.header.search.Inventory;
import org.zstack.utils.StringDSL;

import java.io.Serializable;
import java.sql.Timestamp;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Created by GuoYi on 2019-01-03.
 */
@Inventory(mappingVOClass = BaremetalBondingVO.class)
@ExpandedQueries({
        @ExpandedQuery(expandedField = "baremetalChassis", inventoryClass = BaremetalChassisInventory.class,
                foreignKey = "chassisUuid", expandedInventoryKey = "uuid"),
})
public class BaremetalBondingInventory implements Serializable {
    private String uuid;
    private String chassisUuid;
    private String name;
    private Integer mode;
    private String slaves;
    private String opts;
    private Timestamp createDate;
    private Timestamp lastOpDate;

    public BaremetalBondingInventory() {
    }

    public BaremetalBondingInventory(BaremetalBondingVO vo) {
        this.setUuid(vo.getUuid());
        this.setChassisUuid(vo.getChassisUuid());
        this.setName(vo.getName());
        this.setMode(vo.getMode());
        this.setSlaves(vo.getSlaves());
        this.setOpts(vo.getOpts());
        this.setCreateDate(vo.getCreateDate());
        this.setLastOpDate(vo.getLastOpDate());
    }

    public static BaremetalBondingInventory valueOf(BaremetalBondingVO vo) {
        return new BaremetalBondingInventory(vo);
    }

    public static List<BaremetalBondingInventory> valueOf(Collection<BaremetalBondingVO> vos) {
        return vos.stream().map(BaremetalBondingInventory::valueOf).collect(Collectors.toList());
    }

    public static BaremetalBondingInventory __example__() {
        BaremetalBondingInventory inv = new BaremetalBondingInventory();
        inv.setUuid(StringDSL.createFixedUuid(BaremetalBondingVO.class));
        inv.setChassisUuid(StringDSL.createFixedUuid(BaremetalChassisVO.class));
        inv.setName("bond0");
        inv.setMode(1);
        inv.setSlaves("[\"d4:ae:52:6e:d1:0d\", \"d4:ae:52:6e:d1:0e\"]");
        inv.setOpts("miimon=100");
        inv.createDate = new Timestamp(DocUtils.date);
        inv.lastOpDate = new Timestamp(DocUtils.date);
        return inv;
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

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Integer getMode() {
        return mode;
    }

    public void setMode(Integer mode) {
        this.mode = mode;
    }

    public String getSlaves() {
        return slaves;
    }

    public void setSlaves(String slaves) {
        this.slaves = slaves;
    }

    public String getOpts() {
        return opts;
    }

    public void setOpts(String opts) {
        this.opts = opts;
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
