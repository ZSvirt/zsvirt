package org.zstack.core.thread;

import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.core.CoreGlobalProperty;
import org.zstack.core.debug.DebugManager;
import org.zstack.core.debug.DebugSignalHandler;
import org.zstack.header.Constants;
import org.zstack.header.core.ExceptionSafe;
import org.zstack.header.core.ReturnValueCompletion;
import org.zstack.header.core.progress.ChainInfo;
import org.zstack.header.core.progress.PendingTaskInfo;
import org.zstack.header.core.progress.SingleFlightChainInfo;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.header.errorcode.SysErrors;
import org.zstack.utils.DebugUtils;
import org.zstack.utils.TaskContext;
import org.zstack.utils.Utils;
import org.zstack.utils.gson.JSONObjectUtil;
import org.zstack.utils.logging.CLogger;
import org.zstack.utils.logging.CLoggerImpl;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.zstack.core.Platform.*;

@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE, dependencyCheck = true)
class DispatchQueueImpl implements DispatchQueue, DebugSignalHandler {
    private static final CLogger logger = Utils.getLogger(DispatchQueueImpl.class);

    @Autowired
    ThreadFacade _threadFacade;
    @Autowired
    private org.zstack.core.timeout.Timer zTimer;

    private final HashMap<String, SyncTaskQueueWrapper> syncTasks = new HashMap<>();
    private final Map<String, ChainTaskQueueWrapper> chainTasks = Collections.synchronizedMap(new HashMap<>());
    private final Map<String, SingleFlightQueueWrapper> singleFlightTasks = Collections.synchronizedMap(new HashMap<>());
    private final Map<String, List<String>> apiRunningSignature = new ConcurrentHashMap<>();
    private static final CLogger _logger = CLoggerImpl.getLogger(DispatchQueueImpl.class);

    private String dumpChainTaskQueue() {
        List<String> asyncTasks = new ArrayList<>();
        synchronized (chainTasks) {
            for (Map.Entry<String, ChainTaskQueueWrapper> e : chainTasks.entrySet()) {
                asyncTasks.add(e.getValue().dumpTaskQueueInfo());
            }
        }

        StringBuilder sb = new StringBuilder();
        sb.append("\nASYNC TASK QUEUE DUMP:");
        sb.append(String.format("\nTASK QUEUE NUMBER: %s\n", chainTasks.size()));
        sb.append(StringUtils.join(asyncTasks, "\n"));
        return sb.toString();
    }

    private String dumpSyncTaskQueue() {
        List<String> queueSyncTasks = new ArrayList<>();
        synchronized (syncTasks) {
            for (Map.Entry<String, SyncTaskQueueWrapper> e : syncTasks.entrySet()) {
                queueSyncTasks.add(e.getValue().dumpTaskQueueInfo());
            }
        }

        StringBuilder sb = new StringBuilder();
        sb.append("\nSYNC TASK QUEUE DUMP:");
        sb.append(String.format("\nTASK QUEUE NUMBER: %s\n", syncTasks.size()));
        sb.append(StringUtils.join(queueSyncTasks, "\n"));
        return sb.toString();
    }

    private String dumpSingleFlightTaskQueue() {
        List<String> queueSingleFlightTasks = new ArrayList<>();
        synchronized (singleFlightTasks) {
            for (Map.Entry<String, SingleFlightQueueWrapper> e : singleFlightTasks.entrySet()) {
                queueSingleFlightTasks.add(e.getValue().dumpTaskQueueInfo());
            }
        }

        StringBuilder sb = new StringBuilder();
        sb.append("\nSINGLE FLIGHT TASK QUEUE DUMP:");
        sb.append(String.format("\nTASK QUEUE NUMBER: %s\n", syncTasks.size()));
        sb.append(StringUtils.join(queueSingleFlightTasks, "\n"));
        return sb.toString();
    }

    @Override
    public void handleDebugSignal() {
        StringBuilder sb = new StringBuilder();
        sb.append("\n================= BEGIN TASK QUEUE DUMP ================");
        sb.append(dumpChainTaskQueue());
        sb.append(dumpSyncTaskQueue());
        sb.append(dumpSingleFlightTaskQueue());
        sb.append("\n================= END TASK QUEUE DUMP ==================\n");
        _threadFacade.printThreadsAndTasks();
        logger.debug(sb.toString());
    }

    public void beforeCleanQueuedumpThread(String signatureName) {
        String title = "\n================= Before Clean Task Queue Dump ================";
        dumpSignatureNameThread(signatureName,title);
    }

