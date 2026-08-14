package org.zstack.storage.ceph.primary;

import org.zstack.kvm.VolumeTO;

import java.util.List;

/**
 * Created by frank on 7/30/2015.
 */
public class KVMCephVolumeTO extends VolumeTO {
    public static class MonInfo {
        String hostname;
        int port;

        public String getHostname() {
            return hostname;
        }

        public void setHostname(String hostname) {
            this.hostname = hostname;
        }

        public int getPort() {
            return port;
        }

        public void setPort(int port) {
            this.port = port;
        }
    }

    public KVMCephVolumeTO() {
    }

    public KVMCephVolumeTO(VolumeTO other) {
        super(other);
    }

    private List<MonInfo> monInfo;
    private String secretUuid;

    public String getSecretUuid() {
        return secretUuid;
    }

    public void setSecretUuid(String secretUuid) {
        this.secretUuid = secretUuid;
    }

    public List<MonInfo> getMonInfo() {
        return monInfo;
    }

    public void setMonInfo(List<MonInfo> monInfo) {
        this.monInfo = monInfo;
    }
}
