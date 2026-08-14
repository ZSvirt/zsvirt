package org.zstack.compute.vm;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.compute.vm.devices.VmTpmManager;
import org.zstack.core.cascade.CascadeConstant;
import org.zstack.core.cascade.CascadeFacade;
import org.zstack.core.componentloader.PluginRegistry;
import org.zstack.header.core.Completion;
import org.zstack.header.core.workflow.FlowTrigger;
import org.zstack.header.core.workflow.NoRollbackFlow;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.vm.VmAfterExpungeExtensionPoint;
import org.zstack.header.vm.VmDeletionStruct;
import org.zstack.header.vm.VmInstanceConstant;
import org.zstack.header.vm.VmInstanceDeletionPolicyManager.VmInstanceDeletionPolicy;
import org.zstack.header.vm.VmInstanceInventory;
import org.zstack.header.vm.VmInstanceSpec;
import org.zstack.header.vm.VmInstanceVO;
import org.zstack.utils.CollectionUtils;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.Map;

import static org.zstack.utils.CollectionDSL.list;

/**
 * Runs after volume/metadata expunge flows: fires {@link VmAfterExpungeExtensionPoint}, then
 * {@link CascadeConstant#VM_INSTANCE_EXPUNGE_CODE} so TPM / VM host-file rows are removed before the VM row is deleted.
 */
@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class VmExpungeVmResourceCascadeFlow extends NoRollbackFlow {
    private static final CLogger logger = Utils.getLogger(VmExpungeVmResourceCascadeFlow.class);

    /**
     * Snapshot of TPM uuid before cascade deletes {@link org.zstack.header.tpm.entity.TpmVO};
     * read by {@link VmInstanceBase} expunge done handler for {@code detachTpmKeyProviderBestEffort}.
     */
    public static final String EXPUNGE_CASCADE_TPM_UUID_KEY =
            VmExpungeVmResourceCascadeFlow.class.getSimpleName() + ".tpmUuidBeforeCascade";

    @Autowired
    private CascadeFacade casf;
    @Autowired
    private PluginRegistry pluginRgty;

    @Override
    public void run(FlowTrigger trigger, Map data) {
        final VmInstanceSpec spec = (VmInstanceSpec) data.get(VmInstanceConstant.Params.VmInstanceSpec.toString());
        if (spec == null || spec.getVmInventory() == null) {
            logger.warn("[VmExpungeVmResourceCascadeFlow] missing VmInstanceSpec, skip cascade");
            trigger.next();
            return;
        }

        final VmInstanceInventory inv = spec.getVmInventory();

        CollectionUtils.safeForEach(pluginRgty.getExtensionList(VmAfterExpungeExtensionPoint.class),
                arg -> arg.vmAfterExpunge(inv));

        final String tpmUuidBeforeCascade = VmTpmManager.findTpmUuidForVmOrNull(inv.getUuid());
        data.put(EXPUNGE_CASCADE_TPM_UUID_KEY, tpmUuidBeforeCascade);

        VmDeletionStruct expungeStruct = new VmDeletionStruct();
        expungeStruct.setInventory(inv);
        expungeStruct.setDeletionPolicy(VmInstanceDeletionPolicy.Direct);

        casf.asyncCascade(CascadeConstant.VM_INSTANCE_EXPUNGE_CODE, VmInstanceVO.class.getSimpleName(),
                list(expungeStruct), new Completion(trigger) {
                    @Override
                    public void success() {
                        trigger.next();
                    }

                    @Override
                    public void fail(ErrorCode errorCode) {
                        trigger.fail(errorCode);
                    }
                });
    }
}