    public void afterCleanQueuedumpThread(String signatureName) {
        String title = "\n================= After Clean Task Queue Dump ================";
        dumpSignatureNameThread(signatureName,title);
    }

    public void dumpSignatureNameThread(String signatureName, String title) {
        StringBuilder sb = new StringBuilder();
        sb.append(title);
        sb.append("\nASYNC TASK QUEUE DUMP:");
        sb.append(String.format("\nTASK QUEUE NUMBER: %s\n", chainTasks.size()));
        List<String> asyncTasks = new ArrayList<>();

        ChainTaskQueueWrapper w = chainTasks.get(signatureName);
        if (w == null) {
            sb.append(String.format("\n===== NO QUEUE SYNC SIGNATURE: %s =====", signatureName));
            sb.append(StringUtils.join(asyncTasks, "\n"));
            sb.append("\n================= END TASK QUEUE DUMP ==================\n");
            _threadFacade.printThreadsAndTasks();
            logger.debug(sb.toString());
            return;
        }
        StringBuilder tb = new StringBuilder(String.format("\nQUEUE SYNC SIGNATURE: %s", signatureName));
        tb.append(w.getTaskQueueInfo());
        asyncTasks.add(tb.toString());

        sb.append(StringUtils.join(asyncTasks, "\n"));
        sb.append("\n================= END TASK QUEUE DUMP ==================\n");
        _threadFacade.printThreadsAndTasks();
        logger.debug(sb.toString());
    }

    @Override
    public ChainInfo getChainTaskInfo(String signature) {
        ChainTaskQueueWrapper w = chainTasks.get(signature);
        if (w == null) {
            return new ChainInfo();
        }
        return w.getChainInfo();
    }

    @Override
    public ChainInfo cleanChainTaskInfo(String signature, Integer index, Boolean cleanUp, Boolean isRunningTask) {
        beforeCleanQueuedumpThread(signature);
        long now = System.currentTimeMillis();
        synchronized (chainTasks) {
            ChainInfo info = new ChainInfo();
            ChainTaskQueueWrapper w = chainTasks.get(signature);
            if (w == null) {
                logger.warn(String.format("no queue with a corresponding signatureName[%s]", signature));
                return null;
            }

            ChainInfo Tmp = getChainTaskInfo(signature);
            if (cleanUp) {
                chainTasks.remove(signature);
                afterCleanQueuedumpThread(signature);
                return Tmp;
            }
            if (index == null) {
                if (isRunningTask) {
                    info.setRunningTask(Tmp.getRunningTask());
                    w.runningQueue.clear();
                    afterCleanQueuedumpThread(signature);
                    return info;
                }
                info.setPendingTask(Tmp.getPendingTask());
                w.pendingQueue.clear();
                afterCleanQueuedumpThread(signature);
                return info;
            }

            if (isRunningTask) {
                ChainFuture cf = (ChainFuture) w.runningQueue.get(index);
                info.addRunningTask(TaskInfoBuilder.buildRunningTaskInfo(cf, now, index));
                w.runningQueue.remove(index.intValue());
            } else {
                ChainFuture cf = (ChainFuture) w.pendingQueue.get(index);
                info.addPendingTask(TaskInfoBuilder.buildPendingTaskInfo(cf, now, index));
                w.pendingQueue.remove(index.intValue());
            }

            if (w.runningQueue.isEmpty() && w.pendingQueue.isEmpty()) {
                chainTasks.remove(signature);
            }
            afterCleanQueuedumpThread(signature);       
            return info;
        }
    }

    @Override
    public SingleFlightChainInfo getSingleFlightChainTaskInfo(String signature) {
        long now = System.currentTimeMillis();
        synchronized (singleFlightTasks) {
            SingleFlightChainInfo info = new SingleFlightChainInfo();

            SingleFlightQueueWrapper w = singleFlightTasks.get(signature);
            if (w == null) {
                logger.warn(String.format("no queue with a corresponding signatureName[%s]", signature));
                return null;
            }

            int index = 0;
            if (w.runningTask != null) {
                SingleFlightFuture sf = w.runningTask;
                info.addRunningTask(TaskInfoBuilder.buildRunningTaskInfo(sf, now, index++));
            }

            for (Object obj : w.pendingQueue) {
                SingleFlightFuture sf = (SingleFlightFuture) obj;
                info.addPendingTask(TaskInfoBuilder.buildPendingTaskInfo(sf, now, index++));
            }
            return info;
        }
    }

    @Override
    public Set<String> getApiRunningTaskSignature(String apiId) {
        return new HashSet<>(apiRunningSignature.getOrDefault(apiId, Collections.emptyList()));
    }

