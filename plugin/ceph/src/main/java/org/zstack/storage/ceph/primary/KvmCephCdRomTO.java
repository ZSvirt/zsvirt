package org.zstack.storage.ceph.primary;

import org.zstack.kvm.KVMAgentCommands.CdRomTO;
import java.util.List;

/**
 * Create by lining at 2018/12/29
 */
public class KvmCephCdRomTO extends CdRomTO {
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

    public KvmCephCdRomTO() {
    }

    public KvmCephCdRomTO(CdRomTO other) {
        super(other);
    }

    private List<MonInfo> monInfo;
    private String secretUuid;

    public List<MonInfo> getMonInfo() {
        return monInfo;
    }

    public void setMonInfo(List<MonInfo> monInfo) {
        this.monInfo = monInfo;
    }

    public String getSecretUuid() {
        return secretUuid;
    }

    public void setSecretUuid(String secretUuid) {
        this.secretUuid = secretUuid;
    }
}
