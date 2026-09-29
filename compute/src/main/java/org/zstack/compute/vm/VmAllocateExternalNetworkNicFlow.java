package org.zstack.compute.vm;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.core.asyncbatch.While;
import org.zstack.core.componentloader.PluginRegistry;
import org.zstack.header.core.Completion;
import org.zstack.header.core.WhileDoneCompletion;
import org.zstack.header.core.workflow.Flow;
import org.zstack.header.core.workflow.FlowRollback;
import org.zstack.header.core.workflow.FlowTrigger;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.errorcode.ErrorCodeList;
import org.zstack.header.vm.AfterAllocateExternalNetworkNicExtensionPoint;
import org.zstack.header.vm.VmInstanceConstant;
import org.zstack.header.vm.VmInstanceSpec;
import org.zstack.header.vm.VmNicInventory;

import java.util.List;
import java.util.Map;

@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class VmAllocateExternalNetworkNicFlow implements Flow {
    @Autowired
    private PluginRegistry pluginRgty;

    @Override
    public void run(FlowTrigger trigger, Map data) {
        VmInstanceSpec spec = (VmInstanceSpec) data.get(VmInstanceConstant.Params.VmInstanceSpec.toString());
        List<VmNicInventory> nics = spec.getDestNics();
        List<AfterAllocateExternalNetworkNicExtensionPoint> exts = pluginRgty.getExtensionList(AfterAllocateExternalNetworkNicExtensionPoint.class);
        if (nics == null || nics.isEmpty() || exts.isEmpty()) {
            trigger.next();
            return;
        }
        new While<>(exts).each((ext, completion) -> ext.afterAllocateExternalNetworkNic(spec, nics, new Completion(completion) {
            @Override
            public void success() {
                completion.done();
            }

            @Override
            public void fail(ErrorCode errorCode) {
                completion.addError(errorCode);
                completion.allDone();
            }
        })).run(new WhileDoneCompletion(trigger) {
            @Override
            public void done(ErrorCodeList errors) {
                if (errors.hasError()) {
                    trigger.fail(errors.getCauses().get(0));
                } else {
                    trigger.next();
                }
            }
        });
    }

    @Override
    public void rollback(FlowRollback chain, Map data) {
        VmInstanceSpec spec = (VmInstanceSpec) data.get(VmInstanceConstant.Params.VmInstanceSpec.toString());
        List<VmNicInventory> nics = spec.getDestNics();
        List<AfterAllocateExternalNetworkNicExtensionPoint> exts = pluginRgty.getExtensionList(AfterAllocateExternalNetworkNicExtensionPoint.class);
        if (nics == null || nics.isEmpty() || exts.isEmpty()) {
            chain.rollback();
            return;
        }
        new While<>(exts).each((ext, completion) -> ext.rollbackExternalNetworkNic(spec, nics, new Completion(completion) {
            @Override
            public void success() {
                completion.done();
            }

            @Override
            public void fail(ErrorCode errorCode) {
                completion.done();
            }
        })).run(new WhileDoneCompletion(chain) {
            @Override
            public void done(ErrorCodeList errors) {
                chain.rollback();
            }
        });
    }
}
