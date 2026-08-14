package org.zstack.sdk.sns;



public class SNSSubscriberInventory  {

    public java.lang.String topicUuid;
    public void setTopicUuid(java.lang.String topicUuid) {
        this.topicUuid = topicUuid;
    }
    public java.lang.String getTopicUuid() {
        return this.topicUuid;
    }

    public java.lang.String endpointUuid;
    public void setEndpointUuid(java.lang.String endpointUuid) {
        this.endpointUuid = endpointUuid;
    }
    public java.lang.String getEndpointUuid() {
        return this.endpointUuid;
    }

    public java.sql.Timestamp createDate;
    public void setCreateDate(java.sql.Timestamp createDate) {
        this.createDate = createDate;
    }
    public java.sql.Timestamp getCreateDate() {
        return this.createDate;
    }

    public java.sql.Timestamp lastOpDate;
    public void setLastOpDate(java.sql.Timestamp lastOpDate) {
        this.lastOpDate = lastOpDate;
    }
    public java.sql.Timestamp getLastOpDate() {
        return this.lastOpDate;
    }

}
