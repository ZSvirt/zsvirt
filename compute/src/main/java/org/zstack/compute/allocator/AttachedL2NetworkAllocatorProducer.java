package org.zstack.compute.allocator;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.transaction.annotation.Transactional;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.header.allocator.HostAllocatorSpec;
import org.zstack.header.allocator.HostCandidateProducer;
import org.zstack.header.host.HostVO;
import org.zstack.header.host.HostVO_;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class AttachedL2NetworkAllocatorProducer implements HostCandidateProducer {
    private static final CLogger logger = Utils.getLogger(AttachedL2NetworkAllocatorProducer.class);

    @Autowired
    private DatabaseFacade dbf;

    @Transactional(readOnly = true)
    private List<HostVO> listHosts(Set<String> hostUuids, HostCandidateProducerContext context) {
        if (hostUuids.isEmpty()) {
            return new ArrayList<>();
        }

        Q q = Q.New(HostVO.class)
                .in(HostVO_.uuid, hostUuids);
        if (context.usePagination()) {
            q.start(context.paginationInfo.getOffset());
            q.limit(context.paginationInfo.getLimit());
        }
        return q.list();
    }

    @Override
    public void produce(HostCandidateProducerContext context) {
        final HostAllocatorSpec spec = context.spec;

        if (spec.getL3NetworkUuids().isEmpty()) {
            if (spec.isAllowNoL3Networks()) {
                return;
            }

            Set<String> clusterUuids = new HashSet<>();
            clusterUuids.add(spec.getVmInstance().getClusterUuid());

            Q q = Q.New(HostVO.class)
                    .in(HostVO_.clusterUuid, clusterUuids);
            if (context.usePagination()) {
                q.start(context.paginationInfo.getOffset());
                q.limit(context.paginationInfo.getLimit());
            }
            context.accept(q.list());
            return;
        }

        final AttachedL2NetworkHostMatcher matcher = new AttachedL2NetworkHostMatcher();
        List<String> l3Uuids = matcher.collectL3NetworkUuids(spec);
        final List<HostVO> newcomers = listHosts(matcher.listAllowedHostUuids(l3Uuids), context);
        if (newcomers.isEmpty()) {
            logger.warn(String.format("no host found in clusters that has attached to L2Networks which have L3Networks%s", spec.getL3NetworkUuids()));
        }

        context.accept(newcomers);
    }
}
