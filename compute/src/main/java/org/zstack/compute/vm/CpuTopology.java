package org.zstack.compute.vm;

import org.zstack.header.errorcode.ErrorCode;

import static org.zstack.core.Platform.operr;

public class CpuTopology {
    int cpuNum;
    Integer cpuSockets;
    Integer cpuCores;
    Integer cpuThreads;

    public CpuTopology(int cpuNum, String cpuSockets, String cpuCores, String cpuThreads) {
        this.cpuNum = cpuNum;

        this.cpuSockets = cpuSockets == null ? null : Integer.valueOf(cpuSockets);
        this.cpuCores = cpuCores == null ? null : Integer.valueOf(cpuCores);
        this.cpuThreads = cpuThreads == null ? null : Integer.valueOf(cpuThreads);
    }

    /**
     * Validates topology and updates {@link #cpuSockets}/{@link #cpuCores}/{@link #cpuThreads} when valid.
     *
     * @return null if valid or nothing to validate; otherwise an {@link ErrorCode} describing the failure.
     */
    public ErrorCode calculateValidTopology() {
        if (cpuSockets == null && cpuCores == null && cpuThreads == null) {
            return null;
        }

        int socketNum, coreNum, threadNum;
        if (cpuSockets == null) {
            if (cpuCores != null && cpuThreads != null) {
                socketNum = cpuNum / cpuCores / cpuThreads;
                coreNum = cpuCores;
                threadNum = cpuThreads;
            } else if (cpuCores != null) {
                socketNum = cpuNum / cpuCores;
                coreNum = cpuCores;
                threadNum = 1;
            } else {
                socketNum = cpuNum / cpuThreads;
                coreNum = 1;
                threadNum = cpuThreads;
            }
        } else {
            socketNum = cpuSockets;
            if (cpuThreads == null && cpuCores != null) {
                threadNum = cpuNum / cpuCores / cpuSockets;
                coreNum = cpuCores;
            } else if (cpuThreads == null) {
                coreNum = cpuNum / cpuSockets;
                threadNum = 1;
            } else if (cpuCores == null) {
                coreNum = cpuNum / cpuSockets / cpuThreads;
                threadNum = cpuThreads;
            } else {
                coreNum = cpuCores;
                threadNum = cpuThreads;
            }
        }

        if (cpuNum >= socketNum * coreNum * threadNum) {
            cpuSockets = socketNum;
            cpuCores = coreNum;
            cpuThreads = threadNum;
            return null;
        }

        return operr("cpu topology is not correct, cpuNum[%s], configured cpuSockets[%s], cpuCores[%s], cpuThreads[%s];" +
                        " Calculated cpuSockets[%s], cpuCores[%s], cpuThreads[%s]",
                cpuNum, cpuSockets, cpuCores, cpuThreads, socketNum, coreNum, threadNum);
    }

    public Integer getCpuSockets() {
        return cpuSockets;
    }

    public void setCpuSockets(Integer cpuSockets) {
        this.cpuSockets = cpuSockets;
    }

    public Integer getCpuCores() {
        return cpuCores;
    }

    public void setCpuCores(Integer cpuCores) {
        this.cpuCores = cpuCores;
    }

    public Integer getCpuThreads() {
        return cpuThreads;
    }

    public void setCpuThreads(Integer cpuThreads) {
        this.cpuThreads = cpuThreads;
    }
}
