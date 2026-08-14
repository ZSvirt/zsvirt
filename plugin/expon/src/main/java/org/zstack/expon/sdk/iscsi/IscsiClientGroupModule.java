package org.zstack.expon.sdk.iscsi;

import java.util.List;

/**
 * @example
 * {
 * 	"audit_result": "consistent",
 * 	"audit_result_reason": "",
 * 	"chap_password": "",
 * 	"chap_username": "",
 * 	"client_name": "iscsi_172_25_16_110",
 * 	"client_node_count": 1,
 * 	"create_from": "csm",
 * 	"create_time": 1700498026144,
 * 	"description": "",
 * 	"hosts": ["iqn.1994-05.com.redhat:b31a3f136997"],
 * 	"id": "90b745ad-7e2a-410d-9146-1ca15dd095b6",
 * 	"ig_id": 7,
 * 	"iqn_prefix": null,
 * 	"is_chap": false,
 * 	"iscsi_gw_count": 1,
 * 	"lun_count": 0,
 * 	"name": "iscsi_172_25_16_110",
 * 	"run_status": "normal",
 * 	"snap_num": 0,
 * 	"tianshu_id": "5e59b7ef-e599-4847-8ab5-f1eb727f2e90",
 * 	"tianshu_name": "qRJbA",
 * 	"update_time": 1700498031024,
 * 	"vol_num": 0
 * }
 */

public class  IscsiClientGroupModule {
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
