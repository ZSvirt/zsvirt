package org.zstack.softwarePackage.compute;

import org.zstack.core.db.Q;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.vm.VmInstanceInventory;
import org.zstack.header.vm.VmInstanceState;
import org.zstack.header.vm.VmInstanceVO;
import org.zstack.header.vm.VmInstanceVO_;
import org.zstack.header.vm.VmNicVO;
import org.zstack.header.vm.VmNicVO_;
import org.zstack.kvm.KVMConstant;
import org.zstack.softwarePackage.header.UploadSoftwarePackageToVmMsg;

import java.util.Objects;

import static org.zstack.core.Platform.operr;

final class UploadSoftwarePackageToVmTargetChecker {
    private UploadSoftwarePackageToVmTargetChecker() {
    }

    static ErrorCode refreshForUpload(UploadSoftwarePackageToVmMsg msg) {
        return refresh(msg, false, null);
    }

    static ErrorCode refreshForUpload(UploadSoftwarePackageToVmMsg msg,
                                      UploadSoftwarePackageToVmBackend backend) {
        return refresh(msg, false, backend);
    }

    static ErrorCode refreshBeforeCopy(UploadSoftwarePackageToVmMsg msg,
                                       UploadSoftwarePackageToVmBackend backend) {
        return refresh(msg, true, backend);
    }

    private static ErrorCode refresh(UploadSoftwarePackageToVmMsg msg, boolean requireSameHost,
                                     UploadSoftwarePackageToVmBackend backend) {
        VmInstanceVO vm = Q.New(VmInstanceVO.class)
                .eq(VmInstanceVO_.uuid, msg.getVmInstanceUuid())
                .find();
        if (vm == null) {
            return operr("VM instance[uuid:%s] not found", msg.getVmInstanceUuid());
        }
        if (!KVMConstant.KVM_HYPERVISOR_TYPE.equals(vm.getHypervisorType())) {
            return operr("VM instance[uuid:%s] is not a KVM VM", msg.getVmInstanceUuid());
        }
        if (vm.getState() != VmInstanceState.Running) {
            return operr("VM instance[uuid:%s] must be Running to receive a software package, current state is %s",
                    msg.getVmInstanceUuid(), vm.getState());
        }
        if (vm.getHostUuid() == null) {
            return operr("running VM instance[uuid:%s] has no host", msg.getVmInstanceUuid());
        }
        if (requireSameHost && !Objects.equals(msg.getHostUuid(), vm.getHostUuid())) {
            return operr("VM instance[uuid:%s] moved from host[uuid:%s] to host[uuid:%s] while the software package was uploading",
                    msg.getVmInstanceUuid(), msg.getHostUuid(), vm.getHostUuid());
        }
        if (vm.getDefaultL3NetworkUuid() == null) {
            return operr("VM instance[uuid:%s] has no default L3 network", msg.getVmInstanceUuid());
        }

        String targetIp = Q.New(VmNicVO.class)
                .eq(VmNicVO_.vmInstanceUuid, vm.getUuid())
                .eq(VmNicVO_.l3NetworkUuid, vm.getDefaultL3NetworkUuid())
                .select(VmNicVO_.ip)
                .findValue();
        if (targetIp == null) {
            return operr("VM instance[uuid:%s] has no IP on its default L3 network", msg.getVmInstanceUuid());
        }
        if (backend != null) {
            ErrorCode error = backend.validateTargetVm(VmInstanceInventory.valueOf(vm));
            if (error != null) {
                return error;
            }
        }

        msg.setHostUuid(vm.getHostUuid());
        msg.setTargetIp(targetIp);
        return null;
    }
}
