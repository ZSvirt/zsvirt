package org.zstack.compute.vm;

import java.util.List;

public interface VmDnsBackend {
    String getVmInstanceType();
    void setNicDns(String vmUuid, String vmNicUuid, List<String> dnsList, Integer ipVersion);
    void setVmDns(String vmUuid, List<String> dnsList);
}
