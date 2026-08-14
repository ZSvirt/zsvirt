package org.zstack.compute.vm;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.header.core.Completion;
import org.zstack.header.core.workflow.FlowTrigger;
import org.zstack.header.core.workflow.NoRollbackFlow;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.vm.VmInstanceConstant;
import org.zstack.header.vm.VmInstanceSpec;

import java.util.Map;

/**
 * Created by GuoYi on 2019-09-24.
 */
@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class BeforeVmStopOnHypervisorFlow extends NoRollbackFlow {
    @Autowired
    private VmInstanceExtensionPointEmitter extEmitter;

    @Override
    public void run(final FlowTrigger chain, Map data) {
        final VmInstanceSpec spec = (VmInstanceSpec) data.get(VmInstanceConstant.Params.VmInstanceSpec.toString());
        extEmitter.beforeVmStop(spec.getVmInventory(), new Completion(chain) {
            @Override
            public void success() {
                chain.next();
            }

            @Override
            public void fail(ErrorCode errorCode) {
                chain.fail(errorCode);
            }
        });
    }
}
