package org.zstack.expon.sdk.nvmf;

import java.util.List;

/**
 * @example
 * {
 * "audit_result": "consistent",
 * "audit_result_reason": "",
 * "client_node_count": 2,
 * "create_from": "csm",
 * "create_time": 1692330357308,
 * "description": "",
 * "hg_id": 2,
 * "hosts": [
 * "nqn.2021-05.com.domain:796b81929e1",
 * "nqn.2021-05.com.domain:796b81929e4"
 * ],
 * "id": "a484930c-d8f7-4a70-80cc-bec0bf6a90b0",
 * "name": "ttt",
 * "nvmf_gw_count": 1,
 * "run_status": "normal",
 * "snap_num": 0,
 * "tianshu_id": "2f2efcde-87bb-42fc-a558-6cec67432965",
 * "tianshu_name": "ts",
 * "update_time": 1692928700632,
 * "vol_num": 0
 * }
 */

public class NvmfClientGroupModule {
    private String id;
    private String name;
    private String description;
    private List<String> hosts;
    private String runStatus;
    private int volNum;
    private int snapNum;
    private String auditResult;
    private String auditResultReason;
    private int nvmfGwCount;
    private int clientNodeCount;
    private String createFrom;
    private String hgId;
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

    public int getNvmfGwCount() {
        return nvmfGwCount;
    }

    public void setNvmfGwCount(int nvmfGwCount) {
        this.nvmfGwCount = nvmfGwCount;
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

    public String getHgId() {
        return hgId;
    }

    public void setHgId(String hgId) {
        this.hgId = hgId;
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
