package org.zstack.storage.addon.primary;

import org.hibernate.exception.ConstraintViolationException;
import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.transaction.annotation.Transactional;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.core.thread.SyncTask;
import org.zstack.core.thread.ThreadFacade;
import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.header.host.HostVO;
import org.zstack.header.storage.addon.primary.ExternalPrimaryStorageVO;
import org.zstack.header.storage.addon.primary.ExternalPrimaryStorageHostRefVO;
import org.zstack.header.storage.addon.primary.ExternalPrimaryStorageHostRefVO_;
import org.zstack.header.storage.primary.PrimaryStorageHostStatus;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import javax.persistence.LockModeType;
import java.sql.Timestamp;
import java.util.BitSet;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class ExternalHostIdGetter {
    private static final CLogger logger = Utils.getLogger(ExternalHostIdGetter.class);
    private static final Random random = new Random();

    @Autowired
    protected DatabaseFacade dbf;
    @Autowired
    private ThreadFacade thdf;

    private final int maxHostId;

    public ExternalHostIdGetter(int maxHostId) {
        this.maxHostId = maxHostId;
    }

    public ExternalPrimaryStorageHostRefVO getOrAllocateHostIdRef(String hostUuid, String psUuid) {
        if (!dbf.isExist(psUuid, ExternalPrimaryStorageVO.class)) {
            throw new CloudRuntimeException(String.format(
                    "can not find external primary storage[uuid: %s]", psUuid));
        }

        if (!dbf.isExist(hostUuid, HostVO.class)) {
            throw new CloudRuntimeException(String.format(
                    "can not find host[uuid: %s] for external primary storage[uuid: %s]", hostUuid, psUuid));
        }

        ExternalPrimaryStorageHostRefVO refVO = findRef(hostUuid, psUuid);
        if (refVO != null) {
            return refVO;
        }

        if (allocateAndSetHostId(hostUuid, psUuid) == null) {
            throw new CloudRuntimeException(String.format("cannot allocate host id for primary storage[uuid:%s]", psUuid));
        }

        return Q.New(ExternalPrimaryStorageHostRefVO.class)
                .eq(ExternalPrimaryStorageHostRefVO_.hostUuid, hostUuid)
                .eq(ExternalPrimaryStorageHostRefVO_.primaryStorageUuid, psUuid)
                .find();
    }

    private ExternalPrimaryStorageHostRefVO findRef(String hostUuid, String psUuid) {
        return Q.New(ExternalPrimaryStorageHostRefVO.class)
                .eq(ExternalPrimaryStorageHostRefVO_.hostUuid, hostUuid)
                .eq(ExternalPrimaryStorageHostRefVO_.primaryStorageUuid, psUuid)
                .find();
    }

    private Integer allocateAndSetHostId(String hostUuid, String psUuid) {
        Future<Integer> f = thdf.syncSubmit(new SyncTask<Integer>() {
            @Override
            public String getSyncSignature() {
                return "allocate-and-set-host-id-for-" + psUuid;
            }

            @Override
            public int getSyncLevel() {
                return 0;
            }

            @Override
            public String getName() {
                return String.format("allocate-and-set-host-id-for-host-%s-ps-%s", hostUuid, psUuid);
            }

            @Override
            public Integer call() throws Exception {
                return doAllocateAndSetHostId(hostUuid, psUuid);
            }
        });

        try {
            return f.get(1, TimeUnit.MINUTES);
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            throw new RuntimeException(e);
        }
    }

    @Transactional
    protected Integer doAllocateAndSetHostId(String hostUuid, String psUuid) {
        dbf.getEntityManager().find(ExternalPrimaryStorageVO.class, psUuid, LockModeType.PESSIMISTIC_WRITE);

        ExternalPrimaryStorageHostRefVO vo = findRef(hostUuid, psUuid);
        if (vo != null && vo.getHostId() > 0) {
            return vo.getHostId();
        }

        int hostId = allocateHostId(psUuid);

        try {
            if (vo == null) {
                vo = new ExternalPrimaryStorageHostRefVO();
                vo.setHostId(hostId);
                vo.setCreateDate(new Timestamp(System.currentTimeMillis()));
                vo.setHostUuid(hostUuid);
                vo.setPrimaryStorageUuid(psUuid);
                vo.setStatus(PrimaryStorageHostStatus.Disconnected);
                dbf.getEntityManager().persist(vo);
                logger.debug(String.format(
                        "new external primary storage[%s] host[%s] ref created, allocated host id[%s]", psUuid, hostUuid, vo.getHostId()));
                return hostId;
            } else if (vo.getHostId() != hostId) {
                boolean exists = Q.New(ExternalPrimaryStorageHostRefVO.class)
                        .eq(ExternalPrimaryStorageHostRefVO_.hostId, hostId)
                        .eq(ExternalPrimaryStorageHostRefVO_.primaryStorageUuid, psUuid)
                        .isExists();
                if (!exists) {
                    vo.setHostId(hostId);
                    dbf.getEntityManager().merge(vo);
                    return hostId;
                } else {
                    logger.warn(String.format("found abnormal duplicate entry for ExternalPrimaryStorageHostRefVO[hostUuid: %s, hostId: %s]", hostUuid, hostId));
                    return null;
                }
            }
        } catch (ConstraintViolationException e) {
            logger.error(String.format("found duplicate entry for hostUuid: %s, hostId: %s",
                    hostUuid, hostId));
            return null;
        }
        return null;
    }

    private int allocateHostId(String psUuid) {
        int total = this.maxHostId;
        Integer s = 1;
        Integer e = total;
        Integer ret = steppingAllocate(s, e, total, psUuid);
        if (ret != null) {
            return ret;
        }

        ret = steppingAllocate(s, e, total, psUuid);
        if (ret != null) {
            return ret;
        }

        throw new CloudRuntimeException(String.format("no available host id for external primary storage[uuid:%s]", psUuid));
    }

    private Integer steppingAllocate(Integer s, Integer e, int total, String psUuid) {
        int step = 100;
        int failureCount = 0;
        int failureCheckPoint = 10;

        while (s < e) {
            if (failureCheckPoint == failureCount++) {
                long count = Q.New(ExternalPrimaryStorageHostRefVO.class)
                        .eq(ExternalPrimaryStorageHostRefVO_.primaryStorageUuid, psUuid)
                        .gte(ExternalPrimaryStorageHostRefVO_.hostId, s)
                        .lte(ExternalPrimaryStorageHostRefVO_.hostId, e)
                        .count();
                if (count == total) {
                    logger.debug(String.format("host id range[s: %d, e: %d] has no vni available, try next one", s, e));
                    return null;
                } else {
                    failureCount = 0;
                }
            }

            int te = s + step;
            te = te > e ? e : te;
            List<Integer> used = Q.New(ExternalPrimaryStorageHostRefVO.class).select(ExternalPrimaryStorageHostRefVO_.hostId)
                    .eq(ExternalPrimaryStorageHostRefVO_.primaryStorageUuid, psUuid)
                    .gte(ExternalPrimaryStorageHostRefVO_.hostId, s)
                    .lte(ExternalPrimaryStorageHostRefVO_.hostId, te)
                    .listValues();

            if (te - s + 1 == used.size()) {
                s += step;
                continue;
            }

            Collections.sort(used);

            return randomAllocateHostId(s, te, used);
        }

        return null;
    }

    private static Integer randomAllocateHostId(Integer start, Integer end, List<Integer> allocated) {
        int total = (end - start + 1);
        if (total == allocated.size()) {
            return null;
        }

        BitSet full = new BitSet(total);
        for (Integer alloc : allocated) {
            full.set(alloc - start);
        }

        int next = random.nextInt(total);
        int a = full.nextClearBit(next);

        if (a >= total) {
            a = full.nextClearBit(0);
        }

        return a + start;
    }
}
