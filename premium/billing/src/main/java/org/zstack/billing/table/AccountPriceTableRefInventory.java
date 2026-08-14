package org.zstack.billing.table;

import org.zstack.header.search.Inventory;

import java.sql.Timestamp;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Created by lining on 2019/9/10.
 */
@Inventory(mappingVOClass = AccountPriceTableRefVO.class)
public class AccountPriceTableRefInventory {
    private String accountUuid;
    private String tableUuid;
    private Timestamp createDate;
    private Timestamp lastOpDate;

    public static AccountPriceTableRefInventory valueOf(AccountPriceTableRefVO co) {
        AccountPriceTableRefInventory inv = new AccountPriceTableRefInventory();
        inv.setAccountUuid(co.getAccountUuid());
        inv.setTableUuid(co.getTableUuid());
        inv.setCreateDate(co.getCreateDate());
        inv.setLastOpDate(co.getLastOpDate());
        return inv;
    }

    public static List<AccountPriceTableRefInventory> valueOf(Collection<AccountPriceTableRefVO> cos) {
        return cos.stream().map(AccountPriceTableRefInventory::valueOf).collect(Collectors.toList());
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

    public String getAccountUuid() {
        return accountUuid;
    }

    public void setAccountUuid(String accountUuid) {
        this.accountUuid = accountUuid;
    }

    public String getTableUuid() {
        return tableUuid;
    }

    public void setTableUuid(String tableUuid) {
        this.tableUuid = tableUuid;
    }
}
