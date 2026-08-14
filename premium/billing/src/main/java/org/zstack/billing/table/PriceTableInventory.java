package org.zstack.billing.table;

import org.zstack.header.search.Inventory;
import java.sql.Timestamp;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Created by lining on 2019/9/10.
 */
@Inventory(mappingVOClass = PriceTableVO.class)
public class PriceTableInventory {
    private String uuid;
    private String name;
    private String description;
    private Timestamp createDate;
    private Timestamp lastOpDate;

    public static PriceTableInventory valueOf(PriceTableVO co) {
        PriceTableInventory inv = new PriceTableInventory();
        inv.setUuid(co.getUuid());
        inv.setName(co.getName());
        inv.setDescription(co.getDescription());
        inv.setCreateDate(co.getCreateDate());
        inv.setLastOpDate(co.getLastOpDate());
        return inv;
    }

    public static List<PriceTableInventory> valueOf(Collection<PriceTableVO> cos) {
        return cos.stream().map(PriceTableInventory::valueOf).collect(Collectors.toList());
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
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
}