    public DispatchQueueImpl() {
        DebugManager.registerDebugSignalHandler(DUMP_TASK_DEBUG_SINGAL, this);
    }

    private class SyncTaskFuture<T> extends AbstractFuture<T> {
        public SyncTaskFuture(SyncTask<T> task) {
            super(task);
        }

        private SyncTask getTask() {
            return (SyncTask) task;
        }

        @Override
        public boolean cancel(boolean mayInterruptIfRunning) {
            cancel();
            return true;
        }

        void run() {
            if (isCancelled()) {
                return;
            }

            try {
                ret = (T) getTask().call();
            } catch (Throwable t) {
                _logger.warn(String.format("unhandled exception happened when calling sync task[name:%s, class:%s]",
                        getTask().getName(), getTask().getClass().getName()), t);
                exception = t;
            }

            done();
        }

        String getSyncSignature() {
            return getTask().getSyncSignature();
        }

        int getSyncLevel() {
            return getTask().getSyncLevel();
        }

        String getName() {
            return getTask().getName();
        }
    }

    private abstract class AbstractTaskQueueWrapper {
        String syncSignature;
        private final AtomicInteger abnormalPendingQueueThreshold = new AtomicInteger(CoreGlobalProperty.PENDING_QUEUE_MINIMUM_THRESHOLD);

        /**
         * make getCurrentPendingQueueThreshold() public for test
         * @return int value of abnormalPendingQueueThreshold
         */
        public int getCurrentPendingQueueThreshold() {
            return abnormalPendingQueueThreshold.get();
        }

        /**
         * One task use 56 bytes memory, test on version 4.5.0.
         * Default 50 tasks use 2.73MB memory.
         * Next level 250 tasks use 13.67MB memory.
         * No big change for the memory usage and just use this mechanism to detect
         * system level slow executed task queue.
         * @return current pending queue threshold
         */
        private int extendPendingQueueThresholdForNextDetection() {
            return abnormalPendingQueueThreshold.getAndUpdate(operand -> operand * 5);
        }

        /**
         * When confirm the task queue runs as expected, use this to reset threshold.
         * Now, reset before new task started (means no pending task anymore)
         *
         * Note: Reset pending queue threshold should be manually invoked
         */
        public void resetPendingQueueThreshold() {
            abnormalPendingQueueThreshold.set(CoreGlobalProperty.PENDING_QUEUE_MINIMUM_THRESHOLD);
        }

        /**
         * Dump task queue if needed
         *
         * Pending task size is defined by wrapper itself, use the size compares to
         * threshold and if pending size is over threshold, means too many pending
         * task for current task queue, dump the whole queue for debug.
         *
         * In order to avoid frequent queue dumping, extend the threshold to 5 times
         * for next abnormal detection.
         *
         * When the task queue recovered, use resetPendingQueueThreshold() to reset
         * the threshold
         *
         * @param currentPendingTaskQueueSize the pending task queue size offered by
         *                                    wrapper
         */
        public void dumpTaskQueueIfNeeded(int currentPendingTaskQueueSize) {
            if (currentPendingTaskQueueSize <= getCurrentPendingQueueThreshold()) {
                return;
            }

            // change threshold for next abnormal report
            if (currentPendingTaskQueueSize > getCurrentPendingQueueThreshold()) {
                logger.debug(String.format("syncSignature: %s, pending queue size over abnormal limitation: %d, " +
                                " too many pending tasks, dump task queue for potential problem",
                        syncSignature, extendPendingQueueThresholdForNextDetection()));
                logger.debug("\n================= BEGIN ABNORMAL TASK QUEUE DUMP ================");
                logger.debug(dumpTaskQueueInfo());
                logger.debug("\n================= END ABNORMAL TASK QUEUE DUMP ================");
            }
        }

        /**
         * dump current task queue info
         * @return String with queue description string
         */
        protected abstract String getTaskQueueInfo();

        public String dumpTaskQueueInfo() {
            StringBuilder tb = new StringBuilder(String.format("\nQUEUE SYNC SIGNATURE: %s", syncSignature));

            synchronized (chainTasks) {
                tb.append(getTaskQueueInfo());
            }

            return tb.toString();
        }
    }

    private class SyncTaskQueueWrapper extends AbstractTaskQueueWrapper {
        ConcurrentLinkedQueue queue = new ConcurrentLinkedQueue();
        AtomicInteger counter = new AtomicInteger(0);
        int maxThreadNum = -1;

