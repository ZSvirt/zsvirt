package org.zstack.compute.allocator;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.transaction.annotation.Transactional;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.DeadlockAutoRestart;
import org.zstack.header.allocator.HostCapacityVO;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import javax.persistence.LockModeType;
import javax.persistence.TypedQuery;
import java.util.List;

/**
 * Created by frank on 11/2/2015.
 */
@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class HostCapacityUpdater {
    private static final CLogger logger = Utils.getLogger(HostCapacityUpdater.class);

    @Autowired
    private DatabaseFacade dbf;

    private String hostUuid;
    private TypedQuery<HostCapacityVO> query;
    private HostCapacityVO capacityVO;
    private HostCapacityVO originalCopy;

    public HostCapacityUpdater(String hostUuid) {
        this.hostUuid = hostUuid;
    }

    public HostCapacityUpdater(TypedQuery<HostCapacityVO> query) {
        this.query = query;
    }

    private void logDeletedHost() {
        logger.warn(String.format("[Host Capacity] unable to update capacity for the host[uuid:%s]. It may have been deleted, cannot find it in database",
                hostUuid));
    }

    private void logCapacityChange() {
        if (logger.isTraceEnabled()) {
            StackTraceElement[] stackTraceElements = Thread.currentThread().getStackTrace();
            int index = 0;
            String fileName = HostCapacityUpdater.class.getSimpleName() + ".java";
            for (int i=0; i<stackTraceElements.length; i++) {
                if (fileName.equals(stackTraceElements[i].getFileName())) {
                    index = i;
                }
            }
            StackTraceElement caller = stackTraceElements[index+1];
            logger.trace(String.format("[Host Capacity] %s:%s:%s changed the capacity of the host[uuid:%s] as:\n" +
                            "total cpu: %s --> %s\n" +
                            "available cpu: %s --> %s\n" +
                            "total memory: %s --> %s\n" +
                            "available memory: %s --> %s\n" +
                            "total physical memory: %s --> %s\n" +
                            "available physical memory: %s --> %s\n",
                    caller.getFileName(), caller.getMethodName(), caller.getLineNumber(), capacityVO.getUuid(),
                    originalCopy.getTotalCpu(), capacityVO.getTotalCpu(),
                    originalCopy.getAvailableCpu(), capacityVO.getAvailableCpu(),
                    originalCopy.getTotalMemory(), capacityVO.getTotalMemory(),
                    originalCopy.getAvailableMemory(), capacityVO.getAvailableMemory(),
                    originalCopy.getTotalPhysicalMemory(), capacityVO.getTotalPhysicalMemory(),
                    originalCopy.getAvailablePhysicalMemory(), capacityVO.getAvailablePhysicalMemory()));
        }
    }

    private boolean lockCapacity() {
        if (hostUuid != null) {
            capacityVO = dbf.getEntityManager().find(HostCapacityVO.class, hostUuid, LockModeType.PESSIMISTIC_WRITE);
        } else if (query != null) {
            query.setLockMode(LockModeType.PESSIMISTIC_WRITE);
            List<HostCapacityVO> caps = query.getResultList();
            capacityVO = caps.isEmpty() ? null : caps.get(0);
        }

        if (capacityVO != null) {
            originalCopy = new HostCapacityVO();
            originalCopy.setTotalCpu(capacityVO.getTotalCpu());
            originalCopy.setAvailableCpu(capacityVO.getAvailableCpu());
            originalCopy.setTotalMemory(capacityVO.getTotalMemory());
            originalCopy.setAvailableMemory(capacityVO.getAvailableMemory());
            originalCopy.setTotalPhysicalMemory(capacityVO.getTotalPhysicalMemory());
            originalCopy.setAvailablePhysicalMemory(capacityVO.getAvailablePhysicalMemory());
        }

        return capacityVO != null;
    }

    private void merge() {
        capacityVO = dbf.getEntityManager().merge(capacityVO);
        logCapacityChange();
    }

    @Transactional
    private boolean _run(HostCapacityUpdaterRunnable runnable) {
        if (!lockCapacity()) {
            logDeletedHost();
            return false;
        }

        HostCapacityVO cap = runnable.call(capacityVO);
        if (cap != null) {
            capacityVO = cap;
            merge();
            return true;
        }
        return false;
    }

    @DeadlockAutoRestart
    public boolean run(HostCapacityUpdaterRunnable runnable) {
        return _run(runnable);
    }

}
