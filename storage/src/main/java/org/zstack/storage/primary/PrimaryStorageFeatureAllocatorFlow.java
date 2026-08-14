package org.zstack.storage.primary;

import org.apache.commons.collections.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.core.componentloader.PluginRegistry;
import org.zstack.header.core.workflow.FlowTrigger;
import org.zstack.header.core.workflow.NoRollbackFlow;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.header.storage.primary.PrimaryStorageAllocationSpec;
import org.zstack.header.storage.primary.PrimaryStorageConstant;
import org.zstack.header.storage.primary.PrimaryStorageVO;

import java.util.List;
import java.util.Map;

import static org.zstack.core.Platform.operr;

/**
 * @ Author : yh.w
 * @ Date   : Created in 18:41 2025/7/14
 */
@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class PrimaryStorageFeatureAllocatorFlow extends NoRollbackFlow {
    @Autowired
    private PluginRegistry pluginRgty;

    protected final List<PrimaryStorageFeatureAllocatorExtensionPoint> featureExtensions = pluginRgty.getExtensionList(PrimaryStorageFeatureAllocatorExtensionPoint.class);;

    @Override
    public void run(FlowTrigger trigger, Map data) {
        PrimaryStorageAllocationSpec spec = (PrimaryStorageAllocationSpec) data.get(PrimaryStorageConstant.AllocatorParams.SPEC);
        List<PrimaryStorageVO> candidates = (List<PrimaryStorageVO>) data.get(PrimaryStorageConstant.AllocatorParams.CANDIDATES);
        List<PrimaryStorageVO> ret;
        for (PrimaryStorageFeatureAllocatorExtensionPoint extp : featureExtensions) {
            ret = extp.allocatePrimaryStorage(spec.getRequiredFeatures(), candidates);
            if (ret == null) {
                continue;
            }

            if (ret.isEmpty()) {
                throw new OperationFailureException(operr("PrimaryStorageFeatureAllocatorFlow[%s] returns zero primary storage candidate", extp.getClass().getName()));
            }

            candidates = ret;
        }

        data.put(PrimaryStorageConstant.AllocatorParams.CANDIDATES, candidates);
        trigger.next();
    }

    @Override
    public boolean skip(Map data) {
        PrimaryStorageAllocationSpec spec = (PrimaryStorageAllocationSpec) data.get(PrimaryStorageConstant.AllocatorParams.SPEC);
        return CollectionUtils.isEmpty(spec.getRequiredFeatures());
    }
}
