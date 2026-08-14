package org.zstack.expon.sdk.iscsi;

import org.zstack.expon.sdk.ExponResponse;

import java.util.List;


/**
 * @example {
 * "audit_result": "consistent",
 * "audit_result_reason": "",
 * "chap_password": "",
 * "chap_username": "",
 * "client_name": "iscsi_zstack_heartbeat",
 * "client_node_count": 3,
 * "create_from": "csm",
 * "create_time": 1705314720041,
 * "description": "",
 * "hosts": ["iqn.1994-05.com.redhat:77cacce6c29", "iqn.1994-05.com.redhat:c24c919fc248", "iqn.1994-05.com.redhat:278979fd2069"],
 * "id": "413f59f4-90ae-4ee7-9388-07065d25bd3e",
 * "ig_id": 1,
 * "iqn_prefix": null,
 * "is_chap": false,
 * "iscsi_gw_count": 2,
 * "lun_count": 1,
 * "message": "",
 * "name": "iscsi_zstack_heartbeat",
 * "ret_code": "0",
 * "run_status": "normal",
 * "snap_num": 0,
 * "tianshu_id": "5ba1db5c-7672-4a2b-bceb-015aa3234275",
 * "tianshu_name": "VUZhh",
 * "update_time": 1708411159610,
 * "vol_num": 1
 * }
 */
public class GetIscsiClientGroupResponse extends ExponResponse {
    private String id;
    private String name;
    private String description;
    private List<String> hosts;
    private String runStatus;
    private int volNum;
    private int snapNum;
    private String auditResult;
    private String auditResultReason;
    private int iscsiGwCount;
    private int clientNodeCount;
    private String createFrom;
    private String tianshuId;
    private String tianshuName;
    private long createTime;
    private long updateTime;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public List<String> getHosts() {
        return hosts;
    }

    public void setHosts(List<String> hosts) {
        this.hosts = hosts;
    }

    public String getRunStatus() {
        return runStatus;
    }

    public void setRunStatus(String runStatus) {
        this.runStatus = runStatus;
    }

    public int getVolNum() {
        return volNum;
    }

    public void setVolNum(int volNum) {
        this.volNum = volNum;
    }

    public int getSnapNum() {
        return snapNum;
    }

    public void setSnapNum(int snapNum) {
        this.snapNum = snapNum;
    }

    public String getAuditResult() {
        return auditResult;
    }

    public void setAuditResult(String auditResult) {
        this.auditResult = auditResult;
    }

    public String getAuditResultReason() {
        return auditResultReason;
    }

    public void setAuditResultReason(String auditResultReason) {
        this.auditResultReason = auditResultReason;
    }

    public int getiscsiGwCount() {
        return iscsiGwCount;
    }

    public void setiscsiGwCount(int iscsiGwCount) {
        this.iscsiGwCount = iscsiGwCount;
    }

    public int getClientNodeCount() {
        return clientNodeCount;
    }

    public void setClientNodeCount(int clientNodeCount) {
        this.clientNodeCount = clientNodeCount;
    }

    public String getCreateFrom() {
        return createFrom;
    }

    public void setCreateFrom(String createFrom) {
        this.createFrom = createFrom;
    }

    public String getTianshuId() {
        return tianshuId;
    }

    public void setTianshuId(String tianshuId) {
        this.tianshuId = tianshuId;
    }

    public String getTianshuName() {
        return tianshuName;
    }

    public void setTianshuName(String tianshuName) {
        this.tianshuName = tianshuName;
    }

    public long getCreateTime() {
        return createTime;
    }

    public void setCreateTime(long createTime) {
        this.createTime = createTime;
    }

    public long getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(long updateTime) {
        this.updateTime = updateTime;
    }
}
