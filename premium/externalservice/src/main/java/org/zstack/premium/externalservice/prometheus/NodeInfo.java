package org.zstack.premium.externalservice.prometheus;

import java.sql.Timestamp;

public class NodeInfo {
    private String hostname;
    private Timestamp expiredDate;

    public String getHostname() {
        return hostname;
    }

    public void setHostname(String hostname) {
        this.hostname = hostname;
    }

    public Timestamp getExpiredDate() {
        return expiredDate;
    }

    public void setExpiredDate(Timestamp expiredDate) {
        this.expiredDate = expiredDate;
    }
}
