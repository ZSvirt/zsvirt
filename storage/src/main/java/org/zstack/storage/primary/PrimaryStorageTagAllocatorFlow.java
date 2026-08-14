package org.zstack.storage.primary;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.core.componentloader.PluginRegistry;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.core.errorcode.ErrorFacade;
import org.zstack.header.configuration.DiskOfferingVO;
import org.zstack.header.core.workflow.FlowTrigger;
import org.zstack.header.core.workflow.NoRollbackFlow;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.header.storage.primary.PrimaryStorageAllocationSpec;
import org.zstack.header.storage.primary.PrimaryStorageConstant.AllocatorParams;
import org.zstack.header.storage.primary.PrimaryStorageTagAllocatorExtensionPoint;
import org.zstack.header.storage.primary.PrimaryStorageVO;
import org.zstack.header.tag.SystemTagInventory;
import org.zstack.header.tag.SystemTagVO;
import org.zstack.header.tag.SystemTagVO_;
import org.zstack.header.vm.VmInstanceVO;
import org.zstack.utils.DebugUtils;

import java.util.List;
import java.util.Map;

import static org.zstack.core.Platform.operr;

/**
 */
@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class PrimaryStorageTagAllocatorFlow extends NoRollbackFlow {
    @Autowired
    protected DatabaseFacade dbf;
    @Autowired
    protected ErrorFacade errf;
    @Autowired
    private PluginRegistry pluginRgty;

    protected final List<PrimaryStorageTagAllocatorExtensionPoint> tagExtensions = pluginRgty.getExtensionList(PrimaryStorageTagAllocatorExtensionPoint.class);


    @Override
    public void run(FlowTrigger trigger, Map data) {
        PrimaryStorageAllocationSpec spec = (PrimaryStorageAllocationSpec) data.get(AllocatorParams.SPEC);
        List<PrimaryStorageVO> candidates = (List<PrimaryStorageVO>) data.get(AllocatorParams.CANDIDATES);
        DebugUtils.Assert(candidates != null && !candidates.isEmpty(), "PrimaryStorageTagAllocatorFlow cannot be the first element in allocator chain");

        List<SystemTagVO> tvos = null;
        if (spec.getVmInstanceUuid() != null) {
            tvos = Q.New(SystemTagVO.class)
                    .eq(SystemTagVO_.resourceType, VmInstanceVO.class.getSimpleName())
                    .eq(SystemTagVO_.resourceUuid, spec.getVmInstanceUuid())
                    .list();
        } else if (spec.getDiskOfferingUuid() != null) {
            tvos = Q.New(SystemTagVO.class)
                    .eq(SystemTagVO_.resourceType, DiskOfferingVO.class.getSimpleName())
                    .eq(SystemTagVO_.resourceUuid, spec.getDiskOfferingUuid())
                    .list();
        }

        if (tvos != null && !tvos.isEmpty()) {
            candidates = callTagExtensions(SystemTagInventory.valueOf(tvos), candidates);
            data.put(AllocatorParams.CANDIDATES, candidates);
        }

        trigger.next();
    }

    protected List<PrimaryStorageVO> callTagExtensions(List<SystemTagInventory> tags, List<PrimaryStorageVO> candidates) {
        List<PrimaryStorageVO> ret;
        for (PrimaryStorageTagAllocatorExtensionPoint extp : tagExtensions) {
            ret = extp.allocatePrimaryStorage(tags, candidates);
            if (ret == null) {
                continue;
            }

            if (ret.isEmpty()) {
                throw new OperationFailureException(operr("PrimaryStorageTagAllocatorExtensionPoint[%s] returns zero primary storage candidate", extp.getClass().getName()));
            }

            candidates = ret;
        }

        return candidates;
    }
}
