package org.zstack.testlib.core;

import org.zstack.core.Platform;
import org.zstack.core.thread.DispatchQueue;
import org.zstack.core.thread.PeriodicTask;
import org.zstack.core.thread.ThreadFacadeImpl;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.utils.StringDSL;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.ScheduledFuture;
import java.util.function.BooleanSupplier;

public class ThreadFacadeForTest extends ThreadFacadeImpl {
    private static final CLogger logger = Utils.getLogger(ThreadFacadeForTest.class);

    private volatile CopyOnWriteArraySet<String> blockedPeriodicTasks = new CopyOnWriteArraySet<>();

    @Override
    public void init() {
        super.init();
        reset();
    }

    public void reset() {
        blockedPeriodicTasks = new CopyOnWriteArraySet<>(new ThreadFacadeConfigLoader().loadBlockedPeriodicTasks());
        logger.info("ThreadFacadeForTest blocked periodic tasks:\n" + String.join("\n", blockedPeriodicTasks));
    }

    /**
     * @return cancel handle; {@code true} when this registration is removed
     */
    public BooleanSupplier addBlockedPeriodicTask(String taskName) {
        final CopyOnWriteArraySet<String> tasks = blockedPeriodicTasks;
        tasks.add(taskName);
        if (blockedPeriodicTasks != tasks) { // if add failed (maybe tasks reset), retry
            return addBlockedPeriodicTask(taskName);
        }

        return () -> {
            if (blockedPeriodicTasks != tasks) {
                return false;
            }
            synchronized (tasks) {
                if (blockedPeriodicTasks != tasks) {
                    return false;
                }
                return tasks.remove(taskName);
            }
        };
    }

    @Override
    protected Runnable createPeriodicTaskRunner(final PeriodicTask task) {
        final Runnable runner = super.createPeriodicTaskRunner(task);
        return () -> {
            if (isBlockedPeriodicTask(task.getName())) {
                logger.debug(String.format("skip blocked periodic task: %s", task.getName()));
                return;
            }
            runner.run();
        };
    }

    private boolean isBlockedPeriodicTask(String taskName) {
        Set<String> tasks = blockedPeriodicTasks;
        for (String pattern : tasks) {
            if (StringDSL.matchWildcard(taskName, pattern)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Run one already registered blocked periodic task on the current thread.
     * @return null when the task finishes, otherwise the failure
     */
    public ErrorCode triggerBlockedPeriodicTask(String taskName) {
        List<PeriodicTask> matched = new ArrayList<>();
        for (Map.Entry<PeriodicTask, ScheduledFuture<?>> entry : getPeriodicTasks().entrySet()) {
            PeriodicTask task = entry.getKey();
            if (entry.getValue().isDone() || entry.getValue().isCancelled()) {
                continue;
            }
            if (StringDSL.matchWildcard(task.getName(), taskName) && isBlockedPeriodicTask(task.getName())) {
                matched.add(task);
            }
        }
        if (matched.isEmpty()) {
            return Platform.operr("no blocked periodic task matched: %s", taskName);
        }

        for (PeriodicTask task : matched) {
            try {
                task.run();
            } catch (Throwable t) {
                logger.warn(String.format("blocked periodic task[%s] failed", task.getName()), t);
                if (t instanceof OperationFailureException) {
                    return ((OperationFailureException) t).getErrorCode();
                }
                return Platform.operr("blocked periodic task[%s] failed: %s", task.getName(), t.getMessage());
            }
        }
        return null;
    }

    @Override
    public boolean start() {
        boolean started = super.start();
        logger.info("ThreadFacadeForTest started");
        return started;
    }

    @Override
    public DispatchQueue getDispatchQueue() {
        return super.getDispatchQueue();
    }
}
