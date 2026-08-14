package org.zstack.header.identity;

import org.zstack.header.configuration.PythonClassInventory;
import org.zstack.header.message.DocUtils;
import org.zstack.header.query.ExpandedQueries;
import org.zstack.header.query.ExpandedQuery;
import org.zstack.header.search.Inventory;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@Inventory(mappingVOClass = AccountVO.class)
@ExpandedQueries({
        @ExpandedQuery(expandedField = "quota", inventoryClass = QuotaInventory.class,
                foreignKey = "uuid", expandedInventoryKey = "identityUuid")
})
@PythonClassInventory
public class AccountInventory {
    private String uuid;
    private String name;
    private String description;
    private String type;
    private String source;
    private String state;
    private Timestamp createDate;
    private Timestamp lastOpDate;

    public static AccountInventory valueOf(AccountVO vo) {
        AccountInventory inv = new AccountInventory();
        inv.setUuid(vo.getUuid());
        inv.setName(vo.getName());
        inv.setDescription(vo.getDescription());
        inv.setType(vo.getType().toString());
        inv.setSource(vo.getSource().toString());
        inv.setState(vo.getState().toString());
        inv.setCreateDate(vo.getCreateDate());
        inv.setLastOpDate(vo.getLastOpDate());
        return inv;
    }

    public static List<AccountInventory> valueOf(Collection<AccountVO> vos) {
        List<AccountInventory> lst = new ArrayList<AccountInventory>(vos.size());
        for (AccountVO vo : vos) {
            lst.add(AccountInventory.valueOf(vo));
        }
        return lst;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
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

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
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

    public static AccountInventory __example__() {
        AccountInventory account = new AccountInventory();
        account.setUuid(DocUtils.createFixedUuid(AccountVO.class));
        account.setName("account1");
        account.setDescription("account1-description");
        account.setType(AccountType.Normal.toString());
        account.setSource(AccountSource.Local.toString());
        account.setState(AccountState.Enabled.toString());
        account.setCreateDate(new Timestamp(DocUtils.date));
        account.setLastOpDate(new Timestamp(DocUtils.date));
        return account;
    }
}
