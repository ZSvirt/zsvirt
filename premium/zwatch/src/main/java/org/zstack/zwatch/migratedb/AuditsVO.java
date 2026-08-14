package org.zstack.zwatch.migratedb;

import javax.persistence.*;

@Entity
@Table
public class AuditsVO {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column
    private long id;

    @Column
    private long createTime;

    @Column
    private String apiName;

    @Column
    private String clientBrowser;

    @Column
    private String clientIp;

    @Column
    private long duration;

    @Column
    private String error;

    @Column
    private String operator;

    @Column
    private String requestDump;

    @Column
    private String resourceType;

    @Column
    private String resourceUuid;

    @Column
    private String resourceName;

    @Column
    private String responseDump;

    @Column
    private Boolean success;

    @Column
    private String requestUuid;

    @Column
    private String operatorAccountUuid;

    @Column
    private String signedText;

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public long getCreateTime() {
        return createTime;
    }

    public void setCreateTime(long createTime) {
        this.createTime=createTime;
    }

    public String getApiName() {
        return apiName;
    }

    public void setApiName(String apiName) {
        this.apiName = apiName;
    }

    public String getClientBrowser() {
        return clientBrowser;
    }

    public void setClientBrowser(String clientBrowser) {
        this.clientBrowser = clientBrowser;
    }

    public String getClientIp() {
        return clientIp;
    }

    public void setClientIp(String clientIp) {
        this.clientIp = clientIp;
    }

    public long getDuration() {
        return duration;
    }

    public void setDuration(long duration) {
        this.duration = duration;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }

    public String getOperator() {
        return operator;
    }

    public void setOperator(String operator) {
        this.operator = operator;
    }

    public String getRequestDump() {
        return requestDump;
    }

    public void setRequestDump(String requestDump) {
        this.requestDump = requestDump;
    }

    public String getResourceType() {
        return resourceType;
    }

    public void setResourceType(String resourceType) {
        this.resourceType = resourceType;
    }

    public String getResponseDump() {
        return responseDump;
    }

    public void setResponseDump(String responseDump) {
        this.responseDump = responseDump;
    }

    public String getResourceUuid() {
        return resourceUuid;
    }

    public void setResourceUuid(String resourceUuid) {
        this.resourceUuid = resourceUuid;
    }

    public String getResourceName() {
        return resourceName;
    }

    public void setResourceName(String resourceName) {
        this.resourceName = resourceName;
    }

    public Boolean getSuccess() {
        return success;
    }

    public void setSuccess(Boolean success) {
        this.success = success;
    }

    public String getRequestUuid() {
        return requestUuid;
    }

    public void setRequestUuid(String requestUuid) {
        this.requestUuid = requestUuid;
    }

    public String getOperatorAccountUuid() {
        return operatorAccountUuid;
    }

    public void setOperatorAccountUuid(String operatorAccountUuid) {
        this.operatorAccountUuid = operatorAccountUuid;
    }

    public String getSignedText() {
        return signedText;
    }

    public void setSignedText(String signedText) {
        this.signedText = signedText;
    }
}
