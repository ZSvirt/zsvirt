package org.zstack.compute.vm;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.cascade.AbstractAsyncCascadeExtension;
import org.zstack.core.cascade.CascadeAction;
import org.zstack.core.cascade.CascadeConstant;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.SimpleQuery;
import org.zstack.header.core.Completion;
import org.zstack.header.vm.VmDeletionStruct;
import org.zstack.header.vm.VmInstanceNumaNodeVO;
import org.zstack.header.vm.VmInstanceNumaNodeVO_;
import org.zstack.header.vm.VmInstanceVO;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.zstack.utils.CollectionUtils.transformAndRemoveNull;

public class VmInstanceNumaCascadeExtension extends AbstractAsyncCascadeExtension {
    private static final CLogger logger = Utils.getLogger(VmInstanceNumaCascadeExtension.class);

    @Autowired
    private DatabaseFacade dbf;

    @Override
    public void asyncCascade(CascadeAction action, Completion completion) {
        if (action.isActionCode(CascadeConstant.DELETION_CLEANUP_CODE) ||
                action.isActionCode(CascadeConstant.DELETION_FORCE_DELETE_CODE)) {
            handleCleanDeletion(action, completion);
        } else {
            completion.success();
        }
    }

    private void handleCleanDeletion(CascadeAction action, Completion completion) {
        try {
            final List<String> vmUuids = vmFromAction(action);
            if (vmUuids != null && !vmUuids.isEmpty()) {
                vmUuids.forEach(this::deleteVmInstanceNumaNodes);
            }
        } catch (NullPointerException e) {
            logger.warn(e.getMessage());
        } finally {
            completion.success();
        }
    }

    private void deleteVmInstanceNumaNodes(String vmUuid) {
        SimpleQuery<VmInstanceNumaNodeVO> nodesQuery = dbf.createQuery(VmInstanceNumaNodeVO.class);
        nodesQuery.add(VmInstanceNumaNodeVO_.vmUuid, SimpleQuery.Op.EQ, vmUuid);
        List<VmInstanceNumaNodeVO> nodes = nodesQuery.list();
        if (!nodes.isEmpty()) {
            dbf.removeCollection(nodes, VmInstanceNumaNodeVO.class);
        }
    }

    private List<String> vmFromAction(CascadeAction action) {
        List<String> vmUuid = new ArrayList<>();
        if (VmInstanceVO.class.getSimpleName().equals(action.getParentIssuer())) {
            List<VmDeletionStruct> vms = action.getParentIssuerContext();
            vmUuid = transformAndRemoveNull(vms, arg -> arg.getInventory().getUuid());
        }
        return vmUuid;
    }

    @Override
    public List<String> getEdgeNames() {
        return Arrays.asList(VmInstanceVO.class.getSimpleName());
    }

    @Override
    public String getCascadeResourceName() {
        return VmInstanceNumaNodeVO.class.getSimpleName();
    }

    @Override
    public CascadeAction createActionForChildResource(CascadeAction action) {
        return null;
    }
}
