package org.zstack.storage.backup;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.transaction.annotation.Transactional;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.errorcode.ErrorFacade;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.header.storage.backup.BackupStorageCapacity;
import org.zstack.header.storage.backup.BackupStorageCapacityUpdaterRunnable;
import org.zstack.header.storage.backup.BackupStorageVO;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import javax.persistence.LockModeType;

import static org.zstack.core.Platform.operr;

/**
 * Created by xing5 on 2016/4/28.
 */
@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class BackupStorageCapacityUpdater {
    private static CLogger logger = Utils.getLogger(BackupStorageCapacityUpdater.class);

    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    private CloudBus bus;
    @Autowired
    private ErrorFacade errf;

    private String backupStorageUuid;
    private BackupStorageVO capacityVO;
    private BackupStorageVO originalCopy;

    private long totalForLog;
    private long availForLog;

    public BackupStorageCapacityUpdater(String backupStorageUuid) {
        this.backupStorageUuid = backupStorageUuid;
    }

    private void logCapacityChange() {
        if (logger.isTraceEnabled()) {
            StackTraceElement[] stackTraceElements = Thread.currentThread().getStackTrace();
            int index = 0;
            String fileName = BackupStorageCapacityUpdater.class.getSimpleName() + ".java";
            for (int i=0; i<stackTraceElements.length; i++) {
                if (fileName.equals(stackTraceElements[i].getFileName())) {
                    index = i;
                }
            }
            StackTraceElement caller = stackTraceElements[index+1];
            logger.trace(String.format("[Backup Storage Capacity] %s:%s:%s changed the capacity of the backup storage[uuid:%s] as:\n" +
                            "total: %s --> %s\n" +
                            "available: %s --> %s\n" , caller.getFileName(), caller.getMethodName(), caller.getLineNumber(), capacityVO.getUuid(),
                    totalForLog, capacityVO.getTotalCapacity(),
                    availForLog, capacityVO.getAvailableCapacity()));
        }
    }

    private boolean lockCapacity() {
        if (backupStorageUuid != null) {
            capacityVO = dbf.getEntityManager().find(BackupStorageVO.class, backupStorageUuid, LockModeType.PESSIMISTIC_WRITE);
        }

        if (capacityVO != null) {
            totalForLog = capacityVO.getTotalCapacity();
            availForLog = capacityVO.getAvailableCapacity();

            originalCopy = new BackupStorageVO();
            originalCopy.setAvailableCapacity(capacityVO.getAvailableCapacity());
            originalCopy.setTotalCapacity(capacityVO.getTotalCapacity());
        }

        return capacityVO != null;
    }

    private void checkResize() {
        if (originalCopy != null && capacityVO != null && originalCopy.getTotalCapacity() != 0 && originalCopy.getTotalCapacity() != capacityVO.getTotalCapacity()) {
            logger.debug(String.format("the capacity of backup storage[uuid:%s] changed from %s to %s, this indicates the backup storage is re-sized." +
                    " We need to recalculate its capacity", capacityVO.getUuid(), originalCopy.getTotalCapacity(), capacityVO.getTotalCapacity()));
            // primary storage re-sized
        }
    }

    private void merge() {
        capacityVO = dbf.getEntityManager().merge(capacityVO);
        logCapacityChange();
    }


    private void logDeletedBackupStorage() {
        logger.warn(String.format("[Backup Storage Capacity] unable to update capacity for the backup storage[uuid:%s]. It may have been deleted, cannot find it in database",
                backupStorageUuid));
    }

    @Transactional
    private boolean _increaseAvailableCapacity(long size) {
        if (!lockCapacity()) {
            logDeletedBackupStorage();
            return false;
        }

        long n = capacityVO.getAvailableCapacity() + size;
        if (n > capacityVO.getTotalCapacity()) {
            throw new CloudRuntimeException(String.format("invalid backup storage[uuid:%s] capacity, available capacity[%s] > total capacity[%s]",
                    capacityVO.getUuid(), n, capacityVO.getTotalCapacity()));
        }

        capacityVO.setAvailableCapacity(n);
        merge();
        return true;
    }

    public boolean reserveCapacity(long size) {
        return _reserveCapacity(size, true);
    }

    public boolean reserveCapacity(long size, boolean exceptionOnFailure) {
        return _reserveCapacity(size, exceptionOnFailure);
    }

    @Transactional
    private boolean _reserveCapacity(long size, boolean exceptionOnFailure) {
        if (!lockCapacity()) {
            logDeletedBackupStorage();
            return false;
        }

        if (capacityVO.getAvailableCapacity() < size) {
            if (!exceptionOnFailure) {
                return false;
            } else {
                throw new OperationFailureException(operr("cannot reserve %s on the backup storage[uuid:%s], it only has %s available",
                                size, backupStorageUuid, capacityVO.getAvailableCapacity()));
            }
        }

        return _decreaseAvailableCapacity(size);
    }

    public boolean increaseAvailableCapacity(long size) {
        boolean ret = _increaseAvailableCapacity(size);
        checkResize();
        return ret;
    }

    @Transactional
    private boolean _decreaseAvailableCapacity(long size) {
        if (!lockCapacity()) {
            logDeletedBackupStorage();
            return false;
        }

        capacityVO.setAvailableCapacity(capacityVO.getAvailableCapacity() - size);
        merge();
        return true;
    }

    public boolean decreaseAvailableCapacity(long size) {
        boolean ret = _decreaseAvailableCapacity(size);
        checkResize();
        return ret;
    }

    @Transactional
    private boolean _update(Long total, Long avail) {
        if (!lockCapacity()) {
            logDeletedBackupStorage();
            return false;
        }

        if (total != null) {
            capacityVO.setTotalCapacity(total);
        }
        if (avail != null) {
            capacityVO.setAvailableCapacity(avail);
        }

        merge();
        return true;
    }

    public boolean update(Long total, Long avail) {
        boolean ret = _update(total, avail);
        checkResize();
        return ret;
    }

    @Transactional
    private boolean _run(BackupStorageCapacityUpdaterRunnable runnable) {
        if (!lockCapacity()) {
            logDeletedBackupStorage();
            return false;
        }

        BackupStorageCapacity cap = new BackupStorageCapacity();
        cap.setUuid(capacityVO.getUuid());
        cap.setAvailableCapacity(capacityVO.getAvailableCapacity());
        cap.setTotalCapacity(capacityVO.getTotalCapacity());

        cap = runnable.call(cap);
        if (cap != null) {
            capacityVO.setTotalCapacity(cap.getTotalCapacity());
            capacityVO.setAvailableCapacity(cap.getAvailableCapacity());
            merge();
            return true;
        }

        return false;
    }

    public boolean run(BackupStorageCapacityUpdaterRunnable runnable) {
        boolean ret = _run(runnable);
        checkResize();
        return ret;
    }
}
