package org.zstack.core.aspect;

import java.sql.SQLTransactionRollbackException;
import org.springframework.transaction.annotation.Transactional;
import org.zstack.core.db.DatabaseGlobalProperty;
import org.zstack.utils.DebugUtils;
import org.zstack.core.db.DatabaseFacadeImpl;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicInteger;

public aspect DbDeadlockAspect {
    private static final CLogger logger = Utils.getLogger(DbDeadlockAspect.class);
    private static ThreadLocal<AtomicInteger> local = new ThreadLocal<>();

    declare error: withincode(@org.springframework.transaction.annotation.Transactional * *.*(..)) && withincode(@org.zstack.core.db.DeadlockAutoRestart * *.*(..)) : "@Transactional and @DeadlockAutoRestart can not be present on the same method. @DeadlockAutoRestart must be on parent method which calls method that has @Transactional";

    Object around() : execution(@org.zstack.core.db.DeadlockAutoRestart * *.*(..)) {
        RuntimeException bad = null;
        int times = DatabaseGlobalProperty.retryTimes;
        AtomicInteger refCount = local.get();
        if (refCount == null) {
            refCount = new AtomicInteger(0);
            local.set(refCount);
        }

        refCount.incrementAndGet();
        try {
            do {
                try {
                    return new Callable<Object>() {
                        @Override
                        @Transactional
                        public Object call() {
                           return proceed();
                        }
                    }.call();
                } catch (RuntimeException re) {
                    int c = refCount.get();
                    DatabaseFacadeImpl.increaseDberror();
                    if (c > 1) {
                        logger.warn(String.format("ref = %s, ask outer deadlock handler to handle it", c));
                        throw re;
                    }

                    times --;
                    bad = re;

                    Throwable root = DebugUtils.getRootCause(re);

                    if (root instanceof SQLTransactionRollbackException && root.getMessage().contains("Deadlock")) {
                        logger.warn("deadlock happened, retry");
                        DatabaseFacadeImpl.increaseDeadlock();
                        try {
                            TimeUnit.MILLISECONDS.sleep(ThreadLocalRandom.current().nextInt(400, 600));
                        } catch (InterruptedException e) {
                            logger.warn(e.getMessage(), e);
                        }
                    } else {
                        if (root.getMessage() != null && root.getMessage().contains("Lock wait timeout")) {
                            DatabaseFacadeImpl.increaseLocktimeout();
                        }
                        throw re;
                    }
                }
            } while (times > 0);

            logger.warn(String.format("DB deadlock still happens after retrying %s times, give up", DatabaseGlobalProperty.retryTimes));
            throw bad;
        } finally {
            refCount.decrementAndGet();
        }
    }
}
