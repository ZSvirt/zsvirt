package org.zstack.storage.primary.sharedblock;

import org.hibernate.exception.ConstraintViolationException;
import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.transaction.annotation.Transactional;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.core.db.SimpleQuery;
import org.zstack.core.thread.ChainTask;
import org.zstack.core.thread.SyncTaskChain;
import org.zstack.core.thread.ThreadFacade;
import org.zstack.header.core.Completion;
import org.zstack.header.core.FutureCompletion;
import org.zstack.header.core.FutureReturnValueCompletion;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.header.host.HostVO;
import org.zstack.header.host.HostVO_;
import org.zstack.header.storage.primary.PrimaryStorageHostStatus;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import javax.persistence.LockModeType;
import java.sql.Timestamp;
import java.util.BitSet;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;

@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class SharedBlockHostIdGetter {
    private static final CLogger logger = Utils.getLogger(SharedBlockHostIdGetter.class);
    private static final Random random = new Random();

    @Autowired
    protected DatabaseFacade dbf;
    @Autowired
    private ThreadFacade thdf;

    public SharedBlockGroupPrimaryStorageHostRefVO getHostIdRef(String hostUuid, String psUuid) {
        SharedBlockGroupPrimaryStorageHostRefVO refVO = getRefIfExists(hostUuid, psUuid);
        if (refVO != null) return refVO;

        allocateAndSetHostId(hostUuid, psUuid, null, PrimaryStorageHostStatus.Disconnected);

        return Q.New(SharedBlockGroupPrimaryStorageHostRefVO.class)
                .eq(SharedBlockGroupPrimaryStorageHostRefVO_.hostUuid, hostUuid)
                .eq(SharedBlockGroupPrimaryStorageHostRefVO_.primaryStorageUuid, psUuid)
                .find();
    }

    private SharedBlockGroupPrimaryStorageHostRefVO getRefIfExists(String hostUuid, String psUuid) {
        if (!Q.New(SharedBlockGroupVO.class)
                .eq(SharedBlockGroupVO_.uuid, psUuid)
                .isExists()) {
            throw new CloudRuntimeException(String.format(
                    "can not find shared block primary storage[uuid: %s]", psUuid));
        }

        if (!Q.New(HostVO.class)
                .eq(HostVO_.uuid, hostUuid)
                .isExists()) {
            throw new CloudRuntimeException(String.format(
                    "can not find host[uuid: %s] for shared block primary storage[uuid: %s]", hostUuid, psUuid));
        }

        SharedBlockGroupPrimaryStorageHostRefVO refVO = Q.New(SharedBlockGroupPrimaryStorageHostRefVO.class)
                .eq(SharedBlockGroupPrimaryStorageHostRefVO_.hostUuid, hostUuid)
                .eq(SharedBlockGroupPrimaryStorageHostRefVO_.primaryStorageUuid, psUuid)
                .find();

        return refVO;
    }

    public void allocateAndSetHostId(String hostUuid, String psUuid, Integer hostId, PrimaryStorageHostStatus status) {
        FutureCompletion completion = new FutureCompletion(null);
        thdf.chainSubmit(new ChainTask(completion) {
            @Override
            public String getSyncSignature() {
                return "allocate-and-set-host-id-for-" + psUuid;
            }

            @Override
            public void run(SyncTaskChain chain) {
                doAllocateAndSetHostId(hostUuid, psUuid, hostId, status);
                completion.success();
                chain.next();
            }

            @Override
            public String getName() {
                return String.format("allocate-and-set-host-id-for-host-%s-ps-%s", hostUuid, psUuid);
            }
        });
        completion.await(TimeUnit.MINUTES.toMillis(1));
        if (!completion.isSuccess()) {
            throw new OperationFailureException(completion.getErrorCode());
        }
    }

    @Transactional
    private void doAllocateAndSetHostId(String hostUuid, String psUuid, Integer hostId, PrimaryStorageHostStatus status) {
        dbf.getEntityManager().find(SharedBlockGroupVO.class, psUuid, LockModeType.PESSIMISTIC_WRITE);
        if (hostId == null) {
            hostId = allocateHostId(psUuid);
        }

        SharedBlockGroupPrimaryStorageHostRefVO vo = getRefIfExists(hostUuid, psUuid);
        try {
            if (vo == null) {
                vo = new SharedBlockGroupPrimaryStorageHostRefVO();
                vo.setHostId(hostId);
                vo.setCreateDate(new Timestamp(System.currentTimeMillis()));
                vo.setHostUuid(hostUuid);
                vo.setPrimaryStorageUuid(psUuid);
                vo.setStatus(status);
                dbf.getEntityManager().persist(vo);
            } else if (!vo.getHostId().equals(hostId)){
                boolean exists = Q.New(SharedBlockGroupPrimaryStorageHostRefVO.class)
                        .eq(SharedBlockGroupPrimaryStorageHostRefVO_.hostId, hostId)
                        .eq(SharedBlockGroupPrimaryStorageHostRefVO_.primaryStorageUuid, psUuid)
                        .isExists();
                if (!exists) {
                    vo.setHostId(hostId);
                    dbf.getEntityManager().merge(vo);
                } else {
                    logger.warn(String.format("found abnormal duplicate entry for SharedBlockGroupPrimaryStorageHostRefVO[hostUuid: %s, hostId: %s]", hostUuid, hostId));
                }
            }
        } catch (ConstraintViolationException e) {
            logger.info(String.format("found duplicate entry for hostUuid: %s, hostId: %s",
                    hostUuid, hostId));
        }
        logger.debug(String.format(
                "new sharedblock group primary storage[%s] host[%s] ref created, allocated host id[%s]", psUuid, hostUuid, vo.getHostId()));
    }

    private Integer allocateHostId(String psUuid) {
        int total = 1999;
        Integer s = 1;
        Integer e = 1999;
        Integer ret = steppingAllocate(s, e, total, psUuid);
        if (ret != null) {
            return ret;
        }

        return steppingAllocate(s, e, total, psUuid);
    }

    private Integer steppingAllocate(Integer s, Integer e, int total, String psUuid) {
        int step = 100;
        int failureCount = 0;
        int failureCheckPoint = 20;

        while (s < e) {
            if (failureCheckPoint == failureCount++) {
                SimpleQuery<SharedBlockGroupPrimaryStorageHostRefVO> q = dbf.createQuery(SharedBlockGroupPrimaryStorageHostRefVO.class);
                q.add(SharedBlockGroupPrimaryStorageHostRefVO_.primaryStorageUuid, SimpleQuery.Op.EQ, psUuid);
                q.add(SharedBlockGroupPrimaryStorageHostRefVO_.hostId, SimpleQuery.Op.GTE, s);
                q.add(SharedBlockGroupPrimaryStorageHostRefVO_.hostId, SimpleQuery.Op.LTE, e);
                long count = q.count();
                if (count == total) {
                    logger.debug(String.format("host id range[s: %d, e: %d] has no vni available, try next one", s, e));
                    return null;
                } else {
                    failureCount = 0;
                }
            }

            int te = s + step;
            te = te > e ? e : te;
            SimpleQuery<SharedBlockGroupPrimaryStorageHostRefVO> q = dbf.createQuery(SharedBlockGroupPrimaryStorageHostRefVO.class);
            q.select(SharedBlockGroupPrimaryStorageHostRefVO_.hostId);
            q.add(SharedBlockGroupPrimaryStorageHostRefVO_.hostId, SimpleQuery.Op.GTE, s);
            q.add(SharedBlockGroupPrimaryStorageHostRefVO_.hostId, SimpleQuery.Op.LTE, te);
            q.add(SharedBlockGroupPrimaryStorageHostRefVO_.primaryStorageUuid, SimpleQuery.Op.EQ, psUuid);
            List<Integer> used = q.listValue();
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
