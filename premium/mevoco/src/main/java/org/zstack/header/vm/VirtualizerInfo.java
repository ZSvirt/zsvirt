package org.zstack.header.vm;

import org.zstack.kvm.hypervisor.datatype.HypervisorVersionState;
import org.zstack.kvm.hypervisor.datatype.KvmHypervisorInfoVO;

/**
 * Created by Wenhao.Zhang on 23/02/21
 */
public class VirtualizerInfo {
    /**
     * Maybe "qemu-kvm"
     * equals to {@link KvmHypervisorInfoVO#getHypervisor()}
     */
    private String hypervisor;
    /**
     * for KVM based VM (version of virtualizer device for VM current used)
     * or KVM Host (version of virtualizer device for host installed now)
     */
    private String currentVersion;
    /**
     * for KVM based VM, 'expectVersion' is version of its host installed now;
     * for KVM Host, 'expectVersion' is version of management node expected;
     */
    private String expectVersion;
    private HypervisorVersionState matchState;

    public String getHypervisor() {
        return hypervisor;
    }

    public void setHypervisor(String hypervisor) {
        this.hypervisor = hypervisor;
    }

    public String getCurrentVersion() {
        return currentVersion;
    }

    public void setCurrentVersion(String currentVersion) {
        this.currentVersion = currentVersion;
    }

    public String getExpectVersion() {
        return expectVersion;
    }

    public void setExpectVersion(String expectVersion) {
        this.expectVersion = expectVersion;
    }

    public HypervisorVersionState getMatchState() {
        return matchState;
    }

    public void setMatchState(HypervisorVersionState matchState) {
        this.matchState = matchState;
    }
}
