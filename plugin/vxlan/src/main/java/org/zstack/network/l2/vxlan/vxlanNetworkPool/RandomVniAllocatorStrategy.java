package org.zstack.network.l2.vxlan.vxlanNetworkPool;

import org.zstack.core.db.Q;
import org.zstack.network.l2.vxlan.vxlanNetwork.VxlanNetworkVO;
import org.zstack.network.l2.vxlan.vxlanNetwork.VxlanNetworkVO_;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.BitSet;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * Created by weiwang on 10/03/2017.
 */
public class RandomVniAllocatorStrategy extends AbstractVniAllocatorStrategy {
    private static final CLogger logger = Utils.getLogger(RandomVniAllocatorStrategy.class);
    public static final VniAllocatorType type = new VniAllocatorType(VxlanNetworkPoolConstant.RANDOM_VNI_ALLOCATOR_STRATEGY);
    private static final Random random = new Random();

    @Override
    public VniAllocatorType getType() {
        return type;
    }

    @Override
    public Integer allocateVni(VniAllocateMessage msg) {
        if (msg.getRequiredVni() != null) {
            return allocateRequiredVni(msg);
        }

        List<VniRangeVO> ranges = Q.New(VniRangeVO.class)
                .eq(VniRangeVO_.l2NetworkUuid, msg.getL2NetworkUuid())
                .list();

        Collections.shuffle(ranges);

        do {
            Integer vni = null;
            VniRangeVO tr = null;

            for (VniRangeVO r : ranges) {
                vni = allocateVni(r);
                tr = r;
                if (vni != null) {
                    break;
                }
            }

            if (vni == null) {
                    /* No available vniin ranges */
                return null;
            }
            return vni;
        } while (true);
    }

    private Integer allocateVni(VniRangeVO vo) {
        int total = vo.size();
        Integer s = random.nextInt(total) + vo.getStartVni();
        Integer e = vo.getEndVni();
        Integer ret = steppingAllocate(s, e, total, vo.getUuid(), vo.getL2NetworkUuid());
        if (ret != null) {
            return ret;
        }

        e = s;
        s = vo.getStartVni();
        return steppingAllocate(s, e, total, vo.getUuid(), vo.getL2NetworkUuid());
    }

    private Integer steppingAllocate(Integer s, Integer e, int total, String rangeUuid, String poolUuid) {
        int step = 254;
        int failureCount = 0;
        int failureCheckPoint = 5;

        /*zhanyong.miao s equal e that there is only one vni in the pool/range*/
        while (s <= e) {
            // if failing failureCheckPoint times, the range is probably full,
            // we check the range.
            // why don't we check before steppingAllocate()? because in that case we
            // have to count the used IP every time allocating a Vni, and count operation
            // is a full scan in DB, which is very costly
            if (failureCheckPoint == failureCount++) {
                long count = Q.New(VxlanNetworkVO.class)
                        .eq(VxlanNetworkVO_.poolUuid, poolUuid)
                        .gte(VxlanNetworkVO_.vni, s)
                        .lte(VxlanNetworkVO_.vni, e)
                        .count();
                if (count == total) {
                    logger.debug(String.format("vni range[uuid:%s] has no vni available, try next one", rangeUuid));
                    return null;
                } else {
                    failureCount = 0;
                }
            }

            int te = s + step;
            te = te > e ? e : te;
            List<Integer> used = Q.New(VxlanNetworkVO.class)
                    .select(VxlanNetworkVO_.vni)
                    .gte(VxlanNetworkVO_.vni, s)
                    .lte(VxlanNetworkVO_.vni, te)
                    .eq(VxlanNetworkVO_.poolUuid, poolUuid)
                    .listValues();
            if (te - s + 1 == used.size()) {
                s += step;
                continue;
            }

            Collections.sort(used);

            return randomAllocateVni(s, te, used);
        }

        return null;
    }

    private static Integer randomAllocateVni(Integer startVni, Integer endVni, List<Integer> allocatedVnis) {
        int total = (endVni - startVni + 1);
        if (total == allocatedVnis.size()) {
            return null;
        }

        BitSet full = new BitSet(total);
        for (Integer alloc : allocatedVnis) {
            full.set(alloc - startVni);
        }

        int next = random.nextInt(total);
        int a = full.nextClearBit(next);

        if (a >= total) {
            a = full.nextClearBit(0);
        }

        return a + startVni;
    }

}
