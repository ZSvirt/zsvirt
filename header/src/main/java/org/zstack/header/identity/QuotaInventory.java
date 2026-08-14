package org.zstack.header.identity;

import org.zstack.header.query.ExpandedQueries;
import org.zstack.header.query.ExpandedQuery;
import org.zstack.header.search.Inventory;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Created by frank on 7/14/2015.
 */
@Inventory(mappingVOClass = QuotaVO.class)
@ExpandedQueries({
        @ExpandedQuery(expandedField = "account", inventoryClass = AccountInventory.class,
                foreignKey = "identityUuid", expandedInventoryKey = "uuid")
})
public class QuotaInventory {
    private String uuid;
    private String name;
    private String identityUuid;
    private String identityType;
    private Long value;
    private Timestamp lastOpDate;
    private Timestamp createDate;

    public static QuotaInventory valueOf(QuotaVO vo) {
        QuotaInventory inv = new QuotaInventory();
        inv.setUuid(vo.getUuid());
        inv.setName(vo.getName());
        inv.setIdentityType(vo.getIdentityType());
        inv.setIdentityUuid(vo.getIdentityUuid());
        inv.setValue(vo.getValue());
        inv.setCreateDate(vo.getCreateDate());
        inv.setLastOpDate(vo.getLastOpDate());
        return inv;
    }

    public static List<QuotaInventory> valueOf(Collection<QuotaVO> vos) {
        List<QuotaInventory> invs = new ArrayList<QuotaInventory>();
        for (QuotaVO vo : vos) {
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

    public String getIdentityUuid() {
        return identityUuid;
    }

    public void setIdentityUuid(String identityUuid) {
        this.identityUuid = identityUuid;
    }

    public String getIdentityType() {
        return identityType;
    }

    public void setIdentityType(String identityType) {
        this.identityType = identityType;
    }

    public long getValue() {
        return value;
    }

    public void setValue(long value) {
        this.value = value;
    }

    public Timestamp getLastOpDate() {
        return lastOpDate;
    }

    public void setLastOpDate(Timestamp lastOpDate) {
        this.lastOpDate = lastOpDate;
    }

    public Timestamp getCreateDate() {
        return createDate;
    }

    public void setCreateDate(Timestamp createDate) {
        this.createDate = createDate;
    }
}