        void addTask(SyncTaskFuture task) {
            queue.offer(task);
            if (maxThreadNum == -1) {
                maxThreadNum = task.getSyncLevel();
            }
            if (syncSignature == null) {
                syncSignature = task.getSyncSignature();
            }
        }

        void startThreadIfNeeded() {
            if (counter.get() >= maxThreadNum) {
                logger.debug(String.format("Sync task syncSignature: %s reached maxThreadNum: %s, current: %d, pending queue size: %d",
                        syncSignature, maxThreadNum, counter.get(), queue.size()));
                dumpTaskQueueIfNeeded(queue.size());
                return;
            }

            resetPendingQueueThreshold();
            counter.incrementAndGet();
            _threadFacade.submitTargetPool(new Task<Void>() {
                @Override
                public String getName() {
                    return syncSignature;
                }

                void run() {
                    SyncTaskFuture stask;
                    while (true) {
                        while ((stask = (SyncTaskFuture) queue.poll()) != null) {
                            stask.run();
                        }

                        synchronized (syncTasks) {
                            if (queue.isEmpty()) {
                                if (counter.decrementAndGet() == 0) {
                                    syncTasks.remove(syncSignature);
                                }

                                break;
                            }
                        }
                    }

                }

                @Override
                public Void call() {
                    run();
                    return null;
                }
            }, syncSignature);
        }

        @Override
        protected String getTaskQueueInfo() {
            StringBuilder tb = new StringBuilder();
            tb.append(String.format("\nRUNNING SYNC TASK NUMBER: %s", counter));
            tb.append(String.format("\nPENDING TASK NUMBER: %s", queue.size()));
            tb.append(String.format("\nSYNC LEVEL: %s", maxThreadNum));
            tb.append(String.format("\nPENDING TASK[NAME: %s, TASK QUEUE SIZE: %d, MAX THREAD: %d] ",
                    syncSignature, queue.size(), maxThreadNum));
            for (Object obj : queue) {
                SyncTask task = ((SyncTaskFuture) obj).getTask();

                if (task.getThreadContext() == null) {
                    break;
                }

                String taskId = null;
                if (task.getThreadContext().containsKey(Constants.THREAD_CONTEXT_API)) {
                    taskId = task.getThreadContext().get(Constants.THREAD_CONTEXT_API);
                }

                if (task.getThreadContext().containsKey(Constants.THREAD_CONTEXT_TASK)) {
                    taskId = task.getThreadContext().get(Constants.THREAD_CONTEXT_TASK);
                }

                if (taskId == null) {
                    break;
                }

                tb.append(String.format("\nPENDING TASK[NAME: %s, TASK ID: %s] ",
                        task.getName(), taskId));
            }


            return tb.toString();
        }
    }

    private <T> Future<T> doSyncSubmit(final SyncTask<T> syncTask) {
        assert syncTask.getSyncSignature() != null : "How can you submit a sync task without sync signature ???";

        SyncTaskFuture f;
        synchronized (syncTasks) {
            SyncTaskQueueWrapper wrapper = syncTasks.get(syncTask.getSyncSignature());
            if (wrapper == null) {
                wrapper = new SyncTaskQueueWrapper();
                syncTasks.put(syncTask.getSyncSignature(), wrapper);
            }
            f = new SyncTaskFuture(syncTask);
            wrapper.addTask(f);
            wrapper.startThreadIfNeeded();
        }

        return f;
    }

    @Override
    public <T> Future<T> syncSubmit(SyncTask<T> task) {
        if (task.getSyncLevel() <= 0) {
            return _threadFacade.submitSyncPool(task);
        } else {
            return doSyncSubmit(task);
        }
    }

    class SingleFlightFuture<T> extends AbstractTimeStatisticFuture<T> {
        private Integer taskId;

        public SingleFlightFuture(SingleFlightTask task) {
            super(task);
        }

        protected SingleFlightTask getTask() {
            return (SingleFlightTask) task;
        }

        public Integer getTaskId() {
            return taskId;
        }

        public void setTaskId(Integer taskId) {
            this.taskId = taskId;
        }

        @Override
        public boolean cancel(boolean mayInterruptIfRunning) {
            cancel();
            return true;
        }

        void singleFlightRun(final ReturnValueCompletion<Object> completion) {
            if (isCancelled()) {
                completion.fail(err(SysErrors.CANCEL_ERROR, "task failed due to cancelled"));
                return;
            }

            try {
                getTask().start(completion);
            } catch (Throwable t) {
                try {
                    if (!(t instanceof OperationFailureException)) {
                        _logger.warn(String.format("unhandled exception happened when calling %s", task.getClass().getName()), t);
                    }

                    done();
                } finally {
                    if (t instanceof OperationFailureException) {
                        completion.fail(operr(t.getMessage()));
                    } else {
                        completion.fail(inerr(t.getMessage()));
                    }
                }
            }
        }

