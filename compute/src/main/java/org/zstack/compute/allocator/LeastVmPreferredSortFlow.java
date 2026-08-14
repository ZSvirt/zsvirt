package org.zstack.compute.allocator;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.transaction.annotation.Transactional;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.SQL;
import org.zstack.header.allocator.AbstractHostSortorFlow;
import org.zstack.header.host.HostInventory;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import javax.persistence.Tuple;
import java.util.*;
import java.util.stream.Collectors;

/**
 * This flow returns a list of host candidates sorted by the number of their VMs.
 */
@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class LeastVmPreferredSortFlow extends AbstractHostSortorFlow {
    private static final CLogger logger = Utils.getLogger(LeastVmPreferredSortFlow.class);
    @Autowired
    private DatabaseFacade dbf;

    @Transactional(readOnly = true)
    private List<String> findLeastVmHost(List<String> huuids) {
        String sql = "select host.uuid, count(vm.uuid) as cnt" +
                " from HostVO host" +
                " Left Join VmInstanceVO vm on host.uuid = vm.hostUuid" +
                " where host.uuid in (:huuids)" +
                " group by host.uuid order by cnt";

        /* count vm numbers of each host, save to hostMap,
         * all host with same vm number will in same list */
        List<Tuple> tuples = SQL.New(sql, Tuple.class).param("huuids", huuids).list();
        Map<Long, List<String>> hostMap = new HashMap<>();
        tuples.forEach(tuple -> {
            hostMap.computeIfAbsent(tuple.get(1, Long.class), k-> new ArrayList<>()).add(tuple.get(0, String.class));
        });

        Long hostVmNumberArray[] = hostMap.keySet().toArray(new Long[hostMap.keySet().size()]);
        Arrays.sort(hostVmNumberArray);

        /* move host uuid for input list to result list,
         * 1. hosts with less vm number will be move first
         * 2. hosts with same vm number will move based on the input order
         * !!! #2 is important, it make LeastVmPreferredSortFlow will not overwrite the previous steps result
         * Testcase for this change is  TestConcurrentAllocationCase under test-premium */
        List<String> result = new ArrayList<>();
        for (long number : hostVmNumberArray) {
            Iterator<String> it = huuids.iterator();
            while (it.hasNext()) {
                String host = it.next();
                if (hostMap.get(number).contains(host)) {
                    result.add(host);
                    it.remove();
                }
            }
        }
        result.addAll(huuids);

        return result;
    }

    @Override
    public void sort() {
        Map<String, HostInventory> hosts = candidates.stream().collect(Collectors.toMap(HostInventory::getUuid, (candidate) -> candidate));
        List<String> sortedHostUuids = findLeastVmHost(candidates.stream().map(HostInventory::getUuid).collect(Collectors.toList()));

        candidates.clear();
        sortedHostUuids.forEach(huuid -> candidates.add(hosts.get(huuid)));
        if (logger.isTraceEnabled()) {
            logger.trace(String.format("Sorted by LeastVmPreferred the hosts %s", candidates.stream().map(c -> c.getUuid()).collect(Collectors.toList())));
        }
    }

    @Override
    public boolean skipNext() {
        return true;
    }
}
