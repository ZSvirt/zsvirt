package org.zstack.header.vm;

/**
 * @ Author : yh.w
 * @ Date   : Created in 20:02 2019/9/23
 */
public class PriorityConfigStruct {

    public PriorityConfigStruct(VmPriorityConfigVO vo, String vmUuid) {
        this.vmUuid = vmUuid;
        this.cpuShares = vo.getCpuShares();
        this.oomScoreAdj = vo.getOomScoreAdj();
    }

    private String vmUuid;

    private int cpuShares;

    private int oomScoreAdj;

    public String getVmUuid() {
        return vmUuid;
    }

    public void setVmUuid(String vmUuid) {
        this.vmUuid = vmUuid;
    }

    public int getCpuShares() {
        return cpuShares;
    }

    public void setCpuShares(int cpuShares) {
        this.cpuShares = cpuShares;
    }

    public int getOomScoreAdj() {
        return oomScoreAdj;
    }

    public void setOomScoreAdj(int oomScoreAdj) {
        this.oomScoreAdj = oomScoreAdj;
    }
}