        void singleFlightDone(SingleFlightTaskResult result) {
            getTask().singleFlightDone(result);
        }

        String getSyncSignature() {
            return getTask().getSyncSignature();
        }
    }

    class ChainFuture extends AbstractTimeStatisticFuture {
        private AtomicBoolean isNextCalled = new AtomicBoolean(false);

        public ChainFuture(ChainTask task) {
            super(task);
        }

        ChainTask getTask() {
            return (ChainTask) task;
        }

        @Override
        public boolean cancel(boolean mayInterruptIfRunning) {
            cancel();
            return true;
        }

        private void callNext(SyncTaskChain chain) {
            if (!isNextCalled.compareAndSet(false, true)) {
                return;
            }

            chain.next();
        }

        public void run(final SyncTaskChain chain) {
            if (isCancelled()) {
                callNext(chain);
                return;
            }

            try {
                getTask().run(() -> {
                    try {
                        done();
                    } finally {
                        callNext(chain);
                    }
                });
            } catch (Throwable t) {
                try {
                    if (!(t instanceof OperationFailureException)) {
                        _logger.warn(String.format("unhandled exception happened when calling %s", task.getClass().getName()), t);
                    }

                    done();
                } finally {
                    callNext(chain);
                }
            }
        }

        public int getSyncLevel() {
            return getTask().getSyncLevel();
        }

        public String getSyncSignature() {
            return getTask().getSyncSignature();
        }
    }

    private class SingleFlightQueueWrapper<T> extends AbstractTaskQueueWrapper {
        LinkedList pendingQueue = new LinkedList();
        volatile SingleFlightFuture runningTask = null;
        AtomicInteger taskCounter = new AtomicInteger(0);

        boolean addSingleFlightTask(SingleFlightFuture task) {
            task.setTaskId(taskCounter.addAndGet(1));
            pendingQueue.offer(task);

            if (syncSignature == null) {
                syncSignature = task.getSyncSignature();
            }
            
            return true;
        }

        void startSingleFlightIfNeed() {
            if (taskCounter.get() > 1) {
                logger.debug(String.format("single flight task[signature: %s] thread is running now," +
                                " skip start new thread", syncSignature));
                dumpTaskQueueIfNeeded(pendingQueue.size());
                return;
            }

            resetPendingQueueThreshold();
            _threadFacade.submit(new Task<Void>() {

                @Override
                public Void call() {
                    runSingleFlight();
                    return null;
                }

                @AsyncThread
                private void runSingleFlight() {
                    synchronized (singleFlightTasks) {
                        if (runningTask != null) {
                            logger.debug(String.format("single flight task[signature: %s, id: %s] is running now," +
                                            " skip poll new running task, current pending task num: %d", runningTask.getSyncSignature(),
                                    runningTask.getTaskId(), pendingQueue.size()));
                            return;
                        }

                        runningTask = (SingleFlightFuture) pendingQueue.poll();
                        if (runningTask == null) {
                            logger.debug(String.format("single flight task[signature: %s] has no task available" +
                                    " skip execute", syncSignature));
                            singleFlightTasks.remove(syncSignature);
                            return;
                        }
                    }

                    processTimeoutTask(runningTask);
                    runningTask.setStartExecutionTimeInMills(zTimer.getCurrentTimeMillis());
                    runningTask.singleFlightRun(new ReturnValueCompletion<Object>(null) {
                        @Override
                        public void success(Object object) {
                            executeSingleRunTasks(object, null);
                        }

                        @Override
                        public void fail(ErrorCode errorCode) {
                            executeSingleRunTasks(null, errorCode);
                        }
                    });
                }

                private void executeSingleRunTasks(Object object, ErrorCode errorCode) {
                    synchronized (singleFlightTasks) {
                        safeRun(object, runningTask, errorCode);
                        pendingQueue.forEach(task -> safeRun(object, (SingleFlightFuture) task, errorCode));

                        // all tasks done, reset counter
                        taskCounter.set(0);
                        runningTask = null;
                        pendingQueue.clear();
                    }

                    runSingleFlight();
                }

                private void safeRun(Object object, SingleFlightFuture<T> flightFuture, ErrorCode errorCode) {
                    SingleFlightTaskResult result = new SingleFlightTaskResult();
                    result.setResult(object);
                    try {
                        if (errorCode != null) {
                            result.setErrorCode(errorCode);
                        }
                        flightFuture.singleFlightDone(result);
                    } catch (Throwable t) {
                        if ((t instanceof OperationFailureException)) {
                            return;
                        }

                        _logger.warn(String.format("unhandled exception happened when calling %s", flightFuture.getClass().getName()), t);
                    } finally {
                        logger.debug(String.format("single flight task[signature: %s, id: %s] finish with %s",
                                runningTask.getSyncSignature(), flightFuture.getTaskId(), JSONObjectUtil.toJsonString(result)));
                        flightFuture.done();
                    }
                }

                @Override
                public String getName() {
                    return syncSignature;
                }
            });
        }

