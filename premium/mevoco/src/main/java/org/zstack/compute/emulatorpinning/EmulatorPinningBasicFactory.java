package org.zstack.compute.emulatorpinning;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.compute.cpuPinning.CpuPinningConstant;
import org.zstack.compute.vm.MevocoVmSystemTags;
import org.zstack.core.db.SQL;
import org.zstack.header.Component;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.header.vm.VmInstanceSpec;
import org.zstack.kvm.KVMAgentCommands;
import org.zstack.kvm.KVMHostInventory;
import org.zstack.kvm.KVMStartVmExtensionPoint;

import java.util.Arrays;

import static org.zstack.core.Platform.argerr;

@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class EmulatorPinningBasicFactory implements Component, KVMStartVmExtensionPoint {

    @Override
    public void beforeStartVmOnKvm(KVMHostInventory host, VmInstanceSpec spec, KVMAgentCommands.StartVmCmd cmd) {
        if (!MevocoVmSystemTags.VM_EMULATOR_PINNING.hasTag(spec.getVmInventory().getUuid())) {
            return;
        }
        String emulatorPinning = MevocoVmSystemTags.VM_EMULATOR_PINNING.getTokenByResourceUuid(spec.getVmInventory().getUuid(), MevocoVmSystemTags.VM_EMULATOR_PINNING_TOKEN);
        cmd.getAddons().put("emulatorPinning", emulatorPinning);
    }

    @Override
    public void startVmOnKvmSuccess(KVMHostInventory host, VmInstanceSpec spec) {
    }

    @Override
    public void startVmOnKvmFailed(KVMHostInventory host, VmInstanceSpec spec, ErrorCode err) {
    }

    @Override
    public boolean start() {
        MevocoVmSystemTags.VM_EMULATOR_PINNING.installValidator((resourceUuid, resourceType, systemTag) -> {
            String emulatorPinning = MevocoVmSystemTags.VM_EMULATOR_PINNING.getTokenByTag(systemTag, MevocoVmSystemTags.VM_EMULATOR_PINNING_TOKEN);

            if (!emulatorPinning.matches("^[0-9,]+$")) {
                throw new OperationFailureException(argerr("incorrect input format, only accept '^[0-9,]+$'"));
            }
            Integer pCpuNum = SQL.New("select cap.cpuNum from HostCapacityVO cap, VmInstanceVO vm" +
                            " where vm.uuid =:uuid" +
                            " and cap.uuid = vm.hostUuid")
                    .param("uuid", resourceUuid).find();
            if (pCpuNum != null && Arrays.stream(emulatorPinning.split(CpuPinningConstant.CPU_SET_SEPARATOR)).anyMatch(it -> Integer.parseInt(it) >= pCpuNum)) {
                throw new OperationFailureException(argerr("the host vm located only have % CPUs", pCpuNum));
            }
        });

        return true;
    }

    @Override
    public boolean stop() {
        return true;
    }
}
