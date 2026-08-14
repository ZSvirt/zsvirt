package org.zstack.zwatch.alarm.activealarm.api;

/**
 * Created by ZStack on 2020/10/19.
 */
public class ActiveAlarmStatus {
    private String namespace;

    private String status;

    public String getNamespace() {
        return namespace;
    }

    public void setNamespace(String namespace) {
        this.namespace = namespace;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