        @Override
        protected String getTaskQueueInfo() {
            StringBuilder tb = new StringBuilder();
            if (runningTask != null) {
                tb.append("\nRUNNING SINGLE FLIGHT TASK NUMBER: 1");
                tb.append(String.format("\nRUNNING SINGLE FLIGHT TASK NAME: %s", runningTask.getSyncSignature()));
            }

            tb.append(String.format("\nPENDING SINGLE FLIGHT TASK NUMBER: %s", pendingQueue.size()));
            tb.append(String.format("\nPENDING SINGLE FLIGHT TASK[NAME: %s, TASK QUEUE SIZE: %d] ",
                    syncSignature, pendingQueue.size()));
            for (Object obj : pendingQueue) {
                SingleFlightTask task = ((SingleFlightFuture) obj).getTask();

                if (task.getThreadContext() == null) {
                    break;
                }

                String taskId = null;
                if (task.getThreadContext().containsKey(Constants.THREAD_CONTEXT_API)) {
                    taskId = task.getThreadContext().get(Constants.THREAD_CONTEXT_API);
                }

                if (task.getThreadContext().containsKey(Constants.THREAD_CONTEXT_TASK)) {
                    taskId = task.getThreadContext().get(Constants.THREAD_CONTEXT_TASK);
                }

                if (taskId == null) {
                    break;
                }

                tb.append(String.format("\nPENDING TASK[NAME: %s, TASK ID: %s] ",
                        task.getName(), taskId));
            }

            return tb.toString();
        }
    }

    private class ChainTaskQueueWrapper extends AbstractTaskQueueWrapper {
        final LinkedList pendingQueue = new LinkedList();
        final Map<String, AtomicInteger> subPendingMap = new ConcurrentHashMap<>();
        final LinkedList runningQueue = new LinkedList();
        AtomicInteger counter = new AtomicInteger(0);
        int maxThreadNum = -1;

        int addSubPending(String deduplicateStr) {
            subPendingMap.compute(deduplicateStr, (k, v) -> {
                if (v == null) {
                    return new AtomicInteger(1);
                } else {
                    v.incrementAndGet();
                    return v;
                }
            });
            return subPendingMap.get(deduplicateStr).intValue();
        }

        void removeSubPending(String deduplicateStr, boolean removeIfZero) {
            subPendingMap.computeIfPresent(deduplicateStr, (k, v) -> {
                int r = v.decrementAndGet();
                if (r < 0) {
                    if (removeIfZero) {
                        return null;
                    }
                }
                return v;
            });
        }

        void removeSubPendingZero(String deduplicateStr) {
            subPendingMap.computeIfPresent(deduplicateStr, (k, v) -> {
                int r = v.intValue();
                if (r == 0) {
                    return null;
                }
                return v;
            });
        }

        void warningAndRemove(ChainFuture task, int length, int queueLength) {
            logger.warn(String.format("[%s] max pending size: %d, pending now: %d, throw the task: %s!", task.getTask().getDeduplicateString(), length, queueLength, task.getTask().getName()));
            removeSubPending(task.getTask().getDeduplicateString(), true);
        }

        boolean addTask(ChainFuture task, int length) {
            if (length != -1 && CoreGlobalProperty.CHAIN_TASK_QOS) {
                DebugUtils.Assert(task.getTask().getDeduplicateString() != null, "deduplicate String must be set if max pending string has been set!");
                AtomicInteger r = subPendingMap.get(task.getTask().getDeduplicateString());
                int queueLength = addSubPending(task.getTask().getDeduplicateString());
                if (queueLength > length) {
                    synchronized (runningQueue) {
                        if (length != 0 || queueLength != 1 || r != null) {
                            warningAndRemove(task, length, queueLength);
                            return false;
                        }
                    }
                }
            }
            pendingQueue.offer(task);

            if (maxThreadNum == -1) {
                maxThreadNum = task.getSyncLevel();
            } else if (maxThreadNum < task.getSyncLevel()) {
                logger.warn(String.format("task[name:%s] increases queue[name:%s]'s sync level from %s to %s", task.getTask().getName(), task.getSyncSignature(), maxThreadNum, task.getSyncLevel()));
                maxThreadNum = task.getSyncLevel();
            }

            if (syncSignature == null) {
                syncSignature = task.getSyncSignature();
            }
            return true;
        }

