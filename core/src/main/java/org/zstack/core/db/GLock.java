package org.zstack.core.db;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.utils.DebugUtils;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;

/**
 */
@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class GLock {
    private static final CLogger logger = Utils.getLogger(GLock.class);

    private static final Map<String, ReentrantLock> memLocks = new HashMap<String, ReentrantLock>();

    private static long defaultWaitTimeout = DatabaseGlobalProperty.GLockWaitTimeout;

    private DataSource dataSource;
    private Connection conn;
    private final String name;
    private final long timeout;
    private boolean success = false;
    private static final ThreadLocal<List<String>> isLocked = new ThreadLocal<List<String>>() {
        @Override
        protected List<String> initialValue()
        {
            return new ArrayList<String>();
        }
    };

    private boolean alsoUseMemoryLock = true;

    @Autowired
    private DatabaseFacade dbf;

    public GLock(String name, long timeout) {
        this.name = name;
        this.timeout = timeout;
        dataSource = dbf.getDataSource();
    }

    public boolean isAlsoUseMemoryLock() {
        return alsoUseMemoryLock;
    }

    public void setAlsoUseMemoryLock(boolean alsoUseMemoryLock) {
        this.alsoUseMemoryLock = alsoUseMemoryLock;
    }

    private void checkInThread() {
        List<String> locks = isLocked.get();
        if (locks.contains(name)) {
           throw new CloudRuntimeException(String.format("Thread[%s] has acquired lock[%s], you can NOT acquire the lock again before unlock, GLock is non reentrant",
                   Thread.currentThread().getName(), name));
        }
        locks.add(name);
    }

    private void checkOutThread() {
        List<String> locks = isLocked.get();
        locks.remove(name);
    }

    public void lock() {
        lock(defaultWaitTimeout);
    }

    public void lock(long waitTimeout) {
        if (alsoUseMemoryLock) {
            checkInThread();
        }

        ReentrantLock mlock = null;
        if (alsoUseMemoryLock) {
            synchronized (memLocks) {
                mlock = memLocks.get(name);
                if (mlock == null) {
                    mlock = new ReentrantLock();
                    memLocks.put(name, mlock);
                }

                if (memLocks.size() > 100) {
                    logger.warn(String.format("there are more than 100 GLocks[num:%s] are created, something must be wrong in our program", memLocks.size()));
                }
            }
        }

        try {
            if (logger.isTraceEnabled()) {
                logger.trace(String.format("[GLock]: thread[%s] is acquiring lock[%s]", Thread.currentThread().getName(), name));
            }

            if (alsoUseMemoryLock) {
                mlock.lock();
                if (logger.isTraceEnabled()) {
                    logger.trace(String.format("[GLock Memory Lock]: thread[%s] got memory lock[%s]", Thread.currentThread().getName(), name));
                }
            }

            PreparedStatement pstmt = null;
            try {
                conn = dataSource.getConnection();
                conn.setAutoCommit(true);
                pstmt = conn.prepareStatement(String.format("select get_lock('%s', %s)", name, timeout),ResultSet.TYPE_SCROLL_INSENSITIVE,ResultSet.CONCUR_READ_ONLY);
                //pstmt = conn.prepareStatement(String.format("select get_lock('%s', %s)", name, timeout));
                if (waitTimeout > 0) {
                    pstmt.execute(String.format("set wait_timeout=%d", waitTimeout));
                }
                ResultSet rs = pstmt.executeQuery();
                if (rs == null) {
                    String err = "Unable to get DB lock: " + name + ", internal database error happened";
                    throw new CloudRuntimeException(err);
                } else if (rs.first() && rs.getInt(1) == 0) {
                    throw new CloudRuntimeException(String.format("lock[%s] failed, timeout after %s seconds", name, timeout));
                }

                if (logger.isTraceEnabled()) {
                    logger.trace(String.format("[GLock DB Lock]: thread: %s got DB lock[%s], during timeout[%s secs]", Thread.currentThread().getName(), name, timeout));
                }
            } catch (SQLException e) {
                throw new CloudRuntimeException(String.format("[GLock Error]: cannon get DB connection for lock[%s]", name), e);
            } finally {
                if (pstmt != null) {
                    try {
                        pstmt.close();
                    } catch (SQLException e) {
                        logger.warn("Unable to close PreparedStatement for lock: " + name, e);
                    }
                }
            }
            success = true;
        } catch (Throwable t) {
            if (conn != null) {
                try {
                    conn.close();
                } catch (SQLException e) {
                    logger.warn(e.getMessage(), e);
                }
            }

            if (alsoUseMemoryLock) {
                mlock.unlock();
            }

            success = false;

            if (alsoUseMemoryLock) {
                checkOutThread();
            }

            if (!(t instanceof CloudRuntimeException)) {
                throw new CloudRuntimeException(t);
            } else {
                throw (CloudRuntimeException)t;
            }
        }
    }

    public void unlock() {
        if (!success) {
            if (logger.isTraceEnabled()) {
                logger.trace(String.format("[GLock]: skip unlock for thread[%s] on lock[%s], because previous lock() is not success",
                        Thread.currentThread().getName(), name));
            }

            return;
        }

        ReentrantLock lock = null;
        if (alsoUseMemoryLock) {
            synchronized (memLocks) {
                lock = memLocks.get(name);
            }
        }

        try {
            if (alsoUseMemoryLock) {
                DebugUtils.Assert(lock != null, String.format("cannot find LockWrapper for GLock[%s], is unlock mistakenly called twice???", name));
            }

            if (logger.isTraceEnabled()) {
                logger.trace(String.format("[GLock]: thread[%s] is releasing lock[%s]", Thread.currentThread().getName(), name));
            }

            PreparedStatement pstmt = null;
            try {
                pstmt = conn.prepareStatement(String.format("select release_lock('%s')", name), ResultSet.TYPE_SCROLL_INSENSITIVE,ResultSet.CONCUR_READ_ONLY);
                try (ResultSet rs = pstmt.executeQuery()) {
                    if (!rs.next()) {
                        throw new CloudRuntimeException("Mysql cannot find lock: " + name);
                    } else if (rs.first() && rs.getInt(1) == 0) {
                        String err = "Unable to release DB lock: " + name + ", lock: " + name + " is not held by this connection, internal error";
                        throw new CloudRuntimeException(err);
                    }
                }


                if (logger.isTraceEnabled()) {
                    logger.trace(String.format("[GLock Release DB Lock] thread[%s] released DB lock[%s]", Thread.currentThread().getName(), name));
                }
            } catch (SQLException e) {
                throw new CloudRuntimeException("Unable to release lock: " + name, e);
            } finally {
                if (pstmt != null) {
                    try {
                        pstmt.close();
                    } catch (SQLException e) {
                        logger.warn("Unable to close PreparedStatement for lock: " + name, e);
                    }
                }

                try {
                    conn.close();
                } catch (SQLException e) {
                    logger.warn(e.getMessage(), e);
                }
            }
        } finally {
            if (alsoUseMemoryLock) {
                if (lock != null) {
                    lock.unlock();
                }
            }

            if (alsoUseMemoryLock) {
                checkOutThread();
            }

            if (logger.isTraceEnabled()) {
                logger.trace(String.format("[GLock Release Memory Lock]: thread[%s] released memory lock[%s]", Thread.currentThread().getName(), name));
            }
        }
    }
}
