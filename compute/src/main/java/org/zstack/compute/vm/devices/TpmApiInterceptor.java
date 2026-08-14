package org.zstack.compute.vm.devices;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.Platform;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.db.Q;
import org.zstack.header.apimediator.ApiMessageInterceptionException;
import org.zstack.header.apimediator.ApiMessageInterceptor;
import org.zstack.header.apimediator.InterceptorForService;
import org.zstack.header.apimediator.StopRoutingException;
import org.zstack.header.errorcode.SysErrors;
import org.zstack.header.message.APIMessage;
import org.zstack.header.tpm.api.*;
import org.zstack.header.tpm.entity.TpmVO;
import org.zstack.header.tpm.entity.TpmVO_;
import org.zstack.header.vm.VmInstanceVO;
import org.zstack.header.vm.VmInstanceVO_;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import static org.zstack.core.Platform.err;
import static org.zstack.header.tpm.TpmConstants.*;
import static org.zstack.header.tpm.TpmErrors.*;
import static org.zstack.header.vm.VmInstanceConstant.KVM_HYPERVISOR_TYPE;

@InterceptorForService("tpm")
public class TpmApiInterceptor implements ApiMessageInterceptor {
    private static final CLogger logger = Utils.getLogger(TpmApiInterceptor.class);

    @Autowired
    private CloudBus bus;

    @Override
    public APIMessage intercept(APIMessage msg) throws ApiMessageInterceptionException {
        if (msg instanceof APIGetTpmCapabilityMsg) {
            validate((APIGetTpmCapabilityMsg) msg);
        } else if (msg instanceof APIAddTpmMsg) {
            validate((APIAddTpmMsg) msg);
        } else if (msg instanceof APIRemoveTpmMsg) {
            validate((APIRemoveTpmMsg) msg);
        } else if (msg instanceof APIUpdateTpmMsg) {
            validate((APIUpdateTpmMsg) msg);
        }

        return msg;
    }

    private void validate(APIGetTpmCapabilityMsg msg) {
        makeSureVmInstanceIsKvmType(msg.getVmInstanceUuid());
    }

    private void validate(APIAddTpmMsg msg) {
        makeSureVmInstanceIsKvmType(msg.getVmInstanceUuid());

        boolean tpmExists = Q.New(TpmVO.class)
                .eq(TpmVO_.vmInstanceUuid, msg.getVmInstanceUuid())
                .isExists();
        if (tpmExists) {
            throw new ApiMessageInterceptionException(err(TPM_ALREADY_EXISTS, "tpm device already exists"));
        }

        boolean vmInSupportState = Q.New(VmInstanceVO.class)
                .eq(VmInstanceVO_.uuid, msg.getVmInstanceUuid())
                .in(VmInstanceVO_.state, SUPPORT_VM_STATES_FOR_TPM_OPERATION)
                .isExists();
        if (!vmInSupportState) {
            throw new ApiMessageInterceptionException(err(VM_STATE_ERROR,
                    "The current VM state does not support adding TPM operations")
                    .withOpaque("support.vm.state", SUPPORT_VM_STATES_FOR_TPM_OPERATION));
        }

        if (msg.getResourceUuid() == null) {
            msg.setResourceUuid(Platform.getUuid());
        }
    }

    private void validate(APIRemoveTpmMsg msg) {
        makeSureVmInstanceIsKvmType(msg.getVmInstanceUuid());

        boolean tpmExists = Q.New(TpmVO.class)
                .eq(TpmVO_.vmInstanceUuid, msg.getVmInstanceUuid())
                .isExists();
        if (!tpmExists) {
            APIRemoveTpmEvent evt = new APIRemoveTpmEvent(msg.getId());
            bus.publish(evt);
            throw new StopRoutingException();
        }

        boolean vmInSupportState = Q.New(VmInstanceVO.class)
                .eq(VmInstanceVO_.uuid, msg.getVmInstanceUuid())
                .in(VmInstanceVO_.state, SUPPORT_VM_STATES_FOR_TPM_OPERATION)
                .isExists();
        if (!vmInSupportState) {
            throw new ApiMessageInterceptionException(err(VM_STATE_ERROR,
                    "The current VM state does not support removing TPM operations")
                    .withOpaque("support.vm.state", SUPPORT_VM_STATES_FOR_TPM_OPERATION));
        }
    }

    private void validate(APIUpdateTpmMsg msg) {
        makeSureVmInstanceIsKvmType(msg.getVmInstanceUuid());
        bus.makeTargetServiceIdByResourceUuid(msg, SERVICE_ID, msg.getTpmUuid());
    }

    private void makeSureVmInstanceIsKvmType(String vmInstanceUuid) {
        String hypervisorType = Q.New(VmInstanceVO.class)
                .select(VmInstanceVO_.hypervisorType)
                .eq(VmInstanceVO_.uuid, vmInstanceUuid)
                .findValue();
        if (!KVM_HYPERVISOR_TYPE.equals(hypervisorType)) {
            throw new ApiMessageInterceptionException(err(SysErrors.NOT_SUPPORTED, "only allowed for kvm type VM instance"));
        }
    }
}
