package org.zstack.compute.sriov;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;

import org.zstack.core.asyncbatch.While;
import org.zstack.core.componentloader.PluginRegistry;
import org.zstack.header.core.Completion;
import org.zstack.header.core.WhileDoneCompletion;
import org.zstack.header.core.workflow.FlowTrigger;
import org.zstack.header.core.workflow.NoRollbackFlow;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.errorcode.ErrorCodeList;
import org.zstack.header.sriov.VmVfNicConstant;
import org.zstack.header.sriov.VmVfNicHaState;
import org.zstack.header.sriov.VmVfNicInventory;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.List;
import java.util.Map;

@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class VmVfNicHaStateCallExtensionFlow extends NoRollbackFlow {
    private static final CLogger logger = Utils.getLogger(VmVfNicHaStateCallExtensionFlow.class);
    @Autowired
    private PluginRegistry pluginRgty;

    @Override
    public void run(final FlowTrigger trigger, final Map data) {
        VmVfNicInventory nic = (VmVfNicInventory) data.get(VmVfNicConstant.Params.VmVfNicInventory.toString());
        String haState = (String) data.get(VmVfNicConstant.Params.VmVfNicHaState.toString());

        String oldHaState = nic.getHaState();
        if (VmVfNicHaState.Enabled.toString().equals(haState)
            && VmVfNicHaState.Enabled.toString().equals(oldHaState)) {
            trigger.next();
            return;
        }

        List<VmVfNicHaStateChangeExtensionPoint> exts = pluginRgty.getExtensionList(VmVfNicHaStateChangeExtensionPoint.class);

        new While<>(exts).each((ext, wcomp) -> {
            ext.afterVmVfNicHaStateChange(nic, haState, new Completion(wcomp) {
                @Override
                public void success() {
                    wcomp.done();
                }

                @Override
                public void fail(ErrorCode errorCode) {
                    wcomp.addError(errorCode);
                    wcomp.done();
                    return;
                }
            });
        }).run(new WhileDoneCompletion(trigger) {
            @Override
            public void done(ErrorCodeList errorCodeList) {
                if (errorCodeList != null && !errorCodeList.getCauses().isEmpty()) {
                    trigger.fail(errorCodeList.getCauses().get(0));
                } else {
                    trigger.next();
                }
            }
        });
    }
}
