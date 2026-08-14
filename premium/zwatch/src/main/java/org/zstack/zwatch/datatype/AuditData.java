package org.zstack.zwatch.datatype;

import org.zstack.core.DynamicObject;

import java.util.Map;

/**
 * Created by mingjian.deng on 2020/1/9.
 */
public abstract class AuditData implements DynamicObject {
    protected long id;
    protected String resourceUuid;
    protected String resourceType;
    protected String clientIp;
    protected String clientBrowser;
    protected String apiName;
    protected String error;
    protected String operatorAccountUuid;
    protected long duration;
    protected String requestUuid;
    protected String responseUuid;
    protected String sessionUuid;
    protected String requestDump;
    protected String responseDump;
    protected String operator;
    protected long time = System.currentTimeMillis();

    public abstract Map<String, String> asTags();
    public abstract Map<String, Object> asFields();

    public static Object convertValue(String colName, Object value) {
        if (colName.equals("duration")) {
            return ((Number) value).longValue();
        } else {
            return value;
        }
    }

    public String getResourceUuid() {
        return resourceUuid;
    }

    public void setResourceUuid(String resourceUuid) {
        this.resourceUuid = resourceUuid;
    }

    public String getResourceType() {
        return resourceType;
    }

    public void setResourceType(String resourceType) {
        this.resourceType = resourceType;
    }

    public String getClientIp() {
        return clientIp;
    }

    public void setClientIp(String clientIp) {
        this.clientIp = clientIp;
    }

    public String getClientBrowser() {
        return clientBrowser;
    }

    public void setClientBrowser(String clientBrowser) {
        this.clientBrowser = clientBrowser;
    }

    public String getApiName() {
        return apiName;
    }

    public void setApiName(String apiName) {
        this.apiName = apiName;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }

    public String getOperatorAccountUuid() {
        return operatorAccountUuid;
    }

    public void setOperatorAccountUuid(String operatorAccountUuid) {
        this.operatorAccountUuid = operatorAccountUuid;
    }

    public long getDuration() {
        return duration;
    }

    public void setDuration(long duration) {
        this.duration = duration;
    }

    public String getRequestUuid() {
        return requestUuid;
    }

    public void setRequestUuid(String requestUuid) {
        this.requestUuid = requestUuid;
    }

    public String getResponseUuid() {
        return responseUuid;
    }

    public void setResponseUuid(String responseUuid) {
        this.responseUuid = responseUuid;
    }

    public String getSessionUuid() {
        return sessionUuid;
    }

    public void setSessionUuid(String sessionUuid) {
        this.sessionUuid = sessionUuid;
    }

    public String getRequestDump() {
        return requestDump;
    }

    public void setRequestDump(String requestDump) {
        this.requestDump = requestDump;
    }

    public String getResponseDump() {
        return responseDump;
    }

    public void setResponseDump(String responseDump) {
        this.responseDump = responseDump;
    }

    public String getOperator() {
        return operator;
    }

    public void setOperator(String operator) {
        this.operator = operator;
    }

    public long getTime() {
        return time;
    }

    public void setTime(long time) {
        this.time = time;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }
}
