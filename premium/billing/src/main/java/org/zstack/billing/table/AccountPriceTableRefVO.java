package org.zstack.billing.table;

import javax.persistence.*;
import java.sql.Timestamp;

/**
 * Created by lining on 2019/9/10.
 */
@Entity
@Table
@IdClass(CompositePrimaryKeyForAccountPriceTableRefVO.class)
public class AccountPriceTableRefVO {
    @Id
    @Column
    private String tableUuid;

    @Id
    @Column
    private String accountUuid;

    @Column
    private Timestamp createDate;

    @Column
    private Timestamp lastOpDate;

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

    public String getTableUuid() {
        return tableUuid;
    }

    public void setTableUuid(String tableUuid) {
        this.tableUuid = tableUuid;
    }

    public String getAccountUuid() {
        return accountUuid;
    }

    public void setAccountUuid(String accountUuid) {
        this.accountUuid = accountUuid;
    }
}
