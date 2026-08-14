package org.zstack.header.identity;

import org.zstack.header.configuration.PythonClassInventory;
import org.zstack.header.rest.APINoSee;

import java.io.Serializable;
import java.sql.Timestamp;

@PythonClassInventory
public class SessionInventory implements Serializable {
    private String uuid;
    private String accountUuid;
    private Timestamp expiredDate;
    private Timestamp createDate;
    @APINoSee
    private boolean noSessionEvaluation;

    public static SessionInventory valueOf(SessionVO vo) {
        SessionInventory inv = new SessionInventory();
        inv.setAccountUuid(vo.getAccountUuid());
        inv.setCreateDate(vo.getCreateDate());
        inv.setExpiredDate(vo.getExpiredDate());
        inv.setUuid(vo.getUuid());
        return inv;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getAccountUuid() {
        return accountUuid;
    }

    public void setAccountUuid(String accountUuid) {
        this.accountUuid = accountUuid;
    }

    public Timestamp getExpiredDate() {
        return expiredDate;
    }

    public void setExpiredDate(Timestamp expiredDate) {
        this.expiredDate = expiredDate;
    }

    public Timestamp getCreateDate() {
        return createDate;
    }

    public void setCreateDate(Timestamp createDate) {
        this.createDate = createDate;
    }

    public boolean isNoSessionEvaluation() {
        return noSessionEvaluation;
    }

    public void setNoSessionEvaluation(boolean noSessionEvaluation) {
        this.noSessionEvaluation = noSessionEvaluation;
    }
}
