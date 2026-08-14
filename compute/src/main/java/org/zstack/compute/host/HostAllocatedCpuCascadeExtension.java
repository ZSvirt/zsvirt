package org.zstack.compute.host;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.cascade.AbstractAsyncCascadeExtension;
import org.zstack.core.cascade.CascadeAction;
import org.zstack.core.cascade.CascadeConstant;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.SimpleQuery;
import org.zstack.header.allocator.HostAllocatedCpuVO;
import org.zstack.header.allocator.HostAllocatedCpuVO_;
import org.zstack.header.core.Completion;
import org.zstack.header.host.HostInventory;
import org.zstack.header.host.HostVO;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.zstack.utils.CollectionUtils.transformAndRemoveNull;

public class HostAllocatedCpuCascadeExtension extends AbstractAsyncCascadeExtension {
    private static final CLogger logger = Utils.getLogger(HostAllocatedCpuCascadeExtension.class);

    @Autowired
    private DatabaseFacade dbf;

    @Override
    public void asyncCascade(CascadeAction action, Completion completion) {
        if (action.isActionCode(CascadeConstant.DELETION_DELETE_CODE) ||
                action.isActionCode(CascadeConstant.DELETION_CLEANUP_CODE) ||
                action.isActionCode(CascadeConstant.DELETION_FORCE_DELETE_CODE)) {
            handleDeletion(action, completion);
        } else {
            completion.success();
        }
    }

    private void handleDeletion(CascadeAction action, Completion completion) {
        try {
            final List<String> hostUuids = hostFromAction(action);
            if (hostUuids != null && !hostUuids.isEmpty()) {
                hostUuids.forEach(this::deleteHostAllocatedCpus);
            }
        } catch (NullPointerException e) {
            logger.warn(e.getMessage());
        } finally {
            completion.success();
        }
    }

    private void deleteHostAllocatedCpus(String hostUuid) {
        SimpleQuery<HostAllocatedCpuVO> cpusQuery = dbf.createQuery(HostAllocatedCpuVO.class);
        cpusQuery.add(HostAllocatedCpuVO_.hostUuid, SimpleQuery.Op.EQ, hostUuid);
        List<HostAllocatedCpuVO> cpus = cpusQuery.list();
        if (!cpus.isEmpty()) {
            for (HostAllocatedCpuVO cpu: cpus) {
                dbf.remove(cpu);
            }
        }
    }

    private List<String> hostFromAction(CascadeAction action) {
        List<String> hostUuids = new ArrayList<>();
        if (HostVO.class.getSimpleName().equals(action.getParentIssuer())) {
            List<HostInventory> hosts = action.getParentIssuerContext();
            hostUuids = transformAndRemoveNull(hosts, HostInventory::getUuid);
        }
        return hostUuids;
    }


    @Override
    public List<String> getEdgeNames() {
        return Arrays.asList(HostVO.class.getSimpleName());
    }

    @Override
    public String getCascadeResourceName() {
        return HostAllocatedCpuVO.class.getSimpleName();
    }

    @Override
    public CascadeAction createActionForChildResource(CascadeAction action) {
        return null;
    }
}
