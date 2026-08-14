package org.zstack.compute.allocator;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.transaction.annotation.Transactional;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.core.db.SQL;
import org.zstack.header.allocator.AbstractHostSortorFlow;
import org.zstack.header.allocator.HostCapacityVO;
import org.zstack.header.allocator.HostCapacityVO_;
import org.zstack.header.host.HostInventory;
import org.zstack.header.vm.VmInstanceState;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;
import javax.persistence.Tuple;
import java.util.*;
import java.util.stream.Collectors;

@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class StoppedVmAwareLeastVmPreferredSortFlow extends AbstractHostSortorFlow {
    private static final CLogger logger = Utils.getLogger(StoppedVmAwareLeastVmPreferredSortFlow.class);
    @Autowired
    private DatabaseFacade dbf;

    private List<String> getAvailableHostList(List<String> huuids) {
        List<String> availableHostList = new ArrayList<>();
        availableHostList.addAll(huuids);

        String sql = "select lastHostUuid, sum(cpuNum), sum(memorySize) from VmInstanceVO t0 " +
                "where state = :vmState " +
                "and lastHostUuid is not null " +
                "and lastHostUuid in (:huuids) group by lastHostUuid";
        List<Tuple> lastHostCpuMemList = SQL.New(sql, Tuple.class)
                .param("vmState", VmInstanceState.Stopped)
                .param("huuids", huuids)
                .list();

        lastHostCpuMemList.forEach(tuple -> {
            String hostUuid = tuple.get(0, String.class);
            long cpuNum = tuple.get(1, Long.class);
            long memSiz = tuple.get(2, Long.class);

            boolean available = Q.New(HostCapacityVO.class)
                    .eq(HostCapacityVO_.uuid, hostUuid)
                    .gt(HostCapacityVO_.availableCpu, cpuNum + spec.getCpuCapacity())
                    .gt(HostCapacityVO_.availableMemory, memSiz + spec.getMemoryCapacity())
                    .isExists();

            if (!available) {
                availableHostList.remove(hostUuid);
            }
        });

        return availableHostList;
    }

    private List<String> sortByLastHostVmCount(List<String> availableHostList) {
        List<String> result = new ArrayList();

        Map<String, Long> lastHostVmCountMap = new HashMap<>();
        for (String hostUuid : availableHostList) {
            lastHostVmCountMap.put(hostUuid, 0L);
        }

        List<Tuple> lastHostVmCountList = SQL.New("select lastHostUuid, count(uuid) from VmInstanceVO t0 " +
                "where state = :vmState and lastHostUuid in (:huuids) group by lastHostUuid", Tuple.class)
                .param("vmState", VmInstanceState.Stopped)
                .param("huuids", availableHostList)
                .list();
        lastHostVmCountList.forEach(tuple -> {
            String hostUuid = tuple.get(0, String.class);
            long vmCount = tuple.get(1, Long.class);
            lastHostVmCountMap.put(hostUuid, vmCount);
        });

        lastHostVmCountMap.entrySet().stream().sorted(Map.Entry.<String, Long>comparingByValue()).forEachOrdered(e -> result.add(e.getKey()));
        return result;
    }

    @Transactional(readOnly = true)
    private List<String> findLeastVmHost(List<String> huuids) {
        List<String> availableHostList = getAvailableHostList(huuids);
        if (availableHostList.isEmpty()) {
            availableHostList.addAll(huuids);
        }

        List<String> result = sortByLastHostVmCount(availableHostList);
        huuids.forEach(hostUuid -> {
            if (result.contains(hostUuid)) {
                return;
            }
            result.add(hostUuid);
        });
        return result;
    }

    @Override
    public void sort() {
        Map<String, HostInventory> hosts = candidates.stream().collect(Collectors.toMap(HostInventory::getUuid, (candidate) -> candidate));
        List<String> sortedHostUuids = findLeastVmHost(candidates.stream().map(HostInventory::getUuid).collect(Collectors.toList()));

        candidates.clear();
        sortedHostUuids.forEach(huuid -> candidates.add(hosts.get(huuid)));
        logger.debug(String.format("Sorted by  StoppedVmAwareLeastVmPreferred the hosts %s", candidates.stream().map(c -> c.getUuid()).collect(Collectors.toList())));
    }

    @Override
    public boolean skipNext() {
        return true;
    }
}
