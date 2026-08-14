package org.zstack.compute.affinityGroup;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.core.db.SQL;
import org.zstack.header.affinitygroup.*;
import org.zstack.header.host.HostInventory;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import javax.persistence.Tuple;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static java.util.Arrays.asList;

@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class AffinityGroupHostRating implements AffinityGroupRatingFactory {
    @Autowired
    private DatabaseFacade dbf;

    private static final CLogger logger = Utils.getLogger(AffinityGroupFilterFlow.class);

    @Override
    public AffinityGroupType getAffinityGroupType() {
        return AffinityGroupType.HOST;
    }

    @Override
    public Map<String, Long> ratingHostCandidates(AffinityGroupRatingStruct struct) {
        AffinityGroupInventory inv = AffinityGroupInventory.valueOf(dbf.findByUuid(struct.getAffinityGroupUuid(), AffinityGroupVO.class));

        /* count running vm number of affinityGroup per hosts */
        String sql = "select vm.hostUuid, count(vm.uuid) as cnt from VmInstanceVO vm, AffinityGroupUsageVO ref where ref.affinityGroupUuid = :agUuid " +
                "and ref.resourceUuid = vm.uuid and vm.uuid != :vmUuid and vm.hostUuid is not NULL group by vm.hostUuid order by cnt";
        List<Tuple> tuples = SQL.New(sql, Tuple.class).param("agUuid", inv.getUuid()).param("vmUuid", struct.getSpec().getVmInstance().getUuid()).list();

        if (tuples.isEmpty()){
            return struct.getCandidates().stream().collect(Collectors.toMap(host->host.getUuid(), host->0L));
        } else {
            Map<String, Long> hostWithVm = tuples.stream().collect(Collectors.toMap(tuple ->
                            tuple.get(0, String.class), tuple -> tuple.get(1, Long.class)));

            Map<String, Long> hostWithoutVm = struct.getCandidates().stream().filter(host -> !hostWithVm.containsKey(host.getUuid()))
                    .collect(Collectors.toMap(host->host.getUuid(), host->0L));
            hostWithVm.forEach((k, v) -> hostWithoutVm.put(k, v));
            return hostWithoutVm;
        }
    }
}
