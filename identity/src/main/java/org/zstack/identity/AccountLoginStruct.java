package org.zstack.identity;

import java.sql.Timestamp;

public class AccountLoginStruct {
    private String accountUuid;
    private Timestamp lastOpTime;

    public String getAccountUuid() {
        return accountUuid;
    }

    public void setAccountUuid(String accountUuid) {
        this.accountUuid = accountUuid;
    }

    public Timestamp getLastOpTime() {
        return lastOpTime;
    }

    public void setLastOpTime(Timestamp lastOpTime) {
        this.lastOpTime = lastOpTime;
    }
}
