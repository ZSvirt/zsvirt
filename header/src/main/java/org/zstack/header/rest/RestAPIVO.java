package org.zstack.header.rest;

import javax.persistence.*;
import java.sql.Timestamp;

@Entity
@Table
public class RestAPIVO {
    @Id
    @Column
    private String uuid;

    @Column
    private String apiMessageName;

    @Column
    @Enumerated(EnumType.STRING)
    private RestAPIState state;

    @Column
    private String result;

    @Column
    private Timestamp createDate;

    @Column
    private Timestamp lastOpDate;

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getApiMessageName() {
        return apiMessageName;
    }

    public void setApiMessageName(String apiMessageName) {
        this.apiMessageName = apiMessageName;
    }

    public String getResult() {
        return result;
    }

    public void setResult(String result) {
        this.result = result;
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

    public RestAPIState getState() {
        return state;
    }

    public void setState(RestAPIState state) {
        this.state = state;
    }
}
