package org.zstack.drs.algorithm;

import org.zstack.drs.data.HostNode;
import org.zstack.drs.data.MigrateTask;

import java.util.List;

/**
 * Created by lining on 2019/12/16.
 */
public interface BalanceAlgorithm {
    String getAlgorithmName();

    List<MigrateTask> makeTasks(List<HostNode> hostNodeList, Float cpuUsedPercentThreshold, Float memUsedPercentThreshold);
}