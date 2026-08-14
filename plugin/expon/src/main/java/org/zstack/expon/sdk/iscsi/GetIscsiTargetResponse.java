package org.zstack.expon.sdk.iscsi;

import org.zstack.expon.sdk.ExponResponse;

/** copy from @class: IscsiModule
 *
 */
public class GetIscsiTargetResponse extends ExponResponse {
    private String id;
    private String name;
    private String description;
    private long createTime;
    private long updateTime;
    private String iqn;
    private int port;
    private String chapPassword;
    private String chapUsername;
    private int clientCount;
    private String clientIds;

    private String gatewayName;
    private String healthStatus;
    private String healthStatusReason;
    private boolean isChap;
    private boolean isEnabled;
    private int lunCount;
    private int nodeCount;
    private int queueDepth;
    private String runStatus;
    private String tianshuClusterName;
    private String tianshuId;
    private String vendor;

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

    public String getIqn() {
        return iqn;
    }

    public void setIqn(String iqn) {
        this.iqn = iqn;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public String getChapPassword() {
        return chapPassword;
    }

    public void setChapPassword(String chapPassword) {
        this.chapPassword = chapPassword;
    }

    public String getChapUsername() {
        return chapUsername;
    }

    public void setChapUsername(String chapUsername) {
        this.chapUsername = chapUsername;
    }

    public int getClientCount() {
        return clientCount;
    }

    public void setClientCount(int clientCount) {
        this.clientCount = clientCount;
    }

    public String getClientIds() {
        return clientIds;
    }

    public void setClientIds(String clientIds) {
        this.clientIds = clientIds;
    }

    public String getGatewayName() {
        return gatewayName;
    }

    public void setGatewayName(String gatewayName) {
        this.gatewayName = gatewayName;
    }

    public String getHealthStatus() {
        return healthStatus;
    }

    public void setHealthStatus(String healthStatus) {
        this.healthStatus = healthStatus;
    }

    public String getHealthStatusReason() {
        return healthStatusReason;
    }

    public void setHealthStatusReason(String healthStatusReason) {
        this.healthStatusReason = healthStatusReason;
    }

    public boolean isChap() {
        return isChap;
    }

    public void setChap(boolean chap) {
        isChap = chap;
    }

    public boolean isEnabled() {
        return isEnabled;
    }

    public void setEnabled(boolean enabled) {
        isEnabled = enabled;
    }

    public int getLunCount() {
        return lunCount;
    }

    public void setLunCount(int lunCount) {
        this.lunCount = lunCount;
    }

    public int getNodeCount() {
        return nodeCount;
    }

    public void setNodeCount(int nodeCount) {
        this.nodeCount = nodeCount;
    }

    public int getQueueDepth() {
        return queueDepth;
    }

    public void setQueueDepth(int queueDepth) {
        this.queueDepth = queueDepth;
    }

    public String getRunStatus() {
        return runStatus;
    }

    public void setRunStatus(String runStatus) {
        this.runStatus = runStatus;
    }

    public String getTianshuClusterName() {
        return tianshuClusterName;
    }

    public void setTianshuClusterName(String tianshuClusterName) {
        this.tianshuClusterName = tianshuClusterName;
    }

    public String getTianshuId() {
        return tianshuId;
    }

    public void setTianshuId(String tianshuId) {
        this.tianshuId = tianshuId;
    }

    public String getVendor() {
        return vendor;
    }

    public void setVendor(String vendor) {
        this.vendor = vendor;
    }
}
