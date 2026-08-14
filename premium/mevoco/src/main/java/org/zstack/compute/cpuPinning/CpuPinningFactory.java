package org.zstack.compute.cpuPinning;

import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.utils.RangeSet;

import java.io.Serializable;
import java.util.*;

import static org.zstack.core.Platform.operr;

public interface CpuPinningFactory {
    final class CpuPinningRelation {
        long vCpu;
        Set<Long> pCpuSet;

        CpuPinningRelation(long vCpu, Collection<Long> pCpus){
            this.vCpu = vCpu;
            this.pCpuSet = new HashSet<>(pCpus);
        }
    }

    List<CpuPinningRelation> getRelationFromString(String relation);
    String convertRelationToString(Collection<CpuPinningRelation> relations);
}