        void startThreadIfNeeded() {
            if (counter.get() >= maxThreadNum) {
                logger.debug(String.format("Chain task syncSignature: %s reached maxThreadNum: %s, current: %d, pending queue size: %d",
                        syncSignature, maxThreadNum, counter.get(), pendingQueue.size()));
                dumpTaskQueueIfNeeded(pendingQueue.size());
                return;
            }

            resetPendingQueueThreshold();
            counter.incrementAndGet();
            _threadFacade.submit(new Task<Void>() {
                @Override
                public String getName() {
                    return "sync-chain-thread";
                }

                // start a new thread every time to avoid stack overflow
                @AsyncThread
                private void runQueue() {
                    ChainFuture cf;
                    synchronized (chainTasks) {
                        // remove from pending queue and add to running queue later
                        cf = (ChainFuture) pendingQueue.poll();

                        if (cf == null) {
                            if (counter.decrementAndGet() == 0) {
                                chainTasks.remove(syncSignature);
                            }

                            return;
                        }
                    }

                    synchronized (runningQueue) {
                        processTimeoutTask(cf);
                        cf.setStartExecutionTimeInMills(zTimer.getCurrentTimeMillis());
                        // add to running queue
                        runningQueue.offer(cf);
                        Optional.ofNullable(getApiId(cf))
                                .ifPresent(apiId -> apiRunningSignature.computeIfAbsent(apiId,
                                        k -> Collections.synchronizedList(new ArrayList<>())).add(syncSignature));
                    }

                    // recover task context from backup
                    if (cf.getTask().getTaskContext() != null) {
                        TaskContext.setTaskContext(cf.getTask().getTaskContext());
                    }

                    logger.debug(String.format("Start executing runningQueue: %s, task name: %s", syncSignature, cf.getTask().getName()));

                    if (cf.getTask().getDeduplicateString() != null) {
                        removeSubPending(cf.getTask().getDeduplicateString(), false);
                    }

                    cf.run(() -> {
                        synchronized (runningQueue) {
                            Optional.ofNullable(getApiId(cf))
                                    .ifPresent(apiId -> apiRunningSignature.computeIfPresent(apiId, (k, sigs) -> {
                                        sigs.remove(syncSignature);
                                        return sigs.isEmpty() ? null : sigs;
                                    }));
                            runningQueue.remove(cf);
                            logger.debug(String.format("Finish executing runningQueue: %s, task name: %s", syncSignature, cf.getTask().getName()));

                            if (cf.getTask().getDeduplicateString() != null) {
                                removeSubPendingZero(cf.getTask().getDeduplicateString());
                            }
                        }

                        /*
                          Note: run queue @AsyncThread will set thread context from
                          current task and next task's context will not be set until
                          `cf.run` so the code from `runQueue()` to `cf.run` will
                          use a wrong api id which might be confusing.

                          Manually remove thread context here to avoid this issue.
                         */
                        TaskContext.removeTaskContext();
                        runQueue();
                    });
                }


                private String getApiId(DispatchQueueImpl.ChainFuture cf) {
                    Map<String, String> tc = cf.getTask().getThreadContext();
                    if (tc != null) {
                        return tc.get(Constants.THREAD_CONTEXT_API);
                    } else {
                        return null;
                    }
                }

                @Override
                public Void call() {
                    runQueue();
                    return null;
                }
            });
        }

        @Override
        protected String getTaskQueueInfo() {
            return getChainInfo().toString();
        }

        protected ChainInfo getChainInfo() {
            long now = zTimer.getCurrentTimeMillis();
            ChainInfo info = new ChainInfo();
            info.setMaxThreadNum(maxThreadNum);

            int index = 0;
            synchronized (runningQueue) {
                for (Object obj : runningQueue) {
                    ChainFuture cf = (ChainFuture) obj;
                    info.addRunningTask(TaskInfoBuilder.buildRunningTaskInfo(cf, now, index++));
                }
            }

            // pendingQueue is synchronized with chainTasks, do not synchronize itself
            synchronized (chainTasks) {
                for (Object obj : pendingQueue) {
                    ChainFuture cf = (ChainFuture) obj;
                    info.addPendingTask(TaskInfoBuilder.buildPendingTaskInfo(cf, now, index++));
                }
            }
            return info;
        }
    }

