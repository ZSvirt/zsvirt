package org.zstack.core.thread;

import org.zstack.header.core.progress.ChainInfo;
import org.zstack.header.core.progress.SingleFlightChainInfo;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.Future;

public interface DispatchQueue {
    String DUMP_TASK_DEBUG_SINGAL = "DumpTaskQueue";

    <T> Future<T> syncSubmit(SyncTask<T> task);
    
    Future<Void> chainSubmit(ChainTask task);

    <T> Future<T> singleFlightSubmit(SingleFlightTask task);

    Map<String, SyncTaskStatistic> getSyncTaskStatistics();

    Map<String, ChainTaskStatistic> getChainTaskStatistics();

    boolean isChainTaskRunning(String signature);

    ChainInfo getChainTaskInfo(String signature);

    ChainInfo cleanChainTaskInfo(String signature, Integer index, Boolean cleanUp, Boolean isRunningTask);

    SingleFlightChainInfo getSingleFlightChainTaskInfo(String signature);

    Set<String> getApiRunningTaskSignature(String apiId);
}
