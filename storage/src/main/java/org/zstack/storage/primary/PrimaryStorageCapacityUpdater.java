package org.zstack.storage.primary;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.transaction.annotation.Transactional;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.DeadlockAutoRestart;
import org.zstack.core.errorcode.ErrorFacade;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.header.storage.primary.PrimaryStorageCapacityUpdaterRunnable;
import org.zstack.header.storage.primary.PrimaryStorageCapacityVO;
import org.zstack.header.storage.primary.PrimaryStorageConstant;
import org.zstack.header.storage.primary.RecalculatePrimaryStorageCapacityMsg;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import javax.persistence.EntityManager;
import javax.persistence.LockModeType;
import javax.persistence.TypedQuery;
import java.util.List;

import static org.zstack.core.Platform.operr;

/**
 * Created by frank on 10/19/2015.
 */

@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class PrimaryStorageCapacityUpdater {
    private static CLogger logger = Utils.getLogger(PrimaryStorageCapacityUpdater.class);

    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    private CloudBus bus;
    @Autowired
    private ErrorFacade errf;

    private String primaryStorageUuid;
    private TypedQuery<PrimaryStorageCapacityVO> query;
    private PrimaryStorageCapacityVO capacityVO;
    private PrimaryStorageCapacityVO originalCopy;

    private long totalForLog;
    private long availForLog;
    private long totalPhysicalForLog;
    private long availPhysicalForLog;

    public PrimaryStorageCapacityUpdater(String primaryStorageUuid) {
        this.primaryStorageUuid = primaryStorageUuid;
    }

    public PrimaryStorageCapacityUpdater(TypedQuery<PrimaryStorageCapacityVO> query) {
        this.query = query;
    }

    private void logDeletedPrimaryStorage() {
        logger.warn(String.format("[Primary Storage Capacity] unable to update capacity for the primary storage[uuid:%s]." +
                " It may have been deleted, cannot find it in database", primaryStorageUuid));
    }

    private void logCapacityChange() {
        if (logger.isTraceEnabled()) {
            StackTraceElement[] stackTraceElements = Thread.currentThread().getStackTrace();
            int index = 0;
            String fileName = PrimaryStorageCapacityUpdater.class.getSimpleName() + ".java";
            for (int i = 0; i < stackTraceElements.length; i++) {
                if (fileName.equals(stackTraceElements[i].getFileName())) {
                    index = i;
                }
            }
            StackTraceElement caller = stackTraceElements[index + 1];
            logger.trace(String.format("[Primary Storage Capacity] %s:%s:%s changed the capacity of the primary storage[uuid:%s] as:\n" +
                            "total: %s --> %s\n" +
                            "available: %s --> %s\n" +
                            "physical total: %s --> %s\n" +
                            "physical available: %s --> %s\n",
                    caller.getFileName(), caller.getMethodName(), caller.getLineNumber(), capacityVO.getUuid(),
                    totalForLog, capacityVO.getTotalCapacity(),
                    availForLog, capacityVO.getAvailableCapacity(),
                    totalPhysicalForLog, capacityVO.getTotalPhysicalCapacity(),
                    availPhysicalForLog, capacityVO.getAvailablePhysicalCapacity()));
        }
    }

    private boolean lockCapacity() {
        if (primaryStorageUuid != null) {
            capacityVO = dbf.getEntityManager().find(PrimaryStorageCapacityVO.class, primaryStorageUuid, LockModeType.PESSIMISTIC_WRITE);
        } else if (query != null) {
            query.setLockMode(LockModeType.PESSIMISTIC_WRITE);
            List<PrimaryStorageCapacityVO> caps = query.getResultList();
            capacityVO = caps.isEmpty() ? null : caps.get(0);
        }

        if (capacityVO != null) {
            totalForLog = capacityVO.getTotalCapacity();
            availForLog = capacityVO.getAvailableCapacity();
            totalPhysicalForLog = capacityVO.getTotalPhysicalCapacity();
            availPhysicalForLog = capacityVO.getAvailablePhysicalCapacity();

            originalCopy = new PrimaryStorageCapacityVO();
            originalCopy.setAvailableCapacity(capacityVO.getAvailableCapacity());
            originalCopy.setTotalCapacity(capacityVO.getTotalCapacity());
            originalCopy.setAvailablePhysicalCapacity(capacityVO.getAvailablePhysicalCapacity());
            originalCopy.setTotalPhysicalCapacity(capacityVO.getTotalPhysicalCapacity());
            originalCopy.setSystemUsedCapacity(capacityVO.getSystemUsedCapacity());
        }

        return capacityVO != null;
    }

    private boolean isResized() {
        return originalCopy != null &&
                capacityVO != null &&
                originalCopy.getTotalPhysicalCapacity() != 0 &&
                originalCopy.getTotalPhysicalCapacity() != capacityVO.getTotalPhysicalCapacity();
    }

    private void checkResize() {
        if (isResized()) {
            logger.debug(String.format("the physical capacity of primary storage[uuid:%s] changed from %s to %s," +
                            " this indicates the primary storage is re-sized." +
                            " We need to recalculate its capacity", capacityVO.getUuid(),
                    originalCopy.getTotalPhysicalCapacity(), capacityVO.getTotalPhysicalCapacity()));
            // primary storage re-sized
            RecalculatePrimaryStorageCapacityMsg msg = new RecalculatePrimaryStorageCapacityMsg();
            msg.setPrimaryStorageUuid(capacityVO.getUuid());
            bus.makeLocalServiceId(msg, PrimaryStorageConstant.SERVICE_ID);
            bus.send(msg);
        }
    }

    private void merge() {
        if (isResized()) {
            capacityVO.setTotalCapacity(capacityVO.getTotalPhysicalCapacity());
        }

        EntityManager entityManager = dbf.getEntityManager();
        capacityVO = entityManager.merge(capacityVO);
        entityManager.flush();

        logCapacityChange();
    }

    @Transactional
    private boolean _updateAvailablePhysicalCapacity(long avail) {
        if (!lockCapacity()) {
            logDeletedPrimaryStorage();
            return false;
        }

        capacityVO.setAvailablePhysicalCapacity(avail);
        merge();
        return true;
    }


    @DeadlockAutoRestart
    public boolean updateAvailablePhysicalCapacity(long avail) {
        boolean ret = _updateAvailablePhysicalCapacity(avail);
        checkResize();
        return ret;
    }

    @Transactional
    private boolean _increaseAvailableCapacity(long size) {
        if (!lockCapacity()) {
            logDeletedPrimaryStorage();
            return false;
        }

        long n = capacityVO.getAvailableCapacity() + size;
        if (n > capacityVO.getTotalCapacity()) {
            throw new CloudRuntimeException(String.format("invalid primary storage[uuid:%s] capacity, available capacity[%s] > total capacity[%s]",
                    capacityVO.getUuid(), n, capacityVO.getTotalCapacity()));
        }

        capacityVO.setAvailableCapacity(n);
        merge();
        return true;
    }

    @DeadlockAutoRestart
    public boolean increaseAvailableCapacity(long size) {
        boolean ret = _increaseAvailableCapacity(size);
        checkResize();
        return ret;
    }

    @Transactional
    private boolean _decreaseAvailableCapacity(long size) {
        if (!lockCapacity()) {
            logDeletedPrimaryStorage();
            return false;
        }

        capacityVO.setAvailableCapacity(capacityVO.getAvailableCapacity() - size);
        merge();
        return true;
    }

    @DeadlockAutoRestart
    public boolean decreaseAvailableCapacity(long size) {
        boolean ret = _decreaseAvailableCapacity(size);
        checkResize();
        return ret;
    }

    @Transactional
    private boolean _update(Long total, Long avail, Long physicalTotal, Long physicalAvail) {
        if (!lockCapacity()) {
            logDeletedPrimaryStorage();
            return false;
        }

        if (capacityVO.getSystemUsedCapacity() == null && physicalTotal != null && physicalAvail != null) {
            capacityVO.setSystemUsedCapacity(physicalTotal - physicalAvail);
        }

        if (total != null) {
            capacityVO.setTotalCapacity(total);
        }
        if (avail != null) {
            capacityVO.setAvailableCapacity(avail);
        }
        if (physicalTotal != null) {
            capacityVO.setTotalPhysicalCapacity(physicalTotal);
        }
        if (physicalAvail != null) {
            capacityVO.setAvailablePhysicalCapacity(physicalAvail);
        }

        merge();
        return true;
    }

    @DeadlockAutoRestart
    public boolean update(Long total, Long avail, Long physicalTotal, Long physicalAvail) {
        boolean ret = _update(total, avail, physicalTotal, physicalAvail);
        checkResize();
        return ret;
    }

    @Transactional
    private synchronized boolean _run(PrimaryStorageCapacityUpdaterRunnable runnable) {
        if (!lockCapacity()) {
            logDeletedPrimaryStorage();
            return false;
        }

        PrimaryStorageCapacityVO cap = runnable.call(capacityVO);
        if (cap != null) {
            capacityVO = cap;
            merge();
            return true;
        }
        return false;
    }

    @DeadlockAutoRestart
    public boolean run(PrimaryStorageCapacityUpdaterRunnable runnable) {
        boolean ret = _run(runnable);
        checkResize();
        return ret;
    }

    @Deprecated
    public boolean reserve(long size) {
        return reserve(size, true);
    }

    @Transactional
    private boolean _reserve(long size, boolean exceptionOnFailure) {
        if (!lockCapacity()) {
            logDeletedPrimaryStorage();
            return false;
        }

        if (capacityVO.getAvailableCapacity() < size) {
            if (exceptionOnFailure) {
                throw new OperationFailureException(operr("cannot reserve %s bytes on the primary storage[uuid:%s]," +
                        " it's short of available capacity", size, capacityVO.getUuid()));
            } else {
                return false;
            }
        }

        capacityVO.setAvailableCapacity(capacityVO.getAvailableCapacity() - size);
        merge();
        return true;
    }

    @DeadlockAutoRestart
    public boolean reserve(long size, boolean exceptionOnFailure) {
        return _reserve(size, exceptionOnFailure);
    }
}
