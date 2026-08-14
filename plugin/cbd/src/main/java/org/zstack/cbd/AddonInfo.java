package org.zstack.cbd;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Xingwei Yu
 * @date 2024/4/1 18:12
 */
public class AddonInfo {
    private ClusterInfo clusterInfo;
    private List<MdsInfo> mdsInfos = new ArrayList<>();
    private List<LogicalPoolInfo> logicalPoolInfos = new ArrayList<>();

    public ClusterInfo getClusterInfo() {
        return clusterInfo;
    }

    public void setClusterInfo(ClusterInfo clusterInfo) {
        this.clusterInfo = clusterInfo;
    }

    public List<MdsInfo> getMdsInfos() {
        return mdsInfos;
    }

    public void setMdsInfos(List<MdsInfo> mdsInfos) {
        this.mdsInfos = mdsInfos;
    }

    public List<LogicalPoolInfo> getLogicalPoolInfos() {
        return logicalPoolInfos;
    }

    public void setLogicalPoolInfos(List<LogicalPoolInfo> logicalPoolInfos) {
        this.logicalPoolInfos = logicalPoolInfos;
    }
}
