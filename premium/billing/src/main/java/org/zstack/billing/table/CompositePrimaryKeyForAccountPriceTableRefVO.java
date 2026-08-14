package org.zstack.billing.table;

import java.io.Serializable;

/**
 * Created by lining on 2019/9/10.
 */
public class CompositePrimaryKeyForAccountPriceTableRefVO implements Serializable {
    private String accountUuid;
    private String tableUuid;

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

    @Override
    public int hashCode() {
        return (accountUuid + tableUuid).hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }

        CompositePrimaryKeyForAccountPriceTableRefVO other = (CompositePrimaryKeyForAccountPriceTableRefVO) obj;
        if (accountUuid == null) {
            if (other.accountUuid != null) {
                return false;
            }
        } else if (!accountUuid.equals(other.accountUuid)) {
            return false;
        }
        if (tableUuid == null) {
            if (other.tableUuid != null) {
                return false;
            }
        } else if (!tableUuid.equals(other.tableUuid)) {
            return false;
        }

        return true;
    }
}