    @ExceptionSafe
    private void processTimeoutTask(AbstractTimeStatisticFuture abstractTimeStatisticFuture) {
        long now = System.currentTimeMillis();
        PendingTaskInfo taskInfo = TaskInfoBuilder.buildPendingTaskInfo(abstractTimeStatisticFuture, now, 0);
        long timeout = 0L;
        if (taskInfo.getContext().isEmpty()) {
            return;
        }

        for (String c : taskInfo.getContextList()) {
            Map context = JSONObjectUtil.toObject(c, LinkedHashMap.class);
            timeout = (context.get("timeout") == null) ? 0 : (long) context.get("timeout");
        }

        if (timeout > 0 && taskInfo.getPendingTime() * 1000 > timeout){
            logger.warn(String.format("this task has been pending for %s ms longer than timeout %s ms, cancel it. task info: %s",
                    taskInfo.getPendingTime()*1000, timeout, taskInfo));
            abstractTimeStatisticFuture.cancel(true);
        }
    }

    private <T> Future<T> doChainSyncSubmit(final ChainTask task) {
        assert task.getSyncSignature() != null : "How can you submit a chain task without sync signature ???";
        DebugUtils.Assert(task.getSyncLevel() >= 1, "getSyncLevel() must return 1 at least ");

        synchronized (chainTasks) {
            final String signature = task.getSyncSignature();
            ChainTaskQueueWrapper wrapper = chainTasks.get(signature);
            if (wrapper == null) {
                wrapper = new ChainTaskQueueWrapper();
                chainTasks.put(signature, wrapper);
            }

            ChainFuture cf = new ChainFuture(task);
            boolean succeed = wrapper.addTask(cf, task.getMaxPendingTasks());
            if (!succeed) {
                cf.cancel();
                logger.debug(String.format("Pending queue[%s] exceed max size, task name: %s, start execute callback", task.getSyncSignature(), task.getName()));
                task.exceedMaxPendingCallback();
            } else {
                wrapper.startThreadIfNeeded();
            }
            return cf;
        }
    }


    @Override
    public Future<Void> chainSubmit(ChainTask task) {
        return doChainSyncSubmit(task);
    }

    @Override
    public <T> Future<T> singleFlightSubmit(SingleFlightTask task) {
        return doSingleFlightSyncSubmit(task);
    }

    private <T> Future<T> doSingleFlightSyncSubmit(SingleFlightTask task) {
        assert task.getSyncSignature() != null : "How can you submit a single flight chain task without sync signature ???";

        synchronized (singleFlightTasks) {
            final String signature = task.getSyncSignature();
            SingleFlightQueueWrapper wrapper = singleFlightTasks.get(signature);
            if (wrapper == null) {
                wrapper = new SingleFlightQueueWrapper<T>();
                singleFlightTasks.put(signature, wrapper);
            }

            SingleFlightFuture sf = new SingleFlightFuture(task);
            wrapper.addSingleFlightTask(sf);
            wrapper.startSingleFlightIfNeed();
            return sf;
        }
    }

    @Override
    public Map<String, SyncTaskStatistic> getSyncTaskStatistics() {
        Map<String, SyncTaskStatistic> ret = new ConcurrentHashMap<>();
        synchronized (syncTasks) {
            for (SyncTaskQueueWrapper wrapper : syncTasks.values()) {
                SyncTaskStatistic statistic = new SyncTaskStatistic(
                        wrapper.syncSignature,
                        wrapper.maxThreadNum,
                        wrapper.counter.intValue(),
                        wrapper.queue.size()
                );
                ret.put(statistic.getSyncSignature(), statistic);

                logger.warn(JSONObjectUtil.toJsonString(statistic));
            }
        }

        return ret;
    }

    @Override
    public Map<String, ChainTaskStatistic> getChainTaskStatistics() {
        Map<String, ChainTaskStatistic> ret =  new ConcurrentHashMap<>();
        synchronized (chainTasks) {
            for (ChainTaskQueueWrapper wrapper : chainTasks.values()) {
                ChainTaskStatistic statistic = new ChainTaskStatistic(
                        wrapper.syncSignature,
                        wrapper.maxThreadNum,
                        wrapper.counter.intValue(),
                        wrapper.pendingQueue.size()
                );
                ret.put(statistic.getSyncSignature(), statistic);
            }
        }
        return ret;
    }

    @Override
    public boolean isChainTaskRunning(String signature) {
        synchronized (chainTasks) {
            return chainTasks.containsKey(signature);
        }
    }
}
