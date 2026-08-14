package org.zstack.pluginpremium.compute.allocator;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.compute.vm.VmSystemTags;
import org.zstack.core.db.SQL;
import org.zstack.header.allocator.AbstractHostAllocatorFlow;
import org.zstack.header.allocator.HostCandidate;
import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;
import javax.persistence.Tuple;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import static org.zstack.core.Platform.i18m;
import static org.zstack.utils.CollectionUtils.toMap;

/**
 * Created by lining on 2018/3/6.
 */
@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class MaxInstancePerHostAllocatorFlow extends AbstractHostAllocatorFlow {

    private static final CLogger logger = Utils.getLogger(MaxInstancePerHostAllocatorFlow.class);

    private int getMaxInstancePerHost() {
        String maxInstancePerHost = VmSystemTags.MAX_INSTANCE_PER_HOST.getTokenByResourceUuid(spec.getVmInstance().getUuid(), VmSystemTags.MAX_INSTANCE_PER_HOST_TOKEN);

        if (maxInstancePerHost == null) {
            throw new CloudRuntimeException(String.format("Can not find the %s for %s", HostAllocatorSystemTags.MAX_INSTANCE_PER_HOST_TOKEN, HostAllocatorConstant.MAX_INSTANCE_PER_HOST_HOST_ALLOCATOR_STRATEGY_TYPE));
        }
        return Integer.parseInt(maxInstancePerHost);
    }

    @Override
    public void allocate() {
        int maxInstancePerHost = getMaxInstancePerHost();
        Map<String, HostCandidate> uuidCandidateMap = toMap(candidates, HostCandidate::getUuid, Function.identity());

        String sql = "select host.uuid, count(vm.uuid) as cnt" +
                " from HostVO host" +
                " Left Join VmInstanceVO vm on host.uuid = vm.hostUuid" +
                " where host.uuid in (:huuids)" +
                " group by host.uuid order by cnt";
        List<Tuple> tuples = SQL.New(sql, Tuple.class).param("huuids", uuidCandidateMap.keySet()).list();
        Map<String, Long> hostCountMap = toMap(tuples,
                tuple -> tuple.get(0, String.class),
                tuple -> tuple.get(1, Long.class));

        for (HostCandidate candidate : candidates) {
            String hostUuid = candidate.getUuid();
            Long count = hostCountMap.get(hostUuid);

            if (count != null && count >= maxInstancePerHost) {
                reject(uuidCandidateMap.get(hostUuid),
                        i18m("The number of VMs on this host exceeds the limit %s", maxInstancePerHost));
            }
        }

        next();
    }
}
