package org.zstack.header.network.l3;

import java.sql.Timestamp;

public class L3NetworkDnsRefInventory {
    private long id;
    private String l3NetworkUuid;
    private String dnsUuid;
    private Timestamp createDate;
    private Timestamp lastOpDate;

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getL3NetworkUuid() {
        return l3NetworkUuid;
    }

    public void setL3NetworkUuid(String l3NetworkUuid) {
        this.l3NetworkUuid = l3NetworkUuid;
    }

    public String getDnsUuid() {
        return dnsUuid;
    }

    public void setDnsUuid(String dnsUuid) {
        this.dnsUuid = dnsUuid;
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
}
