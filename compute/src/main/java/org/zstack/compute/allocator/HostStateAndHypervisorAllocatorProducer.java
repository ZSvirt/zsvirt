package org.zstack.compute.allocator;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.SimpleQuery;
import org.zstack.header.allocator.HostCandidateProducer;
import org.zstack.header.host.HostState;
import org.zstack.header.host.HostStatus;
import org.zstack.header.host.HostVO;
import org.zstack.header.host.HostVO_;

@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class HostStateAndHypervisorAllocatorProducer implements HostCandidateProducer {
    @Autowired
    private DatabaseFacade dbf;

    @Override
    public void produce(HostCandidateProducerContext context) {
        final String hypervisorType = context.spec.getHypervisorType();

        SimpleQuery<HostVO> query = dbf.createQuery(HostVO.class);
        query.add(HostVO_.state, SimpleQuery.Op.EQ, HostState.Enabled);
        query.add(HostVO_.status, SimpleQuery.Op.EQ, HostStatus.Connected);
        if (hypervisorType != null) {
            query.add(HostVO_.hypervisorType, SimpleQuery.Op.EQ, hypervisorType);
        }

        if (context.usePagination()) {
            query.setStart(context.paginationInfo.getOffset());
            query.setLimit(context.paginationInfo.getLimit());
        }

        context.accept(query.list());
    }
}
