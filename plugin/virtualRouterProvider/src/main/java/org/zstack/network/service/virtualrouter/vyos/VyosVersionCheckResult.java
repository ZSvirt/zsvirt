package org.zstack.network.service.virtualrouter.vyos;

public class VyosVersionCheckResult {
    public boolean needReconnect = false;
    public boolean rebuildSnat = false;
    public String version;

    public boolean isNeedReconnect() {
        return needReconnect;
    }

    public void setNeedReconnect(boolean needReconnect) {
        this.needReconnect = needReconnect;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public boolean isRebuildSnat() {
        return rebuildSnat;
    }

    public void setRebuildSnat(boolean rebuildSnat) {
        this.rebuildSnat = rebuildSnat;
    }
}
