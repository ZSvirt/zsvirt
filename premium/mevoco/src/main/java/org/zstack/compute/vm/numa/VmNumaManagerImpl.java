package org.zstack.compute.vm.numa;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.compute.vm.MevocoVmSystemTags;
import org.zstack.core.Platform;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.header.Component;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.header.vm.*;
import org.zstack.kvm.*;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.List;
import java.util.Objects;

import static org.zstack.core.Platform.argerr;

/**
 * Created by longtao.wu@zstack.io on 21/12/01
 */
@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class VmNumaManagerImpl implements Component, KVMStartVmExtensionPoint, KVMDestroyVmExtensionPoint {
    private static final CLogger logger = Utils.getLogger(VmNumaManagerImpl.class);

    @Autowired
    private DatabaseFacade dbf;

    @Override
    public void beforeStartVmOnKvm(KVMHostInventory host, VmInstanceSpec spec, KVMAgentCommands.StartVmCmd cmd) {
        logger.debug(String.format("delete vm[%s] numa nodes in VmInstanceNumaNodeVO", spec.getVmInventory().getUuid()));
        dbf.removeCollection(Q.New(VmInstanceNumaNodeVO.class)
                .eq(VmInstanceNumaNodeVO_.vmUuid, spec.getVmInventory().getUuid()).list(), VmInstanceNumaNodeVO.class);
        if (!MevocoVmSystemTags.VM_NUMA_ENABLE.hasTag(spec.getVmInventory().getUuid())) {
            return;
        }
        List<VmNumaNodeInventory> vmNumaNodes = new CommonVmNumaBasicFactory(host.getUuid(), spec).generator();
        cmd.getAddons().put("numaNodes", VmNumaNodeInventory.getVmNumaNodesTOListFromVmNumaNodeList(vmNumaNodes));
        logger.debug(String.format("persist vm[%s] numa nodes in VmInstanceNumaNodeVO", spec.getVmInventory().getUuid()));
        VmNumaNodeInventory.persistVmNumaNodes(vmNumaNodes, spec, dbf);
    }

    @Override
    public void startVmOnKvmSuccess(KVMHostInventory host, VmInstanceSpec spec) {
    }

    @Override
    public void startVmOnKvmFailed(KVMHostInventory host, VmInstanceSpec spec, ErrorCode err) {
        if (!MevocoVmSystemTags.VM_NUMA_ENABLE.hasTag(spec.getVmInventory().getUuid())) {
            return;
        }
        logger.debug(String.format("vm start fail, delete vm[%s] numa nodes in VmInstanceNumaNodeVO", spec.getVmInventory().getUuid()));
        dbf.removeCollection(Q.New(VmInstanceNumaNodeVO.class)
                .eq(VmInstanceNumaNodeVO_.vmUuid, spec.getVmInventory().getUuid()).list(), VmInstanceNumaNodeVO.class);
    }


    @Override
    public void beforeDestroyVmOnKvm(KVMHostInventory host, VmInstanceInventory vm, KVMAgentCommands.DestroyVmCmd cmd) {

    }

    @Override
    public void beforeDirectlyDestroyVmOnKvm(KVMAgentCommands.DestroyVmCmd cmd) {

    }

    @Override
    public void destroyVmOnKvmSuccess(KVMHostInventory host, VmInstanceInventory vm) {
        dbf.removeCollection(Q.New(VmInstanceNumaNodeVO.class).eq(VmInstanceNumaNodeVO_.vmUuid, vm.getUuid()).list(), VmInstanceNumaNodeVO.class);
        logger.debug(String.format("delete vm[%s] numa nodes in VmInstanceNumaNodeVO.", vm.getUuid()));
    }

    @Override
    public void destroyVmOnKvmFailed(KVMHostInventory host, VmInstanceInventory vm, ErrorCode err) {

    }

    @Override
    public boolean start() {
        MevocoVmSystemTags.VM_NUMA_ENABLE.installValidator((resourceUuid, resourceType, systemTag) -> {
            String vmNumaTag = MevocoVmSystemTags.VM_NUMA_ENABLE.getTokenByTag(systemTag, MevocoVmSystemTags.VM_NUMA_ENABLE_TOKEN);
            if (!Objects.equals(vmNumaTag, Boolean.TRUE.toString())) {
                throw new OperationFailureException(argerr(Platform.i18n("fail to set vm numa, incorrect input format, only accept true")));
            }
        });

        return true;
    }

    @Override
    public boolean stop() {
        return true;
    }
}
