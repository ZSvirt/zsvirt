package org.zstack.billing;

import javax.persistence.Column;
import javax.persistence.MappedSuperclass;

/**
 * Created by xing5 on 2016/9/15.
 */
@MappedSuperclass
public class UsageAO {
    @Column
    protected String accountUuid;
    @Column
    protected long dateInLong;

    public String getAccountUuid() {
        return accountUuid;
    }

    public void setAccountUuid(String accountUuid) {
        this.accountUuid = accountUuid;
    }

    public long getDateInLong() {
        return dateInLong;
    }

    public void setDateInLong(long dateInLong) {
        this.dateInLong = dateInLong;
    }
}
