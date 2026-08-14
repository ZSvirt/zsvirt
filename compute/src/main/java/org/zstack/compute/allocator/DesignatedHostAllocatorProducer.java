package org.zstack.compute.allocator;

import org.apache.commons.collections.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.transaction.annotation.Transactional;
import org.zstack.core.Platform;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.header.allocator.HostCandidateProducer;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.host.HostState;
import org.zstack.header.host.HostStatus;
import org.zstack.header.host.HostVO;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import javax.persistence.TypedQuery;
import java.util.List;

@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class DesignatedHostAllocatorProducer implements HostCandidateProducer {
    private static final CLogger logger = Utils.getLogger(DesignatedHostAllocatorProducer.class);

    @Autowired
    private DatabaseFacade dbf;

    @Transactional(readOnly = true)
    private List<HostVO> allocate(String zoneUuid,
                                  List<String> clusterUuids,
                                  String hostUuid,
                                  String hypervisorType,
                                  HostCandidateProducerContext context) {
        StringBuilder sql = new StringBuilder();
        sql.append("select h from HostVO h where ");
        if (zoneUuid != null) {
            sql.append(String.format("h.zoneUuid = '%s' and ", zoneUuid));
        }
        if (!CollectionUtils.isEmpty(clusterUuids)) {
            sql.append(String.format("h.clusterUuid in ('%s') and ", String.join("','", clusterUuids)));
        }
        if (hostUuid != null) {
            sql.append(String.format("h.uuid = '%s' and ", hostUuid));
        }
        if (hypervisorType != null) {
            sql.append(String.format("h.hypervisorType = '%s' and ", hypervisorType));
        }
        sql.append(String.format("h.status = '%s' and h.state = '%s'", HostStatus.Connected, HostState.Enabled));
        logger.debug("DesignatedHostAllocatorFlow sql: " + sql);
        TypedQuery<HostVO> query = dbf.getEntityManager().createQuery(sql.toString(), HostVO.class);

        if (context.usePagination()) {
            query.setFirstResult(context.paginationInfo.getOffset());
            query.setMaxResults(context.paginationInfo.getLimit());
        }

        return query.getResultList();
    }

    @Override
    public void produce(HostCandidateProducerContext context) {
        String zoneUuid = context.spec.getZoneUuid();
        List<String> clusterUuids = context.spec.getClusterUuids();
        String hostUuid = context.spec.getHostUuid();
        String hypervisorType = context.spec.getHypervisorType();

        if (zoneUuid == null && CollectionUtils.isEmpty(clusterUuids) && hostUuid == null && hypervisorType == null) {
            return;
        }

        final List<HostVO> results = allocate(zoneUuid, clusterUuids, hostUuid, hypervisorType, context);
        if (results.isEmpty()) {
            context.reportError(reportNoHostFound(zoneUuid, clusterUuids, hostUuid, hypervisorType));
            return;
        }

        context.accept(results);
    }

    private ErrorCode reportNoHostFound(String zoneUuid, List<String> clusterUuids, String hostUuid, String hypervisorType) {
        StringBuilder args = new StringBuilder();
        if (zoneUuid != null) {
            args.append(String.format("zoneUuid=%s", zoneUuid)).append(" ");
        }
        if (!clusterUuids.isEmpty()) {
            args.append(String.format("clusterUuid in %s", clusterUuids)).append(" ");
        }
        if (hostUuid != null) {
            args.append(String.format("hostUuid=%s", hostUuid)).append(" ");
        }
        if (hypervisorType != null) {
            args.append(String.format("hypervisorType=%s", hypervisorType)).append(" ");
        }
        if (args.length() == 0) {
            args.append("no conditions");
        }

        return Platform.operr("No host with %s found", args);
    }
}
